/**
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.driver.ble.sensortag.localization;

import com.github.rodionmoiseev.c10n.annotations.En;

/**
 * {@link SensorTagMessages} is a localization resource for
 * {@code SensortTag Driver} bundle. It contains all the necessary translations for
 * every string literals mentioned in {@code SensortTag Driver} bundle.
 */
public interface SensorTagMessages {

    @En("Connecting to SensortTag...")
    public String connecting();

    @En("Connecting to SensortTag...Done")
    public String connectingDone();

    @En("Unable to Connect...")
    public String connectionProblem();

    @En("Disconnecting from SensortTag...")
    public String disconnecting();

    @En("Disconnecting from SensortTag...Done")
    public String disconnectingDone();

    @En("Unable to Disconnect...")
    public String disconnectionProblem();

    @En("Error while disconnecting....")
    public String errorDisconnecting();

    @En("Error while retrieving SensortTag address")
    public String errorRetrievingAddress();

    @En("Error while retrieving sensor name")
    public String errorRetrievingSensorName();

    @En("Error while retrieving value type")
    public String errorRetrievingValueType();

    @En("Error while converting the retrieved value to the defined typed")
    public String errorValueTypeConversion();

    @En("Properties cannot be null")
    public String propertiesNonNull();

    @En("SensortTag Read Operation Failed")
    public String readFailed();

    @En("Channel Record cannot be null")
    public String recordNonNull();

    @En("Channel Record list cannot be null")
    public String recordListNonNull();

    @En("Value cannot be null")
    public String valueNonNull();

    @En("Value is null")
    public String valueNull();

    @En("SensortTag Write Operation Failed")
    public String writeFailed();

    @En("Driver is busy")
    public String errorDriverBusy();

    @En("Got Bad Status: {0}")
    public String errorBadResultStatus(long statusCode);

    @En("Operation Result cannot be null")
    public String errorNullResult();

    @En("Operation Result Status cannot be null")
    public String errorNullStatus();

    @En("Failed to stop discovery")
    public String errorStopDiscovery();

}