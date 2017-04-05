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

package jdk.dio.adc;

import jdk.dio.DeviceConfig;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.DeviceManager;
import java.util.Objects;

import serializator.*;
import romizer.DontRenameMethod;

/**
 * The {@code ADCChannelConfig} class encapsulates the hardware addressing information, and static
 * and dynamic configuration parameters of an ADC channel.
 * <p />
 * Some hardware addressing parameter, and static and dynamic configuration parameters may be set to
 * {@link #DEFAULT}. Whether such default settings are supported is platform- as well as device
 * driver-dependent.
 * <p />
 * An instance of {@code ADCChannelConfig} can be passed to the
 * {@link DeviceManager#open(DeviceConfig)} or
 * {@link DeviceManager#open(Class, DeviceConfig)} method to open the designated ADC channel
 * with the specified configuration. A {@link InvalidDeviceConfigException} is thrown when
 * attempting to open a device with an invalid or unsupported configuration.
 *
 * @see DeviceManager#open(DeviceConfig)
 * @see DeviceManager#open(Class, DeviceConfig)
 * @since 1.0
 */
@SerializeMe
public final class ADCChannelConfig implements DeviceConfig<ADCChannel>, DeviceConfig.HardwareAddressing {
    private String controllerName;
    private int channelNumber = DEFAULT;
    private int controllerNumber = DEFAULT;
    private int resolution = DEFAULT;
    private int samplingInterval = DEFAULT;
    private int samplingTime = DEFAULT;

    // hidden constructor for serializer
    @DontRenameMethod
    ADCChannelConfig() {}
    /**
     * Creates a new {@code ADCChannelConfig} with the specified hardware addressing information and
     * configuration parameters.
     *
     * @param controllerNumber
     *            the hardware converter's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param channelNumber
     *            the hardware channel's number (a positive or zero integer) or {@code DeviceConfig.DEFAULT}.
     * @param resolution
     *            the resolution in bits (a positive integer) or {@code DeviceConfig.DEFAULT}.
     * @param samplingInterval
     *            the initial input sampling interval (the amount of time between two samples) in
     *            microseconds (a positive integer) or {@code DeviceConfig.DEFAULT}.
     * @param samplingTime
     *            the sampling time (the amount of time to take the sample) in microseconds (a
     *            positive integer) or {@code DeviceConfig.DEFAULT}.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code controllerNumber} is not in the defined range;</li>
     *             <li>{@code channelNumber} is not in the defined range;</li>
     *             <li>{@code resolution} is not in the defined range;</li>
     *             <li>{@code samplingInterval} is not in the defined range;</li>
     *             <li>{@code samplingTime} is not in the defined range.</li>
     *             </ul>
     */
    public ADCChannelConfig(int controllerNumber, int channelNumber, int resolution, int samplingInterval, int samplingTime) {
        this.controllerNumber = controllerNumber;
        this.channelNumber = channelNumber;
        this.resolution = resolution;
        this.samplingInterval = samplingInterval;
        this.samplingTime = samplingTime;
        checkValues();
    }

    /**
     * Creates a new {@code ADCChannelConfig} with the specified hardware addressing information and
     * configuration parameters.
     *
     * @param controllerName
     *            the controller name (such as its <em>device file</em> name on UNIX systems).
     * @param channelNumber
     *            the hardware channel's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param resolution
     *            the resolution in bits (a positive integer) or {@code DeviceConfig.DEFAULT}.
     * @param samplingInterval
     *            the initial input sampling interval (the amount of time between two samples) in
     *            microseconds (a positive integer) or {@code DeviceConfig.DEFAULT}.
     * @param samplingTime
     *            the sampling time (the amount of time to take the sample) in microseconds (a
     *            positive integer) or {@code DeviceConfig.DEFAULT}.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code channelNumber} is not in the defined range;</li>
     *             <li>{@code resolution} is not in the defined range;</li>
     *             <li>{@code samplingInterval} is not in the defined range;</li>
     *             <li>{@code samplingTime} is not in the defined range.</li>
     *             </ul>
     * @throws NullPointerException
     *             if {@code controllerName} is {@code null}.
     */
    public ADCChannelConfig(String controllerName, int channelNumber, int resolution, int samplingInterval, int samplingTime) {
        this.controllerName = controllerName;
        this.channelNumber = channelNumber;
        this.resolution = resolution;
        this.samplingInterval = samplingInterval;
        this.samplingTime = samplingTime;
        // checks for null
        controllerName.length();
        checkValues();
    }

    /**
     * Gets the configured channel number.
     *
     * @return the hardware channel's number (a positive or zero integer) or {@link #DEFAULT}.
     */
    public int getChannelNumber() {
        return channelNumber;
    }

    /**
     * Gets the configured device number (such as the ADC converter number).
     *
     * @return the device number (a positive or zero integer) or {@link #DEFAULT}.
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
     * Gets the configured resolution.
     *
     * @return the resolution in bits (a positive integer) or {@link #DEFAULT}.
     */
    public int getResolution() {
        return resolution;
    }

    /**
     * Gets the configured default/initial input sampling interval - the amount of time between two
     * samples (in microseconds).
     *
     * @return the default/initial input sampling interval in microseconds (a positive integer) or
     *         {@link #DEFAULT}.
     */
    public int getSamplingInterval() {
        return samplingInterval;
    }

    /**
     * Gets the configured input sampling time - the amount of time to take the sample (in
     * microseconds).
     *
     * @return the input sampling time in microseconds (a positive integer) or {@link #DEFAULT}.
     */
    public int getSamplingTime() {
        return samplingTime;
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(this.controllerName);
        hash = 37 * hash + this.channelNumber;
        hash = 37 * hash + this.controllerNumber;
        hash = 37 * hash + this.resolution;
        hash = 37 * hash + this.samplingInterval;
        hash = 37 * hash + this.samplingTime;
        return hash;
    }

    /**
     * Checks two {@code ADCChannelConfig} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     * @return {@code true} if {@code obj} is a {@code ADCChannelConfig} and has the same hardware
     *         addressing information and configuration parameter values as this
     *         {@code ADCChannelConfig} object.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ADCChannelConfig other = (ADCChannelConfig) obj;
        if (!Objects.equals(this.controllerName, other.controllerName)) {
            return false;
        }
        if (this.channelNumber != other.channelNumber) {
            return false;
        }
        if (this.controllerNumber != other.controllerNumber) {
            return false;
        }
        if (this.resolution != other.resolution) {
            return false;
        }
        if (this.samplingInterval != other.samplingInterval) {
            return false;
        }
        if (this.samplingTime != other.samplingTime) {
            return false;
        }
        return true;
    }
    private void checkValues() throws IllegalArgumentException {
        if ((null == controllerName && DEFAULT > controllerNumber) ||
            DEFAULT > channelNumber ||
            DEFAULT > resolution || 0 == resolution ||
            DEFAULT > samplingInterval || 0 == samplingInterval ||
            DEFAULT > samplingTime || 0 == samplingTime) {
            throw new IllegalArgumentException();
        }
    }
}
