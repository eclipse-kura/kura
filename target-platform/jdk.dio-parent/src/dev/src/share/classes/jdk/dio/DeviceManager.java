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

import java.io.IOException;
//import java.security.AccessController;
import java.util.Iterator;
import java.util.Random;
import java.util.ServiceLoader;

import com.oracle.dio.impl.PeripheralDescriptorImpl;
import com.oracle.dio.impl.PeripheralFactory;
import com.oracle.dio.impl.Platform;
import com.oracle.dio.registry.RegistrationEventHandler;
import com.oracle.dio.registry.RegistrationEventSender;
import com.oracle.dio.registry.Registry;

import jdk.dio.spi.DeviceProvider;

import com.oracle.dio.utils.Constants;
import com.oracle.dio.utils.PrivilegeController;
import com.oracle.dio.utils.PrivilegedAction;
import com.oracle.dio.utils.ExceptionMessage;

/**
 * The {@code DeviceManager} class provides methods for opening and registering
 * devices that can then be handled as {@link Device} instances. A {@code Device} instance
 * of a particular type can be opened using its platform-specific numeric ID or name as well as
 * its properties or using an ad-hoc configuration (in which case, its hardware addressing
 * information must be explicitly provided).
 * <p />
 * A {@code Device} instance may be identified by a numeric ID. This ID is unrelated to the
 * hardware number (hardware addressing information) that may be used to identify a device such as a
 * GPIO pin number or an I2C slave device address. A device ID typically corresponds to a
 * registered configuration for a device. The numeric ID of a device must be
 * greater than or equal to {@code 0} and must be unique. Yet the same device may be
 * directly and indirectly mapped through several IDs; each ID may correspond to a different
 * configuration, representation or abstraction for the same underlying device hardware
 * resource. <br />
 * A {@code Device} instance opened with an ad-hoc configuration - that is: not
 * through one of its registered configurations - is not assigned a numeric ID nor a name. Its
 * numeric ID and name are both undefined and set respectively to
 * {@link DeviceDescriptor#UNDEFINED_ID UNDEFINED_ID} and {@code null}.
 * <p />
 * Devices may be opened in either <a href="{@docRoot}/overview-summary.html#access-model">
 * <em>exclusive</em> or <em>shared</em> mode</a>. By default,
 * devices are opened in exclusive mode. Whether a device can be opened in
 * shared mode depends on the underlying device hardware as well as on the underlying
 * device driver. It also depends on whether the provided {@code Device} implementation is a
 * <em>dedicated</em>, <em>virtualized</em> or <em>shared</em> abstraction of the underlying
 * device resource. <br />
 * When a device is open with an ad-hoc configuration in shared mode then the
 * {@code Device} implementation (or driver) may throw a
 * {@link InvalidDeviceConfigException} if the device is already open and the
 * requested adhoc configuration is incompatible with the current configuration of the device
 * device. <br />
 * When a device is open in shared mode then some explicit means of access
 * synchronization may have to be used such as by invoking {@link Device#tryLock Device.tryLock} and
 * {@link Device#unlock Device.unlock}. Device locks are held on a per {@code Device} instance basis.
 * When the same device is open twice in shared access mode by the same application,
 * locking one of the {@code Device} instances will prevent the other from being accessed/used.
 * <p />
 * Opening a {@code Device} instance of a specific type with a registered configuration is
 * subject to permission checks (see {@link DeviceMgmtPermission#OPEN}). <br />
 * Opening a {@code Device} instance of a specific type with an ad-hoc configuration is subject
 * to permission checks specific for that type (for example see
 * {@link jdk.dio.gpio.GPIOPinPermission GPIOPinPermission.OPEN}). This permission check
 * should be implemented by the {@link jdk.dio.spi.DeviceProvider#open
 * DeviceProvider.open} method. <br />
 * Registration and unregistration of devices are subject to permission checks (see
 * {@link DeviceMgmtPermission#REGISTER} and {@link DeviceMgmtPermission#UNREGISTER}).
 * <br />
 * For more details see <a href="{@docRoot}/overview-summary.html#security-model">Security Model</a>.
 *
 * @see UnavailableDeviceException
 * @see DeviceMgmtPermission
 * @see Device
 * @see jdk.dio.spi.DeviceProvider
 * @see DeviceConfig
 * @since 1.0
 */
public class DeviceManager {
    /**
     * Exclusive access mode.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other access mode bit flags.
     */
    public static final int EXCLUSIVE = 1;
    /**
     * Shared access mode.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other access mode bit flags.
     */
    public static final int SHARED = 2;

    /**
     * Unspecified device numeric ID - requesting the allocation of a free ID.
     *
     * @see #register register
     */
    public static final int UNSPECIFIED_ID = -1;

    /**
     * Platform specific initialization.
     */
    static {
        Platform.initialize();
    }

    /**
     * List all platform- and user-registered devices.
     *
     * @param <P>
     *            the type of devices listed.
     * @return an enumeration of the descriptors of all registered devices.
     */
    public static <P extends Device<? super P>> Iterator<DeviceDescriptor<P>> list() {
        return Registry.getInstance().list(null);
    }

    /**
     * List all platform- and user-registered devices of the designated type.
     *
     * @param <P>
     *            the type of devices to list.
     * @param intf
     *            the interface (sub-interface of {@code Device}) of the device to be
     *            registered.
     * @return an enumeration of the descriptors of all registered devices of the designated
     *         type.
     * @throws NullPointerException
     *             if {@code intf} is {@code null}.
     */
    public static <P extends Device<? super P>> Iterator<DeviceDescriptor<P>> list(Class<P> intf) {
        // checks for null
        intf.isArray();
        return Registry.getInstance().list(intf);
    }

