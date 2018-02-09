/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.asset;

import org.eclipse.kura.asset.provider.AssetConstants;

public enum WireAssetConstants {

    LISTEN_PROP_NAME(AssetConstants.CHANNEL_DEFAULT_PROPERTY_PREFIX.value() + "listen"),

    PROPERTY_SEPARATOR("_"),

    PROP_SINGLE_TIMESTAMP_NAME("assetTimestamp"),
    PROP_ASSET_NAME("assetName"),
    PROP_SUFFIX_TIMESTAMP(PROPERTY_SEPARATOR.value() + "timestamp"),

    ERROR_NOT_SPECIFIED_MESSAGE("ERROR NOT SPECIFIED"),
    PROP_SUFFIX_ERROR(PROPERTY_SEPARATOR.value() + "error"),
    PROP_VALUE_NO_ERROR(""),

    CONF_PID("org.eclipse.kura.wire.WireAsset");

    private String value;

    private WireAssetConstants(final String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

}