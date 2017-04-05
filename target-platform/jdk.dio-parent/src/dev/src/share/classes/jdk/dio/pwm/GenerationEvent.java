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

package jdk.dio.pwm;

import jdk.dio.DeviceEvent;

/**
 * The {@code GenerationEvent} class encapsulates pulse a generation completion condition (i.e. generated pulse count
 * value reached). <br />
 * This kind of events is <em>never coalesced</em>.
 *
 * @see PWMChannel
 * @see GenerationListener
 * @since 1.0
 */
public class GenerationEvent extends DeviceEvent<PWMChannel> {

    /**
     * The generated pulse count value.
     */
    private int pulseCount;

    /**
     * Creates a new {@link GenerationEvent} with the specified type, pulse count and time-stamped with the current
     * time.
     *
     * @param channel
     *            the source PWM channel.
     * @param count
     *            the generated pulse count.
     * @throws NullPointerException
     *             if {@code channel} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code count} is negative.
     */
    public GenerationEvent(PWMChannel channel, int count) {
        this(channel, count, System.currentTimeMillis(), 0);
    }

    /**
     * Creates a new {@link GenerationEvent} with the specified type, pulse count and timestamp.
     *
     * @param channel
     *            the source pulse channel.
     * @param count
     *            the generated pulse count.
     * @param timeStamp
     *            the timestamp (in milliseconds).
     * @param timeStampMicros
     *            the additional microseconds to the timestamp.
     * @throws NullPointerException
     *             if {@code channel} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code count}, {@code timeStamp} or {@code timeStampMicros} is negative.
     */
    public GenerationEvent(PWMChannel channel, int count, long timeStamp, int timeStampMicros) {
        if (null == channel) {
            throw new NullPointerException();
        }

        if (count < 0 || timeStamp < 0 || timeStampMicros < 0) {
            throw new IllegalArgumentException();
        }

        this.device = channel;
        this.pulseCount = count;
        this.timeStamp = timeStamp;
        this.timeStampMicros = timeStampMicros;
        this.count = 1;
    }

    void addOccurence(int count, long timeStamp) {
        this.pulseCount = count;
        this.count++;
        this.lastTimeStamp = timeStamp;
        this.lastTimeStampMicros = timeStampMicros;
    }

    /**
     * Returns the generated pulse count value.
     *
     * @return the generated pulse count value.
     */
    public int getPulseCount() {
        return pulseCount;
    }
}