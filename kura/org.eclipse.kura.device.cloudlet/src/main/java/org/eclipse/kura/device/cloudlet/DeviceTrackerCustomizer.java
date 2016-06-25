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

import java.util.Map;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.device.Device;
import org.eclipse.kura.device.internal.BaseDevice;
import org.eclipse.kura.device.internal.DeviceConfiguration;
import org.eclipse.kura.localization.DeviceCloudletMessages;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * The Class DeviceTrackerCustomizer is responsible for tracking all the
 * existing device instances in the OSGi service registry
 */
final class DeviceTrackerCustomizer implements ServiceTrackerCustomizer<Device, Device> {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(DeviceTrackerCustomizer.class);

	/** Localization Resource */
	private static final DeviceCloudletMessages s_messages = LocalizationAdapter.adapt(DeviceCloudletMessages.class);

	/** Bundle Context */
	private final BundleContext m_context;

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
	DeviceTrackerCustomizer(final BundleContext context) throws InvalidSyntaxException {
		this.m_devices = Maps.newConcurrentMap();
		this.m_context = context;
	}

	/** {@inheritDoc} */
	@Override
	public Device addingService(final ServiceReference<Device> reference) {
		final Device service = this.m_context.getService(reference);
		s_logger.info(s_messages.deviceFoundAdding());
		if (service != null) {
			return this.addService(service);
		}
		return null;
	}

	/**
	 * Adds the service instance to the map of device service instances
	 *
	 * @param service
	 *            the device service instance
	 * @throws KuraRuntimeException
	 *             if provided service is null
	 * @return Device service instance
	 */
	private Device addService(final Device service) {
		checkNull(service, s_messages.deviceServiceNonNull());
		final DeviceConfiguration deviceConfiguration = ((BaseDevice) service).getDeviceConfiguration();
		if (deviceConfiguration != null) {
			final String deviceName = deviceConfiguration.getDeviceName();
			this.m_devices.put(deviceName, service);
		}
		return service;
	}

	/**
	 * Returns the list of found devices in the service registry
	 *
	 * @return the map of devices
	 */
	Map<String, Device> getRegisteredDevices() {
		return ImmutableMap.copyOf(this.m_devices);
	}

	/** {@inheritDoc} */
	@Override
	public void modifiedService(final ServiceReference<Device> reference, final Device service) {
		this.removedService(reference, service);
		this.addingService(reference);
	}

	/** {@inheritDoc} */
	@Override
	public void removedService(final ServiceReference<Device> reference, final Device service) {
		this.m_context.ungetService(reference);
		final DeviceConfiguration deviceConfiguration = ((BaseDevice) service).getDeviceConfiguration();
		if (deviceConfiguration != null) {
			final String deviceName = deviceConfiguration.getDeviceName();
			if (this.m_devices.containsKey(deviceName)) {
				this.m_devices.remove(deviceName);
			}
		}
		s_logger.info(s_messages.deviceRemoved() + service);
	}

}
