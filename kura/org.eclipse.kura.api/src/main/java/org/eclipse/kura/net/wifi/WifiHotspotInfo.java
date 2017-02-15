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
package org.eclipse.kura.net.wifi;

import java.util.Set;

public class WifiHotspotInfo {

    private final String ssid;
    private final String macAddress;
    private final int signalLevel;
    private final int channel;
    private final int frequency;
    private final WifiSecurity security;
    private Set<WifiSecurity> pairCiphers;
    private Set<WifiSecurity> groupCiphers;

    public WifiHotspotInfo(String ssid, String macAddress, int signalLevel, int channel, int frequency,
            WifiSecurity security) {
        super();
        this.ssid = ssid;
        this.macAddress = macAddress;
        this.signalLevel = signalLevel;
        this.channel = channel;
        this.frequency = frequency;
        this.security = security;
    }

    public WifiHotspotInfo(String ssid, String macAddress, int signalLevel, int channel, int frequency,
            WifiSecurity security, Set<WifiSecurity> pairCiphers, Set<WifiSecurity> groupCiphers) {
        this(ssid, macAddress, signalLevel, channel, frequency, security);
        this.pairCiphers = pairCiphers;
        this.groupCiphers = groupCiphers;
    }

    public String getSsid() {
        return this.ssid;
    }

    public String getMacAddress() {
        return this.macAddress;
    }

    public int getSignalLevel() {
        return this.signalLevel;
    }

    public int getChannel() {
        return this.channel;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public WifiSecurity getSecurity() {
        return this.security;
    }

    public Set<WifiSecurity> getPairCiphers() {
        return this.pairCiphers;
    }

    public Set<WifiSecurity> getGroupCiphers() {
        return this.groupCiphers;
    }

    public void setPairCiphers(Set<WifiSecurity> pairCiphers) {
        this.pairCiphers = pairCiphers;
    }

    public void setGroupCiphers(Set<WifiSecurity> groupCiphers) {
        this.groupCiphers = groupCiphers;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(this.macAddress);
        sb.append(" :: ");
        sb.append(this.ssid);
        sb.append(" :: ");
        sb.append(this.signalLevel);
        sb.append(" :: ");
        sb.append(this.channel);
        sb.append(" :: ");
        sb.append(this.frequency);
        sb.append(" :: ");
        sb.append(this.security);

        return sb.toString();
    }
}
