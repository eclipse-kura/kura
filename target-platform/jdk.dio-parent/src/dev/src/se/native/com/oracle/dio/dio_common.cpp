/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <dio_common.h>
#include <stdio.h>
#include <javacall_os.h>
#include <javacall_logging.h>
#include <javacall_memory.h>
#include <javautil_linked_list.h>

/* Java VM interface */
static JavaVM* globalJavaVM = NULL;

/* Internal structure of a device reference */
struct _device_reference {
    javacall_handle handle;
    javacall_int32 refcount;
    javacall_mutex mutex;
    device_closer closer;
    device_locker locker;
    device_unlocker unlocker;
};

/* Signal descriptor */
struct signal {
    signal_type type;
    javacall_handle target;
    javacall_handle parameter;
    javacall_cond condition;
};

/* Signal descriptor list */
static javacall_handle signals = NULL;

/* Signal descriptor list mutex */
static javacall_mutex signalMutex = NULL;

/* Device reference list */
static javacall_handle devlist = NULL;

/* Device reference list mutex */
static javacall_mutex devlistMutex = NULL;

/* Logging file descriptor */
static FILE *loggingFile = NULL;

#ifndef DIO_LOG_FILE
#define DIO_LOG_FILE "/var/log/jdk-dio.log"
#endif /* DIO_LOG_FILE */

/* Cleanup */
static void dioCleanup() {
    if (NULL != signalMutex) {
        javacall_os_mutex_destroy(signalMutex);
        signalMutex = NULL;
    }
    if (NULL != signals) {
        javautil_list_destroy(signals);
        signals = NULL;
    }
    if (NULL != devlistMutex) {
        javacall_os_mutex_destroy(devlistMutex);
        devlistMutex = NULL;
    }
    if (NULL != devlist) {
        javautil_list_destroy(devlist);
        devlist = NULL;
    }
}

/* Returns a global reference to VM */
JavaVM* getGlobalJavaVM() {
    return globalJavaVM;
}

/* Creates a reference to the device identified by handle */
device_reference createDeviceReference(javacall_handle handle, device_closer closer,
                                       device_locker locker, device_unlocker unlocker) {
    device_reference device;
    if ((device = (device_reference)javacall_calloc(1, sizeof(_device_reference))) == NULL) {
        return INVALID_DEVICE_REFERENCE;
    }
    if ((device->mutex = javacall_os_mutex_create()) == NULL) {
        javacall_free(device);
        return INVALID_DEVICE_REFERENCE;
    }
    device->handle = handle;
    device->refcount = 1;
    device->closer = closer;
    device->locker = locker;
    device->unlocker = unlocker;
    javacall_os_mutex_lock(devlistMutex);
    javautil_list_add(devlist, device);
    javacall_os_mutex_unlock(devlistMutex);
    return device;
}

static jobject getHandleObjectFromDeviceObject(JNIEnv* env, jobject deviceObj) {
    //  to improve performance the references below should be cached
    jclass deviceBaseClass = env->FindClass("com/oracle/dio/impl/AbstractPeripheral");
    if (deviceBaseClass == NULL) {
        return NULL;
    }
    jfieldID deviceHandleField = env->GetFieldID(deviceBaseClass, "handle", "Lcom/oracle/dio/impl/Handle;");
    if (deviceHandleField == NULL) {
        return NULL;
    }
    return env->GetObjectField(deviceObj, deviceHandleField);
}

static javacall_dio_result saveDeviceReferenceToHandleObject(JNIEnv* env,
                                                             jobject handleObj,
                                                             device_reference device) {
    //  to improve performance the references below should be cached
    jclass deviceHandleClass = env->FindClass("com/oracle/dio/impl/Handle");
    if (deviceHandleClass == NULL) {
        return JAVACALL_DIO_FAIL;
    }
    jfieldID deviceNativeHandleField = env->GetFieldID(deviceHandleClass, "device_reference", "J");
    if (deviceNativeHandleField == NULL) {
        return JAVACALL_DIO_FAIL;
    }
    env->SetLongField(handleObj, deviceNativeHandleField, (jlong)device);
    return JAVACALL_DIO_OK;
}

static device_reference getDeviceReferenceFromHandleObject(JNIEnv* env,
                                                           jobject handleObj) {
    //  to improve performance the references below should be cached
    jclass deviceHandleClass = env->FindClass("com/oracle/dio/impl/Handle");
    if (deviceHandleClass == NULL) {
        return INVALID_DEVICE_REFERENCE;
    }
    jfieldID deviceNativeHandleField = env->GetFieldID(deviceHandleClass, "device_reference", "J");
    if (deviceNativeHandleField == NULL) {
        return INVALID_DEVICE_REFERENCE;
    }
    long value = env->GetLongField(handleObj, deviceNativeHandleField);
    return (device_reference)value;
}

