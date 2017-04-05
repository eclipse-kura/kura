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

package jdk.dio.counter;

import jdk.dio.DeviceEvent;

/**
 * The {@code CountingEvent} class encapsulates pulse counting conditions such as counter terminal
 * value reached or counting session time interval expired. <br />
 * When/if counting events for the same pulse counter are coalesced the count value and the type
 * (terminal value reached or time interval expired) retained are that of the last occurrence.
 *
 * @see PulseCounter
 * @see CountingListener
 * @since 1.0
 */
public class CountingEvent extends DeviceEvent<PulseCounter> {
    /**
     * Indicates that the pulse counting time interval has expired.
     */
    public static final int INTERVAL_EXPIRED = 1;

    /**
     * Indicates that the pulse count value has reached the defined terminal value.
     */
    public static final int TERMINAL_VALUE_REACHED = 0;

    /**
     * The actual counting time interval.
     */
    private long interval;

    /**
     * The type of counting condition (see {@link #TERMINAL_VALUE_REACHED} and
     * {@link #INTERVAL_EXPIRED}).
     */
    private int type;

    /**
     * The pulse count value.
     */
    private int value;

    /**
     * Creates a new {@code CountingEvent} with the specified type, pulse count value and actual
     * counting time interval and time-stamped with the current time.
     *
     * @param counter
     *            the source pulse counter.
     * @param type
     *            the type of counting condition being notified: {@link #INTERVAL_EXPIRED} or
     *            {@link #TERMINAL_VALUE_REACHED}.
     * @param value
     *            the pulse count value.
     * @param interval
     *            the actual counting time interval (in microseconds).
     * @throws NullPointerException
     *             if {@code counter} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code type} is not one of the defined types or if {@code value} or
     *             {@code interval} is negative.
     */
    public CountingEvent(PulseCounter counter, int type, int value, long interval) {
        this(counter, type, value, interval, System.currentTimeMillis(), 0);
    }

    /**
     * Creates a new {@code CountingEvent} with the specified type, pulse count value, actual
     * counting time interval and timestamp.
     *
     * @param counter
     *            the source pulse counter.
     * @param type
     *            the type of counting condition being notified: {@link #INTERVAL_EXPIRED} or
     *            {@link #TERMINAL_VALUE_REACHED}.
     * @param value
     *            the pulse count value.
     * @param interval
     *            the actual counting time interval (in microseconds).
     * @param timeStamp
     *            the timestamp (in milliseconds).
     * @param timeStampMicros
     *            the additional microseconds to the timestamp.
     * @throws NullPointerException
     *             if {@code counter} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code type} is not one of the defined types or if {@code value},
     *             {@code interval}, {@code timeStamp} or {@code timeStampMicros} is negative.
     */
    public CountingEvent(PulseCounter counter, int type, int value, long interval, long timeStamp, int timeStampMicros) {
        this.device = counter;
        this.type = type;
        this.value = value;
        this.interval = interval;
        this.timeStamp = timeStamp;
        this.timeStampMicros = timeStampMicros;
        this.count = 1;
        // checks for null
        counter.isOpen();
        if ((TERMINAL_VALUE_REACHED != type &&
            INTERVAL_EXPIRED != type) ||
            0 > value ||
            0 > interval ||
            0 > timeStamp ||
            0 > timeStampMicros) {
            throw new IllegalArgumentException();
        }
    }

    void addOccurence(int type, int value, long interval, long timeStamp) {
        this.type = type;
        this.value = value;
        this.interval = interval;
        this.count++;
        this.lastTimeStamp = timeStamp;
        this.lastTimeStampMicros = timeStampMicros;
    }

    /**
     * Returns the actual counting time interval (in microseconds). The actual counting time
     * interval may be smaller than the defined counting time interval if the count terminal value
     * has been reached.
     *
     * @return the actual counting time interval.
     */
    public long getInterval() {
        return interval;
    }

    /**
     * Returns the type of counting condition being notified.
     *
     * @return the type of counting condition being notified: {@link #INTERVAL_EXPIRED} or
     *         {@link #TERMINAL_VALUE_REACHED}..
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the pulse count value.
     *
     * @return the pulse count value.
     */
    public int getValue() {
        return value;
    }
}