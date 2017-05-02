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

package jdk.dio.uart;

import jdk.dio.modem.ModemSignalsControl;

/**
 * The {@code ModemUART} interface provides methods for controlling and accessing a UART (Universal Asynchronous
 * Receiver/Transmitter) with Modem control lines.
 * <p />
 * Even if CTS/RTS hardware flow control is enabled (see {@link UARTConfig#getFlowControlMode UARTConfig.getFlowControlMode}), registering
 * for notification of CTS signal state changes (see
 * {@link ModemSignalsControl#setSignalChangeListener ModemSignalsControl.setSignalChangeListener} may not
 * always be supported. Additionally, when supported and because of latency, CTS signal state change notification may
 * only be indicative: CTS flow control may be handled directly by the hardware or by the native driver.
 *
 *
 * @since 1.0
 */
public interface ModemUART extends UART, ModemSignalsControl<ModemUART> {
}
