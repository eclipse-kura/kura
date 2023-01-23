/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server.net2.status;

import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;

public class NetworkStatusServiceAdapter {

    private static final String NA = "N/A";

    public NetworkStatusServiceAdapter() {
        // TODO init status services here
    }

    public GwtNetInterfaceConfig getGwtNetInterfaceConfig(String ifname) {
        GwtNetInterfaceConfig gwtConfig = new GwtNetInterfaceConfig();

        setCommonStateProperties(gwtConfig, ifname);
        setModemStateProperties(gwtConfig, ifname);

        return gwtConfig;
    }

    private void setCommonStateProperties(GwtNetInterfaceConfig gwtConfig, String ifname) {
        gwtConfig.setHwState(NA);
        gwtConfig.setHwAddress(NA); // MAC address
        gwtConfig.setHwDriver(NA);
        gwtConfig.setHwDriverVersion(NA);
        gwtConfig.setHwFirmware(NA);
        gwtConfig.setHwMTU(99);
        gwtConfig.setHwUsbDevice(NA);
        gwtConfig.setHwSerial(NA);
        gwtConfig.setHwRssi(NA);
    }

    private void setModemStateProperties(GwtNetInterfaceConfig gwtConfig, String ifname) {
        if (gwtConfig instanceof GwtModemInterfaceConfig) {
            GwtModemInterfaceConfig gwtModemConfig = (GwtModemInterfaceConfig) gwtConfig;

            gwtModemConfig.setHwSerial(NA); // imei
            // gwtModemConfig.setHwRssi(Integer.toString(rssi));
            gwtModemConfig.setHwICCID(NA);
            gwtModemConfig.setHwIMSI(NA);
            gwtModemConfig.setHwRegistration(NA);
            gwtModemConfig.setHwPLMNID(NA);
            // gwtModemConfig.setHwNetwork(network);
            // gwtModemConfig.setHwRadio(radio);
            gwtModemConfig.setHwBand(NA);
            // gwtModemConfig.setHwLAC(lac);
            // gwtModemConfig.setHwCI(ci);
            gwtModemConfig.setGpsSupported(false);
            gwtModemConfig.setHwFirmware(NA); // firmware version
            gwtModemConfig.setConnectionType("PPP"); // PPP or DirectIP
            // gwtModemConfig.setNetworkTechnology(); // HSPDA/EVMO/...
        }
    }

}
