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
 * Interfaces and classes for generating PWM pulses on a digital output line.
 * <p />
 * In order to access and control a specific PWM channel, an application should first open and obtain an
 * {@link jdk.dio.pwm.PWMChannel} instance for the PWM generator's channel the application wants to
 * access and control, using its numeric ID, name, type (interface) and/or properties:
 * <dl>
 * <dt>Using its ID</dt>
 * <dd>
 * <blockquote>
 *
 * <pre>
 * PWMChannel channel = (PWMChannel) DeviceManager.open(8);
 * </pre>
 *
 * </blockquote></dd>
 * <dt>Using its name and interface</dt>
 * <dd>
 * <blockquote>
 *
 * <pre>
 * PWMChannel channel = DeviceManager.open(&quot;DIMMER&quot;, PWMChannel.class, null);
 * </pre>
 *
 * </blockquote></dd>
 * </dl>
 * Once opened, an application can set the period of generated pulses using the
 * {@link jdk.dio.pwm.PWMChannel#setPulsePeriod} method ; then generate pulses of a specified width or
 * duty cycle by calling one of the {@link jdk.dio.pwm.PWMChannel#generate} or
 * {@link jdk.dio.pwm.PWMChannel#startGeneration}. <blockquote>
 *
 * <pre>
 * channel.setPulsePeriod(1000000); // Pulse period = 1 second
 * channel.generate(500000, 10); // Generate 10 pulses with a width of 0.5 second
 * </pre>
 *
 * </blockquote> When done, the application should call the {@link jdk.dio.pwm.PWMChannel#close
 * PWMChannel.close} method to close PWM channel. <blockquote>
 *
 * <pre>
 * channel.close();
 * </pre>
 *
 * </blockquote> The following sample code gives an example of using the PWM channel API to progressively dim the light
 * of a LED (for example) starting from its maximum intensity (100% duty cycle) in 10 successive steps of 10 seconds
 * each: <blockquote>
 *
 * <pre>
 * class VaryingDimmer implements GenerationRoundListener {
 *
 *     private PWMChannel channel = null;
 *     private int step = 10;
 *
 *     public void pulseGenerationCompleted(GenerationEvent event) {
 *         if (step &gt; 0) {
 *             try {
 *                 channel.startGeneration((channel.getPulsePeriod() / 10) * --step, 10, this);
 *             } catch (IOException ex) {
 *                 // Iggnored
 *             }
 *         }
 *     }
 *
 *     public void start(int channelID) throws IOException, NonAvailableDeviceException, DeviceNotFoundException {
 *         if (channel != null) {
 *             throw new IllegalStateException();
 *         }
 *         channel = (PWMChannel) DeviceManager.open(channelID);
 *         channel.setPulsePeriod(1000000); // period = 1 second
 *         channel.startGeneration((channel.getPulsePeriod() / 10) * step, 10, this);
 *     }
 *
 *     public void stop() throws IOException, NonAvailableDeviceException {
 *         if (channel != null) {
 *             channel.stopGeneration();
 *             channel.close();
 *         }
 *     }
 *
 *     public void failed(Throwable exception, PWMChannel source) {
 *          // Ignored
 *     }
 * }
 * </pre>
 *
 * </blockquote> Because of performance issue, procedures handling PWM events, and especially event listeners, should be
 * implemented to be as fast as possible.
 * <p />
 * Unless otherwise noted, passing a {@code null} argument to a constructor or method in any class
 * or interface in this package will cause a {@link java.lang.NullPointerException NullPointerException} to be thrown.
 *
 * @since 1.0
 */
package jdk.dio.pwm;