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
package org.eclipse.kura.localization.resources;

import com.github.rodionmoiseev.c10n.annotations.En;

/**
 * AssetMessages is considered to be a localization resource for
 * {@code Asset Internal} bundle and {@code WireAsset} component. It contains
 * all the necessary translation for every string literals mentioned in
 * {@code Asset Internal} bundle and {@code WireAsset} component.
 */
public interface AssetMessages {

	@En("Activating Base Asset...")
	public String activating();

	@En("Activating Base Asset...Done")
	public String activatingDone();

	@En("asset_flag")
	public String assetFlag();

	@En("Asset Helper Service cannot be null")
	public String assetHelperNonNull();

	@En("Asset cannot be null")
	public String assetNonNull();

	@En("Asset Record cannot be null")
	public String assetRecordNonNull();

	@En("Asset Records cannot be empty")
	public String assetRecordsNonEmpty();

	@En("Asset Records cannot be null")
	public String assetRecordsNonNull();

	@En("Bundle context cannot be null")
	public String bundleContextNonNull();

	@En("Channel ID must be provided as an integer")
	public String channelAsInteger();

	@En("Channel ID cannot be zero or less")
	public String channelIdNotLessThanZero();

	@En("channel_name")
	public String channelName();

	@En("Channel name cannot be null")
	public String channelNameNonNull();

	@En("Channel cannot be null")
	public String channelNonNull();

	@En("Associated Channels")
	public String channels();

	@En("List of channel names cannot be empty")
	public String channelsNonEmpty();

	@En("List of channel names cannot be null")
	public String channelsNonNull();

	@En("Channel type not within defined types (READ OR READ_WRITE) : ")
	public String channelTypeNotReadable();

	@En("Channel type not within defined types (WRITE OR READ_WRITE) : ")
	public String channelTypeNotWritable();

	@En("Channel not available")
	public String channelUnavailable();

	@En("Asset Configuration")
	public String configuration();

	@En("Component context cannot be null")
	public String contextNonNull();

	@En("Release Asset Resources...")
	public String deactivating();

	@En("Release Asset Resources...Done")
	public String deactivatingDone();

	@En("Asset Description")
	public String description();

	@En("Attaching driver instance...")
	public String driverAttach();

	@En("Attaching driver instance...Done")
	public String driverAttachDone();

	@En("Driver Event cannot be null")
	public String driverEventNonNull();

	@En("Driver has been found by the driver tracker....==> adding service")
	public String driverFoundAdding();

	@En("Driver has been found by the driver tracker....==> open")
	public String driverFoundOpen();

	@En("Driver ID cannot be null")
	public String driverIdNonNull();

	@En("Driver Name")
	public String driverName();

	@En("Driver cannot be null")
	public String driverNonNull();

	@En("Driver Record cannot be null")
	public String driverRecordNonNull();

	@En("Driver has been removed by the driver tracker...")
	public String driverRemoved();

	@En("Error in disconnecting driver...")
	public String errorDriverDisconnection();

	@En("Error while retrieving channels from the provided configurable properties...")
	public String errorRetrievingChannels();

	@En("field name")
	public String fieldName();

	@En("Provided indices cannot be null")
	public String indicesNonNull();

	@En("Asset Listener cannot be null")
	public String listenerNonNull();

	@En("Asset Name")
	public String name();

	@En("Prefix cannot be null")
	public String prefixNonNull();

	@En("Properties cannot be null")
	public String propertiesNonNull();

	@En("Reading asset channels...")
	public String readingChannels();

	@En("Reading asset channels...Done")
	public String readingChannelsDone();

	@En("Registering Asset Listener for monitoring...")
	public String registeringListener();

	@En("Registering Asset Listener for monitoring...Done")
	public String registeringListenerDone();

	@En("Retrieving single channel information from the properties...")
	public String retrievingChannel();

	@En("Retrieving single channel information from the properties...Done")
	public String retrievingChannelDone();

	@En("Retrieving configurations from the properties...")
	public String retrievingConf();

	@En("Retrieving configurations from the properties...Done")
	public String retrievingConfDone();

	@En("timestamp")
	public String timestamp();

	@En("Unregistering Asset Listener...")
	public String unregisteringListener();

	@En("Unregistering Asset Listener...Done")
	public String unregisteringListenerDone();

	@En("Initializing Asset Configurations...")
	public String updating();

	@En("Initializing Asset Configurations...Done")
	public String updatingDone();

	@En("value")
	public String value();

	@En("Value cannot be null")
	public String valueNonNull();

	@En("Wire Envelope cannot be null")
	public String wireEnvelopeNonNull();

	@En("Wire Enveloped received...")
	public String wireEnvelopeReceived();

	@En("Writing to channels...")
	public String writing();

	@En("Writing to channels...Done")
	public String writingDone();

}
