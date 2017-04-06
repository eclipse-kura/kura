/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetMessages;
import org.eclipse.kura.util.service.ServiceSupplier;
import org.eclipse.kura.util.service.ServiceUtil;
import org.osgi.framework.ServiceReference;

/**
 * The Class {@link AssetServiceImpl} is an implementation of the utility API
 * {@link AssetService} to provide useful factory methods for {@link Asset}s
 */
public final class AssetServiceImpl implements AssetService {

    /** Localization instance */
    private static final AssetMessages message = LocalizationAdapter.adapt(AssetMessages.class);

    /** {@inheritDoc} */
    @Override
    public Optional<Asset> getAsset(final String assetPid) {
        requireNonNull(assetPid, message.assetPidNonNull());
        final String filter = "(" + KURA_SERVICE_PID + "=" + assetPid + ")";
        try (ServiceSupplier<Asset> asset = ServiceSupplier.supply(Asset.class, filter)) {
            return firstElement(asset.get());
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<String> getAssetPid(final Asset asset) {
        requireNonNull(asset, message.assetNonNull());
        final Collection<ServiceReference<Asset>> refs = ServiceUtil.getServiceReferences(Asset.class, null);
        for (final ServiceReference<Asset> ref : refs) {
            try (ServiceSupplier<Asset> assetRef = ServiceSupplier.supply(ref)) {
                final Optional<Asset> assetOptional = firstElement(assetRef.get());
                if (assetOptional.isPresent() && assetOptional.get() == asset) {
                    return Optional.of(ref.getProperty(KURA_SERVICE_PID).toString());
                }
            }
        }
        return Optional.empty();
    }

    /** {@inheritDoc} */
    @Override
    public List<Asset> listAssets() {
        try (ServiceSupplier<Asset> assetRef = ServiceSupplier.supply(Asset.class, null)) {
            return assetRef.get();
        }
    }

    /**
     * Returns the first element from the provided {@link List}
     *
     * @param elements
     *            the {@link List} instance
     * @return the first element if the {@link List} is not empty
     */
    private static <T> Optional<T> firstElement(final List<T> elements) {
        if (elements.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(elements.get(0));
    }
}
