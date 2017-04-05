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
#include <javacall_gpio.h>
#include <javacall_memory.h>
#include <javanotify_gpio.h>
#include <dio_event_queue.h>

extern "C" {

/*
 * Opens GPIO pin and returns a handle.
 * Class:     com_oracle_dio_gpio_impl_GPIOPinImpl
 * Method:    openPinByConfig0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_gpio_impl_GPIOPinImpl_openPinByConfig0
  (JNIEnv* env, jobject obj, jint port, jint pin, jint direction, jint mode, jint trigger,
   jboolean value, jboolean access) {
    javacall_handle handle = JAVACALL_INVALID_HANDLE;
    javacall_dio_result result;
    result = javacall_gpio_pin_open(port, pin,
                                    (javacall_gpio_dir)direction,
                                    (javacall_gpio_mode)mode,
                                    (javacall_gpio_trigger_mode)trigger,
                                    (javacall_bool)value,
                                    (javacall_bool)access,
                                    &handle);

    if (result == JAVACALL_DIO_OK) {
        device_reference device;
        device = createDeviceReference(handle,
                                       javacall_gpio_pin_close,
                                       javacall_gpio_pin_lock,
                                       javacall_gpio_pin_unlock);

        if (device == INVALID_DEVICE_REFERENCE) {
            javacall_gpio_pin_close(handle);
            result = JAVACALL_DIO_OUT_OF_MEMORY;
        } else {
            result = saveDeviceReferenceToDeviceObject(env, obj, device);
        }
    }
    if (env->ExceptionCheck() != JNI_TRUE) {
        checkJavacallFailure(env, result);
    }
}

/*
 * Opens GPIO port and returns a handle.
 * Class:     com_oracle_dio_gpio_impl_GPIOPortImpl
 * Method:    openPortByConfig0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_gpio_impl_GPIOPortImpl_openPortByConfig0
  (JNIEnv* env, jobject obj, jobjectArray data, jint direction, jint value, jboolean access) {

    javacall_dio_result result = JAVACALL_DIO_OK;
    jint dataLength = env->GetArrayLength(data);
    javacall_int32* portsAndPins;

    portsAndPins = (javacall_int32*)javacall_malloc(4 * dataLength * sizeof(javacall_int32));
    if (portsAndPins != NULL) {
        for (jint i = 0; i < dataLength; i++) {
            jintArray portAndPin = (jintArray)env->GetObjectArrayElement(data, i);
            jint* elements = env->GetIntArrayElements(portAndPin, NULL);
            if (elements != NULL) {
                portsAndPins[4 * i] = elements[0];
                portsAndPins[4 * i + 1] = elements[1];
                portsAndPins[4 * i + 2] = elements[2];
                portsAndPins[4 * i + 3] = elements[3];
                env->ReleaseIntArrayElements(portAndPin, elements, JNI_ABORT);
            }
        }

        javacall_handle handle = JAVACALL_INVALID_HANDLE;
        result = javacall_gpio_port_open_with_pins((javacall_int32(*)[4])portsAndPins,
                                                   dataLength,
                                                   (javacall_gpio_dir)direction,
                                                   value,
                                                   (javacall_bool)access,
                                                   &handle);
        javacall_free(portsAndPins);

        if (result == JAVACALL_DIO_OK) {
            device_reference device;

            device = createDeviceReference(handle,
                                           javacall_gpio_port_close,
                                           javacall_gpio_port_lock,
                                           javacall_gpio_port_unlock);

            if (device == INVALID_DEVICE_REFERENCE) {
                javacall_gpio_port_close(handle);
                result = JAVACALL_DIO_OUT_OF_MEMORY;
            } else {
                result = saveDeviceReferenceToDeviceObject(env, obj, device);
            }
        }
    } else {
        result = JAVACALL_DIO_OUT_OF_MEMORY;
    }
    if (env->ExceptionCheck() != JNI_TRUE) {
        checkJavacallFailure(env, result);
    }
}

/*
 * Reads data from a pin.
 * Class:     com_oracle_dio_gpio_impl_GPIOPinImpl
 * Method:    readPin0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_gpio_impl_GPIOPinImpl_readPin0
  (JNIEnv* env, jobject obj) {
    javacall_bool value = JAVACALL_FALSE;
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result;
    result = javacall_gpio_pin_read(getDeviceHandle(device), &value);
    checkJavacallFailure(env, result);
    return (jint)value;
}

/*
 * Reads data from a port.
 * Class:     com_oracle_dio_gpio_impl_GPIOPortImpl
 * Method:    readPort0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_gpio_impl_GPIOPortImpl_readPort0
  (JNIEnv* env, jobject obj) {
    javacall_int32 value = 0;
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result;
    result = javacall_gpio_port_read(getDeviceHandle(device), &value);
    checkJavacallFailure(env, result);
    return (jint)value;
}

/*
 * Writes data to a pin.
 * Class:     com_oracle_dio_gpio_impl_GPIOPinImpl
 * Method:    writePin0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_gpio_impl_GPIOPinImpl_writePin0
  (JNIEnv* env, jobject obj, jboolean value) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result;
    result = javacall_gpio_pin_write(getDeviceHandle(device),
                                     value != JNI_FALSE ? JAVACALL_TRUE : JAVACALL_FALSE);
    checkJavacallFailure(env, result);
}

/*
 * Writes data to a port.
 * Class:     com_oracle_dio_gpio_impl_GPIOPortImpl
 * Method:    writePort0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_gpio_impl_GPIOPortImpl_writePort0
  (JNIEnv* env, jobject obj, jint value) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result;
    result = javacall_gpio_port_write(getDeviceHandle(device), value);
    checkJavacallFailure(env, result);
}

/*
 * Starts notifications from a pin.
 * Class:     com_oracle_dio_gpio_impl_GPIOPinImpl
 * Method:    startNoti0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_gpio_impl_GPIOPinImpl_startNoti0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result;
    result = javacall_gpio_pin_notification_start(getDeviceHandle(device));
    if (env->ExceptionCheck() != JNI_TRUE) {
        checkJavacallFailure(env, result);
    }
}

/*
 * Starts notifications from a port.
 * Class:     com_oracle_dio_gpio_impl_GPIOPortImpl
 * Method:    startNoti0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_gpio_impl_GPIOPortImpl_startNoti0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result;
    result = javacall_gpio_port_notification_start(getDeviceHandle(device));
    if (env->ExceptionCheck() != JNI_TRUE) {
       checkJavacallFailure(env, result);
    }
}

/*
 * Stops notifications from a pin.
 * Class:     com_oracle_dio_gpio_impl_GPIOPinImpl
 * Method:    stopNoti0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_gpio_impl_GPIOPinImpl_stopNoti0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result;
    result = javacall_gpio_pin_notification_stop(getDeviceHandle(device));
    checkJavacallFailure(env, result);
}

/*
 * Stops notifications from a port.
 * Class:     com_oracle_dio_gpio_impl_GPIOPortImpl
 * Method:    stopNoti0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_gpio_impl_GPIOPortImpl_stopNoti0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result;
    result = javacall_gpio_port_notification_stop(getDeviceHandle(device));
    checkJavacallFailure(env, result);
}

/*
 * Sets direction of a pin.
 * Class:     com_oracle_dio_gpio_impl_GPIOPinImpl
 * Method:    setOutputMode0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_gpio_impl_GPIOPinImpl_setOutputMode0
  (JNIEnv* env, jobject obj, jboolean output) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result;
    result = javacall_gpio_pin_direction_set(getDeviceHandle(device),
                                             output != JNI_FALSE ? JAVACALL_TRUE : JAVACALL_FALSE);
    checkJavacallFailure(env, result);
}

/*
 * Sets direction of a port.
 * Class:     com_oracle_dio_gpio_impl_GPIOPortImpl
 * Method:    setOutputMode0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_gpio_impl_GPIOPortImpl_setOutputMode0
  (JNIEnv* env, jobject obj, jboolean output) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result;
    result = javacall_gpio_port_direction_set(getDeviceHandle(device),
                                              output != JNI_FALSE ? JAVACALL_TRUE : JAVACALL_FALSE);
    checkJavacallFailure(env, result);
}

/*
 * Gets direction of a pin.
 * Class:     com_oracle_dio_gpio_impl_GPIOPinImpl
 * Method:    getOutputMode0
 */
JNIEXPORT jboolean JNICALL Java_com_oracle_dio_gpio_impl_GPIOPinImpl_getOutputMode0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_bool mode = JAVACALL_FALSE;
    javacall_dio_result result;
    result = javacall_gpio_pin_direction_get(getDeviceHandle(device), &mode);
    checkJavacallFailure(env, result);
    return mode != JAVACALL_FALSE ? JNI_TRUE : JNI_FALSE;
}

