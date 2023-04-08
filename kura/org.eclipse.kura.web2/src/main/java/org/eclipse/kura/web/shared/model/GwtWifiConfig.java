/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.List;

public class GwtWifiConfig extends KuraBaseModel implements Serializable {

    private static final long serialVersionUID = -7610506986073264800L;

    private static final String IGNORE_SSID = "ignoreSSID";
    private static final String PING_ACCESS_POINT = "pingAccessPoint";
    private static final String BGSCAN_LONG_INTERVAL = "bgscanLongInterval";
    private static final String BGSCAN_SHORT_INTERVAL = "bgscanShortInterval";
    private static final String BGSCAN_RSSI_THRESHOLD = "bgscanRssiThreshold";
    private static final String BGSCAN_MODULE = "bgscanModule";
    private static final String PASSWORD = "password";
    private static final String GROUP_CIPHERS = "groupCiphers";
    private static final String PAIRWISE_CIPHERS = "pairwiseCiphers";
    private static final String SECURITY = "security";
    private static final String CHANNELS = "channels";
    private static final String RADIO_MODE = "radioMode";
    private static final String DRIVER = "driver";
    private static final String WIRELESS_SSID = "wirelessSsid";
    private static final String WIRELESS_MODE = "wirelessMode";
    private static final String CHANNELS_FREQUENCY = "ChannelsFrequency";
    private static final String COUNTRY_CODE = "CountryCode";

    public GwtWifiConfig() {
        setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeStation.name());
        setRadioMode(GwtWifiRadioMode.netWifiRadioModeBGN.name());
        setSecurity(GwtWifiSecurity.netWifiSecurityWPA2.name());
    }

    public String getWirelessMode() {
        return get(WIRELESS_MODE);
    }

    public void setWirelessMode(String wirelessMode) {
        set(WIRELESS_MODE, wirelessMode);
    }

    public GwtWifiWirelessMode getWirelessModeEnum() {
        return GwtWifiWirelessMode.valueOf(getWirelessMode());
    }

    public String getWirelessSsid() {
        return get(WIRELESS_SSID);
    }

    public void setWirelessSsid(String wirelessSsid) {
        set(WIRELESS_SSID, wirelessSsid);
    }

    public String getDriver() {
        return get(DRIVER);
    }

    public void setDriver(String driver) {
        set(DRIVER, driver);
    }

    public String getRadioMode() {
        return get(RADIO_MODE);
    }

    public void setRadioMode(String radioMode) {
        set(RADIO_MODE, radioMode);
    }

    public GwtWifiRadioMode getRadioModeEnum() {
        return GwtWifiRadioMode.valueOf(getRadioMode());
    }

    public List<Integer> getChannels() {
        return get(CHANNELS);
    }

    public void setChannels(List<Integer> channels) {
        set(CHANNELS, channels);
    }

    public String getSecurity() {
        return get(SECURITY);
    }

    public void setSecurity(String security) {
        set(SECURITY, security);
    }

    public GwtWifiSecurity getSecurityEnum() {
        return GwtWifiSecurity.valueOf(getSecurity());
    }

    public String getPairwiseCiphers() {
        return get(PAIRWISE_CIPHERS);
    }

    public void setPairwiseCiphers(String ciphers) {
        set(PAIRWISE_CIPHERS, ciphers);
    }

    public GwtWifiCiphers getPairwiseCiphersEnum() {
        return GwtWifiCiphers.valueOf(getPairwiseCiphers());
    }

    public String getGroupCiphers() {
        return get(GROUP_CIPHERS);
    }

    public void setGroupCiphers(String ciphers) {
        set(GROUP_CIPHERS, ciphers);
    }

    public GwtWifiCiphers getGroupCiphersEnum() {
        return GwtWifiCiphers.valueOf(getGroupCiphers());
    }

    public String getPassword() {
        String password = get(PASSWORD);
        if (password != null) {
            return password;
        }
        return "";
    }

    public void setPassword(String password) {
        set(PASSWORD, password);
    }

    public String getBgscanModule() {
        return get(BGSCAN_MODULE);
    }

    public void setBgscanModule(String bgscanModule) {
        set(BGSCAN_MODULE, bgscanModule);
    }

    public GwtWifiBgscanModule getBgscanModuleEnum() {
        return GwtWifiBgscanModule.valueOf(getBgscanModule());
    }

    public int getBgscanRssiThreshold() {
        Object bgScanRssiThreshold = get(BGSCAN_RSSI_THRESHOLD);
        if (bgScanRssiThreshold != null) {
            return (Integer) bgScanRssiThreshold;
        }
        return 0;
    }

    public void setBgscanRssiThreshold(int bgscanRssiThreshold) {
        set(BGSCAN_RSSI_THRESHOLD, bgscanRssiThreshold);
    }

    public int getBgscanShortInterval() {
        Object bgScanShortInterval = get(BGSCAN_SHORT_INTERVAL);
        if (bgScanShortInterval != null) {
            return (Integer) bgScanShortInterval;
        }
        return 0;
    }

    public void setBgscanShortInterval(int bgscanShortInterval) {
        set(BGSCAN_SHORT_INTERVAL, bgscanShortInterval);
    }

    public int getBgscanLongInterval() {
        Object bgScanLongInterval = get(BGSCAN_LONG_INTERVAL);
        if (bgScanLongInterval != null) {
            return (Integer) bgScanLongInterval;
        }
        return 0;
    }

    public void setBgscanLongInterval(int bgscanLongInterval) {
        set(BGSCAN_LONG_INTERVAL, bgscanLongInterval);
    }

    public boolean pingAccessPoint() {
        Object pingAccessPoint = get(PING_ACCESS_POINT);
        if (pingAccessPoint != null) {
            return (Boolean) pingAccessPoint;
        }
        return false;
    }

    public void setPingAccessPoint(boolean pingAccessPoint) {
        set(PING_ACCESS_POINT, pingAccessPoint);
    }

    public boolean ignoreSSID() {
        Object ignoreSsid = get(IGNORE_SSID);
        if (ignoreSsid != null) {
            return (Boolean) ignoreSsid;
        }
        return false;
    }

    public void setIgnoreSSID(boolean ignoreSSID) {
        set(IGNORE_SSID, ignoreSSID);
    }

    public List<GwtWifiHotspotEntry> getChannelsFrequency() {
        return get(CHANNELS_FREQUENCY);
    }

    public void setChannelsFrequency(List<GwtWifiHotspotEntry> list) {
        set(CHANNELS_FREQUENCY, list);
    }

    public String getCountryCode() {
        return get(COUNTRY_CODE);
    }

    public void setCountryCode(String countryCode) {
        set(COUNTRY_CODE, countryCode);
    }
}
