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

import java.io.IOException;

import jdk.dio.Device;
import jdk.dio.ClosedDeviceException;
import jdk.dio.UnavailableDeviceException;

/**
 * The {@code GenericDevice} interface defines methods for setting and getting
 * device-specific configuration and access (I/O) controls as well as registering event listeners.
 * <p/>
 * A generic device may be identified by the numeric ID and by the name (if any defined) that
 * correspond to its registered configuration. An {@code GenericDevice} instance can be opened by a
 * call to one of the {@link jdk.dio.DeviceManager#open(int)
 * DeviceManager.open(id,...)} methods using its ID or by a call to one of the
 * {@link jdk.dio.DeviceManager#open(java.lang.String, java.lang.Class, java.lang.String[])
 * DeviceManager.open(name,...)} methods using its name.
 * <p />
 * An application can set and get configuration and access (I/O) controls. A control is identified
 * by a {@link GenericDeviceControl} instance and can be set or gotten using the
 * {@link #setControl setControl} and
 * {@link #getControl getControl} methods. Controls can
 * be used to configured a device a well as performing basic input/output operations. The
 * list of controls supported by a device is device-specific.
 * <p />
 * An application can also register an {@link GenericEventListener} instance to monitor native
 * events of the designated type fired by the device. To register a
 * {@link GenericEventListener} instance, the application must call the
 * {@link #setEventListener setEventListener} method. The registered listener can later on
 * be removed by calling the same method with a {@code null} listener parameter. Asynchronous
 * notification may not be supported by all devices. An attempt to set a listener on a device which
 * does not supports it will result in an {@link UnsupportedOperationException} being thrown.
 * <p />
 * A platform implementer may allow through this interface access and control of devices
 * which do not require read and write operations and for which there exist no other more specific
 * API such as {@link jdk.dio.gpio.GPIOPin} or
 * {@link jdk.dio.gpio.GPIOPort}.
 *
 * @see GenericEventListener
 * @see GenericDeviceControl
 * @see GenericPermission
 * @since 1.0
 */
public interface GenericDevice extends Device<GenericDevice> {

    /**
     * Gets the value of the specified control.
     *
     * @param <T>
     *            the type of the control's value.
     * @param control
     *            the control to get.
     * @return the value of the specified control.
     * @throws IllegalArgumentException
     *             if {@code control} is not recognized or invalid.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    <T> T getControl(GenericDeviceControl<T> control) throws IOException, UnavailableDeviceException,
            ClosedDeviceException;

    /**
     * Sets the value of the specified control.
     *
     * @param <T>
     *            the type of the control's value.
     * @param control
     *            the control to set.
     * @param value
     *            the value to set.
     * @throws IllegalArgumentException
     *             if {@code control} is not recognized or invalid.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    <T> void setControl(GenericDeviceControl<T> control, T value) throws IOException, UnavailableDeviceException,
            ClosedDeviceException;

    /**
     * Registers a {@link GenericEventListener} instance to monitor native events of the designated
     * type fired by the device associated to this {@code GenericDevice} object. While
     * the listener can be triggered by hardware interrupts, there are no real-time guarantees of
     * when the listener will be called.
     * <p />
     * A list of event type IDs is defined in {@link GenericEvent}. This list can be extended with
     * device-specific IDs.
     * <p />
     * If this {@code GenericDevice} is open in
     * {@link jdk.dio.DeviceManager#SHARED} access mode the listeners registered
     * by all the applications sharing the underlying device will get notified of the events they
     * registered for.
     * <p />
     * If {@code listener} is {@code null} then listener previously registered for the specified
     * event type will be removed.
     * <p />
     * Only one listener can be registered at a particular time for a particular event type.
     *
     * @param eventId
     *            ID of the native event to listen to.
     * @param listener
     *            the {@link GenericEventListener} instance to be notified upon occurrence of the
     *            designated event.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalArgumentException
     *             if {@code eventId} does not correspond to any supported event.
     * @throws UnsupportedOperationException
     *             if this {@code GenericDevice} object does not support asynchronous event
     *             notification.
     * @throws IllegalStateException
     *             if {@code listener} is not {@code null} and a listener is already registered for
     *             the specified event type.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void setEventListener(int eventId, GenericEventListener listener) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;
}
