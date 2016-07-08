/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.localization;

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

	@En("channel_name")
	public String channel();

	@En("Cloudlet GET Request received on the Asset Cloudlet")
	public String cloudGETReqReceived();

	@En("Cloudlet GET Request receiving on the Asset Cloudlet...")
	public String cloudGETReqReceiving();

	@En("Cloudlet PUT Request received on the Asset Cloudlet")
	public String cloudPUTReqReceived();

	@En("Cloudlet PUT Request receiving on the Asset Cloudlet...")
	public String cloudPUTReqReceiving();

	@En("Deactivating Asset Cloudlet...")
	public String deactivating();

	@En("Deactivating Asset Cloudlet...Done")
	public String deactivatingDone();

	@En("Asset has been found by Asset Cloudlet Tracker....==> adding service")
	public String assetFoundAdding();

	@En("Asset has been found by Asset Cloudlet Tracker....==> open")
	public String assetFoundOpen();

	@En("Asset Record cannot be null")
	public String assetRecordNonNull();

	@En("List of asset records cannot be null")
	public String assetRecordsNonNull();

	@En("Asset has been removed by Asset Cloudlet Tracker....")
	public String assetRemoved();

	@En("Asset service instance cannot be null")
	public String assetServiceNonNull();

	@En("flag")
	public String flag();

	@En("Response Payload cannot be null")
	public String respPayloadNonNull();

	@En("timestamp")
	public String timestamp();

	@En("User Provided Type cannot be null")
	public String typeNonNull();

	@En("value")
	public String value();

	@En("User Provided Value cannot be null")
	public String valueNonNull();

}
