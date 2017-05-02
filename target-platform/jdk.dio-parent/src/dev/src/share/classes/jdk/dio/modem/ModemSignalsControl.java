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

package jdk.dio.modem;

import jdk.dio.ClosedDeviceException;
import jdk.dio.Device;
import jdk.dio.UnavailableDeviceException;
import java.io.IOException;

/**
 * The {@code ModemSignalsControl} interface provides methods for controlling and monitoring modem signals.
 *
 * @param <P>
 *            the device type implementing this interface.
 * @since 1.0
 */
public interface ModemSignalsControl<P extends Device<? super P>> {

    /**
     * Clear To Send (CTS) signal.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other signal bit flags.
     */
    public static final int CTS_SIGNAL = 32;
    /**
     * Data Carrier Detect (DCD) signal.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other signal bit flags.
     */
    public static final int DCD_SIGNAL = 2;
    /**
     * Data Set Ready (DSR) signal.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other signal bit flags.
     */
    public static final int DSR_SIGNAL = 4;
    /**
     * Data Terminal Ready (DTR) signal.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other signal bit flags.
     */
    public static final int DTR_SIGNAL = 1;
    /**
     * Ring Indicator (RI) signal.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other signal bit flags.
     */
    public static final int RI_SIGNAL = 8;
    /**
     * Ready To Send (RTS) signal.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other signal bit flags.
     */
    public static final int RTS_SIGNAL = 16;

    /**
     * Gets the state of the designated signal.
     *
     * @param signalID
     *            the ID of the signal ({@link #DCD_SIGNAL}, {@link #DSR_SIGNAL}, {@link #RI_SIGNAL} or
     *            {@link #CTS_SIGNAL}).
     * @return the state of the designated signal: {@code true} for set; {@code false} for cleared.
     * @throws IllegalArgumentException
     *             if {@code signalID} is not one of the defined values.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws UnsupportedOperationException
     *             if the specified signal is not supported by the underlying device hardware.
     */
    public boolean getSignalState(int signalID) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Registers a {@link ModemSignalListener} instance which will get asynchronously notified when one of the
     * designated signals changes. Notification will automatically begin after registration completes.
     * <p />
     * If {@code listener} is {@code null} then the previously registered listener will be removed.
     * <p />
     * Only one listener can be registered at a particular time.
     *
     * @param listener
     *            the {@link ModemSignalListener} instance to be notified when a signal state changes.
     * @param signals
     *            the signals to monitor (bit-wise combination of the signal IDs: {@link #DCD_SIGNAL},
     *            {@link #DSR_SIGNAL}, {@link #RI_SIGNAL} or {@link #CTS_SIGNAL}).
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalArgumentException
     *             if {@code signals} is not a bit-wise combination of valid signal IDs.
     * @throws IllegalStateException
     *             if {@code listener} is not {@code null} and a listener is already registered for any of the specified
     *             signals.
     * @throws UnsupportedOperationException
     *             if the specified signal is not supported by the underlying device hardware.
     */
    void setSignalChangeListener(ModemSignalListener<P> listener, int signals) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets or clears the designated signal.
     *
     * @param signalID
     *            the ID of the signal ({@link #DTR_SIGNAL} or {@link #RTS_SIGNAL}).
     * @param state
     *            {@code true} to set; {@code false} to clear.
     * @throws IllegalArgumentException
     *             if {@code signalID} is not one of the defined values.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws UnsupportedOperationException
     *             if the specified signal is not supported by the underlying device hardware.
     */
    public void setSignalState(int signalID, boolean state) throws IOException, UnavailableDeviceException, ClosedDeviceException;
}
