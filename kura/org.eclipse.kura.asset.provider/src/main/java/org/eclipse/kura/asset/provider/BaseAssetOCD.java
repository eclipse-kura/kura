/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     
 *******************************************************************************/
package org.eclipse.kura.asset.provider;

import static org.eclipse.kura.asset.provider.AssetConstants.ASSET_DESC_PROP;
import static org.eclipse.kura.asset.provider.AssetConstants.ASSET_DRIVER_PROP;

import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;

public class BaseAssetOCD extends Tocd {

    public BaseAssetOCD() {
        setId(BaseAsset.CONF_PID);
        setName("Wire Asset");
        setDescription("Configure Wire Asset Instance");

        final Tad assetDescriptionAd = new Tad();
        assetDescriptionAd.setId(ASSET_DESC_PROP.value());
        assetDescriptionAd.setName(ASSET_DESC_PROP.value());
        assetDescriptionAd.setCardinality(0);
        assetDescriptionAd.setType(Tscalar.STRING);
        assetDescriptionAd.setDescription("Asset Description");
        assetDescriptionAd.setRequired(false);

        final Tad driverNameAd = new Tad();
        driverNameAd.setId(ASSET_DRIVER_PROP.value());
        driverNameAd.setName(ASSET_DRIVER_PROP.value());
        driverNameAd.setCardinality(0);
        driverNameAd.setType(Tscalar.STRING);
        driverNameAd.setDescription("Driver Name");
        driverNameAd.setRequired(true);

        addAD(assetDescriptionAd);
        addAD(driverNameAd);
    }

}
