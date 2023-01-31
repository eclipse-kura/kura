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

import java.util.Arrays;

import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter to convert status-related properties to a
 * {@link GwtNetInterfaceConfig} object.
 *
 */
public class NetworkStatusServiceAdapter {

    private static final String NA = "N/A";
    private static final Logger logger = LoggerFactory.getLogger(NetworkStatusServiceAdapter.class);
    private GwtNetInterfaceConfig gwtConfig;
    private String ifname;

    public NetworkStatusServiceAdapter() {
        // TODO init status services here
    }

    public GwtNetInterfaceConfig fillWithStatusProperties(String ifname, GwtNetInterfaceConfig gwtConfigToUpdate) {
        this.gwtConfig = gwtConfigToUpdate;
        this.ifname = ifname;

        setCommonStateProperties();
        setIpv4DhcpClientProperties();
        setModemStateProperties();

        return gwtConfig;
    }

    private void setCommonStateProperties() {
        this.gwtConfig.setHwState(NA);
        this.gwtConfig.setHwAddress(NA); // MAC address
        this.gwtConfig.setHwDriver(NA);
        this.gwtConfig.setHwDriverVersion(NA);
        this.gwtConfig.setHwFirmware(NA);
        this.gwtConfig.setHwMTU(99);
        this.gwtConfig.setHwUsbDevice(NA);
        this.gwtConfig.setHwSerial(NA);
        this.gwtConfig.setHwRssi(NA);
    }

    private void setIpv4DhcpClientProperties() {
        String ipConfigMode = gwtConfig.getConfigMode();

        if (isDhcpClient(ipConfigMode)) {
            // fetch ip address, mask, gateway, dns
            this.gwtConfig.setIpAddress("192.168.2.10");
            this.gwtConfig.setGateway("192.168.2.1");
            this.gwtConfig.setSubnetMask("255.255.255.0");
            this.gwtConfig.setReadOnlyDnsServers("8.8.8.8");
        }
    }

    private boolean isDhcpClient(String ipConfigMode) {
        return ipConfigMode != null && ipConfigMode.equals(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());
    }

    private void setModemStateProperties() {
        if (this.gwtConfig instanceof GwtModemInterfaceConfig) {
            GwtModemInterfaceConfig gwtModemConfig = (GwtModemInterfaceConfig) this.gwtConfig;

            gwtModemConfig.setHwSerial(NA); // imei
            gwtModemConfig.setHwRssi(NA); // Integer.toString(rssi)
            gwtModemConfig.setHwICCID(NA);
            gwtModemConfig.setHwIMSI(NA);
            gwtModemConfig.setHwRegistration(NA);
            gwtModemConfig.setHwPLMNID(NA);
            gwtModemConfig.setHwNetwork(NA);
            gwtModemConfig.setHwRadio(NA);
            gwtModemConfig.setHwBand(NA);
            gwtModemConfig.setHwLAC(NA);
            gwtModemConfig.setHwCI(NA);
            gwtModemConfig.setGpsSupported(false);
            gwtModemConfig.setHwFirmware(NA); // firmware version
            gwtModemConfig.setConnectionType("PPP"); // PPP or DirectIP
            gwtModemConfig.setNetworkTechnology(Arrays.asList(NA)); // HSPDA/EVMO/...

            // this is a duplication because the GwtModemInterfaceConfig is poorly designed
            gwtModemConfig.setIpAddress("10.10.10.10");
            gwtModemConfig.setSubnetMask("255.255.255.0");
            gwtModemConfig.setGateway("10.10.10.1");
        }
    }

}
