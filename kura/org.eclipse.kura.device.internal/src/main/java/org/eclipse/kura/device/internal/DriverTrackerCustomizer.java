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
package org.eclipse.kura.device.internal;

import org.eclipse.kura.device.Device;
import org.eclipse.kura.device.Driver;
import org.eclipse.kura.localization.DeviceMessages;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class DriverTracker is responsible for tracking the specific driver as
 * provided. It tracks for the service in the OSGi service registry and it
 * triggers the specific methods as soon as it is injected.
 */
final class DriverTrackerCustomizer implements ServiceTrackerCustomizer<Driver, Driver> {

	/** Driver ID Property */
	private static final String DRIVER_ID_PROPERTY = "instance.name";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(DriverTrackerCustomizer.class);

	/** Localization Resource */
	private static final DeviceMessages s_message = LocalizationAdapter.adapt(DeviceMessages.class);

	/** Bundle Context */
	private final BundleContext m_context;

	/** The Device Instance */
	private final Device m_device;

	/** The Driver Identifier */
	private final String m_driverId;

	/**
	 * Instantiates a new driver tracker.
	 *
	 * @param context
	 *            the bundle context
	 * @param device
	 *            the device
	 * @param driverId
	 *            the driver id
	 * @throws InvalidSyntaxException
	 *             the invalid syntax exception
	 */
	DriverTrackerCustomizer(final BundleContext context, final Device device, final String driverId)
			throws InvalidSyntaxException {
		this.m_driverId = driverId;
		this.m_device = device;
		this.m_context = context;
	}

	/** {@inheritDoc} */
	@Override
	public Driver addingService(final ServiceReference<Driver> reference) {
		final Driver driver = this.m_context.getService(reference);
		if (reference.getProperty(DRIVER_ID_PROPERTY).equals(this.m_driverId)) {
			s_logger.info(s_message.driverFoundAdding());
			((BaseDevice) this.m_device).m_driver = driver;
		}
		return driver;
	}

	/** {@inheritDoc} */
	@Override
	public void modifiedService(final ServiceReference<Driver> reference, final Driver service) {
		this.removedService(reference, service);
		this.addingService(reference);
	}

	/** {@inheritDoc} */
	@Override
	public void removedService(final ServiceReference<Driver> reference, final Driver service) {
		this.m_context.ungetService(reference);
		if (reference.getProperty(DRIVER_ID_PROPERTY).equals(this.m_driverId)) {
			s_logger.info(s_message.driverRemoved() + service);
			((BaseDevice) this.m_device).m_driver = null;
		}
	}

}
