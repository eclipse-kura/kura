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
#include <javacall_spi.h>
#include <javacall_memory.h>

extern "C" {

static javacall_dio_result _spi_close_slave(javacall_handle handle) {
    javacall_spi_end(handle);
    javacall_spi_close_slave(handle);
    return JAVACALL_DIO_OK;
}

/*
 * Class:     com_oracle_dio_spibus_impl_SPISlaveImpl
 * Method:    openSPIDeviceByConfig0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_spibus_impl_SPISlaveImpl_openSPIDeviceByConfig0
  (JNIEnv* env, jobject obj, jint deviceNumber, jint address, jint csActive,
   jint clockFrequency, jint clockMode, jint wordLen, jint bitOrdering, jboolean exclusive) {
    javacall_dio_result result;
    javacall_handle handle = JAVACALL_INVALID_HANDLE;
    result = javacall_spi_open_slave_with_config(deviceNumber,
                                                 address,
                                                 (javacall_spi_cs_active)csActive,
                                                 clockFrequency,
                                                 clockMode,
                                                 wordLen,
                                                 (javacall_byteorder)bitOrdering,
                                                 (exclusive != JNI_FALSE ? JAVACALL_TRUE: JAVACALL_FALSE),
                                                 &handle);
    if (result == JAVACALL_DIO_OK) {
        device_reference device;
        device = createDeviceReference(handle, _spi_close_slave,
                                       javacall_spi_lock, javacall_spi_unlock);
        if (device == INVALID_DEVICE_REFERENCE) {
            javacall_spi_close_slave(handle);
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
 * Class:     com_oracle_dio_spibus_impl_SPISlaveImpl
 * Method:    begin0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_spibus_impl_SPISlaveImpl_begin0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    if (device != INVALID_DEVICE_REFERENCE) {
        javacall_dio_result result = javacall_spi_begin(getDeviceHandle(device));
        checkJavacallFailure(env, result);
    }
    return 0;
}

/*
 * Class:     com_oracle_dio_spibus_impl_SPISlaveImpl
 * Method:    end0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_spibus_impl_SPISlaveImpl_end0
  (JNIEnv* env, jobject obj) {
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    if (device != INVALID_DEVICE_REFERENCE) {
        javacall_dio_result result = javacall_spi_end(getDeviceHandle(device));
        checkJavacallFailure(env, result);
    }
    return 0;
}

/*
 * Class:     com_oracle_dio_spibus_impl_SPISlaveImpl
 * Method:    writeAndRead0
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_spibus_impl_SPISlaveImpl_writeAndRead0
  (JNIEnv* env, jobject obj, jobject src, jobject dst) {

    jint srcPos = 0, srcCap = 0, srcOff = 0, srcLim = 0;
    jint dstPos = 0, dstCap = 0, dstOff = 0, dstLim = 0;
    jboolean srcReadonly = JNI_TRUE, dstReadonly = JNI_TRUE;
    jbyte *srcDirectArray = NULL, *dstDirectArray = NULL;
    jbyteArray srcHeapArray = NULL, dstHeapArray = NULL;
    if (src != NULL) {
        if (!getByteBufferInformation(env, src, &srcDirectArray, &srcHeapArray,
                                      &srcOff, &srcPos, &srcCap, &srcLim,
                                      &srcReadonly)) {
            env->ExceptionClear();
            throwRuntimeException(env, "fault source buffer");
            return;
        }
    }
    if (dst != NULL) {
        if (!getByteBufferInformation(env, dst, &dstDirectArray, &dstHeapArray,
                                      &dstOff, &dstPos, &dstCap, &dstLim,
                                      &dstReadonly)) {
            env->ExceptionClear();
            throwRuntimeException(env, "fault destination buffer");
            return;
        }

        if (dstReadonly != JNI_FALSE) {
            throwIOException(env, "readonly destination buffer");
            return;
        }
    } else {
        dstLim = srcLim;
        dstPos = srcPos;
    }
    if ((srcLim - srcPos) != (dstLim - dstPos)) {
        throwIOException(env, "destination and source buffer lengths differ");
        return;
    }
    if (srcHeapArray != NULL) {
        srcDirectArray = env->GetByteArrayElements(srcHeapArray, NULL);
    }
    if (dstHeapArray != NULL) {
        dstDirectArray = env->GetByteArrayElements(dstHeapArray, NULL);
    }

    // Get synchronized with the object to avoid closing the handle by another
    // thread. See the implementation of close(). Always check the handle is valid.
    // Do not generate ClosedDeviceException until all JNI calls are made.
    javacall_dio_result result = JAVACALL_DIO_OK;
    bool throwClosedException = true;
    env->MonitorEnter(obj);
    device_reference device = INVALID_DEVICE_REFERENCE;
    if (env->ExceptionCheck() != JNI_TRUE) {
        device = getDeviceReferenceFromDeviceObject(env, obj);
        if (device != INVALID_DEVICE_REFERENCE) {
            javacall_handle handle = getDeviceHandle(device);
            if (handle != JAVACALL_INVALID_HANDLE) {
                result = javacall_spi_send_and_receive_start(
                                    handle,
                                    (const char*)(srcDirectArray + srcOff + srcPos),
                                    (char*)(dstDirectArray + dstOff + dstPos),
                                    srcLim - srcPos);
                if (result == JAVACALL_DIO_WOULD_BLOCK) {
                    retainDeviceReference(device);
                }
                throwClosedException = false;
            }
        }
        env->MonitorExit(obj);
    } else {
        result = JAVACALL_DIO_FAIL;
    }

    // If the call returns JAVACALL_WOULD_BLOCK, have to wait for a
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
                if (env->ExceptionCheck() != JNI_TRUE) {
                    device = getDeviceReferenceFromDeviceObject(env, obj);
                    if (device != INVALID_DEVICE_REFERENCE) {
                        javacall_handle handle = getDeviceHandle(device);
                        if (handle != JAVACALL_INVALID_HANDLE) {
                            result = javacall_spi_send_and_receive_finish(
                                             handle,
                                             JAVACALL_FALSE,
                                             (const char*)(srcDirectArray + srcOff + srcPos),
                                             (char*)(dstDirectArray + dstOff + dstPos),
                                             dstLim - dstPos);
                            if (result != JAVACALL_DIO_OK) {
                                // the notified status has higher priority
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

    if (srcHeapArray != NULL && srcDirectArray != NULL) {
        env->ReleaseByteArrayElements(srcHeapArray, srcDirectArray,
                                      JNI_ABORT);
    }
    if (dstHeapArray != NULL && dstDirectArray != NULL) {
        env->ReleaseByteArrayElements(dstHeapArray, dstDirectArray,
                                      result == JAVACALL_DIO_OK ? 0 : JNI_ABORT);
    }
    if (env->ExceptionCheck() != JNI_TRUE) {
        if (throwClosedException) {
            throwClosedDeviceException(env);
        } else {
            checkJavacallFailure(env, result);
        }
    }
}

/*
 * Class:     com_oracle_dio_spibus_impl_SPISlaveImpl
 * Method:    getGrpID0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_spibus_impl_SPISlaveImpl_getGrpID0
  (JNIEnv* env, jobject obj) {
    javacall_int32 grp = -1;
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    javacall_spi_get_group_id(getDeviceHandle(device), &grp);
    return (jint)grp;
}

/*
 * Class:     com_oracle_dio_spibus_impl_SPISlaveImpl
 * Method:    getWordLength0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_spibus_impl_SPISlaveImpl_getWordLength0
  (JNIEnv* env, jobject obj) {
    javacall_dio_result result;
    javacall_int32 wordSize;
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    result = javacall_spi_get_word_size(getDeviceHandle(device), &wordSize);
    checkJavacallFailure(env, result);
    return (jint)wordSize;
}

/*
 * Class:     com_oracle_dio_spibus_impl_SPISlaveImpl
 * Method:    getByteOrdering0
 */
JNIEXPORT jint JNICALL Java_com_oracle_dio_spibus_impl_SPISlaveImpl_getByteOrdering0
  (JNIEnv* env, jobject obj) {
    javacall_dio_result result;
    javacall_int32 byteOrdering;
    device_reference device = getDeviceReferenceFromDeviceObject(env, obj);
    result = javacall_spi_get_byte_ordering(getDeviceHandle(device), &byteOrdering);
    checkJavacallFailure(env, result);
    return (jint)byteOrdering;
}

/**
* See javacall_spi.h for definition
*/
void javanotify_spi_event(const javacall_handle handle, javacall_int32 result) {
    generateSignal(SEND_SIGNAL, handle, (javacall_handle)result);
}

} // extern "C"
