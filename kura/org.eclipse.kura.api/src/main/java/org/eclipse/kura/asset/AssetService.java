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
package org.eclipse.kura.asset;

import java.util.List;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The interface {@link AssetService} is an utility service API to provide useful
 * methods for {@link Asset}s
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.2
 */
@ProviderType
public interface AssetService {

    /**
     * Gets the {@link Asset} instance by the provided {@link Asset} PID
     * ({@code kura.service.pid})
     *
     * @param assetPid
     *            the {@link Asset} PID ({@code kura.service.pid}) to check
     * @return the {@link Asset} instance wrapped in {@link Optional}
     *         instance or an empty {@link Optional} instance
     * @throws NullPointerException
     *             if the provided {@link Asset} PID ({@code kura.service.pid})
     *             is {@code null}
     */
    public Optional<Asset> getAsset(String assetPid);

    /**
     * Gets the {@link Asset} PID ({@code kura.service.pid}) by the provided
     * {@link Asset} instance
     *
     * @param asset
     *            the {@link Asset} instance to check
     * @return the {@link Asset} PID ({@code kura.service.pid}) wrapped in
     *         {@link Optional} instance or an empty {@link Optional} instance
     * @throws NullPointerException
     *             if the provided {@link Asset} instance is {@code null}
     */
    public Optional<String> getAssetPid(Asset asset);

    /**
     * Returns the list containing all the available {@link Asset} instances
     *
     * @return the list of {@link Asset} instances available in service
     *         registry or empty list if no {@link Asset} instance is available
     */
    public List<Asset> listAssets();

}
