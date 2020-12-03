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
 *******************************************************************************/
package org.eclipse.kura.ble.eddystone;

import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.internal.ble.eddystone.EddystoneFrameType;
import org.eclipse.kura.internal.ble.eddystone.EddystoneURLScheme;

public class BluetoothLeEddystone extends BluetoothLeBeacon {

    // Common fields
    private EddystoneFrameType frameType;
    private short txPower;
    // UID fields
    private byte[] namespace;
    private byte[] instance;
    // URL fields
    private EddystoneURLScheme urlScheme;
    private String url;

    public BluetoothLeEddystone() {
        super();
    }

    public void configureEddystoneUIDFrame(byte[] namespace, byte[] instance, short txPower) {
        this.frameType = EddystoneFrameType.UID;
        this.txPower = txPower;
        this.namespace = namespace;
        this.instance = instance;
    }

    public void configureEddystoneURLFrame(String url, short txPower) {
        this.frameType = EddystoneFrameType.URL;
        this.txPower = txPower;
        buildURL(url);
    }

    public String getFrameType() {
        return this.frameType.name();
    }

    public void setFrameType(String frameType) {
        this.frameType = EddystoneFrameType.valueOf(frameType);
    }

    public byte[] getNamespace() {
        return this.namespace;
    }

    public void setNamespace(byte[] namespace) {
        this.namespace = namespace;
    }

    public byte[] getInstance() {
        return this.instance;
    }

    public void setInstance(byte[] instance) {
        this.instance = instance;
    }

    public String getUrlScheme() {
        return this.urlScheme.getUrlScheme();
    }

    public void setUrlScheme(String urlScheme) {
        this.urlScheme = EddystoneURLScheme.encodeURLScheme(urlScheme);
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public short getTxPower() {
        return this.txPower;
    }

    public void setTxPower(short txPower) {
        this.txPower = txPower;
    }

    private void buildURL(String url) {
        this.urlScheme = EddystoneURLScheme.encodeURLScheme(url);
        this.url = url.substring(this.urlScheme.getLength());
    }

}
