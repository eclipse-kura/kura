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

package jdk.dio.generic;

import jdk.dio.BufferAccess;
import jdk.dio.ClosedDeviceException;
import jdk.dio.UnavailableDeviceException;
import java.io.*;
import java.nio.*;
import java.nio.channels.ByteChannel;

/**
 * The {@code GenericBufferIODevice} interface defines generic methods for accessing and controlling
 * devices using read and write operations.
 * <p />
 * A platform implementer may allow through this interface access and control of devices
 * for which there exist no other more specific API such as
 * {@link jdk.dio.spibus.SPIDevice} or
 * {@link jdk.dio.i2cbus.I2CDevice}.
 * @since 1.0
 */
public interface GenericBufferIODevice extends GenericDevice, ByteChannel, BufferAccess<ByteBuffer> {

    /**
     * Reads a sequence of bytes from this device into the given buffer.
     * <p />
     * The availability of new input data may be notified through an {@link GenericEvent}
     * with ID {@link GenericEvent#INPUT_DATA_AVAILABLE}.
     * <p />
     * {@inheritDoc }
     *
     * @param dst
     *            The buffer into which bytes are to be transferred.
     * @return The number of bytes read into {@code dst}, possibly zero, or {@code -1} if the device has reached
     *         end-of-stream
     * @throws NullPointerException
     *             If {@code dst} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if an I/O error occurred such as the device is not readable.
     */
    @Override
    int read(ByteBuffer dst) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Reads a sequence of bytes from this device into the given buffer, skipping the first
     * {@code skip} bytes read.
     * <p />
     * Apart from skipping the first {@code skip} bytes, this method behaves identically to
     * {@link #read(java.nio.ByteBuffer)}.
     *
     * @param skip
     *            the number of read bytes that must be ignored/skipped before filling in the
     *            {@code dst} buffer.
     * @param dst
     *            The buffer into which bytes are to be transferred.
     * @return The number of bytes read into {@code dst}, possibly zero, or {@code -1} if the device has reached
     *         end-of-stream
     * @throws NullPointerException
     *             If {@code dst} is {@code null}.
     * @throws IllegalArgumentException
     *             If {@code skip} is negative.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if an I/O error occurred such as the device is not readable.
     */
    int read(int skip, ByteBuffer dst) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Writes a sequence of bytes to this device from the given buffer.
     * <p />
     * An empty output buffer condition may be notified through an {@link GenericEvent}
     * with ID {@link GenericEvent#OUTPUT_BUFFER_EMPTY}.
     * <p />
     * {@inheritDoc}
     *
     * @param src
     *            The buffer from which bytes are to be retrieved.
     * @return The number of bytes written from {@code src}, possibly zero.
     * @throws NullPointerException
     *             If {@code src} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if an I/O error occurred such as the device is not writable.
     */
    @Override
    int write(ByteBuffer src) throws IOException, UnavailableDeviceException, ClosedDeviceException;
}