    /**
     * Opens a {@code Device} instance of the specified type with the specified hardware
     * addressing information and configuration. The specified device type and specified
     * configuration type must be compatible. An instance of the first <em>available</em>
     * matching {@code Device} is returned. If a matching {@code Device} is already open
     * (therefore <em>not available</em>) the next matching {@code Device} is considered.
     * <p />
     * The device is opened in exclusive access mode.
     * <p />
     * The returned {@code Device} instance's ID and name are undefined.
     * <p />
     * A new instance is returned upon each call.
     *
     * @param <P>
     *            the type of the device to open.
     * @param intf
     *            the interface (sub-interface of {@code Device}) of the device being looked
     *            up.
     * @param config
     *            the device configuration (which includes hardware addressing information as
     *            well as configuration parameters).
     * @return a {@code Device} instance for the specified configuration.
     * @throws UnsupportedDeviceTypeException
     *             if the designated device type is not supported.
     * @throws InvalidDeviceConfigException
     *             if the provided device configuration (as defined by the configuration
     *             parameters) is not valid/supported.
     * @throws DeviceNotFoundException
     *             if the device designated by the hardware addressing information is not found.
     * @throws UnavailableDeviceException
     *             if the designated device is not currently available - such as if it is
     *             already open with exclusive access.
     * @throws IOException
     *             if any other I/O error occurred.
     * @throws SecurityException
     *             if the caller has no permission to access the designated device (see
     *             {@link DevicePermission#OPEN}).
     * @throws NullPointerException
     *             if {@code intf} or {@code config} is {@code null}.
     * @throws ClassCastException
     *             if the device configuration type specified by {@code config}
     *             is not applicable to the device type specified by {@code intf}.
     */
    public static <P extends Device<? super P>> P open(Class<P> intf, DeviceConfig<? super P> config)
            throws IOException, InvalidDeviceConfigException, UnsupportedDeviceTypeException,
            DeviceNotFoundException, UnavailableDeviceException {
        return open(intf, config, EXCLUSIVE);
    }

