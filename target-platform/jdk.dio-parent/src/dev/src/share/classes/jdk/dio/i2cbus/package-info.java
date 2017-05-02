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
 * Interfaces and classes for I2C (Inter-Integrated Circuit Bus) device access.
 * <p />
 * The functionalities supported by this API are those of an I2C master.
 * <p />
 * In order to communicate with a specific slave device, an application should first open and obtain
 * an {@link jdk.dio.i2cbus.I2CDevice} instance for the I2C slave device the
 * application wants to exchange data with, using its numeric ID, name, type (interface) and/or
 * properties:
 * <dl>
 * <dt>Using its ID</dt>
 * <dd><blockquote>
 *
 * <pre>
 * I2CDevice slave = (I2CDevice) DeviceManager.open(3);
 * </pre>
 * </blockquote></dd>
 * <dt>Using its name and interface</dt>
 * <dd><blockquote>
 *
 * <pre>
 * I2CDevice slave = DeviceManager.open(&quot;ADC1&quot;, I2CDevice.class, null);
 * </pre>
 * </blockquote></dd>
 * </dl>
 * Once the device opened, the application can exchange data with the I2C slave device using
 * methods of the {@link jdk.dio.i2cbus.I2CDevice} interface such as the
 * {@link jdk.dio.i2cbus.I2CDevice#write(ByteBuffer) write} method. <blockquote>
 *
 * <pre>
 * slave.write(sndBuf, 0, 1);
 * </pre>
 * </blockquote> When the data exchange is over, the application should call the
 * {@link jdk.dio.i2cbus.I2CDevice#close I2CDevice.close} method to close the I2C
 * slave device. <blockquote>
 *
 * <pre>
 * slave.close();
 * </pre>
 * </blockquote> The following samples code give 2 examples of using the I2C API to communicate with
 * an I2C slave device: <blockquote>
 *
 * <pre>
 * try (I2CDevice slave = DeviceManager.open(&quot;LED_CONTROLLER&quot;, I2CDevice.class, null)) {
 *     ByteBuffer stopCmd = ByteBuffer.wrap(LED_STOP_COMMAND);
 *     ByteBuffer offCmd = ByteBuffer.wrap(LED_OFF_COMMAND);
 *     ByteBuffer onCmd = ByteBuffer.wrap(LED_ON_COMMAND);
 *     // Clear all status of the 'LED' slave device
 *     slave.write(ByteBuffer.wrap(stopCmd));
 *     slave.write(ByteBuffer.wrap(offCmd));
 *
 *     for (int i = 0; i &lt; LED_LOOP_COUNT; i++) {
 *         // turning 'LED' on and keeping it on for 1500ms
 *         slave.write(ByteBuffer.wrap(onCmd));
 *         try {
 *             Thread.sleep(LED_BLINK_TIME);
 *         } catch (InterruptedException ex) {
 *         }
 *
 *         // turning 'LED' off keeping it off for 1500ms
 *         slave.write(ByteBuffer.wrap(offCmd));
 *         try {
 *             Thread.sleep(LED_BLINK_TIME);
 *         } catch (InterruptedException ex) {
 *         }
 *     }
 * } catch (IOException ioe) {
 *     // handle exception
 * }
 * </pre>
 * </blockquote> Or, <blockquote>
 *
 * <pre>
 * try (I2CDevice slave = DeviceManager.open("EEPROM", I2CDevice.class, (String) null)) {
 *     try {
 *         byte[] addr = new byte[]{/* Some address * /};
 *         ByteBuffer data = ByteBuffer.allocateDirect(4);
 *         slave.begin();
 *         slave.write(ByteBuffer.wrap(addr)); // Writes the address
 *         int count = slave.read(data); // Read the data at that EEPROM address
 *     } finally {
 *         slave.end();
 *     }
 * }
 * </pre>
 * </blockquote> The preceding examples are using a <em>try-with-resources</em> statement;
 * the {@link jdk.dio.i2cbus.I2CDevice#close I2CDevice.close} method is
 * automatically invoked by the platform at the end of the statement.
 * <p />
 * Information about the I2C-bus specification can be found at <a
 * href="http://www.nxp.com/documents/user_manual/UM10204.pdf"
 * >http://www.nxp.com/documents/user_manual/UM10204.pdf</a>.
 * <p />
 * Unless otherwise noted, passing a {@code null} argument to a constructor or method in any class
 * or interface in this package will cause a {@link java.lang.NullPointerException
 * NullPointerException} to be thrown.
 *
 * @since 1.0
 */
package jdk.dio.i2cbus;

