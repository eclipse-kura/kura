/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.wire.asset;

import org.eclipse.kura.asset.provider.AssetConstants;

public enum WireAssetConstants {

    LISTEN_PROP_NAME(AssetConstants.CHANNEL_DEFAULT_PROPERTY_PREFIX.value() + "listen"),

    PROPERTY_SEPARATOR("_"),

    PROP_SINGLE_TIMESTAMP_NAME("assetTimestamp"),
    PROP_ASSET_NAME("assetName"),
    PROP_SUFFIX_TIMESTAMP(PROPERTY_SEPARATOR.value() + "timestamp"),
    
    PROP_SUFFIX_UNIT(PROPERTY_SEPARATOR.value() + "unit"),

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