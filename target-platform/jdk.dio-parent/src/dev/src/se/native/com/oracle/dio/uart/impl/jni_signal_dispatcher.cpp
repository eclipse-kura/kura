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
#include <javacall_serial.h>
#include <javacall_memory.h>
#include <dio_event_queue.h>

extern "C" {

/* Entities required for sending modem signal notifications */
static jobject eventBuffer = NULL;
static jclass eventClass = NULL;

/*
 * Class      com_oracle_dio_uart_impl_ModemSignalDispatcher
 * Method:    setNativeEntries
 * Signature: (Ljava/nio/Buffer;Ljava/lang/Class;)V
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_uart_impl_ModemSignalDispatcher_setNativeEntries
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
 * Class:     com_oracle_dio_uart_impl_ModemSignalDispatcher
 * Method:    startListening0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_oracle_dio_uart_impl_ModemSignalDispatcher_startListening0
  (JNIEnv* env, jobject obj, jlong serialHandler) {
    javacall_handle handle = (javacall_handle)serialHandler;
    javacall_handle context;
    javacall_result result;
    result = javacall_serial_start_dce_signal_listening(handle,
                                                        NULL, // target is optional
                                                        &context);
    if (JAVACALL_OK != result) {
        context = JAVACALL_INVALID_HANDLE;
    }
    return (jlong)context;
}

/*
 * Class:     com_oracle_dio_uart_impl_ModemSignalDispatcher
 * Method:    stopListening0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_dio_uart_impl_ModemSignalDispatcher_stopListening0
  (JNIEnv* env, jobject obj, jlong context) {
    if ((javacall_handle)context != JAVACALL_INVALID_HANDLE) {
        javacall_serial_stop_dce_signal_listening((javacall_handle)(jlong)context);
    }
}

/**
 * A callback function to be called for notification of DTE/DCE
 * signal status
 * @param port      serial port handle
 * @param target    target to receive signal (optional)
 * @param signal    signal type
 * @param value     signal value
 */
void javanotify_serial_signal
  (javacall_handle port, javacall_handle target, javacall_serial_signal_type signal, javacall_bool value) {
    if (eventBuffer == NULL || eventClass == NULL) {
        return;
    }

    device_reference device = getDeviceReference(port);
    if (device == INVALID_DEVICE_REFERENCE) {
        return;
    }

    // reserve 4 bytes for the port handler, 4 bytes for the signal line,
    // and 1 byte for the value
    const int size = 9;
    char payload[size];

    payload[0] = (long)device >> 24, payload[1] = (long)device >> 16;
    payload[2] = (long)device >> 8,  payload[3] = (long)device >> 0;
    payload[4] = (long)signal >> 24, payload[5] = (long)signal >> 16;
    payload[6] = (long)signal >> 8,  payload[7] = (long)signal >> 0;
    payload[8] = (value != JAVACALL_FALSE ? 1 : 0);

    JavaVM* vm = getGlobalJavaVM();
    event_queue_put_native_event(vm, eventBuffer, eventClass, payload, size);
}

} // extern "C"
