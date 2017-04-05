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

import jdk.dio.Device;
import jdk.dio.DeviceEvent;

/**
 * The {@code ModemSignalEvent} class encapsulates modem signal state changes. When/if signal state change events for
 * the same device are coalesced the value retained is that of the last occurrence.
 *
 * @param <P>
 *            the device type the event is defined for.
 *
 * @see ModemSignalListener
 * @see ModemSignalsControl
 * @since 1.0
 */
public class ModemSignalEvent<P extends Device<? super P>> extends DeviceEvent<P> {
    /**
     * The signal ID.
     */
    protected int signalID;
    /**
     * The signal state.
     */
    protected boolean signalState;

    /**
     * Creates a new {@link ModemSignalEvent} with the specified value and time-stamped with the current time.
     *
     * @param device
     *            the source device.
     * @param signalID
     *            the ID of the signal that changed ({@link ModemSignalsControl#DCD_SIGNAL},
     *            {@link ModemSignalsControl#DSR_SIGNAL}, {@link ModemSignalsControl#RI_SIGNAL} or
     *            {@link ModemSignalsControl#CTS_SIGNAL}).
     * @param signalState
     *            the new signal state.
     * @throws NullPointerException
     *             if {@code device} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code signalID} is not a valid signal ID.
     */
    public ModemSignalEvent(P device, int signalID, boolean signalState) {
        this(device, signalID, signalState, System.currentTimeMillis(), 0);
    }

    /**
     * Creates a new {@link ModemSignalEvent} with the specified value and timestamp.
     *
     * @param device
     *            the source device.
     * @param signalID
     *            the ID of the signal that changed ({@link ModemSignalsControl#DCD_SIGNAL},
     *            {@link ModemSignalsControl#DSR_SIGNAL}, {@link ModemSignalsControl#RI_SIGNAL} or
     *            {@link ModemSignalsControl#CTS_SIGNAL}).
     * @param signalState
     *            the new signal state.
     * @param timeStamp
     *            the timestamp (in milliseconds).
     * @param timeStampMicros
     *            the additional microseconds to the timestamp.
     * @throws NullPointerException
     *             if {@code device} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code signalID} is not a valid signal ID or if {@code timeStamp} or {@code timeStampMicros} is
     *             negative.
     */
    public ModemSignalEvent(P device, int signalID, boolean signalState, long timeStamp, int timeStampMicros) {
        this.device = device;
        this.signalID = signalID;
        this.signalState = signalState;
        this.timeStamp = timeStamp;
        this.timeStampMicros = timeStampMicros;
        this.count = 1;
    }

    void addOccurence(boolean signalState, long timeStamp) {
        this.signalState = signalState;
        this.count++;
        this.lastTimeStamp = timeStamp;
        this.lastTimeStampMicros = timeStampMicros;
    }

    /**
     * Returns the signal ID.
     *
     * @return the signal ID ({@link ModemSignalsControl#DCD_SIGNAL}, {@link ModemSignalsControl#DSR_SIGNAL},
     *         {@link ModemSignalsControl#RI_SIGNAL} or {@link ModemSignalsControl#CTS_SIGNAL}).
     */
    public int getSignalID() {
        return signalID;
    }

    /**
     * Returns the new signal state.
     *
     * @return the new signal state.
     */
    public boolean getSignalState() {
        return signalState;
    }
}