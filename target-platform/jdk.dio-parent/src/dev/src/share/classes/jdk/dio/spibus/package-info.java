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

/**
 * Interfaces and classes for SPI (Serial Peripheral Interface Bus) device access.
 * <p />
 * The functionalities supported by this API are those of an SPI master.
 * <p />
 * In order to communicate with a specific slave, an application should first open and obtain an
 * {@link jdk.dio.spibus.SPIDevice} instance for the SPI slave device the application wants to exchange
 * data with, using its numeric ID, name, type (interface) or properties:
 * <dl>
 * <dt>Using its ID</dt>
 * <dd>
 * <blockquote>
 *
 * <pre>
 * SPIDevice slave = (SPIDevice) DeviceManager.open(3);
 * </pre>
 *
 * </blockquote></dd>
 * <dt>Using its name and interface</dt>
 * <dd>
 * <blockquote>
 *
 * <pre>
 * SPIDevice slave = DeviceManager.open(&quot;RTC1&quot;, SPIDevice.class, null);
 * </pre>
 *
 * </blockquote></dd>
 * </dl>
 * Once the device opened, the application can exchange data with the SPI slave device using methods of the
 * {@link jdk.dio.spibus.SPIDevice} interface such as the
 * {@link jdk.dio.spibus.SPIDevice#writeAndRead(ByteBuffer, ByteBuffer) writeAndRead} method.
 * <blockquote>
 *
 * <pre>
 * slave.writeAndRead(sndBuf, 0, 1, rcvBuf, 0, 1);
 * </pre>
 *
 * </blockquote> When the data exchange is over, the application should call the
 * {@link jdk.dio.spibus.SPIDevice#close() } method to close the SPI slave device. <blockquote>
 *
 * <pre>
 * slave.close();
 * </pre>
 *
 * </blockquote> The following sample code gives an example of using the SPI API to communicate with SPI slaves:
 * <blockquote>
 *
 * <pre>
 * try (SPIDevice slave = DeviceManager.open("SPI1", SPIDevice.class, null)) {
 *    ByteBuffer sndBuf1 = ByteBuffer.wrap(new byte[] {0x01});
 *    ByteBuffer sndBuf2 = ByteBuffer.wrap(new byte[] {0x02});
 *    ByteBuffer rcvBuf = ByteBuffer.wrap(new byte[3]);
 *    slave.writeAndRead(sndBuf1, rcvBuf); //received data will be stored in rcvBuf[0]
 *    slave.writeAndRead(sndBuf2, rcvBuf); //received data will be stored in rcvBuf[1] and rcvBuf[2]
 * } catch (IOException ioe) {
 *     // handle exception
 * }
 * </pre>
 *
 * </blockquote> The preceding example is using a <em>try-with-resources</em> statement and that the
 * {@link jdk.dio.spibus.SPIDevice#close SPIDevice.close} method is automatically invoked by the
 * platform at the end of the statement.
 * <p />
 * Information about the SPI-bus specification can be found at <a
 * href="http://www.freescale.com/files/microcontrollers/doc/ref_manual/M68HC11RM.pdf"
 * >http://www.freescale.com/files/microcontrollers/doc/ref_manual/M68HC11RM.pdf</a>.
 * <p />
 * Unless otherwise noted, passing a {@code null} argument to a constructor or method in any class
 * or interface in this package will cause a {@link java.lang.NullPointerException NullPointerException} to be thrown.
 */
package jdk.dio.spibus;