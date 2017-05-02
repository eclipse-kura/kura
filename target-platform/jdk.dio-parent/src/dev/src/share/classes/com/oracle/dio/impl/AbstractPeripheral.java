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

package com.oracle.dio.impl;
import java.io.IOException;
import java.nio.ByteBuffer;

import jdk.dio.ClosedDeviceException;
import jdk.dio.Device;
import jdk.dio.DeviceDescriptor;
import jdk.dio.DeviceManager;
import jdk.dio.UnavailableDeviceException;
import com.oracle.dio.utils.Constants;
import com.oracle.dio.utils.ExceptionMessage;
import java.nio.Buffer;

/* It is recommended to synchronize subclass native operation on {@code handle} lock.
   @see {@link #unlock()} for the reason
  */
public abstract class AbstractPeripheral<P extends Device<P>> implements Device<P> {

    protected DeviceDescriptor<P> dscr;

    protected Handle handle;

    protected int access;

    protected boolean busyFlag;

    protected AbstractPeripheral(DeviceDescriptor<P> dscr, int access) {
        this.dscr = dscr;
        this.access = access;
        handle = new Handle();
        busyFlag = false;
    }

    /**
     *  Indicates that lock is explicit and native operation need
     *  not to release it
     */
    private boolean locked;
    @Override
    public void tryLock(int timeout) throws UnavailableDeviceException, ClosedDeviceException, IOException {
        checkOpen();
        if (DeviceManager.SHARED == access) {
            locked = true;
            if (!tryLock0(timeout)) {
                locked = false;
                throw new UnavailableDeviceException(
                    ExceptionMessage.format(ExceptionMessage.DEVICE_LOCKED_BY_OTHER_APP)
                );
            }
        }
    }

    /*
     * It is a bit spec violation but it seems more logic to wait
     * native operation completion prior to categorical unlock
     * <p>
     * @see class description about mandatory requirement for IO
     *      operation to be synchronized on {@code handle} object
     */
    @Override
    public void unlock() throws IOException {
        synchronized(handle) {
            unlock0();
        }
    }

    /* it is mandatory for the method to be called with acquired {@code handle} object lock */
    protected void conditionalLock() throws UnavailableDeviceException{
        if (!tryLock0(1)) {
            throw new UnavailableDeviceException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_LOCKED_BY_OTHER_APP)
            );
        }
    }

    /* it is mandatory for the method to be called with acquired {@code handle} object lock */
    protected void conditionalUnlock() {
        if (!locked) {
            unlock0();
        }
    }

    @Override
    public void close() throws IOException {
        handle.close();
    }

    @Override
    public boolean isOpen() {
        return handle.isOpen();
    }

    @Override
    public DeviceDescriptor<P> getDescriptor() {
        return dscr;
    }

    public Handle getHandle() {
        return handle;
    }

    @Override
    public int hashCode() {
        return handle.hashCode();
    }

    protected void checkOpen() throws ClosedDeviceException {
        if (!handle.isOpen()) {
            throw new ClosedDeviceException();
        }
    }

    protected synchronized void setBusyFlag(boolean newFlag, int exceptionMsgId) throws IllegalArgumentException {
        if (busyFlag && newFlag) {
            throw new IllegalStateException(
                ExceptionMessage.format(exceptionMsgId)
            );
        }else{
            busyFlag = newFlag;
        }
    }

    /*
        updates buffer position.
        shift in bytes.
        called after native call on Buffer.clice() returns,
        and necessary shift original buffers position
    */
    protected void shiftBufferPosition(Buffer buff, int newExpectedPosition) throws IOException {
        synchronized (buff){
            if (newExpectedPosition > buff.position()){
                //try/catch because of possible not synchronized change of buff in another thread
                try{
                    int limit = buff.limit();
                    buff.position(newExpectedPosition > limit ? limit : newExpectedPosition);
                }catch(IllegalArgumentException e){
                    //limit() again cause that may only happen if position is shifted out of this thread
                    buff.position(buff.limit());
                }
            }
        }
    }

    // if src is byte[] wrapper then returned direct buffer is limited by toCompare length
    protected ByteBuffer convert(ByteBuffer src, final ByteBuffer toCompare) {
        ByteBuffer tmp = null;
        if (null != src && src.hasRemaining()) {
            if (src.isDirect()) {
                tmp = src.slice();
                if (null != toCompare && toCompare.remaining() < src.remaining()) {
                    tmp.limit(toCompare.remaining());
                }
            } else {
                final int limit = (null == toCompare || 0 == toCompare.remaining()) ? Integer.MAX_VALUE : toCompare.remaining();
                final int size = src.remaining() > limit ? limit : src.remaining();
                tmp = ByteBuffer.allocateDirect(size);
                tmp.order(src.order());
                tmp.put((ByteBuffer)src.slice().limit(size)).order(src.order());
                tmp.flip();
            }
        }

        return tmp;
    }

    /** Tries to lock given pripheral. Waits <code>timeout</code>
     *  time if device is locked by other application.
     *
     *  @return <code>true</code> if exclusive access was taken,
     *  <code>false</code> otherwsie
     */
    protected boolean tryLock0(int timeout) { return handle.tryLock(timeout); }

    /**
     * Release peripheral for shared access
     */
    protected  void unlock0() { handle.unlock(); }
}
