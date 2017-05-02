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

package com.oracle.dio.gpio.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import jdk.dio.ClosedDeviceException;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.DeviceDescriptor;
import jdk.dio.DeviceManager;
import jdk.dio.DeviceNotFoundException;
import jdk.dio.UnsupportedDeviceTypeException;
import jdk.dio.UnavailableDeviceException;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;
import jdk.dio.gpio.GPIOPort;
import jdk.dio.gpio.GPIOPortConfig;
import jdk.dio.gpio.PortListener;
import com.oracle.dio.impl.AbstractPeripheral;
import com.oracle.dio.utils.Constants;
import com.oracle.dio.utils.ExceptionMessage;
import jdk.dio.gpio.GPIOPortPermission;
//import java.security.AccessController;

class GPIOPortImpl extends AbstractPeripheral<GPIOPort> implements GPIOPort {

    private PortListener listener;
    private int maxVal;

    public GPIOPortImpl(DeviceDescriptor<GPIOPort> dscr, int mode) throws
                                    DeviceNotFoundException, InvalidDeviceConfigException {
        super(dscr, mode);

        this.dscr = dscr;
        GPIOPortConfig cfg = dscr.getConfiguration();

        //GPIOPortPermission permission = new GPIOPortPermission(dscr.getName()==null?"":dscr.getName());
        //AccessController.getContext().checkPermission(permission);

        GPIOPinConfig[] pinCfgs = cfg.getPinConfigs();
        int[][] portsAndPins = new int[pinCfgs.length][4];
        for (int i = 0; i < pinCfgs.length; ++i) {
            GPIOPinConfig pinCfg = pinCfgs[i];

            if(pinCfg.getControllerName() != null) {
                throw new InvalidDeviceConfigException(
                    ExceptionMessage.format(ExceptionMessage.DEVICE_OPEN_WITH_DEVICENAME_UNSUPPORTED)
                );
            }

            portsAndPins[i][0] = pinCfg.getControllerNumber();
            portsAndPins[i][1] = pinCfg.getPinNumber();
            portsAndPins[i][2] = pinCfg.getDriveMode();
            portsAndPins[i][3] = pinCfg.getTrigger();
        }
        openPortByConfig0(portsAndPins, cfg.getDirection(), cfg.getInitValue(),
                                   mode == DeviceManager.EXCLUSIVE);

        maxVal = getMaxVal0();
        GPIOPinFake[] pins  = new GPIOPinFake[pinCfgs.length];
        for (int i = 0; i < pins.length; i++) {
            pins[i] = new GPIOPinFake(pinCfgs[i]);
        }

        assignPins0(cfg, pins);
    }

    @Override
    public int getMaxValue() throws ClosedDeviceException {
        checkOpen();
        return maxVal;
    }

    @Override
    public synchronized void setValue(int value)
        throws IOException,
                UnavailableDeviceException  {

        checkOpen();

        if(value > maxVal || 0 > value){
            throw new IllegalArgumentException();
        }

        if(!getOutputMode0()){
            throw new UnsupportedOperationException (
                ExceptionMessage.format(ExceptionMessage.GPIO_WRITE_TO_INPUT_PORT)
            );
        }

        writePort0(value);
    }

    @Override
    public synchronized int getValue()
        throws IOException, UnavailableDeviceException {

        checkOpen();

        return readPort0();
    }

    @Override
    public synchronized int getDirection() throws IOException,
            UnavailableDeviceException{
        checkOpen();
        return (getOutputMode0() ? OUTPUT : INPUT );
    }

    @Override
    public synchronized void setDirection(int direction)
        throws IOException,
                UnavailableDeviceException{

        //AccessController.getContext().checkPermission(new GPIOPortPermission(dscr.getName()==null?"":dscr.getName(),GPIOPortPermission.SET_DIRECTION));

        checkOpen();

        if( direction != OUTPUT && direction != INPUT){
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.GPIO_DIR_SHOULD_BE_INPUT_OR_OUTPUT)
            );
        }

        int dir = ((GPIOPortConfig)getDescriptor().getConfiguration()).getDirection();

        if ((OUTPUT == direction &&
            GPIOPinConfig.DIR_INPUT_ONLY == dir) ||
            (INPUT == direction &&
             GPIOPinConfig.DIR_OUTPUT_ONLY == dir)) {
            throw new UnsupportedOperationException (
                ExceptionMessage.format(ExceptionMessage.GPIO_INCOMPATIBLE_DIR)
            );
        }


        setOutputMode0(((OUTPUT == direction)?true:false) );
    }

    @Override
    public synchronized void setInputListener(PortListener listener) throws java.io.IOException,
                         UnavailableDeviceException,
                         ClosedDeviceException {
        checkOpen();

        if(getOutputMode0()){
            throw new UnsupportedOperationException (
                ExceptionMessage.format(ExceptionMessage.GPIO_REGISTER_LISTENER_TO_OUTPUT_PORT)
            );
        }
        if(null == listener){
            GPIOPortEventHandler.getInstance().removeEventListener(this);
            if(null != this.listener){
                try {
                    stopNoti0();
                } catch (IOException ex) {
                }
            }
            this.listener = null;

        }else if (null == this.listener) {
            GPIOPortEventHandler.getInstance().setEventListener(this, listener);
            this.listener = listener;
            try {
                startNoti0();
            } catch (IOException ex) {
                GPIOPortEventHandler.getInstance().removeEventListener(this);
                this.listener = null;
                throw new UnsupportedOperationException (
                    ExceptionMessage.format(ExceptionMessage.GPIO_CANNOT_START_NOTIFICATION)
                );
            }

        }else{
            throw new IllegalStateException (
                ExceptionMessage.format(ExceptionMessage.GPIO_LISTENER_ALREADY_ASSIGNED)
            );
        }
    }

    public synchronized void close() throws IOException {
        if(isOpen()){
            if(null != listener){
                try{
                    setInputListener(null);
                }catch(IOException ex){
                }
            }
            GPIOPortConfig cfg = dscr.getConfiguration();
            for(GPIOPin pin: cfg.getPins()) {
                ((GPIOPinFake)pin).closeInternal();
            }
            // for DM.register: nullifying to remove redundant information from serialized class
            assignPins0(cfg, null);
            super.close();
        }
    }

    private native void openPortByConfig0(int[][] portsAndPins, int direction,
                                          int value, boolean access);
    private native int getMaxVal0();
    private native int readPort0() throws IOException;
    private native void writePort0(int value) throws IOException;
    private native void setOutputMode0(boolean out);
    private native boolean getOutputMode0();
    private native void startNoti0() throws IOException;
    private native void stopNoti0() throws IOException;

    private native void assignPins0(GPIOPortConfig cfg, GPIOPin[] pins);
}

