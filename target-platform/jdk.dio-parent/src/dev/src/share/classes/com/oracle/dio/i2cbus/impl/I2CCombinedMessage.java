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

package com.oracle.dio.i2cbus.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.oracle.dio.utils.ExceptionMessage;
import com.oracle.dio.utils.Logging;

import jdk.dio.ClosedDeviceException;
import jdk.dio.DeviceConfig;
import jdk.dio.UnavailableDeviceException;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;

class I2CCombinedMessage implements jdk.dio.i2cbus.I2CCombinedMessage {
    ArrayList<Message<I2CSlaveImpl>> messageList = new ArrayList<Message<I2CSlaveImpl>>();
    int messageBus = DeviceConfig.DEFAULT;
    boolean isAlreadyTransferedOnce;
    int rxMessageCount;

    private class Message<P extends I2CDevice> {
        public P device;
        public ByteBuffer buf;
        public int skip;
        public boolean isRx;
        public Message(P device, ByteBuffer buf, int skip, boolean isRx) {
            this.device = device;
            this.buf = buf;
            this.skip = skip;
            this.isRx = isRx;
        }
    }

    void check(Message message) throws ClosedDeviceException {

        if (isAlreadyTransferedOnce) {
            throw new IllegalStateException(
                ExceptionMessage.format(ExceptionMessage.I2CBUS_ALREADY_TRANSFERRED_MESSAGE)
            );
        }

        if (null == message.buf) {
            throw new NullPointerException(
                ExceptionMessage.format(ExceptionMessage.I2CBUS_NULL_BUFFER)
            );
        }

        if (0 > message.skip) {
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.I2CBUS_NEGATIVE_SKIP_ARG)
            );
        }

        if (!message.device.isOpen()) {
            throw new ClosedDeviceException();
        }

        /*  check that can get '-1' here */
        int busNumber = ((I2CDeviceConfig) message.device.getDescriptor().getConfiguration()).getControllerNumber();

        if (DeviceConfig.DEFAULT == messageBus) {
            messageBus = busNumber;
        } else {
            if (messageBus != busNumber) {
                throw new IllegalArgumentException(
                    ExceptionMessage.format(ExceptionMessage.I2CBUS_DIFFERENT_BUS_SLAVE_OPERATION)
                );
            }
        }

        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).buf == message.buf) {
                throw new IllegalArgumentException(
                    ExceptionMessage.format(ExceptionMessage.I2CBUS_BUFFER_GIVEN_TWICE)
                );
            }
        }
    }

    /**
     * Creates a new {@code I2CCombinedMessage} instance.
     */
    I2CCombinedMessage() {
    }

    /**
     * Appends a read message/operation from the provided I2C slave device. Reads up to
     * {@code rwBuf.remaining()} bytes of data from this slave device into the buffer {@code rxBuf}.
     *
     * @param slave
     *            the I2C slave device to read from.
     * @param rxBuf
     *            the buffer into which the data is read.
     * @return a reference to this {@code I2CCombinedMessage} object.
     * @throws NullPointerException
     *             If {@code rxBuf} is {@code null}.
     * @throws IllegalStateException
     *             if this message has already been transferred once.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalArgumentException
     *             if appending the read operation to a slave on a different bus.
     */
    public I2CCombinedMessage appendRead(I2CDevice slave, ByteBuffer rxBuf)
            throws ClosedDeviceException {
        Message message = new Message(slave, rxBuf, 0, true);
        synchronized (this) {
            check(message);
            messageList.add(message);
            ++rxMessageCount;
        }
        return this;
    }

    /**
     * Appends a read message/operation from the provided I2C slave device. Reads up to
     * {@code rwBuf.remaining()} bytes of data from this slave device into the buffer skipping
     * {@code rxBuf} the first {@code rxSkip} bytes read.
     *
     * @param slave
     *            the I2C slave device to read from.
     * @param rxSkip
     *            the number of read bytes that must be ignored/skipped before filling in the
     *            {@code rxBuf} buffer.
     * @param rxBuf
     *            the buffer into which the data is read.
     * @return a reference to this {@code I2CCombinedMessage} object.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws NullPointerException
     *             If {@code rxBuf} is {@code null}.
     * @throws IllegalStateException
     *             if this message has already been transferred once.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalArgumentException
     *             if {@code rxSkip} is negative or if appending the read operation to a slave on a
     *             different bus.
     */
    public I2CCombinedMessage appendRead(I2CDevice slave, int rxSkip,
            ByteBuffer rxBuf) throws IOException, ClosedDeviceException {
        Message message = new Message(slave, rxBuf, rxSkip, true);
        synchronized (this) {
            check(message);
            messageList.add(message);
            ++rxMessageCount;
        }
        return this;
    }

    /**
     * Appends a write message/operation from the provided I2C slave device. Writes to this slave
     * device {@code txBuff.remaining()} bytes from the buffer {@code txBuf}.
     *
     * @param slave
     *            the I2C slave device to write to.
     * @param txBuf
     *            the buffer containing the bytes to write.
     * @return a reference to this {@code I2CCombinedMessage} object.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws NullPointerException
     *             If {@code txBuf} is {@code null}.
     * @throws IndexOutOfBoundsException
     *             {@code txOff} or {@code txLen} points or results in pointing outside
     *             {@code txBuf}.
     * @throws IllegalStateException
     *             if this message has already been transferred once.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalArgumentException
     *             if appending the write operation to a slave on a different bus.
     */
    public I2CCombinedMessage appendWrite(I2CDevice slave, ByteBuffer txBuf) throws IOException,
            ClosedDeviceException {
        Message message = new Message(slave, txBuf, 0, false);
        synchronized (this) {
            check(message);
            messageList.add(message);
        }
        return this;
    }

    /**
     * Transfers this combined message. This will result in each of the contained
     * messages/operations to be sent/executed in the same order they have been appended to this
     * combined message.
     * <p />
     * Once transfer no additional operation can be appended anymore to this combined message. Any
     * such attempt will result in a {@link IllegalStateException} to be thrown. <br />
     * This combined message can be transferred several times.
     *
     * @return an array containing the number of bytes read for each of the read operations of this
     *         combined message; the results of each read operations appear in the very same order
     *         the read operations have been appended to this combined message.
     * @throws IOException
     *             if an I/O error occurred
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if any of the targeted devices is not currently available (has been closed).
     */
    public int[] transfer() throws IOException, UnavailableDeviceException, ClosedDeviceException {
        int bytesRead[];
        synchronized (this) {
            /* Forbid adding more messages to this combined message */
            isAlreadyTransferedOnce = true;
            if (0 == messageList.size()) {
                notifyAll();
                return null;
            }

            bytesRead = new int[rxMessageCount];
            int bytesReadIdx = 0;

            try {
                final int size = messageList.size();
                if (1 == size) {
                    Message message = messageList.get(0);
                    int skip = (message.isRx) ? message.skip : -1;
                    Logging.reportInformation(ExceptionMessage.format(ExceptionMessage.I2CBUS_LAST_MESSAGE));
                    int res = ((I2CSlaveImpl)message.device).transfer(I2CSlaveImpl.I2C_REGULAR, skip, message.buf);
                    if (rxMessageCount > 0) {
                        bytesRead[0] = res;
                    }
                } else {
                    int flag = I2CSlaveImpl.I2C_COMBINED_START;
                    Logging.reportInformation(ExceptionMessage.format(ExceptionMessage.I2CBUS_FIRST_MESSAGE));
                    for (int i = 0; i < messageList.size(); i++) {
                        Message message = messageList.get(i);
                        int skip = (message.isRx) ? message.skip : -1;
                        if(i == messageList.size() - 1) {
                            Logging.reportInformation(ExceptionMessage.format(ExceptionMessage.I2CBUS_LAST_MESSAGE));
                            flag = I2CSlaveImpl.I2C_COMBINED_END;
                        }
                        int res = ((I2CSlaveImpl)message.device).transfer(flag, skip, message.buf);
                        if (message.isRx) {
                            bytesRead[bytesReadIdx++] = res;
                        }
                        flag = I2CSlaveImpl.I2C_COMBINED_BODY;
                    }
                }
            } finally {
                notifyAll();
            }

        }
        return bytesRead;
    }
}
