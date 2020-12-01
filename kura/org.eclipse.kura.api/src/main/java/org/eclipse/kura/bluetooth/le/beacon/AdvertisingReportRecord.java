/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.bluetooth.le.beacon;

import org.osgi.annotation.versioning.ProviderType;

/**
 * AdvertisingReportRecord contains all the fields of a advertising record.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 1.3
 */
@ProviderType
public class AdvertisingReportRecord {

    private AdvertisingReportEventType eventType;
    private AdvertisingReportAddressType addressType;
    private String address;
    private byte[] reportData;
    private int length;
    private int rssi;

    public AdvertisingReportEventType getEventType() {
        return this.eventType;
    }

    public void setEventType(int eventType) {
        this.eventType = AdvertisingReportEventType.valueOf((byte) eventType);
    }

    public AdvertisingReportAddressType getAddressType() {
        return this.addressType;
    }

    public void setAddressType(int addressType) {
        this.addressType = AdvertisingReportAddressType.valueOf((byte) addressType);
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public byte[] getReportData() {
        return this.reportData;
    }

    public void setReportData(byte[] reportData) {
        this.reportData = reportData;
    }

    public int getLength() {
        return this.length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getRssi() {
        return this.rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
