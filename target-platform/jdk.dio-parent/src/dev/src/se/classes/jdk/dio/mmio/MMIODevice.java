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
import jdk.dio.Device;
import jdk.dio.DeviceManager;
import jdk.dio.UnavailableDeviceException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The {@code MMIODevice} class provides methods to retrieve memory-mapped registers and memory blocks of a device
 * device.
 * <p />
 * An MMIO device may be identified by the numeric ID and by the name (if any defined) that correspond to its
 * registered configuration. An {@code MMIODevice} instance can be opened by a call to one of the
 * {@link DeviceManager#open(int) DeviceManager.open(id,...)} methods using its ID or by a call to one of the
 * {@link DeviceManager#open(java.lang.String, java.lang.Class, java.lang.String[])
 * DeviceManager.open(name,...)} methods using its name. When an {@code MMIODevice} instance is opened with an
 * ad-hoc {@link MMIODeviceConfig} configuration (which includes its hardware addressing information) using one of the
 * {@link DeviceManager#open(jdk.dio.DeviceConfig) DeviceManager.open(config,...)} it is not
 * assigned any ID nor name.
 * <p />
 * With memory-mapped I/O, devices can be controlled by directly reading or writing to memory areas
 * representing the registers or memory blocks of the device. Each register or memory block is represented by
 * a {@link RawMemory} instance. Each register or memory block is also usually assigned a name which can be used for
 * name-based lookup. The complete memory area the device is mapped to can also be retrieved as single
 * {@link RawBlock} instance, allowing for offset-based access to all the registers and memory blocks of the device.
 * <p/>
 * An application can register an {@link MMIOEventListener} instance to monitor native events of the designated type
 * fired by the device. To register a {@link MMIOEventListener} instance, the application must call the
 * {@link #setMMIOEventListener(int, MMIOEventListener)} method. The registered listener can later on be removed by
 * calling the same method with a {@code null} listener parameter. Asynchronous notification may not be supported by all
 * memory-mapped devices. An attempt to set a listener on a memory-mapped device which does not supports it will result
 * in an {@link UnsupportedOperationException} being thrown.
 * <p />
 * When done, an application should call the {@link #close MMIODevice.close} method to close the MMIO device. Any
 * further attempt to access an MMIO device which has been closed will result in a {@link ClosedDeviceException}
 * been thrown.
 * <p />
 * Opening a {@link MMIODevice} instance is subject to permission checks (see {@link MMIOPermission}).
 * <p />
 * The event types (IDs) supported by a memory-mapped device are device as well as platform specific. For
 * example, each interrupt request line of a device may be mapped to a distinct event type ID. Refer to the
 * device data sheet and the platform configuration.
 * <p />
 * The {@code byte}, {@code short} and {@code int} values passed to and returned by this API have to be
 * interpreted as 8 bit, 16 bit and 32 bit unsigned quantities. Proper handling is needed when performing integer
 * arithmetic on these quantities.
 *
 * @see MMIOPermission
 * @see ClosedDeviceException
 * @since 1.0
 */
public interface MMIODevice extends Device<MMIODevice> {

    /**
     * Gets the complete memory area this device is mapped to as a {@code RawBlock} instance.
     *
     * @return a {@link RawBlock} object encapsulating whole memory area this device is mapped to.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    RawBlock getAsRawBlock() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Gets the designated memory block.
     *
     * @param name
     *            name of the memory block.
     * @return a {@link RawBlock} object encapsulating the designated memory block.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalArgumentException
     *             if the provided name does not correspond to any memory block.
     * @throws NullPointerException
     *             if {@code name} is {@code null}.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    RawBlock getBlock(String name) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Returns the byte ordering of this memory-mapped device.
     *
     * @return {@link #BIG_ENDIAN} if big-endian; {@link #LITTLE_ENDIAN} if little-endian; {@link #MIXED_ENDIAN}
     *         otherwise.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int getByteOrdering() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Gets the designated register holding a value of the designated type.
     *
     * @param name
     *            the name of the register.
     * @param type
     *            the type of the value held in the register.
     * @return a {@link RawRegister} object encapsulating the value of the designated register.
     * @throws IllegalArgumentException
     *             if the provided name does not correspond to any register or if the type of of the value held in the
     *             register does not match.
     * @throws NullPointerException
     *             if {@code name} or {@code type} is {@code null}.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    <T extends Number> RawRegister<T> getRegister(String name, Class<T> type) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * Registers a {@link MMIOEventListener} instance to monitor native events of the designated type fired by the
     * device mapped to this </code>MMIODevice</code> object and capture the content of designated memory
     * area.
     * <p />
     * While the listener can be triggered by hardware interrupts, there are no real-time guarantees of when the
     * listener will be called.
     * <p />
     * The content of the designated memory area will be captured upon the occurrence of the designated event.
     * Upon notification of the corresponding {@link MMIOEvent}, the captured content of a designated memory area
     * may be retrieved using {@link MMIOEvent#getCapturedMemoryContent MMIOEvent.getCapturedMemoryContent}.
      * <p />
     * If this {@code MMIODevice} is open in {@link DeviceManager#SHARED} access mode
     * the listeners registered by all the applications sharing the underlying device will get
     * notified of the events they registered for.
     * <p />
     * If {@code listener} is {@code null} then listener previously registered for the specified event type will be
     * removed.
     * <p />
     * Only one listener can be registered at a particular time for a particular event type.
     *
     * @param eventId
     *            ID of the native event to listen to.
     * @param capturedIndex
     *            the byte index in this device mapped raw memory area to capture.
     * @param captureBuffer
     *            the direct {@link ByteBuffer} to save the captured memory area in; up to {@code captureBuffer.remaining()} bytes will
     *            be copied from this device mapped raw memory area starting at {@code capturedIndex}
     *            into this buffer starting at the buffer's position at the moment this method is called.
     * @param listener
     *            the {@link MMIOEventListener} instance to be notified upon occurrence of the designated event.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws AccessOutOfBoundsException
     *             if {@code capturedIndex} and/or {@code captureBuffer.remaining()} would result in pointing outside this device
     *             mapped raw memory area.
     * @throws IllegalArgumentException
     *             if {@code eventId} does not correspond to any supported event or if {@code captureBuffer} is not a direct {@link ByteBuffer}.
     * @throws UnsupportedOperationException
     *             if this {@code MMIODevice} object does not support asynchronous event notification.
     * @throws IllegalStateException
     *             if {@code listener} is not {@code null} and a listener is already registered for the specified event
     *             type.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws NullPointerException
     *             if {@code captureBuffer} is {@code null}.
     */
    void setMMIOEventListener(int eventId, int capturedIndex, ByteBuffer captureBuffer,
            MMIOEventListener listener) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Registers a {@link MMIOEventListener} instance to monitor native events of the designated type fired by the
     * device mapped to this </code>MMIODevice</code> object. While the listener can be triggered by hardware
     * interrupts, there are no real-time guarantees of when the listener will be called.
     * <p />
     * If this {@code MMIODevice} is open in {@link DeviceManager#SHARED} access mode
     * the listeners registered by all the applications sharing the underlying device will get
     * notified of the events they registered for.
     * <p />
     * If {@code listener} is {@code null} then listener previously registered for the specified event type will be
     * removed.
     * <p />
     * Only one listener can be registered at a particular time for a particular event type.
     *
     * @param eventId
     *            ID of the native event to listen to.
     * @param listener
     *            the {@link MMIOEventListener} instance to be notified upon occurrence of the designated event.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalArgumentException
     *             if {@code eventId} does not correspond to any supported event.
     * @throws UnsupportedOperationException
     *             if this {@code MMIODevice} object does not support asynchronous event notification.
     * @throws IllegalStateException
     *             if {@code listener} is not {@code null} and a listener is already registered for the specified event
     *             type.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void setMMIOEventListener(int eventId, MMIOEventListener listener) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Registers a {@link MMIOEventListener} instance to monitor native events of the designated type fired by the
     * device mapped to this </code>MMIODevice</code> object and capture the content of designated register
     * or block.
     * <p />
     * While the listener can be triggered by hardware interrupts, there are no real-time guarantees of when the
     * listener will be called.
     * <p />
     * The content of the designated register or block will be captured upon the occurrence of the designated event.
     * Upon notification of the corresponding {@link MMIOEvent}, the captured value of a designated register may
     * be retrieved using {@link MMIOEvent#getCapturedRegisterValue MMIOEvent.getCapturedRegisterValue} while the captured content of a designated block
     * may be retrieved using {@link MMIOEvent#getCapturedMemoryContent MMIOEvent.getCapturedMemoryContent}.
     * <p />
     * If this {@code MMIODevice} is open in {@link DeviceManager#SHARED} access mode
     * the listeners registered by all the applications sharing the underlying device will get
     * notified of the events they registered for.
     * <p />
     * If {@code listener} is {@code null} then listener previously registered for the specified event type will be
     * removed.
     * <p />
     * Only one listener can be registered at a particular time for a particular event type.
     *
     * @param eventId
     *            ID of the native event to listen to.
     * @param capturedName
     *            the name of the register or memory block whose content is to be captured at the time of the underlying
     *            event occurs.
     * @param listener
     *            the {@link MMIOEventListener} instance to be notified upon occurrence of the designated event.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws IllegalArgumentException
     *             if {@code eventId} does not correspond to any supported event; or if the provided name does not
     *             correspond to any memory block or register.
     * @throws UnsupportedOperationException
     *             if this {@code MMIODevice} object does not support asynchronous event notification.
     * @throws IllegalStateException
     *             if {@code listener} is not {@code null} and a listener is already registered for the specified event
     *             type.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws NullPointerException
     *             if {@code capturedName} is {@code null}.
     */
    void setMMIOEventListener(int eventId, String capturedName, MMIOEventListener listener) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;
}