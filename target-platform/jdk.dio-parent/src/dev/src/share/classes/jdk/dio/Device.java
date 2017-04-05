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

package jdk.dio;

import java.io.IOException;
import java.nio.channels.Channel;

/**
 * The {@code Device} interface represents devices in the system. This interface
 * provides generic methods for handling devices. <br />
 * All <i>devices</i> must implement the {@code Device} interface.
 * <p />
 * When a device is open in shared mode then access synchronization may be performed by
 * invoking {@link #tryLock tryLock} and {@link #unlock unlock}. Device locks are held on a per
 * {@code Device} instance basis. When the same device is open twice in shared access
 * mode by the same application, locking one of the {@code Device} instances will prevent the
 * other form being accessed/used.
 *
 * @param <P>
 *            the device type the descriptor is defined for.
 * @since 1.0
 */
public interface Device<P extends Device<? super P>> extends Channel {

    /**
     * Big-endian byte or bit ordering.
     */
    int BIG_ENDIAN = 1;
    /**
     * Little-endian byte or bit ordering.
     */
    int LITTLE_ENDIAN = 0;
    /**
     * Mixed-endian (non-standard) byte ordering.
     */
    int MIXED_ENDIAN = 2;

    /**
     * Attempts to lock for (platform-wide) exclusive access the underlying device
     * resource. This method will block until the designated device resource becomes
     * available for exclusive access by this {@code Device} instance or the specified amount of
     * real time has elapsed. A {@code timeout} of {@code 0} means to wait forever.
     * <p />
     * This method returns silently if the underlying device resource is open in
     * exclusive access mode or is already acquired for exclusive access (locked).
     *
     * @param timeout
     *            the timeout in milliseconds.
     * @throws IllegalArgumentException
     *             if {@code timeout} is negative.
     * @throws UnavailableDeviceException
     *             if this device is not currently available.
     * @throws ClosedDeviceException
     *             if this device has been closed.
     * @throws IOException
     *             if any other I/O error occurred.
     */
    void tryLock(int timeout) throws UnavailableDeviceException, ClosedDeviceException, IOException;

    /**
     * Closes this device, relinquishing the underlying device resource and
     * making it available to other applications. Upon closing the underlying device
     * resource MUST be set to the state (power state and configuration) it was in prior to opening
     * it.
     * <p />
     * Once closed, subsequent operations on that very same {@code Device} instance will result
     * in a {@link ClosedDeviceException} being thrown.
     * <p />
     * This method has no effects if the device has already been closed.
     *
     * @throws IOException
     *             if an I/O error occurs.
     */
    @Override
    void close() throws IOException;

    /**
     * Indicates whether this device is open/available to the calling application.
     *
     * @return {@code true} if, and only if, this device is open; {@code false} otherwise.
     */
    @Override
    boolean isOpen();

    /**
     * Releases from (platform-wide) exclusive access the underlying device resource.
     * <p />
     * This method returns silently if the underlying device resource is either open in
     * exclusive access mode or is not currently acquired for exclusive access (locked) or has
     * already been closed.
     *
     * @throws IOException
     *             if any other I/O error occurred.
     */
    void unlock() throws IOException;

    /**
     * Retrieves the identifying and descriptive information of this device.
     *
     * @param <U>
     *            this {@code Device} type or a subtype of it (allows for subclassing
     *            {@code Device} types - see {@link jdk.dio.uart.UART UART} and
     *            {@link jdk.dio.uart.ModemUART ModemUART}).
     * @return the {@code DeviceDescriptor} which encapsulates the identifying and descriptive
     *         information of this device.
     */
    <U extends P> DeviceDescriptor<U> getDescriptor();
}
