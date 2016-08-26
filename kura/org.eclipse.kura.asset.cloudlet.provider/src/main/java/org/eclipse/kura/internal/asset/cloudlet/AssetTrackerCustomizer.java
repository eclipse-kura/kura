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
package org.eclipse.kura.internal.asset.cloudlet;

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Map;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetCloudletMessages;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class AssetTrackerCustomizer is responsible for tracking all the existing
 * asset instances in the OSGi service registry
 */
final class AssetTrackerCustomizer implements ServiceTrackerCustomizer<Asset, Asset> {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(AssetTrackerCustomizer.class);

	/** Localization Resource */
	private static final AssetCloudletMessages s_message = LocalizationAdapter.adapt(AssetCloudletMessages.class);

	/** The map of assets present in the OSGi service registry. */
	private final Map<String, Asset> m_assets;

	/** The Asset Service dependency. */
	private final AssetService m_assetService;

	/** Bundle Context */
	private final BundleContext m_context;

	/**
	 * Instantiates a new asset tracker.
	 *
	 * @param context
	 *            the bundle context
	 * @throws InvalidSyntaxException
	 *             the invalid syntax exception
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	AssetTrackerCustomizer(final BundleContext context, final AssetService assetService) throws InvalidSyntaxException {
		checkNull(context, s_message.bundleContextNonNull());
		checkNull(context, s_message.assetServiceNonNull());

		this.m_assets = CollectionUtil.newConcurrentHashMap();
		this.m_context = context;
		this.m_assetService = assetService;
	}

	/** {@inheritDoc} */
	@Override
	public Asset addingService(final ServiceReference<Asset> reference) {
		final Asset service = this.m_context.getService(reference);
		s_logger.info(s_message.assetFoundAdding());
		if (service != null) {
			return this.addService(service);
		}
		return null;
	}

	/**
	 * Adds the service instance to the map of asset service instances
	 *
	 * @param service
	 *            the asset service instance
	 * @throws KuraRuntimeException
	 *             if provided service is null
	 * @return Asset service instance
	 */
	private Asset addService(final Asset service) {
		checkNull(service, s_message.assetServiceNonNull());
		final String assetPid = this.m_assetService.getAssetPid(service);
		this.m_assets.put(assetPid, service);
		return service;
	}

	/**
	 * Returns the list of found assets in the service registry
	 *
	 * @return the map of assets
	 */
	Map<String, Asset> getRegisteredAssets() {
		return CollectionUtil.newConcurrentHashMap(this.m_assets);
	}

	/** {@inheritDoc} */
	@Override
	public void modifiedService(final ServiceReference<Asset> reference, final Asset service) {
		this.removedService(reference, service);
		this.addingService(reference);
	}

	/** {@inheritDoc} */
	@Override
	public void removedService(final ServiceReference<Asset> reference, final Asset service) {
		this.m_context.ungetService(reference);
		final String assetPid = this.m_assetService.getAssetPid(service);
		if (this.m_assets.containsKey(assetPid)) {
			this.m_assets.remove(assetPid);
		}
		s_logger.info(s_message.assetRemoved() + service);
	}

}
