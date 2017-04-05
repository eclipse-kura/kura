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
 * Interfaces and classes for controlling devices using generic I/O operations.
 * <p />
 * The generic device API allows for accessing devices when there are no more specific
 * standard Java API such as {@link jdk.dio.i2cbus.I2CDevice},
 * {@link jdk.dio.spibus.SPIDevice}, {@link jdk.dio.gpio.GPIOPin} or
 * {@link jdk.dio.gpio.GPIOPort}...
 * <p />
 * This API offers 2 main interfaces:
 * <dl>
 * <dt>{@link jdk.dio.generic.GenericDevice}</dt>
 * <dd>Device control operations and event listener registration. A device may implements this sole
 * interface if it does not support any read and write operations.</dd>
 * <dt>{@link jdk.dio.generic.GenericBufferIODevice}</dt>
 * <dd>Device control operations and event listener registration as inherited from
 * {@link jdk.dio.generic.GenericBufferIODevice} and byte buffer read and write
 * operations.</dd>
 * </dl>
 * In order to access a device using its generic interface, an application should first open and
 * obtain a {@link jdk.dio.generic.GenericDevice} instance for the device using its
 * numeric ID, name, type (interface) and/or properties:
 * <dl>
 * <dt>Using its ID</dt>
 * <dd><blockquote>
 *
 * <pre>
 * GenericDevice device = (GenericDevice) DeviceManager.open(17);
 * </pre>
 * </blockquote></dd>
 * <dt>Using its name and interface</dt>
 * <dd><blockquote>
 *
 * <pre>
 * GeneriBufferIODevice device = DeviceManager.open(&quot;STORAGE&quot;, GeneriBufferIODevice.class, null);
 * </pre>
 * </blockquote></dd>
 * </dl>
 * Once the device opened, the application can set and get controls, read and write data using
 * methods of the {@link jdk.dio.generic.GenericDevice} or
 * {@link jdk.dio.generic.GenericBufferIODevice} interfaces. <blockquote>
 *
 * <pre>
 * device.read(buffer, 0, buffer.length);
 * </pre>
 * </blockquote> When done, the application should call the
 * {@link jdk.dio.generic.GenericDevice#close GenericDevice.close} method to close
 * the Generic device. <blockquote>
 *
 * <pre>
 * device.close();
 * </pre>
 * </blockquote> The following sample codes give examples of using the Generic API to communicate
 * Real Time Clock device: <blockquote>
 *
 * <pre>
 * public static final int EVT_ALARM = 0;
 * public static final GenericDeviceControl&lt;Byte&gt; SECONDS = new GenericDeviceControl&lt;&gt;(0, Byte.class);
 * public static final GenericDeviceControl&lt;Byte&gt; SEC_ALARM = new GenericDeviceControl&lt;&gt;(1, Byte.class);
 * public static final GenericDeviceControl&lt;Byte&gt; MINUTES = new GenericDeviceControl&lt;&gt;(2, Byte.class);
 * public static final GenericDeviceControl&lt;Byte&gt; MIN_ALARM = new GenericDeviceControl&lt;&gt;(3, Byte.class);
 * public static final GenericDeviceControl&lt;Byte&gt; HR_ALARM = new GenericDeviceControl&lt;&gt;(4, Byte.class);
 * public static final GenericDeviceControl&lt;Byte&gt; HOURS = new GenericDeviceControl&lt;&gt;(5, Byte.class);
 * public static final GenericDeviceControl&lt;Boolean&gt; ALARM_ENABLED = new GenericDeviceControl&lt;&gt;(6, Boolean.class);
 *
 * // Sets the daily alarm for after some delay
 * public void setAlarm(byte delaySeconds, byte delayMinutes, byte delayHours) throws IOException, DeviceException {
 *     try (GenericDevice rtc = DeviceManager.open(&quot;RTC&quot;, GenericDevice.class, (String) null)) {
 *         byte currentSeconds = rtc.getControl(SECONDS);
 *         byte currentMinutes = rtc.getControl(MINUTES);
 *         byte currentHours = rtc.getControl(HOURS);
 *         byte i = (byte) ((currentSeconds + delaySeconds) % 60);
 *         byte j = (byte) ((currentSeconds + delaySeconds) / 60);
 *         rtc.setControl(SEC_ALARM, i);
 *         i = (byte) ((currentMinutes + delayMinutes + j) % 60);
 *         j = (byte) ((currentMinutes + delayMinutes + j) / 60);
 *         rtc.setControl(MIN_ALARM, i);
 *         i = (byte) ((currentHours + delayHours + j) % 24);
 *         rtc.setControl(HR_ALARM, i);
 *
 *         rtc.setEventListener(EVT_ALARM, new GenericEventListener() {
 *
 *             public void eventDispatched(GenericEvent event) {
 *                 GenericDevice rtc = event.getDevice();
 *                 // Notify application of alarm
 *             }
 *         });
 *         // Enable alarm.
 *         rtc.setControl(ALARM_ENABLED, true);
 *     }
 * }
 * </pre>
 * </blockquote> The preceding example is using a <em>try-with-resources</em> statement.
 * The {@link jdk.dio.generic.GenericDevice#close GenericDevice.close}
 * method is automatically invoked by the platform at the end of the statement. <blockquote>
 *
 * <pre>
 * public static final int EVT_VOLUME_CHANGED = 0;
 * public static final GenericDeviceControl&lt;Float&gt; MIC_VOLUME = new GenericDeviceControl&lt;&gt;(0, Float.class);
 * public static final GenericDeviceControl&lt;Float&gt; MIC_SAMPLE_RATE = new GenericDeviceControl&lt;&gt;(1, Float.class);
 * public static final GenericDeviceControl&lt;Boolean&gt; MIC_AUTOMATIC_GAIN = new GenericDeviceControl&lt;&gt;(2, Boolean.class);
 * public static final GenericDeviceControl&lt;Boolean&gt; MIC_MUTE = new GenericDeviceControl&lt;&gt;(3, Boolean.class);
 *
 * public void audioCapture(ByteBuffer buffer, float sampleRate, boolean agc) throws IOException, DeviceException {
 *     try (GenericBufferIODevice mic = DeviceManager.open(&quot;MICROPHONE&quot;, GenericBufferIODevice.class, (String) null)) {
 *         mic.setControl(MIC_SAMPLE_RATE, sampleRate);
 *         mic.setControl(MIC_AUTOMATIC_GAIN, agc);
 *         mic.setControl(MIC_MUTE, false);
 *
 *         mic.setEventListener(EVT_VOLUME_CHANGED, new GenericEventListener() {
 *
 *             public void eventDispatched(GenericEvent event) {
 *                 GenericDevice mic = event.getDevice();
 *                 try {
 *                     float currentVolume = mic.getControl(MIC_VOLUME);
 *                     // ...
 *                 } catch (ClosedDeviceException ex) {
 *                     ex.printStackTrace();
 *                 } catch (IOException ex) {
 *                     ex.printStackTrace();
 *                 }
 *             }
 *         });
 *         mic.read(buffer);
 *     }
 * }
 * </pre>
 * </blockquote>
 * <p />
 * Unless otherwise noted, passing a {@code null} argument to a constructor or method in any class
 * or interface in this package will cause a {@link java.lang.NullPointerException
 * NullPointerException} to be thrown.
 *
 * @since 1.0
 */
package jdk.dio.generic;

