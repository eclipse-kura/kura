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

package jdk.dio.pwm;

import java.io.IOException;
import java.util.Objects;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;
import com.oracle.dio.utils.ExceptionMessage;

import romizer.*;
import serializator.*;

/**
 * The {@code PWMChannelConfig} class encapsulates the hardware addressing information, and static and dynamic
 * configuration parameters of a PWM channel.
 * <p />
 * Some hardware addressing parameter, and static and dynamic configuration parameters may be set to {@link #DEFAULT}.
 * Whether such default settings are supported is platform- as well as device driver-dependent.
 * <p />
 * An instance of {@code PWMChannelConfig} can be passed to the {@link DeviceManager#open(DeviceConfig)} or
 * {@link DeviceManager#open(Class, DeviceConfig)} method to open the designated PWM channel with the specified
 * configuration. A {@link InvalidDeviceConfigException} is thrown when attempting to open a device with
 * an invalid or unsupported configuration.
 *
 * @see DeviceManager#open(DeviceConfig)
 * @see DeviceManager#open(Class, DeviceConfig)
 * @since 1.0
 */
@SerializeMe
public class PWMChannelConfig implements DeviceConfig<PWMChannel>, DeviceConfig.HardwareAddressing {

    /**
     * High idle state. The idle state is the state of the PWM output when no pulse is generated.
     */
    public static final int IDLE_STATE_HIGH = 0;
    /**
     * Low idle state. The idle state is the state of the PWM output when no pulse is generated.
     */
    public static final int IDLE_STATE_LOW = 1;
    /**
     * Center alignement. The pulse is centered within the pulse period.
     */
    public static final int ALIGN_CENTER = 0;
    /**
     * Left alignement. The start of the pulse coincides with the start of the pulse period.
     */
    public static final int ALIGN_LEFT = 1;
    /**
     * Right alignement. The end of the pulse coincides with the end of the pulse period.
     */
    public static final int ALIGN_RIGHT = 2;

    private String controllerName;
    private int controllerNumber = DEFAULT;
    private int channelNumber = DEFAULT;
    private GPIOPinConfig outputConfig ;
    private GPIOPin output;
    private int idleState = DEFAULT;
    private int pulsePeriod = DEFAULT;
    private int pulseAlignment = DEFAULT;

    // hidden constructor for serializer
    @DontRenameMethod
    PWMChannelConfig() {}

    /**
     * Creates a new {@code PWMChannelConfig} with the specified hardware addressing information and type. The output of
     * the PWM channel is implicit (such as a dedicated output pin).
     *
     * @param controllerNumber
     *            the hardware PWM controller (or generator)'s number (a positive or zero integer) or {@link #DEFAULT}.
     * @param channelNumber
     *            the hardware PWM channel's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param idleState
     *            the output idle state: {@link #IDLE_STATE_HIGH}, {@link #IDLE_STATE_LOW} or {@link #DEFAULT}.
     * @param pulsePeriod
     *            the default pulse period in microseconds (a positive integer) or {@link #DEFAULT}.
     * @param pulseAlignment
     *            the pulse alignment: {@link #ALIGN_CENTER}, {@link #ALIGN_LEFT}, {@link #ALIGN_RIGHT} or
     *            {@link #DEFAULT}.
     *
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code controllerNumber} is not in the defined range;</li>
     *             <li>{@code channelNumber} is not in the defined range;</li>
     *             <li>{@code idleState} is not in the defined range;</li>
     *             <li>{@code pulsePeriod} is not in the defined range.</li>
     *             <li>{@code pulseAlignment} is not in the defined range.</li>
     *             </ul>
     */
    public PWMChannelConfig(int controllerNumber, int channelNumber, int idleState, int pulsePeriod, int pulseAlignment) {
        this.controllerNumber = controllerNumber;
        this.channelNumber = channelNumber;
        this.idleState = idleState;
        this.pulsePeriod = pulsePeriod;
        this.pulseAlignment = pulseAlignment;
        checkParameters();
    }

