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

package jdk.dio.i2cbus;

import jdk.dio.DeviceConfig;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.DeviceManager;
import java.util.Objects;
import serializator.*;
import romizer.DontRenameMethod;

/**
 * The {@code I2CDeviceConfig} class encapsulates the hardware addressing information, and static
 * and dynamic configuration parameters of an I2C slave device.
 * <p />
 * Some hardware addressing parameter, and static and dynamic configuration parameters may be set to
 * {@link #DEFAULT}. Whether such default settings are supported is platform- as well as device
 * driver-dependent.
 * <p />
 * An instance of {@code I2CDeviceConfig} can be passed to the
 * {@link DeviceManager#open(DeviceConfig)} or
 * {@link DeviceManager#open(Class, DeviceConfig)} method to open the designated I2C slave
 * device with the specified configuration. A {@link InvalidDeviceConfigException} is thrown
 * when attempting to open a device with an invalid or unsupported configuration.
 *
 * @see DeviceManager#open(DeviceConfig)
 * @see DeviceManager#open(Class, DeviceConfig)
 * @since 1.0
 */
@SerializeMe
public final class I2CDeviceConfig implements DeviceConfig<I2CDevice>, DeviceConfig.HardwareAddressing {

    /**
     * 7 bit slave address.
     */
    public static final int ADDR_SIZE_7 = 7;
    /**
     * 10 bit slave address.
     */
    public static final int ADDR_SIZE_10 = 10;

    private String controllerName;
    private int address;
    private int addressSize;
    private int controllerNumber = DEFAULT;
    private int clockFrequency = DEFAULT;


    // hidden constructor for serializer
    @DontRenameMethod
    I2CDeviceConfig() {}

    /**
     * Creates a new {@code I2CDeviceConfig} with the specified hardware addressing information and
     * configuration parameters.
     *
     * @param controllerNumber
     *            the number of the bus the slave device is connected to (a positive or zero
     *            integer) or {@link #DEFAULT}.
     * @param address
     *            the address of the slave device on the bus (a positive or zero integer).
     * @param addressSize
     *            the address size: {@link #ADDR_SIZE_7} bits, {@link #ADDR_SIZE_10} bits or
     *            {@link #DEFAULT}.
     * @param clockFrequency
     *            the clock frequency of the slave device in Hz (a positive integer) or
     *            {@link #DEFAULT}.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code controllerNumber} is not in the defined range;</li>
     *             <li>{@code address} is not in the defined range;</li>
     *             <li>{@code addressSize} is not in the defined range;</li>
     *             <li>{@code clockFrequency} is not in the defined range.</li>
     *             </ul>
     */
    public I2CDeviceConfig(int controllerNumber, int address, int addressSize, int clockFrequency) {
        this.controllerNumber = controllerNumber;
        this.address = address;
        this.addressSize = addressSize;
        this.clockFrequency = clockFrequency;
        checkValues();
    }

    /**
     * Creates a new {@code I2CDeviceConfig} with the specified hardware addressing information and
     * configuration parameters.
     *
     * @param controllerName
     *            the controller name (such as its <em>device file</em> name on UNIX systems).
     * @param address
     *            the address of the slave device on the bus (a positive or zero integer).
     * @param addressSize
     *            the address size: {@link #ADDR_SIZE_7} bits, {@link #ADDR_SIZE_10} bits or
     *            {@link #DEFAULT}.
     * @param clockFrequency
     *            the clock frequency of the slave device in Hz (a positive integer) or
     *            {@link #DEFAULT}.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code address} is not in the defined range;</li>
     *             <li>{@code addressSize} is not in the defined range;</li>
     *             <li>{@code clockFrequency} is not in the defined range.</li>
     *             </ul>
     * @throws NullPointerException
     *             if {@code controllerName} is {@code null}.
     */
    public I2CDeviceConfig(String controllerName, int address, int addressSize, int clockFrequency) {
        this.controllerName = controllerName;
        this.address = address;
        this.addressSize = addressSize;
        this.clockFrequency = clockFrequency;
        // check for null
        controllerName.length();
        checkValues();
    }

    /**
     * Gets the configured address of the I2C slave device.
     *
     * @return the address of the slave device on the bus (a positive or zero integer).
     */
    public int getAddress() {
        return address;
    }

    /**
     * Gets the configured address size of the I2C slave device.
     *
     * @return the address size: {@link #ADDR_SIZE_7} bits, {@link #ADDR_SIZE_10} bits or
     *         {@link #DEFAULT}.
     */
    public int getAddressSize() {
        return addressSize;
    }

    /**
     * Gets the configured controller number (the controller number the I2C bus adapter the I2C slave device
     * is connected to).
     *
     * @return the controller number (a positive or zero integer) or {@link #DEFAULT}.
     */
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
     * Gets the configured clock frequency (in Hz) supported by the I2C slave device.
     *
     * @return the clock frequency of the slave device in Hz (a positive integer) or
     *         {@link #DEFAULT}.
     */
    public int getClockFrequency() {
        return clockFrequency;
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
        hash = 97 * hash + this.address;
        hash = 97 * hash + this.addressSize;
        hash = 97 * hash + this.controllerNumber;
        hash = 97 * hash + this.clockFrequency;
        return hash;
    }

    /**
     * Checks two {@code I2CDeviceConfig} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     * @return {@code true} if {@code obj} is a {@code I2CDeviceConfig} and has the same hardware
     *         addressing information and configuration parameter values as this
     *         {@code I2CDeviceConfig} object.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final I2CDeviceConfig other = (I2CDeviceConfig) obj;
        if (!Objects.equals(this.controllerName, other.controllerName)) {
            return false;
        }
        if (this.address != other.address) {
            return false;
        }
        if (this.addressSize != other.addressSize) {
            return false;
        }
        if (this.controllerNumber != other.controllerNumber) {
            return false;
        }
        if (this.clockFrequency != other.clockFrequency) {
            return false;
        }
        return true;
    }

    private void checkValues() {
        if ((null == controllerName && DEFAULT > controllerNumber) ||
            0 > address ||
            (ADDR_SIZE_7 != addressSize && ADDR_SIZE_10 != addressSize && DEFAULT != addressSize) ||
            DEFAULT > clockFrequency || 0 == clockFrequency) {
            throw new IllegalArgumentException();
        }
    }

}
