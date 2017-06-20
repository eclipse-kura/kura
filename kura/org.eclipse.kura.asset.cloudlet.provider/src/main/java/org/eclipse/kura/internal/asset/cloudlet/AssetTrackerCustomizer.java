/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.asset.cloudlet;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.Map;
import java.util.Optional;

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
 * The Class {@link AssetTrackerCustomizer} is responsible for tracking all the existing
 * asset instances in the OSGi service registry
 */
final class AssetTrackerCustomizer implements ServiceTrackerCustomizer<Asset, Asset> {

    /** The Logger instance. */
    private static final Logger logger = LoggerFactory.getLogger(AssetTrackerCustomizer.class);

    /** Localization Resource */
    private static final AssetCloudletMessages message = LocalizationAdapter.adapt(AssetCloudletMessages.class);

    /** The map of assets present in the OSGi service registry. */
    private final Map<String, Asset> assets;

    /** The Asset Service dependency. */
    private final AssetService assetService;

    /** Bundle Context */
    private final BundleContext context;

    /**
     * Instantiates a new asset tracker.
     *
     * @param context
     *            the bundle context
     * @throws InvalidSyntaxException
     *             the invalid syntax exception
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    AssetTrackerCustomizer(final BundleContext context, final AssetService assetService) throws InvalidSyntaxException {
        requireNonNull(context, message.bundleContextNonNull());
        requireNonNull(context, message.assetServiceNonNull());

        this.assets = CollectionUtil.newConcurrentHashMap();
        this.context = context;
        this.assetService = assetService;
    }

    /** {@inheritDoc} */
    @Override
    public Asset addingService(final ServiceReference<Asset> reference) {
        final Asset service = this.context.getService(reference);
        logger.info(message.assetFoundAdding());
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
     * @throws NullPointerException
     *             if provided service is null
     * @return Asset service instance
     */
    private Asset addService(final Asset service) {
        requireNonNull(service, message.assetServiceNonNull());
        final Optional<String> assetPid = this.assetService.getAssetPid(service);
        if (assetPid.isPresent()) {
            this.assets.put(assetPid.get(), service);
        }
        return service;
    }

    /**
     * Returns the list of found assets in the service registry
     *
     * @return the map of assets
     */
    Map<String, Asset> getRegisteredAssets() {
        return CollectionUtil.newConcurrentHashMap(this.assets);
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
        final String assetPid = String.valueOf(reference.getProperty(KURA_SERVICE_PID));
        this.context.ungetService(reference);
        if (assetPid != null && this.assets.containsKey(assetPid)) {
            this.assets.remove(assetPid);
        }
        logger.info(message.assetRemoved() + service);
    }

}
