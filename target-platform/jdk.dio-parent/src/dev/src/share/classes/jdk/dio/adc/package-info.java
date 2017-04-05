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
 * Interfaces and classes for reading analog inputs using an Analog to Digital Converter (ADC).
 * <p />
 * One ADC converter can have several channels. Each channel can sample a continuous input voltage and convert it to a
 * numeric value.
 * <p />
 * In order to access and control a specific ADC channel, an application should first open and obtain an
 * {@link jdk.dio.adc.ADCChannel ADCChannel} instance for the ADC channel the application wants to
 * access and control, using its numeric ID, name, type (interface) and/or properties:
 * <dl>
 * <dt>Using its ID</dt>
 * <dd>
 * <blockquote>
 *
 * <pre>
 * ADCChannel channel = DeviceManager.open(8);
 * </pre>
 *
 * </blockquote></dd>
 * <dt>Using its name and interface</dt>
 * <dd>
 * <blockquote>
 *
 * <pre>
 * ADCChannel channel = DeviceManager.open(&quot;TEMPERATURE&quot;, ADCChannel.class, null);
 * </pre>
 *
 * </blockquote></dd>
 * </dl>
 * Once the device opened, the application can read or monitor sampled input values using methods of the
 * {@code ADCChannel} interface such as the
 * {@link jdk.dio.adc.ADCChannel#acquire() acquire} method. <blockquote>
 *
 * <pre>
 * int temp = channel.acquire();
 * </pre>
 *
 * </blockquote> When done, the application should call the {@link jdk.dio.adc.ADCChannel#close
 * ADCChannel.close} method to close ADC channel. <blockquote>
 *
 * <pre>
 * channel.close();
 * </pre>
 *
 * </blockquote> The following sample codes give examples of using the ADC API: <blockquote>
 *
 * <pre>
 * class ADCAcquisition implements AcquisitionRoundListener {
 *
 *     private ADCChannel channel = null;
 *
 *     public void start(int channelID) throws IOException, NonAvailableDeviceException, DeviceNotFoundException {
 *         channel = (ADCChannel) DeviceManager.open(channelID);
 *         channel.setSamplingInterval(100); // every 100 milliseconds
 *         int[] values = new int[10];
 *         channel.startAcquisition(IntBuffer.wrap(values), this);
 *     }
 *
 *     public void inputRoundCompleted(RoundCompletionEvent&lt;ADCChannel, IntBuffer&gt; event) {
 *         IntBuffer buffer = event.getBuffer();
 *         while (buffer.hasRemaining()) {
 *             int value = buffer.get();
 *             // Handle value...
 *         }
 *     }
 *
 *     public void stop() throws IOException {
 *         if (channel != null) {
 *             channel.stopAcquisition();
 *             channel.close();
 *         }
 *     }
 *
 *     public void failed(Throwable exception, ADCChannel source) {
 *          // Ignored
 *     }
 * }
 * </pre>
 *
 * </blockquote> <blockquote>
 *
 * <pre>
 * class ADCThreshold implements MonitoringListener {
 *
 *     private ADCChannel channel = null;
 *
 *     public void start(int channelID, int low, int high) throws IOException, NonAvailableDeviceException,
 *             DeviceNotFoundException {
 *         channel = (ADCChannel) DeviceManager.open(channelID);
 *         channel.setSamplingInterval(100); // every 100 milliseconds
 *         channel.startMonitoring(low, high, this);
 *     }
 *
 *     public void thresholdReached(MonitoringEvent event) {
 *         if (event.getType() == MonitoringEvent.OUT_OF_RANGE) {
 *             int value = event.acquire();
 *             // Handle condition...
 *         }
 *     }
 *
 *     public void stop() throws IOException {
 *         if (channel != null) {
 *             channel.stopMonitoring();
 *             channel.close();
 *         }
 *     }
 *
 *     public void failed(Throwable exception, ADCChannel source) {
 *          // Ignored
 *     }
 * }
 * </pre>
 *
 * </blockquote> Because of performance issue, procedures handling analog inputs, and especially event listeners, should
 * be implemented to be as fast as possible.
 * <p />
 * Unless otherwise noted, passing a {@code null} argument to a constructor or method in any class
 * or interface in this package will cause a {@link java.lang.NullPointerException NullPointerException} to be thrown.
 *
 * @since 1.0
 */
package jdk.dio.adc;
