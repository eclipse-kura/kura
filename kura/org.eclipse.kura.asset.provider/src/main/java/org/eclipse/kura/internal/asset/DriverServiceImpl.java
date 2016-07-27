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
import static org.osgi.framework.Constants.SERVICE_PID;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverEvent;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverService;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetMessages;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * The Class DriverServiceImpl is an implementation of the utility API
 * DriverService to provide useful static factory methods for drivers
 */
public final class DriverServiceImpl implements DriverService {

	/** Localization Resource */
	private static final AssetMessages s_message = LocalizationAdapter.adapt(AssetMessages.class);

	/** {@inheritDoc} */
	@Override
	public Driver getDriver(final String driverId) {
		checkNull(driverId, s_message.driverIdNonNull());
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		final ServiceReference<Driver>[] refs = this.getServiceReferences(context, Driver.class, null);
		for (final ServiceReference<Driver> ref : refs) {
			if (ref.getProperty("kura.service.pid").equals(driverId)) {
				final Driver driver = context.getService(ref);
				return driver;
			}
		}
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public String getDriverId(final Driver driver) {
		checkNull(driver, s_message.driverNonNull());
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		final ServiceReference<Driver>[] refs = this.getServiceReferences(context, Driver.class, null);
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
	public String getDriverPid(final Driver driver) {
		checkNull(driver, s_message.driverNonNull());
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		final ServiceReference<Driver>[] refs = this.getServiceReferences(context, Driver.class, null);
		for (final ServiceReference<Driver> ref : refs) {
			final Driver driverRef = context.getService(ref);
			if (driverRef == driver) {
				return ref.getProperty("service.pid").toString();
			}
		}
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public String getDriverPid(final String driverId) {
		checkNull(driverId, s_message.driverIdNonNull());
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		final ServiceReference<Driver>[] refs = this.getServiceReferences(context, Driver.class, null);
		for (final ServiceReference<Driver> ref : refs) {
			if (ref.getProperty("kura.service.pid").equals(driverId)) {
				return ref.getProperty(SERVICE_PID).toString();
			}
		}
		return null;
	}

	/**
	 * Returns references to <em>all</em> services matching the given class name
	 * and OSGi filter.
	 *
	 * @param bundleContext
	 *            OSGi bundle context
	 * @param clazz
	 *            fully qualified class name (can be <code>null</code>)
	 * @param filter
	 *            valid OSGi filter (can be <code>null</code>)
	 * @return non-<code>null</code> array of references to matching services
	 * @throws KuraRuntimeException
	 *             if the filter syntax is wrong (even though filter is
	 *             nullable) or bundle syntax or class instance name is null
	 */

	private <T> ServiceReference<T>[] getServiceReferences(final BundleContext bundleContext, final Class<T> clazz,
			final String filter) {
		checkNull(bundleContext, s_message.bundleContextNonNull());
		checkNull(bundleContext, s_message.clazzNonNull());

		try {
			final ServiceReference<?>[] refs = bundleContext.getServiceReferences(clazz.getName(), filter);
			@SuppressWarnings("unchecked")
			final ServiceReference<T>[] reference = (refs == null ? new ServiceReference[0] : refs);
			return reference;
		} catch (final InvalidSyntaxException ise) {
			throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, ThrowableUtil.stackTraceAsString(ise));
		}
	}

	/** {@inheritDoc} */
	@Override
	public DriverEvent newDriverEvent(final DriverRecord driverRecord) {
		return new DriverEvent(driverRecord);
	}

	/** {@inheritDoc} */
	@Override
	public DriverRecord newDriverRecord(final long channelId) {
		return new DriverRecord(channelId);
	}

}
