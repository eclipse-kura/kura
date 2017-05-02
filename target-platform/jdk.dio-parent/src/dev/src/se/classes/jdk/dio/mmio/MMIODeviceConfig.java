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

import jdk.dio.DeviceConfig;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.DeviceManager;
import java.util.Arrays;
import java.util.Objects;

/**
 * The {@code MMIODeviceConfig} class encapsulates the hardware addressing information, and static and dynamic
 * configuration parameters of an MMIO device.
 * <p />
 * Some hardware addressing parameter, and static and dynamic configuration parameters may be set to {@link #DEFAULT}.
 * Whether such default settings are supported is platform- as well as device driver-dependent.
 * <p />
 * An instance of {@code MMIODeviceConfig} can be passed to the {@link DeviceManager#open(DeviceConfig)} or
 * {@link DeviceManager#open(Class, DeviceConfig)} method to open the designated MMIO device with the specified
 * configuration. A {@link InvalidDeviceConfigException} is thrown when attempting to open a device with
 * an invalid or unsupported configuration.
 *
 * @see DeviceManager#open(DeviceConfig)
 * @see DeviceManager#open(Class, DeviceConfig)
 * @since 1.0
 */
public final class MMIODeviceConfig implements DeviceConfig<MMIODevice>, DeviceConfig.HardwareAddressing {

    /**
     * The {@code RawBlockConfig} class encapsulates the configuration parameters of a memory block.
     */
    public static final class RawBlockConfig extends RawMemoryConfig {

        private int size;

        /**
         * Constructs a new {@code RawBlockConfig} with the provided parameters.
         *
         * @param offset
         *            the offset of the register from the base address (a positive or zero integer).
         * @param name
         *            the name for the register.
         * @param size
         *            the size in bytes of the memory block (a positive integer).
         *
         * @throws IllegalArgumentException
         *             if any of the following is true:
         *             <ul>
         *             <li>{@code offset} is lesser than {@code 0};</li>
         *             <li>{@code size} is lesser than or equal to {@code 0}.</li>
         *             </ul>
         * @throws NullPointerException
         *             if {@code name} is {@code null}.
         */
        public RawBlockConfig(int offset, String name, int size) {
            super(offset, name);
            this.size = size;
        }

        /**
         * Gets the configured size in bytes of the memory block.
         *
         * @return the size in bytes of the memory block (a positive integer).
         */
        public int getSize() {
            return size;
        }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
        @Override
        public int hashCode() {
            int hash = 3;
            hash = 37 * hash + this.size;
            return hash;
        }

