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

import jdk.dio.ClosedDeviceException;
import jdk.dio.Device;
import jdk.dio.DeviceManager;
import jdk.dio.UnavailableDeviceException;
import java.io.IOException;

/**
 * The {@code GPIOPin} interface provides methods for controlling a GPIO pin.
 * <p />
 * A GPIO pin may be identified by the numeric ID and by the name (if any defined) that correspond
 * to its registered configuration. A {@code GPIOPin} instance can be opened by a call to one of the
 * {@link DeviceManager#open(int) DeviceManager.open(id,...)} methods using its ID or by a
 * call to one of the
 * {@link DeviceManager#open(java.lang.String, java.lang.Class, java.lang.String[])
 * DeviceManager.open(name,...)} methods using its name. When a {@code GPIOPin} instance is
 * opened with an ad-hoc {@link GPIOPinConfig} configuration (which includes its hardware addressing
 * information) using one of the
 * {@link DeviceManager#open(jdk.dio.DeviceConfig)
 * DeviceManager.open(config,...)} it is not assigned any ID nor name.
 * <p />
 * A GPIO pin may be configured for output or input. Output pins are both writable and readable
 * while input pins are only readable.
 * <p />
 * Once opened, an application can obtain the current value of a GPIO pin by calling the
 * {@link #getValue getValue} method and set its value by calling the {@link #setValue setValue}
 * method.
 * <p/>
 * An application can either monitor a GPIO pin value changes using polling or can register a
 * {@link PinListener} instance which will get asynchronously notified of any pin value changes. To
 * register a {@link PinListener} instance, the application must call the {@link #setInputListener
 * setInputListener} method. The registered listener can later on be removed by calling the same
 * method with a {@code null} listener parameter. Asynchronous notification is only supported for
 * GPIO pin configured for input. An attempt to set a listener on a GPIO pin configured for output
 * will result in an {@link UnsupportedOperationException} being thrown.
 * <p />
 * When an application is no longer using a GPIO pin it should call the {@link #close close} method
 * to close the GPIO pin. Any further attempt to set or get the value of a GPIO pin which has been
 * closed will result in a {@link ClosedDeviceException} been thrown.
 * <p />
 * The initial direction of a GPIO pin which may be used for output or input as well as
 * the initial value of a GPIO pin set for output is configuration-specific. An application should
 * always initially set the GPIO pin's direction; or first query the GPIO pin's direction then set
 * it if necessary.
 * <p />
 * The underlying platform configuration may allow for some GPIO pins to be set by an application for either
 * output or input while others may be used for input only or output only and their direction cannot
 * be changed by the application. Asynchronous notification of pin value changes is
 * only loosely tied to hardware-level interrupt requests. The platform does not guarantee
 * notification in a deterministic/timely manner.
 *
 * @see PinListener
 * @see GPIOPinPermission
 * @since 1.0
 */
public interface GPIOPin extends Device<GPIOPin> {

    /**
     * Input port direction.
     */
    int INPUT = 0;
    /**
     * Output port direction.
     */
    int OUTPUT = 1;

