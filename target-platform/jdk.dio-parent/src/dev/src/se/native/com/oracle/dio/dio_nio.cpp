/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

#include <dio_nio.h>

bool getByteBufferInformation(JNIEnv* env, jobject buffer, jbyte** directArray,
  jbyteArray* heapArray, jint* offset, jint* position, jint* capacity,
  jint* limit, jboolean* readonly) {

    // For all the refs below there might be created cached global
    // refs in order to improve performance. Do that atomically to avoid dups.
    // Assumed the buffer is already checked to be non-null.

    jclass bufferClass = env->FindClass("java/nio/Buffer");
    if (bufferClass == NULL) {
        return false;
    }
    jfieldID directBufferAddressField = env->GetFieldID(bufferClass, "address", "J");
    if (directBufferAddressField == NULL) {
        return false;
    }
    jfieldID bufferPositionField = env->GetFieldID(bufferClass, "position", "I");
    if (bufferPositionField == NULL) {
        return false;
    }
    jfieldID bufferCapacityField = env->GetFieldID(bufferClass, "capacity", "I");
    if (bufferCapacityField == NULL) {
        return false;
    }
    jfieldID bufferLimitField = env->GetFieldID(bufferClass, "limit", "I");
    if (bufferLimitField == NULL) {
        return false;
    }
    jclass byteBufferClass = env->FindClass("java/nio/ByteBuffer");
    if (byteBufferClass == NULL) {
        return false;
    }
    jfieldID byteBufferHeapBufferField = env->GetFieldID(byteBufferClass, "hb", "[B");
    if (byteBufferHeapBufferField == NULL) {
        return false;
    }
    jfieldID byteBufferOffsetField = env->GetFieldID(byteBufferClass, "offset", "I");
    if (byteBufferOffsetField == NULL) {
        return false;
    }
    jfieldID byteBufferReadonlyField = env->GetFieldID(byteBufferClass, "isReadOnly", "Z");
    if (byteBufferReadonlyField == NULL) {
        return false;
    }
    jclass directByteBufferClass = env->FindClass("java/nio/DirectByteBuffer");
    if (directByteBufferClass == NULL) {
        return false;
    }
    if (env->IsInstanceOf(buffer, directByteBufferClass) != JNI_FALSE) {
        // the buffer is a direct byte buffer
        *directArray = (jbyte*)env->GetLongField(buffer, directBufferAddressField);
        *heapArray   = NULL;
        *offset      = 0;
        *readonly    = JNI_FALSE;
    } else {
        // the buffer is a heap byte buffer
        *directArray = NULL;
        *heapArray   = (jbyteArray)env->GetObjectField(buffer, byteBufferHeapBufferField);
        *offset      = env->GetIntField(buffer, byteBufferOffsetField);
        *readonly    = env->GetBooleanField(buffer, byteBufferReadonlyField);
    }
    *position = env->GetIntField(buffer, bufferPositionField);
    *capacity = env->GetIntField(buffer, bufferCapacityField);
    *limit    = env->GetIntField(buffer, bufferLimitField);
    return true;
}

bool setByteBufferPosition(JNIEnv* env, jobject buffer, jint position) {
    jclass bufferClass = env->FindClass("java/nio/Buffer");
    if (bufferClass == NULL) {
        return false;
    }
    jfieldID bufferPositionField = env->GetFieldID(bufferClass, "position", "I");
    if (bufferPositionField == NULL) {
        return false;
    }
    env->SetIntField(buffer, bufferPositionField, position);
    return true;
}
