/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Scott Ware
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
    private AdvertisingReportAddressType directAddressType;
    private String directAddress;
    private AdvertisingReportPhy primaryPhy;
    private AdvertisingReportPhy secondaryPhy;
    private byte[] reportData;
    private int sid;
    private int txPower;
    private int periodicAdvertisingInterval;
    private int length;
    private int rssi;
    private boolean extended;

    public AdvertisingReportEventType getEventType() {
        return this.eventType;
    }

    public void setEventType(int eventType) {
        this.eventType = AdvertisingReportEventType.valueOf((byte) eventType, this.extended);
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

    /**
     * @since 3.0
     */
    public AdvertisingReportAddressType getDirectAddressType() {
        return this.directAddressType;
    }

    /**
     * @since 3.0
     */
    public void setDirectAddressType(int addressType) {
        this.directAddressType = AdvertisingReportAddressType.valueOf((byte) addressType);
    }

    /**
     * @since 3.0
     */
    public String getDirectAddress() {
        return this.directAddress;
    }

    /**
     * @since 3.0
     */
    public void setDirectAddress(String address) {
        this.directAddress = address;
    }

    /**
     * @since 3.0
     */
    public AdvertisingReportPhy getPrimaryPhy() {
        return this.primaryPhy;
    }

    /**
     * @since 3.0
     */
    public void setPrimaryPhy(int phy) {
        this.primaryPhy = AdvertisingReportPhy.valueOf((byte) phy);
    }

    /**
     * @since 3.0
     */
    public AdvertisingReportPhy getSecondaryPhy() {
        return this.secondaryPhy;
    }

    /**
     * @since 3.0
     */
    public void setSecondaryPhy(int phy) {
        this.secondaryPhy = AdvertisingReportPhy.valueOf((byte) phy);
    }

    /**
     * @since 3.0
     */
    public int getPeriodicAdvertisingInterval() {
        return this.periodicAdvertisingInterval;
    }

    /**
     * @since 3.0
     */
    public void setPeriodicAdvertisingInterval(int periodicAdvertisingInterval) {
        this.periodicAdvertisingInterval = periodicAdvertisingInterval;
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

    /**
     * @since 3.0
     */
    public int getSid() {
        return this.sid;
    }

    /**
     * @since 3.0
     */
    public void setSid(int sid) {
        this.sid = sid;
    }

    /**
     * @since 3.0
     */
    public int getTxPower() {
        return this.txPower;
    }

    /**
     * @since 3.0
     */
    public void setTxPower(int txPower) {
        this.txPower = txPower;
    }

    public int getRssi() {
        return this.rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    /**
     * @since 3.0
     */
    public boolean isExtendedReport() {
        return this.extended;
    }

    /**
     * @since 3.0
     */
    public void setExtendedReport(boolean extended) {
        this.extended = extended;
    }
}
