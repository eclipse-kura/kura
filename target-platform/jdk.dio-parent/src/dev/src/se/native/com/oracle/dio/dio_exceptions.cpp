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

#include <dio_exceptions.h>

void throwException(JNIEnv* env, const char* exception, const char* message /*= NULL*/) {
    jclass clazz = env->FindClass(exception);
    if (clazz != NULL) {
        env->ThrowNew(clazz, message);
        env->DeleteLocalRef(clazz);
    }
}

void throwDeviceNotFoundException(JNIEnv* env, const char* message /*= NULL*/) {
    throwException(env, "jdk/dio/DeviceNotFoundException", message);
}

void throwUnavailableDeviceException(JNIEnv* env, const char* message /*= NULL*/) {
    throwException(env, "jdk/dio/UnavailableDeviceException", message);
}

void throwInvalidDeviceConfigException(JNIEnv* env, const char* message /*= NULL*/) {
    throwException(env, "jdk/dio/InvalidDeviceConfigException", message);
}

void throwUnsupportedAccessModeException(JNIEnv* env, const char* message /*= NULL*/) {
    throwException(env, "jdk/dio/UnsupportedAccessModeException", message);
}

void throwUnsupportedOperationException(JNIEnv* env, const char* message /*= NULL*/) {
    throwException(env, "java/lang/UnsupportedOperationException", message);
}

void throwIllegalStateException(JNIEnv* env, const char* message /*= NULL*/) {
    throwException(env, "java/lang/IllegalStateException", message);
}

void throwOutOfMemoryError(JNIEnv* env, const char* message /*= NULL*/) {
    throwException(env, "java/lang/OutOfMemoryError", message);
}

void throwIOException(JNIEnv* env, const char* message /*= NULL*/) {
    throwException(env, "java/io/IOException", message);
}

void throwIllegalArgumentException(JNIEnv* env, const char* message /*= NULL*/) {
    throwException(env, "java/lang/IllegalArgumentException", message);
}

void throwRuntimeException(JNIEnv* env, const char* message /*= NULL*/) {
    throwException(env, "java/lang/RuntimeException", message);
}

void throwClosedDeviceException(JNIEnv* env, const char* message /*= NULL*/) {
    throwException(env, "jdk/dio/ClosedDeviceException", message);
}

/**
 * Check for a javacall failure and throw a proper exception.
 */
void checkJavacallFailure(JNIEnv* env, javacall_dio_result result) {
    if (JAVACALL_DIO_OK == result){
        return;
    }

    switch (result) {
    case JAVACALL_DIO_BUSY:
        throwUnavailableDeviceException(env, "Device is not available");
        break;
    case JAVACALL_DIO_NOT_FOUND:
        throwDeviceNotFoundException(env, "Peripheral is not found");
        break;
    case JAVACALL_DIO_OUT_OF_MEMORY:
        throwOutOfMemoryError(env, "Out of Memory");
        break;
    case JAVACALL_DIO_INVALID_CONFIG:
        throwInvalidDeviceConfigException(env, "Invalid device configuration");
        break;
    case JAVACALL_DIO_UNSUPPORTED_ACCESS_MODE:
        throwUnsupportedAccessModeException(env, "Illegal access mode");
        break;
    case JAVACALL_DIO_UNSUPPORTED_OPERATION:
        throwUnsupportedOperationException(env, "Illegal operation");
        break;
    case JAVACALL_DIO_CLOSED:
        throwClosedDeviceException(env, "Device is closed");
        break;
    case JAVACALL_DIO_INVALID_STATE:
        throwIllegalStateException(env, "Illegal state");
        break;
    case JAVACALL_DIO_FAIL:
    default:
        throwIOException(env);
    }
}

