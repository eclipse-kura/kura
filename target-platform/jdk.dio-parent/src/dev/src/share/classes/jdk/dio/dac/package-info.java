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
 * Interfaces and classes for writing analog outputs using a Digital to Analog Converter (DAC).
 * <p />
 * One DAC converter can have several channels. Each channel can generate an analog output from numeric values that
 * are converted to output voltages.
 * <p />
 * In order to access and control a specific DAC channel, an application should first open and obtain an
 * {@link jdk.dio.dac.DACChannel DACChannel} instance for the DAC channel the application wants to
 * access and control, using its numeric ID, name, type (interface) and/or properties:
 * <dl>
 * <dt>Using its ID</dt>
 * <dd>
 * <blockquote>
 *
 * <pre>
 * DACChannel channel = (DACChannel) DeviceManager.open(5);
 * </pre>
 *
 * </blockquote></dd>
 * <dt>Using its name and interface</dt>
 * <dd>
 * <blockquote>
 *
 * <pre>
 * DACChannel channel = (DACChannel) DeviceManager.open(&quot;LED&quot;, DACChannel.class, null);
 * </pre>
 *
 * </blockquote></dd>
 * </dl>
 * Once the device opened, an application can write output values to a DAC channel using methods of the
 * {@link jdk.dio.dac.DACChannel DACChannel} interface such as the
 * {@link jdk.dio.dac.DACChannel#generate(int) generate} method. <blockquote>
 *
 * <pre>
 * channel.generate(brightness);
 * </pre>
 *
 * </blockquote> When done, the application should call the {@link jdk.dio.dac.DACChannel#close
 * DACChannel.close} method to close the DAC channel. <blockquote>
 *
 * <pre>
 * channel.close();
 * </pre>
 *
 * </blockquote> The following sample codes give examples of using the DAC API: <blockquote>
 *
 * <pre>
 * class VaryingDimmer implements GenerationRoundListener {
 *
 *     private DACChannel channel = null;
 *
 *     public void start(int channelID) throws IOException, NonAvailableDeviceException, DeviceNotFoundException {
 *         if (channel != null) {
 *             throw new InvalidStateException();
 *         }
 *         channel = (DACChannel) DeviceManager.open(channelID);
 *         channel.setSamplingInterval(1000); // every 1000 milliseconds
 *         // Creates a series of samples varying from min value to max value
 *         int[] values = new int[10];
 *         int min = channel.getMinValue();
 *         int max = channel.getMaxValue();
 *         IntBuffer values = IntBuffer.wrap(new int[10]);
 *         createSamples(values, channel.getMinValue(), channel.getMaxValue(), 10);
 *         channel.startGeneration(values, this);
 *     }
 *
 *     public void outputRoundCompleted(RoundCompletionEvent&lt;DACChannel, IntBuffer&gt; event) {
 *         try {
 *             // Replay the same sample series
 *             createSamples(event.getBuffer(), event.getDevice().getMinValue(), event.getDevice().getMaxValue(),
 *                     10);
 *         } catch (IOException ioe) {
 *             // Ignored
 *         }
 *     }
 *
 *     // Creates a series of samples varying from min value to max value
 *     private void createSamples(IntBuffer buffer, int min, int max, int count) {
 *         for (int i = 0; i &lt; count; i++) {
 *             buffer.put(min + (((max - min) / (count - 1)) * i));
 *         }
 *         buffer.flip();
 *     }
 *
 *     public void stop() throws IOException {
 *         if (channel != null) {
 *             channel.stopGeneration();
 *             channel.close();
 *         }
 *     }
 *
 *     public void failed(Throwable exception, DACChannel source) {
 *          // Ignored
 *     }
 * }
 * </pre>
 *
 * </blockquote> Because of performance issue, procedures handling analog outputs, and especially event listeners,
 * should be implemented to be as fast as possible.
 * <p />
 * Unless otherwise noted, passing a {@code null} argument to a constructor or method in any class
 * or interface in this package will cause a {@link java.lang.NullPointerException NullPointerException} to be thrown.
 *
 * @since 1.0
 */
package jdk.dio.dac;
