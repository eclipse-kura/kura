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
 * Interfaces and classes for power management of devices.
 * <p />
 * A {@link jdk.dio.Device} implementing class may implement the
 * {@link jdk.dio.power.PowerManaged} interface if the underlying device supports some form
 * of power management and saving states that can be mapped to the states defined by this API.
 * <p />
 * The following sample code gives an examples of using the power saving/management API: <blockquote>
 *
 * <pre>
 * class SignalLevelMonitor implements MonitoringListener, PowerSavingHandler {
 *
 *     private ADCChannel channel = null;
 *     private boolean inRange = false;
 *
 *     public void start(int channelID, int low, int high) throws IOException, UnavailableDeviceException,
 *             DeviceNotFoundException {
 *         channel = (ADCChannel) DeviceManager.open(channelID);
 *         channel.setSamplingInterval(1000); // every 1 seconds
 *         channel.startMonitoring(low, high, this);
 *         if (channel instanceof PowerManaged) {
 *             ((PowerManaged) channel).enablePowerSaving(PowerManaged.LOW_POWER, this); // Only enable LOW_POWER saving mode (POWER_ON is implicit)
 *         }
 *     }
 *
 *     &#64;Override
 *     public void thresholdReached(MonitoringEvent event) {
 *         inRange = (event.getType() == MonitoringEvent.BACK_TO_RANGE);
 *     }

 *     &#64;Override
 *     public &lt;P extends Device&lt;? super P&gt;&gt; long handlePowerStateChangeRequest(P device,
 *             PowerManaged.Group group, int currentState, int requestedState, long duration) {
 *         if (requestedState == PowerManaged.LOW_POWER) {
 *             return inRange ? duration : 0; // Only accept to change to LOW_POWER if signal is back in range
 *         }
 *         return duration; // Accept returning to POWER_ON
 *     }
 *
 *     &#64;Override
 *     public &lt;P extends Device&lt;? super P&gt;&gt; void handlePowerStateChange(P device,
 *             PowerManaged.Group group, int currentState, int requestedState, long duration) {
 *         // Do nothing
 *     }
 *
 *     public void stop() throws IOException {
 *         if (channel != null) {
 *             channel.stopMonitoring();
 *             if (channel instanceof PowerManaged) {
 *                 ((PowerManaged) channel).disablePowerSaving();
 *             }
 *             channel.close();
 *         }
 *     }
 * }
 * </pre>
 *
 * </blockquote>
 * <p />
 * Unless otherwise noted, passing a {@code null} argument to a constructor or method in any class
 * or interface in this package will cause a {@link java.lang.NullPointerException NullPointerException} to be thrown.
 *
 * @since 1.0
 */
package jdk.dio.power;