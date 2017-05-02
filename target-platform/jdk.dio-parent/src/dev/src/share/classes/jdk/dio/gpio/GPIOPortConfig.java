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

import jdk.dio.DeviceManager;
import jdk.dio.DeviceConfig;
import jdk.dio.InvalidDeviceConfigException;
import com.oracle.dio.utils.ExceptionMessage;
import com.oracle.dio.gpio.impl.GPIOPinFake;
import serializator.*;
import romizer.DontRenameMethod;

/**
 * The {@code GPIOPortConfig} class encapsulates the hardware addressing information, and static and
 * dynamic configuration parameters of a GPIO port.
 * <p />
 * Some hardware addressing parameter, and static and dynamic configuration parameters may be set to
 * {@link #DEFAULT}. Whether such default settings are supported is platform- as well as device
 * driver-dependent.
 * <p />
 * An instance of {@code GPIOPortConfig} can be passed to the
 * {@link DeviceManager#open(DeviceConfig)} or
 * {@link DeviceManager#open(Class, DeviceConfig)} method to open the designated GPIO port
 * with the specified configuration. A {@link InvalidDeviceConfigException} is thrown when
 * attempting to open a device with an invalid or unsupported configuration.
 * <p />
 * The value change notification trigger of a GPIO port is defined by the interrupt trigger(s) configured
 * for its pins (see {@link GPIOPinConfig#getTrigger GPIOPinConfig.getTrigger}). Any of
 * the {@code GPIOPin}s configured with an interrupt trigger (other than {@link GPIOPinConfig#TRIGGER_NONE})
 * that compose a {@code GPIOPort} may trigger a notification for that {@code GPIOPort}.
 *
 * @see DeviceManager#open(DeviceConfig)
 * @see DeviceManager#open(Class, DeviceConfig)
 * @since 1.0
 */
@SerializeMe
public final class GPIOPortConfig implements DeviceConfig<GPIOPort> {

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
    // private int portNumber = DEFAULT;
    private int direction;
    private int initValue;
    private GPIOPinConfig[] pinConfigs;
    private GPIOPinFake[] pins;

    // hidden constructor for serializer
    @DontRenameMethod
    GPIOPortConfig() {}


    /**
     * Creates a new {@code GPIOPortConfig} with the specified hardware addressing information and
     * configuration parameters.
     * <p />
     * If the access modes (exclusive or shared) supported by the designated
     * {@code GPIOPin}s are incompatible with those required by the underlying {@code GPIOPort}
     * device or device driver, attempting to open
     * the {@code GPIOPort} device using this configuration may result in a
     * {@link InvalidDeviceConfigException} to be thrown.
     *
     * @param direction
     *            the allowed and initial direction of the port, one of: {@link #DIR_INPUT_ONLY},
     *            {@link #DIR_OUTPUT_ONLY}, {@link #DIR_BOTH_INIT_INPUT},
     *            {@link #DIR_BOTH_INIT_OUTPUT}.
     * @param initValue
     *            the initial value of the port when initially set for output.
     * @param pins
     *            the pin configurations in the exact same order they compose the port.
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code direction} is not one of the defined values;</li>
     *             <li>{@code pins.length} is {@code 0};</li>
     *             <li>if any of the provided pin configurations does not support the specified
     *             direction.</li>
     *             </ul>
     * @throws NullPointerException
     *             if {@code pins} is {@code null}.
     */
    public GPIOPortConfig(int direction, int initValue, GPIOPinConfig[] pins) {
        checkValues(direction, initValue, pins);
        this.direction = direction;
        this.initValue = initValue;
        this.pinConfigs = new GPIOPinConfig[pins.length];
        System.arraycopy(pins, 0, this.pinConfigs, 0, pins.length);
    }

    private void checkValues(int direction, int initValue, GPIOPinConfig[] pins) {
        if (DIR_INPUT_ONLY > direction ||
            DIR_BOTH_INIT_OUTPUT < direction ||
            pins.length == 0 ) {
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.GPIO_ILLEGAL_DIRECTION_OR_INIT_VALUE)
            );
        }

        for (GPIOPinConfig pin : pins) {
            if (pin.getDirection() != direction &&
                pin.getDirection() < DIR_BOTH_INIT_INPUT) {
                throw new IllegalArgumentException(
                    ExceptionMessage.format(ExceptionMessage.GPIO_DIR_UNSUPPORTED_BY_PIN_CONFIG)
                );
            }
        }
    }

    /**
     * Gets the configured port number.
     *
     * @return the hardware port's number (a positive or zero integer) or {@link #DEFAULT} if no
     *         port number was specified.
     */
    // public int getPortNumber() {
    // return portNumber;
    // }

    /**
     * Gets the configured port direction.
     *
     * @return the port direction, one of: {@link #DIR_INPUT_ONLY}, {@link #DIR_OUTPUT_ONLY},
     *         {@link #DIR_BOTH_INIT_INPUT}, {@link #DIR_BOTH_INIT_OUTPUT}.
     */
    public int getDirection() {
        return direction;
    }

    /**
     * Gets the configured default/initial value of the port, if configured for output.
     *
     * @return the port's default/initial output value.
     */
    public int getInitValue() {
        return initValue;
    }

    /**
     * Gets the configured configurations of the pins composing the port (in the exact same order
     * they compose the port).
     *
     * @return the pins composing the port (a defensive copy is returned).
     */
    public GPIOPinConfig[] getPinConfigs() {
        if (pinConfigs != null) {
            GPIOPinConfig[] clone = new GPIOPinConfig[pinConfigs.length];
            System.arraycopy(pinConfigs, 0, clone, 0, pinConfigs.length);
            return clone;
        }
        return null;
    }

    /**
     * Gets the pins composing the port (in the exact same order they compose the port).
     * <p />
     * A concurrent runtime change of the dynamic configuration parameters of any of the pins composing the port (such
     * as of its direction) may result in {@code IOException} being thrown by port operations.
     *
     * @return the pins composing the port (a defensive copy is returned); or {@code null}
     * if this {@code GPIOPortConfig} instance is not associated to an actual GPIOPort instance - that is the
     * GPIOPortConfig instance was not retrieved from a call to getDescriptor().getConfiguration() on the {@code
     * GPIOPort} instance.
     */
    public GPIOPin[] getPins() {
        if (pins != null) {
            GPIOPin[] clone = new GPIOPin[pins.length];
            System.arraycopy(pins, 0, clone, 0, pins.length);
            return clone;
        }
        return null;
    }
}
