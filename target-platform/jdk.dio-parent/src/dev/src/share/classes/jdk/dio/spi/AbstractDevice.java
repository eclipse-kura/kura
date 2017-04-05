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

package jdk.dio.spi;

import jdk.dio.ClosedDeviceException;
import jdk.dio.Device;
import jdk.dio.DeviceDescriptor;
import jdk.dio.UnavailableDeviceException;
import java.io.IOException;

/**
 * The {@code AbstractDevice} class is the base implementation for
 * {@code Device}-implementing classes instantiated by {@link DeviceProvider}.
 * <br />
 * This class encapsulates the low-level machinery required to implement
 * platform-wide locking for exclusive access. It also provides methods that
 * allow for retrieving the {@link DeviceDescriptor} of {@code Device} instances.
 *
 * @param <P> the device type the descriptor is defined for.
 * @since 1.0
 */
public abstract class AbstractDevice<P extends Device<? super P>> implements Device<P> {

    /**
     * Attempts to lock for (platform-wide) exclusive access the underlying
     * device resource. This method will block until the designated
     * device resource becomes available for exclusive access or the
     * specified amount of real time has elapsed. A {@code timeout} of {@code 0}
     * means to wait forever.
     * <p />
     * This method returns silently if the underlying device resource
     * is currently acquired for exclusive access.
     *
     * @param timeout the timeout in milliseconds.
     * @throws IllegalArgumentException if {@code timeout} is negative.
     * @throws UnavailableDeviceException if this device is not
     * currently available.
     * @throws ClosedDeviceException if this device has been closed.
     * @throws IOException if any other I/O error occurred.
     */
    @Override
    public void tryLock(int timeout) throws UnavailableDeviceException, ClosedDeviceException, IOException {
    }

    /**
     * Releases from (platform-wide) exclusive access the underlying device
     * device resource.
     * <p />
     * This method returns silently if the underlying device resource
     * is not currently acquired for exclusive access or has already been
     * closed.
     *
     * @throws IOException if any other I/O error occurred.
     */
    @Override
    public void unlock() throws IOException {
    }

    /**
     * Retrieves the identifying and descriptive information of this device
     * device.
     *
     * @param <U> this {@code Device} type or a subtype of it (allows for
     * subclassing {@code Device} types - see
     * {@link jdk.dio.uart.UART UART} and
     * {@link jdk.dio.uart.ModemUART ModemUART}).
     *
     * @return the {@code DeviceDescriptor} which encapsulates the
     * identifying and descriptive information of this device.
     */
    @Override
    public final <U extends P> DeviceDescriptor<U> getDescriptor() {
        return null;
    }
 }
