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
 * DeviceMessages is considered to be a localization resource for
 * {@code Device Internal} bundle and {@code WireDevice} component. It contains
 * all the necessary translation for every string literals mentioned in
 * {@code Device Internal} bundle and {@code WireDevice} component.
 */
public interface DeviceMessages {

	@En("Activating Base Device...")
	public String activating();

	@En("Activating Base Device...Done")
	public String activatingDone();

	@En("Activating Wire Device...")
	public String activatingWireDevice();

	@En("Activating Wire Device...Done")
	public String activatingWireDeviceDone();

	@En("Attribute Definition Prefix cannot be null")
	public String adPrefixNonNull();

	@En("Boolean")
	public String booleanString();

	@En("Byte Array")
	public String byteArray();

	@En("Byte")
	public String byteStr();

	@En("Channel Name")
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

	@En("Device Configuration")
	public String configuration();

	@En("Deactivating Base Device...")
	public String deactivating();

	@En("Deactivating Base Device...Done")
	public String deactivatingDone();

	@En("Deactivating Wire Device...")
	public String deactivatingWireDevice();

	@En("Deactivating Wire Device...Done")
	public String deactivatingWireDeviceDone();

	@En("Device Description")
	public String description();

	@En("Device Flag")
	public String deviceFlag();

	@En("Device Records cannot be empty")
	public String deviceRecordsNonEmpty();

	@En("Device Records cannot be null")
	public String deviceRecordsNonNull();

	@En("Double")
	public String doubleStr();

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

	@En("Driver has been removed by the driver tracker...")
	public String driverRemoved();

	@En("Error in disconnecting driver...")
	public String errorDriverDisconnection();

	@En("Error while performing read from the wire device...")
	public String errorPerformingRead();

	@En("Error while performing write from the wire device...")
	public String errorPerformingWrite();

	@En("Error while retrieving channels from the provided configurable properties...")
	public String errorRetrievingChannels();

	@En("field name")
	public String fieldName();

	@En("Integer")
	public String integerStr();

	@En("Device Listener cannot be null")
	public String listenerNonNull();

	@En("Long")
	public String longStr();

	@En("Device Name")
	public String name();

	@En("Old Attribute Definition cannot be null")
	public String oldAdNonNull();

	@En("Name of the Point")
	public String pointName();

	@En("Properties cannot be null")
	public String propertiesNonNull();

	@En("Reading device channels...")
	public String readingChannels();

	@En("Reading device channels...Done")
	public String readingChannelsDone();

	@En("Registering Device Listener for monitoring...")
	public String registeringListener();

	@En("Registering Device Listener for monitoring...Done")
	public String registeringListenerDone();

	@En("Retrieving single channel information from the properties...")
	public String retrievingChannel();

	@En("Retrieving single channel information from the properties...Done")
	public String retrievingChannelDone();

	@En("Retrieving configurations from the properties...")
	public String retrievingConf();

	@En("Retrieving configurations from the properties...Done")
	public String retrievingConfDone();

	@En("Short")
	public String shortStr();

	@En("String")
	public String string();

	@En("Timestamp")
	public String timestamp();

	@En("Primitive type of the Point")
	public String typePoint();

	@En("Unregistering Device Listener...")
	public String unregisteringListener();

	@En("Unregistering Device Listener...Done")
	public String unregisteringListenerDone();

	@En("Updating Base Device Configurations...")
	public String updating();

	@En("Updating Base Device Configurations...Done")
	public String updatingDone();

	@En("Updating Wire Device...")
	public String updatingWireDevice();

	@En("Updating Wire Device...Done")
	public String updatingWireDeviceDone();

	@En("Value")
	public String value();

	@En("Value cannot be null")
	public String valueNonNull();

	@En("Wire Envelope cannot be null")
	public String wireEnvelopeNonNull();

	@En("Wire Enveloped received...")
	public String wireEnvelopeReceived();

	@En("Wire records cannot be empty")
	public String wireRecordsNonEmpty();

	@En("Writing to channels...")
	public String writing();

	@En("Writing to channels...Done")
	public String writingDone();

}