    /**
     * Checks two {@code RawBlockConfig} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     *
     * @return {@code true} if {@code obj} is a {@code RawBlockConfig} and has
     * the same hardware addressing information and configuration parameter values
     * as this {@code RawBlockConfig} object.
     */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final RawBlockConfig other = (RawBlockConfig) obj;
            if (this.size != other.size) {
                return false;
            }
            return true;
        }

    }

    /**
     * The {@code RawMemoryConfig} class encapsulates the configuration parameters of a generic raw memory area.
     */
    public static abstract class RawMemoryConfig {

        private String name;
        private int offset;

        /**
         * Constructs a new {@code RawMemoryConfig} with the provided parameters.
         *
         * @param offset
         *            the offset of the raw memory area from the base address (a positive or zero integer).
         * @param name
         *            the name for the raw memory area.
         *
         * @throws IllegalArgumentException
         *             if {@code offset} is lesser than {@code 0}.
         * @throws NullPointerException
         *             if {@code name} is {@code null}.
         */
        RawMemoryConfig(int offset, String name) {
            this.offset = offset;
            this.name = name;
        }

        /**
         * Gets the configured name for the raw memory area.
         *
         * @return the name for the raw memory area.
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the configured offset of the raw memory area from the base address.
         *
         * @return the offset of the raw memory area from the base address (a positive or zero integer).
         */
        public int getOffset() {
            return offset;
        }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.name);
            hash = 97 * hash + this.offset;
            return hash;
        }

    /**
     * Checks two {@code RawMemoryConfig} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     *
     * @return {@code true} if {@code obj} is a {@code RawMemoryConfig} and has
     * the same hardware addressing information and configuration parameter values
     * as this {@code RawMemoryConfig} object.
     */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final RawMemoryConfig other = (RawMemoryConfig) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            if (this.offset != other.offset) {
                return false;
            }
            return true;
        }

    }

    /**
     * The {@code RawRegisterConfig} class encapsulates the configuration parameters of a register.
     *
     * @param <T>
     *            the type of the value held by the register.
     */
    public static final class RawRegisterConfig<T extends Number> extends RawMemoryConfig {
        private Class<T> type;

        /**
         * Constructs a new {@code RawRegisterConfig} with the provided parameters.
         *
         * @param offset
         *            the offset of the register from the base address (a positive or zero integer).
         * @param name
         *            the name for the register.
         * @param type
         *            the type of the value held by the register, one of: {@link Byte}, {@link Short} or {@link Integer}
         *            .
         *
         * @throws IllegalArgumentException
         *             if any of the following is true:
         *             <ul>
         *             <li>{@code offset} is lesser than {@code 0};</li>
         *             <li>{@code type} is not one of the defined values.</li>
         *             </ul>
         * @throws NullPointerException
         *             if {@code name} is {@code null}.
         */
        public RawRegisterConfig(int offset, String name, Class<T> type) {
            super(offset, name);
            this.type = type;
        }

        /**
         * Gets the configured type of the value held by the register.
         *
         * @return the type of the value held by the register, one of: {@link Byte}, {@link Short} or {@link Integer}.
         */
        public Class<T> getType() {
            return type;
        }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
        @Override
        public int hashCode() {
            int hash = 5;
            hash = 67 * hash + Objects.hashCode(this.type);
            return hash;
        }

    /**
     * Checks two {@code RawRegisterConfig} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     *
     * @return {@code true} if {@code obj} is a {@code RawRegisterConfig} and has
     * the same hardware addressing information and configuration parameter values
     * as this {@code RawRegisterConfig} object.
     */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final RawRegisterConfig<T> other = (RawRegisterConfig<T>) obj;
            if (!Objects.equals(this.type, other.type)) {
                return false;
            }
            return true;
        }
    }

    private long address;
    private int controllerNumber = DEFAULT;
    private String controllerName = null;

    private int byteOrdering = DEFAULT;

    private RawMemoryConfig[] memConfigs = null;

    private int size;

    /**
     * Creates a new {@code MMIODeviceConfig} with the specified hardware addressing information and configuration
     * parameters.
     * <p />
     * If no raw block and raw register configuration is provided, the specified memory area will be mapped to
     * the {@link RawBlock} instance returned by a call to {@link MMIODevice#getAsRawBlock MMIODevice.getAsRawBlock}.
     * <p />
     * If the designated memory region is protected or if it overlaps with that of an existing
     * MMIO device configuration and the requested access mode ({@link DeviceManager#EXCLUSIVE} or
     * {@link DeviceManager#SHARED}) is incompatible or unsupported, attempting to open an {@code MMIODevice}
     * device using this configuration may result in either a {@code SecurityException} or
     * a {@link InvalidDeviceConfigException} to be thrown.
     *
    * @param address
     *            the memory address of the device (a positive or zero integer).
     * @param size
     *            the size of the memory-mapped area of the device (a positive integer).
     * @param byteOrdering
     *            the byte ordering of the device, one of: {@link MMIODevice#BIG_ENDIAN},
     *            {@link MMIODevice#LITTLE_ENDIAN}, {@link MMIODevice#MIXED_ENDIAN} or {@link MMIODeviceConfig#DEFAULT}.
     * @param memConfigs
     *            the raw block and raw register configurations (may be {@code null} or empty).
     *
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code address} is not in the defined range;</li>
     *             <li>{@code size} is not in the defined range;</li>
     *             <li>{@code byteOrdering} is not one of the defined values.</li>
     *             </ul>
     * @throws IndexOutOfBoundsException
     *             if any of the {@code memConfigs} elements is pointing outside of the defined address range ([
     *             {@code 0}-</code>size</code>]).
     */
    public MMIODeviceConfig(long address, int size, int byteOrdering, RawMemoryConfig... memConfigs) {
        this.address = address;
        this.size = size;
        this.byteOrdering = byteOrdering;
        if (memConfigs != null) {
            this.memConfigs = memConfigs.clone();
        }
    }

    /**
     * Creates a new {@code MMIODeviceConfig} with the specified hardware addressing information and configuration
     * parameters.
     * <p />
     * If no raw block and raw register configuration is provided, the specified memory area will be mapped to
     * the {@link RawBlock} instance returned by a call to {@link MMIODevice#getAsRawBlock MMIODevice.getAsRawBlock}.
     *
     * @param controllerName
     *            the controller name (such as its <em>device file</em> name on UNIX systems).
     * @param address
     *            the memory address of the device (a positive or zero integer)or {@link MMIODeviceConfig#DEFAULT}.
     * @param size
     *            the size of the memory-mapped area of the device (a positive integer).
     * @param byteOrdering
     *            the byte ordering of the device, one of: {@link MMIODevice#BIG_ENDIAN},
     *            {@link MMIODevice#LITTLE_ENDIAN}, {@link MMIODevice#MIXED_ENDIAN} or {@link MMIODeviceConfig#DEFAULT}.
     * @param memConfigs
     *            the raw block and raw register configurations (may be {@code null} or empty).
     *
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code address} is not in the defined range;</li>
     *             <li>{@code size} is not in the defined range;</li>
     *             <li>{@code byteOrdering} is not one of the defined values.</li>
     *             </ul>
     * @throws NullPointerException
     *             if {@code controllerName} is {@code null}.
     * @throws IndexOutOfBoundsException
     *             if any of the {@code memConfigs} elements is pointing outside of the defined address range ([
     *             {@code 0}-</code>size</code>]).
     */
    public MMIODeviceConfig(String controllerName, long address, int size, int byteOrdering, RawMemoryConfig... memConfigs) {
        this.controllerName = controllerName;
        this.address = address;
        this.size = size;
        this.byteOrdering = byteOrdering;
        if (memConfigs != null) {
            this.memConfigs = memConfigs.clone();
        }
    }

    /**
     * Gets the configured memory address of the MMIO device.
     *
     * @return the memory address of the MMIO device (a positive or zero integer) or {@link MMIODeviceConfig#DEFAULT}..
     */
    public long getAddress() {
        return address;
    }

    /**
     * Gets the configured controller name (such as its <em>device file</em> name on UNIX systems).
     *
     * @return the controller name or {@code null}.
     */
    @Override
    public String getControllerName() {
        return controllerName;
    }

    /**
     * Gets the configured controller number.
     *
     * @return {@link #DEFAULT}.
     */
    @Override
    public int getControllerNumber() {
        return controllerNumber;
    }

    /**
     * Gets the configured byte ordering of the MMIO device.
     *
     * @return the byte ordering of the MMIO device, one of: {@link MMIODevice#BIG_ENDIAN},
     *         {@link MMIODevice#LITTLE_ENDIAN}, {@link MMIODevice#MIXED_ENDIAN} or {@link MMIODeviceConfig#DEFAULT}.
     */
    public int getByteOrdering() {
        return byteOrdering;
    }

    /**
     * Gets the set of configured registers and memory blocks.
     *
     * @return an array (a defensive copy thereof) containing the set of configured registers and memory blocks or
     *         {@code null} if none is defined.
     */
    public RawMemoryConfig[] getRawMemoryConfigs() {
        if (memConfigs != null) {
            return memConfigs.clone();
        }
        return null;
    }

    /**
     * Gets the configured size of the memory-mapped area of the MMIO device.
     *
     * @return the size of the memory-mapped area of the device (a positive integer).
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (int) (this.address ^ (this.address >>> 32));
        hash = 97 * hash + this.controllerNumber;
        hash = 97 * hash + Objects.hashCode(this.controllerName);
        hash = 97 * hash + this.byteOrdering;
        for (RawMemoryConfig memConfig : this.memConfigs) {
            hash = 97 * hash + Objects.hashCode(memConfig);       }
        hash = 97 * hash + this.size;
        return hash;
    }

    /**
     * Checks two {@code MMIODeviceConfig} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     *
     * @return {@code true} if {@code obj} is a {@code MMIODeviceConfig} and has
     * the same hardware addressing information and configuration parameter values
     * as this {@code MMIODeviceConfig} object.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MMIODeviceConfig other = (MMIODeviceConfig) obj;
        if (this.address != other.address) {
            return false;
        }
        if (this.controllerNumber != other.controllerNumber) {
            return false;
        }
        if (!Objects.equals(this.controllerName, other.controllerName)) {
            return false;
        }
        if (this.byteOrdering != other.byteOrdering) {
            return false;
        }
        if (!Arrays.equals(this.memConfigs, other.memConfigs)) {
            return false;
        }
        if (this.size != other.size) {
            return false;
        }
        return true;
    }
}
