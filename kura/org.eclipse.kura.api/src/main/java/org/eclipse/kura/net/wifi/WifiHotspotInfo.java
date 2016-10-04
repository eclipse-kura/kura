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

import java.util.EnumSet;

public class WifiHotspotInfo {

    private final String m_ssid;
    private final String m_macAddress;
    private final int m_signalLevel;
    private final int m_channel;
    private final int m_frequency;
    private final WifiSecurity m_security;
    private EnumSet<WifiSecurity> m_pairCiphers;
    private EnumSet<WifiSecurity> m_groupCiphers;

    public WifiHotspotInfo(String ssid, String macAddress, int signalLevel, int channel, int frequency,
            WifiSecurity security) {
        super();
        this.m_ssid = ssid;
        this.m_macAddress = macAddress;
        this.m_signalLevel = signalLevel;
        this.m_channel = channel;
        this.m_frequency = frequency;
        this.m_security = security;
    }

    public WifiHotspotInfo(String ssid, String macAddress, int signalLevel, int channel, int frequency,
            WifiSecurity security, EnumSet<WifiSecurity> pairCiphers, EnumSet<WifiSecurity> groupCiphers) {
        this(ssid, macAddress, signalLevel, channel, frequency, security);
        this.m_pairCiphers = pairCiphers;
        this.m_groupCiphers = groupCiphers;
    }

    public String getSsid() {
        return this.m_ssid;
    }

    public String getMacAddress() {
        return this.m_macAddress;
    }

    public int getSignalLevel() {
        return this.m_signalLevel;
    }

    public int getChannel() {
        return this.m_channel;
    }

    public int getFrequency() {
        return this.m_frequency;
    }

    public WifiSecurity getSecurity() {
        return this.m_security;
    }

    public EnumSet<WifiSecurity> getPairCiphers() {
        return this.m_pairCiphers;
    }

    public EnumSet<WifiSecurity> getGroupCiphers() {
        return this.m_groupCiphers;
    }

    @Override
    public String toString() {

        StringBuffer sb = new StringBuffer();
        sb.append(this.m_macAddress);
        sb.append(" :: ");
        sb.append(this.m_ssid);
        sb.append(" :: ");
        sb.append(this.m_signalLevel);
        sb.append(" :: ");
        sb.append(this.m_channel);
        sb.append(" :: ");
        sb.append(this.m_frequency);
        sb.append(" :: ");
        sb.append(this.m_security);

        return sb.toString();
    }
}
