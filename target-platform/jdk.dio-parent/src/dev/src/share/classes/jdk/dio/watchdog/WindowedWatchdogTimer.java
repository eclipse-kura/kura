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

import jdk.dio.ClosedDeviceException;
import java.io.IOException;

/**
 * The {@code WindowedWatchdogTimer} interface provides methods for controlling a watchdog timer that can be used to
 * force the device to restart (or depending on the platform, the Java Virtual Machine to restart).
 * <p />
 * The windowed watchdog timer must be refreshed within an open time window. If the watchdog is refreshed too soon -
 * during the closed window - or if it is refreshed too late - after the watchdog timeout has expired - the device will
 * be restarted (or the JVM restarted).
 * <p />
 * A {@code WindowedWatchdogTimer} instance may represent a virtual windowed watchdog timer. If the device has a single
 * physical windowed watchdog timer, all of the virtual watchdog timers are mapped onto this one physical watchdog
 * timer. It gets set with a refresh window starting when the virtual windowed watchdog with the longest closed window
 * delay is scheduled to end and ending when the virtual windowed watchdog with the earliest timeout is scheduled to
 * expire. The corresponding watchdog timer device is therefore shared and several applications can concurrently
 * acquire the same watchdog timer device.
 *
 */
public interface WindowedWatchdogTimer extends WatchdogTimer {

    /**
     * Get the current closed window delay for the watchdog timer.
     * <p />
     * If the timer is disabled {@code 0} is returned.
     *
     * @return the delay (in milliseconds) until the watchdog timer can be refreshed;
     * or {@code 0} if the timer is disabled.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    public long getClosedWindowTimeout() throws IOException, ClosedDeviceException;

    /**
     * Starts the watchdog timer with the specified timeout and with a closed window delay set to {@code 0}. If the
     * watchdog timer is not refreshed by a call to {@link #refresh refresh} before the watchdog timing out, the device will
     * be restarted (or the JVM restarted).
     * <p />
     * Calling this method twice is equivalent to stopping the timer as per a call to {@link #stop} and starting
     * it again with the new specified timeout and with a closed window delay set to {@code 0}.
     *
     * @param timeout
     *            the time interval (in milliseconds) until watchdog times out.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalArgumentException
     *             if {@code timeout} is not greater than {@code 0}.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    @Override
    public void start(long timeout) throws IOException, ClosedDeviceException;

    /**
     * Starts the windowed watchdog timer with the specified closed window delay and timeout. If the {@link #refresh refresh}
     * method is called too soon, that is within the closed window delay, or too late, that is not called before the
     * watchdog timing out, the device will be restarted (or the JVM restarted).
     * <p />
     * Calling this method twice is equivalent to stopping the timer as per a call to {@link #stop} and starting
     * it again with the new specified closed window delay and timeout.
     *
     * @param closedWindowDelay
     *            the delay (in milliseconds) until the watchdog timer can be refreshed.
     * @param timeout
     *            the time interval (in milliseconds) until watchdog times out.
     * @throws IllegalArgumentException
     *             if {@code timeout} is not greater than {@code 0} or if {@code closedWindowDelay} is negative or
     *             {@code closedWindowDelay} is greater than {@code timeout}.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    public void start(long closedWindowDelay, long timeout) throws IOException, ClosedDeviceException;
}
