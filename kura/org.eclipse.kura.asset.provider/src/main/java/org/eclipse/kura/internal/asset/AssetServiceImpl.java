/**
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.asset;

import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.List;

import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetMessages;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.util.service.ServiceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * The Class AssetServiceImpl is an implementation of the utility API
 * {@link AssetService} to provide useful factory methods for assets
 */
public final class AssetServiceImpl implements AssetService {

    /** Localization Resource */
    private static final AssetMessages s_message = LocalizationAdapter.adapt(AssetMessages.class);

    /** {@inheritDoc} */
    @Override
    public Asset getAsset(final String assetPid) {
        checkNull(assetPid, s_message.assetPidNonNull());
        final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        final ServiceReference<Asset>[] refs = ServiceUtil.getServiceReferences(context, Asset.class, null);
        try {
            for (final ServiceReference<Asset> ref : refs) {
                if (ref.getProperty(KURA_SERVICE_PID).equals(assetPid)) {
                    return context.getService(ref);
                }
            }
        } finally {
            ServiceUtil.ungetServiceReferences(context, refs);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getAssetPid(final Asset asset) {
        checkNull(asset, s_message.assetNonNull());
        final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        final ServiceReference<Asset>[] refs = ServiceUtil.getServiceReferences(context, Asset.class, null);
        try {
            for (final ServiceReference<Asset> ref : refs) {
                final Asset assetRef = context.getService(ref);
                if (assetRef == asset) {
                    return ref.getProperty(KURA_SERVICE_PID).toString();
                }
            }
        } finally {
            ServiceUtil.ungetServiceReferences(context, refs);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<Asset> listAssets() {
        final List<Asset> assets = CollectionUtil.newArrayList();
        final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        final ServiceReference<Asset>[] refs = ServiceUtil.getServiceReferences(context, Asset.class, null);
        try {
            for (final ServiceReference<Asset> ref : refs) {
                final Asset assetRef = context.getService(ref);
                assets.add(assetRef);
            }
        } finally {
            ServiceUtil.ungetServiceReferences(context, refs);
        }
        return assets;
    }

}
