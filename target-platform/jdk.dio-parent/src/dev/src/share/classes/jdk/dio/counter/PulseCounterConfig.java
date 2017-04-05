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

package jdk.dio.counter;

import java.util.Objects;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceDescriptor;
import jdk.dio.DeviceManager;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;

import com.oracle.dio.utils.ExceptionMessage;

import romizer.*;
import serializator.*;

/**
 * The {@code PulseCounterConfig} class encapsulates the hardware addressing information, and static
 * and dynamic configuration parameters of a pulse counter.
 * <p />
 * Some hardware addressing parameter, and static and dynamic configuration parameters may be set to
 * {@link #DEFAULT}. Whether such default settings are supported is platform- as well as device
 * driver-dependent.
 * <p />
 * An instance of {@code PulseCounterConfig} can be passed to the
 * {@link DeviceManager#open(DeviceConfig)} or
 * {@link DeviceManager#open(Class, DeviceConfig)} method to open the designated counter
 * with the specified configuration. A {@link InvalidDeviceConfigException} is thrown when
 * attempting to open a device with an invalid or unsupported configuration.
 *
 * @see DeviceManager#open(DeviceConfig)
 * @see DeviceManager#open(Class, DeviceConfig)
 * @since 1.0
 */
@SerializeMe
public class PulseCounterConfig implements DeviceConfig<PulseCounter>, DeviceConfig.HardwareAddressing {

    /**
     * Falling pulse edge (counting only falling pulse edges). When the pulse source is a GPIO pin
     * then this pulse edge type can only be used if the pin is configured with the
     * {@link GPIOPinConfig#TRIGGER_FALLING_EDGE} trigger mode.
     */
    public static final int TYPE_FALLING_EDGE_ONLY = 0;

    /**
     * Negative edge pulse: measured from falling edge to rising edge (counting well-formed negative
     * edge pulses). When the pulse source is a GPIO pin then this pulse edge type can only be used
     * if the pin is configured with the {@link GPIOPinConfig#TRIGGER_BOTH_EDGES} trigger mode.
     */
    public static final int TYPE_NEGATIVE_PULSE = 3;
    /**
     * Positive edge pulse: measured from rising edge to falling edge (counting well-formed positive
     * edge pulses). When the pulse source is a GPIO pin then this pulse edge type can only be used
     * if the pin is configured with the {@link GPIOPinConfig#TRIGGER_BOTH_EDGES} trigger mode.
     */
    public static final int TYPE_POSITIVE_PULSE = 2;

    /**
     * Rising pulse edge (counting only rising pulse edges). When the pulse source is a GPIO pin
     * then this pulse edge type can only be used if the pin is configured with the
     * {@link GPIOPinConfig#TRIGGER_RISING_EDGE} trigger mode.
     */
    public static final int TYPE_RISING_EDGE_ONLY = 1;

    private String controllerName;

    private int controllerNumber = DEFAULT;

    private int channelNumber = DEFAULT;

    private GPIOPinConfig sourceConfig;

    private GPIOPin source;

    private int type;

    // hidden constructor for serializer
    @DontRenameMethod
    PulseCounterConfig(){}

    /**
     * Creates a new {@code PulseCounterConfig} with the specified hardware addressing information
     * and type. The source of the pulse counter is implicit (such as a dedicated input pin).
     *
     * @param controllerNumber
     *            the hardware controller's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param channelNumber
     *            the hardware counter's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param type
     *            the pulse or pulse edge type: {@link #TYPE_FALLING_EDGE_ONLY},
     *            {@link #TYPE_RISING_EDGE_ONLY}, {@link #TYPE_NEGATIVE_PULSE} or
     *            {@link #TYPE_POSITIVE_PULSE}.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code controllerNumber} is not in the defined range;</li>
     *             <li>{@code channelNumber} is not in the defined range;</li>
     *             <li>{@code type} is not one of the defined values.</li>
     *             </ul>
     */
    public PulseCounterConfig(int controllerNumber, int channelNumber, int type) {
        this.controllerNumber = controllerNumber;
        this.channelNumber = channelNumber;
        this.type = type;
        checkValues();
    }

