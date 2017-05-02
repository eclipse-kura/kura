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

package jdk.dio.spibus;

import jdk.dio.BufferAccess;
import jdk.dio.Device;
import jdk.dio.DeviceManager;
import jdk.dio.ClosedDeviceException;
import jdk.dio.UnavailableDeviceException;
import jdk.dio.Transactional;
import java.io.IOException;
import java.nio.*;
import java.nio.channels.ByteChannel;

/**
 * The {@code SPIDevice} interface provides methods for transmitting and receiving data to/from an SPI slave device.
 * <p />
 * An SPI slave device may be identified by the numeric ID and by the name (if any defined) that correspond to its
 * registered configuration. An {@code SPIDevice} instance can be opened by a call to one of the
 * {@link DeviceManager#open(int) DeviceManager.open(id,...)} methods using its ID or by a call to one of the
 * {@link DeviceManager#open(java.lang.String, java.lang.Class, java.lang.String[])
 * DeviceManager.open(name,...)} methods using its name. When an {@code SPIDevice} instance is opened with an ad-hoc
 * {@link SPIDeviceConfig} configuration (which includes its hardware addressing information) using one of the
 * {@link DeviceManager#open(jdk.dio.DeviceConfig) DeviceManager.open(config,...)} it is not
 * assigned any ID nor name.
 * <p />
 * On an SPI bus, data is transferred between the SPI master device and an SPI slave device in full duplex. That is,
 * data is transmitted by the SPI master to the SPI slave device at the same time data is received from the SPI slave
 * device by the SPI master.
 * <p />
 * To perform such a bidirectional exchange of data with an SPI slave device, an application may use one of the
 * {@link #writeAndRead(ByteBuffer, ByteBuffer) writeAndRead} methods. <br />
 * When an application only wants to send data to or receive data from an SPI slave device, it may use the
 * {@link #write(ByteBuffer) write} or the {@link #read(ByteBuffer) read} method, respectively. When writing only, the
 * data received from the SPI slave device will be ignored/discarded. When reading only, dummy data will be sent to the
 * slave.
 * <p/>
 * A data exchange consists of words of a certain length which may vary from SPI slave device to SPI slave device. <br />
 * Words in the sending and receiving byte buffers are not packed (bit-wise) and must be byte-aligned. The most
 * significant bits of a word are stored at the lower index (that is first). If a word's length is not a multiple of 8
 * (the byte length in bits) then the most significant bits will be undefined when receiving or unused when sending. If
 * the designated portion of a sending or receiving byte buffer cannot contain a (positive) integral number of words
 * then an {@link InvalidWordLengthException} will be thrown. For example, if the word length is 16bits and the
 * designated portion of buffer is only 1-byte long or is 3-byte long an {@link InvalidWordLengthException} will be
 * thrown. <br />
 * Assuming a word length <em>w</em>, the length <em>l</em> of the designated portion of the sending or receiving byte
 * buffer must be such that: <br />
 * <em>((l % (((w - 1) / 8) + 1)) == 0)</em>
 * <p />
 * Since the SPI master device controls the serial transmission clock read and write operations are non-blocking
 * (unless another read or write operation is already on-going in a different thread on the same {@code SPIDevice} instance).
 * It is the responsibility of the application to appropriately control the timing between a call to the {@link #begin begin} method which
 * may assert the Chip Select (depending on the configuration, see {@link SPIDeviceConfig#CS_NOT_CONTROLLED}) and
 * subsequent calls to the {@link #read read}, {@link #write write} and {@link #writeAndRead writeAndRead} methods.
 * <p />
 * When the data exchange is over, an application should call the {@link #close SPIDevice.close} method to close the
 * SPI slave device. Any further attempt to transmit/write or receive/read to/from an SPI slave device which has been
 * closed will result in a {@link ClosedDeviceException} been thrown.
 *
 * @see SPIPermission
 * @see InvalidWordLengthException
 * @see ClosedDeviceException
 * @since 1.0
 */
public interface SPIDevice extends Device<SPIDevice>, ByteChannel, Transactional, BufferAccess<ByteBuffer> {

