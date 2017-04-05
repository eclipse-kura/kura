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

package jdk.dio.atcmd;

import jdk.dio.DeviceConfig;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.DeviceManager;
import java.util.Objects;

import serializator.*;
import romizer.DontRenameMethod;

/**
 * The {@code ATDeviceConfig} class encapsulates the hardware addressing information, and static and
 * dynamic configuration parameters of an AT device.
 * <p />
 * Some hardware addressing parameter, and static and dynamic configuration parameters may be set to
 * {@link #DEFAULT}. Whether such default settings are supported is platform- as well as device
 * driver-dependent.
 * <p />
 * An instance of {@code ATDeviceConfig} can be passed to the
 * {@link DeviceManager#open(DeviceConfig)} or
 * {@link DeviceManager#open(Class, DeviceConfig)} method to open the designated AT device
 * with the specified configuration. A {@link InvalidDeviceConfigException} is thrown when
 * attempting to open a device with an invalid or unsupported configuration.
 *
 * @see DeviceManager#open(DeviceConfig)
 * @see DeviceManager#open(Class, DeviceConfig)
 * @since 1.0
 */
@SerializeMe
public class ATDeviceConfig implements DeviceConfig<ATDevice>, DeviceConfig.HardwareAddressing {

    private String controllerName;

    private int controllerNumber = DEFAULT;

    private int channelNumber = DEFAULT;

    // hidden constructor for serializer
    @DontRenameMethod
    ATDeviceConfig(){}

    /**
     * Creates a new {@code ATDeviceConfig} with the specified hardware addressing information.
     *
     * @param controllerName
     *            the controller name (such as its <em>device file</em> name on UNIX systems).
     * @param channelNumber
     *            the hardware channel's number (a positive or zero integer) or {@link #DEFAULT}.
     * @throws IllegalArgumentException
     *             if {@code channelNumber} is not in the defined range.</li>
     * @throws NullPointerException
     *             if {@code controller name} is {@code null}.
     */
    public ATDeviceConfig(String controllerName, int channelNumber) {
        this.controllerName = controllerName;
        this.channelNumber = channelNumber;
        // checks for NPE
        controllerName.length();
        if (DEFAULT > channelNumber) {
            throw new  IllegalArgumentException();
        }
    }

    /**
     * Creates a new {@code ATDeviceConfig} with the specified hardware addressing information.
     *
     * @param controllerNumber
     *            the controller number (a positive or zero integer) or {@link #DEFAULT}.
     * @param channelNumber
     *            the hardware channel's number (a positive or zero integer) or {@link #DEFAULT}.
     * @throws IllegalArgumentException
     *             if {@code channelNumber} is not in the defined range.</li>
     */
    public ATDeviceConfig(int controllerNumber, int channelNumber) {
        this.controllerNumber = controllerNumber;
        this.channelNumber = channelNumber;
        if (DEFAULT > channelNumber || DEFAULT > controllerNumber) {
            throw new  IllegalArgumentException();
        }
    }

    /**
     * Gets the configured channel number.
     *
     * @return the channel number (a positive or zero integer); or {@link #DEFAULT}.
     */
    public int getChannelNumber() {
        return channelNumber;
    }

    /**
     * Gets the configured controller number.
     *
     * @return the controller number (a positive or zero integer) or {@link #DEFAULT}.
     */
    @Override
    public int getControllerNumber() {
        return controllerNumber;
    }

    /**
     * Gets the configured controller name (such as its <em>device file</em> name on UNIX systems).
     *
     * @return the controller name or {@code null}.
     */
    @Override
    public String getControllerName() {
        return controllerName;
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.controllerName);
        hash = 79 * hash + this.controllerNumber;
        hash = 79 * hash + this.channelNumber;
        return hash;
    }

    /**
     * Checks two {@code ATDeviceConfig} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     * @return {@code true} if {@code obj} is a {@code ATDeviceConfig} and has the same hardware
     *         addressing information and configuration parameter values as this
     *         {@code ATDeviceConfig} object.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ATDeviceConfig other = (ATDeviceConfig) obj;
        if (!Objects.equals(this.controllerName, other.controllerName)) {
            return false;
        }
        if (this.controllerNumber != other.controllerNumber) {
            return false;
        }
        if (this.channelNumber != other.channelNumber) {
            return false;
        }
        return true;
    }
}
