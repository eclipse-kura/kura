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
package dio.gpio;

import java.io.IOException;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.GPIOPort;
import jdk.dio.gpio.GPIOPortConfig;

/**
 * @title Utility class to store port configuration
 * @author stanislav.smirnov@oracle.com
 */
public class PortConfig {

    private final int ID;
    private final String name;
    private final GPIOPortConfig portConfig;
    private final int direction;
    private GPIOPort myDevice;

    public PortConfig(GPIOPortConfig portConfig) {
        this.portConfig = portConfig;
        this.name = "";
        this.ID = -1;
        this.direction = portConfig.getDirection();
    }

    public String getName() {
        return name;
    }

    public GPIOPort open() throws IOException {
        if (this.ID >= 0) {
            System.out.println("GPIOPort open by id");
            this.myDevice = (GPIOPort) DeviceManager.open(this.ID);
            return this.myDevice;
        }
        if (this.name.length() > 0) {
            System.out.println("GPIOPort open by name");
            this.myDevice = (GPIOPort) DeviceManager.open(this.name, GPIOPort.class);
            return this.myDevice;
        }
        if (this.portConfig != null) {
            System.out.println("GPIOPort open by config");
            this.myDevice = (GPIOPort) DeviceManager.open(this.portConfig);
            return this.myDevice;
        }
        return null;
    }

    public GPIOPortConfig getPortConfig(){
        return portConfig;
    }

    public int getDirection(){
        return direction;
    }

    public void close() throws IOException {
        if (myDevice != null) {
            this.myDevice.close();
        }
    }

    public boolean isOpen() {
        if (myDevice != null) {
            return myDevice.isOpen();
        }
        return false;
    }
}
