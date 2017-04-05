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

import jdk.dio.DeviceEvent;

/**
 * The {@code UARTEvent} class encapsulates events fired by devices that
 * implement the {@link UART} interface.
 *
 * @see UART
 * @see UARTEventListener
 * @since 1.0
 */
public class UARTEvent extends DeviceEvent<UART> {

    /**
     * Event ID indicating that input buffer overrun.
     */
    public static final int INPUT_BUFFER_OVERRUN = 1;
    /**
     * Event ID indicating that input data is available for reading.
     */
    public static final int INPUT_DATA_AVAILABLE = 0;
    /**
     * Event ID indicating that the output buffer is empty and that additional
     * data may be written.
     */
    public static final int OUTPUT_BUFFER_EMPTY = 2;
    /**
     * Event ID indicating a break interrupt.
     */
    public static final int BREAK_INTERRUPT = 4;
    /**
     * Event ID indicating a parity error.
     */
    public static final int PARITY_ERROR = 8;
    /**
     * Event ID indicating a parity error.
     */
    public static final int FRAMING_ERROR = 16;
    /**
     * This event ID.
     */
    private int id;

    /**
     * Creates a new {@link UARTEvent} with the specified value and time-stamped
     * with the current time.
     *
     * @param uart the source uart.
     * @param id the event ID: {@link #INPUT_DATA_AVAILABLE}, {@link #INPUT_BUFFER_OVERRUN},
     *         {@link #OUTPUT_BUFFER_EMPTY}, {@link #BREAK_INTERRUPT},
     *         {@link #FRAMING_ERROR} or {@link #PARITY_ERROR}.
     * @throws NullPointerException if {@code uart} is {@code null}.
     * @throws IllegalArgumentException if {@code id} is not a valid event ID.
     */
    public UARTEvent(UART uart, int id) {
        this(uart, id, System.currentTimeMillis(), 0);
    }

    /**
     * Creates a new {@link UARTEvent} with the specified value and timestamp.
     *
     * @param uart the source uart.
     * @param id the event ID: {@link #INPUT_DATA_AVAILABLE}, {@link #INPUT_BUFFER_OVERRUN},
     *         {@link #OUTPUT_BUFFER_EMPTY}, {@link #BREAK_INTERRUPT},
     *         {@link #FRAMING_ERROR} or {@link #PARITY_ERROR}.
     * @param timeStamp the timestamp (in milliseconds).
     * @param timeStampMicros the additional microseconds to the timestamp.
     * @throws NullPointerException if {@code uart} is {@code null}.
     * @throws IllegalArgumentException if {@code id} is not a valid event ID or
     * if {@code timeStamp} or {@code timeStampMicros} is negative.
     */
    public UARTEvent(UART uart, int id, long timeStamp, int timeStampMicros) {
        if(null == uart){
            throw new NullPointerException();
        }
        this.device = uart;
        this.id = id;
        this.timeStamp = timeStamp;
        this.timeStampMicros = timeStampMicros;
        this.count = 1;
    }

    void addOccurence(long timeStamp) {
        this.count++;
        this.lastTimeStamp = timeStamp;
        this.lastTimeStampMicros = timeStampMicros;
    }

    /**
     * Returns this event ID.
     *
     * @return this event ID: {@link #INPUT_DATA_AVAILABLE}, {@link #INPUT_BUFFER_OVERRUN},
     *         {@link #OUTPUT_BUFFER_EMPTY}, {@link #BREAK_INTERRUPT},
     *         {@link #FRAMING_ERROR} or {@link #PARITY_ERROR}.
     */
    public int getID() {
        return id;
    }
}