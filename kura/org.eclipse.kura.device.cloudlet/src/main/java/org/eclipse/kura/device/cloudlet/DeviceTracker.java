/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.device.cloudlet;

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Collection;
import java.util.Map;

import org.eclipse.kura.device.Device;
import org.eclipse.kura.device.internal.BaseDevice;
import org.eclipse.kura.device.internal.DeviceConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

/**
 * The Class DeviceTracker is responsible for tracking all the existing device
 * instances in the OSGi service registry
 */
final class DeviceTracker extends ServiceTracker<Object, Object> {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(DeviceTracker.class);

	/** The map of devices present in the OSGi service registry. */
	private final Map<String, Device> m_devices;

	/**
	 * Instantiates a new device tracker.
	 *
	 * @param context
	 *            the bundle context
	 * @throws InvalidSyntaxException
	 *             the invalid syntax exception
	 */
	DeviceTracker(final BundleContext context) throws InvalidSyntaxException {
		super(context, context.createFilter("(" + Constants.OBJECTCLASS + "=*)"), null);
		this.m_devices = Maps.newConcurrentMap();
	}

	/** {@inheritDoc} */
	@Override
	public Object addingService(final ServiceReference<Object> reference) {
		final Object service = super.addingService(reference);
		if (service instanceof Device) {
			s_logger.info("Device has been found by Device Cloudlet Tracker....==> adding service");
			if (service instanceof Device) {
				this.addService(service);
			}
		}
		return service;
	}

	/**
	 * Adds the service instance to the map of device service instances
	 *
	 * @param service
	 *            the device service instance
	 */
	private void addService(final Object service) {
		checkNull(service, "Device service instance cannot be null");
		final Device device = (Device) service;
		final DeviceConfiguration deviceConfiguration = ((BaseDevice) device).getDeviceConfiguration();
		if (deviceConfiguration != null) {
			final String deviceName = deviceConfiguration.getDeviceName();
			this.m_devices.put(deviceName, device);
		}
	}

	/**
	 * Returns the list of found devices in the service registry
	 *
	 * @return the map of devices
	 */
	Map<String, Device> getRegisteredDevices() {
		return this.m_devices;
	}

	/** {@inheritDoc} */
	@Override
	public void open() {
		super.open();
		try {
			final Collection<ServiceReference<Device>> deviceServiceRefs = this.context
					.getServiceReferences(Device.class, null);
			for (final ServiceReference<Device> ref : deviceServiceRefs) {
				s_logger.info("Device has been found by Device Cloudlet Tracker....==> open");
				final Object object = this.context.getService(ref);
				if (object instanceof Device) {
					this.addService(object);
				}
			}
		} catch (final InvalidSyntaxException e) {
			Throwables.propagate(e);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void removedService(final ServiceReference<Object> reference, final Object service) {
		super.removedService(reference, service);
		if (service instanceof Device) {
			final DeviceConfiguration deviceConfiguration = ((BaseDevice) service).getDeviceConfiguration();
			if (deviceConfiguration != null) {
				final String deviceName = deviceConfiguration.getDeviceName();
				if (this.m_devices.containsKey(deviceName)) {
					this.m_devices.remove(deviceName);
				}
			}
			s_logger.info("Device has been removed by Device Cloudlet Tracker..." + service);
		}
	}

}
