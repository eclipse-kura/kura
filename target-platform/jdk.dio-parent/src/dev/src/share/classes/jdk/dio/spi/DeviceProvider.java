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

package jdk.dio.spi;

import java.io.IOException;

import jdk.dio.Device;
import jdk.dio.DeviceConfig;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.DeviceNotFoundException;
import jdk.dio.DevicePermission;
import jdk.dio.UnavailableDeviceException;
import jdk.dio.UnsupportedAccessModeException;

/**
 * The {@code DeviceProvider} interface provides methods to open {@link Device} instances of a certain type
 * and optionally with a specific configuration and a list of properties.
 * <p />
 * {@code DeviceProvider} classes are <em>Service Provider</em> classes that must conform to the
 * <em>Service-Provider Loading</em> facility requirements so that they can be located and instantiated on demand when
 * they are deployed as part of libraries.
 * <p />
 * When a device of a specific <i>type</i> (or configuration <i>type</i>) is looked up using a specific
 * set of <i>properties</i> the {@link jdk.dio.DeviceManager DeviceManager} looks up a suitable
 * {@code DeviceProvider} using the <em>Service-Provider Loading</em> facility. It iterates over all the
 * {@code DeviceProvider} classes registered as <em>Service Providers</em> until all the following steps succeed:
 * </dt>
 * <dd>
 * <ol>
 * <li>check that the {@code DeviceProvider} is of the proper <i>type</i> by first invoking its {@link #getType()
 * getType} method and/or is supporting the proper <i>configuration type</i> by invoking its {@link #getConfigType()
 * getConfigType} method</li>
 * <li>check if the {@code DeviceProvider} can open a {@link Device} instance with the specified
 * <i>properties</i> by invoking the {@link #matches(java.lang.String[]) matches}
 * method,</li>
 * <li>invoke the
 * {@link #open(DeviceConfig, java.lang.String[], int) open} method with the specified
 * <i>configuration</i>, <i>properties</i> and access mode; note that this step may fail with an exception.</li>
 * </ol>
 * </dd>
 * </dl>
 * <p />
 * Classes implementing the {@code DeviceProvider} interface MUST have a zero-argument constructor so that they
 * can be instantiated by the device manager (as per the <em>Service-Provider Loading</em> facility specification
 * requirements).
 * <p />
 * A library JAR file containing a {@code DeviceProvider} implementation MUST contain a file named:
 * <blockquote>
 * {@code META-INF/services/jdk.dio.spi.DeviceProvider}
 * </blockquote>
 * This file MUST contain the fully qualified name of the class implementing the {@code DeviceProvider} interface.
 * For example, for a JAR file containing the driver for the Real-Time Clock sample, this file
 * may contain the following single line:
 * <blockquote>
 * {@code jdk.dio.samples.rtc.RealTimeClockProvider} #Real-Time Clock sample
 * </blockquote>
 *
 * @param <P>
 *            the device type the provider is defined for.
 *
 * @see jdk.dio.DeviceManager#open(jdk.dio.DeviceConfig)
 * @see jdk.dio.DeviceManager#open(jdk.dio.DeviceConfig, int)
 * @see jdk.dio.DeviceManager#open(java.lang.Class, jdk.dio.DeviceConfig)
 * @see jdk.dio.DeviceManager#open(java.lang.Class, jdk.dio.DeviceConfig, int)
 * @see jdk.dio.DeviceManager#register DeviceManager.register
 * @since 1.0
 */
public interface DeviceProvider<P extends Device<? super P>> {

    /**
     * Opens a {@link Device} instance with the specified properties, configuration and access mode.
     * <p />
     * Property-based lookup only uses exact (case-insensitive) matching and does not perform any semantic
     * interpretation.
     * <p />
     * Prior to opening the {@code Device} instance the permission (subclass of {@link DevicePermission})
     * specific to that {@code Device} instance must be checked, if any defined. For example,
     * if this provider opens {@link jdk.dio.gpio.GPIOPin GPIOPin} instances the
     * {@link jdk.dio.gpio.GPIOPinPermission GPIOPinPermission} must be checked with a target name
     * composed of the relevant hardware addressing information (the device name or device
     * number and the pin number) and with the action {@link jdk.dio.gpio.GPIOPinPermission#OPEN GPIOPinPermission.OPEN}.
     *
     * @param config
     *            the device configuration or {@code null} if none is defined.
     * @param mode
     *            the access mode, one of: {@link jdk.dio.DeviceManager#EXCLUSIVE}
     * or {@link jdk.dio.DeviceManager#SHARED}.
     * @param properties
     *            the device properties or {@code null} if none is defined.
     * @return a {@link Device} instance with the specified configuration.
     *
     * @throws DeviceNotFoundException
     *             if the designated device is not found, such as if the hardware addressing information
     * or the properties do not match a supported device.
     * @throws UnavailableDeviceException
     *             if the designated device is not currently available - such as when it is already open in an
     *             access mode incompatible with the requested access mode.
     * @throws InvalidDeviceConfigException
     *             if the provided device configuration (as defined by the configuration parameters) is not valid/supported.
     * @throws UnsupportedAccessModeException
     *             if the requested access mode is not supported.
     * @throws IOException
     *             if any other I/O error occurred.
     * @throws SecurityException
     *             if the caller has no permission to access the designated device.
     */
    AbstractDevice<? super P> open(DeviceConfig<? super P> config, String[] properties, int mode) throws
    DeviceNotFoundException, UnavailableDeviceException, InvalidDeviceConfigException, UnsupportedAccessModeException, IOException;

    /**
     * Returns the type of the {@link DeviceConfig} this provider can handle.
     *
     * @return the type of the {@link DeviceConfig} this provider can handle; {@code null} if none is defined.
     */
     Class<? extends DeviceConfig<? super P>> getConfigType();

    /**
     * Returns the type of the {@link Device} instance this provider opens.
     *
     * @return the type of the {@link Device} instance this provider opens.
     */
    Class<P> getType();

    /**
     * Checks whether this {@code DeviceProvider} can open an instance of {@link Device} with the specified
     * properties.
     * <p />
     * The properties, if specified, are matched against the properties of the devices this
     * {@code DeviceProvider} can open instances of.
     * <p />
     * Property-based lookup only uses exact (case-insensitive) matching and does not perform any semantic
     * interpretation.
     *
     * @param properties
     *            the device properties or {@code null} to stand for any properties.
     * @return {@code true} if this {@code DeviceProvider} can open an instance of {@link Device} with the
     *         specified properties; {@code false} otherwise.
     */
    boolean matches(String[] properties);
}
