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

import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @deprecated This class is deprecated in favor of {@link org.eclipse.kura.bluetooth.le.beacon.AdvertisingReportRecord}
 *
 */
@ProviderType
@Deprecated
public class AdvertisingReportRecord {

    private int eventType;
    private int addressType;
    private String address;
    private byte[] reportData;
    private int length;

    public AdvertisingReportRecord() {
    }

    public int getEventType() {
        return this.eventType;
    }

    public void setEventType(int eventType) {
        this.eventType = eventType;
    }

    public int getAddressType() {
        return this.addressType;
    }

    public void setAddressType(int addressType) {
        this.addressType = addressType;
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
}
