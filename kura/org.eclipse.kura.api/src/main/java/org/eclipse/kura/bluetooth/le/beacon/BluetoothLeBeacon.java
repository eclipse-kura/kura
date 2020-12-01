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
        return this.leBrHost;
    }

    public void setLeBrHost(boolean leBrHost) {
        this.leBrHost = leBrHost;
    }

    public boolean isLeBrController() {
        return this.leBrController;
    }

    public void setLeBrController(boolean leBrController) {
        this.leBrController = leBrController;
    }

    public boolean isBrEdrSupported() {
        return this.brEdrSupported;
    }

    public void setBrEdrSupported(boolean brEdrSupported) {
        this.brEdrSupported = brEdrSupported;
    }

    public boolean isLeGeneral() {
        return this.leGeneral;
    }

    public void setLeGeneral(boolean leGeneral) {
        this.leGeneral = leGeneral;
    }

    public boolean isLeLimited() {
        return this.leLimited;
    }

    public void setLeLimited(boolean leLimited) {
        this.leLimited = leLimited;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getRssi() {
        return this.rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

}
