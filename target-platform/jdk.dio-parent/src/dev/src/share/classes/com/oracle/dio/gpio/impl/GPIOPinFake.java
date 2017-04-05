/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.dio.impl.AbstractPeripheral;
import com.oracle.dio.impl.FakeHandle;
import com.oracle.dio.impl.PeripheralDescriptorImpl;

import jdk.dio.ClosedDeviceException;
import jdk.dio.DeviceManager;
import jdk.dio.UnavailableDeviceException;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;
import jdk.dio.gpio.PinListener;

/* degenerate implemenation only for GPIOPortConfig.getPins(). Will be reimplemented at 8.1*/
public final class GPIOPinFake extends AbstractPeripheral<GPIOPin> implements GPIOPin {

    private GPIOPinConfig cfg;
    private boolean open;

    public GPIOPinFake(GPIOPinConfig cfg) {
        super(new PeripheralDescriptorImpl(-1, null, cfg, GPIOPin.class, null), DeviceManager.EXCLUSIVE);
        handle  = FakeHandle.getFakeHandle();

    }

    public int getDirection() throws IOException, UnavailableDeviceException, ClosedDeviceException {
        return cfg.getDirection();
    }

    public int getTrigger() throws IOException, UnavailableDeviceException, ClosedDeviceException {
        return cfg.getTrigger();
    }

    public boolean getValue() throws IOException, UnavailableDeviceException, ClosedDeviceException {
        return false;
    }

    public void setDirection(int direction) throws IOException, UnavailableDeviceException, ClosedDeviceException {
        throw new IOException();
    }

    public void setTrigger(int trigger) throws IOException, UnavailableDeviceException, ClosedDeviceException {
        throw new IOException();
    }

    public void setInputListener(PinListener listener) throws IOException, ClosedDeviceException {
        throw new IOException();
    }

    public void setValue(boolean value) throws IOException, UnavailableDeviceException, ClosedDeviceException {
        throw new IOException();
    }

    public void close() throws IOException{
        throw new IOException();
    }

    void closeInternal() {
        try {
            super.close();
        } catch (Exception e) {
            // intentionally ignored
        }
    }
}
