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
#include <dio_exceptions.h>
#include <dio_nio.h>
#include <javacall_dio.h>
#include <javacall_i2c.h>
#include <javacall_memory.h>

static javacall_dio_result _i2c_close(javacall_handle handle) {
    javacall_i2c_close(handle);
    return JAVACALL_DIO_OK;
}

extern "C" {

/*
 * Class:     com_oracle_dio_i2cbus_impl_I2CSlaveImpl
 * Method:    open0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_i2cbus_impl_I2CSlaveImpl_open0
  (JNIEnv* env, jobject obj, jobject config, jboolean exclusive) {
    jclass configClass = env->GetObjectClass(config);

    jfieldID deviceNumberID = configClass ?
            env->GetFieldID(configClass, "controllerNumber", "I") :
            NULL;
    jint deviceNumber = deviceNumberID ?
            env->GetIntField(config, deviceNumberID) :
            0;

    jfieldID addressID = deviceNumberID ?
            env->GetFieldID(configClass, "address", "I") :
            NULL;
    jint address = addressID ?
            env->GetIntField(config, addressID) :
            0;

    jfieldID addressSizeID = addressID ?
            env->GetFieldID(configClass, "addressSize", "I") :
            NULL;
    jint addressSize = addressSizeID ?
            env->GetIntField(config, addressSizeID) :
            0;

    jfieldID clockFrequencyID = addressSizeID ?
            env->GetFieldID(configClass, "clockFrequency", "I") :
            NULL;
    jint clockFrequency = clockFrequencyID ?
            env->GetIntField(config, clockFrequencyID) :
            0;

    javacall_handle handle = JAVACALL_INVALID_HANDLE;
    javacall_dio_result result = JAVACALL_DIO_FAIL;
    if (clockFrequencyID) {
        result = javacall_i2c_open_slave_with_config(deviceNumber, address,
                                                     addressSize, clockFrequency,
                                                     exclusive != JNI_FALSE ? JAVACALL_TRUE : JAVACALL_FALSE,
                                                     &handle);

        device_reference device = INVALID_DEVICE_REFERENCE;
        if (result == JAVACALL_DIO_OK) {
            device = createDeviceReference(handle, _i2c_close,
                                           javacall_i2c_lock, javacall_i2c_unlock);
            if (device == INVALID_DEVICE_REFERENCE) {
                javacall_i2c_close(handle);
                result = JAVACALL_DIO_OUT_OF_MEMORY;
            } else {
                result = saveDeviceReferenceToDeviceObject(env, obj, device);
            }
        }
    }

    if (env->ExceptionCheck() != JNI_TRUE) {
        checkJavacallFailure(env, result);
    }
}

/*
 * Class:     com_oracle_dio_i2cbus_impl_I2CSlaveImpl
 * Method:    transfer0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_i2cbus_impl_I2CSlaveImpl_transfer0
  (JNIEnv* env, jobject obj, jboolean write, jobject buf, jint flag) {

    jint pos = 0, cap = 0, off = 0, lim = 0;
    jboolean readonly = JNI_TRUE;
    jbyte* directArray = NULL;
    jbyteArray heapArray = NULL;

    if (!getByteBufferInformation(env, buf, &directArray, &heapArray,
                                  &off, &pos, &cap, &lim,
                                  &readonly)) {
        env->ExceptionClear();
        throwRuntimeException(env, "fault source buffer");
        return 0;
    }
    if (heapArray != NULL) {
        directArray = env->GetByteArrayElements(heapArray, NULL);
    }

    // Get synchronized with the object to avoid closing the handle by another
    // thread. See the implementation of close(). Always check the handle is valid.
    // Do not generate ClosedDeviceException until all JNI calls are made.
    javacall_dio_result result = JAVACALL_DIO_OK;
    javacall_int32 bytesTransferred = 0;
    bool throwClosedException = true;
    env->MonitorEnter(obj);
    device_reference device = INVALID_DEVICE_REFERENCE;
    if (env->ExceptionCheck() != JNI_TRUE) {
        device = getDeviceReferenceFromDeviceObject(env, obj);
        if (device != INVALID_DEVICE_REFERENCE) {
            javacall_handle handle = getDeviceHandle(device);
            if (handle != JAVACALL_INVALID_HANDLE) {
                result = javacall_i2c_transfer_start(handle,
                                    (javacall_i2c_message_type)flag,
                                    (write != JNI_FALSE ? JAVACALL_TRUE : JAVACALL_FALSE),
                                    (char*)(directArray + off + pos),
                                    lim - pos,
                                    &bytesTransferred);
                if (result == JAVACALL_DIO_WOULD_BLOCK) {
                    retainDeviceReference(device);
                }
                throwClosedException = false;
            }
        }
        env->MonitorExit(obj);
    }

    // If the call returns JAVACALL_DIO_WOULD_BLOCK, have to wait for a
    // completion signal.
    if (result == JAVACALL_DIO_WOULD_BLOCK) {
        javacall_handle status = NULL;
        result = waitForSignal(SEND_SIGNAL, getDeviceHandle(device), &status, 0);
        releaseDeviceReference(device);
        device = INVALID_DEVICE_REFERENCE;

        if (result == JAVACALL_DIO_OK) {
            // check for asynchronous exceptions, proceed if none
            if (env->ExceptionCheck() == JNI_FALSE) {
                // again, get synchronized, check the handle is valid
                throwClosedException = true;
                env->MonitorEnter(obj);
                if (env->ExceptionCheck() == JNI_FALSE) {
                    device = getDeviceReferenceFromDeviceObject(env, obj);
                    if (device != INVALID_DEVICE_REFERENCE) {
                        javacall_handle handle = getDeviceHandle(device);
                        if (handle != JAVACALL_INVALID_HANDLE) {
                            result = javacall_i2c_transfer_finish(handle,
                                             JAVACALL_FALSE,
                                             (char*)(directArray + off + pos),
                                             lim - pos,
                                             &bytesTransferred);
                            if (result != JAVACALL_DIO_OK) {
                                // notified status has higher proirity
                                result = (javacall_dio_result)(javacall_int32)status;
                            }
                            throwClosedException = false;
                        }
                    }
                    env->MonitorExit(obj);
                }
            }
        }
    }

    if (heapArray != NULL && directArray != NULL) {
        env->ReleaseByteArrayElements(heapArray, directArray, 0);
    }

    if (env->ExceptionCheck() != JNI_TRUE) {
        if (throwClosedException) {
            throwClosedDeviceException(env);
        } else {
            checkJavacallFailure(env, result);
        }
    }

    return bytesTransferred;
}

/*
 * Class:     com_oracle_dio_i2cbus_impl_I2CSlaveImpl
 * Method:    getGrpID0
 */
JNIEXPORT jlong JNICALL Java_com_oracle_dio_i2cbus_impl_I2CSlaveImpl_getGrpID0
  (JNIEnv* env, jobject obj) {
    long grpId = 0;
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result;
    result = javacall_i2c_get_group_id(getDeviceHandle(device), &grpId);
    if (result != JAVACALL_DIO_OK) {
        throwIOException(env, "Error while getting group ID");
    }
    return grpId;
}

/**
 * See javacall_i2c.h for definition
 */
void javanotify_i2c_event
  (const javacall_i2c_signal_type signal, const javacall_handle handle, javacall_int32 result) {
    switch (signal) {
    case JAVACALL_I2C_SEND_SIGNAL:
        generateSignal(SEND_SIGNAL, handle, (javacall_handle)result);
        break;
    case JAVACALL_I2C_RECV_SIGNAL:
        generateSignal(RECV_SIGNAL, handle, (javacall_handle)result);
        break;
    }
}

} // extern "C"
