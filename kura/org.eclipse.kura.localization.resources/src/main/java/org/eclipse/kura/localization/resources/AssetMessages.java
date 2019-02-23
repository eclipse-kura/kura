/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
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
 * AssetMessages is considered to be a localization resource for
 * {@code Asset Internal} bundle and {@code WireAsset} component. It contains
 * all the necessary translation for every string literals mentioned in
 * {@code Asset Internal} bundle and {@code WireAsset} component.
 */
public interface AssetMessages {

    @En("Activating Asset...")
    public String activating();

    @En("Activating Asset...Done")
    public String activatingDone();

    @En("asset_flag")
    public String assetFlag();

    @En("Asset Helper Service cannot be null")
    public String assetHelperNonNull();

    @En("Asset cannot be null")
    public String assetNonNull();

    @En("Asset PID cannot be null")
    public String assetPidNonNull();

    @En("Channel Record cannot be null")
    public String channelRecordNonNull();

    @En("Channel Records cannot be empty")
    public String channelRecordsNonEmpty();

    @En("Channel Records cannot be null")
    public String channelRecordsNonNull();

    @En("BOOLEAN")
    public String booleanString();

    @En("Bundle context cannot be null")
    public String bundleContextNonNull();

    @En("BYTE_ARRAY")
    public String byteArray();

    @En("BYTE")
    public String byteStr();

    @En("Channel Key cannot be null")
    public String channelKeyNonNull();

    @En("Channel not found")
    public String channelNameNotFound();

    @En("channel_name")
    public String channelName();

    @En("Name of the Channel")
    public String channelNameDesc();

    @En("Channel name cannot be null")
    public String channelNameNonNull();

    @En("Channel cannot be null")
    public String channelNonNull();

    @En("Associated Channels")
    public String channels();

    @En("The provided set of channel names cannot be empty")
    public String channelsNonEmpty();

    @En("List of channel names cannot be null")
    public String channelsNonNull();

    @En("Channel type not within defined types (READ OR READ_WRITE) : ")
    public String channelTypeNotReadable();

    @En("Channel type not within defined types (WRITE OR READ_WRITE) : ")
    public String channelTypeNotWritable();

    @En("Channel not available")
    public String channelUnavailable();

    @En("Channel Value Type cannot be null")
    public String channelValueTypeNonNull();

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

    @En("DOUBLE")
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

    @En("Driver Name")
    public String driverName();

    @En("Driver cannot be null")
    public String driverNonNull();

    @En("Driver PID cannot be null")
    public String driverPidNonNull();

    @En("Driver has been removed by the driver tracker...")
    public String driverRemoved();

    @En("ERROR")
    public String error();

    @En("Error while trying to track driver instances.")
    public String errorDriverTracking();

    @En("Failed to update current configuration from Driver Descriptor")
    public String errorUpdatingAssetConfiguration();

    @En("Error in disconnecting driver...")
    public String errorDriverDisconnection();

    @En("Error while retrieving channels from the provided configurable properties...")
    public String errorRetrievingChannels();

    @En("field name")
    public String fieldName();

    @En("Provided indices cannot be null")
    public String indicesNonNull();

    @En("INFO")
    public String info();

    @En("INTEGER")
    public String integerStr();

    @En("Asset Listener cannot be null")
    public String listenerNonNull();

    @En("LONG")
    public String longStr();

    @En("Asset Name")
    public String name();

    @En("Configure Wire Asset Instance")
    public String ocdDescription();

    @En("WireAsset")
    public String ocdName();

    @En("Old Attribute Definition cannot be null")
    public String oldAdNonNull();

    @En("Prefix cannot be null")
    public String prefixNonNull();

    @En("Properties cannot be null")
    public String propertiesNonNull();

    @En("READ")
    public String read();

    @En("Reading asset channels...")
    public String readingChannels();

    @En("Reading asset channels...Done")
    public String readingChannelsDone();

    @En("READ_WRITE")
    public String readWrite();

    @En("Registering Channel Listener for monitoring...")
    public String registeringListener();

    @En("Registering Channel Listener for monitoring...Done")
    public String registeringListenerDone();

    @En("Retrieving single channel information from the properties...")
    public String retrievingChannel();

    @En("Retrieving single channel information from the properties...Done")
    public String retrievingChannelDone();

    @En("Retrieving configurations from the properties...")
    public String retrievingConf();

    @En("Retrieving configurations from the properties...Done")
    public String retrievingConfDone();

    @En("SHORT")
    public String shortStr();

    @En("STRING")
    public String string();

    @En("timestamp")
    public String timestamp();

    @En("Type of the channel")
    public String type();

    @En("Value type of the channel")
    public String typeChannel();

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

    @En("WRITE")
    public String write();

    @En("Writing to channels...")
    public String writing();

    @En("Writing to channels...Done")
    public String writingDone();

    @En("Failed to prepare read")
    public String errorPreparingRead();

    @En("Failed close prepared read")
    public String errorClosingPreparingRead();

    @En("Failed to register channel listener")
    public String errorRegisteringChannelListener();

    @En("Failed to unregister channel listener")
    public String errorUnregisteringChannelListener();

    @En("Determines if the channel is enabled or not")
    public String enabledDescription();

    @En("Channel is not enabled")
    public String channelNotEnabled();

}