    /**
     * Creates a new {@code PulseCounterConfig} the specified hardware addressing information, type
     * and GPIO pin source.
     *
     * @param controllerNumber
     *            the hardware controller's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param channelNumber
     *            the hardware counter's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param type
     *            the pulse or pulse edge type: {@link #TYPE_FALLING_EDGE_ONLY},
     *            {@link #TYPE_RISING_EDGE_ONLY}, {@link #TYPE_NEGATIVE_PULSE} or
     *            {@link #TYPE_POSITIVE_PULSE}.
     * @param source
     *            the configuration of the source (a GPIO input pin) on which the pulses are to be
     *            counted.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code controllerNumber} is not in the defined range;</li>
     *             <li>{@code channelNumber} is not in the defined range;</li>
     *             <li>{@code type} is not one of the defined values;</li>
     *             <li>if the provided source is not configured for input;</li>
     *             <li>if the provided source is not configured with an edge trigger mode compatible
     *             with the specified pulse type.</li>
     *             </ul>
     * @throws NullPointerException
     *             if {@code source} is {@code null}.
     */
    public PulseCounterConfig(int controllerNumber, int channelNumber, int type, GPIOPinConfig source) {
        this.controllerNumber = controllerNumber;
        this.channelNumber = channelNumber;
        this.type = type;
        this.sourceConfig = source;
        // check for null
        source.getDirection();
        checkValues();
    }

    /**
     * Creates a new {@code PulseCounterConfig} with the specified hardware addressing information
     * and type. The source of the pulse counter is implicit (such as a dedicated input pin).
     *
     * @param controllerName
     *            the controller name (such as its <em>device file</em> name on UNIX systems).
     * @param channelNumber
     *            the hardware counter's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param type
     *            the pulse or pulse edge type: {@link #TYPE_FALLING_EDGE_ONLY},
     *            {@link #TYPE_RISING_EDGE_ONLY}, {@link #TYPE_NEGATIVE_PULSE} or
     *            {@link #TYPE_POSITIVE_PULSE}.
     * @throws NullPointerException
     *             if {@code controllerName} is {@code null}.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code channelNumber} is not in the defined range;</li>
     *             <li>{@code type} is not one of the defined values.</li>
     *             </ul>
     */
    public PulseCounterConfig(String controllerName, int channelNumber, int type) {
        // checks for null
        controllerName.length();
        this.controllerName = controllerName;
        this.channelNumber = channelNumber;
        this.type = type;
        checkValues();
    }

    /**
     * Creates a new {@code PulseCounterConfig} the specified hardware addressing information, type
     * and GPIO pin source.
     *
     * @param controllerName
     *            the controller name (such as its <em>device file</em> name on UNIX systems).
     * @param channelNumber
     *            the hardware counter's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param type
     *            the pulse or pulse edge type: {@link #TYPE_FALLING_EDGE_ONLY},
     *            {@link #TYPE_RISING_EDGE_ONLY}, {@link #TYPE_NEGATIVE_PULSE} or
     *            {@link #TYPE_POSITIVE_PULSE}.
     * @param source
     *            the configuration of the source (a GPIO input pin) on which the pulses are to be
     *            counted.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code channelNumber} is not in the defined range;</li>
     *             <li>{@code type} is not one of the defined values;</li>
     *             <li>if the provided source is not configured for input;</li>
     *             <li>if the provided source is not configured with an edge trigger mode compatible
     *             with the specified pulse type.</li>
     *             </ul>
     * @throws NullPointerException
     *             if {@code controllerName} or {@code source} is {@code null}.
     */
    public PulseCounterConfig(String controllerName, int channelNumber, int type, GPIOPinConfig source) {
        // checks for null
        controllerName.length();
        this.controllerName = controllerName;
        this.channelNumber = channelNumber;
        this.type = type;
        this.sourceConfig = source;
        // checks for null
        source.getClass();
        checkValues();
    }

    /**
     * Gets the configured controller number.
     *
     * @return the controller number (a positive or zero integer); or {@link #DEFAULT}.
     */
    @Override
    public int getControllerNumber() {
        return controllerNumber;
    }

