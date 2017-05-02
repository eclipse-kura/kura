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
 * Interfaces and classes for controlling a Data Communication Equipment such as a modem or a
 * cellular module using AT commands.
 * <p />
 * AT commands for GSM phone or modem are standardized through ETSI GSM 07.07 and ETSI GSM 07.05
 * specifications. A typical modem or an cellular module supports most of its features through AT
 * commands and many manufactures provide additional features by adding proprietary extensions to
 * the AT commands set.
 * <p />
 * In this specification, a device that can be controlled using AT commands is generically referred
 * to as an AT device.
 * <p />
 * To control a specific AT device, an application should first open and obtain an
 * {@link jdk.dio.atcmd.ATDevice} or {@link jdk.dio.atcmd.ATModem}
 * instance for the device the application wants to control, using its numeric ID, name, type
 * (interface) and/or properties:
 * <dl>
 * <dt>Using its ID</dt>
 * <dd><blockquote>
 *
 * <pre>
 * ATDevice device = DeviceManager.open(15);
 * </pre>
 * </blockquote></dd>
 * <dt>Using its name, properties and interface</dt>
 * <dd><blockquote>
 *
 * <pre>
 * ATDevice device = DeviceManager.open(&quot;MODEM&quot;, ATDevice.class, &quot;jdk.dio.atcmd.psd=true&quot;,
 *         &quot;jdk.dio.atcmd.sms=true&quot;);
 * </pre>
 * </blockquote> Or (with modem signals control properties), <blockquote>
 *
 * <pre>
 * ATModem device = DeviceManager.open(&quot;MODEM&quot;, ATModem.class, &quot;jdk.dio.atcmd.psd=true&quot;,
 *         &quot;jdk.dio.atcmd.sms=true&quot;);
 * </pre>
 * </blockquote></dd>
 * </dl>
 * Once the device opened, the application can issue AT commands to the device using methods
 * of the {@link jdk.dio.atcmd.ATDevice} interface such as the
 * {@link jdk.dio.atcmd.ATDevice#sendCommand(String cmd, CommandResponseHandler handler)
 * sendCommand} methods. <blockquote>
 *
 * <pre>
 * device.sendCommand(&quot;AT\n&quot;);
 * </pre>
 * </blockquote> When done, the application should call the
 * {@link jdk.dio.atcmd.ATDevice#close() } method to close AT device. <blockquote>
 *
 * <pre>
 * device.close();
 * </pre>
 * </blockquote> The following sample codes give examples of using the AT API to send an SMS:
 * <blockquote>
 *
 * <pre>
 * public static final int SUBMITTED = 1;
 * public static final int SENT = 2;
 * public static final int ERROR = 3;
 * private ATDevice modem = null;
 * private int status = 0;
 *
 * private class SMSHandler implements CommandResponseHandler {
 *
 *     String text;
 *
 *     public SMSHandler(String text) {
 *         this.text = text;
 *     }
 *
 *     public String processResponse(ATDevice modem, String response) {
 *         // Assume that command echo has been previously disabled (such as with an {@code ATE0} command).
 *
 *         if (response.equals(&quot;&gt; \n&quot;)) { // Prompt for text
 *             return text;
 *         } else if (response.equals(&quot;OK\n&quot;)) {
 *             status = SENT;  // Sent successfully
 *         } else if (response.contains(&quot;ERROR&quot;)) {
 *             status = ERROR; // Failed to send
 *         }
 *         return null;
 *     }
 * }
 *
 * public boolean sendSMS(final String number, final String text) {
 *     // Acquire a modem with &quot;sms&quot; properties
 *     try {
 *         if (modem == null) {
 *             modem = DeviceManager.open(null, ATDevice.class, &quot;jdk.dio.atcmd.sms=true&quot;);
 *         }
 *         // Send SMS command
 *         SMSHandler sh = new SMSHandler(text);
 *         modem.sendCommand(&quot;AT+CMGS=\&quot;&quot; + number + &quot;\&quot;\n&quot;, sh);
 *         status = SUBMITTED;
 *         return true; // Submitted successfully
 *     } catch (IOException ioe) {
 *         return false; // Failed to submit
 *     }
 * }
 *
 * public int getStatus() {
 *     return status;
 * }
 *
 * public void close() {
 *     if (modem != null) {
 *         try {
 *             modem.close();
 *         } catch (IOException ex) {
 *             // Ignored
 *         }
 *     }
 * }
 * </pre>
 * </blockquote>
 * <p />
 * Unless otherwise noted, passing a {@code null} argument to a constructor or method in any class
 * or interface in this package will cause a {@link java.lang.NullPointerException
 * NullPointerException} to be thrown.
 * <p />
 * This package requires the {@link jdk.dio.modem} package.
 *
 * @since 1.0
 */
package jdk.dio.atcmd;

