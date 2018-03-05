/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *     Amit Kumar Mondal
 *******************************************************************************/
package org.eclipse.kura.internal.asset;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.List;

import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.util.service.ServiceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * The Class AssetServiceImpl is an implementation of the utility API
 * {@link AssetService} to provide useful factory methods for assets
 */
public class AssetServiceImpl implements AssetService {

    /** {@inheritDoc} */
    @Override
    public Asset getAsset(final String assetPid) {
        requireNonNull(assetPid, "Asset PID cannot be null");
        final BundleContext context = getBundleContext();
        final ServiceReference<Asset>[] refs = getAssetServiceReferences(context);
        try {
            for (final ServiceReference<Asset> ref : refs) {
                if (ref.getProperty(KURA_SERVICE_PID).equals(assetPid)) {
                    return context.getService(ref);
                }
            }
        } finally {
            ungetServiceReferences(context, refs);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getAssetPid(final Asset asset) {
        requireNonNull(asset, "Asset cannot be null");
        final BundleContext context = getBundleContext();
        final ServiceReference<Asset>[] refs = getAssetServiceReferences(context);
        try {
            for (final ServiceReference<Asset> ref : refs) {
                final Asset assetRef = context.getService(ref);
                if (assetRef == asset) {
                    return ref.getProperty(KURA_SERVICE_PID).toString();
                }
            }
        } finally {
            ungetServiceReferences(context, refs);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<Asset> listAssets() {
        final List<Asset> assets = CollectionUtil.newArrayList();
        final BundleContext context = getBundleContext();
        final ServiceReference<Asset>[] refs = getAssetServiceReferences(context);
        try {
            for (final ServiceReference<Asset> ref : refs) {
                final Asset assetRef = context.getService(ref);
                assets.add(assetRef);
            }
        } finally {
            ungetServiceReferences(context, refs);
        }
        return assets;
    }

    protected ServiceReference<Asset>[] getAssetServiceReferences(final BundleContext context) {
        return ServiceUtil.getServiceReferences(context, Asset.class, null);
    }

    protected BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    }

    protected void ungetServiceReferences(final BundleContext context, final ServiceReference<Asset>[] refs) {
        ServiceUtil.ungetServiceReferences(context, refs);
    }
}
