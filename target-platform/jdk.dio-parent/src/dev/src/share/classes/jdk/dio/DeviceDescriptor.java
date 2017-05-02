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
 * The {@code DeviceDescriptor} interface encapsulates the identifying and descriptive
 * information of a device as well as its registered configuration (hardware addressing
 * parameters, and static and dynamic configuration parameters).
 *
 * @param <P>
 *            the device type the descriptor is defined for.
 * @since 1.0
 */
public interface DeviceDescriptor<P extends Device<? super P>> {

    /**
     * Undefined device numeric ID.
     */
    int UNDEFINED_ID = -1;

    /**
     * Returns the numeric ID of the device. If the device has been opened with an
     * ad-hoc configuration such as by calling
     * {@link DeviceManager#open(java.lang.Class, jdk.dio.DeviceConfig)}
     * this device's numeric ID is undefined.
     *
     * @return the device's numeric ID (an integer number greater than or equal to {@code 0})
     *         or {@link #UNDEFINED_ID} if none is defined.
     */
    int getID();

    /**
     * Returns the name of the device. A name is a descriptive version of the device
     * ID. For example, <em>"LED1"</em>.
     *
     * @return the device's name (a free-form strings) or {@code null} if none is defined.
     */
    String getName();

    /**
     * Returns the properties of the device.
     *
     * @return a {@code String} array (a defensive copy) of the device properties or an
     *         empty {@code String} array if none are defined.
     */
    String[] getProperties();

    /**
     * Returns the interface (sub-interface of {@code Device}) of the device.
     *
     * @return the interface (sub-interface of {@code Device}) of the device.
     */
    Class<P> getInterface();

    /**
     * Returns the registered configuration of the device.
     *
     * @param <C>
     *            the device configuration type.
     * @return the registered {@code DeviceConfig} which encapsulates the hardware addressing
     *         information, and static and dynamic configuration parameters of this device
     *         device; or {@code null} if none is defined.
     */
    <C extends DeviceConfig<? super P>> C getConfiguration();
}
