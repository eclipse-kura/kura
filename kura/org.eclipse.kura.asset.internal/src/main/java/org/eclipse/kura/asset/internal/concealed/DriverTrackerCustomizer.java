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
package org.eclipse.kura.asset.internal.concealed;

import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.asset.internal.AssetOptions.ASSET_DRIVER_PROP;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.Driver;
import org.eclipse.kura.asset.internal.BaseAsset;
import org.eclipse.kura.localization.AssetMessages;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class DriverTrackerCustomizer is responsible for tracking the specific
 * driver as provided. It tracks for the service in the OSGi service registry
 * and it triggers the specific methods as soon as it is injected.
 */
public final class DriverTrackerCustomizer implements ServiceTrackerCustomizer<Driver, Driver> {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(DriverTrackerCustomizer.class);

	/** Localization Resource */
	private static final AssetMessages s_message = LocalizationAdapter.adapt(AssetMessages.class);

	/** The Asset Instance */
	private final Asset m_asset;

	/** Bundle Context */
	private final BundleContext m_context;

	/** The Driver Identifier */
	private final String m_driverId;

	/**
	 * Instantiates a new driver tracker.
	 *
	 * @param context
	 *            the bundle context
	 * @param asset
	 *            the asset
	 * @param driverId
	 *            the driver id
	 * @throws InvalidSyntaxException
	 *             the invalid syntax exception
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public DriverTrackerCustomizer(final BundleContext context, final Asset asset, final String driverId)
			throws InvalidSyntaxException {
		checkNull(context, s_message.bundleContextNonNull());
		checkNull(asset, s_message.assetNonNull());
		checkNull(driverId, s_message.driverIdNonNull());

		this.m_driverId = driverId;
		this.m_asset = asset;
		this.m_context = context;
	}

	/** {@inheritDoc} */
	@Override
	public Driver addingService(final ServiceReference<Driver> reference) {
		final Driver driver = this.m_context.getService(reference);
		if (reference.getProperty(ASSET_DRIVER_PROP).equals(this.m_driverId)) {
			s_logger.info(s_message.driverFoundAdding());
			((BaseAsset) this.m_asset).setDriver(driver);
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
		if (reference.getProperty(ASSET_DRIVER_PROP).equals(this.m_driverId)) {
			s_logger.info(s_message.driverRemoved() + service);
			((BaseAsset) this.m_asset).setDriver(null);
		}
	}

}
