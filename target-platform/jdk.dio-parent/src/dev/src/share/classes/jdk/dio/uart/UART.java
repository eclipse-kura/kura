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

package jdk.dio.uart;

import jdk.dio.BufferAccess;
import jdk.dio.ClosedDeviceException;
import jdk.dio.InputRoundListener;
import jdk.dio.OutputRoundListener;
import jdk.dio.Device;
import jdk.dio.DeviceManager;
import jdk.dio.UnavailableDeviceException;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * The {@code UART} interface provides methods for controlling and accessing a UART (Universal Asynchronous
 * Receiver/Transmitter).
 * <p />
 * A UART device may be identified by the numeric ID and by the name (if any defined) that correspond to its
 * registered configuration. A {@code UART} instance can be opened by a call to one of the
 * {@link DeviceManager#open(int) DeviceManager.open(id,...)} methods using its ID or by a call to one of the
 * {@link DeviceManager#open(java.lang.String, java.lang.Class, java.lang.String[])
 * DeviceManager.open(name,...)} methods using its name. When a {@code UART} instance is opened with an ad-hoc
 * {@link UARTConfig} configuration (which includes its hardware addressing information) using one of the
 * {@link DeviceManager#open(jdk.dio.DeviceConfig) DeviceManager.open(config,...)} it is not
 * assigned any ID nor name.
 * <p />
 * <p />
 * Once opened, an application can read the received data bytes and write the data bytes to be transmitted through the
 * UART using methods of the {@link ByteChannel} interface.
 * <p/>
 * An application can register a {@link UARTEventListener} instance which will get asynchronously notified of input data
 * availability, input buffer overrun and/or empty output buffer conditions. The input and output buffers for
 * which these events may be notified may not necessarily correspond to the transmit and receive FIFO buffers of the
 * UART hardware but may be buffers allocated by the underlying native driver. To register a {@link UARTEventListener}
 * instance, the application must call the {@link #setEventListener setEventListener} method. The registered
 * listener can later on be removed by calling the same method with a {@code null} listener parameter.
 * <p />
 * When done, an application should call the {@link #close UART.close} method to close the UART. Any further attempt
 * to access or control a UART which has been closed will result in a {@link ClosedDeviceException} been thrown.
 *
 * @see UARTPermission
 * @see ClosedDeviceException
 * @since 1.0
 */
public interface UART extends Device<UART>, ByteChannel, BufferAccess<ByteBuffer> {

    /**
     * Gets the current baud rate. If the baud rate was not set previously using {@link #setBaudRate setBaudRate} the
     * device configuration-specific default value is returned.
     *
     * @return the current baud rate.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    int getBaudRate() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Gets the current number of bits per character.
     *
     * @return the number bits per character: {@link UARTConfig#DATABITS_5}, {@link UARTConfig#DATABITS_6},
     *         {@link UARTConfig#DATABITS_7}, {@link UARTConfig#DATABITS_8} or {@link UARTConfig#DATABITS_9}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    int getDataBits() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Gets the current parity.
     *
     * @return the speed parity: {@link UARTConfig#PARITY_ODD}, {@link UARTConfig#PARITY_EVEN},
     *         {@link UARTConfig#PARITY_MARK}, {@link UARTConfig#PARITY_SPACE}, or {@link UARTConfig#PARITY_NONE}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    int getParity() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Gets the current number of stop bits per character.
     *
     * @return the number of stop bits per character: {@link UARTConfig#STOPBITS_1}, {@link UARTConfig#STOPBITS_1_5}, or
     *         {@link UARTConfig#STOPBITS_2}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    int getStopBits() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets the baud rate.
     *
     * @param baudRate
     *            the baud rate to set.
     *
     * @throws UnsupportedOperationException
     *             if this UART cannot be configured with the requested baud rate.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    void setBaudRate(int baudRate) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets the number of bits per character.
     *
     * @param dataBits
     *            the number bits per character: {@link UARTConfig#DATABITS_5}, {@link UARTConfig#DATABITS_6},
     *            {@link UARTConfig#DATABITS_7}, {@link UARTConfig#DATABITS_8} or {@link UARTConfig#DATABITS_9}.
     * @throws UnsupportedOperationException
     *             if this UART cannot be configured with the requested number of bits per character.
     * @throws IllegalArgumentException
     *             if {@code dataBits} is not one of the defined values.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    void setDataBits(int dataBits) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Registers a {@link UARTEventListener} instance to monitor input data availability, input buffer overrun and/or
     * empty output buffer conditions. While the listener can be triggered by hardware interrupts, there are no
     * real-time guarantees of when the listener will be called.
     * <p />
     * A list of event type IDs is defined in {@link UARTEvent}.
     * <p />
     * If this {@code UART} is open in {@link DeviceManager#SHARED} access mode
     * the listeners registered by all the applications sharing the underlying device will get
     * notified of the events they registered for.
     * <p />
     * If {@code listener} is {@code null} then listener previously registered for the specified event type will be
     * removed.
     * <p />
     * Only one listener can be registered at a particular time for a particular event type.
     *
     * @param eventId
     *            ID of the native event to listen to: {@link UARTEvent#INPUT_DATA_AVAILABLE},
     *              {@link UARTEvent#INPUT_BUFFER_OVERRUN},
     *              {@link UARTEvent#OUTPUT_BUFFER_EMPTY}, {@link UARTEvent#BREAK_INTERRUPT},
     *              {@link UARTEvent#FRAMING_ERROR} or {@link UARTEvent#PARITY_ERROR}.
     * @param listener
     *            the {@link UARTEventListener} instance to be notified upon occurrence of the designated event.
     * @throws IllegalArgumentException
     *             if {@code eventId} does not correspond to any of the supported event types.
     * @throws IllegalStateException
     *             if {@code listener} is not {@code null} and a listener is already registered for the specified event
     *             type.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws UnsupportedOperationException
     *             if this UART does not support asynchronous event notification of the requested conditions (eg. input data availability, input
     *             buffer overrun, empty output buffer and/or error conditions).
     * @throws IOException
     *             if some other I/O error occurs.
     */
    void setEventListener(int eventId, UARTEventListener listener) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets the parity.
     *
     * @param parity
     *            the speed parity: {@link UARTConfig#PARITY_ODD}, {@link UARTConfig#PARITY_EVEN},
     *            {@link UARTConfig#PARITY_MARK}, {@link UARTConfig#PARITY_SPACE}, or {@link UARTConfig#PARITY_NONE}.
     * @throws UnsupportedOperationException
     *             if this UART cannot be configured with the requested parity.
     * @throws IllegalArgumentException
     *             if {@code parity} is not one of the defined values.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    void setParity(int parity) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets the number of stop bits per character.
     *
     * @param stopBits
     *            the number of stop bits per character: {@link UARTConfig#STOPBITS_1}, {@link UARTConfig#STOPBITS_1_5},
     *            or {@link UARTConfig#STOPBITS_2}.
     * @throws UnsupportedOperationException
     *             if this UART cannot be configured with the requested number of stop bits per character.
     * @throws IllegalArgumentException
     *             if {@code stopBits} is not one of the defined values.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    void setStopBits(int stopBits) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Starts asynchronous writing in successive rounds - initially writing the data remaining in the provided
     * buffer. Additional rounds are asynchronously fetched by notifying the provided {@link OutputRoundListener}
     * instance once the initial data have been written. The initial data to be written
     * is retrieved from the provided buffer; the data to write during the subsequent rounds is retrieved
     * from that very same buffer upon invocation of the provided {@link OutputRoundListener} instance.
     * <p />
     * Writing can be stopped by a call to {@link #stopWriting stopWriting}.
     * <p />
     * <i>r</i> bytes will be written to this {@code UART},
     * where <i>r</i> is the number of bytes remaining in the buffer (possibly {@code 0}), that is,
     * <code>src.remaining()</code>, at the moment this method is initially invoked
     * and then subsequently when the listener is returning.
     * <p />
     * Suppose that a byte sequence of length <i>n</i> is written, where
     * <i>{@code 0 <= n <= r}</i>.
     * This byte sequence will be transferred from the buffer starting at index
     * <i>p</i>, where <i>p</i> is the buffer's position at the moment this
     * method is initially invoked
     * and then subsequently when the listener is returning; the index of the last byte written will be
     * <i>{@code p + n - 1}</i>.
     * Upon invocation of the listener for fetching more data to write the buffer's position will be equal to
     * <i>{@code p + n}</i>; its limit will not have changed.
     * <br />
     * The buffer's position upon stopping this asynchronous operation by a call to {@link #stopWriting stopWriting}
     * is not predictable unless called from within the listener.
     * <p />
     * The data will be written according to the current baud rate as returned by {@link #getBaudRate getBaudRate}. The
     * baud rate and other configuration parameters can be changed by the provided {@link OutputRoundListener} instance
     * upon notification of each round.
     * <p />
     * A buffer with {@code 0} bytes remaining to be written (that is a buffer already empty) at the moment this method is initially
     * invoked or then subsequently when the listener is returning will not stop the asynchronous operation; the listener is
     * guaranteed to be called back again at the latest as soon as all other events pending at the time of notification have been dispatched.
     * <p />
     * Interfering with the asynchronous operation by accessing and modifying the provided buffer concurrently
     * may yield unpredictable results.
     * <p />
     * Only one write operation (synchronous or asynchronous) can be going on at any time.
     * <br />
     * Note therefore that while empty output buffer conditions ({@link UARTEvent#OUTPUT_BUFFER_EMPTY}) may be
     * notified to the registered {@code UARTEventListener}
     * independently to the invocation of the provided {@code OutputRoundListener} attempting to call
     * the {@code write} method from within the registered {@code UARTEventListener} will result in an exception.
     *
     * <p />
     * Buffers are not safe for use by multiple concurrent threads so care should
     * be taken to not access the provided buffer until the operation (or a round thereof) has completed.
     *
     * @param src
     *            the buffer for the data to be written.
     * @param listener
     *            the {@link OutputRoundListener} instance to be notified when the all the data remaining
     * in the buffer has been written.
     * @throws NullPointerException
     *             If {@code src} or {@code listener} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if another synchronous or asynchronous output operation is already active.
     * @throws IOException
     *             if some other I/O error occurs such as the device is not writable.
     */
    void startWriting(ByteBuffer src, OutputRoundListener<UART, ByteBuffer> listener) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Starts asynchronous writing in successive rounds.
     * <p />
     * This method behaves identically to {@link #startWriting(ByteBuffer, OutputRoundListener)} excepts that it
     * uses double-buffering. Notification will happen when all the data remaining in the current working buffer (initially {@code src1}) has been written
     * and writing will proceed with the alternate buffer (which will become the
     * current working buffer). Writing will only be suspended if the previous event has not yet been handled.
     * <p />
     * A working buffer with {@code 0} bytes remaining to be written (that is a buffer already empty) at the moment this method is initially
     * invoked or then subsequently when the listener is returning will not stop the asynchronous operation; the listener is
     * guaranteed to be called back again at the latest as soon as all other events pending at the time of notification have been dispatched.
     * <p />
     * Only one write operation (synchronous or asynchronous) can be going on at any time.
     * <br />
     * Note therefore that while empty output buffer conditions ({@link UARTEvent#OUTPUT_BUFFER_EMPTY}) may be
     * notified to the registered {@code UARTEventListener}
     * independently from the invocation of the provided {@code OutputRoundListener} attempting to call
     * the {@code write} method from within the registered {@code UARTEventListener} will result in an exception.
     *
     * <p />
     * Buffers are not safe for use by multiple concurrent threads so care should
     * be taken to not access the working buffer until the operation (or a round thereof) has completed.
     *
     * @param src1
     *            the first buffer for the data to be written.
     * @param src2
     *            the second buffer for the data to be written.
     * @param listener
     *            the {@link OutputRoundListener} instance to be notified when all
     *            the data remaining in the working buffer has been written.
     * @throws NullPointerException
     *             If {@code src1}, {@code src2} or {@code listener} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if another synchronous or asynchronous output operation is already active.
     * @throws IOException
     *             if some other I/O error occurs such as the device is not writable.
     */
    void startWriting(ByteBuffer src1, ByteBuffer src2, OutputRoundListener<UART, ByteBuffer> listener) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * Stops (cancels) the currently active writing session.
     * <p />
     * This method return silently if no writing session is currently active.
     *
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    void stopWriting() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Starts asynchronous reading in successive rounds - reading data into the provided
     * buffer. The provided {@link InputRoundListener} is cyclicly notified when the provided buffer has been filled
     * with input data. Reading into the buffer and notification will only resume once the
     * event has been handled. Reading and notification will immediately start and will repeat until it is stopped by a
     * call to {@link #stopReading stopReading}.
     * <p />
     * <i>r</i> bytes will be read from this {@code UART},
     * where <i>r</i> is the number of bytes remaining in the buffer (possibly {@code 0}), that is,
     * <tt>dst.remaining()</tt>, at the moment this method is initially invoked
     * and then subsequently when the listener is returning.
     * <p />
     * Suppose that a byte sequence of length <i>n</i> is read, where {@code 0 <= n <= r}.
     * This byte sequence will be transferred into the buffer so that the first
     * byte in the sequence is at index <i>p</i> and the last byte is at index
     * <i>{@code p + n - 1}</i>,
     * where <i>p</i> is the buffer's position at the moment this
     * method is initially invoked
     * and then subsequently when the listener is returning.
     * Upon invocation of the listener for fetching more data to write the buffer's position will be equal to
     * <i>{@code p + n}</i>; its limit will not have changed.
     * <br />
     * The buffer's position upon stopping this asynchronous operation by a call to {@link #stopReading stopReading}
     * is not predictable unless called from within the listener..
     * <p />
     * The data will be read according to the current baud rate as returned by {@link #getBaudRate getBaudRate}. The
     * baud rate and other configuration parameters can be changed by the provided {@link InputRoundListener} instance
     * upon notification of each round.
      * <p />
     * A buffer with {@code 0} bytes remaining to be read (that is a buffer already full) at the moment this method is initially
     * invoked or then subsequently when the listener is returning will not stop the asynchronous operation; the listener is
     * guaranteed to be called back again at the latest as soon as all other events pending at the time of notification have been dispatched.
     * <p />
     * Interfering with the asynchronous operation by accessing and modifying the provided buffer concurrently
     * may yield unpredictable results.
     * <p />
     * Only one read operation (synchronous or asynchronous) can be going on at any time.
     * <br />
     * Note therefore that while the availability of new input data ({@link UARTEvent#INPUT_DATA_AVAILABLE}) may be
     * notified to the registered {@code UARTEventListener}
     * independently from the invocation of the provided {@code InputRoundListener} attempting to call
     * the {@code read} method from within the registered {@code UARTEventListener} will result in an exception.
     *
     * <p />
     * Buffers are not safe for use by multiple concurrent threads so care should
     * be taken to not access the provided buffer until the operation (or a round thereof) has completed.
     *
     * @param dst
     *            the buffer for the data to be read.
     * @param listener
     *            the {@link InputRoundListener} instance to be notified when the all remaining
     * space in the buffer has been filled with input data.
     * @throws NullPointerException
     *             If {@code src} or {@code listener} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if another synchronous or asynchronous input operation is already active.
     * @throws IOException
     *             if some other I/O error occurs such as the device is not readable.
     */
    void startReading(ByteBuffer dst, InputRoundListener<UART, ByteBuffer> listener) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Starts asynchronous reading in successive rounds.
     * <p />
     * This method behaves identically to {@link #startReading(ByteBuffer, InputRoundListener)} excepts that it
     * uses double-buffering. Notification will happen when all the remaining space in the current working buffer (initially {@code dst1}) has been filled
     * and reading will proceed with the alternate buffer (which will become the
     * current working buffer). Reading will only be suspended if the previous event has not yet been handled.
     * <p />
     * A buffer with {@code 0} bytes remaining to be read (that is a buffer already full) at the moment this method is initially
     * invoked or then subsequently when the listener is returning will not stop the asynchronous operation; the listener is
     * guaranteed to be called back again at the latest as soon as all other events pending at the time of notification have been dispatched.
     * <p />
     * Only one read operation (synchronous or asynchronous) can be going on at any time.
     * <br />
     * Note therefore that while the availability of new input data ({@link UARTEvent#INPUT_DATA_AVAILABLE}) may be
     * notified to the registered {@code UARTEventListener}
     * independently from the invocation of the provided {@code InputRoundListener} attempting to call
     * the {@code read} method from within the registered {@code UARTEventListener} will result in an exception.
     *
     * <p />
     * Buffers are not safe for use by multiple concurrent threads so care should
     * be taken to not access the working buffer until the operation (or a round thereof) has completed.
     *
     * @param dst1
     *            the first buffer for the data to be read.
     * @param dst2
     *            the second buffer for the data to be read.
     * @param listener
     *            the {@link InputRoundListener} instance to be notified when all
     *            the space remaining in the working buffer has been filled with input data.
     * @throws NullPointerException
     *             If {@code dst1}, {@code dst2} or {@code listener} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if another synchronous or asynchronous input operation is already active.
     * @throws IOException
     *             if some other I/O error occurs such as the device is not readable.
     */
    void startReading(ByteBuffer dst1, ByteBuffer dst2, InputRoundListener<UART, ByteBuffer> listener) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * Stops (cancels) the currently active reading session.
     * <p />
     * This method return silently if no reading session is currently active.
     *
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    void stopReading() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Generates a break condition for the specified duration.
     *
     * @param duration duration of the break condition to generate.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws UnsupportedOperationException
     *             if this UART cannot be configured with the requested receive trigger level.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    void generateBreak(int duration) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets the receive trigger level. The {@link UARTEventListener} instance registered
     * for {@link UARTEvent#INPUT_DATA_AVAILABLE} events (if any) will get notified after
     * the specified number of bytes have been received in the input buffer.
     * If a synchronous read operation is on-going it may then immediately return
     * with the number of bytes already read.
     *
     * @param level the trigger level, in bytes.
     *
     * @throws UnsupportedOperationException
     *             if this UART cannot be configured with the requested receive trigger level.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    void setReceiveTriggerLevel(int level) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Gets the current receive trigger level.
     *
     * @return the trigger level, in bytes or {@code 0} if this feature is not supported.
     *
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    int getReceiveTriggerLevel() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets the receive timeout. The {@link UARTEventListener} instance registered
     * for {@link UARTEvent#INPUT_DATA_AVAILABLE} events (if any) will get notified if
     * there is at least one byte in the input buffer and the specified timeout has elapsed.
     * If a synchronous read operation is on-going it may then immediately return
     * with the number of bytes already read.
     *
     * @param timeout the timeout, in milliseconds.
     *
     * @throws UnsupportedOperationException
     *             if this UART cannot be configured with the requested receive timeout.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    void setReceiveTimeout(int timeout) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Gets the current receive timeout.
     *
     * @return the timeout, in milliseconds or {@code 0} if this feature is not supported.
     *
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    int getReceiveTimeout() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Reads a sequence of bytes from this UART into the given buffer. The method will block
     * until the requested number of bytes are read or the receive trigger level (if set) has been reached
     * or the receive timeout (if set) has elapsed.
     * <p />
     * The availability of new input data may be notified through an {@link UARTEvent} with ID
     * {@link UARTEvent#INPUT_DATA_AVAILABLE} according to the receive trigger level or receive timeout (if set).
     *
     * {@inheritDoc}
     *
     * @param dst
     *            The buffer into which bytes are to be transferred
     *
     * @return The number of bytes read into {@code dst}, possibly zero, or {@code -1} if the device has reached end-of-stream
     *
     * @throws NullPointerException
     *             If {@code dst} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if an I/O error occurred such as the device is not readable.
     *
     * @see #setReceiveTriggerLevel setReceiveTriggerLevel
     * @see #setReceiveTimeout setReceiveTimeout
     */
    @Override
    int read(ByteBuffer dst) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * Writes a sequence of bytes to this UART from the given buffer.
     * <p />
     * An empty output buffer condition may be notified through an {@link UARTEvent} with ID
     * {@link UARTEvent#OUTPUT_BUFFER_EMPTY}.
     *
     * {@inheritDoc}
     *
     * @param src
     *            The buffer from which bytes are to be retrieved
     * @return The number of bytes written from {@code src}, possibly zero.
     * @throws NullPointerException
     *             If {@code src} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if an I/O error occurred such as the device is not writable.
     */
    @Override
    int write(ByteBuffer src) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;
}
