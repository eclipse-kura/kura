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

package jdk.dio.gpio;

import jdk.dio.DeviceConfig;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.DeviceManager;
import java.util.Objects;
import com.oracle.dio.utils.ExceptionMessage;
import serializator.*;
import romizer.*;
/**
 * The {@code GPIOPinConfig} class encapsulates the hardware addressing information, and static and
 * dynamic configuration parameters of a GPIO pin.
 * <p />
 * Some hardware addressing parameter, and static and dynamic configuration parameters may be set to
 * {@link #DEFAULT}. Whether such default settings are supported is platform- as well as device
 * driver-dependent.
 * <p />
 * An instance of {@code GPIOPinConfig} can be passed to the
 * {@link DeviceManager#open(DeviceConfig)} or
 * {@link DeviceManager#open(Class, DeviceConfig)} method to open the designated GPIO pin
 * with the specified configuration. A {@link InvalidDeviceConfigException} is thrown when
 * attempting to open a device with an invalid or unsupported configuration.
 *
 * @see DeviceManager#open(DeviceConfig)
 * @see DeviceManager#open(Class, DeviceConfig)
 * @since 1.0
 */
@SerializeMe
public final class GPIOPinConfig implements  DeviceConfig<GPIOPin>, DeviceConfig.HardwareAddressing {

    /**
     * Bidirectional port direction with initial input direction.
     */
    public static final int DIR_BOTH_INIT_INPUT = 2;
    /**
     * Bidirectional port direction with initial output direction.
     */
    public static final int DIR_BOTH_INIT_OUTPUT = 3;
    /**
     * Input port direction.
     */
    public static final int DIR_INPUT_ONLY = 0;
    /**
     * Output port direction.
     */
    public static final int DIR_OUTPUT_ONLY = 1;
    /**
     * Input pull-down drive mode.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other drive mode bit flags.
     */
    public static final int MODE_INPUT_PULL_DOWN = 2;
    /**
     * Input pull-up drive mode.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other drive mode bit flags.
     */
    public static final int MODE_INPUT_PULL_UP = 1;
    /**
     * Output open-drain drive mode.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other drive mode bit flags.
     */
    public static final int MODE_OUTPUT_OPEN_DRAIN = 8;
    /**
     * Output push-pull drive mode.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other drive mode bit flags.
     */
    public static final int MODE_OUTPUT_PUSH_PULL = 4;
    /**
     * Rising edge trigger.
     */
    public static final int TRIGGER_BOTH_EDGES = 3;
    /**
     * Both levels trigger.
     */
    public static final int TRIGGER_BOTH_LEVELS = 6;
    /**
     * Falling edge trigger.
     */
    public static final int TRIGGER_FALLING_EDGE = 1;
    /**
     * High level trigger.
     */
    public static final int TRIGGER_HIGH_LEVEL = 4;
    /**
     * Low level trigger.
     */
    public static final int TRIGGER_LOW_LEVEL = 5;
    /**
     * No interrupt trigger.
     */
    public static final int TRIGGER_NONE = 0;
    /**
     * Rising edge trigger.
     */
    public static final int TRIGGER_RISING_EDGE = 2;

    private String controllerName;
    private int direction;
    private boolean initValue;
    private int mode;
    private int pinNumber = DEFAULT;
    private int controllerNumber = DEFAULT;
    private int trigger;

    // hidden constructor for serializer
    @DontRenameMethod
    GPIOPinConfig() {}

    /**
     * Creates a new {@code GPIOPinConfig} with the specified hardware addressing information and
     * configuration parameters.
     *
     * @param controllerNumber
     *            the hardware port's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param pinNumber
     *            the hardware pin's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param direction
     *            the allowed and initial direction of the pin, one of: {@link #DIR_INPUT_ONLY},
     *            {@link #DIR_OUTPUT_ONLY}, {@link #DIR_BOTH_INIT_INPUT},
     *            {@link #DIR_BOTH_INIT_OUTPUT}.
     * @param mode
     *            the drive mode of the pin: either {@link #DEFAULT} or a bitwise OR of at least one
     *            of: {@link #MODE_INPUT_PULL_UP}, {@link #MODE_INPUT_PULL_DOWN} ,
     *            {@link #MODE_OUTPUT_PUSH_PULL}, {@link #MODE_OUTPUT_OPEN_DRAIN}; if the pin can be
     *            set in both input and output direction then the mode must specify both an input
     *            drive mode and an output drive mode (bit mask).
     * @param trigger
     *            the initial interrupt trigger events, one of: {@link #TRIGGER_NONE},
     *            {@link #TRIGGER_FALLING_EDGE}, {@link #TRIGGER_RISING_EDGE},
     *            {@link #TRIGGER_BOTH_EDGES}, {@link #TRIGGER_HIGH_LEVEL},
     *            {@link #TRIGGER_LOW_LEVEL}, {@link #TRIGGER_BOTH_LEVELS}.
     * @param initValue
     *            the initial value of the pin when initially set for output; ignored otherwise.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code controllerNumber} is not in the defined range;</li>
     *             <li>{@code pinNumber} is not in the defined range;</li>
     *             <li>{@code direction} is not one of the defined values;</li>
     *             <li>{@code trigger} is not one of the defined values;</li>
     *             <li>{@code mode} does not designate any mode (i.e. equals {@code 0});</li>
     *             <li>{@code mode} designates more than one input or output drive mode;</li>
     *             <li>{@code mode} does not designates a drive mode for or designates a drive mode
     *             incompatible with the direction(s) designated by {@code direction}.</li>
     *             </ul>
     */
    public GPIOPinConfig(int controllerNumber, int pinNumber, int direction, int mode, int trigger, boolean initValue) {
        this.controllerNumber = controllerNumber;
        this.pinNumber = pinNumber;
        if (controllerNumber < DeviceConfig.DEFAULT || pinNumber < DeviceConfig.DEFAULT ) {
            throw new IllegalArgumentException();
        }
        this.direction = direction;
        this.trigger = trigger;
        this.mode = mode;
        this.initValue = initValue;
        checkValues(direction, mode, trigger);
    }