/*
 * Gets direction of a port.
 * Class:     com_oracle_dio_gpio_impl_GPIOPortImpl
 * Method:    getOutputMode0
 */
JNIEXPORT jboolean JNICALL Java_com_oracle_dio_gpio_impl_GPIOPortImpl_getOutputMode0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_bool mode = JAVACALL_FALSE;
    javacall_dio_result result;
    result = javacall_gpio_port_direction_get(getDeviceHandle(device), &mode);
    checkJavacallFailure(env, result);
    return mode != JAVACALL_FALSE ? JNI_TRUE : JNI_FALSE;
}

/*
 * Sets trigger mode of a pin.
 * Class:     com_oracle_dio_gpio_impl_GPIOPinImpl
 * Method:    setTrigger0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_gpio_impl_GPIOPinImpl_setTrigger0
  (JNIEnv* env, jobject obj, jint trigger) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result;
    result = javacall_gpio_pin_set_trigger(getDeviceHandle(device),
                                           (javacall_gpio_trigger_mode)trigger);
    checkJavacallFailure(env, result);
}

/*
 * Gets trigger mode of a pin.
 * Class:     com_oracle_dio_gpio_impl_GPIOPinImpl
 * Method:    getTrigger0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_gpio_impl_GPIOPinImpl_getTrigger0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_gpio_trigger_mode trigger;
    javacall_dio_result result;
    result = javacall_gpio_pin_get_trigger(getDeviceHandle(device), &trigger);
    checkJavacallFailure(env, result);
    return (jint)trigger;
}

/*
 * Gets handle of a port the pin relates to.
 * Class:     com_oracle_dio_gpio_impl_GPIOPinImpl
 * Method:    getPortId0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_gpio_impl_GPIOPinImpl_getGrpID0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_int32 port;
    javacall_dio_result result;
    result = javacall_gpio_pin_get_group_id(getDeviceHandle(device), &port);
    checkJavacallFailure(env, result);
    return (jint)port;
}

/*
 * Get Max value of a GPIO port.
 * Class:     com_oracle_dio_gpio_impl_GPIOPortImpl
 * Method:    getMaxVal0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_gpio_impl_GPIOPortImpl_getMaxVal0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_int32 value = 0;
    javacall_dio_result result;
    result = javacall_gpio_port_get_max_value(getDeviceHandle(device), &value);
    checkJavacallFailure(env, result);
    return (jint)value;
}

/*
 * Sets the GPIOPortConfig.pins private field.
 * Class:     com_oracle_dio_gpio_impl_GPIOPortImpl
 * Method:    assignPins0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_gpio_impl_GPIOPortImpl_assignPins0
  (JNIEnv* env, jobject obj, jobject cfg, jobjectArray pins) {
    jclass cfgClass = env->GetObjectClass(cfg);
    jfieldID pinsField = cfgClass ? env->GetFieldID(cfgClass, "pins", "[Lcom/oracle/dio/gpio/impl/GPIOPinFake;") :
                                    NULL;
    if (pinsField) {
        env->SetObjectField(cfg, pinsField, pins);
    }
}


/* Entities required for sending GPIO pin notifications */
static jobject pinEventBuffer = NULL;
static jclass pinEventClass = NULL;