/* Saves the reference to an instance of AbstractPeripheral */
javacall_dio_result saveDeviceReferenceToDeviceObject(JNIEnv* env, jobject deviceObj,
                                                      device_reference device) {
    jobject handleObj = getHandleObjectFromDeviceObject(env, deviceObj);
    if (handleObj == NULL) {
        return JAVACALL_DIO_FAIL;
    }
    return saveDeviceReferenceToHandleObject(env, handleObj, device);
}

/* Returns a reference stored by an instance of AbstractPeripheral */
device_reference getDeviceReferenceFromDeviceObject(JNIEnv* env, jobject deviceObj) {
    jobject handleObj = getHandleObjectFromDeviceObject(env, deviceObj);
    if (handleObj == NULL) {
        return INVALID_DEVICE_REFERENCE;
    }
    return getDeviceReferenceFromHandleObject(env, handleObj);
}

/* Returns the handle associated with the device reference */
javacall_handle getDeviceHandle(device_reference device) {
    return device->handle;
}

/* Restricted use only.
 * Retains the device reference by incrementing its reference counter, thus
 * preventing it from being immediately destroyed by releaseDeviceReference().
 * Must be balanced with a call to releaseDeviceReference() to let the
 * reference be finally destroyed. */
void retainDeviceReference(device_reference device) {
    javacall_os_mutex_lock(device->mutex);
    device->refcount++;
    javacall_os_mutex_unlock(device->mutex);
}

/* Releases a reference */
void releaseDeviceReference(device_reference device) {
    if (device == NULL) {
        return;
    }
    javacall_mutex m = device->mutex;
    javacall_os_mutex_lock(m);
    if (--device->refcount == 0) {
        javacall_os_mutex_lock(devlistMutex);
        javautil_list_remove(devlist, device);
        javacall_free(device);
        javacall_os_mutex_unlock(devlistMutex);
        device = NULL;
    }
    javacall_os_mutex_unlock(m);
    if (device == NULL) {
        javacall_os_mutex_destroy(m);
    }
}

/* Returns an existing reference to the device identified by handle. */
device_reference getDeviceReference(javacall_handle handle) {
    device_reference device = INVALID_DEVICE_REFERENCE;
    javacall_os_mutex_lock(devlistMutex);
    javautil_list_reset_iterator(devlist);
    device_reference d = NULL;
    while (javautil_list_get_next(devlist, (void**)&d) == JAVACALL_OK) {
        if (d->handle == handle) {
            device = d;
            break;
        }
    }
    javacall_os_mutex_unlock(devlistMutex);
    return device;
}

static void destroySignal(signal* sig) {
    if (sig->condition != NULL) {
        javacall_mutex m = javacall_os_cond_get_mutex(sig->condition);
        if (m != NULL) {
           javacall_os_mutex_destroy(m);
        }
        javacall_os_cond_destroy(sig->condition);
    }
    javacall_free(sig);
}

static signal* createSignal(signal_type signalType, javacall_handle signalTarget) {
    signal* sig;
    if ((sig = (signal*)javacall_calloc(1, sizeof(signal))) == NULL) {
        return NULL;
    }

    javacall_mutex cond_mutex;
    if ((cond_mutex = javacall_os_mutex_create()) == NULL) {
        destroySignal(sig);
        return NULL;
    }
    if ((sig->condition = javacall_os_cond_create(cond_mutex)) == NULL) {
        javacall_os_mutex_destroy(cond_mutex);
        destroySignal(sig);
        return NULL;
    }

    sig->type = signalType;
    sig->target = signalTarget;
    return sig;
}

static signal* findTarget(signal_type signalType, javacall_handle signalTarget) {
    signal* sig = NULL;
    javautil_list_reset_iterator(signals);
    while (javautil_list_get_next(signals, (void**)&sig) == JAVACALL_OK) {
        if (sig->type == signalType && sig->target == signalTarget) {
            return sig;
        }
    }
    return NULL;
 }

/* Blocks the current thread, until a signal is received, or the timeout expires */
javacall_dio_result waitForSignal(signal_type signalType, javacall_handle signalTarget,
                                  /*OUT*/ javacall_handle* signalParameter, long timeout) {
    signal* sig = NULL;

    javacall_os_mutex_lock(signalMutex);
    if ((sig = findTarget(signalType, signalTarget)) == NULL) {
        sig = createSignal(signalType, signalTarget);
        if (sig != NULL) {
            javautil_list_add(signals, sig);

            javacall_os_mutex_unlock(signalMutex);
            javacall_os_cond_wait(sig->condition, timeout);
        }
    } else {
        javacall_os_mutex_unlock(signalMutex);
    }

    javacall_dio_result result = JAVACALL_DIO_OUT_OF_MEMORY;

    if (sig) {
        result = JAVACALL_DIO_OK;

        javacall_os_mutex_lock(signalMutex);
        javautil_list_remove(signals, sig);
        javacall_os_mutex_unlock(signalMutex);

        if (signalParameter != NULL) {
            *signalParameter = sig->parameter;
        }

        destroySignal(sig);
    }

    return result;
}

