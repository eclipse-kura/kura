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

/**
 * The {@code Transactional} interface provides methods for demarcating a communication transaction.
 * If a {@link Device} instance implements this interface then a transaction will be demarcated
 * by a call to {@link #begin begin} and a call to {@link #end end}. The read and write operations
 * between these two calls will be part of a single transaction. A device driver may then
 * use this transaction demarcation to qualify the sequence of read and write operations
 * accordingly. For example, an I2C driver will treat the sequence of read and write operations to
 * the same I2C slave device as a combined message. An SPI driver will treat the sequence of read
 * and write operations to the same SPI slave device as a single transaction and will assert the
 * Slave Select line during the whole transaction.
 * <p />
 * In order to ensure that the {@link #end end} method is always invoked, these methods should be
 * used within a {@code try ... finally} block: <blockquote>
 *
 * <pre>
 * try {
 *     device.begin();
 *     // read and write operations
 * } finally {
 *     device.end();
 * }
 * </pre>
 * </blockquote>
 *
 * @since 1.0
 */
public interface Transactional {

    /**
     * Demarcates the beginning of a transaction.
     *
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalStateException
     *             if a transaction is already in progress.
     */
    public void begin() throws ClosedDeviceException, IOException;

    /**
     * Demarcates the end of a transaction.
     *
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalStateException
     *             if a transaction is not currently in progress.
     */
    public void end() throws ClosedDeviceException, IOException;
}
