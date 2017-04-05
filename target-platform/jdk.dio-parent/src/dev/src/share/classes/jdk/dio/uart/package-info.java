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
 * Interfaces and classes for controlling and reading and writing from/to Universal Asynchronous Receiver/Transmitter
 * (UART), with optional Modem signals control.
 * <p />
 * In order to access and control a specific UART device, an application should first open and obtain an
 * {@link jdk.dio.uart.UART} instance for the UART device using its numeric ID, name, type (interface)
 * and/or properties:
 * <dl>
 * <dt>Using its ID</dt>
 * <dd>
 * <blockquote>
 *
 * <pre>
 * UART uart = (UART) DeviceManager.open(14);
 * </dd>
 * <dt>Using its name and interface</dt>
 * <dd>
 * <blockquote>
 *
 * <pre>
 * UART uart = DeviceManager.open(&quot;HOST&quot;, UART.class, null);
 * </pre>
 *
 * </blockquote> Or (with modem signals control properties), <blockquote>
 *
 * <pre>
 * ModemUART uart = DeviceManager.open(&quot;MODEM&quot;, ModemUART.class, null);
 * </pre>
 *
 * </blockquote></dd>
 * </dl>
 * Once opened, an application can read the received data bytes and write the data bytes to be transmitted through the
 * UART using methods of the {@link java.nio.channels.ByteChannel ByteChannel} interface. <blockquote>
 *
 * <pre>
 * ByteBuffer buffer = new ByteBuffer();
 * ...
 * uart.read(buffer);
 * ...
 * uart.write(buffer);
 * ...
 * </pre>
 *
 * </blockquote> When done, the application should call the {@link jdk.dio.uart.UART#close() } method to
 * close the UART device. <blockquote>
 *
 * <pre>
 * uart.close();
 * </pre>
 *
 * </blockquote> The following sample code gives an example of using the UART API to communicate with some host
 * terminal: <blockquote>
 *
 * <pre>
 * try (UART host = DeviceManager.open(&quot;HOST&quot;, UART.class, (String) null);
 *         InputStream is = Channels.newInputStream(host);
 *         OutputStream os = Channels.newOutputStream(host)) {
 *     StringBuffer cmd = new StringBuffer();
 *     int c = 0;
 *     while (true) {
 *         os.write('$');
 *         os.write(' '); // echo prompt
 *         while (c != '\n' &amp;&amp; c != '\003') { // echo input
 *             c = is.read();
 *             os.write(c);
 *             cmd.append(c);
 *         }
 *         if (c == '\003') { // CTL-C
 *             break;
 *         }
 *         process(cmd); // Procees the command
 *     }
 * } catch (IOException ioe) {
 *     // Handle exception
 * }
 * </pre>
 *
 * </blockquote> The following sample codes give examples of using the ModemUART API to additionally control the MODEM
 * signals: <blockquote>
 *
 * <pre>
 * try (ModemUART modem = DeviceManager.open(&quot;HOST&quot;, ModemUART.class, (String) null);
 *         InputStream is = Channels.newInputStream(modem);
 *         OutputStream os = Channels.newOutputStream(modem)) {
 *     modem.setSignalChangeListener(new ModemSignalListener&lt;ModemUART&gt;() {
 *
 *         &#064;Override
 *         public void signalStateChanged(ModemSignalEvent&lt;ModemUART&gt; event) {
 *             if (event.getSignalState() == false) {
 *                 ModemUART modem = event.getDevice();
 *                 // Process MODEM hang-up...
 *             }
 *         }
 *     }, ModemSignalsControl.DCD_SIGNAL);
 *     // Process input and output...
 * } catch (IOException ioe) {
 *     // Handle exception
 * }
 * </pre>
 *
 * </blockquote> The preceding example is using a <em>try-with-resources</em> statement. The
 * {@link jdk.dio.uart.UART#close UART.close}, {@link java.io.InputStream#close
 * InputStream.close} and {@link java.io.OutputStream#close OutputStream.close} methods are automatically invoked
 * by the platform at the end of the statement.
 * <p />
 * Unless otherwise noted, passing a {@code null} argument to a constructor or method in any class
 * or interface in this package will cause a {@link java.lang.NullPointerException NullPointerException} to be thrown.
 * <p />
 * This package requires the {@link jdk.dio.modem} package.
 *
 * @since 1.0
 */
package jdk.dio.uart;