/* Unblocks a thread that is waiting for a signal */
void generateSignal(signal_type signalType, javacall_handle signalTarget,
                    javacall_handle signalParameter) {
    javacall_os_mutex_lock(signalMutex);

    signal* sig = findTarget(signalType, signalTarget);
    if (sig != NULL) {
        sig->parameter = signalParameter;
        javacall_os_cond_signal(sig->condition);
    } else {
        // signal is being sent before wait started
        // create the signal and add it to the list
        sig = createSignal(signalType, signalTarget);
        if (sig != NULL) {
            sig->parameter = signalParameter;
            javautil_list_add(signals, sig);
        }
    }

    javacall_os_mutex_unlock(signalMutex);
}

/* Closes the referenced device */
javacall_dio_result closeDevice(device_reference device) {
    if (device == NULL) {
        return JAVACALL_DIO_INVALID_ARGUMENT;
    }
    if (device->closer == NULL) {
        return JAVACALL_DIO_FAIL;
    }
    if (device->handle != JAVACALL_INVALID_HANDLE) {
        device->closer(device->handle);
        device->handle = JAVACALL_INVALID_HANDLE;
    }
    return JAVACALL_DIO_OK;
}

/* Locks the referenced device */
javacall_dio_result lockDevice(device_reference device, /*OUT*/ javacall_handle* owner) {
    if (device->locker == NULL || device->handle == JAVACALL_INVALID_HANDLE) {
        return JAVACALL_DIO_FAIL;
    }
    return device->locker(device->handle, owner);
}

/* Unlocks the referenced device */
javacall_dio_result unlockDevice(device_reference device) {
    if (device->unlocker == NULL || device->handle == JAVACALL_INVALID_HANDLE) {
        return JAVACALL_DIO_FAIL;
    }
    return device->unlocker(device->handle);
}


extern "C" {

/* Native logging initialization */
void javacall_logging_initialize(void) {
    // Stubbed, unless this feature is required
}

void javacall_logging_printf(int severity, javacall_logging_channel channelID,
        const char* filename, int lineno, const char *format, ...)
{
    va_list args;
    va_start(args, format);
    fprintf(loggingFile, "From: %s, line %d\n", filename, lineno);
    vfprintf(loggingFile, format, args);
    fprintf(loggingFile, "\n");
    fflush(loggingFile);
    va_end(args);
}

/*******************************************************************************
 * JNI functions
 */

/* The VM calls this function upon loading the native library. */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved) {

    globalJavaVM = jvm;
    javacall_os_initialize();

    if (javautil_list_create(&signals) != JAVACALL_OK) {
        dioCleanup();
        return JNI_ERR;
    }

    if ((signalMutex = javacall_os_mutex_create()) == NULL) {
        dioCleanup();
        return JNI_ERR;
    }
    if (javautil_list_create(&devlist) != JAVACALL_OK) {
        dioCleanup();
        return JNI_ERR;
    }
    if ((devlistMutex = javacall_os_mutex_create()) == NULL) {
        dioCleanup();
        return JNI_ERR;
    }

    loggingFile = fopen(DIO_LOG_FILE, "w");

    if(loggingFile == NULL) {
        dioCleanup();
        return JNI_ERR;
    }

    return JNI_VERSION_1_2;
}

/* This function is called when the native library gets unloaded by the VM. */
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* jvm, void* reserved) {
    dioCleanup();
    javacall_os_dispose();
    globalJavaVM = NULL;

    if (loggingFile != NULL) {
        fclose(loggingFile);
    }
}

/*
 * Closes the device.
 * Class:     com_oracle_dio_impl_Handle
 * Method:    close
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_impl_Handle_close
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromHandleObject(env, obj);
    if (device != INVALID_DEVICE_REFERENCE) {
        saveDeviceReferenceToHandleObject(env, obj, INVALID_DEVICE_REFERENCE);
        closeDevice(device);
        releaseDeviceReference(device);
    }
}

/*
 * Tries locking the device.
 * Class:     com_oracle_dio_impl_Handle
 * Method:    tryLock
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_impl_Handle_tryLock
  (JNIEnv* env, jobject obj, jint timeout) {
    device_reference device = getDeviceReferenceFromHandleObject(env, obj);
    if (device != INVALID_DEVICE_REFERENCE) {
        //  add an implementation
    }
}

/*
 * Unlocks the device.
 * Class:     com_oracle_dio_impl_Handle
 * Method:    unlock
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_impl_Handle_unlock
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromHandleObject(env, obj);
    if (device != INVALID_DEVICE_REFERENCE) {
        //  add an implementation
    }
}

} // extern "C" /* JNI functions */
