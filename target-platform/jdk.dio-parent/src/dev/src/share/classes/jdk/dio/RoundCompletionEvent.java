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

package jdk.dio;

import java.nio.*;

/**
 * The {@code RoundCompletionEvent} class encapsulates the completion condition of a round of I/O
 * operation. <br />
 * This kind of events is <em>never coalesced</em>.
 *
 * @param <P>
 *            the device type the event is defined for.
 * @param <B>
 *            the I/O buffer type.
 * @see InputRoundListener
 * @see OutputRoundListener
 * @since 1.0
 */
public class RoundCompletionEvent<P extends Device<? super P>, B extends Buffer> extends DeviceEvent<P> {
    /**
     * The input or output buffer.
     */
    private B buffer;

    /**
     * The number of elements input or output during the round.
     */
    private int number;

    /**
     * Whether an input buffer overrun or output buffer underrun condition occurred and the I/O
     * operation had to be suspended temporarily.
     */
    private boolean onError;

    /**
     * Creates a new {@link RoundCompletionEvent} with the specified I/O buffer and time-stamped
     * with the current time.
     *
     * @param device
     *            the source device.
     * @param buffer
     *            the input or output buffer.
     * @param number
     *            the number of elements input or output during the round.
     * @throws NullPointerException
     *             if {@code device} or {@code buffer} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code number} is negative.
     */
    public RoundCompletionEvent(P device, B buffer, int number) {
        this(device, buffer, number, System.currentTimeMillis(), 0);
    }

    /**
     * Creates a new {@link RoundCompletionEvent} with the specified I/O buffer and timestamp.
     *
     * @param device
     *            the source device.
     * @param buffer
     *            the I/O buffer.
     * @param number
     *            the number of elements input or output during the round.
     * @param timeStamp
     *            the timestamp (in milliseconds).
     * @param timeStampMicros
     *            the additional microseconds to the timestamp.
     * @throws NullPointerException
     *             if {@code device} or {@code buffer} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code timeStamp}, {@code timeStampMicros} or {@code number} is negative.
     */
    public RoundCompletionEvent(P device, B buffer, int number, long timeStamp, int timeStampMicros) {
        if (null == device || buffer == null) {
            throw new NullPointerException();
        }

        if (number < 0 || number > buffer.limit() || timeStamp < 0 || timeStampMicros < 0) {
            throw new IllegalArgumentException();
        }
        this.device = device;
        this.buffer = buffer;
        this.number = number;
        this.timeStamp = timeStamp;
        this.timeStampMicros = timeStampMicros;
    }

    /**
     * Returns the input or output buffer.
     *
     * @return the input or output buffer.
     */
    public B getBuffer() {
        return buffer;
    }

    /**
     * Return the number of elements input or output during the round.
     *
     * @return the number of elements input or output during the round.
     */
    public int getNumber() {
        return number;
    }

    /**
     * Indicates whether an input buffer overrun or output buffer underrun condition occurred and
     * the I/O operation had to be suspended temporarily.
     *
     * @return {@code true} if an input buffer overrun or output buffer underrun condition occurred.
     */
    public boolean isOnError() {
        return onError;
    }
}