    /**
     * Gets the configured counter number.
     *
     * @return the counter number (a positive or zero integer); or {@link #DEFAULT}.
     */
    public int getChannelNumber() {
        return channelNumber;
    }

    /**
     * Gets the configured controller name (such as its <em>device file</em> name on UNIX systems).
     *
     * @return the controllerName or {@code null}.
     */
    @Override
    public String getControllerName() {
        return controllerName;
    }

    /**
     * Gets the input source on which the pulses are to be counted/measured.
     * <p />
     * A concurrent runtime change of the
     * dynamic configuration parameters of the source (such as of its direction) may result in
     * {@code IOException} being thrown by counting operations.
     *
     * @return the source on which the pulses are to be counted/measured; or {@code null} if the source is implicit
     * or if this {@code PusleCounterConfig} instance is not associated to an actual {@code PulseCounter} instance.
     */
    public GPIOPin getSource() {
        return source;
    }

    /**
     * Gets the configured input source configuration on which the pulses are to be
     * counted/measured.
     *
     * @return the configuration of the source on which the pulses are to be counted/measured; or
     *         {@code null} if the source is implicit.
     */
    public GPIOPinConfig getSourceConfig() {
        return sourceConfig;
    }

    /**
     * Gets the configured pulse or pulse edge type.
     *
     * @return the pulse or pulse edge type: {@link #TYPE_FALLING_EDGE_ONLY},
     *         {@link #TYPE_RISING_EDGE_ONLY}, {@link #TYPE_NEGATIVE_PULSE} or
     *         {@link #TYPE_POSITIVE_PULSE}.
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.controllerName);
        hash = 83 * hash + this.controllerNumber;
        hash = 83 * hash + this.channelNumber;
        hash = 83 * hash + Objects.hashCode(this.sourceConfig);
        hash = 83 * hash + Objects.hashCode(this.source);
        hash = 83 * hash + this.type;
        return hash;
    }

    /**
     * Checks two {@code PulseCounterConfig} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     * @return {@code true} if {@code obj} is a {@code PulseCounterConfig} and has the same hardware
     *         addressing information and configuration parameter values as this
     *         {@code PulseCounterConfig} object.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PulseCounterConfig other = (PulseCounterConfig) obj;
        if (!Objects.equals(this.controllerName, other.controllerName)) {
            return false;
        }
        if (this.controllerNumber != other.controllerNumber) {
            return false;
        }
        if (this.channelNumber != other.channelNumber) {
            return false;
        }
        if (!Objects.equals(this.sourceConfig, other.sourceConfig)) {
            return false;
        }
        if (!Objects.equals(this.source, other.source)) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        return true;
    }

    private void checkValues() throws IllegalArgumentException {
        if (controllerNumber < DEFAULT) {
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.COUNTER_INVALID_CONTROLLER_NUMBER)
            );
        }
        if(channelNumber < DEFAULT) {
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.COUNTER_INVALID_CHANNEL_NUMBER)
            );
        }
        if (TYPE_NEGATIVE_PULSE < type || TYPE_FALLING_EDGE_ONLY > type) {
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.COUNTER_INVALID_TYPE)
            );
        }
        GPIOPinConfig cfg =  sourceConfig;

        if (null != cfg) {
            if ((cfg.getDirection() != GPIOPinConfig.DIR_INPUT_ONLY &&
                cfg.getDirection() != GPIOPinConfig.DIR_BOTH_INIT_OUTPUT &&
                cfg.getDirection() != GPIOPinConfig.DIR_BOTH_INIT_INPUT) ||
                // rising and falling edges
                ((cfg.getTrigger() >> 1) != type &&
                //When the pulse source is a GPIO pin  then this pulse edge type can only be used if the pin is configured with the
                // GPIOPinConfig#TRIGGER_BOTH_EDGES trigger mode
                (type > TYPE_RISING_EDGE_ONLY && cfg.getTrigger() != GPIOPinConfig.TRIGGER_BOTH_EDGES))
                ) {
                throw new IllegalArgumentException(
                    ExceptionMessage.format(ExceptionMessage.COUNTER_CONFIG_CANNOT_BE_USED)
                );
            }
        }
    }

}
