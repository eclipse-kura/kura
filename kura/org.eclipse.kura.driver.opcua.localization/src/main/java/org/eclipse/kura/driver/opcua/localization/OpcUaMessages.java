/**
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal
 */
package org.eclipse.kura.driver.opcua.localization;

import com.github.rodionmoiseev.c10n.annotations.En;

/**
 * {@link OpcUaMessages} is considered to be a localization resource for
 * {@code OPC-UA Driver} bundle. It contains all the necessary translations for
 * every string literals mentioned in {@code OPC-UA Driver} bundle.
 */
public interface OpcUaMessages {

    @En("Activating OPC-UA Driver.....")
    public String activating();

    @En("Activating OPC-UA Driver.....Done")
    public String activatingDone();

    @En("Connecting to OPC-UA...")
    public String connecting();

    @En("Connecting to OPC-UA...Done")
    public String connectingDone();

    @En("Unable to Connect...No desired Endpoints returned")
    public String connectionProblem();

    @En("Crypto Service cannot be null")
    public String cryptoServiceNonNull();

    @En("Deactivating OPC-UA Driver.....")
    public String deactivating();

    @En("Deactivating OPC-UA Driver.....Done")
    public String deactivatingDone();

    @En("Disconnecting from OPC-UA...")
    public String disconnecting();

    @En("Disconnecting from OPC-UA...Done")
    public String disconnectingDone();

    @En("Unable to Disconnect...")
    public String disconnectionProblem();

    @En("Error while disconnecting....")
    public String errorDisconnecting();

    @En("Error while retrieving Node ID")
    public String errorRetrievingNodeId();

    @En("Error while retrieving Node Namespace index")
    public String errorRetrievingNodeNamespace();

    @En("Error while retrieving value type")
    public String errorRetrievingValueType();

    @En("Error while converting the retrieved value to the defined typed")
    public String errorValueTypeConversion();

    @En("node.id")
    public String nodeId();

    @En("node.namespace.index")
    public String nodeNamespaceIndex();

    @En("Properties cannot be null")
    public String propertiesNonNull();

    @En("OPC-UA Read Operation Failed")
    public String readFailed();

    @En("Channel Record cannot be null")
    public String recordNonNull();

    @En("Channel Record list cannot be null")
    public String recordListNonNull();

    @En("Updating OPC-UA Driver.....")
    public String updating();

    @En("Updating OPC-UA Driver.....Done")
    public String updatingDone();

    @En("Value cannot be null")
    public String valueNonNull();

    @En("Value is null")
    public String valueNull();

    @En("OPC-UA Write Operation Failed")
    public String writeFailed();

    @En("Driver is busy")
    public String errorDriverBusy();

    @En("Searching for endpoints")
    public String searchingEndpoints();

    @En("Got Bad Status: {0}")
    public String errorBadResultStatus(long statusCode);

    @En("Operation Result cannot be null")
    public String errorNullResult();

    @En("Operation Result Status cannot be null")
    public String errorNullStatus();

    @En("Operation Result Variant cannot be null")
    public String errorNullVariant();

}