    /**
     * Demarcates the beginning of an SPI transaction so that this slave's Select line (SS) will be remain asserted
     * during the subsequent read and write operations and until the transaction ends.
     *
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalStateException
     *             if a transaction is already in progress.
     */
    @Override
    void begin() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Demarcates the end of a transaction hence ending the assertion of this slave's Select line (SS).
     *
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalStateException
     *             if a transaction is not currently in progress.
     */
    @Override
    void end() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Gets the transfer word length in bits supported by this slave device.
     * <p>
     * If the length of data to be exchanged belies a slave's word length an {@link InvalidWordLengthException} will be
     * thrown.
     *
     * @return this slave's transfer word length in bits.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int getWordLength() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Reads one data word of up to 32 bits from this slave device.
     *
     * @return the data word read.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws InvalidWordLengthException
     *             if the number of bytes to receive belies word length; that is this slave's word length is bigger than
     *             32 bits.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int read() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Reads a sequence of bytes from this slave device into the given buffer.
     * <p />
     * Dummy data will be sent to this slave device by the platform.
     * <p />
     * {@inheritDoc}
     *
     * @param dst
     *            The buffer into which bytes are to be transferred
     *
     * @return The number of bytes read into {@code dst}, possibly zero, or {@code -1} if the device has reached end-of-stream.
     *
     * @throws NullPointerException
     *             If {@code dst} is {@code null}.
     * @throws InvalidWordLengthException
     *             if the number of bytes to receive belies word length.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             If some other I/O error occurs
     */
    @Override
    int read(ByteBuffer dst) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Reads a sequence of bytes from this device into the given buffer, skipping the first {@code skip} bytes read.
     * <p />
     * Dummy data will be sent to this slave device by the platform.
     * <p />
     * Apart from skipping the first {@code skip} bytes, this method behaves identically to
     * {@link #read(java.nio.ByteBuffer)}.
     *
     * @param skip
     *            the number of read bytes that must be ignored/skipped before filling in the {@code dst} buffer.
     * @param dst
     *            The buffer into which bytes are to be transferred
     *
     * @return The number of bytes read into {@code dst}, possibly zero, or {@code -1} if the device has reached end-of-stream.
     *
     * @throws NullPointerException
     *             If {@code dst} is {@code null}.
     * @throws IllegalArgumentException
     *              If {@code skip} is negative.
     * @throws InvalidWordLengthException
     *             if the total number of bytes to receive belies word length.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             If some other I/O error occurs
     */
    int read(int skip, ByteBuffer dst) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Writes a sequence of bytes to this slave device from the given buffer.
     * <p />
     * {@inheritDoc}
     *
     * @param src
     *            The buffer from which bytes are to be retrieved
     * @return The number of bytes written from {@code src}, possibly zero
     * @throws NullPointerException
     *             If {@code src} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             If some other I/O error occurs
     */
    @Override
    int write(ByteBuffer src) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Writes one data word of up to 32 bits to this slave device.
     *
     * @param txData
     *            the data word to be written
     * @throws IOException
     *             if an I/O error occurred
     * @throws InvalidWordLengthException
     *             if the number of bytes to send belies word length; that is this slave's word length is bigger than 32
     *             bits.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void write(int txData) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Exchanges (transmits and receives) data with this slave device.
     * <p />
     * The designated portions of the sending and receiving byte buffers may not have the same length. When sending more
     * than is being received the extra received bytes are ignored/discarded. Conversely, when sending less than is
     * being received extra dummy data will be sent.
     * <p />
     * This method behaves as a combined {@link SPIDevice#write(java.nio.ByteBuffer)} and
     * {@link SPIDevice#read(java.nio.ByteBuffer)}.
     *
     * @param src
     *            The buffer from which bytes are to be retrieved
     * @param dst
     *            The buffer into which bytes are to be transferred
     * @return The number of bytes read into {@code dst}, possibly zero, or {@code -1} if the device has reached end-of-stream.
     * @throws NullPointerException
     *             If {@code src} or {@code dst} is {@code null}.
     * @throws InvalidWordLengthException
     *             if the number of bytes to receive or send belies word length.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             If some other I/O error occurs
     */
    int writeAndRead(ByteBuffer src, ByteBuffer dst) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Exchanges (transmits and receives) data with this slave device skipping the specified number of bytes received.
     * <p />
     * The designated portions of the sending and receiving byte buffers may not have the same length. When sending more
     * than is being received the extra received bytes are ignored/discarded. Conversely, when sending less than is
     * being received extra dummy data will be sent.
     * <p />
     * This method behaves as a combined {@link SPIDevice#write(java.nio.ByteBuffer)} and
     * {@link SPIDevice#read(java.nio.ByteBuffer)}.
     *
     * @param src
     *            The buffer from which bytes are to be retrieved
     * @param skip
     *            the number of received bytes that must be ignored/skipped before filling in the {@code dst} buffer.
     * @param dst
     *            The buffer into which bytes are to be transferred
     * @return The number of bytes read into {@code dst}, possibly zero, or {@code -1} if the device has reached end-of-stream.
     * @throws NullPointerException
     *             If {@code src} or {@code dst} is {@code null}.
     * @throws IllegalArgumentException
     *              If {@code skip} is negative.
     * @throws InvalidWordLengthException
     *             if the total number of bytes to receive or send belies word length.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             If some other I/O error occurs
     */
    int writeAndRead(ByteBuffer src, int skip, ByteBuffer dst) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Exchanges (transmits and receives) one data word of up to 32 bits with this slave device.
     *
     * @param txData
     *            the word to send.
     * @return the word received.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws InvalidWordLengthException
     *             if the numbers of bytes to send or to receive bely word length; that is this slave's word length is
     *             bigger than 32 bits.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int writeAndRead(int txData) throws IOException, UnavailableDeviceException, ClosedDeviceException;
}
