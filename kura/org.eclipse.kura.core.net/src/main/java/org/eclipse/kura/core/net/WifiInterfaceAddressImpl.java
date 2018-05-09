/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.net;

import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiInterfaceAddress;
import org.eclipse.kura.net.wifi.WifiMode;

public class WifiInterfaceAddressImpl extends NetInterfaceAddressImpl implements WifiInterfaceAddress {

    private WifiMode mode;
    private long bitrate;
    private WifiAccessPoint wifiAccessPoint;

    public WifiInterfaceAddressImpl() {
        super();
    }

    public WifiInterfaceAddressImpl(WifiInterfaceAddress other) {
        super(other);
        this.mode = other.getMode();
        this.bitrate = other.getBitrate();
        this.wifiAccessPoint = other.getWifiAccessPoint();
    }

    @Override
    public WifiMode getMode() {
        return this.mode;
    }

    public void setMode(WifiMode mode) {
        this.mode = mode;
    }

    @Override
    public long getBitrate() {
        return this.bitrate;
    }

    public void setBitrate(long bitrate) {
        this.bitrate = bitrate;
    }

    @Override
    public WifiAccessPoint getWifiAccessPoint() {
        return this.wifiAccessPoint;
    }

    public void setWifiAccessPoint(WifiAccessPoint wifiAccessPoint) {
        this.wifiAccessPoint = wifiAccessPoint;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (int) (this.bitrate ^ this.bitrate >>> 32);
        result = prime * result + (this.mode == null ? 0 : this.mode.hashCode());
        result = prime * result + (this.wifiAccessPoint == null ? 0 : this.wifiAccessPoint.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        if (!(obj instanceof WifiInterfaceAddress)) {
            return false;
        }

        WifiInterfaceAddress other = (WifiInterfaceAddress) obj;

        return compare(this.mode, other.getMode()) && compare(this.bitrate, other.getBitrate())
                && compare(this.wifiAccessPoint, other.getWifiAccessPoint());
    }
}
