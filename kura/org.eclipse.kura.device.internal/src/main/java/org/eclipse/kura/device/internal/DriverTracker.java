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

import java.util.Collection;

import org.eclipse.kura.device.Device;
import org.eclipse.kura.device.Driver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

/**
 * The Class DriverTracker is responsible for tracking the specific driver as
 * provided. It tracks for the service in the OSGi service registry and it
 * triggers the specific methods as soon as it is injected.
 */
public final class DriverTracker extends ServiceTracker<Object, Object> {

	/** Driver ID Property */
	private static final String DRIVER_ID_PROPERTY = "instance.name";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(DriverTracker.class);

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
	public DriverTracker(final BundleContext context, final Device device, final String driverId)
			throws InvalidSyntaxException {
		super(context, context.createFilter("(" + Constants.OBJECTCLASS + "=*)"), null);
		this.m_driverId = driverId;
		this.m_device = device;
	}

	/** {@inheritDoc} */
	@Override
	public Object addingService(final ServiceReference<Object> reference) {
		final Object service = super.addingService(reference);

		if ((service instanceof Driver) && reference.getProperty(DRIVER_ID_PROPERTY).equals(this.m_driverId)) {
			s_logger.info("Driver has been found by the driver tracker....==> adding service");
			if (service instanceof Driver) {
				((BaseDevice) this.m_device).m_driver = (Driver) service;
			}
		}
		return service;
	}

	/** {@inheritDoc} */
	@Override
	public void open() {
		super.open();
		try {
			final Collection<ServiceReference<Driver>> driverRefs = this.context.getServiceReferences(Driver.class,
					null);
			for (final ServiceReference<Driver> ref : driverRefs) {
				if (ref.getProperty(DRIVER_ID_PROPERTY).equals(this.m_driverId)) {
					s_logger.info("Driver has been found by the driver tracker....==> open");
					if (ref instanceof Driver) {
						((BaseDevice) this.m_device).m_driver = this.context.getService(ref);
					}
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
		if ((service instanceof Driver) && reference.getProperty(DRIVER_ID_PROPERTY).equals(this.m_driverId)) {
			s_logger.info("Driver has been removed by the driver tracker..." + service);
			((BaseDevice) this.m_device).m_driver = null;
		}
	}

}
