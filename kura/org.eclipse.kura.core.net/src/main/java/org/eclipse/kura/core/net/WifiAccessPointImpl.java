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

import java.util.EnumSet;
import java.util.List;

import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;

public class WifiAccessPointImpl implements WifiAccessPoint {

    private final String ssid;
    private byte[] hardwareAddress;
    private long frequency;
    private WifiMode mode;
    private List<Long> bitrate;
    private int strength;
    private EnumSet<WifiSecurity> wpaSecurity;
    private EnumSet<WifiSecurity> rsnSecurity;
    private List<String> capabilities;

    public WifiAccessPointImpl(String ssid) {
        this.ssid = ssid;
    }

    @Override
    public String getSSID() {
        return this.ssid;
    }

    @Override
    public byte[] getHardwareAddress() {
        return this.hardwareAddress;
    }

    public void setHardwareAddress(byte[] hardwareAddress) {
        this.hardwareAddress = hardwareAddress;
    }

    @Override
    public long getFrequency() {
        return this.frequency;
    }

    public void setFrequency(long frequency) {
        this.frequency = frequency;
    }

    @Override
    public WifiMode getMode() {
        return this.mode;
    }

    public void setMode(WifiMode mode) {
        this.mode = mode;
    }

    @Override
    public List<Long> getBitrate() {
        return this.bitrate;
    }

    public void setBitrate(List<Long> bitrate) {
        this.bitrate = bitrate;
    }

    @Override
    public int getStrength() {
        return this.strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    @Override
    public EnumSet<WifiSecurity> getWpaSecurity() {
        return this.wpaSecurity;
    }

    public void setWpaSecurity(EnumSet<WifiSecurity> wpaSecurity) {
        this.wpaSecurity = wpaSecurity;
    }

    @Override
    public EnumSet<WifiSecurity> getRsnSecurity() {
        return this.rsnSecurity;
    }

    public void setRsnSecurity(EnumSet<WifiSecurity> rsnSecurity) {
        this.rsnSecurity = rsnSecurity;
    }

    @Override
    public List<String> getCapabilities() {
        return this.capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ssid=").append(this.ssid);
        if (this.hardwareAddress != null && this.hardwareAddress.length > 0) {
            sb.append(" :: hardwareAddress=").append(NetworkUtil.macToString(this.hardwareAddress));
        }
        sb.append(" :: frequency=").append(this.frequency).append(" :: mode=").append(this.mode);
        if (this.bitrate != null && this.bitrate.size() > 0) {
            sb.append(" :: bitrate=");
            for (Long rate : this.bitrate) {
                sb.append(rate).append(" ");
            }
        }
        sb.append(" :: strength=").append(this.strength);
        if (this.wpaSecurity != null && this.wpaSecurity.size() > 0) {
            sb.append(" :: wpaSecurity=");
            for (WifiSecurity security : this.wpaSecurity) {
                sb.append(security).append(" ");
            }
        }
        if (this.rsnSecurity != null && this.rsnSecurity.size() > 0) {
            sb.append(" :: rsnSecurity=");
            for (WifiSecurity security : this.rsnSecurity) {
                sb.append(security).append(" ");
            }
        }
        return sb.toString();
    }
}
