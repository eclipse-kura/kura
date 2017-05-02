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
 * Interfaces and classes for counting pulses (or events) on a digital input line.
 * <p />
 * In order to access and control a specific pulse counter, an application should first open and
 * obtain an {@link jdk.dio.counter.PulseCounter} instance for the pulse counter the
 * application wants to access and control, using its numeric ID, name, type (interface) and/or
 * properties:
 * <dl>
 * <dt>Using its ID</dt>
 * <dd><blockquote>
 *
 * <pre>
 * PulseCounter counter = (PulseCounter) DeviceManager.open(8);
 * </pre>
 * </blockquote></dd>
 * <dt>Using its name and interface</dt>
 * <dd><blockquote>
 *
 * <pre>
 * PulseCounter counter = DeviceManager.open(&quot;ENCODER&quot;, PulseCounter.class, null);
 * </pre>
 * </blockquote></dd>
 * </dl>
 * Once opened, an application can either start a pulse counting session using the
 * {@link jdk.dio.counter.PulseCounter#startCounting() startCounting} method and
 * retrieve the current pulse count on-the-fly by calling the
 * {@link jdk.dio.counter.PulseCounter#getCount} method; or it can start a pulse
 * counting session with a terminal count value and a counting time interval using the
 * {@link jdk.dio.counter.PulseCounter#startCounting(int, long, jdk.dio.counter.CountingListener) }
 * and get asynchronously notified once the terminal count value has been reached or the counting
 * time interval has expired. In both cases, the application can retrieve the current pulse count
 * value at any time (on-the-fly) by calling the {@code getCount}. <blockquote>
 *
 * <pre>
 * counter.startCounting(); // Start counting pulses
 * // Perform some task...
 * int count = counter.getCount(); // Retrieve the number of pulses that occurred while performing the task
 * counter.stopCounting(); // Stop counting pulses
 * </pre>
 * </blockquote> When done, the application should call the
 * {@link jdk.dio.counter.PulseCounter#close close} method to close Pulse counter.
 * <blockquote>
 *
 * <pre>
 * counter.close();
 * </pre>
 * </blockquote> The following sample codes give examples of using the counter/timer API:
 * <blockquote>
 *
 * <pre>
 * class PulseCounting implements CountingListener {
 *
 *     private PulseCounter counter = null;
 *
 *     public void start(int counterID) throws IOException, NonAvailableDeviceException, DeviceNotFoundException {
 *         counter = (PulseCounter) DeviceManager.open(counterID);
 *         counter.startCounting(-1, 1000, this); // Count events occurring during 1 second (without terminal count value)
 *     }
 *
 *     public void countValueAvailable(CountingEvent event) {
 *         int count = event.getValue();
 *         // Handle pulse count...
 *     }
 *
 *     public void stop() throws IOException {
 *         if (counter != null) {
 *             counter.stopCounting();
 *             counter.close();
 *         }
 *     }
 * }
 * </pre>
 * </blockquote> Because of performance issue, procedures handling pulse counting events, and
 * especially event listeners, should be implemented to be as fast as possible.
 * <p />
 * Unless otherwise noted, passing a {@code null} argument to a constructor or method in any class
 * or interface in this package will cause a {@link java.lang.NullPointerException
 * NullPointerException} to be thrown.
 *
 * @since 1.0
 */
package jdk.dio.counter;