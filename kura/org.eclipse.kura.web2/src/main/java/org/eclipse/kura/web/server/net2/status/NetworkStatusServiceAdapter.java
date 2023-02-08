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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.kura.core.util.NetUtil;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.modem.ModemInterface;
import org.eclipse.kura.net.status.NetworkStatusService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
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

    private final NetworkStatusService networkStatusService;

    public NetworkStatusServiceAdapter() throws GwtKuraException {
        this.networkStatusService = ServiceLocator.getInstance().getService(NetworkStatusService.class);
    }

    public List<String> getNetInterfaces() {
        return this.networkStatusService.getInterfaceNames();
    }

    public Optional<GwtNetInterfaceConfig> fillWithStatusProperties(String ifname,
            GwtNetInterfaceConfig gwtConfigToUpdate) {

        NetInterface<? extends NetInterfaceAddress> networkInterface = this.networkStatusService
                .getNetworkStatus(ifname);

        if (networkInterface != null) {
            setCommonStateProperties(gwtConfigToUpdate, networkInterface);
            setIpv4DhcpClientProperties(gwtConfigToUpdate, networkInterface);
            // TODO: for WiFi properties we used WifiMonitorService calls
            // setWifiStateProperties(gwtConfigToUpdate, networkInterface);
            setModemStateProperties(gwtConfigToUpdate, networkInterface);

            return Optional.of(gwtConfigToUpdate);
        }

        logger.debug("No status information retrieved for interface '{}'.", ifname);

        return Optional.empty();
    }

    @SuppressWarnings("restriction")
    private void setCommonStateProperties(GwtNetInterfaceConfig gwtConfig,
            NetInterface<? extends NetInterfaceAddress> networkInterface) {

        if (networkInterface.getState() != null) {
            gwtConfig.setHwState(networkInterface.getState().name());
        }
        if (networkInterface.getType() != null) {
            gwtConfig.setHwType(networkInterface.getType().name());
        }
        gwtConfig.setHwAddress(NetUtil.hardwareAddressToString(networkInterface.getHardwareAddress()));
        gwtConfig.setHwName(networkInterface.getName());
        gwtConfig.setHwDriver(networkInterface.getDriver());
        gwtConfig.setHwDriverVersion(networkInterface.getDriverVersion());
        gwtConfig.setHwFirmware(networkInterface.getFirmwareVersion());
        gwtConfig.setHwMTU(networkInterface.getMTU());
        if (networkInterface.getUsbDevice() != null) {
            gwtConfig.setHwUsbDevice(networkInterface.getUsbDevice().getUsbDevicePath());
        }

        logger.debug("GWT common state properties for interface {}:\\n{}\\n", gwtConfig.getName(),
                gwtConfig.getProperties());
    }

    private void setIpv4DhcpClientProperties(GwtNetInterfaceConfig gwtConfig,
            NetInterface<? extends NetInterfaceAddress> networkInterface) {

        String ipConfigMode = gwtConfig.getConfigMode();
        if (isDhcpClient(ipConfigMode)) {
            /*
             * An interface can have multiple active addresses, we select just the first
             * one. This is a limit of the current GWT UI.
             */
            if (!networkInterface.getNetInterfaceAddresses().isEmpty()) {
                NetInterfaceAddress address = networkInterface.getNetInterfaceAddresses().get(0);
                gwtConfig.setIpAddress(address.getAddress().getHostAddress());
                gwtConfig.setGateway(address.getGateway().getHostAddress());
                gwtConfig.setSubnetMask(address.getNetmask().getHostAddress());
                gwtConfig.setReadOnlyDnsServers(prettyPrintDnsServers(address.getDnsServers()));
            }
        }
    }

    private boolean isDhcpClient(String ipConfigMode) {
        return ipConfigMode != null && ipConfigMode.equals(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());
    }

    private <T extends IPAddress> String prettyPrintDnsServers(List<T> dnsAddresses) {
        StringBuilder result = new StringBuilder();
        for (T dnsAddress : dnsAddresses) {
            result.append(dnsAddress.getHostAddress());
            result.append("\n");
        }

        return result.toString();
    }

    private void setModemStateProperties(GwtNetInterfaceConfig gwtConfig,
            NetInterface<? extends NetInterfaceAddress> networkInterface) {
        if (gwtConfig instanceof GwtModemInterfaceConfig && networkInterface instanceof ModemInterface<?>) {
            GwtModemInterfaceConfig gwtModemConfig = (GwtModemInterfaceConfig) gwtConfig;
            ModemInterface<?> modemInterface = (ModemInterface<?>) networkInterface;

            if (modemInterface.getSerialNumber() != null) {
                gwtModemConfig.setHwSerial(modemInterface.getSerialNumber());
            } else {
                gwtModemConfig.setHwSerial(NA);
            }
            // TODO: previously, we used the ModemService to retrieve such info
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

            gwtModemConfig.setModel(modemInterface.getModel());
            gwtModemConfig.setGpsSupported(modemInterface.isGpsSupported());
            gwtModemConfig.setHwFirmware(modemInterface.getFirmwareVersion());
            gwtModemConfig.setConnectionType(gwtModemConfig.getPdpType().name()); // PPP or DirectIP
            gwtModemConfig.setNetworkTechnology(
                    modemInterface.getTechnologyTypes().stream().map(Enum::name).collect(Collectors.toList()));

            // this is a duplication because the GwtModemInterfaceConfig is poorly designed
            gwtModemConfig.setIpAddress(gwtConfig.getIpAddress());
            gwtModemConfig.setSubnetMask(gwtConfig.getSubnetMask());
            gwtModemConfig.setGateway(gwtConfig.getGateway());
        }
    }

}
