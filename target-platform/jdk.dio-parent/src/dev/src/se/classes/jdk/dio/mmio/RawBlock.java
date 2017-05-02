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
package jdk.dio.mmio;

import jdk.dio.ClosedDeviceException;
import jdk.dio.UnavailableDeviceException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The {@code RawBlock} interface provides methods to access a continuous range of physical memory (raw memory) as a
 * {@link ByteBuffer}. A {@code RawBlock} instance can be obtained from a {@link MMIODevice} instance.
 * <p />
 * The {@code index} parameter of {@code ByteBuffer} <i>absolute</i> operations map to physical memory addresses. The
 * {@code index} values are relative to the base address of the raw memory area. The {@code index} value {@code 0}
 * corresponds to the base address of the raw memory area.
 * @since 1.0
 */
public interface RawBlock extends RawMemory {
    /**
     * Gets the complete memory area this {@code RawBlock} is mapped to as a <em>direct</em> {@link ByteBuffer}. The
     * byte order of the retrieved buffer will be that of this device unless the byte ordering is not standard (as
     * indicated by the value {@link MMIODevice#MIXED_ENDIAN}; in which case the buffer's byte order will be set to
     * {@link ByteOrder#BIG_ENDIAN}.
     * <p />
     * When the returned {@code ByteBuffer} instance is invalidated because the device is either closed or in
     * exclusive use by some other application then an attempt to access the {@code ByteBuffer} instance will not change
     * the buffer's content and will cause a {@code ClosedDeviceException} or some other unspecified exception to be
     * thrown either at the time of the access or at some later time.
     *
     * @return the <em>direct</em> {@link ByteBuffer} for the complete raw memory area associated with this
     *         {@code RawBlock}.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    ByteBuffer asDirectBuffer() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Returns the name assigned to this {@code RawBlock} instance.
     *
     * @return this {@code RawBlock} instance's name or {@code null} if this {@code RawBlock} instance was obtained by a
     *         call to {@link MMIODevice#getAsRawBlock MMIODevice.getAsRawBlock}.
     */
    @Override
    String getName();
}