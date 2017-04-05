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

import jdk.dio.Device;
import jdk.dio.DeviceManager;
import jdk.dio.ClosedDeviceException;
import jdk.dio.UnavailableDeviceException;
import java.io.IOException;

/**
 * The {@code PulseCounter} interface provides methods for controlling a pulse counter. A pulse
 * counter can count pulses (or events) on a digital input line (possibly a GPIO pin).
 * <p />
 * A pulse counter may be identified by the numeric ID and by the name (if any defined) that
 * correspond to its registered configuration. A {@code PulseCounter} instance can be opened by a
 * call to one of the {@link DeviceManager#open(int) DeviceManager.open(id,...)} methods
 * using its ID or by a call to one of the
 * {@link DeviceManager#open(java.lang.String, java.lang.Class, java.lang.String[])
 * DeviceManager.open(name,...)} methods using its name. When a {@code PulseCounter} instance is
 * opened with an ad-hoc {@link PulseCounterConfig} configuration (which includes its hardware
 * addressing information) using one of the
 * {@link DeviceManager#open(jdk.dio.DeviceConfig)
 * DeviceManager.open(config,...)} it is not assigned any ID nor name.
 * <p />
 * Once opened, an application can either start a pulse counting session using the
 * {@link #startCounting() startCounting} method and retrieve the current pulse count on-the-fly
 * by calling the {@link #getCount getCount} method; or it can start a pulse counting session with a
 * terminal count value and a counting time interval using the
 * {@link #startCounting(int, long, jdk.dio.counter.CountingListener) } and get
 * asynchronously notified once the terminal count value has been reached or the counting time
 * interval has expired. In both cases, the application can retrieve the current pulse count at any
 * time (on-the-fly) by calling the {@link #getCount getCount}.
 * <p />
 * The pulse counting session can be suspended by calling {@link #suspendCounting suspendCounting}
 * and later on resumed from its previous count value by calling {@link #resumeCounting
 * resumeCounting}. Suspending the pulse counting also suspends the session counting time interval
 * timer if active.
 * <p />
 * The pulse count value can be reset at any time during counting by calling {@link #resetCounting
 * resetCounting}. This also resets the session counting time interval timer if active.
 * <p />
 * Finally, the pulse counting can be stopped by calling {@link #stopCounting stopCounting}.
 * <p />
 * When an application is no longer using a pulse counter it should call the {@link #close close}
 * method to close the pulse counter. Any further attempt to use a pulse counter which has been
 * closed will result in a {@link ClosedDeviceException} been thrown.
 * <p />
 * Asynchronous notification of pulse counting conditions is only loosely tied to
 * hardware-level interrupt requests. The platform does not guarantee notification in a
 * deterministic/timely manner.
 *
 * @see CountingListener
 * @see CounterPermission
 * @since 1.0
 */
public interface PulseCounter extends Device<PulseCounter> {

    /**
     * Gets the pulse count measured so far during the current (if still active) or previous
     * counting session.
     *
     * @return the pulse count measured so far during the current or previous counting session;
     *         {@code 0} is returned if none has been measured so far.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int getCount() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Resets the current count value.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if counting is not active.
     */
    void resetCounting() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Resumes the counting starting from the <i>frozen</i> count value.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if counting is not active.
     */
    void resumeCounting() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Starts a pulse counting session.
     * <p />
     * The pulse count value is first reset.
     * <p />
     * If the counter overflows it restarts from {@code 0} without any further notification. To be
     * notified of such conditions the
     * {@link #startCounting(int, long, jdk.dio.counter.CountingListener) } method
     * should be used instead.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if counting is already active.
     */
    void startCounting() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Starts an asynchronous pulse counting session. The provided {@link CountingListener} instance
     * will be asynchronously invoked when the pulse count reaches the provided terminal count value
     * or the specified counting time interval expires - whichever happens first.
     * <p />
     * The pulse count value is first reset and will be reset every time the terminal count value is
     * reached or the counting time interval expires.
     * <p />
     * If {@code limit} is equal to or less than {@code 0} then the counting time interval will end
     * only after the time specified by {@code interval} has passed. If {@code interval} is equal to
     * or less than {@code 0} then the counting time interval will end only after the pulse count
     * has reached the terminal count value specified by {@code limit}. The counting time interval
     * is expressed in microseconds; if the underlying platform does not support a microsecond timer
     * resolution then {@code interval} will be <em>rounded up</em> to accommodate the supported timer
     * resolution.
     * <p/>
     * Pulse counting and notification will immediately start and will repeat until it is stopped by
     * a call to {@link #stopCounting stopCounting}.
     * <p />
     * Only one pulse counting session can be going on at any time.
     *
     * @param listener
     *            the {@link CountingListener} instance to be notified when the pulse count reaches
     *            the terminal count value.
     * @param limit
     *            the terminal count value.
     * @param interval
     *            the counting time interval (in microseconds).
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalArgumentException
     *             if {@code limit} and {@code interval} are both equal to or less than {@code 0}.
     * @throws NullPointerException
     *             if {@code listener} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if counting is already active.
     */
    void startCounting(int limit, long interval, CountingListener listener) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * Stops the pulse counting and freezes the current count value. The count value will be reset
     * upon the next start.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void stopCounting() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Suspends the pulse counting and freezes the current count value.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalStateException
     *             if counting is not active.
     */
    void suspendCounting() throws IOException, UnavailableDeviceException, ClosedDeviceException;
}
