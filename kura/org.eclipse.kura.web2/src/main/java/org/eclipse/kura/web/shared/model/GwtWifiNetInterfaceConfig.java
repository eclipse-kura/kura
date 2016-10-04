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
package org.eclipse.kura.web.shared.model;

import java.util.Map;

public class GwtWifiNetInterfaceConfig extends GwtNetInterfaceConfig {

    private static final long serialVersionUID = -7509149975400963837L;

    private GwtWifiConfig m_adhocWifiConfig;
    private GwtWifiConfig m_stationWifiConfig;
    private GwtWifiConfig m_accessPointWifiConfig;

    public GwtWifiNetInterfaceConfig() {
        super();

        this.m_adhocWifiConfig = new GwtWifiConfig();
        this.m_stationWifiConfig = new GwtWifiConfig();
        this.m_accessPointWifiConfig = new GwtWifiConfig();

        setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeStation.name());
    }

    public GwtWifiNetInterfaceConfig(GwtWifiConfig adhocConfig, GwtWifiConfig stationConfig,
            GwtWifiConfig accessPointConfig) {
        super();

        this.m_adhocWifiConfig = adhocConfig;
        this.m_stationWifiConfig = stationConfig;
        this.m_accessPointWifiConfig = accessPointConfig;

        setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeStation.name());
    }

    public void setAdhocWifiConfig(GwtWifiConfig adhocConfig) {
        this.m_adhocWifiConfig = adhocConfig;
    }

    public void setAdhocWifiConfig(Map<String, Object> properties) {
        this.m_adhocWifiConfig = new GwtWifiConfig();
        this.m_adhocWifiConfig.setProperties(properties);
    }

    public GwtWifiConfig getAdhocWifiConfig() {
        return this.m_adhocWifiConfig;
    }

    public Map<String, Object> getAdhocWifiConfigProps() {
        return this.m_adhocWifiConfig.getProperties();
    }

    public void setStationWifiConfig(GwtWifiConfig stationConfig) {
        this.m_stationWifiConfig = stationConfig;
    }

    public void setStationWifiConfig(Map<String, Object> properties) {
        this.m_stationWifiConfig = new GwtWifiConfig();
        this.m_stationWifiConfig.setProperties(properties);
    }

    public GwtWifiConfig getStationWifiConfig() {
        return this.m_stationWifiConfig;
    }

    public Map<String, Object> getStationWifiConfigProps() {

        return this.m_stationWifiConfig.getProperties();
    }

    public void setAccessPointWifiConfig(GwtWifiConfig accessPointConfig) {
        this.m_accessPointWifiConfig = accessPointConfig;
    }

    public void setAccessPointWifiConfig(Map<String, Object> properties) {
        this.m_accessPointWifiConfig = new GwtWifiConfig();
        this.m_accessPointWifiConfig.setProperties(properties);
    }

    public GwtWifiConfig getAccessPointWifiConfig() {
        return this.m_accessPointWifiConfig;
    }

    public Map<String, Object> getAccessPointWifiConfigProps() {

        return this.m_accessPointWifiConfig.getProperties();
    }

    public void setWifiConfig(GwtWifiConfig wifiConfig) {
        GwtWifiWirelessMode wifiMode = wifiConfig.getWirelessModeEnum();

        if (wifiMode.equals(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint)) {
            setAccessPointWifiConfig(wifiConfig);
        } else if (wifiMode.equals(GwtWifiWirelessMode.netWifiWirelessModeAdHoc)) {
            setAdhocWifiConfig(wifiConfig);
        } else if (wifiMode.equals(GwtWifiWirelessMode.netWifiWirelessModeStation)) {
            setStationWifiConfig(wifiConfig);
        }
    }

    public GwtWifiConfig getActiveWifiConfig() {
        GwtWifiWirelessMode wifiMode = getWirelessModeEnum();
        GwtWifiConfig activeConfig = null;

        if (wifiMode.equals(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint)) {
            activeConfig = this.m_accessPointWifiConfig;
        } else if (wifiMode.equals(GwtWifiWirelessMode.netWifiWirelessModeAdHoc)) {
            activeConfig = this.m_adhocWifiConfig;
        } else if (wifiMode.equals(GwtWifiWirelessMode.netWifiWirelessModeStation)) {
            activeConfig = this.m_stationWifiConfig;
        }

        return activeConfig;
    }

    public void setWirelessMode(String wirelessMode) {
        set("wirelessMode", wirelessMode);
    }

    public String getWirelessMode() {
        return get("wirelessMode");
    }

    public GwtWifiWirelessMode getWirelessModeEnum() {
        return GwtWifiWirelessMode.valueOf(getWirelessMode());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GwtWifiNetInterfaceConfig)) {
            return false;
        }

        if (super.equals(o) == false) {
            return false;
        }

        GwtWifiNetInterfaceConfig other = (GwtWifiNetInterfaceConfig) o;

        if (compare(getActiveWifiConfig(), other.getActiveWifiConfig()) == false) {
            return false;
        }

        return true;
    }

    private boolean compare(Object obj1, Object obj2) {
        if (obj1 == null) {
            if (obj2 != null) {
                return false;
            }
        } else {
            return obj1.equals(obj2);
        }

        return true;
    }
}
