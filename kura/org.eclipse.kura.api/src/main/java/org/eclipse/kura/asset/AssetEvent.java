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

import static java.util.Objects.requireNonNull;

import org.eclipse.kura.annotation.NotThreadSafe;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This class represents an event occurred while monitoring specific channel
 * configuration by the asset
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.2
 */
@NotThreadSafe
@ProviderType
public class AssetEvent {

    /**
     * Represents the asset record as triggered due to the asset specific
     * monitor operation
     */
    private final AssetRecord assetRecord;

    /**
     * Instantiates a new asset event.
     *
     * @param assetRecord
     *            the asset record
     * @throws NullPointerException
     *             if the argument is null
     */
    public AssetEvent(final AssetRecord assetRecord) {
        requireNonNull(assetRecord, "Asset record cannot be null");
        this.assetRecord = assetRecord;
    }

    /**
     * Returns the associated asset record.
     *
     * @return the asset record
     */
    public AssetRecord getAssetRecord() {
        return this.assetRecord;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "AssetEvent [assetRecord=" + this.assetRecord + "]";
    }

}
