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
 * The {@code DeviceConfig} class is a tagging interface for all device configuration
 * classes.
 * <p />
 * A device configuration contains the following elements:
 * <dl>
 * <dt><b>Hardware Addressing Information</b></dt>
 * <dd>Information required to address the device. Examples are an I2C bus number and an
 * I2C slave device address or a GPIO controller number and pin index.</dd>
 * <dt><b>Static Configuration Parameters</b></dt>
 * <dd>Configuration parameters that must be set before the device is opened and which
 * may not be changed afterward. Examples are an SPI slave device clock mode or word length.</dd>
 * <dt><b>Dynamic Configuration Parameters</b></dt>
 * <dd>Configuration parameters for which a default value may be set before the device is
 * opened and which may still be changed while the device is open. Dynamic configuration
 * parameters can be changed after the device is open through methods of
 * {@link Device} sub-interfaces. Examples are a UART baud rate or the current direction of a
 * bidirectional GPIO pin.</dd>
 * </dl>
 * {@code DeviceConfig} instances should be immutable. <br />
 * A compliant implementation of this specification MUST ensure that information encapsulated in a
 * {@code DeviceConfig} instance cannot be altered while it is handling it and SHOULD either
 * create its own private copy of the instance or of the information it contains.
 * <p />
 * Some hardware addressing parameter, and static and dynamic configuration parameters may be set to
 * {@link #DEFAULT}. Whether such default settings are supported is platform- as well as device
 * driver-dependent.
 * <p />
 * An instance of {@code DeviceConfig} can be passed to the
 * {@link DeviceManager#open(DeviceConfig)} or
 * {@link DeviceManager#open(Class, DeviceConfig)} method to open the designated device
 * device with the specified configuration. A {@link InvalidDeviceConfigException} is thrown
 * when attempting to open a device with an invalid or unsupported configuration.
 *
 * @param <P>
 *            the device type the configuration is defined for.
 * @see DeviceManager#open(DeviceConfig)
 * @see DeviceManager#open(Class, DeviceConfig)
 * @see InvalidDeviceConfigException
 * @since 1.0
 */
public interface DeviceConfig<P extends Device<? super P>> {

    /**
     * Used to indicate that the default value of a configuration parameter should be used.
     */
    int DEFAULT = -1;

    /**
     * The {@code HardwareAddressing} interface defines an abstraction of an hardware addressing
     * information common on different platforms.
     *
     * @since 1.0
     */
    public interface HardwareAddressing {

        /**
         * Gets the controller number.
         *
         * @return the controller number (a positive or zero integer) or {@link #DEFAULT}.
         */
        int getControllerNumber();

        /**
         * Gets the controller name (such as a <em>device file</em> name on UNIX systems).
         *
         * @return the controller name or {@code null}.
         */
        String getControllerName();
    }
}
