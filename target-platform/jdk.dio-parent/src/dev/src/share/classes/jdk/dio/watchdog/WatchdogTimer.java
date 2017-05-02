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

package jdk.dio.watchdog;

import jdk.dio.Device;
import jdk.dio.DeviceManager;
import jdk.dio.ClosedDeviceException;
import jdk.dio.UnavailableDeviceException;
import java.io.IOException;

/**
 * The {@code WatchdogTimer} interface provides methods for controlling a watchdog timer that forces the
 * device to restart (or depending on the platform, the Java Virtual Machine to restart).
 * <p />
 * A {@code WatchdogTimer} instance may represent a virtual watchdog timer. If the device has a single physical watchdog
 * timer, all of the virtual watchdog timers are mapped onto this one physical watchdog timer. It gets set to expire
 * when the virtual watchdog with the earliest timeout is scheduled to expire. The corresponding watchdog timer
 * device is therefore shared and several applications can concurrently acquire the same watchdog timer device.
 * <p />
 * A watchdog timer may be identified by the numeric ID and by the name (if any defined) that correspond to its
 * registered configuration. A {@code WatchdogTimer} instance can be opened by a call to one of the
 * {@link DeviceManager#open(int) DeviceManager.open(id,...)} methods using its ID or by a call to one of the
 * {@link DeviceManager#open(java.lang.String, java.lang.Class, java.lang.String[])
 * DeviceManager.open(name,...)} methods using its name. <br />
 * If a watchdog timer is virtualized, a particular platform implementation may allow for several {@code WatchdogTimer}
 * instances representing each a virtual instance of that same physical watchdog timer to be opened concurrently using
 * the same device ID, or,
 * alternatively, it may assign each virtual watchdog timer instance a distinct device ID (and a common name). <br
 * />
 * <p />
 * Once the device opened, the application can start using it and can especially start the timer using the
 * {@link jdk.dio.watchdog.WatchdogTimer#start(long) WatchdogTimer.start} method and subsequently
 * refresh the timer periodically using the {@link jdk.dio.watchdog.WatchdogTimer#refresh
 * WatchdogTimer.refresh} method
 * <p/>
 * When done, an application should call the {@link #close WatchdogTimer.close} method to close the watchdog timer.
 * Any further attempt to access or control a watchdog timer which has been closed will result in a
 * {@link ClosedDeviceException} been thrown.
 *
 */
public interface WatchdogTimer extends Device<WatchdogTimer> {

    /**
     * Checks if the last device restart (or JVM restart) was caused by the watchdog timing out.
     *
     * @return true if the watchdog timer caused the last device restart (or JVM restart).
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    public boolean causedLastReboot() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Gets the maximum timeout that can be set for the watchdog timer.
     *
     * @return the maximum time interval (in milliseconds) that can be set for the watchdog timer.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    public long getMaxTimeout() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Get the current timeout for the watchdog timer. A value of zero indicates that the watchdog timer is disabled.
     *
     * @return the time interval (in milliseconds) until watchdog times out.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    public long getTimeout() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Refreshes the watchdog timer. This method must be called periodically to prevent the watchdog from timing out and
     * restarting the device (or restarting the JVM).
     * <p />
     * This method has no effect if the timer is currently disabled.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    public void refresh() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Starts the watchdog timer with the specified timeout. If the watchdog timer is not refreshed by a call to
     * {@link #refresh refresh} before the watchdog timing out, the device will be restarted (or the JVM restarted).
     * <p />
     * Calling this method twice is equivalent to stopping the timer as per a call to {@link #stop} and starting
     * it again with the new specified timeout.
     *
     * @param timeout
     *            the time interval (in milliseconds) until watchdog times out.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalArgumentException
     *             if {@code timeout} is not greater than {@code 0}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    public void start(long timeout) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Stops this watchdog timer.
     * <p />
     * This method returns silently if the timer is already disabled.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    public void stop() throws IOException, UnavailableDeviceException, ClosedDeviceException;
}
