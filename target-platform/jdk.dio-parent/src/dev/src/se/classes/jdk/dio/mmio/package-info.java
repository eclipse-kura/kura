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
 * Interfaces and classes for performing memory-mapped I/O.
 * <p />
 * Memory mapped I/O is typically used for controlling hardware devices by reading from and writing to registers or
 * memory blocks of the hardware mapped to the system memory. The MMIO APIs allows for low level control over the
 * device.
 * <p />
 * In order to access a specific memory block a device has been mapped to, an application should first open and obtain
 * an {@link jdk.dio.mmio.MMIODevice} instance for the memory-mapped I/O device the application wants to
 * control and access, using its numeric ID, name, type (interface) and/or properties:
 * <dl>
 * <dt>Using its ID</dt>
 * <dd>
 * <blockquote>
 *
 * <pre>
 * MMIODevice device = (MMIODevice) DeviceManager.open(7);
 * </pre>
 *
 * </blockquote></dd>
 * <dt>Using its name and interface</dt>
 * <dd>
 * <blockquote>
 *
 * <pre>
 * MMIODevice device = DeviceManager.open(&quot;RTC&quot;, MMIODevice.class, null);
 * </pre>
 *
 * </blockquote></dd>
 * </dl>
 * Once the device opened, the application can retrieve registers using methods of the
 * {@link jdk.dio.mmio.MMIODevice MMIODevice} interface such as the
 * {@link jdk.dio.mmio.MMIODevice#getRegister MMIODevice.getRegister} method. <blockquote>
 *
 * <pre>
 * device.getRegister(&quot;Seconds&quot;, Byte.class);
 * </pre>
 *
 * </blockquote> When done, the application should call the {@link jdk.dio.mmio.MMIODevice#close MMIODevice.close}
 * method to close the MMIO device. <blockquote>
 *
 * <pre>
 * device.close();
 * </pre>
 *
 * </blockquote> The following sample codes give examples of using the MMIO API to communicate Real Time Clock device:
 * <blockquote>
 *
 * <pre>
 * static final int INTERRUPT = 0;
 *
 * try (MMIODevice rtc = DeviceManager.open("RTC", MMIODevice.class, null)) {
 *     //The RTC device has 14 bytes of clock and control registers and 50 bytes
 *     // of general purpose RAM (see the data sheet of the 146818 Real Time Clock such as HITACHI HD146818).
 *     RawRegister<Byte> seconds = rtc.getRegister("Seconds", Byte.class);
 *     RawRegister<Byte> secAlarm = rtc.getRegister("SecAlarm", Byte.class);
 *     RawRegister<Byte> minutes = rtc.getRegister("Minutes", Byte.class);
 *     RawRegister<Byte> minAlarm = rtc.getRegister("MinAlarm", Byte.class);
 *     RawRegister<Byte> hours = rtc.getRegister("Hours", Byte.class);
 *     RawRegister<Byte> hrAlarm = rtc.getRegister("HrAlarm", Byte.class);
 *     ... // More registers
 *     RawRegister<Byte> registerA = rtc.getRegister("RegisterA", Byte.class);
 *     RawRegister<Byte> registerB = rtc.getRegister("RegisterB", Byte.class);
 *     RawRegister<Byte> registerC = rtc.getRegister("RegisterC", Byte.class);
 *     RawRegister<Byte> registerD = rtc.getRegister("RegisterD", Byte.class);
 *     RawBlock userRAM = rtc.getBlock("UserRam");
 *     ...
 * } catch (IOException ioe) {
 *     // handle exception
 * }
 * </pre>
 *
 * </blockquote> The preceding example is using a <em>try-with-resources</em> statement; the
 * {@link jdk.dio.mmio.MMIODevice#close MMI0Device.close} method is automatically invoked by the
 * platform at the end of the statement. <blockquote>
 *
 * <pre>
 * // Sets the daily alarm for after some delay
 * public void setAlarm(MMIODevice rtc, byte delaySeconds, byte delayMinutes, byte delayHours) throws IOException,
 *         DeviceException {
 *     Register&lt;Byte&gt; seconds = rtc.getByteRegister(&quot;Seconds&quot;, Byte.class);
 *     Register&lt;Byte&gt; secAlarm = rtc.getByteRegister(&quot;SecAlarm&quot;, Byte.class);
 *     Register&lt;Byte&gt; minutes = rtc.getByteRegister(&quot;Minutes&quot;, Byte.class);
 *     Register&lt;Byte&gt; minAlarm = rtc.getByteRegister(&quot;MinAlarm&quot;, Byte.class);
 *     Register&lt;Byte&gt; hours = rtc.getByteRegister(&quot;Hours&quot;, Byte.class);
 *     Register&lt;Byte&gt; hrAlarm = rtc.getByteRegister(&quot;HrAlarm&quot;, Byte.class);
 *     Register&lt;Byte&gt; registerB = rtc.getByteRegister(&quot;RegisterB&quot;, Byte.class);
 *     RawBlock userRAM = rtc.getBlock(&quot;UserRam&quot;, Byte.class);
 *
 *     // Directly read from/write to the registers using RawByte instances.
 *     byte currentSeconds = seconds.get();
 *     byte currentMinutes = minutes.get();
 *     byte currentHours = hours.get();
 *     int i = (currentSeconds + delaySeconds) % 60;
 *     int j = (currentSeconds + delaySeconds) / 60;
 *     secAlarm.set((byte) i);
 *     i = (currentMinutes + delayMinutes + j) % 60;
 *     j = (currentMinutes + delayMinutes + j) / 60;
 *     minAlarm.set((byte) i);
 *     i = (currentHours + delayHours + j) % 24;
 *     hrAlarm.set((byte) i);
 *     rtc.setMMIOEventListener(INTERRUPT, new MMIOEventListener() {
 *
 *         public void eventDispatched(MMIOEvent event) {
 *             try {
 *                 MMIODevice rtc = event.getDevice();
 *                 Register&lt;Byte&gt; registerC = rtc.getByteRegister(&quot;RegisterC&quot;, Byte.class);
 *                 // Check the Alarm Interrupt Flag (AF)
 *                 if ((registerC.get() &amp; 0X20) != 0) {
 *                     // Notify application of alarm
 *                 }
 *             } catch (IOException ioe) {
 *                 // handle exception
 *             }
 *         }
 *     });
 *     // Set the Alarm Interrupt Enabled (AIE) flag
 *     registerB.set((byte) (registerB.get() | 0X20));
 * }
 * </pre>
 *
 * </blockquote> Alternatively, in this example, the value of {@code RegisterC} could be automatically captured upon
 * occurrence of an interrupt request from the Real Time Clock device as follows: <blockquote>
 *
 * <pre>
 * rtc.setMMIOEventListener(INTERRUPT, &quot;RegisterC&quot;, new MMIOEventListener() {
 *
 *     public void eventDispatched(MMIOEvent event) {
 *         Byte v = event.getCapturedRegisterValue();
 *         // Check the Alarm Interrupt Flag (AF)
 *         if ((v.byteValue() &amp; 0X20) != 0) {
 *             // Notify application of alarm
 *         }
 *     }
 * });
 * </pre>
 *
 * </blockquote>
 * <p />
 * Unless otherwise noted, passing a {@code null} argument to a constructor or method in any class
 * or interface in this package will cause a {@link java.lang.NullPointerException NullPointerException} to be thrown.
 *
 * @since 1.0
 */
package jdk.dio.mmio;

