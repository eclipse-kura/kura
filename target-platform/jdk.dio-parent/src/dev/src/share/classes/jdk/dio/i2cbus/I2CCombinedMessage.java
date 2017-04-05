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

package jdk.dio.i2cbus;

import jdk.dio.ClosedDeviceException;
import jdk.dio.UnavailableDeviceException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The {@code I2CCombinedMessage} interface provides methods for constructing a
 * combined message. A combined message may be constituted of at least two reads
 * and/or writes to one or more I2C slaves. A combined message starts with a
 * START bit and ends with a STOP bit. But each of read and write messages
 * constituting the combined message following the very first of these messages
 * starts with a REPEATED START bit (one that is not preceded by a STOP bit).
 * <p />
 * Here is an example of a combined message to several slaves: <blockquote>
 *
 * <pre>
 * try (I2CDevice slave1 = DeviceManager.open("TEMP", I2CDevice.class, null);
 *         I2CDevice slave2 = DeviceManager.open("EEPROM", I2CDevice.class, null)) {
 *     ByteBuffer temp = ByteBuffer.allocateDirect(4);
 *     byte[] addr = new byte[]{...};
 *     int byteRead = slave1.getBus().createCombinedMessage()
 *             .appendRead(slave1, temp) // Reads the temperature from TEMP sensor
 *             .appendWrite(slave2, ByteBuffer.wrap(addr)) // Writes the address to EEPROM to select the location
 *             .appendWrite(slave2, temp) // Writes the temperature at the selected EEPROM address
 *             .transfer()[0];
 * } catch (IOException ioe) {
 *     // Handle exception
 * }
 * </pre>
 * </blockquote> The preceding example is using a
 * <em>try-with-resources</em> statement. The
 * {@link I2CDevice#close I2CDevice.close} method is automatically invoked by
 * the platform at the end of the statement.
 * <p />
 * While a combined message containing a single read or a single write
 * or a write followed by a read to the same I2C slave device can be constructed
 * using this class, it is more effective to use directly the {@link I2CDevice#read(ByteBuffer) I2CDevice.read},
 * {@link I2CDevice#write(ByteBuffer) I2CDevice.write}, {@link I2CDevice#read(int, int, ByteBuffer)
 * I2CDevice.read(subaddress,...)} {@link I2CDevice#write(int, int, ByteBuffer)
 * I2CDevice.write(subaddress,...)} methods for that purpose.
 *
 * @since 1.0
 */
public interface I2CCombinedMessage {

    /**
     * Appends a read message/operation from the provided I2C slave device.
     * Reads up to {@code rwBuf.remaining()} bytes of data from this slave
     * device into the buffer {@code rxBuf}.
     * <p />
     * Buffers are not safe for use by multiple concurrent threads so care should
     * be taken to not access the provided buffer until the operation has completed - that
     * is: until the {@link #transfer() transfer} method has been invoked and has returned.
     * <p />
     * The appended operation will have a behavior equivalent to that of the
     * {@link I2CDevice#read(java.nio.ByteBuffer)} method.
     *
     * @param slave the I2C slave device to read from.
     * @param rxBuf the buffer into which the data is read.
     * @return a reference to this {@code I2CCombinedMessage} object.
     * @throws NullPointerException If {@code rxBuf} is {@code null}.
     * @throws IllegalStateException if this message has already been
     * transferred once.
     * @throws ClosedDeviceException if the device has been closed.
     * @throws IllegalArgumentException if appending the read operation to a
     * slave on a different bus.
     * @throws IOException if some other I/O error occurs.
     */
    I2CCombinedMessage appendRead(I2CDevice slave, ByteBuffer rxBuf) throws IOException, ClosedDeviceException;

    /**
     * Appends a read message/operation from the provided I2C slave device.
     * Reads up to {@code rwBuf.remaining()} bytes of data from this slave
     * device into the buffer skipping {@code rxBuf} the first {@code rxSkip}
     * bytes read.
     * <p />
     * Buffers are not safe for use by multiple concurrent threads so care should
     * be taken to not access the provided buffer until the operation has completed - that
     * is: until the {@link #transfer() transfer} method has been invoked and has returned.
     * <p />
     * The appended operation will have a behavior equivalent to that of the
     * {@link I2CDevice#read(int, java.nio.ByteBuffer)} method.
     *
     * @param slave the I2C slave device to read from.
     * @param rxSkip the number of read bytes that must be ignored/skipped
     * before filling in the {@code rxBuf} buffer.
     * @param rxBuf the buffer into which the data is read.
     * @return a reference to this {@code I2CCombinedMessage} object.
     * @throws NullPointerException If {@code rxBuf} is {@code null}.
     * @throws IllegalStateException if this message has already been
     * transferred once.
     * @throws ClosedDeviceException if the device has been closed.
     * @throws IllegalArgumentException if {@code rxSkip} is negative or if
     * appending the read operation to a slave on a different bus.
     * @throws IOException if some other I/O error occurs.
     */
    I2CCombinedMessage appendRead(I2CDevice slave, int rxSkip, ByteBuffer rxBuf) throws IOException,
            ClosedDeviceException;

    /**
     * Appends a write message/operation from the provided I2C slave device.
     * Writes to this slave device {@code txBuff.remaining()} bytes from the
     * buffer {@code txBuf}.
     * <p />
     * Buffers are not safe for use by multiple concurrent threads so care should
     * be taken to not access the provided buffer until the operation has completed - that
     * is: until the {@link #transfer() transfer} method has been invoked and has returned.
     * <p />
     * The appended operation will have a behavior equivalent to that of the
     * {@link I2CDevice#write(java.nio.ByteBuffer)} method.
     *
     * @param slave the I2C slave device to write to.
     * @param txBuf the buffer containing the bytes to write.
     * @return a reference to this {@code I2CCombinedMessage} object.
     * @throws NullPointerException If {@code txBuf} is {@code null}.
     * @throws IllegalStateException if this message has already been
     * transferred once.
     * @throws ClosedDeviceException if the device has been closed.
     * @throws IllegalArgumentException if appending the write operation to a
     * slave on a different bus.
     * @throws IOException if some other I/O error occurs.
     */
    I2CCombinedMessage appendWrite(I2CDevice slave, ByteBuffer txBuf) throws IOException,
            ClosedDeviceException;

    /**
     * Transfers this combined message. This will result in each of the
     * contained messages/operations to be sent/executed in the same order they
     * have been appended to this combined message.
     * <p />
     * Once transferred no additional operation can be appended anymore to this
     * combined message. Any such attempt will result in a
     * {@link IllegalStateException} to be thrown.
     * <p />
     * This combined message can be transferred several times. The data transferred
     * to or from each of the provided {@code ByteBuffer}s is determined by its current {@code position}
     * and {@code remaining} attributes at the time this method is call.
     * The caller is responsible for preparing the buffers between each transfer.
     * <br />
     * Buffers are not safe for use by multiple concurrent threads so care should
     * be taken to not access the provided buffers until the transfer has completed.
     *
     * @return an array containing the number of bytes read for each of the read
     * operations of this combined message; the results of each read operations
     * appear in the very same order the read operations have been appended to
     * this combined message.
     * @throws UnavailableDeviceException if any of the targeted devices is not
     * currently available - such as it is locked by another application.
     * @throws ClosedDeviceException if any of the targeted devices is not
     * currently available (has been closed).
     * @throws IOException if some other I/O error occurred
     */
    int[] transfer() throws IOException, UnavailableDeviceException, ClosedDeviceException;
}
