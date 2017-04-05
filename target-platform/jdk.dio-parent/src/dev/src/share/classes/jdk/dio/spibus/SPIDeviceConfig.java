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

package jdk.dio.spibus;

import jdk.dio.DeviceConfig;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.DeviceManager;
import java.util.Objects;
import serializator.*;
import romizer.DontRenameMethod;

/**
 * The {@code SPIDeviceConfig} class encapsulates the hardware addressing information, and static and dynamic
 * configuration parameters of an SPI slave device.
 * <p />
 * Some hardware addressing parameter, and static and dynamic configuration parameters may be set to {@link #DEFAULT}.
 * Whether such default settings are supported is platform- as well as device driver-dependent.
 * <p />
 * <h3><a name="mode">SPI Clock Modes</a></h3>
 * The clock mode is a number from 0 to 3 which represents the combination of the CPOL (SPI Clock Polarity Bit) and CPHA
 * (SPI Clock Phase Bit) signals where CPOL is the high order bit and CPHA is the low order bit:
 * <table style="border:1px solid black;border-collapse:collapse;" * summary="The clock mode is a number which represents the combination of the CPOL (SPI Clock Polarity Bit) * and CPHA (SPI Clock Phase Bit) signals.">
 * <tr style="border:1px solid black;">
 * <th id="t1" style="border:1px solid black;">Mode</th>
 * <th id="t2" style="border:1px solid black;">CPOL</th>
 * <th id="t3" style="border:1px solid black;">CPHA</th>
 * </tr>
 * <tr style="border:1px solid black;">
 * <td headers="t1" style="border:1px solid black;">0</td>
 * <td headers="t2" style="border:1px solid black;">0 = Active-high clocks selected.</td>
 * <td headers="t3" style="border:1px solid black;">0 = Sampling of data occurs at odd edges of the SCK clock</td>
 * </tr>
 * <tr style="border:1px solid black;">
 * <td headers="t1" style="border:1px solid black;">1</td>
 * <td headers="t2" style="border:1px solid black;">0 = Active-high clocks selected.</td>
 * <td headers="t3" style="border:1px solid black;">1 = Sampling of data occurs at even edges of the SCK clock</td>
 * </tr>
 * <tr style="border:1px solid black;">
 * <td headers="t1" style="border:1px solid black;">2</td>
 * <td headers="t2" style="border:1px solid black;">1 = Active-low clocks selected.</td>
 * <td headers="t3" style="border:1px solid black;">0 = Sampling of data occurs at odd edges of the SCK clock</td>
 * </tr>
 * <tr style="border:1px solid black;">
 * <td headers="t1" style="border:1px solid black;">3</td>
 * <td headers="t2" style="border:1px solid black;">1 = Active-low clocks selected.</td>
 * <td headers="t3" style="border:1px solid black;">1 = Sampling of data occurs at even edges of the SCK clock</td>
 * </tr>
 * </table>
 * <p />
 * An instance of {@code SPIDeviceConfig} can be passed to the {@link DeviceManager#open(DeviceConfig)} or
 * {@link DeviceManager#open(Class, DeviceConfig)} method to open the designated SPI slave device with the
 * specified configuration. A {@link InvalidDeviceConfigException} is thrown when attempting to open a
 * device with an invalid or unsupported configuration.
 *
 * @see DeviceManager#open(DeviceConfig)
 * @see DeviceManager#open(Class, DeviceConfig)
 * @since 1.0
 */
@SerializeMe
public final class SPIDeviceConfig implements DeviceConfig<SPIDevice>, DeviceConfig.HardwareAddressing {

    /**
     * High Chip Select active level.
     */
    public static final int CS_ACTIVE_HIGH = 0;

    /**
     * Low Chip Select active level.
     */
    public static final int CS_ACTIVE_LOW = 1;

    /**
     * Chip Select not controlled by driver. If this mode is configured, the Chip Select signal
     * is assumed to be controlled independently such as with an independent {@link jdk.dio.gpio.GPIOPin GPIOPin}.
     */
    public static final int CS_NOT_CONTROLLED = 2;

