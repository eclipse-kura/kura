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

#ifndef __DEVICEACCESS_COMMON_H
#define __DEVICEACCESS_COMMON_H

#include <jni.h>
#include <javacall_defs.h>
#include <javacall_dio.h>

/* Returns a global reference to VM */
JavaVM* getGlobalJavaVM();

/* A device reference abstraction */
typedef struct _device_reference* device_reference;

/* Invalid reference constant */
#define INVALID_DEVICE_REFERENCE (device_reference)-1

/* Device handle operation prototypes */
typedef javacall_dio_result (*device_closer)(javacall_handle);
typedef javacall_dio_result (*device_locker)(const javacall_handle, javacall_handle* const);
typedef javacall_dio_result (*device_unlocker)(const javacall_handle);

typedef enum {
    SEND_SIGNAL,
    RECV_SIGNAL,
    COMM_OPEN_SIGNAL,
    COMM_CLOSE_SIGNAL,
    COMM_READ_SIGNAL,
    COMM_WRITE_SIGNAL
} signal_type;

/* Creates a reference to the device identified by handle */
device_reference createDeviceReference(javacall_handle handle, device_closer closer,
                                       device_locker locker, device_unlocker unlocker);

/* Saves the reference to an instance of AbstractPeripheral */
javacall_dio_result saveDeviceReferenceToDeviceObject(JNIEnv* env, jobject obj,
                                                      device_reference device);

/* Returns a reference stored by an instance of AbstractPeripheral */
device_reference getDeviceReferenceFromDeviceObject(JNIEnv* env, jobject obj);

/* Returns the handle associated with the device reference */
javacall_handle getDeviceHandle(device_reference device);

/* Restricted use only.
 * Retains the device reference by incrementing its reference counter, thus
 * preventing it from being immediately destroyed by releaseDeviceReference().
 * Must be balanced with a call to releaseDeviceReference() to let the
 * reference be finally destroyed. */
void retainDeviceReference(device_reference device);

/* Releases a reference */
void releaseDeviceReference(device_reference device);

/* Returns an exisiting reference to the device identified by handle. */
device_reference getDeviceReference(javacall_handle handle);

/* Blocks the current thread, until a signal is received, or the timeout expires */
javacall_dio_result waitForSignal(signal_type signalType, javacall_handle signalTarget,
                                  /*OUT*/ javacall_handle* signalParameter, long timeout);

/* Unblocks a thread that is waiting for a signal */
void generateSignal(signal_type signalType, javacall_handle signalTarget,
                    javacall_handle signalParameter);

/* Closes the referenced device */
javacall_dio_result closeDevice(device_reference device);

/* Locks the referenced device */
javacall_dio_result lockDevice(device_reference device, /*OUT*/ javacall_handle* owner);

/* Unlocks the referenced device */
javacall_dio_result unlockDevice(device_reference device);

#endif /*  __DEVICEACCESS_COMMON_H */
