/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "dio_event_queue.h"
#include <string.h>

static const int JCLASS_SIZE = sizeof(jclass);

extern "C" {

JNIEXPORT jclass JNICALL Java_com_oracle_dio_impl_EventQueue_getEventClass
  (JNIEnv* env, jclass clazz, jobject buffer, jint currPos) {
    // required methods references
    jclass bufferClass = env->GetObjectClass(buffer);

    jmethodID setPositionID = bufferClass ? env->GetMethodID(bufferClass, "position", "(I)Ljava/nio/Buffer;") :
                                            NULL;

    // Not necessary to synchronize on buffer, it's always called from protected section
    jbyte* buf = setPositionID ? (jbyte*)env->GetDirectBufferAddress(buffer) :
                                 NULL;

    jclass eventClass = NULL;
    if (buf) {
        memcpy(&eventClass, buf + currPos, JCLASS_SIZE);
        // update buffer position
        env->CallObjectMethod(buffer, setPositionID, currPos + JCLASS_SIZE);
    }

    return eventClass;
}

} /* extern "C */

/*
 * Puts an event to the provided NIO buffer and sends a notification to the
 * EventQueue object that handles the buffer. See the implementation
 * of the com.oracle.dio.impl.EventQueue class for details.
 */
void event_queue_put_native_event
  (JavaVM* cachedJVM, jobject eventBufferRef, jclass eventClass, const char *payload, int payload_size) {
    JNIEnv* env;
    cachedJVM->AttachCurrentThread((void**)&env, NULL);

    jclass bufferClass = env->GetObjectClass(eventBufferRef);
    jmethodID notifyID = bufferClass ? env->GetMethodID(bufferClass, "notify", "()V") :
                                       NULL;
    jmethodID limitID = notifyID ? env->GetMethodID(bufferClass, "limit", "()I") :
                                  NULL;
    jmethodID setLimitID = limitID ? env->GetMethodID(bufferClass, "limit", "(I)Ljava/nio/Buffer;") :
                                     NULL;

    if (setLimitID) {
        env->MonitorEnter(eventBufferRef);

        if (env->ExceptionCheck() != JNI_TRUE) {
            // check enough space in direct buffer
            jlong capacity = env->GetDirectBufferCapacity(eventBufferRef);
            jint limit = env->CallIntMethod(eventBufferRef, limitID);

            jint newLimit = limit + JCLASS_SIZE + payload_size + 2;

            if (newLimit < capacity) {
                jbyte* buf = (jbyte*)env->GetDirectBufferAddress(eventBufferRef);

                buf += limit;

                memcpy(buf, &eventClass, JCLASS_SIZE);
                buf += JCLASS_SIZE;

                // payload
                *buf++ = (jbyte)((payload_size & 0xFF00) >> 8); // high byte
                *buf++ = (jbyte)(payload_size & 0xFF);          // then low byte
                memcpy(buf, payload, payload_size);

                env->CallObjectMethod(eventBufferRef, setLimitID, newLimit);
                env->CallVoidMethod(eventBufferRef, notifyID);
            }

            env->MonitorExit(eventBufferRef);
        }
    }
    cachedJVM->DetachCurrentThread();
}
