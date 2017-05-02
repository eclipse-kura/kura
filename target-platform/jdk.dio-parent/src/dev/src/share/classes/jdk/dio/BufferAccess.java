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

import java.io.*;
import java.nio.*;

/**
 * The {@code BufferAccess} interface provides methods for getting access to the device
 * (or the driver thereof) I/O buffers, if any.
 *
 * @param <B>
 *            the I/O buffer type.
 * @since 1.0
 */
public interface BufferAccess<B extends Buffer> {

    /**
     * Gets the <em>direct</em> input buffer of this device <i>(optional operation)</i>.
     * <p />
     * The input buffer will get allocated on a per-application basis to avoid conflicts;
     * but only one such buffer will be allocated per application. The capacity of the buffer
     * will be determined based on the property of the underlying device.
     * <p />
     * When the returned {@code Buffer} instance is invalidated because the device is either
     * closed or in exclusive use by some other application then an attempt to access the
     * {@code Buffer} instance will not change the buffer's content and will cause a
     * {@code ClosedDeviceException} or some other unspecified exception to be thrown either at
     * the time of the access or at some later time.
     *
     * @return the direct input buffer of this device.
     * @throws UnsupportedOperationException
     *             if this device (or driver thereof) does not have or does not allow direct access
     *             to its input buffer.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if an I/O error occurred such as the device is not readable.
     */
    B getInputBuffer() throws ClosedDeviceException, IOException;

    /**
     * Gets the <em>direct</em> output buffer of this device <i>(optional operation)</i>.
     * <p />
     * The output buffer will get allocated on a per-application basis to avoid conflicts;
     * but only one such buffer will be allocated per application. The capacity of the buffer
     * will be determined based on the property of the underlying device.
     * <p />
     * When the returned {@code Buffer} instance is invalidated because the device is either
     * closed or in exclusive use by some other application then an attempt to access the
     * {@code Buffer} instance will not change the buffer's content and will cause a
     * {@code ClosedDeviceException} or some other unspecified exception to be thrown either at
     * the time of the access or at some later time.
     *
     * @return the direct output buffer of this device.
     * @throws UnsupportedOperationException
     *             if this device (or driver thereof) does not have or does not allow direct access
     *             to its output buffer.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if an I/O error occurred such as the device is not writable.
     */
    B getOutputBuffer() throws ClosedDeviceException, IOException;
}