/*
 * Sets references to the Java entities required for sending notifications.
 * Class:     com_oracle_dio_gpio_impl_GPIOPinEventHandler
 * Method:    setNativeEntries
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_gpio_impl_GPIOPinEventHandler_setNativeEntries
  (JNIEnv* env, jclass clazz, jobject buffer, jclass event) {
    if (pinEventBuffer) {
        env->DeleteGlobalRef(pinEventBuffer);
    }
    pinEventBuffer = env->NewGlobalRef(buffer);
    if (pinEventClass) {
        env->DeleteGlobalRef(pinEventClass);
    }
    pinEventClass = (jclass)env->NewGlobalRef(event);
}

/* Entities required for sending GPIO port notifications */
static jobject portEventBuffer = NULL;
static jclass portEventClass = NULL;

/*
 * Sets references to the Java entities required for sending notifications.
 * Class:     com_oracle_dio_gpio_impl_GPIOPinEventHandler
 * Method:    setNativeEntries
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_gpio_impl_GPIOPortEventHandler_setNativeEntries
  (JNIEnv* env, jclass clazz, jobject buffer, jclass event) {
    if (portEventBuffer) {
        env->DeleteGlobalRef(portEventBuffer);
    }
    portEventBuffer = env->NewGlobalRef(buffer);
    if (portEventClass) {
        env->DeleteGlobalRef(portEventClass);
    }
    portEventClass = (jclass)env->NewGlobalRef(event);
}

/*
 * Sends a pin value change event.
 * @param handle    open pin handle
 * @param value     value of the given pin
 */