    private String controllerName;
    private int address;
    private int csActive = DEFAULT;
    private int controllerNumber = DEFAULT;
    private int bitOrdering = DEFAULT;
    private int clockFrequency = DEFAULT;
    private int clockMode = DEFAULT;
    private int wordLength = DEFAULT;

    // hidden constructor for serializer
    @DontRenameMethod
    SPIDeviceConfig(){}

    /**
     * Creates a new {@code SPIDeviceConfig} with the specified hardware addressing information and configuration
     * parameters. The Chip Select active level is platform and/or driver-dependent (i.e. {@link #DEFAULT}).
     *
     * @param controllerNumber
     *            the number of the bus the slave device is connected to (a positive or zero integer) or
     *            {@link #DEFAULT}.
     * @param address
     *            the Chip Select address of the slave device on the bus (a positive or zero integer).
     * @param clockFrequency
     *            the clock frequency of the slave device in Hz (a positive integer) or {@link #DEFAULT}.
     * @param clockMode
     *            the clock mode, one of: {@code 0}, {@code 1}, {@code 2} or {@code 4} (see <a href="#mode">SPI Clock
     *            Modes</a>).
     * @param wordLength
     *            the word length of the slave device (a positive integer) or {@link #DEFAULT}.
     * @param bitOrdering
     *            the bit (shifting) ordering of the slave device, one of: {@link SPIDevice#BIG_ENDIAN},
     *            {@link SPIDevice#LITTLE_ENDIAN}, {@link SPIDevice#MIXED_ENDIAN} or {@link #DEFAULT}.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code controllerNumber} is not in the defined range;</li>
     *             <li>{@code address} is not in the defined range;</li>
     *             <li>{@code clockFrequency} is not in the defined range;</li>
     *             <li>{@code clockMode} is not one of the defined values;</li>
     *             <li>{@code wordLength} is not in the defined range;</li>
     *             <li>{@code bitOrdering} is not one of the defined values.</li>
     *             </ul>
     */
    public SPIDeviceConfig(int controllerNumber, int address, int clockFrequency, int clockMode, int wordLength,
            int bitOrdering) {
        this(controllerNumber, address, DEFAULT, clockFrequency, clockMode, wordLength, bitOrdering);
    }

    /**
     * Creates a new {@code SPIDeviceConfig} with the specified hardware addressing information and configuration
     * parameters.
     *
     * @param controllerNumber
     *            the number of the bus the slave device is connected to (a positive or zero integer) or
     *            {@link #DEFAULT}.
     * @param address
     *            the Chip Select address of the slave device on the bus (a positive or zero integer).
     * @param csActive
     *            the Chip Select active level, one of {@link #CS_ACTIVE_LOW},
     *            {@link #CS_ACTIVE_HIGH}, {@link #CS_NOT_CONTROLLED} or {@link #DEFAULT}.
     * @param clockFrequency
     *            the clock frequency of the slave device in Hz (a positive integer) or {@link #DEFAULT}.
     * @param clockMode
     *            the clock mode, one of: {@code 0}, {@code 1}, {@code 2} or {@code 4} (see <a href="#mode">SPI Clock
     *            Modes</a>).
     * @param wordLength
     *            the word length of the slave device (a positive integer) or {@link #DEFAULT}.
     * @param bitOrdering
     *            the bit (shifting) ordering of the slave device, one of: {@link SPIDevice#BIG_ENDIAN},
     *            {@link SPIDevice#LITTLE_ENDIAN}, {@link SPIDevice#MIXED_ENDIAN} or {@link #DEFAULT}.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code controllerNumber} is not in the defined range;</li>
     *             <li>{@code address} is not in the defined range;</li>
     *             <li>{@code clockFrequency} is not in the defined range;</li>
     *             <li>{@code clockMode} is not one of the defined values;</li>
     *             <li>{@code wordLength} is not in the defined range;</li>
     *             <li>{@code bitOrdering} is not one of the defined values.</li>
     *             </ul>
     */
    public SPIDeviceConfig(int controllerNumber, int address, int csActive, int clockFrequency, int clockMode, int wordLength,
            int bitOrdering) {
        this.controllerNumber = controllerNumber;
        this.address = address;
        this.csActive = csActive;
        this.clockFrequency = clockFrequency;
        this.clockMode = clockMode;
        this.wordLength = wordLength;
        this.bitOrdering = bitOrdering;
        checkParameters();
    }

