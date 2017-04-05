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

#ifndef __DEVICEACCESS_EXCEPTIONS_H
#define __DEVICEACCESS_EXCEPTIONS_H

#include <jni.h>
#include <javacall_defs.h>
#include <javacall_dio.h>

void throwException(JNIEnv* env, const char* exception, const char* message = NULL);

void throwDeviceNotFoundException(JNIEnv* env, const char* message = NULL);
void throwUnavailableDeviceException(JNIEnv* env, const char* message = NULL);
void throwInvalidDeviceConfigException(JNIEnv* env, const char* message = NULL);
void throwUnsupportedAccessModeException(JNIEnv* env, const char* message = NULL);
void throwUnsupportedOperationException(JNIEnv* env, const char* message = NULL);
void throwIllegalStateException(JNIEnv* env, const char* message = NULL);
void throwOutOfMemoryError(JNIEnv* env, const char* message = NULL);
void throwIOException(JNIEnv* env, const char* message = NULL);
void throwIllegalArgumentException(JNIEnv* env, const char* message = NULL);
void throwRuntimeException(JNIEnv* env, const char* message = NULL);
void throwClosedDeviceException(JNIEnv* env, const char* message = NULL);

/**
 * Check for a javacall failure and throw a proper exception.
 */
void checkJavacallFailure(JNIEnv* env, javacall_dio_result result);

#endif /*__DEVICEACCESS_EXCEPTIONS_H*/