void javanotify_gpio_pin_value_changed
  (const javacall_handle handle, const javacall_int32 value) {
    if (pinEventBuffer == NULL || pinEventClass == NULL) {
        return;
    }
    device_reference device = getDeviceReference(handle);
    if (device == INVALID_DEVICE_REFERENCE) {
        return;
    }
    const int size = 8; // reserve 4 bytes for the handle and 4 bytes for the value
    char payload[size];
    payload[0] = (long)device >> 24, payload[1] = (long)device >> 16;
    payload[2] = (long)device >> 8,  payload[3] = (long)device;
    payload[4] = value >> 24, payload[5] = value >> 16;
    payload[6] = value >> 8,  payload[7] = value;
    JavaVM* vm = getGlobalJavaVM();
    event_queue_put_native_event(vm, pinEventBuffer, pinEventClass, payload, size);
}

/*
 * Sends a port value change event.
 * @param handle    open port handle
 * @param value     value of the given port
 */
void javanotify_gpio_port_value_changed
  (const javacall_handle handle, const javacall_int32 value) {
    if (portEventBuffer == NULL || portEventClass == NULL) {
        return;
    }
    device_reference device = getDeviceReference(handle);
    if (device == INVALID_DEVICE_REFERENCE) {
        return;
    }
    const int size = 8; // reserve 4 bytes for the handle and 4 bytes for the value
    char payload[size];
    payload[0] = (long)device >> 24, payload[1] = (long)device >> 16;
    payload[2] = (long)device >> 8,  payload[3] = (long)device;
    payload[4] = value >> 24, payload[5] = value >> 16;
    payload[6] = value >> 8,  payload[7] = value;
    JavaVM* vm = getGlobalJavaVM();
    event_queue_put_native_event(vm, portEventBuffer, portEventClass, payload, size);
}

} // extern "C"
