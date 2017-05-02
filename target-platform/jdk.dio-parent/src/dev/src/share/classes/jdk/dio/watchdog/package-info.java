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

/**
 * Interfaces and classes for using system watchdog timers (WDT).
 * <p />
 * A watchdog timer is used to reset/restart the system in case of hang or critical failure. This is used to reset the
 * system from a unresponsive state to a normal state. A watchdog timer can be set with a time interval for the
 * reset/restart. Continuously refreshing the watchdog timer within the specified time interval prevents the
 * reset/restart. If the watchdog timer has not been refreshed within the specified time interval a critical failure is
 * assumed and a system reset/restart is carried out. <br />
 * A windowed watchdog timer must be refreshed within an open time window. If the watchdog is refreshed too soon -
 * during the closed window - or if it is refreshed too late - after the watchdog timeout has expired - the device will
 * be rebooted.
 * <p />
 * In order to use with a specific watchdog timer, an application should first open and obtain an
 * {@link jdk.dio.watchdog.WatchdogTimer} instance for the watchdog timer the application wants to use,
 * using its numeric ID, name, type (interface) or properties:
 * <dl>
 * <dt>Using its ID</dt>
 * <dd>
 * <blockquote>
 *
 * <pre>
 * WatchdogTimer wdt = DeviceManager.open(8);
 * </pre>
 *
 * </blockquote></dd>
 * <dt>Using its name and interface</dt>
 * <dd>
 * <blockquote>
 *
 * <pre>
 * WatchdogTimer wdt = DeviceManager.open(&quot;WDT&quot;, WatchdogTimer.class, null);
 * </pre>
 *
 * </blockquote> Or for a windowed watchdog timer, <blockquote>
 *
 * <pre>
 * WindowedWatchdogTimer wdt = DeviceManager.open(&quot;WWDT&quot;, WindowedWatchdogTimer.class, null);
 * </pre>
 *
 * </blockquote></dd>
 * </dl>
 * Once the device opened, the application can start using it and can especially start the timer using the
 * {@link jdk.dio.watchdog.WatchdogTimer#start(long) WatchdogTimer.start} method and subsequently
 * refresh the timer periodically using the {@link jdk.dio.watchdog.WatchdogTimer#refresh
 * WatchdogTimer.refresh} method <blockquote>
 *
 * <pre>
 * wdt.start(1000);
 * ...
 * wdt.refresh();
 * </pre>
 *
 * </blockquote> When done, the application should call the
 * {@link jdk.dio.watchdog.WatchdogTimer#close WatchdogTimer.close} method to close the watchdog
 * timer. <blockquote>
 *
 * <pre>
 * wdt.close();
 * </pre>
 *
 * </blockquote>
 * <p />
 * The following sample codes give examples of using the watchdog timer API: <blockquote>
 *
 * <pre>
 * public class WatchdogSample {
 *     public boolean checkSomeStatus() {
 *         // check some status....
 *         // if status is ok then return true to kick watch dog timer.
 *         return true;
 *     }
 *
 *     public void test_loop() {
 *         WatchdogTimer watchdogTimer = (WatchdogTimer) DeviceManager.open(WDT_ID);
 *
 *         watchdogTimer.start(180000); // Start watch dog timer with 3 min duration.
 *
 *         while (true) {
 *             if (checkSomeStatus() == true) {
 *                 // Everything goes fine, timer will be kick.
 *                 watchdogTimer.refresh();
 *                 // do something more...
 *             } else {
 *                 // Something goes wrong. Timer will not be kick.
 *                 // If status not recovered within 2-3 turns then system will be restart.
 *             }
 *             sleep(60000); // sleep for 1 min.
 *         }
 *     }
 * }
 * </pre>
 *
 * </blockquote>
 * <p />
 * Unless otherwise noted, passing a {@code null} argument to a constructor or method in any class
 * or interface in this package will cause a {@link java.lang.NullPointerException NullPointerException} to be thrown.
 */
package jdk.dio.watchdog;