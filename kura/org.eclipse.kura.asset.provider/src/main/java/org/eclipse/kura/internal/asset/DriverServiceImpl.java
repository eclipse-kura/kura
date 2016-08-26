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
package org.eclipse.kura.internal.asset;

import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.asset.AssetConstants.ASSET_DRIVER_PROP;
import static org.eclipse.kura.driver.DriverConstants.DRIVER_PID;

import java.util.List;

import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverService;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetMessages;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.util.service.ServiceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * The Class DriverServiceImpl is an implementation of the utility API
 * {@link DriverService} to provide useful factory methods for drivers
 */
public final class DriverServiceImpl implements DriverService {

	/** Localization Resource */
	private static final AssetMessages s_message = LocalizationAdapter.adapt(AssetMessages.class);

	/** {@inheritDoc} */
	@Override
	public Driver getDriver(final String driverId) {
		checkNull(driverId, s_message.driverPidNonNull());
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		final ServiceReference<Driver>[] refs = ServiceUtil.getServiceReferences(context, Driver.class, null);
		for (final ServiceReference<Driver> ref : refs) {
			if (ref.getProperty(DRIVER_PID.value()).equals(driverId)) {
				return context.getService(ref);
			}
		}
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public String getDriverPid(final Driver driver) {
		checkNull(driver, s_message.driverNonNull());
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		final ServiceReference<Driver>[] refs = ServiceUtil.getServiceReferences(context, Driver.class, null);
		for (final ServiceReference<Driver> ref : refs) {
			final Driver driverRef = context.getService(ref);
			if (driverRef == driver) {
				return ref.getProperty(ASSET_DRIVER_PROP.value()).toString();
			}
		}
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public List<Driver> listDrivers() {
		final List<Driver> drivers = CollectionUtil.newArrayList();
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		final ServiceReference<Driver>[] refs = ServiceUtil.getServiceReferences(context, Driver.class, null);
		for (final ServiceReference<Driver> ref : refs) {
			final Driver driverRef = context.getService(ref);
			drivers.add(driverRef);
		}
		return drivers;
	}

}
