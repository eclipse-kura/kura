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

package jdk.dio.generic;

import jdk.dio.DeviceEvent;

/**
 * The {@code GenericEvent} class encapsulates events fired by devices that implement the
 * {@link GenericDevice} interface.
 *
 * @see GenericDevice
 * @see GenericEventListener
 * @since 1.0
 */
public class GenericEvent extends DeviceEvent<GenericDevice> {

    /**
     * Event ID indicating an input buffer overrun.
     */
    public static final int INPUT_BUFFER_OVERRUN = 1;

    /**
     * Event ID indicating that input data is available for reading.
     */
    public static final int INPUT_DATA_AVAILABLE = 0;

    /**
     * Event ID indicating that the output buffer is empty and that additional data may be written.
     */
    public static final int OUTPUT_BUFFER_EMPTY = 2;

    /**
     * This event ID.
     */
    private int id;

    /**
     * Creates a new {@link GenericEvent} with the specified value and time-stamped with the current
     * time.
     *
     * @param device
     *            the source device.
     * @param id
     *            the event ID: {@link #INPUT_DATA_AVAILABLE}, {@link #INPUT_BUFFER_OVERRUN},
     *            {@link #OUTPUT_BUFFER_EMPTY} or other device-specific event ID.
     * @throws NullPointerException
     *             if {@code device} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code id} is negative.
     */
    public GenericEvent(GenericDevice device, int id) {
        this(device, id, System.currentTimeMillis(), 0);
    }

    /**
     * Creates a new {@link GenericEvent} with the specified value and timestamp.
     *
     * @param device
     *            the source device.
     * @param id
     *            the event ID: {@link #INPUT_DATA_AVAILABLE}, {@link #INPUT_BUFFER_OVERRUN},
     *            {@link #OUTPUT_BUFFER_EMPTY} or other device-specific event ID.
     * @param timeStamp
     *            the timestamp (in milliseconds).
     * @param timeStampMicros
     *            the additional microseconds to the timestamp.
     * @throws NullPointerException
     *             if {@code device} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code id} is negative or if {@code timeStamp} or {@code timeStampMicros} is
     *             negative.
     */
    public GenericEvent(GenericDevice device, int id, long timeStamp, int timeStampMicros) {
        // NPE check
        device.getDescriptor();
        this.device = device;
        this.id = id;
        this.timeStamp = timeStamp;
        this.timeStampMicros = timeStampMicros;
        this.count = 1;
        if (id < 0 || timeStamp < 0 || timeStampMicros < 0) {
            throw new IllegalArgumentException();
        }
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
     *         {@link #OUTPUT_BUFFER_EMPTY} or other device-specific event IDs.
     */
    public int getID() {
        return id;
    }
}