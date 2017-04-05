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
package jdk.dio.mmio;

import jdk.dio.DeviceEvent;
import java.nio.ByteBuffer;

/**
 * The {@code MMIOEvent} class encapsulates events fired by devices mapped to memory.
 *
 * @see MMIODevice
 * @see MMIOEventListener
 * @since 1.0
 */
public class MMIOEvent extends DeviceEvent<MMIODevice> {

    /**
     * The captured content of the memory area or memory block designated upon registration.
     */
    private ByteBuffer capturedMemoryContent = null;

    /**
     * The captured value of the register designated upon registration.
     */
    private Number capturedRegisterValue = null;

    /**
     * This event ID.
     */
    private int id;

    /**
     * Creates a new {@link MMIOEvent} with the specified event ID and time-stamped
     * with the current time.
     *
     * @param device
     *            the source device.
     * @param id
     *            the event ID.
     * @throws NullPointerException
     *             if {@code device} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code id} is negative.
     */
    public MMIOEvent(MMIODevice device, int id) {
        this(device, id, System.currentTimeMillis(), 0);
    }

    /**
     * Creates a new {@link MMIOEvent} with the specified event ID and timestamp, and
     * with the captured content of the memory area or memory block designated upon registration.
     *
     * @param device
     *            the source device.
     * @param id
     *            the event ID.
     * @param capturedMemoryContent
     *            the captured content of the memory area or memory block designated upon registration.
     * @param timeStamp
     *            the timestamp (in milliseconds).
     * @param timeStampMicros
     *            the additional microseconds to the timestamp.
     * @throws NullPointerException
     *             if {@code device} or {@code capturedMemoryContent} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code id}, {@code timeStamp} or {@code timeStampMicros} is negative.
     */
    public MMIOEvent(MMIODevice device, int id, ByteBuffer capturedMemoryContent, long timeStamp, int timeStampMicros) {
        this.device = device;
        this.id = id;
        this.capturedRegisterValue = null;
        this.capturedMemoryContent = capturedMemoryContent;
        this.timeStamp = timeStamp;
        this.timeStampMicros = timeStampMicros;
        this.count = 1;
    }

    /**
     * Creates a new {@link MMIOEvent} with the specified event ID and timestamp.
     *
     * @param device
     *            the source device.
     * @param id
     *            the event ID.
     * @param timeStamp
     *            the timestamp (in milliseconds).
     * @param timeStampMicros
     *            the additional microseconds to the timestamp.
     * @throws NullPointerException
     *             if {@code device} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code id}, {@code timeStamp} or {@code timeStampMicros} is negative.
     */
    public MMIOEvent(MMIODevice device, int id, long timeStamp, int timeStampMicros) {
        this.device = device;
        this.id = id;
        this.capturedRegisterValue = null;
        this.capturedMemoryContent = null;
        this.timeStamp = timeStamp;
        this.timeStampMicros = timeStampMicros;
        this.count = 1;
    }

    /**
     * Creates a new {@link MMIOEvent} with the specified event ID and timestamp, and
     * the captured value of the register designated upon registration.
     *
     * @param <T> the type of the captured value.
     *
     * @param device
     *            the source device.
     * @param id
     *            the event ID.
     * @param capturedRegisterValue
     *            the captured value of the register designated upon registration.
     * @param timeStamp
     *            the timestamp (in milliseconds).
     * @param timeStampMicros
     *            the additional microseconds to the timestamp.
     * @throws NullPointerException
     *             if {@code device} or {@code capturedRegisterValue} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code id}, {@code timeStamp} or {@code timeStampMicros} is negative.
     */
    public <T extends Number> MMIOEvent(MMIODevice device, int id, T capturedRegisterValue, long timeStamp,
            int timeStampMicros) {
        this.device = device;
        this.id = id;
        this.capturedRegisterValue = capturedRegisterValue;
        this.capturedMemoryContent = null;
        this.timeStamp = timeStamp;
        this.timeStampMicros = timeStampMicros;
        this.count = 1;
    }

    /**
     * Coalesces events. Must only be called if the event has not yet been dispatched.
     *
     * @param <T> the type of the captured value.
     *
     * @param registerValue
     *            the captured value of the register designated upon registration.
     * @param memoryContent
     *            the captured content of the memory area or memory block designated upon registration.
     * @param timeStamp
     *            the timestamp (in milliseconds).
     * @param timeStampMicro
     *            the additional microseconds to the timestamp.
     */
    <T extends Number> void addOccurence(T registerValue, ByteBuffer memoryContent, long timeStamp) {
        this.count++;
        this.capturedRegisterValue = registerValue;
        this.capturedMemoryContent = memoryContent;
        this.lastTimeStamp = timeStamp;
        this.lastTimeStampMicros = timeStampMicros;
    }

    /**
     * Gets the captured content of the memory area or memory block designated upon registration.
     *
     * @return the captured content of the memory area or block; or {@code null} if no memory area or block content was
     *         captured. If the listener was registered using the MMIODevice#setMMIOEventListener(int, int, ByteBuffer, jdk.dio.mmio.MMIOEventListener) method
     *         the {@code ByteBuffer} returned is the {@code ByteBuffer} that was passed as the {@code captureBuffer} parameter.
     *
     * @see MMIODevice#setMMIOEventListener(int, java.lang.String, jdk.dio.mmio.MMIOEventListener)
     * @see MMIODevice#setMMIOEventListener(int, int, ByteBuffer, jdk.dio.mmio.MMIOEventListener)
     */
    public ByteBuffer getCapturedMemoryContent() {
        return capturedMemoryContent;
    }

    /**
     * Gets the captured value of the register designated upon registration.
     *
     * @param <T>
     *            the type of the value held by the register.
     * @return the captured value of the register designated upon registration or {@code null} if no register value was captured.
     *
     * @see MMIODevice#setMMIOEventListener(int, java.lang.String, jdk.dio.mmio.MMIOEventListener)
     */
    @SuppressWarnings("unchecked")
    public <T extends Number> T getCapturedRegisterValue() {
        return (T) capturedRegisterValue;
    }

    /**
     * Returns this event ID.
     *
     * @return this event ID.
     */
    public int getID() {
        return id;
    }
}