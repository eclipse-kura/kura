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
 * DeviceCloudletMessages is considered to be a localization resource for
 * {@code Device Cloudlet} bundle. It contains all the necessary translation for
 * every string literals mentioned in {@code Device Cloudlet} bundle.
 */
public interface DeviceCloudletMessages {

	@En("Activating Device Cloudlet...")
	public String activating();

	@En("Activating Device Cloudlet...Done")
	public String activatingDone();

	@En("channel_name")
	public String channel();

	@En("Cloudlet GET Request received on the Device Cloudlet")
	public String cloudGETReqReceived();

	@En("Cloudlet GET Request receiving on the Device Cloudlet...")
	public String cloudGETReqReceiving();

	@En("Cloudlet PUT Request received on the Device Cloudlet")
	public String cloudPUTReqReceived();

	@En("Cloudlet PUT Request receiving on the Device Cloudlet...")
	public String cloudPUTReqReceiving();

	@En("Deactivating Device Cloudlet...")
	public String deactivating();

	@En("Deactivating Device Cloudlet...Done")
	public String deactivatingDone();

	@En("Device has been found by Device Cloudlet Tracker....==> adding service")
	public String deviceFoundAdding();

	@En("Device has been found by Device Cloudlet Tracker....==> open")
	public String deviceFoundOpen();

	@En("Device Record cannot be null")
	public String deviceRecordNonNull();

	@En("List of device records cannot be null")
	public String deviceRecordsNonNull();

	@En("Device has been removed by Device Cloudlet Tracker....")
	public String deviceRemoved();

	@En("Device service instance cannot be null")
	public String deviceServiceNonNull();

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
