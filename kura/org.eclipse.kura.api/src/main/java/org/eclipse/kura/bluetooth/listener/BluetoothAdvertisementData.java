/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.bluetooth.listener;

import java.util.ArrayList;
import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @deprecated
 *
 */
@ProviderType
@Deprecated
public class BluetoothAdvertisementData {

    private byte[] rawData;
    private byte packetType;
    private byte eventType;
    private int parameterLength;
    private byte subEventCode;
    private int numberOfReports;

    private List<AdvertisingReportRecord> reportRecords;

    public byte[] getRawData() {
        return this.rawData;
    }

    public void setRawData(byte[] rawData) {
        this.rawData = new byte[rawData.length];
        System.arraycopy(rawData, 0, this.rawData, 0, rawData.length);
    }

    public byte getPacketType() {
        return this.packetType;
    }

    public void setPacketType(byte packetType) {
        this.packetType = packetType;
    }

    public byte getEventType() {
        return this.eventType;
    }

    public void setEventType(byte eventType) {
        this.eventType = eventType;
    }

    public int getParameterLength() {
        return this.parameterLength;
    }

    public void setParameterLength(int parameterLength) {
        this.parameterLength = parameterLength;
    }

    public byte getSubEventCode() {
        return this.subEventCode;
    }

    public void setSubEventCode(byte subEventCode) {
        this.subEventCode = subEventCode;
    }

    public int getNumberOfReports() {
        return this.numberOfReports;
    }

    public void setNumberOfReports(int numberOfReports) {
        this.numberOfReports = numberOfReports;
    }

    public List<AdvertisingReportRecord> getReportRecords() {
        return this.reportRecords;
    }

    public void addReportRecord(AdvertisingReportRecord advertisingReportRecord) {
        if (this.reportRecords == null) {
            this.reportRecords = new ArrayList<>();
        }
        this.reportRecords.add(advertisingReportRecord);
    }
}
