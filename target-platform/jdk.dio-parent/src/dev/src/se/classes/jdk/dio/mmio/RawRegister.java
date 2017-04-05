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

import jdk.dio.ClosedDeviceException;
import jdk.dio.UnavailableDeviceException;
import java.io.IOException;

/**
 * The {@code RawRegister} interface provides methods for setting and getting the value of a register or memory area
 * holding a value of type {@code byte}, {@code short}, {@code int} or {@code long}. A {@code RawRegister} instance can
 * be obtained from a {@link MMIODevice} instance.
 * <p />
 * Developers should be aware when using this API of the potential performance and memory allocation penalty induced by
 * primitive auto-boxing. For intensive reading and writing of registers such as data input and data output registers,
 * the use of the {@link RawBlock} returned by {@link MMIODevice#getAsRawBlock() } should be preferred.
 *
 * @param <T>
 *            the type of the value held by the register.
 * @since 1.0
 */
public interface RawRegister<T extends Number> extends RawMemory {

    /**
     * Sets the value at the memory area associated with this object to the result of performing a logical AND of the
     * value at the memory area with the argument value.
     *
     * @param value
     *            the value to perform a logical AND with.
     * @throws IOException
     *             if the associated memory area is not writable.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void and(T value) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Clears (sets to {@code 0}) the designated bit value at the memory area associated with this object.
     *
     * @param index
     *            the index of the bit.
     * @throws IOException
     *             if the associated memory area is not writable.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalArgumentException
     *             if {@code index} is less than {@code 0} or greater or equal to the size in bits of the value at the
     *             memory area associated with this object.
     */
    void clearBit(int index) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Clears (sets to {@code 0}) the specified bits at the memory area associated with this object.
     *
     * @param mask
     *            the bit mask.
     * @throws IOException
     *             if the associated memory area is not writable.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void clearBits(T mask) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Gets the value at the memory area associated with this object.
     *
     * @return the value.
     * @throws IOException
     *             if the associated memory area is not readable.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    T get() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Returns the type of the value this {@code RawRegister} instance can hold.
     *
     * @return this {@code RawRegister} instance's value type.
     */
    Class<T> getType();

    /**
     * Checks whether the designated bit value at the memory area associated with this object is set (equal to {@code 1}
     * )
     *
     * @param index
     *            the index of the bit.
     * @return the bit value.
     * @throws IOException
     *             if the associated memory area is not readable.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalArgumentException
     *             if {@code index} is less than {@code 0} or greater or equal to the size in bits of the value at the
     *             memory area associated with this object.
     */
    boolean isBitSet(int index) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets the value at the memory area associated with this object to the result of performing a logical OR of the
     * value at the memory area with the argument value.
     *
     * @param value
     *            the value to perform a logical OR with.
     * @throws IOException
     *             if the associated memory area is not writable.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void or(T value) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets the value at the memory area associated with this object.
     *
     * @param value
     *            the value to set.
     * @throws IOException
     *             if the associated memory area is not writable.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void set(T value) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets (to {@code 1}) the designated bit value at the memory area associated with this object.
     *
     * @param index
     *            the index of the bit.
     * @throws IOException
     *             if the associated memory area is not writable.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IllegalArgumentException
     *             if {@code index} is less than {@code 0} or greater or equal to the size in bits of the value at the
     *             memory area associated with this object.
     */
    void setBit(int index) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets (to {@code 1}) the specified bits at the memory area associated with this object.
     *
     * @param mask
     *            the bit mask.
     * @throws IOException
     *             if the associated memory area is not writable.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void setBits(T mask) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sets the value at the memory area associated with this object to the result of performing a logical XOR of the
     * value at the memory area with the argument value.
     *
     * @param value
     *            the value to perform a logical XOR with.
     * @throws IOException
     *             if the associated memory area is not writable.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void xor(T value) throws IOException, UnavailableDeviceException, ClosedDeviceException;
}