    /**
     * Creates a new {@code SPIDeviceConfig} with the specified hardware addressing information and configuration
     * parameters. The Chip Select active level is platform and/or driver-dependent (i.e. {@link #DEFAULT}).
     *
     * @param controllerName
     *            the controller name (such as its <em>device file</em> name on UNIX systems).
     * @param address
     *            the Chip Select address of the slave device on the bus (a positive or zero integer).
     * @param clockFrequency
     *            the clock frequency of the slave device in Hz (a positive integer) or {@link #DEFAULT}.
     * @param clockMode
     *            the clock mode, one of: {@code 0}, {@code 1}, {@code 2} or {@code 4} (see <a href="#mode">SPI Clock
     *            Modes</a>).
     * @param wordLength
     *            the word length of the slave device (a positive integer) or {@link #DEFAULT}.
     * @param bitOrdering
     *            the bit (shifting) ordering of the slave device, one of: {@link SPIDevice#BIG_ENDIAN},
     *            {@link SPIDevice#LITTLE_ENDIAN}, {@link SPIDevice#MIXED_ENDIAN} or {@link #DEFAULT}.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code address} is not in the defined range;</li>
     *             <li>{@code clockFrequency} is not in the defined range;</li>
     *             <li>{@code clockMode} is not one of the defined values;</li>
     *             <li>{@code wordLength} is not in the defined range;</li>
     *             <li>{@code bitOrdering} is not one of the defined values.</li>
     *             </ul>
     * @throws NullPointerException
     *             if {@code controllerName} is {@code null}.
     */
    public SPIDeviceConfig(String controllerName, int address, int clockFrequency, int clockMode, int wordLength,
            int bitOrdering) {
        this(controllerName, address, DEFAULT, clockFrequency, clockMode, wordLength, bitOrdering);
    }

    /**
     * Creates a new {@code SPIDeviceConfig} with the specified hardware addressing information and configuration
     * parameters.
     *
     * @param controllerName
     *            the controller name (such as its <em>device file</em> name on UNIX systems).
     * @param address
     *            the Chip Select address of the slave device on the bus (a positive or zero integer).
     * @param csActive
     *            the Chip Select active level, one of {@link #CS_ACTIVE_LOW},
     *            {@link #CS_ACTIVE_HIGH}, {@link #CS_NOT_CONTROLLED} or {@link #DEFAULT}.
     * @param clockFrequency
     *            the clock frequency of the slave device in Hz (a positive integer) or {@link #DEFAULT}.
     * @param clockMode
     *            the clock mode, one of: {@code 0}, {@code 1}, {@code 2} or {@code 4} (see <a href="#mode">SPI Clock
     *            Modes</a>).
     * @param wordLength
     *            the word length of the slave device (a positive integer) or {@link #DEFAULT}.
     * @param bitOrdering
     *            the bit (shifting) ordering of the slave device, one of: {@link SPIDevice#BIG_ENDIAN},
     *            {@link SPIDevice#LITTLE_ENDIAN}, {@link SPIDevice#MIXED_ENDIAN} or {@link #DEFAULT}.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code address} is not in the defined range;</li>
     *             <li>{@code clockFrequency} is not in the defined range;</li>
     *             <li>{@code clockMode} is not one of the defined values;</li>
     *             <li>{@code wordLength} is not in the defined range;</li>
     *             <li>{@code bitOrdering} is not one of the defined values.</li>
     *             </ul>
     * @throws NullPointerException
     *             if {@code controllerName} is {@code null}.
     */
    public SPIDeviceConfig(String controllerName, int address, int csActive, int clockFrequency, int clockMode, int wordLength,
            int bitOrdering) {
        // checks for null
        controllerName.length();
        this.controllerName = controllerName;
        this.address = address;
        this.csActive = csActive;
        this.clockFrequency = clockFrequency;
        this.clockMode = clockMode;
        this.wordLength = wordLength;
        this.bitOrdering = bitOrdering;
        checkParameters();
    }