    /**
     * Creates a new {@code GPIOPinConfig} with the specified hardware addressing information and
     * configuration parameters.
     *
     * @param controllerName
     *            the controller name (such as its <em>device file</em> name on UNIX systems).
     * @param pinNumber
     *            the hardware pin's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param direction
     *            the allowed and initial direction of the pin, one of: {@link #DIR_INPUT_ONLY},
     *            {@link #DIR_OUTPUT_ONLY}, {@link #DIR_BOTH_INIT_INPUT},
     *            {@link #DIR_BOTH_INIT_OUTPUT}.
     * @param mode
     *            the drive mode of the pin: either {@link #DEFAULT} or a bitwise OR of at least one
     *            of: {@link #MODE_INPUT_PULL_UP}, {@link #MODE_INPUT_PULL_DOWN} ,
     *            {@link #MODE_OUTPUT_PUSH_PULL}, {@link #MODE_OUTPUT_OPEN_DRAIN}; if the pin can be
     *            set in both input and output direction then the mode must specify both an input
     *            drive mode and an output drive mode (bit mask).
     * @param trigger
     *            the initial interrupt trigger events, one of: {@link #TRIGGER_NONE},
     *            {@link #TRIGGER_FALLING_EDGE}, {@link #TRIGGER_RISING_EDGE},
     *            {@link #TRIGGER_BOTH_EDGES}, {@link #TRIGGER_HIGH_LEVEL},
     *            {@link #TRIGGER_LOW_LEVEL}, {@link #TRIGGER_BOTH_LEVELS}.
     * @param initValue
     *            the initial value of the pin when initially set for output; ignored otherwise.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code pinNumber} is not in the defined range;</li>
     *             <li>{@code direction} is not one of the defined values;</li>
     *             <li>{@code trigger} is not one of the defined values;</li>
     *             <li>{@code mode} does not designate any mode (i.e. equals {@code 0});</li>
     *             <li>{@code mode} designates more than one input or output drive mode;</li>
     *             <li>{@code mode} does not designates a drive mode for or designates a drive mode
     *             incompatible with the direction(s) designated by {@code direction}.</li>
     *             </ul>
     * @throws NullPointerException
     *             if {@code controllerName} is {@code null}.
     */
    public GPIOPinConfig(String controllerName, int pinNumber, int direction, int mode, int trigger, boolean initValue) {
        this.controllerName = controllerName;
        this.pinNumber = pinNumber;
        controllerName.length();// NPE check
        if (pinNumber < DeviceConfig.DEFAULT) {
            throw new IllegalArgumentException();
        }
        this.direction = direction;
        this.trigger = trigger;
        this.mode = mode;
        this.initValue = initValue;
        checkValues(direction, mode, trigger);
    }

    /**
     * Gets the configured pin direction.
     *
     * @return the pin direction, one of: {@link #DIR_INPUT_ONLY}, {@link #DIR_OUTPUT_ONLY},
     *         {@link #DIR_BOTH_INIT_INPUT}, {@link #DIR_BOTH_INIT_OUTPUT}.
     */
    public int getDirection() {
        return direction;
    }

    /**
     * Gets the configured pin drive mode.
     *
     * @return the pin drive mode: either {@link #DEFAULT} or a bitwise OR of at least one of :
     *         {@link #MODE_INPUT_PULL_UP}, {@link #MODE_INPUT_PULL_DOWN},
     *         {@link #MODE_OUTPUT_PUSH_PULL}, {@link #MODE_OUTPUT_OPEN_DRAIN}.
     */
    public int getDriveMode() {
        return mode;
    }

