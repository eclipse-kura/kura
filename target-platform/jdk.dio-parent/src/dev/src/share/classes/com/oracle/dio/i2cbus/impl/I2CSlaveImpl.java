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

package com.oracle.dio.i2cbus.impl;

import java.io.IOException;
import java.nio.*;
//import java.security.AccessController;

import com.oracle.dio.power.impl.PowerManagedBase;
import com.oracle.dio.utils.Constants;
import com.oracle.dio.utils.ExceptionMessage;
import com.oracle.dio.utils.Logging;

import jdk.dio.*;
import jdk.dio.i2cbus.*;
import jdk.dio.i2cbus.I2CDevice.Bus;

import romizer.Hidden;

class I2CSlaveImpl extends PowerManagedBase<I2CDevice> implements I2CDevice {

    static final int I2C_REGULAR = 0;
    static final int I2C_COMBINED_START = 1;
    static final int I2C_COMBINED_END = 2;
    static final int I2C_COMBINED_BODY = 3;

    /**
     * Hidden transaction created when I2CDevice.begin is called
     */
    private I2CCombinedMessage internalTransaction;

    I2CSlaveImpl(DeviceDescriptor<I2CDevice> dscr, int mode) throws
            DeviceNotFoundException, InvalidDeviceConfigException {
        super(dscr, mode);

        I2CDeviceConfig cfg = dscr.getConfiguration();

        if (cfg.getControllerName() != null) {
            throw new InvalidDeviceConfigException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_OPEN_WITH_DEVICENAME_UNSUPPORTED)
            );
        }

        //I2CPermission permission = new I2CPermission(getSecurityName());
        //AccessController.getContext().checkPermission(permission);

        open0(cfg, mode == DeviceManager.EXCLUSIVE);

        initPowerManagement();
    }

    public Bus getBus() throws IOException {
        return new Bus() {
                public jdk.dio.i2cbus.I2CCombinedMessage createCombinedMessage() {
                    return new I2CCombinedMessage();
                }
            };
    }

    private String getSecurityName() {
        I2CDeviceConfig cfg = dscr.getConfiguration();
        String securityName = (DeviceConfig.DEFAULT == cfg.getControllerNumber()) ?
            "" : String.valueOf(cfg.getControllerNumber());
        securityName = (DeviceConfig.DEFAULT == cfg.getAddress()) ?
            securityName : securityName + ":" + cfg.getAddress();
        return securityName;
    }

    protected void checkPowerPermission() {
        //AccessController.getContext().checkPermission(new I2CPermission(getSecurityName(),
        //                                 DevicePermission.POWER_MANAGE));
    }

    private void doCheck(int skip, ByteBuffer buf) {
        if (buf == null)
            throw new NullPointerException();

        if (skip < 0)
            throw new IllegalArgumentException();
    }

    @Override
    public ByteBuffer getInputBuffer() throws ClosedDeviceException,
            IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer getOutputBuffer() throws ClosedDeviceException,
            IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public  int read() throws IOException,
            UnavailableDeviceException, ClosedDeviceException {
        ByteBuffer dst = ByteBuffer.allocateDirect(1);
        read(dst);
        return dst.get(0);
    }

    @Override
    public  int read(ByteBuffer dst) throws IOException,
            UnavailableDeviceException, ClosedDeviceException {
        return read(0, dst);
    }

    @Override
    public  int read(int skip, ByteBuffer dst) throws IOException,
            UnavailableDeviceException, ClosedDeviceException {

        doCheck(skip, dst);

        I2CCombinedMessage message = null;
        int pos = 0;
        synchronized(this) {
            message = internalTransaction;
            if (null != message) {
                pos  = dst.position();
                message.appendRead(this, skip, dst);
            }
        }
        if (null != message) {
            return 0;
        } else {
            return transfer(I2C_REGULAR, skip, dst);
        }
    }

    @Override
    public  int read(int subaddress, int subaddressSize,
            ByteBuffer dst) throws IOException, UnavailableDeviceException,
            ClosedDeviceException {
        return read(subaddress, subaddressSize, 0, dst);
    }

    @Override
    public  int read(int subaddress, int subaddressSize, int skip,
            ByteBuffer dst) throws IOException, UnavailableDeviceException,
            ClosedDeviceException {

        if (subaddressSize <= 0 || subaddressSize > 4 || subaddress < 0)
            throw new IllegalArgumentException();

        doCheck(skip, dst);

        ByteBuffer tmp = ByteBuffer.wrap(new byte[4/*sizeof(int)*/]);
        tmp.order(ByteOrder.BIG_ENDIAN);
        tmp.putInt(subaddress);
        tmp.position(4-subaddressSize);
        write(tmp);
        return read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException,
            UnavailableDeviceException, ClosedDeviceException {
        doCheck(0, src);

        I2CCombinedMessage message = null;
        int pos = 0;
        synchronized(this) {
            message = internalTransaction;
            if (null != message) {
                pos  = src.position();
                message.appendWrite(this,src);
            }
        }
        if (null != message) {
            return 0;
        } else {
            return transfer(I2C_REGULAR, -1, src);
        }
    }


    @Override
    public void write(int srcData) throws IOException,
            UnavailableDeviceException, ClosedDeviceException {
        ByteBuffer dst = ByteBuffer.allocateDirect(1);
        dst.put((byte)srcData);
        dst.flip();
        write(dst);
    }

    @Override
    public int write(int subaddress, int subaddressSize,
            ByteBuffer src) throws IOException, UnavailableDeviceException,
            ClosedDeviceException {
        boolean callEnd = false;

        if (subaddressSize <= 0 || subaddressSize > 4 || subaddress < 0) {
            throw new IllegalArgumentException();
        }

        doCheck(0, src);

        ByteBuffer tmp = ByteBuffer.allocateDirect(4/*size of int*/ + src.remaining());
        tmp.order(ByteOrder.BIG_ENDIAN);
        tmp.putInt(subaddress);
        tmp.put(src);
        tmp.position(4-subaddressSize);
        return write(tmp) - subaddressSize;
    }

    /**
     * @throws IllegalStateException if the bus is occupied for
     *        communication with other peripheral or with other
     *        transaction
     */
    int transfer(int flag, int skip, ByteBuffer buf)  throws
            UnavailableDeviceException, ClosedDeviceException, IOException {
        int ret = 0;
        ByteBuffer toSend;

        if (!buf.hasRemaining()) {
            return 0;
        }

        if (skip > 0) {
            toSend = ByteBuffer.allocateDirect(buf.remaining() + skip);
        } else {
            toSend = convert(buf, null);
        }

        checkPowerState();
        // if driver supports combined message
        // synchronized block introduce small since {@code transfer0} returns
        // immediately if single shot is trying to interrupt ongoing I2C combined
        // message.
        // if not, then it is pipelining transfer request.
        synchronized(handle) {
            try {
                conditionalLock();
                ret = transfer0(skip < 0, toSend, flag);
            } finally {
                conditionalUnlock();
            }
        }

        toSend.limit(ret);

        if (skip > 0) {
            if (ret > skip) {
                // need to count only bytes transfered to recv buffer
                ret -= skip;
                toSend.position(skip);
                try {
                    buf.put(toSend);
                } catch (IllegalArgumentException e) {
                    // application do something with ByteBuffer that affects its
                    // postion() and limit() in such a way that setting new
                    // position throws IllegalArgumentException
                    Logging.reportError(ExceptionMessage.format(ExceptionMessage.BUFFER_IS_MODIFIED));
                }
            } else {
                ret = -1;
            }
        } else {
            try{
                // if write just update buffer postion
                if (skip < 0) {
                    buf.position(buf.position() + ret);
                } else {
                    // slight overhead if buf and toSend point to the same memory area
                    buf.put(toSend);
                }
            } catch (IllegalArgumentException e) {
                // application do something with ByteBuffer that affects its
                // postion() and limit() in such a way that setting new
                // position throws IllegalArgumentException
                Logging.reportError(ExceptionMessage.format(ExceptionMessage.BUFFER_IS_MODIFIED));
            }
        }

        return ret;
    }

    /*
     *  It is acceptable to sync on {@code this} object since this
     *  is short operation
     */
    @Override
    public void begin() throws ClosedDeviceException, IOException {
        checkPowerState();
        synchronized(this) {
            if (null != internalTransaction) {
                throw new IllegalStateException();
            }
            // we do not rely on driver ability to keep bus locked between begin/end.
            // Instead we buffer all subsequent read/write operations inside
            // single transaction
            internalTransaction = new I2CCombinedMessage();
        }
    }

    @Override
    public void end() throws ClosedDeviceException, IOException {
        I2CCombinedMessage message;
        checkOpen();
        synchronized(this) {
            message = internalTransaction;
            // cleaning variable gives a chance to prepare next transaction
            // while current is ongoing
            internalTransaction = null;
        }
        if (null == message) {
            throw new IllegalStateException("No transaction in progress");
        }

        try {
            tryLock(1);
            message.transfer();
        } finally {
            unlock();
        }
    }

    protected synchronized long getGrpID() {
        return getGrpID0();
    }

    private native void open0(Object config, boolean isExclusive);

    /**
     * Transfers data from {@code dst} buffer over I2C bus. The
     * direction of transfer is set by {@code skip} value
     *
     * @param flag  type of message: REGULAR, COMBINED_START ...
     *              COMBINED_END
     * @param write direction of transfer, {@code true} for write,
     *              {@code false} for read
     * @param dst   the buffer that holds the data or to store data
     *              to.
     *
     * @return number of bytes was transfered over the bus.
     */
    private native int transfer0(boolean write, ByteBuffer dst, int flag);

    private native long getGrpID0();
}