    /**
     * Gets the configured address of the SPI slave device.
     *
     * @return the Chip Select address of the slave device on the bus (a positive or zero integer).
     */
    public int getAddress() {
        return address;
    }

    /**
     * Gets the configured controller number (the controller number of the SPI bus adapter the slave device is connected to).
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
     * Gets the configured bit (shifting) ordering of the SPI slave device.
     *
     * @return the bit ordering of the slave device, one of: {@link SPIDevice#BIG_ENDIAN},
     *         {@link SPIDevice#LITTLE_ENDIAN}, {@link SPIDevice#MIXED_ENDIAN} or {@link #DEFAULT}.
     */
    public int getBitOrdering() {
        return bitOrdering;
    }

    /**
     * Gets the clock frequency (in Hz) supported by the SPI slave device.
     *
     * @return the clock frequency of the slave device in Hz (a positive integer) or {@link #DEFAULT}.
     */
    public int getClockFrequency() {
        return clockFrequency;
    }

    /**
     * Gets the configured clock mode (combining clock polarity and phase) for communicating with the SPI slave device.
     *
     * @return the clock mode, one of: {@code 0}, {@code 1}, {@code 2} or {@code 4} (see <a href="#mode">SPI Clock
     *         Modes</a>).
     */
    public int getClockMode() {
        return clockMode;
    }

    /**
     * Gets the configured Chip Select active level for selecting the SPI slave device.
     *
     * @return the Chip Select active level,one of {@link #CS_ACTIVE_LOW},
     *            {@link #CS_ACTIVE_HIGH}, {@link #CS_NOT_CONTROLLED} or {@link #DEFAULT}.
     */
    public int getCSActiveLevel() {
        return csActive;
    }

    /**
     * Gets the configured word length for communicating with the SPI slave device.
     *
     * @return the word length of the slave device (a positive integer) or {@link #DEFAULT}.
     */
    public int getWordLength() {
        return wordLength;
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.controllerName);
        hash = 47 * hash + this.address;
        hash = 47 * hash + this.controllerNumber;
        hash = 47 * hash + this.bitOrdering;
        hash = 47 * hash + this.clockFrequency;
        hash = 47 * hash + this.clockMode;
        hash = 47 * hash + this.csActive;
        hash = 47 * hash + this.wordLength;
        return hash;
    }

    /**
     * Checks two {@code SPIDeviceConfig} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     *
     * @return {@code true} if {@code obj} is a {@code SPIDeviceConfig} and has
     * the same hardware addressing information and configuration parameter values
     * as this {@code SPIDeviceConfig} object.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SPIDeviceConfig other = (SPIDeviceConfig) obj;
        if (!Objects.equals(this.controllerName, other.controllerName)) {
            return false;
        }
        if (this.address != other.address) {
            return false;
        }
        if (this.controllerNumber != other.controllerNumber) {
            return false;
        }
        if (this.bitOrdering != other.bitOrdering) {
            return false;
        }
        if (this.clockFrequency != other.clockFrequency) {
            return false;
        }
        if (this.clockMode != other.clockMode) {
            return false;
        }
        if (this.csActive != other.csActive) {
            return false;
        }
        if (this.wordLength != other.wordLength) {
            return false;
        }
        return true;
    }

    private void checkParameters(){
        if ((null == controllerName && DEFAULT > controllerNumber) ||
            address < 0 ||
            (csActive != DEFAULT && csActive != CS_ACTIVE_LOW && csActive != CS_ACTIVE_HIGH && csActive != CS_NOT_CONTROLLED) ||
            (clockFrequency < DEFAULT || clockFrequency == 0) ||
            (clockMode < 0 || clockMode > 4) ||
            (wordLength < DEFAULT || wordLength == 0) ||
            (bitOrdering != DEFAULT && bitOrdering != SPIDevice.BIG_ENDIAN &&
            bitOrdering != SPIDevice.LITTLE_ENDIAN && bitOrdering != SPIDevice.MIXED_ENDIAN) ) {
            throw new IllegalArgumentException();
        }
    }
}
