/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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

import java.util.List;

public class GwtModemInterfaceConfig extends GwtNetInterfaceConfig {

    private static final String DIVERSITY_ENABLED_KEY = "diversityEnabled";
    private static final long serialVersionUID = -2586979604841994110L;

    public GwtModemInterfaceConfig() {
        super();
    }

    public String getManufacturer() {
        return get("manufacturer");
    }

    public void setManufacturer(String manufacturer) {
        set("manufacturer", manufacturer);
    }

    public String getModel() {
        return get("model");
    }

    public void setModel(String model) {
        set("model", model);
    }

    public int getPppNum() {
        Object pppNum = get("pppNum");
        if (pppNum != null) {
            return ((Integer) pppNum).intValue();
        } else {
            return 0;
        }
    }

    public void setPppNum(int pppNum) {
        set("pppNum", Integer.valueOf(pppNum));
    }

    public String getModemId() {
        return get("modemId");
    }

    public void setModemId(String modemId) {
        set("modemId", modemId);
    }

    public String getDialString() {
        return get("dialString");
    }

    public void setDialString(String dialString) {
        set("dialString", dialString);
    }

    public GwtModemAuthType getAuthType() {
        if (get("authType") != null) {
            return GwtModemAuthType.valueOf((String) get("authType"));
        } else {
            return null;
        }
    }

    public void setAuthType(GwtModemAuthType authType) {
        set("authType", authType.name());
    }

    public String getUsername() {
        return get("username");
    }

    public void setUsername(String username) {
        set("username", username);
    }

    public String getPassword() {
        return get("password");
    }

    public void setPassword(String password) {
        set("password", password);
    }

    public boolean isPersist() {
        if (get("persist") != null) {
            return (Boolean) get("persist");
        }
        return false;
    }

    public void setPersist(boolean persist) {
        set("persist", persist);
    }

    public int getMaxFail() {
        return ((Integer) get("maxFail")).intValue();
    }

    public void setMaxFail(int maxFail) {
        set("maxFail", Integer.valueOf(maxFail));
    }

    public int getIdle() {
        return ((Integer) get("idle")).intValue();
    }

    public void setIdle(int idle) {
        set("idle", Integer.valueOf(idle));
    }

    public String getActiveFilter() {
        return get("activeFilter");
    }

    public void setActiveFilter(String activeFilter) {
        set("activeFilter", activeFilter);
    }

    public int getLcpEchoInterval() {
        return ((Integer) get("lcpEchoInterval")).intValue();
    }

    public void setLcpEchoInterval(int lcpEchoInterval) {
        set("lcpEchoInterval", Integer.valueOf(lcpEchoInterval));
    }

    public int getLcpEchoFailure() {
        return ((Integer) get("lcpEchoFailure")).intValue();
    }

    public void setLcpEchoFailure(int lcpEchoFailure) {
        set("lcpEchoFailure", Integer.valueOf(lcpEchoFailure));
    }

    public int getProfileID() {
        if (get("profileID") != null) {
            return (Integer) get("profileID");
        } else {
            return 0;
        }
    }

    public void setProfileID(int id) {
        set("profileID", id);
    }

    public GwtModemPdpType getPdpType() {
        if (get("pdpType") != null) {
            return GwtModemPdpType.valueOf((String) get("pdpType"));
        } else {
            return null;
        }
    }

    public void setPdpType(GwtModemPdpType pdpType) {
        set("pdpType", pdpType.name());
    }

    public String getApn() {
        return get("apn");
    }

    public void setApn(String apn) {
        set("apn", apn);
    }

    public int getDataCompression() {
        if (get("dataCompression") != null) {
            return (Integer) get("dataCompression");
        } else {
            return 0;
        }
    }

    public void setDataCompression(int dataCompression) {
        set("dataCompression", dataCompression);
    }

    public int getHeaderCompression() {
        if (get("headerCompression") != null) {
            return (Integer) get("headerCompression");
        } else {
            return 0;
        }
    }

    public void setHeaderCompression(int headerCompression) {
        set("headerCompression", headerCompression);
    }

    public boolean isDataCompression() {
        return getDataCompression() == 0 ? false : true;
    }

    public boolean isHeaderCompression() {
        return getHeaderCompression() == 0 ? false : true;
    }

    public void setNetworkTechnology(List<String> networkTechnology) {
        set("networkTechnology", networkTechnology);
    }

    public List<String> getNetworkTechnology() {
        return get("networkTechnology");
    }

    public void setConnectionType(String connectionType) {
        set("connectionType", connectionType);
    }

    public String getConnectionType() {
        return get("connectionType");
    }

    public int getResetTimeout() {
        return ((Integer) get("resetTimeout")).intValue();
    }

    public void setResetTimeout(int resetTimeout) {
        set("resetTimeout", Integer.valueOf(resetTimeout));
    }

    public boolean isGpsEnabled() {
        if (get("gpsEnabled") != null) {
            return (Boolean) get("gpsEnabled");
        }
        return false;
    }

    public void setGpsEnabled(boolean gpsEnabled) {
        set("gpsEnabled", gpsEnabled);
    }

    public boolean isGpsSupported() {
        if (get("gpsSupported") != null) {
            return (Boolean) get("gpsSupported");
        }
        return false;
    }

    public void setGpsSupported(boolean gpsSupported) {
        set("gpsSupported", gpsSupported);
    }

    public boolean isDiversityEnabled() {
        if (get(DIVERSITY_ENABLED_KEY) != null) {
            return (Boolean) get(DIVERSITY_ENABLED_KEY);
        }
        return false;
    }

    public void setDiversityEnabled(boolean diversityEnabled) {
        set(DIVERSITY_ENABLED_KEY, diversityEnabled);
    }

}
