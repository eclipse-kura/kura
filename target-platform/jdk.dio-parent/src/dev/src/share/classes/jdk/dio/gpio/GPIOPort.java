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
 * The {@code GPIOPort} interface provides methods for controlling a GPIO port.
 * <p />
 * Each GPIO port is identified by a numeric ID and optionally by a name.
 * <p />
 * A GPIO port is a platform-defined or an ad-hoc grouping of GPIO pins that may be configured for output or
 * input. Output ports are both writable and readable while input ports are only readable.
 * Whether GPIO pins that are part of a platform-defined GPIO port can be retrieved and controlled individually as
 * {@link GPIOPin} instances depends on the hardware and platform configuration (and especially
 * whether the GPIO pins can be shared through different abstractions).
 * <p />
 * A GPIO port may be identified by the numeric ID and by the name (if any defined) that
 * correspond to its registered configuration. A {@code GPIOPort} instance can be opened by a call
 * to one of the {@link DeviceManager#open(int) DeviceManager.open(id,...)} methods using
 * its ID or by a call to one of the
 * {@link DeviceManager#open(java.lang.String, java.lang.Class, java.lang.String[])
 * DeviceManager.open(name,...)} methods using its name. When a {@code GPIOPort} instance is
 * opened with an ad-hoc {@link GPIOPortConfig} configuration (which includes its hardware
 * addressing information) using one of the
 * {@link DeviceManager#open(jdk.dio.DeviceConfig)
 * DeviceManager.open(config,...)} it is not assigned any ID nor name.
 * <p />
 * Once opened, an application can obtain the current value of a GPIO port by calling the
 * {@link #getValue getValue} method and set its value by calling the {@link #setValue setValue}
 * method. <br />
 * A GPIO port has a minimum/maximum value range. The minimum value is zero. An application can
 * check the maximum value by calling {@link #getMaxValue getMaxValue} method. An attempt to set a
 * GPIO port with a value that exceeds its maximum range value will result in an
 * {@link IllegalArgumentException} being thrown.
 * <p/>
 * An application can either monitor a GPIO port value changes using polling or can register a
 * {@link PortListener} instance which will get asynchronously notified of any value changes. To
 * register a {@link PortListener} instance, the application must call the {@link #setInputListener
 * setInputListener} method. The registered listener can later on be removed by calling the same
 * method with a {@code null} listener parameter. Asynchronous notification is only supported for
 * GPIO port configured for input. An attempt to set a listener on a GPIO port configured for output
 * will result in an {@link UnsupportedOperationException} being thrown.
 * <p />
 * When an application is no longer using a GPIO port it should call the {@link #close close} method
 * to close the GPIO port. Any further attempt to set or get the value of a GPIO port which has been
 * closed will result in a {@link ClosedDeviceException} been thrown.
 * <p />
 * The initial direction of a GPIO port which may be used for output or input as well as
 * the initial value of a GPIO port set for output is configuration-specific. An application should
 * always initially set the GPIO port's direction; or first query the GPIO port's direction then set
 * it if necessary.
 * <p />
 * The underlying platform configuration may allow for some GPIO ports to be set by an application for either
 * output or input while others may be used for input only or output only and their direction cannot
 * be changed by an application. Asynchronous notification of port value changes is
 * only loosely tied to hardware-level interrupt requests. The platform does not guarantee
 * notification in a deterministic/timely manner.
 *
 * @see PortListener
 * @see GPIOPortPermission
 * @since 1.0
 */
public interface GPIOPort extends Device<GPIOPort> {

    /**
     * Input port direction.
     */
    int INPUT = 0;
    /**
     * Output port direction.
     */
    int OUTPUT = 1;

    /**
     * Returns the current direction of this GPIO port. If the direction was not set previously
     * using {@link #setDirection setDirection} the device configuration-specific default value
     * is returned.
     *
     * @return {@link GPIOPort#OUTPUT} if this GPIO port is currently set as output;
     *         {@link GPIOPort#INPUT} otherwise (the GPIO port is set as input).
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
     * Returns the maximum value of this GPIO port. The value returned should be interpreted as an
     * unsigned 32-bit integer.
     *
     * @return the maximum value this GPIO port can handle.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int getMaxValue() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Returns the current value of this GPIO port. The value returned should be interpreted as an
     * unsigned 32-bit integer. If the value was not set previously using {@link #setValue setValue}
     * the device configuration-specific default value is returned.
     * <p />
     * This method can be called on both output and input ports.
     *
     * @return the current value of this GPIO port.
     * @throws IOException
     *             if an I/O error occurred such as the port is not readable.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int getValue() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets this GPIO port for output or input.
     * <p />
     * An attempt to set the direction of a GPIO port to a value that is not supported by the
     * platform configuration will result in a {@link UnsupportedOperationException} being thrown.
     *
     * @param direction
     *            {@link GPIOPort#OUTPUT} for output; {@link GPIOPort#INPUT} for input.
     * @throws IllegalArgumentException
     *             if {@code direction} is not equal to {@link GPIOPort#OUTPUT} or
     *             {@link GPIOPort#INPUT}.
     * @throws UnsupportedOperationException
     *             if this GPIO port cannot be configured for the desired direction.
     * @throws SecurityException
     *             if the caller does not have the required permission.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void setDirection(int direction) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Registers a {@link PortListener} instance which will get asynchronously notified when this
     * GPIO port's value changes and according to its trigger mode.
     * Notification will automatically begin after registration completes.
     * <p />
     * A listener can only be registered for a GPIO port currently configured for input.
     * <p />
     * If this {@code GPIOPort} is open in {@link DeviceManager#SHARED} access mode and if this
     * {@code GPIOPort} is currently configured for input, the listeners registered by all the
     * applications sharing the underlying GPIO port will get notified when its value changes.
     * <p />
     * If {@code listener} is {@code null} then the previously registered listener is removed.
     * <p />
     * Only one listener can be registered at a particular time.
     *
     * @param listener
     *            the {@link PortListener} instance to be notified when this GPIO port's value
     *            changes.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnsupportedOperationException
     *             if this GPIO port is currently configured for output.
     * @throws IllegalStateException
     *             if {@code listener} is not {@code null} and a listener is already registered.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void setInputListener(PortListener listener) throws IOException, ClosedDeviceException;

    /**
     * Sets the value of this GPIO port. The value passed should be interpreted as an unsigned
     * 32-bit integer.
     * <p />
     * An attempt to set the value on a GPIO port currently configured for input will result in a
     * {@link UnsupportedOperationException} being thrown.
     *
     * @param value
     *            the new port value.
     * @throws IllegalArgumentException
     *             if the new value exceeds this GPIO port's maximum value.
     * @throws UnsupportedOperationException
     *             if trying to set the value for a port configured for input.
     * @throws IOException
     *             if an I/O error occurred such as the port is not writable.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void setValue(int value) throws IOException, UnavailableDeviceException, ClosedDeviceException;
}
