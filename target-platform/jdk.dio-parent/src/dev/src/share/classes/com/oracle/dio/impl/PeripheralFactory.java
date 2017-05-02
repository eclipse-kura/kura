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

package com.oracle.dio.impl;

import java.io.IOException;

import jdk.dio.Device;
import jdk.dio.DeviceConfig;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.DeviceDescriptor;
import jdk.dio.DeviceNotFoundException;
import jdk.dio.DevicePermission;
import jdk.dio.UnavailableDeviceException;
import jdk.dio.UnsupportedAccessModeException;

/**
 *
 * @param <P>
 *            the peripheral type the provider is defined for.
 */
public interface PeripheralFactory<P extends Device<? super P>> {

    /**
     * Creates a {@link Device} instance with the specified
     * descriptor.
     * <p />
     * @note configuration and properties from provided descriptor
     *       are objects for device lookup procedure
     * @note id and name are assigned value
     *
     * @param dscr the peripheral device descriptor
     * @param mode access mode
     *
     * @return a {@link Device} instance with the specified name and properties or {@code null} if the requested
     *         properties are not supported.
     *
     * @throws UnsupportedAccessModeException
     *             if the requested access mode is not supported.
     * @throws DeviceNotFoundException
     *             if the designated peripheral is not found.
     * @throws UnavailableDeviceException
     *             if the designated peripheral is not currently available - such as when it is already open in an
     *             access mode incompatible with the requested access mode.
     * @throws UnsupportedAccessModeException
     *             if the requested access mode is not supported.
     * @throws IOException
     *             if any other I/O error occurred.
     * @throws SecurityException
     *             if the caller has no permission to access the designated peripheral.
     * @throws IllegalArgumentException
     *             if {@code id} is not greater than or equal to {@code 0}.
     */
    P create(DeviceDescriptor<P> dscr, int mode) throws DeviceNotFoundException,
        UnavailableDeviceException, UnsupportedAccessModeException, IOException,
        InvalidDeviceConfigException;
}
