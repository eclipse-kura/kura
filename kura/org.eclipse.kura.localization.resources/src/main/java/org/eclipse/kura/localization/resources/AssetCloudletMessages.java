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
package org.eclipse.kura.localization.resources;

import com.github.rodionmoiseev.c10n.annotations.En;

/**
 * AssetCloudletMessages is considered to be a localization resource for
 * {@code Asset Cloudlet} bundle. It contains all the necessary translation for
 * every string literals mentioned in {@code Asset Cloudlet} bundle.
 */
public interface AssetCloudletMessages {

    @En("Activating Asset Cloudlet...")
    public String activating();

    @En("Activating Asset Cloudlet...Done")
    public String activatingDone();

    @En("AssetCloudlet activation failed {0}")
    public String activationFailed(Throwable e);

    @En("Asset has been found by Asset Cloudlet Tracker....==> adding service")
    public String assetFoundAdding();

    @En("Asset has been found by Asset Cloudlet Tracker....==> open")
    public String assetFoundOpen();

    @En("Asset PID cannot be null")
    public String assetPidNonNull();

    @En("Channel Record cannot be null")
    public String channelRecordNonNull();

    @En("List of channel records cannot be null")
    public String channelRecordsNonNull();

    @En("Asset has been removed by Asset Cloudlet Tracker....")
    public String assetRemoved();

    @En("Asset service instance cannot be null")
    public String assetServiceNonNull();

    @En("Bundle context cannot be null")
    public String bundleContextNonNull();

    @En("Channel Name cannot be null")
    public String channelNameNonNull();

    @En("Channel cannot be null")
    public String channelNonNull();

    @En("List of channel Names cannot be empty")
    public String channelsNonEmpty();

    @En("List of channel Names cannot be null")
    public String channelsNonNull();

    @En("Cloudlet GET Request received on the Asset Cloudlet...")
    public String cloudGETReqReceived();

    @En("Cloudlet EXEC Request received on the Asset Cloudlet...")
    public String cloudEXECReqReceived();

    @En("Deactivating Asset Cloudlet...")
    public String deactivating();

    @En("Deactivating Asset Cloudlet...Done")
    public String deactivatingDone();

    @En("Response Payload cannot be null")
    public String respPayloadNonNull();

    @En("User Provided Type cannot be null")
    public String typeNonNull();

    @En("User Provided Value cannot be null")
    public String valueNonNull();

    @En("The provided value type is erroneous")
    public String valueTypeConversionError();

    @En("Unknown error")
    public String unknownError();

    @En("Asset not found")
    public String assetNotFound();

}