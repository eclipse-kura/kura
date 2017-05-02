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
#include <javacall_serial.h>
#include <javacall_uart.h>
#include <javacall_memory.h>
#include <dio_event_queue.h>

extern "C" {

/* Entities required for sending uart notifications */
static jobject eventBuffer = NULL;
static jclass eventClass = NULL;

/* Cleanup function */
static javacall_dio_result uart_cleanup(javacall_handle handle) {
    return javacall_uart_close_start(handle, NULL);
}

/*
 * Class      com_oracle_dio_uart_impl_UARTEventHandler
 * Method:    setNativeEntries
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_uart_impl_UARTEventHandler_setNativeEntries
  (JNIEnv* env, jclass clazz, jobject buffer, jclass event) {
    if (eventBuffer) {
        env->DeleteGlobalRef(eventBuffer);
    }
    eventBuffer = env->NewGlobalRef(buffer);
    if (eventClass) {
        env->DeleteGlobalRef(eventClass);
    }
    eventClass = (jclass)env->NewGlobalRef(event);
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    openUARTByConfig0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_openUARTByConfig0
  (JNIEnv* env, jobject obj, jbyteArray devNameArray, jint baudRate, jint stopBits,
   jint flowControl, jint bitsPerChar, jint parity, jboolean exclusive) {

    jsize devNameLen = env->GetArrayLength(devNameArray);
    char* devName = (char*)javacall_malloc(devNameLen + 1);
    if (devName == NULL) {
        throwOutOfMemoryError(env);
        return;
    }
    env->GetByteArrayRegion(devNameArray, 0, devNameLen, (jbyte*)devName);
    devName[devNameLen] = 0;

    javacall_handle handle = JAVACALL_INVALID_HANDLE;
    javacall_dio_result result;
    result = javacall_uart_open_start(devName, baudRate,
                                     (javacall_uart_stop_bits)stopBits,
                                     flowControl,
                                     (javacall_uart_bits_per_char)bitsPerChar,
                                     (javacall_uart_parity)parity,
                                     (exclusive != JNI_FALSE ? JAVACALL_TRUE : JAVACALL_FALSE),
                                     &handle);
    do {
        if (result == JAVACALL_DIO_OK) {
            break;
        }
        if (result != JAVACALL_DIO_WOULD_BLOCK) {
            checkJavacallFailure(env, result);
            break;
        }

        // wait for a signal, check for asynchronous exceptions, stop if any
        waitForSignal(COMM_OPEN_SIGNAL, handle, NULL, 0);
        if (env->ExceptionCheck() == JNI_TRUE) {
            break;
        }
        result = javacall_uart_open_finish(devName, baudRate,
                                           (javacall_uart_stop_bits)stopBits,
                                           flowControl,
                                           (javacall_uart_bits_per_char)bitsPerChar,
                                           (javacall_uart_parity)parity,
                                           &handle);
    } while (1);

    javacall_free(devName);

    if (result == JAVACALL_DIO_OK && env->ExceptionCheck() == JNI_FALSE) {
        device_reference device;
        device = createDeviceReference(handle, uart_cleanup, javacall_uart_lock,
                                       javacall_uart_unlock);
        if (device == INVALID_DEVICE_REFERENCE) {
            javacall_uart_close_start(handle, NULL);
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
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    setDTESignalState0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_setDTESignalState0
  (JNIEnv* env, jobject obj, jint signal, jboolean state) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_result result;
    result = javacall_serial_set_dte_signal(getDeviceHandle(device),
                                            (javacall_serial_signal_type)signal,
                                            (state != JNI_FALSE ? JAVACALL_TRUE : JAVACALL_FALSE));
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    getDCESignalState0
 */
JNIEXPORT jboolean JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_getDCESignalState0
  (JNIEnv* env, jobject obj, jint signal) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_bool state = JAVACALL_FALSE;
    javacall_result result;
    result = javacall_serial_get_dce_signal(getDeviceHandle(device),
                                            (javacall_serial_signal_type)signal,
                                            &state);
    return (state != JAVACALL_FALSE ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    removeEventListener0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_removeEventListener0
  (JNIEnv* env, jobject obj, jint event) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result;
    result = javacall_uart_stop_event_listening(getDeviceHandle(device),
                                                (javacall_uart_event_type)event);
    checkJavacallFailure(env, result);
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    setEventListener0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_setEventListener0
  (JNIEnv* env, jobject obj, jint event) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result;
    result = javacall_uart_start_event_listening(getDeviceHandle(device),
                                                 (javacall_uart_event_type)event);
    checkJavacallFailure(env, result);
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    write0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_write0
  (JNIEnv* env, jobject obj, jobject src) {

    jint pos = 0, cap = 0, off = 0, lim = 0;
    jboolean readonly = JNI_TRUE;
    jbyte *directArray = NULL;
    jbyteArray heapArray = NULL;
    if (!getByteBufferInformation(env, src, &directArray, &heapArray,
                                  &off, &pos, &cap, &lim,
                                  &readonly)) {
        env->ExceptionClear();
        throwRuntimeException(env, "fault source buffer");
        return 0;
    }
    if (heapArray != NULL) {
        directArray = env->GetByteArrayElements(heapArray, NULL);
    }

    jint bytesWritten = 0;
    javacall_dio_result result = JAVACALL_DIO_OK;
    javacall_handle context = NULL;
    bool throwClosedException = true;

    // Get synchronized with the object to avoid closing the handle by another
    // thread. See the implementation of close(). Always check the handle is valid.
    // Do not generate ClosedDeviceException until all JNI calls are made.
    env->MonitorEnter(obj);
    device_reference device = INVALID_DEVICE_REFERENCE;
    if (env->ExceptionCheck() != JNI_TRUE) {
        device = getDeviceReferenceFromDeviceObject(env, obj);
        if (device != INVALID_DEVICE_REFERENCE) {
            javacall_handle handle = getDeviceHandle(device);
            if (handle != JAVACALL_INVALID_HANDLE) {
                result = result = javacall_uart_write_start(handle,
                                                            (unsigned char*)(directArray + pos + off),
                                                            lim - pos,
                                                            &bytesWritten,
                                                            &context);
                if (result == JAVACALL_DIO_WOULD_BLOCK) {
                    retainDeviceReference(device);
                }
                throwClosedException = false;
            }
        }
        env->MonitorExit(obj);
    }

    do {
        if (result == JAVACALL_DIO_OK) {
            break;
        }
        if (result != JAVACALL_DIO_WOULD_BLOCK && env->ExceptionCheck() != JNI_TRUE) {
            throwIOException(env, "writing to port has failed");
            break;
        }

        javacall_handle status = NULL;
        result = waitForSignal(COMM_WRITE_SIGNAL, getDeviceHandle(device), &status, 0);
        releaseDeviceReference(device);
        device = INVALID_DEVICE_REFERENCE;

        if ((result != JAVACALL_DIO_OK) ||
            (result = (javacall_dio_result)(javacall_int32)status) != JAVACALL_DIO_OK ||
            (env->ExceptionCheck() == JNI_TRUE)) {
            break;
        }

        // again, get synchronized, check the handle is valid
        throwClosedException = true;
        env->MonitorEnter(obj);
        device = INVALID_DEVICE_REFERENCE;
        if (env->ExceptionCheck() != JNI_TRUE) {
            device = getDeviceReferenceFromDeviceObject(env, obj);
            if (device != INVALID_DEVICE_REFERENCE) {
                javacall_handle handle = getDeviceHandle(device);
                if (handle != JAVACALL_INVALID_HANDLE) {
                    result = result = javacall_uart_write_finish(handle,
                                                                 (unsigned char*)(directArray + pos + off),
                                                                 lim - pos,
                                                                 &bytesWritten,
                                                                 context);
                    if (result == JAVACALL_DIO_WOULD_BLOCK) {
                        retainDeviceReference(device);
                    }
                    throwClosedException = false;
                }
            }
            env->MonitorExit(obj);
        }
    } while (1);

    if (heapArray != NULL && directArray != NULL) {
        env->ReleaseByteArrayElements(heapArray, directArray, JNI_ABORT);
    }
    if (throwClosedException && env->ExceptionCheck() != JNI_TRUE) {
        throwClosedDeviceException(env);
    }
    return bytesWritten;
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    read0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_read0
  (JNIEnv* env, jobject obj, jobject dst) {

    jint pos = 0, cap = 0, off = 0, lim = 0;
    jboolean readonly = JNI_TRUE;
    jbyte *directArray = NULL;
    jbyteArray heapArray = NULL;
    if (!getByteBufferInformation(env, dst, &directArray, &heapArray,
                                  &off, &pos, &cap, &lim,
                                  &readonly)) {
        env->ExceptionClear();
        throwRuntimeException(env, "fault destination buffer");
        return 0;
    }
    if (readonly != JNI_FALSE) {
        throwIOException(env, "readonly destination buffer");
        return 0;
    }
    if (heapArray != NULL) {
        directArray = env->GetByteArrayElements(heapArray, NULL);
    }

    jint bytesRead = 0;
    javacall_dio_result result = JAVACALL_DIO_OK;
    javacall_handle context = NULL;
    bool throwClosedException = true;

    // Get synchronized with the object to avoid closing the handle by another
    // thread. See the implementation of close(). Always check the handle is valid.
    // Do not generate ClosedDeviceException until all JNI calls are made.
    env->MonitorEnter(obj);
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    if (device != INVALID_DEVICE_REFERENCE) {
        javacall_handle handle = getDeviceHandle(device);
        if (handle != JAVACALL_INVALID_HANDLE) {
            result = javacall_uart_read_start(handle,
                                              (unsigned char*)(directArray + pos + off),
                                              lim - pos,
                                              &bytesRead,
                                              &context);
            if (result == JAVACALL_DIO_WOULD_BLOCK) {
                retainDeviceReference(device);
            }
            throwClosedException = false;
        }
    }
    env->MonitorExit(obj);

    do {
        if (result == JAVACALL_DIO_OK) {
            break;
        } else if (result != JAVACALL_DIO_WOULD_BLOCK) {
            throwIOException(env, "reading from port has failed");
            break;
        }

        javacall_handle status = NULL;
        result = waitForSignal(COMM_READ_SIGNAL, getDeviceHandle(device), &status, 0);
        releaseDeviceReference(device);
        device = NULL;

        if ((result != JAVACALL_DIO_OK) ||
            (result = (javacall_dio_result)(javacall_int32)status) != JAVACALL_DIO_OK ||
            (env->ExceptionCheck() == JNI_TRUE)) {
            break;
        }

        // again, get synchronized, check the handle is valid
        throwClosedException = true;
        env->MonitorEnter(obj);
        device = getDeviceReferenceFromDeviceObject(env, obj);
        if (device != INVALID_DEVICE_REFERENCE) {
            javacall_handle handle = getDeviceHandle(device);
            if (handle != JAVACALL_INVALID_HANDLE) {
                result = javacall_uart_read_finish(handle,
                                                   (unsigned char*)(directArray + pos + off),
                                                   lim - pos,
                                                   &bytesRead,
                                                   context);
                if (result == JAVACALL_DIO_WOULD_BLOCK) {
                    retainDeviceReference(device);
                }
                throwClosedException = false;
            }
        }
        env->MonitorExit(obj);
    } while (1);

    if (heapArray != NULL && directArray != NULL) {
        env->ReleaseByteArrayElements(heapArray, directArray,
                                      result == JAVACALL_DIO_OK ? 0 : JNI_ABORT);
    }
    if (throwClosedException) {
        throwClosedDeviceException(env);
    }
    return bytesRead;

}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    getBaudRate0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_getBaudRate0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    jint baudRate = 0;
    if (JAVACALL_FAIL == javacall_serial_get_baudRate(getDeviceHandle(device), &baudRate)) {
        checkJavacallFailure(env, JAVACALL_DIO_FAIL);
    }
    return baudRate;
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    setBaudRate0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_setBaudRate0
  (JNIEnv* env, jobject obj, jint baudRate) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    if (JAVACALL_FAIL == javacall_serial_set_baudRate(getDeviceHandle(device), baudRate)) {
        checkJavacallFailure(env, JAVACALL_DIO_UNSUPPORTED_OPERATION);
    }
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    getDataBits0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_getDataBits0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_uart_bits_per_char dataBits;
    javacall_dio_result result = javacall_uart_get_bits_per_char(getDeviceHandle(device), &dataBits);
    checkJavacallFailure(env, result);
    return (jint)dataBits;
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    setDataBits0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_setDataBits0
  (JNIEnv* env, jobject obj, jint dataBits) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result;
    result = javacall_uart_set_bits_per_char(getDeviceHandle(device),
                                            (javacall_uart_bits_per_char)dataBits);
    checkJavacallFailure(env, result);
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    getParity0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_getParity0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_uart_parity parity;
    javacall_dio_result result = javacall_uart_get_parity(getDeviceHandle(device), &parity);
    checkJavacallFailure(env, result);
    return (jint)parity;
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    setParity0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_setParity0
  (JNIEnv* env, jobject obj, jint parity) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result = javacall_uart_set_parity(getDeviceHandle(device),
                                                          (javacall_uart_parity)parity);
    checkJavacallFailure(env, result);
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    getStopBits0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_getStopBits0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_uart_stop_bits stopBits;
    javacall_dio_result result = javacall_uart_get_stop_bits(getDeviceHandle(device),
                                                             &stopBits);
    checkJavacallFailure(env, result);
    return (jint)stopBits;
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    setStopBits0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_setStopBits0
  (JNIEnv* env, jobject obj, jint stopBits) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result = javacall_uart_set_stop_bits(getDeviceHandle(device),
                                        (javacall_uart_stop_bits)stopBits);
    checkJavacallFailure(env, result);
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    stopWriting0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_stopWriting0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result = javacall_uart_stop_writing(getDeviceHandle(device));
    checkJavacallFailure(env, result);
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    stopReading0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_stopReading0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_dio_result result = javacall_uart_stop_reading(getDeviceHandle(device));
    checkJavacallFailure(env, result);
}

/*
 * Class:     com_oracle_dio_uart_impl_UARTImpl
 * Method:    getUartId0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_uart_impl_UARTImpl_getUartId0
  (JNIEnv* env, jobject obj) {
    javacall_int32 grp = -1;
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_uart_get_group_id(getDeviceHandle(device), &grp);
    return (jint)grp;
}

/**
 * A callback function to be called for notification of uart events.
 *
 * @param port      serial port handle
 * @param type      type of event
 * @param bytes     bytes processed
 * @param result    result
 */
void javanotify_uart_event
  (javacall_uart_event_type type, javacall_handle port, javacall_int32 bytes,
   javacall_result result) {
    if (eventBuffer == NULL || eventClass == NULL) {
        return;
    }

    device_reference device = getDeviceReference(port);
    if (device == INVALID_DEVICE_REFERENCE) {
        return;
    }

    // reserve 4 bytes for the port handler, 4 bytes for event type,
    // and 4 bytes for the bytes processed value
    const int size = 12;
    char payload[size];

    payload[0] = (long)device >> 24, payload[1] = (long)device >> 16;
    payload[2] = (long)device >> 8,  payload[3] = (long)device >> 0;
    payload[4] = (long)type >> 24, payload[5] = (long)type >> 16;
    payload[6] = (long)type >> 8,  payload[7] = (long)type >> 0;
    payload[8]  = (long)bytes >> 24, payload[9]  = (long)bytes >> 16;
    payload[10] = (long)bytes >> 8,  payload[11] = (long)bytes >> 0;

    JavaVM* vm = getGlobalJavaVM();
    event_queue_put_native_event(vm, eventBuffer, eventClass, payload, size);
}

/**
 * A callback function to be called for notification of serial communication
 * related events.
 *
 * @param port      serial port handle
 * @param type      type of event
 * @param result    operation result
*/
void javanotify_serial_event
  (javacall_serial_callback_type type, javacall_handle port, javacall_result result) {
    signal_type signal;
    switch(type) {
        case JAVACALL_EVENT_SERIAL_OPEN:    signal = COMM_OPEN_SIGNAL;  break;
        case JAVACALL_EVENT_SERIAL_CLOSE:   signal = COMM_CLOSE_SIGNAL; break;
        case JAVACALL_EVENT_SERIAL_RECEIVE: signal = COMM_READ_SIGNAL;  break;
        case JAVACALL_EVENT_SERIAL_WRITE:   signal = COMM_WRITE_SIGNAL; break;
        default: return;
    }
    generateSignal(signal, port, (javacall_handle)result);
}

} // extern "C"
