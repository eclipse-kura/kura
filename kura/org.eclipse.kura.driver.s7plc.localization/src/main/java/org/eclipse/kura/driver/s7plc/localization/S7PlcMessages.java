/**
 * Copyright (c) 2017 Eurotech and/or its affiliates
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
package org.eclipse.kura.driver.s7plc.localization;

import com.github.rodionmoiseev.c10n.annotations.En;

/**
 * S7PlcMessages is considered to be a localization resource for
 * {@code S7 PLC Driver} bundle. It contains all the necessary translations for
 * every string literals mentioned in {@code S7 PLC Driver} bundle.
 */
public interface S7PlcMessages {

    @En("Activating S7 PLC Driver.....")
    public String activating();

    @En("Activating S7 PLC Driver.....Done")
    public String activatingDone();

    @En("Authenticating")
    public String authenticating();

    @En("DB number")
    public String areaNo();

    @En("byte count")
    public String byteCount();

    @En("Byte Count (required for READ operation)")
    public String byteCountDesc();

    @En("Connecting to S7 PLC...")
    public String connecting();

    @En("Connection problems detected, disconnecting, will attemp to reconnect at next read/write")
    public String connectionProblemsDetected();

    @En("Reconnecting after configuration update...")
    public String reconnectingAfterConfigurationUpdate();

    @En("Failed to reset connection after update")
    public String errorReconnectFailed();

    @En("Connecting to S7 PLC...Done")
    public String connectingDone();

    @En("Unable to Connect...")
    public String connectionProblem();

    @En("Deactivating S7 PLC Driver.....")
    public String deactivating();

    @En("Deactivating S7 PLC Driver.....Done")
    public String deactivatingDone();

    @En("Disconnecting from S7 PLC...")
    public String disconnecting();

    @En("Disconnecting from S7 PLC...Done")
    public String disconnectingDone();

    @En("Unable to Disconnect...")
    public String disconnectionProblem();

    @En("Error while disconnecting....")
    public String errorDisconnecting();

    @En("Failed to connect to PLC, ConnectTo() failed with code: ")
    public String errorConnectToFailed();

    @En("Connection failed, unexpected exception")
    public String errorUnexpectedConnectionException();

    @En("Unexpected exception")
    public String errorUnexpectedException();

    @En("Authentication failed, SetSessionPassword() failed with code: ")
    public String errorAuthenticating();

    @En("Operation failed due to IO error")
    public String errorIOFailed();

    @En("Error while retrieving Area No")
    public String errorRetrievingAreaNo();

    @En("Error while retrieving Area Offset")
    public String errorRetrievingAreaOffset();

    @En("Error while retrieving Byte Count")
    public String errorRetrievingByteCount();

    @En("Error while retrieving value type")
    public String errorRetrievingValueType();

    @En("Error while retrieving S7 Data Type")
    public String errorRetrievingS7DataType();

    @En("Error while retreiving bit index")
    public String errorRetrievingBitIndex();

    @En("Channel Value Type must be ")
    public String errorConvertingType();

    @En("Unable to determine operation")
    public String errorUnknownOperation();

    @En("Invalid channel configuration")
    public String errorInvalidChannelConfig();

    @En("Invalid channel configuration, requested access to invalid address")
    public String errorUnfeasibleProblem();

    @En("offset")
    public String offset();

    @En("Properties cannot be null")
    public String propertiesNonNull();

    @En("Updating S7 PLC Driver.....")
    public String updating();

    @En("Updating S7 PLC Driver.....Done")
    public String updatingDone();

    @En("Step7 Data Type")
    public String s7DataTypeDesc();

    @En("bit index")
    public String bitIndexDesc();
}