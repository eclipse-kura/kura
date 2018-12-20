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

import java.util.Arrays;
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
        if (this.hardwareAddress != null && this.hardwareAddress.length == 6) {
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.bitrate == null ? 0 : this.bitrate.hashCode());
        result = prime * result + (this.capabilities == null ? 0 : this.capabilities.hashCode());
        result = prime * result + (int) (this.frequency ^ this.frequency >>> 32);
        result = prime * result + Arrays.hashCode(this.hardwareAddress);
        result = prime * result + (this.mode == null ? 0 : this.mode.hashCode());
        result = prime * result + (this.rsnSecurity == null ? 0 : this.rsnSecurity.hashCode());
        result = prime * result + (this.ssid == null ? 0 : this.ssid.hashCode());
        result = prime * result + this.strength;
        result = prime * result + (this.wpaSecurity == null ? 0 : this.wpaSecurity.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof WifiAccessPointImpl)) {
            return false;
        }
        WifiAccessPointImpl other = (WifiAccessPointImpl) obj;
        if (this.bitrate == null) {
            if (other.bitrate != null) {
                return false;
            }
        } else if (!this.bitrate.equals(other.bitrate)) {
            return false;
        }
        if (this.capabilities == null) {
            if (other.capabilities != null) {
                return false;
            }
        } else if (!this.capabilities.equals(other.capabilities)) {
            return false;
        }
        if (this.frequency != other.frequency) {
            return false;
        }
        if (!Arrays.equals(this.hardwareAddress, other.hardwareAddress)) {
            return false;
        }
        if (this.mode != other.mode) {
            return false;
        }
        if (this.rsnSecurity == null) {
            if (other.rsnSecurity != null) {
                return false;
            }
        } else if (!this.rsnSecurity.equals(other.rsnSecurity)) {
            return false;
        }
        if (this.ssid == null) {
            if (other.ssid != null) {
                return false;
            }
        } else if (!this.ssid.equals(other.ssid)) {
            return false;
        }
        if (this.strength != other.strength) {
            return false;
        }
        if (this.wpaSecurity == null) {
            if (other.wpaSecurity != null) {
                return false;
            }
        } else if (!this.wpaSecurity.equals(other.wpaSecurity)) {
            return false;
        }
        return true;
    }
}
