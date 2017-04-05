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

package jdk.dio.generic;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.InvalidDeviceConfigException;
import java.util.Objects;
import serializator.*;
import romizer.DontRenameMethod;


/**
 * The {@code GenericDeviceConfig} class encapsulates the hardware addressing information of generic
 * device.
 * <br />
 * It does not encapsulates static or dynamic configuration parameters;
 * configuration parameters should be set using the {@link GenericDevice#setControl
 * GenericDevice.setControl} method.
 * <p />
 * An instance of {@code GenericDeviceConfig} can be passed to the
 * {@link DeviceManager#open(DeviceConfig)} or
 * {@link DeviceManager#open(Class, DeviceConfig)} method to open the designated generic
 * device with the specified configuration. A {@link InvalidDeviceConfigException} is thrown
 * when attempting to open a device with an invalid or unsupported configuration.
 *
 * @see DeviceManager#open(DeviceConfig)
 * @see DeviceManager#open(Class, DeviceConfig)
 * @since 1.0
 */
@SerializeMe
public final class GenericDeviceConfig implements DeviceConfig<GenericDevice>, DeviceConfig.HardwareAddressing {
    private String controllerName;
    private int channelNumber = DEFAULT;
    private int controllerNumber = DEFAULT;

    // hidden constructor for serializer
    @DontRenameMethod
    GenericDeviceConfig(){}

    /**
     * Creates a new {@code GenericDeviceConfig} with the specified hardware addressing information
     * .
     *
     * @param controllerNumber
     *            the hardware controller's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param channelNumber
     *            the hardware device's number (a positive or zero integer) or {@link #DEFAULT}.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code controllerNumber} is not in the defined range;</li>
     *             <li>{@code channelNumber} is not in the defined range.</li>
     *             </ul>
     */
    public GenericDeviceConfig(int controllerNumber, int channelNumber) {
        if (controllerNumber< DEFAULT || channelNumber < DEFAULT) {
            throw new IllegalArgumentException();
        }
        this.controllerNumber = controllerNumber;
        this.channelNumber = channelNumber;
    }

    /**
     * Creates a new {@code GenericDeviceConfig} with the specified hardware addressing information.
     *
     * @param controllerName
     *            the controller name (such as its <em>device file</em> name on UNIX systems).
     * @throws NullPointerException
     *             if {@code controllerName} is {@code null}.
     */
    public GenericDeviceConfig(String controllerName) {
        this.controllerName = controllerName;
        // NPE check
        controllerName.length();
    }

    /**
     * Gets the configured channel number.
     *
     * @return the hardware device channel's number (a positive or zero integer) or {@link #DEFAULT}
     *         .
     */
    public int getChannelNumber() {
        return channelNumber;
    }

    /**
     * Gets the configured controller number.
     *
     * @return the hardware device controller's number (a positive or zero integer) or
     *         {@link #DEFAULT}.
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
        hash = 97 * hash + Objects.hashCode(this.controllerName);
        hash = 97 * hash + this.channelNumber;
        hash = 97 * hash + this.controllerNumber;
        return hash;
    }

    /**
     * Checks two {@code GenericDeviceConfig} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     * @return {@code true} if {@code obj} is a {@code GenericDeviceConfig} and has the same hardware
     *         addressing information and configuration parameter values as this
     *         {@code GenericDeviceConfig} object.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GenericDeviceConfig other = (GenericDeviceConfig) obj;
        if (!Objects.equals(this.controllerName, other.controllerName)) {
            return false;
        }
        if (this.channelNumber != other.channelNumber) {
            return false;
        }
        if (this.controllerNumber != other.controllerNumber) {
            return false;
        }
        return true;
    }
}