    /**
     * Creates a new {@code PWMChannelConfig} the specified hardware addressing information, type and GPIO pin output.
     * <p />
     * If the access modes (exclusive or shared) supported by the designated
     * GPIO pin output are incompatible with those required by the underlying {@code PWMChannel}
     * device or device driver, attempting to open
     * the {@code PWMChannel} device using this configuration may result in a
     * {@link InvalidDeviceConfigException} to be thrown.
     *
     * @param controllerNumber
     *            the hardware PWM controller (or generator)'s number (a positive or zero integer) or {@link #DEFAULT}.
     * @param channelNumber
     *            the hardware PWM channel's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param idleState
     *            the output idle state: {@link #IDLE_STATE_HIGH}, {@link #IDLE_STATE_LOW} or {@link #DEFAULT}.
     * @param pulsePeriod
     *            the default pulse period in microseconds (a positive integer) or {@link #DEFAULT}.
     * @param pulseAlignment
     *            the pulse alignment: {@link #ALIGN_CENTER}, {@link #ALIGN_LEFT}, {@link #ALIGN_RIGHT} or
     *            {@link #DEFAULT}.
     * @param output
     *            the configuration of the output (a GPIO output pin) on which the pulses are to be generated.
     *
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code controllerNumber} is not in the defined range;</li>
     *             <li>{@code channelNumber} is not in the defined range;</li>
     *             <li>{@code idleState} is not in the defined range;</li>
     *             <li>{@code pulsePeriod} is not in the defined range.</li>
     *             <li>{@code pulseAlignment} is not in the defined range.</li>
     *             <li>if the provided GPIO pin is not configured for output.</li>
     *             </ul>
     * @throws NullPointerException
     *             if {@code output} is {@code null}.
     */
    public PWMChannelConfig(int controllerNumber, int channelNumber, int idleState, int pulsePeriod,
            int pulseAlignment, GPIOPinConfig output) {
        // checks for null as well
        if (output.getDirection() != GPIOPinConfig.DIR_OUTPUT_ONLY &&
            output.getDirection() != GPIOPinConfig.DIR_BOTH_INIT_OUTPUT){
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.PWM_OUTPUT_PIN_NOT_CONFIGURED)
            );
        }
        this.controllerNumber = controllerNumber;
        this.channelNumber = channelNumber;
        this.idleState = idleState;
        this.pulsePeriod = pulsePeriod;
        this.outputConfig = output;
        this.pulseAlignment = pulseAlignment;
        checkParameters();
    }

    /**
     * Creates a new {@code PWMChannelConfig} with the specified hardware addressing information and type. The output of
     * the PWM channel is implicit (such as a dedicated output pin).
     *
     * @param controllerName
     *            the controller name (such as its <em>device file</em> name on UNIX systems).
     * @param channelNumber
     *            the hardware PWM channel's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param idleState
     *            the output idle state: {@link #IDLE_STATE_HIGH}, {@link #IDLE_STATE_LOW} or {@link #DEFAULT}.
     * @param pulsePeriod
     *            the default pulse period in microseconds (a positive integer) or {@link #DEFAULT}.
     * @param pulseAlignment
     *            the pulse alignment: {@link #ALIGN_CENTER}, {@link #ALIGN_LEFT}, {@link #ALIGN_RIGHT} or
     *            {@link #DEFAULT}.
     *
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code channelNumber} is not in the defined range;</li>
     *             <li>{@code idleState} is not in the defined range;</li>
     *             <li>{@code pulsePeriod} is not in the defined range.</li>
     *             <li>{@code pulseAlignment} is not in the defined range.</li>
     *             </ul>
     * @throws NullPointerException
     *             if {@code controllerName} is {@code null}.
     */
    public PWMChannelConfig(String controllerName, int channelNumber, int idleState, int pulsePeriod, int pulseAlignment) {
        this(DEFAULT, channelNumber, idleState, pulsePeriod, pulseAlignment);
        // null check
        controllerName.length();
        this.controllerName = controllerName;
    }

    /**
     * Creates a new {@code PWMChannelConfig} the specified hardware addressing information, type and GPIO pin output.
     * <p />
     * If the access modes (exclusive or shared) supported by the designated
     * GPIO pin output are incompatible with those required by the underlying {@code PWMChannel}
     * device or device driver, attempting to open
     * the {@code PWMChannel} device using this configuration may result in a
     * {@link InvalidDeviceConfigException} to be thrown.
      *
     * @param controllerName
     *            the controller name (such as its <em>device file</em> name on UNIX systems).
     * @param channelNumber
     *            the hardware PWM channel's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param idleState
     *            the output idle state: {@link #IDLE_STATE_HIGH}, {@link #IDLE_STATE_LOW} or {@link #DEFAULT}.
     * @param pulsePeriod
     *            the default pulse period in microseconds (a positive integer) or {@link #DEFAULT}.
     * @param pulseAlignment
     *            the pulse alignment: {@link #ALIGN_CENTER}, {@link #ALIGN_LEFT}, {@link #ALIGN_RIGHT} or
     *            {@link #DEFAULT}.
     * @param output
     *            the configuration of the output (a GPIO output pin) on which the pulses are to be generated.
     *
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code channelNumber} is not in the defined range;</li>
     *             <li>{@code idleState} is not in the defined range;</li>
     *             <li>{@code pulsePeriod} is not in the defined range.</li>
     *             <li>{@code pulseAlignment} is not in the defined range.</li>
     *             <li>if the provided GPIO pin is not configured for output.</li>
     *             </ul>
     * @throws NullPointerException
     *             if {@code controllerName} or {@code output} is {@code null}.
     */
    public PWMChannelConfig(String controllerName, int channelNumber, int idleState, int pulsePeriod,
            int pulseAlignment, GPIOPinConfig output) {
       this(DEFAULT, channelNumber, idleState, pulsePeriod, pulseAlignment, output);
       // null check
       controllerName.length();
       this.controllerName = controllerName;
    }

    private void checkParameters(){
        if ( (null == controllerName && controllerNumber  < DEFAULT) ||
             (idleState != DEFAULT && idleState != IDLE_STATE_HIGH && idleState != IDLE_STATE_LOW) ||
             (pulsePeriod < DEFAULT || pulsePeriod == 0) ||
             (pulseAlignment != DEFAULT && pulseAlignment != ALIGN_CENTER && pulseAlignment != ALIGN_RIGHT && pulseAlignment != ALIGN_LEFT) ||
             channelNumber < DEFAULT ) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Gets the configured PWM channel number.
     *
     * @return the hardware channel's number (a positive or zero integer) or {@link #DEFAULT}.
     */
    public int getChannelNumber() {
        return channelNumber;
    }

    /**
     * Gets the configured controller number (the PWM controller or generator number).
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
     * @return the controllerName or {@code null}.
     */
    @Override
    public String getControllerName() {
        return controllerName;
    }

    /**
     * Gets the configured output configuration on which the pulses are to be generated.
     *
     * @return the output on which the pulses are to be generated; or {@code null} if the output is implicit.
     */
    public GPIOPinConfig getOutputConfig() {
        return outputConfig;
    }

    /**
     * Gets the output on which the pulses are to be generated.
     * <p />
     * A concurrent runtime change of the
     * dynamic configuration parameters of the output (such as of its direction) may result in
     * {@code IOException} being thrown by PWM operations.
     *
     * @return the output on which the pulses are to be generated; or {@code null} if the output is implicit
     * or if this {@code PWMChannelConfig} instance is not associated to an actual {@code PWMChannel} instance.
     */
    public GPIOPin getOutput() {
        return output;
    }

    /**
     * Gets the configured default/initial pulse period (in microseconds).
     *
     * @return the default/initial pulse period in microseconds (a positive integer) or {@link #DEFAULT}.
     */
    public int getPulsePeriod() {
        return pulsePeriod;
    }

    /**
     * Gets the configured idle output state.
     *
     * @return the idle output state: : {@link #IDLE_STATE_HIGH}, {@link #IDLE_STATE_LOW} or {@link #DEFAULT}.
     */
    public int getIdleState() {
        return idleState;
    }

    /**
     * Gets the configured pulse alignment. The alignment of the pulse within the pulse period.
     *
     * @return the pulse alignment: {@link #ALIGN_CENTER}, {@link #ALIGN_LEFT}, {@link #ALIGN_RIGHT} or {@link #DEFAULT}
     */
    public int getPulseAlignment() {
        return pulseAlignment;
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.controllerName);
        hash = 37 * hash + this.controllerNumber;
        hash = 37 * hash + this.channelNumber;
        hash = 37 * hash + Objects.hashCode(this.outputConfig);
        hash = 37 * hash + Objects.hashCode(this.output);
        hash = 37 * hash + this.idleState;
        hash = 37 * hash + this.pulsePeriod;
        hash = 37 * hash + this.pulseAlignment;
        return hash;
    }

    /**
     * Checks two {@code PWMChannelConfig} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     *
     * @return {@code true} if {@code obj} is a {@code PWMChannelConfig} and has
     * the same hardware addressing information and configuration parameter values
     * as this {@code PWMChannelConfig} object.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PWMChannelConfig other = (PWMChannelConfig) obj;
        if (!Objects.equals(this.controllerName, other.controllerName)) {
            return false;
        }
        if (this.controllerNumber != other.controllerNumber) {
            return false;
        }
        if (this.channelNumber != other.channelNumber) {
            return false;
        }
        if (!Objects.equals(this.outputConfig, other.outputConfig)) {
            return false;
        }
        if (!Objects.equals(this.output, other.output)) {
            return false;
        }
        if (this.idleState != other.idleState) {
            return false;
        }
        if (this.pulsePeriod != other.pulsePeriod) {
            return false;
        }
        if (this.pulseAlignment != other.pulseAlignment) {
            return false;
        }
        return true;
    }
}
