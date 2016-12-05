/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.asset;

import java.util.List;

/**
 * The interface AssetService is an utility service API to provide useful
 * methods for assets
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface AssetService {

    /**
     * Gets the asset instance by the provided asset PID
     * ({@code kura.service.pid}).
     *
     * @param assetPid
     *            the asset PID to check
     * @return the asset instance
     */
    public Asset getAsset(String assetPid);

    /**
     * Gets the asset PID. ({@code kura.service.pid}) by the provided asset
     * instance
     *
     * @param asset
     *            the asset instance to check
     * @return the asset PID
     */
    public String getAssetPid(Asset asset);

    /**
     * Returns the list containing all the available asset instances
     *
     * @return the list of asset available in service registry
     */
    public List<Asset> listAssets();

}
