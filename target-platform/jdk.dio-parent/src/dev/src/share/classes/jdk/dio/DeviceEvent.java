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

/**
 * The {@code DeviceEvent} class encapsulates events fired by or on behalf of a
 * device. An event may correspond to an hardware interrupt or to some software signal.
 * <p />
 * When an event burst occurs - that is more events occurred than can be handled - events may be
 * coalesced. Coalescing of events is platform and device-dependent.
 *
 * @param <P>
 *            the device type the event is defined for.
 * @since 1.0
 */
public abstract class DeviceEvent<P extends Device<? super P>> {

    /**
     * The number of underlying coalesced hardware interrupts or software signals this event may
     * represent.
     */
    protected int count;
    /**
     * The time (in milliseconds) when the last coalesced event occurred. If events were not
     * coalesced then the time is the same as that of the first event.
     */
    protected long lastTimeStamp;
    /**
     * The additional microseconds to the timestamp for when the last coalesced event occurred. If
     * events were not coalesced then this is the same as that of the first event.
     * <p />
     * The actual last timestamp in microseconds is equal to: <i>(lastTimeStamp * 1000) +
     * lastTimeStampMicros</i>.
     */
    protected int lastTimeStampMicros;
    /**
     * The {@code Device} instance that fired this event or for which this event was fired.
     */
    protected P device;
    /**
     * The time (in milliseconds) when this event (first) occurred. If events were coalesced then
     * the time is that of the first event.
     */
    protected long timeStamp;
    /**
     * The additional microseconds to the timestamp for when this event (first) occurred. If events
     * were coalesced then this is that of the first event.
     * <p />
     * The actual timestamp in microseconds is equal to: <i>(timeStamp * 1000) +
     * timeStampMicros</i>.
     */
    protected int timeStampMicros;

    /**
     * Returns the number of underlying coalesced hardware interrupts or software signals this event
     * may represent.
     *
     * @return the number of underlying coalesced hardware interrupts software signals this event
     *         may represent.
     */
    public final int getCount() {
        return count;
    }

    /**
     * Returns the time (in milliseconds) when the last coalesced event occurred. If events were not
     * coalesced then the time is the same as that of the first event.
     *
     * @return the time (in milliseconds) when this event last occurred.
     */
    public final long getLastTimeStamp() {
        return timeStamp;
    }

    /**
     * Returns the additional microseconds to the timestamp for when the last coalesced event
     * occurred. If events were not coalesced then this is the same as that of the first event.
     *
     * @return the additional microseconds to the timestamp for when the last coalesced event
     *         occurred.
     */
    public final int getLastTimeStampMicros() {
        return timeStampMicros;
    }

    /**
     * Returns the {@code Device} instance that fired this event or for which this event was
     * fired.
     *
     * @return the {@code Device} instance that fired this event.
     */
    public final P getDevice() {
        return device;
    }

    /**
     * Returns the time (in milliseconds) when this event (first) occurred. If events were coalesced
     * then the time is that of the first event.
     *
     * @return the time (in milliseconds) when this event (first) occurred.
     */
    public final long getTimeStamp() {
        return timeStamp;
    }

    /**
     * Returns the additional microseconds to the timestamp for when this event (first) occurred. If
     * events were coalesced then the time is that of the first event.
     *
     * @return the additional microseconds to the timestamp for when this event (first) occurred.
     */
    public final int getTimeStampMicros() {
        return timeStampMicros;
    }
}
