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
package com.oracle.dio.impl;

import com.oracle.dio.utils.Constants;

/**
 * A device handle abstraction with basic operations.
 */
public class Handle {

    /** Device native handle */
    protected long device_reference = Constants.INVALID_HANDLE;

    /** Closes the device */
    public native void close();

    /** Tries locking the device */
    public native boolean tryLock(int timeout);

    /** Unlocks the device */
    public native void unlock();

    /** Returns the device handle */
    public final long getNativeHandle() {
        return device_reference;
    }

    /** Reports if the device is opened or not */
    public final boolean isOpen() {
        return device_reference != Constants.INVALID_HANDLE;
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Long.hashCode(device_reference);
    }
}
