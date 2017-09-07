/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.bluetooth.le.beacon;

import org.osgi.annotation.versioning.ProviderType;

/**
 * BluetoothLeBeacon is a representation of a generic Beacon advertise packet.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 1.3
 */
@ProviderType
public abstract class BluetoothLeBeacon {

    public static final byte AD_BYTES_NUMBER = 0x02;
    public static final byte AD_FLAG = 0x01;

    private boolean leBrHost;
    private boolean leBrController;
    private boolean brEdrSupported;
    private boolean leGeneral;
    private boolean leLimited;
    private String address;
    private int rssi;

    public BluetoothLeBeacon() {
        this.leBrHost = true;
        this.leBrController = true;
        this.brEdrSupported = false;
        this.leGeneral = true;
        this.leLimited = false;
    }

    public boolean isLeBrHost() {
        return leBrHost;
    }

    public void setLeBrHost(boolean leBrHost) {
        this.leBrHost = leBrHost;
    }

    public boolean isLeBrController() {
        return leBrController;
    }

    public void setLeBrController(boolean leBrController) {
        this.leBrController = leBrController;
    }

    public boolean isBrEdrSupported() {
        return brEdrSupported;
    }

    public void setBrEdrSupported(boolean brEdrSupported) {
        this.brEdrSupported = brEdrSupported;
    }

    public boolean isLeGeneral() {
        return leGeneral;
    }

    public void setLeGeneral(boolean leGeneral) {
        this.leGeneral = leGeneral;
    }

    public boolean isLeLimited() {
        return leLimited;
    }

    public void setLeLimited(boolean leLimited) {
        this.leLimited = leLimited;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

}