    /**
     * Returns the current direction of this GPIO pin. If the direction was not set previously using
     * {@link #setDirection setDirection} the device configuration-specific default value is
     * returned.
     *
     * @return {@link GPIOPin#OUTPUT} if this GPIO pin is currently set as output;
     *         {@link GPIOPin#INPUT} otherwise (the GPIO pin is set as input).
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int getDirection() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Returns the current pin interrupt trigger. If the trigger mode was not set previously using
     * {@link #setTrigger setTrigger} the device configuration-specific default value is
     * returned. Setting the trigger mode to {@link GPIOPinConfig#TRIGGER_NONE} disables
     * the notification of the {@link PinListener} instance (see {@link #setInputListener
     * setInputListener} without unregistering it.
     *
     * @return the current pin interrupt trigger, one of: {@link GPIOPinConfig#TRIGGER_NONE},
     *         {@link GPIOPinConfig#TRIGGER_FALLING_EDGE}, {@link GPIOPinConfig#TRIGGER_RISING_EDGE}
     *         , {@link GPIOPinConfig#TRIGGER_BOTH_EDGES}, {@link GPIOPinConfig#TRIGGER_HIGH_LEVEL},
     *         {@link GPIOPinConfig#TRIGGER_LOW_LEVEL}, {@link GPIOPinConfig#TRIGGER_BOTH_LEVELS}.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int getTrigger() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Returns the current value of this GPIO pin. If the value was not set previously using
     * {@link #setValue setValue} the device configuration-specific default value is returned.
     * <p />
     * This method can be called on both output and input pins.
     *
     * @return true if this pin is currently <em>high</em>.
     * @throws IOException
     *             if an I/O error occurred such as the pin is not readable.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    boolean getValue() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets this GPIO pin for output or input.
     * <p />
     * An attempt to set the direction of a GPIO pin to a value that is not supported by the
     * platform configuration will result in a {@link UnsupportedOperationException} being thrown.
     *
     * @param direction
     *            {@link GPIOPin#OUTPUT} for output; {@link GPIOPin#INPUT} for input.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnsupportedOperationException
     *             if this GPIO pin cannot be configured for the desired direction.
     * @throws IllegalArgumentException
     *             if {@code direction} is not equal to {@link GPIOPin#OUTPUT} or
     *             {@link GPIOPin#INPUT}.
     * @throws SecurityException
     *             if the caller does not have the required permission.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void setDirection(int direction) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets this GPIO pin trigger mode.
     *
     * @param trigger
     *            the interrupt trigger events, one of: {@link GPIOPinConfig#TRIGGER_NONE},
     *            {@link GPIOPinConfig#TRIGGER_FALLING_EDGE},
     *            {@link GPIOPinConfig#TRIGGER_RISING_EDGE},
     *            {@link GPIOPinConfig#TRIGGER_BOTH_EDGES}, {@link GPIOPinConfig#TRIGGER_HIGH_LEVEL}
     *            , {@link GPIOPinConfig#TRIGGER_LOW_LEVEL},
     *            {@link GPIOPinConfig#TRIGGER_BOTH_LEVELS}.
     * @throws IOException
     *             if an I/O error occurred.
     * @throws UnsupportedOperationException
     *             if this GPIO pin cannot be configured with the desired trigger mode.
     * @throws IllegalArgumentException
     *             if {@code trigger} is not one of the defined values.
     * @throws SecurityException
     *             if the caller does not have the required permission.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void setTrigger(int trigger) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Registers a {@link PinListener} instance which will get asynchronously notified when this
     * GPIO pin's value changes and according to the current trigger mode (see {@link #getTrigger
     * getTrigger}). Notification will automatically begin after registration completes.
     * <p />
     * If this {@code GPIOPin} is open in {@link DeviceManager#SHARED} access mode and if this
     * {@code GPIOPin} is currently configured for input, the listeners registered by all the
     * applications sharing the underlying GPIO pin will get notified when its value changes.
     * <p />
     * A listener can only be registered for a GPIO pin currently configured for input.
     * <p />
     * If {@code listener} is {@code null} then the previously registered listener is removed.
     * <p />
     * Only one listener can be registered at a particular time.
     *
     * @param listener
     *            the {@link PinListener} instance to be notified when this GPIO pin's value
     *            changes.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnsupportedOperationException
     *             if this GPIO pin is currently configured for output.
     * @throws IllegalStateException
     *             if {@code listener} is not {@code null} and a listener is already registered.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void setInputListener(PinListener listener) throws IOException, ClosedDeviceException;

    /**
     * Sets the value of this GPIO pin.
     * <p />
     * An attempt to set the value on a GPIO pin currently configured for input will result in a
     * {@link UnsupportedOperationException} being thrown.
     *
     * @param value
     *            the new pin value: {@code true} for <em>high</em>, {@code false} for <em>low</em>.
     * @throws UnsupportedOperationException
     *             if trying to set the value for pin configured for input.
     * @throws IOException
     *             if an I/O error occurred such as the pin is not writable.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void setValue(boolean value) throws IOException, UnavailableDeviceException, ClosedDeviceException;
}
