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
 * Interfaces and classes for reading and writing from/to GPIO (General Purpose Input Output) pins
 * and ports of the device.
 * <p/>
 * A GPIO port is a platform-defined or an ad-hoc grouping of GPIO pins that may be configured for output or
 * input. Whether GPIO pins that are part of a platform-defined GPIO port can be retrieved and controlled individually as
 * {@code GPIOPin} instances depends on the hardware and platform configuration (and especially
 * whether the GPIO pins can be shared through different abstractions).
 * <p/>
 * In order to use a specific pin or port, an application should first open and obtain and obtain a
 * {@link jdk.dio.gpio.GPIOPin} instance or
 * {@link jdk.dio.gpio.GPIOPort} instance, respectively, for the pin or port it
 * wants to use using its numeric ID, name, type (interface) and/or properties:
 * <ul>
 * <li>Using its ID <blockquote>
 *
 * <pre>
 * GPIOPin pin = (GPIOPin) DeviceManager.open(1);
 * GPIOPort port = (GPIOPort) DeviceManager.open(0);
 * </pre>
 * </blockquote></li>
 * <li>Using its name and interface <blockquote>
 *
 * <pre>
 * GPIOPin pin = (GPIOPin) DeviceManager.open(&quot;LED_PIN&quot;, GPIOPin.class, null);
 * GPIOPort port = (GPIOPort) DeviceManager.open(&quot;LCD_DATA_PORT&quot;, GPIOPort.class, null);
 * </pre>
 * </blockquote></li>
 * </ul>
 * Once a pin opened, an application can obtain the current value of a GPIO pin by calling the
 * {@link jdk.dio.gpio.GPIOPin#getValue GPIOPin.getValue} method and set its value by calling the
 * {@link jdk.dio.gpio.GPIOPin#setValue GPIOPin.setValue} method. <br />
 * Once a port opened, an application can obtain the current value of a GPIO port by calling the
 * {@link jdk.dio.gpio.GPIOPort#getValue GPIOPort.getValue} method and set its value by calling the
 * {@link jdk.dio.gpio.GPIOPort#setValue GPIOPort.setValue} method. <blockquote>
 *
 * <pre>
 * pin.setValue(true);
 * port.setValue(0xFF);
 * </pre>
 * </blockquote> When done, the application should call the
 * {@link jdk.dio.gpio.GPIOPin#close GPIOPin.close} or
 * {@link jdk.dio.gpio.GPIOPort#close GPIOPort.close} method to close the pin or
 * port, respectively. <blockquote>
 *
 * <pre>
 * pin.close();
 * port.close();
 * </pre>
 * </blockquote> The following sample code gives an example of using the GPIO API. It shows how to
 * control GPIO Pins. It registers a pin listener for the GPIO input pin a switch button is attached
 * to. When the button is pressed the listener is notified to turn the LED on or off by setting
 * accordingly the GPIO output pin the LED is attached to. <blockquote>
 *
 * <pre>
 * try (GPIOPin switchPin = (GPIOPin) DeviceManager.open(1); GPIOPin ledPin = (GPIOPin) DeviceManager.open(3)) {
 *     switchPin.setInputListener(new PinListener() {
 *
 *         public void valueChanged(PinEvent event) {
 *             try {
 *                 ledPin.setValue(event.getValue()); // turn LED on or off
 *             } catch (IOException ioe) {
 *                 // handle exception
 *             }
 *         }
 *     });
 *     // perform some other computation
 * } catch (IOException ioe) {
 *     // handle exception
 * }
 * </pre>
 * </blockquote> The preceding example is using a <em>try-with-resources</em> statement.
 * The {@link jdk.dio.gpio.GPIOPin#close GPIOPin.close} method is
 * automatically invoked by the platform at the end of the statement.
 * <p />
 * The underlying platform configuration may allow for some GPIO pins or ports to be set
 * by an application for either output or input while others may be used for input only or output
 * only and their direction can not be changed by an application. The asynchronous
 * notification of pin or port value changes is only loosely tied to hardware-level interrupt
 * requests. The platform does not guarantee notification in a deterministic/timely manner.
 * <p/>
 * Because of performance issue, procedures handling GPIO pins, and especially event listeners,
 * should be implemented to be as fast as possible.
 * <p />
 * Unless otherwise noted, passing a {@code null} argument to a constructor or method in any class
 * or interface in this package will cause a {@link java.lang.NullPointerException
 * NullPointerException} to be thrown.
 *
 * @since 1.0
 */
package jdk.dio.gpio;

