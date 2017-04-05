/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.dio.spibus.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
//import java.security.AccessController;
import java.util.Vector;
import java.lang.Runnable;

import com.oracle.dio.power.impl.PowerManagedBase;
import com.oracle.dio.utils.Constants;
import com.oracle.dio.utils.ExceptionMessage;
import com.oracle.dio.utils.Logging;

import jdk.dio.ClosedDeviceException;
import jdk.dio.DeviceConfig;
import jdk.dio.DeviceDescriptor;
import jdk.dio.DeviceManager;
import jdk.dio.DeviceNotFoundException;
import jdk.dio.DevicePermission;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.UnavailableDeviceException;
import jdk.dio.spibus.InvalidWordLengthException;
import jdk.dio.spibus.InvalidWordLengthException;
import jdk.dio.spibus.SPIDevice;
import jdk.dio.spibus.SPIDeviceConfig;
import jdk.dio.spibus.SPIPermission;

/**
 *Implementation of SPISlave Interface.
 */
class SPISlaveImpl extends PowerManagedBase<SPIDevice> implements SPIDevice {

    //every call checkWordLen updates these two variables
    private int byteNum;
    private int bitNum;

    // indicate transaction is ongoing
    private Vector<Runnable> pendingActions;

    public SPISlaveImpl(DeviceDescriptor<SPIDevice> dscr, int mode) throws
            DeviceNotFoundException, InvalidDeviceConfigException {
        super(dscr, mode);

        SPIDeviceConfig cfg = dscr.getConfiguration();
        if(cfg.getControllerName() != null) {
            throw new InvalidDeviceConfigException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_OPEN_WITH_DEVICENAME_UNSUPPORTED)
            );
        }

        //SPIPermission permission = new SPIPermission(getSecurityName());
        //AccessController.getContext().checkPermission(permission);

        openSPIDeviceByConfig0(cfg.getControllerNumber(), cfg.getAddress(),
                                        cfg.getCSActiveLevel(), cfg.getClockFrequency(),
                                        cfg.getClockMode(), cfg.getWordLength(),
                                        cfg.getBitOrdering(), mode == DeviceManager.EXCLUSIVE);

        initPowerManagement();
    }

    private String getSecurityName(){
        SPIDeviceConfig cfg = dscr.getConfiguration();
        String securityName = (DeviceConfig.DEFAULT == cfg.getControllerNumber()) ? "" : String.valueOf(cfg.getControllerNumber());
        securityName = (DeviceConfig.DEFAULT == cfg.getAddress()) ? securityName : securityName + ":" + cfg.getAddress();
        return securityName;
    }

    protected void checkPowerPermission(){
        SPIDeviceConfig cfg = dscr.getConfiguration();

        String securityName = (DeviceConfig.DEFAULT == cfg.getControllerNumber()) ? "" : String.valueOf(cfg.getControllerNumber());
        securityName = (DeviceConfig.DEFAULT == cfg.getAddress()) ? securityName : securityName + ":" + cfg.getAddress();

        //SPIPermission permission = new SPIPermission(securityName, DevicePermission.POWER_MANAGE);

        //AccessController.getContext().checkPermission(permission);
    }

    @Override
    public synchronized void begin() throws IOException,
            UnavailableDeviceException, ClosedDeviceException {
        checkPowerState();
        tryLock(1);
        try {
            begin0();
            pendingActions = new Vector<>(2);
        } catch (IOException | IllegalStateException e) {
            unlock();
            throw e;
        }
    }

    @Override
    public synchronized void end() throws IOException,
            UnavailableDeviceException, ClosedDeviceException {
        checkOpen();
        try {
            end0();
            for (Runnable toRun : pendingActions) {
                toRun.run();
            }
        } finally {
            // release all ByteBuffers
            pendingActions = null;
            unlock();
        }
    }

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
     *             if this peripheral is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the peripheral has been closed.
     */
    @Override
    public synchronized int getWordLength() throws IOException,
            UnavailableDeviceException, ClosedDeviceException {
        checkPowerState();
        return getWordLength0();
    }

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
     *             if this peripheral is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the peripheral has been closed.
     */
    @Override
    public int read() throws IOException, UnavailableDeviceException,
            ClosedDeviceException {
        synchronized(this) {
            checkPowerState();
            checkWordLen();
        }
        ByteBuffer dst = ByteBuffer.allocateDirect(byteNum);
        transfer(null, 0, dst);
        return byteArray2int(dst);
    }

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
     * @return The number of bytes read, possibly zero, or {@code -1} if the device has reached end-of-stream
     *
     * @throws NullPointerException
     *             If {@code dst} is {@code null}.
     * @throws InvalidWordLengthException
     *             if the number of bytes to receive belies word length.
     * @throws UnavailableDeviceException
     *             if this peripheral is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the peripheral has been closed.
     * @throws IOException
     *             If some other I/O error occurs
     */
    @Override
    public int read(ByteBuffer dst) throws IOException,
            UnavailableDeviceException, ClosedDeviceException {
        return read(0, dst);
    }

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
     * @return The number of bytes read, possibly zero, or {@code -1} if the device has reached end-of-stream
     *
     * @throws NullPointerException
     *             If {@code dst} is {@code null}.
     * @throws InvalidWordLengthException
     *             if the total number of bytes to receive belies word length.
     * @throws UnavailableDeviceException
     *             if this peripheral is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the peripheral has been closed.
     * @throws IOException
     *             If some other I/O error occurs
     */
    @Override
    public int read(int skip, ByteBuffer dst) throws IOException,
            UnavailableDeviceException, ClosedDeviceException {
        if (0 > skip) {
            throw new IllegalArgumentException();
        }
        synchronized(this) {
            checkPowerState();
            checkWordLen();
            checkBuffer(dst);
        }
        return transfer(null, skip, dst);
    }

    /**
     * Writes a sequence of bytes to this slave device from the given buffer.
     * <p />
     * {@inheritDoc}
     *
     * @param src
     *            The buffer from which bytes are to be retrieved
     * @return The number of bytes written, possibly zero
     * @throws NullPointerException
     *             If {@code src} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this peripheral is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the peripheral has been closed.
     * @throws IOException
     *             If some other I/O error occurs
     */
    @Override
    public int write(ByteBuffer src) throws IOException,
            UnavailableDeviceException, ClosedDeviceException {
        synchronized(this) {
            checkPowerState();
            checkWordLen();
            checkBuffer(src);
        }
        return transfer(src, 0, null);
    }

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
     *             if this peripheral is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the peripheral has been closed.
     */
    @Override
    public void write(int txData) throws IOException,
            UnavailableDeviceException, ClosedDeviceException {
        writeAndRead(txData);
    }

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
     * @return The number of bytes read, possibly zero, or {@code -1} if the device has reached end-of-stream
     * @throws NullPointerException
     *             If {@code src} or {@code dst} is {@code null}.
     * @throws InvalidWordLengthException
     *             if the number of bytes to receive or send belies word length.
     * @throws UnavailableDeviceException
     *             if this peripheral is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the peripheral has been closed.
     * @throws IOException
     *             If some other I/O error occurs
     */
    @Override
    public int writeAndRead(ByteBuffer src, ByteBuffer dst)
            throws IOException, UnavailableDeviceException,
            ClosedDeviceException {
        return writeAndRead(src, 0, dst);
    }

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
     * @return The number of bytes read, possibly zero, or {@code -1} if the device has reached end-of-stream
     * @throws NullPointerException
     *             If {@code src} or {@code dst} is {@code null}.
     * @throws InvalidWordLengthException
     *             if the total number of bytes to receive or send belies word length.
     * @throws UnavailableDeviceException
     *             if this peripheral is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the peripheral has been closed.
     * @throws IOException
     *             If some other I/O error occurs
     */
    @Override
    public int writeAndRead(ByteBuffer src, int skip, ByteBuffer dst)
            throws IOException, UnavailableDeviceException,
            ClosedDeviceException {
        if (0 > skip) {
            throw new IllegalArgumentException();
        }
        synchronized(this) {
            checkPowerState();
            checkWordLen();
            checkBuffer(src);
            checkBuffer(dst);
        }
        return transfer(src, skip, dst);
    }

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
     *             if this peripheral is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the peripheral has been closed.
     */
    @Override
    public int writeAndRead(int txData) throws IOException, UnavailableDeviceException, ClosedDeviceException{
        synchronized(this) {
            checkPowerState();
            checkWordLen();
        }
        ByteBuffer tx = int2byteArray(txData);
        ByteBuffer rx = tx.slice();
        transfer(tx, 0, rx);
        return byteArray2int(rx);
    }

    protected int getGrpID() {
        return getGrpID0();
    }

    @Override
    public ByteBuffer getInputBuffer() throws ClosedDeviceException,
            IOException {
        throw new java.lang.UnsupportedOperationException();
    }

    @Override
    public ByteBuffer getOutputBuffer() throws ClosedDeviceException,
            IOException {
        throw new java.lang.UnsupportedOperationException();
    }


    private void checkWordLen(){
        bitNum = getWordLength0();
        if (bitNum > Constants.MAX_WORD_LEN) {
            throw new InvalidWordLengthException(
                ExceptionMessage.format(ExceptionMessage.SPIBUS_SLAVE_WORD_LENGTH, bitNum)
            );
        }
        byteNum = (bitNum - 1)/8 + 1;
    }

    // checkWordLen ought to be called before checkBuffer to get byteNum is up to date
    private void checkBuffer(ByteBuffer buffer) {
        if (buffer == null){
            throw new NullPointerException(
                ExceptionMessage.format(ExceptionMessage.SPIBUS_NULL_BUFFER)
            );
        }

        if ((buffer.remaining() % byteNum) != 0) {
            throw new InvalidWordLengthException(
                ExceptionMessage.format(ExceptionMessage.SPIBUS_BYTE_NUMBER_BELIES_WORD_LENGTH)
            );
        }
    }

    private ByteBuffer int2byteArray(int intVal) {
        ByteBuffer retA = ByteBuffer.allocateDirect(byteNum);
        for (int i=0; i< byteNum ; i++) {
            retA.put((byte)((intVal >> (8*(byteNum-i-1))) & 0xff));
        }
        retA.flip();
        return retA;
    }

    private int byteArray2int(ByteBuffer byteA) {
        byteA.rewind();
        int retI = 0;
        int tmp;
        for (int i = 0; i< byteNum ;i++) {
            tmp = byteA.get();
            retI |= ((tmp & 0xff ) << (8*(byteNum - i - 1)));
        }
        return retI;
    }

    /* Returns number of recevied bytes if dst is not NULL, or number of sent bytes otherwise */
    private int transfer(ByteBuffer src, int skip, ByteBuffer dst) throws IOException {
        int xfered = 0;
        final boolean count_recv = null != dst;
        final boolean combined = (0 != skip || (null != dst && null != src && dst.remaining() != src.remaining()));
        Vector<Runnable> localActions;

        /* synchronized allows to avoid IllegaStateException for the case when transfer()
           is called while previous operation is incomplete.
           sync on handle to avoid dead lock on close() since close is synchronized on this instance.
        */
        synchronized(handle){

        synchronized(this) {
            localActions = pendingActions;
        }
        final boolean trStart = combined && null == localActions;

        if (trStart) {
            try {
                begin();
                localActions = pendingActions;
            } catch (UnsupportedOperationException e) {
                Logging.reportWarning("Combined message is unsupported. Continue...");
            }
        }

        try {
            do {
                // convert tries to align toSend and toRecv buffer length if src or dst are nondirect.
                ByteBuffer toRecv = convert(dst, src);
                ByteBuffer toSend = convert(src, toRecv);


                // if there is nothing to send, use recv buffer as dummy send data
                if (null == toSend) {
                    toSend = toRecv.slice();
                }

                if (null != toRecv) {
                    // always align send and recv buffers len,
                    // or recv to NULL buffer
                    if (toSend.remaining() <= skip) {
                        toRecv = null;
                    }
                }


                try {
                    conditionalLock();
                     writeAndRead0(toSend, toRecv);
                } finally {
                    conditionalUnlock();
                }


                if (count_recv) {
                    xfered += (null == toRecv) ? 0 : toRecv.remaining();
                } else {
                    xfered += toSend.remaining();
                }

                if (null != src) {
                    if (null != localActions) {
                        final ByteBuffer ref = toSend;
                        localActions.add(new Runnable() {
                                public void run() {
                                    // dummy action to keep refence in queue
                                    ref.remaining();
                                };
                            });
                    }

                    try {
                        src.position(src.position() + toSend.remaining());
                    }catch (IllegalArgumentException e){
                        // the buffer was updated in parallel
                        Logging.reportWarning(ExceptionMessage.format(ExceptionMessage.BUFFER_IS_MODIFIED));
                        //
                        src.position(src.limit());
                    }
                }

                if (skip > 0) {
                    if(null != toRecv) {
                        // ability to fit 'skip' bytes was checked above (see if (toSend.remaining() <= skip) )
                        toRecv.position(skip);
                        skip = 0;
                    } else {
                        skip-=toSend.remaining();
                    }
                }
                if (null != toRecv) {
                    // transaction requires postponed reverse copying
                    if (null != localActions) {
                        final ByteBuffer to = dst.slice();
                        final ByteBuffer from = toRecv.slice();
                        localActions.add(new Runnable() {
                                public void run() {
                                    to.put(from);
                                };
                            });

                        try {
                            dst.position(dst.position() + toRecv.remaining());
                        }catch (IllegalArgumentException e){
                            // the buffer was updated in parallel
                            Logging.reportWarning(ExceptionMessage.format(ExceptionMessage.BUFFER_IS_MODIFIED));
                            //
                            dst.position(dst.limit());
                        }

                    } else {
                        dst.put(toRecv);
                    }
                }

            } while ((null != dst && dst.hasRemaining()) || (null != src && src.hasRemaining()));

        } finally  {
            if (trStart) {
                try {
                    // will initiate transfer on some platfroms
                    end();
                } catch (IllegalStateException e) {
                    // intentionally skip
                }
            }
        }
        }//synchronized(handle)
        return xfered;
    }



    private native int begin0() throws IOException, UnsupportedOperationException, IllegalStateException;
    private native int end0() throws IllegalStateException;

    private native void openSPIDeviceByConfig0(int deviceNumber, int address,
                                              int csActive, int clockFrequency,
                                              int clockMode, int wordLen,
                                              int bitOrdering, boolean exclusive);

    /* PREREQUISITES: either dst.len must be equals to src.len or dst must null */
    private native void writeAndRead0(ByteBuffer src, ByteBuffer dst) throws IOException;

    private native int getGrpID0();
    private native int getWordLength0();
    private native int getByteOrdering0();
}
