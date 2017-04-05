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

package jdk.dio.gpio;

import jdk.dio.DeviceEvent;

/**
 * The {@code PinEvent} class encapsulates GPIO pin value changes. When/if value change events for
 * the same GPIO pin are coalesced the value retained is that of the last occurrence.
 *
 * @see GPIOPin
 * @see PinListener
 * @since 1.0
 */
public class PinEvent extends DeviceEvent<GPIOPin> {

    /**
     * The new GPIO pin's value.
     */
    private boolean value;

    /**
     * Creates a new {@link PinEvent} with the specified value and time-stamped with the current
     * time.
     *
     * @param pin
     *            the source GPIO pin.
     * @param value
     *            the new value.
     * @throws NullPointerException
     *             if {@code pin} is {@code null}.
     */
    public PinEvent(GPIOPin pin, boolean value) {
        this(pin, value, System.currentTimeMillis(), 0);
    }

    /**
     * Creates a new {@link PinEvent} with the specified value and timestamp.
     *
     * @param pin
     *            the source GPIO pin.
     * @param value
     *            the new value.
     * @param timeStamp
     *            the timestamp (in milliseconds).
     * @param timeStampMicros
     *            the additional microseconds to the timestamp.
     * @throws NullPointerException
     *             if {@code pin} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code timeStamp} or {@code timeStampMicros} is negative.
     */
    public PinEvent(GPIOPin pin, boolean value, long timeStamp, int timeStampMicros) {
        // checks for null
        pin.isOpen();
        if (0 > timeStamp || 0 > timeStampMicros) {
            throw new IllegalArgumentException();
        }
        this.device = pin;
        this.value = value;
        this.timeStamp = timeStamp;
        this.timeStampMicros = timeStampMicros;
        this.count = 1;
    }

    void addOccurence(boolean value, long timeStamp) {
        this.value = value;
        this.count++;
        this.lastTimeStamp = timeStamp;
        this.lastTimeStampMicros = timeStampMicros;
    }

    /**
     * Returns the new GPIO pin's value.
     *
     * @return the new GPIO pin's value.
     */
    public boolean getValue() {
        return value;
    }
}