    /**
     * Opens a {@code Device} instance of the specified type with the specified hardware
     * addressing information and configuration. The specified device type and specified
     * configuration type must be compatible. An instance of the first <em>available</em> matching {@code Device}
     * is returned. If a matching {@code Device} is already open in a mode that is not
     * compatible with the requested mode the next matching {@code Device} is considered.
     * <p />
     * The device is opened in the designated access mode. A device may be
     * opened in shared mode if supported by the underlying driver and hardware and if it is not
     * already opened in exclusive mode. A device may be opened in exclusive mode if
     * supported by the underlying driver and hardware and if it is not already opened.
     * <p />
     * The returned {@code Device} instance's ID and name are undefined.
     * <p />
     * A new instance is returned upon each call.
     *
     * @param <P>
     *            the type of the device to open.
     * @param intf
     *            the interface (sub-interface of {@code Device}) of the device being looked
     *            up.
     * @param config
     *            the device configuration (which includes hardware addressing information as
     *            well as configuration parameters).
     * @param mode
     *            the access mode, one of: {@link #EXCLUSIVE} or {@link #SHARED}.
     * @return a {@code Device} instance for the specified configuration.
     * @throws UnsupportedDeviceTypeException
     *             if the designated device type is not supported.
     * @throws InvalidDeviceConfigException
     *             if the provided device configuration (as defined by the configuration
     *             parameters) is not valid/supported or when opened in shared mode, if the provided
     *             device configuration is incompatible with the currently open configuration of
     *             the device.
     * @throws DeviceNotFoundException
     *             if the device designated by the hardware addressing information is not found.
     * @throws UnavailableDeviceException
     *             if the designated device is not currently available - such as if it is
     *             already open with exclusive access.
     * @throws UnsupportedAccessModeException
     *             if the requested access mode is not supported.
     * @throws IOException
     *             if any other I/O error occurred.
     * @throws SecurityException
     *             if the caller has no permission to access the designated device (see
     *             {@link DevicePermission#OPEN}).
     * @throws NullPointerException
     *             if {@code intf} or {@code config} is {@code null}.
     * @throws ClassCastException
     *             if the device configuration type specified by {@code config}
     *             is not applicable to the device type specified by {@code intf}.
     */
    public static <P extends Device<? super P>> P open(Class<P> intf, DeviceConfig<? super P> config, int mode)
    throws IOException, InvalidDeviceConfigException, UnsupportedDeviceTypeException,
            DeviceNotFoundException, UnavailableDeviceException, UnsupportedAccessModeException {

        if (null == intf) {
            throw new NullPointerException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_NULL_CONFIG_OR_INTF)
            );
        }
        return openWithConfig(intf, config, mode);
    }

    private static <P extends Device<? super P>> P openWithConfig(Class<P> intf, DeviceConfig<? super P> config, int mode)
        throws IOException, InvalidDeviceConfigException, UnsupportedDeviceTypeException,
            DeviceNotFoundException, UnavailableDeviceException, UnsupportedAccessModeException {

        if (null == config) {
            throw new NullPointerException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_NULL_CONFIG_OR_INTF)
            );
        }
        checkMode(mode);

        if (null != intf) {
            PeripheralDescriptorImpl<DeviceConfig<P>, P> descr = new PeripheralDescriptorImpl(UNSPECIFIED_ID, null, config, intf, null);
            try {
                return ((PeripheralFactory<P>)getFactory(intf)).create(descr, mode);
            } catch (DeviceNotFoundException | UnsupportedDeviceTypeException e) {
                P res = (P)loadFromDriver(config, mode);
                if (null == res) {
                    throw e;
                }
                return res;
            }
        } else {
            // special case: getDefaultType returns null that means config is not for embedded drivers
            // try to load from installed drivers
            P res = (P)loadFromDriver(config, mode);
            if (null == res) {
                throw new UnsupportedDeviceTypeException(config.toString());
            }
            return res;
        }
    }

    /**
     * Looks up and opens a {@code Device} instance for the provided numeric ID.
     * <p />
     * The device is opened in exclusive access mode.
     * <p />
     * A new instance is returned upon each call.
     *
     * @param <P>
     *            the type of the device to open.
     * @param id
     *            the numeric device id.
     * @return a {@code Device} instance for the given numeric ID.
     * @throws DeviceNotFoundException
     *             if the designated device is not found.
     * @throws UnavailableDeviceException
     *             if the designated device is not currently available - such as if it is
     *             already open with exclusive access.
     * @throws IOException
     *             if any other I/O error occurred.
     * @throws SecurityException
     *             if the caller has no permission to access the designated device (see
     *             {@link DeviceMgmtPermission#OPEN}).
     * @throws IllegalArgumentException
     *             if {@code id} is less than {@code 0}.
     */
    public static <P extends Device<? super P>> P open(int id) throws IOException, DeviceNotFoundException,
        UnavailableDeviceException {
        try {
            return (P)open(id, Device.class);
        } catch (UnsupportedDeviceTypeException e) {
            throw new DeviceNotFoundException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_CONFIG_PROBLEM, e.getMessage())
            );
        }
    }

    /**
     * Looks up and opens a {@code Device} instance for the provided numeric ID and type.
     * <p />
     * The device is opened in exclusive access mode.
     * <p />
     * A new instance is returned upon each call.
     *
     * @param <P>
     *            the type of the device to open.
     * @param id
     *            the numeric device id.
     * @param intf
     *            the interface (sub-interface of {@code Device}) of the device being looked
     *            up.
     * @return a {@code Device} instance for the given numeric ID.
     * @throws DeviceNotFoundException
     *             if the designated device is not found.
     * @throws UnsupportedDeviceTypeException
     *             if the designated device type is not supported.
     * @throws UnavailableDeviceException
     *             if the designated device is not currently available - such as if it is
     *             already open with exclusive access.
     * @throws IOException
     *             if any other I/O error occurred.
     * @throws SecurityException
     *             if the caller has no permission to access the designated device (see
     *             {@link DeviceMgmtPermission#OPEN}).
     * @throws IllegalArgumentException
     *             if {@code id} is less than {@code 0}.
     * @throws NullPointerException
     *             if {@code intf} is {@code null}.
     */
    public static <P extends Device<? super P>> P open(int id, Class<P> intf) throws IOException,
        UnsupportedDeviceTypeException, DeviceNotFoundException, UnavailableDeviceException {
        try {
            return open(id, intf, EXCLUSIVE);
        } catch (UnsupportedAccessModeException e) {
            throw new DeviceNotFoundException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_EXCLUSIVE_MODE_UNSUPPORTED)
            );
        }
    }

    /**
     * Looks up and opens a {@code Device} instance for the provided numeric ID and type.
     * <p />
     * The device is opened in the designated access mode. A device may be
     * opened in shared mode if supported by the underlying driver and hardware and if it is not
     * already opened in exclusive mode. A device may be opened in exclusive mode if
     * supported by the underlying driver and hardware and if it is not already opened.
     * <p />
     * A new instance is returned upon each call.
     *
     * @param <P>
     *            the type of the device to open.
     * @param id
     *            the numeric device id.
     * @param intf
     *            the interface (sub-interface of {@code Device}) of the device being looked
     *            up.
     * @param mode
     *            the access mode, one of: {@link #EXCLUSIVE} or {@link #SHARED}.
     * @return a {@code Device} instance for the given numeric ID.
     * @throws UnsupportedDeviceTypeException
     *             if the designated device type is not supported.
     * @throws DeviceNotFoundException
     *             if the designated device is not found.
     * @throws UnsupportedDeviceTypeException
     *             if the designated device type is not supported.
     * @throws UnavailableDeviceException
     *             if the designated device is not currently available - such as when it is
     *             already open in an access mode incompatible with the requested access mode.
     * @throws UnsupportedAccessModeException
     *             if the requested access mode is not supported.
     * @throws IOException
     *             if any other I/O error occurred.
     * @throws SecurityException
     *             if the caller has no permission to access the designated device (see
     *             {@link DeviceMgmtPermission#OPEN}).
     * @throws IllegalArgumentException
     *             if {@code id} is less than {@code 0}.
     * @throws NullPointerException
     *             if {@code intf} is {@code null}.
     */
    public static <P extends Device<? super P>> P open(int id, Class<P> intf, int mode) throws IOException,
        UnsupportedDeviceTypeException, DeviceNotFoundException, UnavailableDeviceException,
        UnsupportedAccessModeException {
        checkID(id);
        checkMode(mode);
        if (null == intf) {
            throw new NullPointerException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_NULL_INTF)
            );
        }
        DeviceDescriptor<P> descr = Registry.getInstance().get(id);
        if (null == descr) {
            throw new DeviceNotFoundException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_NOT_FOUND, String.valueOf(id))
            );
        }
        if (!intf.isAssignableFrom(descr.getInterface())) {
            throw new DeviceNotFoundException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_HAS_DIFFERENT_TYPE, id, descr.getInterface())
            );
        }

        do {
        	//String name = (null == descr.getName()) ? ":" + id : descr.getName() + ":" + id;
        	//AccessController.getContext().checkPermission(new DeviceMgmtPermission(name, DeviceMgmtPermission.OPEN));
        } while (false);

        try {
            final PeripheralFactory<P> f = getFactory(descr.getInterface());
            final DeviceDescriptor<P> fdescr = descr;
            final int fmode = mode;
            return PrivilegeController.doPrivileged(new PrivilegedAction<P>() {
                    public P run() throws IOException {
                        return f.create(fdescr, fmode);
                    }
                });
        } catch (InvalidDeviceConfigException e) {
            throw new DeviceNotFoundException(e.getMessage());
        } catch (DeviceNotFoundException | UnsupportedDeviceTypeException e) {
            DeviceConfig<? super P> config = descr.getConfiguration();
            P res = (P)loadFromDriver(config, mode);
            if (null == res) {
                throw e;
            }
            return res;
        }
    }

    /**
     * Looks up and opens a {@code Device} instance for the provided numeric ID.
     * <p />
     * The device is opened in the designated access mode. A device may be
     * opened in shared mode if supported by the underlying driver and hardware and if it is not
     * already opened in exclusive mode. A device may be opened in exclusive mode if
     * supported by the underlying driver and hardware and if it is not already opened.
     * <p />
     * A new instance is returned upon each call.
     *
     * @param <P>
     *            the type of the device to open.
     * @param id
     *            the numeric device id.
     * @param mode
     *            the access mode, one of: {@link #EXCLUSIVE} or {@link #SHARED}.
     * @return a {@code Device} instance for the given numeric ID.
     * @throws DeviceNotFoundException
     *             if the designated device is not found.
     * @throws UnavailableDeviceException
     *             if the designated device is not currently available - such as when it is
     *             already open in an access mode incompatible with the requested access mode.
     * @throws UnsupportedAccessModeException
     *             if the requested access mode is not supported.
     * @throws IOException
     *             if any other I/O error occurred.
     * @throws SecurityException
     *             if the caller has no permission to access the designated device (see
     *             {@link DeviceMgmtPermission#OPEN}).
     * @throws IllegalArgumentException
     *             if {@code id} is less than {@code 0}.
     */
    public static <P extends Device<? super P>> P open(int id, int mode) throws IOException, DeviceNotFoundException,
        UnavailableDeviceException, UnsupportedAccessModeException {
        try {
            return (P)open(id, Device.class, mode);
        } catch (UnsupportedDeviceTypeException e) {
            throw new DeviceNotFoundException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_CONFIG_PROBLEM, e.getMessage())
            );
        }
    }

    /**
     * Opens a {@code Device} instance with the specified hardware addressing information and
     * configuration. The type of the device is inferred from the configuration type. An
     * instance of the first <em>available</em> matching {@code Device}. If a matching
     * {@code Device} is already open (therefore <em>not available</em>) the next matching
     * {@code Device} is considered.
     * <p />
     * The device is opened in exclusive access mode.
     * <p />
     * The returned {@code Device} instance's ID and name are undefined.
     * <p />
     * A new instance is returned upon each call.
     *
     * @param <P>
     *            the type of the device to open.
     * @param config
     *            the device configuration (which includes hardware addressing information as
     *            well as configuration parameters).
     * @return a {@code Device} instance for the specified configuration.
     * @throws UnsupportedDeviceTypeException
     *             if the designated device type is not supported.
     * @throws InvalidDeviceConfigException
     *             if the provided device configuration (as defined by the configuration
     *             parameters) is not valid/supported.
     * @throws DeviceNotFoundException
     *             if the device designated by the hardware addressing information is not found.
     * @throws UnavailableDeviceException
     *             if the designated device is not currently available - such as if it is
     *             already open with exclusive access.
     * @throws IOException
     *             if any other I/O error occurred.
     * @throws SecurityException
     *             if the caller has no permission to access the designated device (see
     *             {@link DevicePermission#OPEN}).
     * @throws NullPointerException
     *             if {@code config} is {@code null}.
     */
    public static <P extends Device<? super P>> P open(DeviceConfig<? super P> config) throws IOException,
        InvalidDeviceConfigException, UnsupportedDeviceTypeException, DeviceNotFoundException,
        UnavailableDeviceException {
        return open(config, EXCLUSIVE);
    }

    /**
     * Opens a {@code Device} instance with the specified hardware addressing information and
     * configuration. The type of the device is inferred from the configuration type. An
     * instance of the first <em>available</em> matching {@code Device} is returned. If a
     * matching {@code Device} is already open in a mode that is not compatible with the
     * requested mode the next matching {@code Device} is considered.
     * <p />
     * The device is opened in the designated access mode. A device may be
     * opened in shared mode if supported by the underlying driver and hardware and if it is not
     * already opened in exclusive mode. A device may be opened in exclusive mode if
     * supported by the underlying driver and hardware and if it is not already opened.
     * <p />
     * The returned {@code Device} instance's ID and name are undefined.
     * <p />
     * A new instance is returned upon each call.
     *
     * @param config
     *            the device configuration (which includes hardware addressing information as
     *            well as configuration parameters).
     * @param mode
     *            the access mode, one of: {@link #EXCLUSIVE} or {@link #SHARED}.
     * @return a {@code Device} instance for the specified configuration.
     * @throws UnsupportedDeviceTypeException
     *             if the designated device type is not supported.
     * @throws InvalidDeviceConfigException
     *             if the provided device configuration (as defined by the configuration
     *             parameters) is not valid/supported or when opened in shared mode, if the provided
     *             device configuration is incompatible with the currently open configuration of
     *             the device.
     * @throws DeviceNotFoundException
     *             if the device designated by the hardware addressing information is not found.
     * @throws UnavailableDeviceException
     *             if the designated device is not currently available - such as if it is
     *             already open with exclusive access.
     * @throws UnsupportedAccessModeException
     *             if the requested access mode is not supported.
     * @throws IOException
     *             if any other I/O error occurred.
     * @throws SecurityException
     *             if the caller has no permission to access the designated device (see
     *             {@link DevicePermission#OPEN}).
     * @throws NullPointerException
     *             if {@code config} is {@code null}.
     */
    public static <P extends Device<? super P>> P open(DeviceConfig<? super P> config, int mode) throws IOException,
            InvalidDeviceConfigException, UnsupportedDeviceTypeException, DeviceNotFoundException,
            UnavailableDeviceException, UnsupportedAccessModeException {
        return openWithConfig(getDefaultType(config),config,mode);
    }

    /**
     * Looks up and opens a {@code Device} instance for the specified name, type and/or
     * properties. An instance of the first <em>available</em> matching {@code Device} is
     * returned. If a matching {@code Device} is already open in a mode that is not compatible
     * with the requested mode the next matching {@code Device} is considered.
     * <p />
     * The device is opened in the designated access mode. A device may be
     * opened in shared mode if supported by the underlying driver and hardware and if it is not
     * already opened in exclusive mode. A device may be opened in exclusive mode if
     * supported by the underlying driver and hardware and if it is not already opened.
     * <p />
     * A new instance is returned upon each call.
     * <p />
     * Property-based lookup only uses exact (case-insensitive) matching and does not perform any
     * semantic interpretation.
     *
     * @param <P>
     *            the type of the device to open.
     * @param name
     *            the device name; may be {@code null}.
     * @param intf
     *            the interface (sub-interface of {@code Device}) of the device being looked
     *            up.
     * @param mode
     *            the access mode, one of: {@link #EXCLUSIVE} or {@link #SHARED}.
     * @param properties
     *            the list of required properties; may be {@code null}.
     * @return a {@code Device} instance for the given name and required properties.
     * @throws UnsupportedDeviceTypeException
     *             if the designated device type is not supported.
     * @throws DeviceNotFoundException
     *             if the designated device is not found.
     * @throws UnavailableDeviceException
     *             if the designated device is not currently available - such as when it is
     *             already open in an access mode incompatible with the requested access mode.
     * @throws UnsupportedAccessModeException
     *             if the requested access mode is not supported.
     * @throws IOException
     *             if any other I/O error occurred.
     * @throws SecurityException
     *             if the caller has no permission to access the designated device (see
     *             {@link DeviceMgmtPermission#OPEN}).
     * @throws IllegalArgumentException
     *             if both {@code name} is {@code null} and {@code properties} is empty.
     * @throws NullPointerException
     *             if {@code intf} is {@code null}.
     */
    public static <P extends Device<? super P>> P open(String name, Class<P> intf, int mode, String... properties)
        throws IOException, UnsupportedDeviceTypeException, DeviceNotFoundException,
        UnavailableDeviceException, UnsupportedAccessModeException {
        if (null == name && (null == properties || 0 == properties.length)) {
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_NULL_NAME_AND_PROPERTIES)
            );
        }
        if (null == intf) {
            throw new NullPointerException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_NULL_INTF)
            );
        }
        checkMode(mode);
        Iterator<DeviceDescriptor<P>> iter = Registry.getInstance().get(name, intf, properties);
        // indicates if given device type is supprted
        boolean supported = false;
        while (iter.hasNext()) {
            DeviceDescriptor<P> descr = iter.next();
            try {
                do {
                	//String perm =  (null == descr.getName()) ? "" : descr.getName();
                	//perm = (UNSPECIFIED_ID == descr.getID()) ? perm : perm + ":" + descr.getID();
                	//AccessController.getContext().checkPermission(new DeviceMgmtPermission(name, DeviceMgmtPermission.OPEN));
                } while (false);

                final PeripheralFactory<P> f =  getFactory(descr.getInterface());

                // we found at least one driver
                supported = true;

                final DeviceDescriptor<P> fdescr = descr;
                final int fmode = mode;
                return PrivilegeController.doPrivileged(new PrivilegedAction<P>() {
                        public P run() throws IOException {
                            return f.create(fdescr, fmode);
                        }
                    });
            } catch (InvalidDeviceConfigException e) {
                throw new DeviceNotFoundException(e.getMessage());
            } catch (UnavailableDeviceException e2) {
                if (iter.hasNext()) {
                    // find next configuration
                    continue;
                }
                throw e2;
            } catch (DeviceNotFoundException | UnsupportedDeviceTypeException e) {
                DeviceConfig<? super P> config = descr.getConfiguration();
                P res = (P)loadFromDriver(config, mode);
                if (null == res) {
                    throw e;
                }
            }
        }

        if (!supported) {
            // if there is no configuration for given type/name
            // try to guess if this device type is valid
            try {
                getFactory(intf);
                supported = true;
            } catch (UnsupportedDeviceTypeException e) {
                // nothing found yet
            }
        }

        // special case if config is null
        if (null != properties) {
            return loadFromDriver(supported, intf, mode, properties);
        }

        if (supported) {
            // type is valid, but no valid config is found
            throw new DeviceNotFoundException(name);
        } else {
            throw new UnsupportedDeviceTypeException(intf.toString());
        }
    }

    /**
     * Looks up and opens a {@code Device} instance for the specified name, type and/or
     * properties. An instance of the first <em>available</em> matching {@code Device} is
     * returned. If a matching {@code Device} is already open (therefore <em>not available</em>)
     * the next matching {@code Device} is considered.
     * <p />
     * The device is opened in exclusive access mode.
     * <p />
     * A new instance is returned upon each call.
     * <p />
     * Property-based lookup only uses exact (case-insensitive) matching and does not perform any
     * semantic interpretation.
     *
     * @param <P>
     *            the type of the device to open.
     * @param name
     *            the device name; may be {@code null}.
     * @param intf
     *            the interface (sub-interface of {@code Device}) of the device being looked
     *            up.
     * @param properties
     *            the list of required properties; may be {@code null}.
     * @return a {@code Device} instance for the given name and required properties.
     * @throws UnsupportedDeviceTypeException
     *             if the designated device type is not supported.
     * @throws DeviceNotFoundException
     *             if the designated device is not found.
     * @throws UnavailableDeviceException
     *             if the designated device is not currently available - such as if it is
     *             already open with exclusive access.
     * @throws IOException
     *             if any other I/O error occurred.
     * @throws SecurityException
     *             if the caller has no permission to access the designated device (see
     *             {@link DeviceMgmtPermission#OPEN}).
     * @throws IllegalArgumentException
     *             if both {@code name} is {@code null} and {@code properties} is empty.
     * @throws NullPointerException
     *             if {@code intf} is {@code null}.
     */
    public static <P extends Device<? super P>> P open(String name, Class<P> intf, String... properties) throws IOException,
        UnsupportedDeviceTypeException, DeviceNotFoundException, UnavailableDeviceException {
        try {
            return open(name, intf, EXCLUSIVE, properties);
        } catch (UnsupportedAccessModeException e) {
            throw new DeviceNotFoundException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_EXCLUSIVE_MODE_UNSUPPORTED)
            );
        }
    }

    /**
     * Registers under the specified ID (and optionally name and properties) a new device
     * supporting the provided configuration. Upon successful registration all
     * {@link RegistrationListener} instances registered for the type of the registered device
     * are notified.
     * <p />
     * An implementation of the {@code DeviceManager} must guarantee that an application
     * registering a device is the first one to get notified (in the event it has registered a
     * {@code RegistrationListener} for that type of devices).
     * <p />
     * The designated device may be probed to check if the provided configuration is valid.
     * <p />
     * Prior to registering a new device of a certain type the
     * {@link DeviceMgmtPermission} is checked with the action
     * {@link DeviceMgmtPermission#REGISTER DeviceMgmtPermission.REGISTER}. <br />
     * For example, if a device of type {@link jdk.dio.gpio.GPIOPin
     * GPIOPin} is to be registered the {@code DeviceMgmtPermission} is checked with a target
     * name composed of the requested device name and ID and with the action
     * {@code DeviceMgmtPermission#REGISTER DeviceMgmtPermission.REGISTER}.
     * <p />
     * The following is an example of how this method may be used to register a new UART with Modem
     * control lines: <blockquote>
     *
     * <pre>
     * DeviceManager.register(10, // the device ID
     *         ModemUART.class, // the device type/interface
     *         new UARTConfig(0, 2400, UARTConfig.DATABITS_8, UARTConfig.PARITY_EVEN, UARTConfig.STOPBITS_1,
     *                 UARTConfig.FLOWCONTROL_NONE), // the device configuration
     *         &quot;MODEM&quot;, // the device name
     *         &quot;com.foobar.modem.xxx=true&quot;, &quot;com.foobar.modem.yyy=true&quot; // the device capabilities
     * );
     * </pre>
     * </blockquote>
     * This method cannot be used to register a device of a
     * custom type. If an attempt is made to register a custom implementation of
     * {@link DeviceConfig} a {@code UnsupportedDeviceTypeException} will be thrown.
     *
     * @param <P>
     *            the type of the device to be registered.
     * @param id
     *            the device ID; if {@code id} is equal to {@link #UNSPECIFIED_ID} a free ID
     *            will be allocated.
     * @param intf
     *            the interface (sub-interface of {@code Device}) of the device to be
     *            registered.
     * @param config
     *            the device configuration.
     * @param name
     *            the name of the device to be registered; may be {@code null}.
     * @param properties
     *            the list of properties/capabilities of the device to be registered; may be
     *            {@code null}.
     * @return the assigned device ID.
     * @throws InvalidDeviceConfigException
     *             if the provided device configuration (as defined by the configuration
     *             parameters) is not valid/supported.
     * @throws UnsupportedDeviceTypeException
     *             if the designated device type is not supported.
     * @throws DeviceNotFoundException
     *             if the device designated by the hardware addressing information is not found.
     * @throws DeviceAlreadyExistsException
     *             if {@code id} is already assigned to a device.
     * @throws IOException
     *             if any other I/O error occurred.
     * @throws NullPointerException
     *             if {@code intf} or {@code config} is {@code null}.
     * @throws IllegalArgumentException
     *             if {@code id} is less than {@code 0} and is not equal to {@link #UNSPECIFIED_ID}.
     * @throws UnsupportedOperationException
     *             if registering a new device is not supported - such as may be the case
     *             on a platform supporting only a <a href="{@docRoot}/overview-summary.html#closed-config">closed device topology</a>.
     * @throws SecurityException
     *             if the caller does not have the required permission (see
     *             {@link DeviceMgmtPermission#REGISTER}).
     * @throws ClassCastException
     *             if the device configuration type specified by {@code config}
     *             is not applicable to the device type specified by {@code intf}.
     */
    public static <P extends Device<? super P>> int register(int id, Class<P> intf, DeviceConfig<? super P> config,
                                                                 String name, String... properties) throws IOException, UnsupportedDeviceTypeException, InvalidDeviceConfigException,
        DeviceNotFoundException, DeviceAlreadyExistsException {

        if (id < UNSPECIFIED_ID) {
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_INVALID_ID)
            );
        }

        do {
        	//String perm = (UNSPECIFIED_ID == id) ? "" : ":"+id;
        	//perm = (null == name) ? perm : name + perm;
        	//AccessController.getContext().checkPermission(new DeviceMgmtPermission(perm, DeviceMgmtPermission.REGISTER));
        } while (false);


        return registerUnchecked(id, intf, config, name, properties);
    }

    private static <P extends Device<? super P>> int registerUnchecked(final int id, final Class<P> intf, final DeviceConfig<? super P> config,
                                                                      final String name, final String... properties)
    throws IOException, UnsupportedDeviceTypeException, InvalidDeviceConfigException, DeviceNotFoundException, DeviceAlreadyExistsException {

        return PrivilegeController.doPrivileged(new PrivilegedAction<Integer>() {
                public Integer run() throws IOException {
                    Random rnd = new Random();
                    int new_id = id;

                    do {
                        if (id == -1) {
                            //  verify generated ID
                            new_id = rnd.nextInt();
                            if (new_id < 1) {
                                continue;
                            }
                        }
                        try {
                            Device p = open(new_id);
                            p.close();
                        } catch (DeviceNotFoundException pnfe1) {
                            // this is the only right way to break "while" condition
                            break;
                        } catch (UnavailableDeviceException pnae1) {}
                        if (id  != UNSPECIFIED_ID) {
                            throw new DeviceAlreadyExistsException(
                                ExceptionMessage.format(ExceptionMessage.DEVICE_NONUNIQUE_ID)
                            );
                        }
                        continue;
                    } while (true);

                    while (null != name || null != properties) {
                        try {
                            Device p = open(name, intf, properties);
                            p.close();
                        } catch (DeviceNotFoundException pnfe2) {
                            // this is the only right way to continue
                            break;
                        } catch (UnavailableDeviceException pnae2) {}
                        throw new DeviceAlreadyExistsException(
                            ExceptionMessage.format(ExceptionMessage.DEVICE_ALREADY_EXISTING_CONFIG)
                        );
                    }

                    try {
                        Device p = open(intf, config);
                        p.close();
                    } catch (UnavailableDeviceException pnae3) {}

                    Registry.getInstance().register(new_id, intf, config, name, properties);
                    RegistrationEventSender.notifyRegistered(null, new PeripheralDescriptorImpl(new_id, name, config, intf, properties));

                    return new_id;
                }

        }).intValue();
    }

    /**
     * Unregisters the device associated with the specified ID. Upon successful
     * unregistration all {@link RegistrationListener} instances registered for the type of the
     * device that has been unregistered are notified.
     * <p />
     * Some devices are registered by the underlying platform and cannot be unregistered.
     * <p />
     * Unregistration of a device has no side effect on its currently open
     * {@code Device} instances. These {@code Device} instances especially retain the
     * device ID that was assigned to them at the time they were open.
     * <p />
     * This method returns silently if the provided ID does not correspond to a registered
     * device.
     *
     * @param id
     *            the ID of the device to unregister.
     * @throws IllegalArgumentException
     *             if {@code id} is less than {@code 0} or if {@code id} corresponds to a device
     *             device registered by the platform.
     * @throws SecurityException
     *             if the caller does not have the required permission (see
     *             {@link DeviceMgmtPermission#UNREGISTER}).
     */
    public static void unregister(int id) {
        checkID(id);

        final Registry r = Registry.getInstance();
        final DeviceDescriptor d = r.get(id);
        //no record for id found
        if (d == null) return;

        do {
        	//String perm = (UNSPECIFIED_ID == d.getID()) ? "" : ":"+d.getID();
        	//perm = (null == d.getName()) ? perm : d.getName() + perm;
        	//AccessController.getContext().checkPermission(new DeviceMgmtPermission(perm, DeviceMgmtPermission.UNREGISTER));
        } while (false);

        // if OK then delete
        try {
            final int _id = id;
            PrivilegeController.doPrivileged(new PrivilegedAction() {
                    public Object run() throws IOException {
                        r.unregister(_id);
                        RegistrationEventSender.notifyUnregistered(null, d);
                        return null;
                    }
                });
        } catch (IOException e) {
            // all errors are handled at corresponding functions
        }
    }

    /**
     * Adds the specified registration listener to receive notification of registration and
     * unregistration of devices of the specified type.
     *
     * @param <P>
     *            the type of the device to be listened for.
     * @param listener
     *            the registration listener.
     * @param intf
     *            the interface (sub-interface of {@code Device}) of the devices to be
     *            listened for.
     * @throws NullPointerException
     *             if {@code listener} or {@code intf} is {@code null}.
     */
    public static <P extends Device<? super P>> void addRegistrationListener(RegistrationListener<P> listener, Class<P> intf) {
        // checks for null
        listener.getClass();
        intf.isArray();

        RegistrationEventHandler.addListener(listener, intf);
    }

    /**
     * Removes the specified registration listener so that it no longer receives notification of
     * registration and unregistration of devices of the specified type.
     *
     * @param <P>
     *            the type of the device listened for.
     * @param listener
     *            the registration listener.
     * @param intf
     *            the interface (sub-interface of {@code Device}) of the devices listened
     *            for.
     * @throws NullPointerException
     *             if {@code listener} or {@code intf} is {@code null}.
     */
    public static <P extends Device<? super P>> void removeRegistrationListener(RegistrationListener<P> listener, Class<P> intf) {
        // checks for null
        listener.getClass();
        intf.isArray();

        RegistrationEventHandler.removeListener(listener, intf);
    }

    /* ------------------- Private API ---------------- */

    private static void checkID(int ID) throws IllegalArgumentException {
        if (ID < 0) {
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_NEGATIVE_ID)
            );
        }
    }

    private static void checkMode(int mode) throws UnsupportedAccessModeException {
        if (SHARED != mode && EXCLUSIVE != mode) {
            throw new UnsupportedAccessModeException();
        }
    }

    private static <P extends Device<? super P>> Class<P> getDefaultType(DeviceConfig<? super P> config)  throws UnsupportedDeviceTypeException {

        String fullName = config.getClass().getName();

        if (-1 != fullName.indexOf(Constants.PREFIX)) {

            try {
                // extract peripheral name from config name
                // i.e. ADCChannel from com.oracle.dio.ADCChannelConfig
                int configPos = fullName.indexOf(Constants.CONFIG);
                return (Class<P>)Class.forName(fullName.substring(0, configPos));
            } catch (RuntimeException | ClassNotFoundException e) {
            }

        }
        //  this will cause NPE at open(Class, DeviceConfig, int)
        // fix later
        return null;
    }

    private static PeripheralFactory getFactory(Class clazz) throws UnsupportedDeviceTypeException {
        // get package name of com.oracle.dio.PACAKAGE_NAME.PERIPHERAL_IFACE
        // following code is correct for precompiled peripheral driver that following DAAPI name convention.
        String fullName = clazz.getName();

        // check for name correctness from current spec point of view.
        // it is enough to check only name of class itself because of all DAAPI config class are final.
        // it is assumed that neither driver nor application can create a code for com.oracle.* domain.
        // it is not correct for JavaSE but running code has no super user rights.
        if (-1 != fullName.indexOf(Constants.PREFIX)) {

            int pIndex = fullName.indexOf('.', Constants.PREFIX.length());

            try {
                String pack =  fullName.substring(Constants.PREFIX.length(), pIndex);
                String device = fullName.substring(pIndex + 1);
                return (PeripheralFactory)Class.forName(Constants.FACTORY_PREFIX + pack + Constants.IMPL + device + Constants.FACTORY).newInstance();
            } catch (RuntimeException | ClassNotFoundException |
                     InstantiationException  | IllegalAccessException e) {
            }
        }
        throw new UnsupportedDeviceTypeException(
            ExceptionMessage.format(ExceptionMessage.DEVICE_INVALID_CLASSNAME, fullName)
        );
    }

    // is called in response to UDTE and DNFE
    private static <P extends Device<? super P>> P loadFromDriver(DeviceConfig<? super P> config, int mode) throws
        DeviceNotFoundException, UnavailableDeviceException, InvalidDeviceConfigException,
        UnsupportedAccessModeException, IOException {
        ServiceLoader<DeviceProvider> loader = ServiceLoader.load(DeviceProvider.class);
        Iterator<DeviceProvider>  iter = loader.iterator();

        if (!iter.hasNext()) {
            return null;
        }

        int rejected, total;
        rejected = total = 0;
        while (iter.hasNext()) {
            total++;
            DeviceProvider provider = iter.next();
            if (!provider.getConfigType().isAssignableFrom(config.getClass())) {
                rejected++;
            }
        }

        if (rejected == total) {
            // caller will rethrow UDTE or DNFE
            return null;
        }

        iter = loader.iterator();

        boolean found = false;
        while (iter.hasNext()) {
            DeviceProvider provider = iter.next();

            try {
                // properties was checked by Registry when descriptor was loaded up
                return (P)provider.open(config,null,mode);
            } catch (UnavailableDeviceException e) {
                // throw UPE if sunsequent operation is not success
                found = true;
                continue;
            } catch (DeviceNotFoundException e2) {
                // try other driver.
                // now there is no way to determine what driver accepts config with correct hardware information
                // therefore it is neccesarry to test all of them
                continue;
            }
        }
        if (found) {
            throw new UnavailableDeviceException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_FOUND_BUT_PERIPHERAL_IS_BUSY)
            );
        } else {
            throw new DeviceNotFoundException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_DRIVERS_NOT_MATCH)
            );
        }
    }

    private static <P extends Device<? super P>> P loadFromDriver(boolean supported, Class<P> intf, int mode, String... props) throws
        DeviceNotFoundException, UnavailableDeviceException, InvalidDeviceConfigException,
        UnsupportedAccessModeException, IOException {
        Iterator<DeviceProvider>  iter = ServiceLoader.load(DeviceProvider.class).iterator();
        while (iter.hasNext()) {
            DeviceProvider provider = iter.next();

            if (!provider.getConfigType().isAssignableFrom(intf)) {
                continue;
            }

            // found driver that recognizes intf type
            supported = true;

            if (provider.matches(props)) {
                try {
                    return (P)provider.open(null, props, mode);
                } catch (UnavailableDeviceException e) {
                    if (!iter.hasNext()) {
                        throw e;
                    }
                }
            }
        }
        if (supported) {
            throw new DeviceNotFoundException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_NOT_FOUND, intf.toString())
            );
        } else {
            throw new UnsupportedDeviceTypeException (
                ExceptionMessage.format(ExceptionMessage.DEVICE_DRIVER_MISSING)
            );
        }
    }
}