    /**
     * Gets the configured initial value of the pin, if configured for output.
     *
     * @return the pin's initial output value; {@code false} if configured for input.
     */
    public boolean getInitValue() {
        return initValue;
    }

    /**
     * Gets the configured pin number.
     *
     * @return the hardware pin's number (a positive or zero integer) or {@link #DEFAULT}.
     */
    public int getPinNumber() {
        return pinNumber;
    }

    /**
     * Gets the configured controller number for the pin.
     *
     * @return the hardware port's number (a positive or zero integer) or {@link #DEFAULT}.
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
     * Gets the configured initial pin interrupt trigger.
     *
     * @return the pin interrupt trigger, one of: {@link #TRIGGER_NONE},
     *         {@link #TRIGGER_FALLING_EDGE}, {@link #TRIGGER_RISING_EDGE},
     *         {@link #TRIGGER_BOTH_EDGES}, {@link #TRIGGER_HIGH_LEVEL}, {@link #TRIGGER_LOW_LEVEL},
     *         {@link #TRIGGER_BOTH_LEVELS}.
     */
    public int getTrigger() {
        return trigger;
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.controllerName);
        hash = 59 * hash + this.direction;
        hash = 59 * hash + (this.initValue ? 1 : 0);
        hash = 59 * hash + this.mode;
        hash = 59 * hash + this.pinNumber;
        hash = 59 * hash + this.controllerNumber;
        hash = 59 * hash + this.trigger;
        return hash;
    }

    /**
     * Checks two {@code GPIOPinConfig} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     * @return {@code true} if {@code obj} is a {@code GPIOPinConfig} and has the same hardware
     *         addressing information and configuration parameter values as this
     *         {@code GPIOPinConfig} object.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GPIOPinConfig other = (GPIOPinConfig) obj;
        if (!Objects.equals(this.controllerName, other.controllerName)) {
            return false;
        }
        if (this.direction != other.direction) {
            return false;
        }
        if (this.initValue != other.initValue) {
            return false;
        }
        if (this.mode != other.mode) {
            return false;
        }
        if (this.pinNumber != other.pinNumber) {
            return false;
        }
        if (this.controllerNumber != other.controllerNumber) {
            return false;
        }
        if (this.trigger != other.trigger) {
            return false;
        }
        return true;
    }

    private void checkValues(int direction, int mode, int trigger) {
        if ( GPIOPinConfig.TRIGGER_NONE > trigger || GPIOPinConfig.TRIGGER_BOTH_LEVELS < trigger || 0 == mode ) {
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.GPIO_TRIGGER_OR_MODE)
            );
        }

        switch (direction) {
        case GPIOPinConfig.DIR_INPUT_ONLY:
            if (DeviceConfig.DEFAULT != mode &&
                (0 !=  (mode & ~(GPIOPinConfig.MODE_INPUT_PULL_DOWN | GPIOPinConfig.MODE_INPUT_PULL_UP)) ||
                mode > GPIOPinConfig.MODE_INPUT_PULL_DOWN )) {
                throw new IllegalArgumentException(
                    ExceptionMessage.format(ExceptionMessage.GPIO_MODE_NOT_FOR_DIRINPUTONLY)
                );
            }
            break;
        case GPIOPinConfig.DIR_OUTPUT_ONLY:
            if (DeviceConfig.DEFAULT != mode &&
                (0 != (mode & ~(GPIOPinConfig.MODE_OUTPUT_OPEN_DRAIN | GPIOPinConfig.MODE_OUTPUT_PUSH_PULL )) ||
                mode > GPIOPinConfig.MODE_OUTPUT_OPEN_DRAIN) ) {
                throw new IllegalArgumentException(
                    ExceptionMessage.format(ExceptionMessage.GPIO_MODE_NOT_FOR_DIROUTPUTONLY)
                );
            }
            break;
        case GPIOPinConfig.DIR_BOTH_INIT_INPUT:
        case GPIOPinConfig.DIR_BOTH_INIT_OUTPUT:
            if (DeviceConfig.DEFAULT != mode &&
               ((mode != (MODE_INPUT_PULL_DOWN | MODE_OUTPUT_OPEN_DRAIN)) &&
                (mode != (MODE_INPUT_PULL_DOWN | MODE_OUTPUT_PUSH_PULL)) &&
                (mode != (MODE_INPUT_PULL_UP   | MODE_OUTPUT_OPEN_DRAIN)) &&
                (mode != (MODE_INPUT_PULL_UP   | MODE_OUTPUT_PUSH_PULL))) )  {
                throw new IllegalArgumentException(
                    ExceptionMessage.format(ExceptionMessage.GPIO_MODE_NOT_FOR_DIRBOTH, mode)
                );
            }
            break;
        default:
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.GPIO_INVALID_DIRECTION)
            );
        }
    }
}
