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
//import java.security.AccessController;

import com.oracle.dio.power.impl.PowerManagedBase;
import com.oracle.dio.utils.Constants;
import com.oracle.dio.utils.ExceptionMessage;
import com.oracle.dio.impl.Handle;

import jdk.dio.*;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;
import jdk.dio.gpio.GPIOPinPermission;
import jdk.dio.gpio.PinListener;

/* "public" is only for PulseCounterImpl, PWMChannelImpl contructor */
class GPIOPinImpl extends PowerManagedBase<GPIOPin> implements GPIOPin {

    private PinListener listener;

    GPIOPinImpl(DeviceDescriptor<GPIOPin> dscr, int mode) throws
    DeviceNotFoundException, InvalidDeviceConfigException{
        super(dscr, mode);

        GPIOPinConfig cfg = dscr.getConfiguration();

        if(cfg.getControllerName() != null) {
            throw new InvalidDeviceConfigException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_OPEN_WITH_DEVICENAME_UNSUPPORTED)
            );
        }

        //GPIOPinPermission permission = new GPIOPinPermission(getSecurityName());
        //AccessController.getContext().checkPermission(permission);

        openPinByConfig0( cfg.getControllerNumber(), cfg.getPinNumber(),
                          cfg.getDirection(), cfg.getDriveMode(),
                          cfg.getTrigger(), cfg.getInitValue(),
                          mode == DeviceManager.EXCLUSIVE);

        initPowerManagement();
    }

    private String getSecurityName(){
        GPIOPinConfig cfg = dscr.getConfiguration();
        String securityName = (DeviceConfig.DEFAULT == cfg.getControllerNumber()) ? "" : String.valueOf(cfg.getControllerNumber());
        securityName = (DeviceConfig.DEFAULT == cfg.getPinNumber()) ? securityName : securityName + ":" + cfg.getPinNumber();
       return securityName;
    }

    protected void checkPowerPermission(){
    	//AccessController.getContext().checkPermission(new GPIOPinPermission(getSecurityName(), DevicePermission.POWER_MANAGE));
    }

    @Override
    public synchronized void setTrigger(int trigger)
    throws java.io.IOException,
    UnavailableDeviceException,
    ClosedDeviceException {
        checkOpen();
        if (GPIOPinConfig.TRIGGER_NONE > trigger ||
            GPIOPinConfig.TRIGGER_BOTH_LEVELS < trigger) {
            throw new IllegalArgumentException(String.valueOf(trigger));
        }
        setTrigger0( trigger);
    }

    @Override
    public synchronized int getTrigger()
    throws java.io.IOException,
    UnavailableDeviceException,
    ClosedDeviceException {
        checkOpen();
        return getTrigger0();
    }

    /**
        * Returns the current value of the pin, this can be called on both outputs and inputs.
        *
        * @return     true if pin is currently high
        */
    public synchronized boolean getValue() throws IOException,UnavailableDeviceException, ClosedDeviceException {

        checkOpen();

        checkPowerState();

        int ret = readPin0();

        return((ret == 1)?true:false);
    }

    /**
        * Set pin value.
        * InvalidOperationException will be thrown if try to set value to
        * output pin.
        * @param      value  binary value
        * @throws InvalidOperationException
        */
    public synchronized void setValue(boolean value)
    throws IOException,UnavailableDeviceException{

        checkOpen();

        checkPowerState();

        if (getOutputMode0() == false) {
            throw new UnsupportedOperationException(
                ExceptionMessage.format(ExceptionMessage.GPIO_SET_TO_INPUT_PIN)
            );
        }

        writePin0(value);
    }

    /**
        * Returns the current direction of pin.
        * Check whether current pin is output pin or input pin.
        * @return     true if pin is currently set as output.
        */
    public synchronized int getDirection()
    throws IOException,UnavailableDeviceException{

        checkOpen();

        return(getOutputMode0() ? OUTPUT : INPUT );
    }

    public synchronized void setInputListener(PinListener listener) throws java.io.IOException,
    UnavailableDeviceException,
    ClosedDeviceException{

        checkOpen();

        if (getOutputMode0() == true) {
            throw new UnsupportedOperationException (
                ExceptionMessage.format(ExceptionMessage.GPIO_REGISTER_LISTENER_TO_OUTPUT_PIN)
            );
        }
        if (null == listener) {
            GPIOPinEventHandler.getInstance().removeEventListener(this);
            if (null != this.listener) {
                try {
                    stopNoti0();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            this.listener = null;
        } else if (this.listener == null) {
            GPIOPinEventHandler.getInstance().setEventListener(this, listener);
            this.listener = listener;
            try {
                startNoti0();
            } catch (IOException ex) {
                GPIOPinEventHandler.getInstance().removeEventListener(this);
                this.listener = null;
                throw new UnsupportedOperationException(
                    ExceptionMessage.format(ExceptionMessage.GPIO_CANNOT_START_NOTIFICATION)
                );
            }
        } else {
            throw new IllegalStateException (
                ExceptionMessage.format(ExceptionMessage.GPIO_LISTENER_ALREADY_ASSIGNED)
            );
        }
    }

    public synchronized void setDirection(int direction)
    throws UnavailableDeviceException, IOException{

    	//AccessController.getContext().checkPermission(new GPIOPinPermission(getSecurityName(), GPIOPinPermission.SET_DIRECTION));

        checkOpen();

        checkPowerState();

        int dir = ((GPIOPinConfig)dscr.getConfiguration()).getDirection();

        if ( direction != OUTPUT && direction != INPUT) {
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.GPIO_DIR_SHOULD_BE_INPUT_OR_OUTPUT)
            );
        }

        if ((OUTPUT == direction &&
            GPIOPinConfig.DIR_INPUT_ONLY == dir) ||
            (INPUT == direction &&
             GPIOPinConfig.DIR_OUTPUT_ONLY == dir)) {
            throw new UnsupportedOperationException (
                ExceptionMessage.format(ExceptionMessage.GPIO_INCOMPATIBLE_DIR)
            );
        }

        setOutputMode0( ((direction == OUTPUT) ? true : false) );
    }

    @Override
    public synchronized void close() throws IOException {
        if (isOpen()) {
            if (null != listener) {
                try {
                    setInputListener(null);
                } catch (Exception ex) {
                    //  handle exception.
                }
            }
            super.close();
        }
    }

    protected synchronized int getGrpID() {
        return getGrpID0();
    }

    private native void openPinByConfig0(int port, int pin, int direction, int mode, int trigger, boolean value, boolean access);

    private native int readPin0() throws IOException;
    private native void writePin0(boolean value) throws IOException;
    private native void startNoti0() throws IOException;
    private native void stopNoti0() throws IOException;
    private native void setOutputMode0(boolean out);
    private native boolean getOutputMode0();

    private native void setTrigger0(int trigger);
    private native int getTrigger0();

    private native int getGrpID0();

}
