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

package jdk.dio.adc;

import jdk.dio.DeviceEvent;

/**
 * The {@code MonitoringEvent} class encapsulates ADC channel under- and over-threshold value
 * conditions. <br />
 * When/if range events for the same ADC channel are coalesced the value and the type (in or out of
 * range) retained are that of the last occurrence.
 *
 * @see ADCChannel
 * @see MonitoringListener
 * @since 1.0
 */
public class MonitoringEvent extends DeviceEvent<ADCChannel> {
    /**
     * Indicates that the ADC channel value got back within the defined range.
     */
    public static final int BACK_TO_RANGE = 1;

    /**
     * Indicates that the ADC channel value exceeded the defined range.
     */
    public static final int OUT_OF_RANGE = 0;

    /**
     * The type of range condition (see {@link #OUT_OF_RANGE OUT_OF_RANGE} and
     * {@link #BACK_TO_RANGE BACK_TO_RANGE}).
     */
    private int type;

    /**
     * The new ADC channel's value.
     */
    private int value;

    /**
     * Creates a new {@code MonitoringEvent} with the specified raw sampled value and time-stamped
     * with the current time..
     *
     * @param channel
     *            the source ADC channel.
     * @param type
     *            the type of range condition being notified: {@link #OUT_OF_RANGE} or
     *            {@link #BACK_TO_RANGE}.
     * @param value
     *            the raw sampled value.
     * @throws NullPointerException
     *             if {@code channel} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code type} is not one of the defined types.
     */
    public MonitoringEvent(ADCChannel channel, int type, int value) {
        this(channel, type, value, System.currentTimeMillis(), 0);
    }

    /**
     * Creates a new {@code MonitoringEvent} with the specified raw sampled value and timestamp.
     *
     * @param channel
     *            the source ADC channel.
     * @param type
     *            the type of range condition being notified: {@link #OUT_OF_RANGE} or
     *            {@link #BACK_TO_RANGE}.
     * @param value
     *            the raw sampled value.
     * @param timeStamp
     *            the timestamp (in milliseconds).
     * @param timeStampMicros
     *            the additional microseconds to the timestamp.
     * @throws NullPointerException
     *             if {@code channel} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code type} is not one of the defined types or if {@code timeStamp} or
     *             {@code timeStampMicros} is negative.
     */
    public MonitoringEvent(ADCChannel channel, int type, int value, long timeStamp, int timeStampMicros) {
        if (null == channel) {
            throw new NullPointerException();
        }

        if ((type != BACK_TO_RANGE && type != OUT_OF_RANGE) || timeStamp < 0 || timeStampMicros < 0) {
            throw new IllegalArgumentException();
        }
        this.device = channel;
        this.type = type;
        this.value = value;
        this.timeStamp = timeStamp;
        this.timeStampMicros = timeStampMicros;
        this.count = 1;
    }

    void addOccurence(int value, long timeStamp, int timeStampMicros) {
        this.value = value;
        this.count++;
        this.lastTimeStamp = timeStamp;
        this.lastTimeStampMicros = timeStampMicros;
    }

    /**
     * Returns the type of range condition being notified.
     *
     * @return the type of range condition being notified: {@link #OUT_OF_RANGE} or
     *         {@link #BACK_TO_RANGE}.
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the new ADC channel's value.
     *
     * @return the new ADC channel's value.
     */
    public int getValue() {
        return value;
    }
}