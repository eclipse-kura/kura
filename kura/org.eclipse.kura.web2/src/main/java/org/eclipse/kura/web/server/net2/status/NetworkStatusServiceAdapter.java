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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.core.util.NetUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddress;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkInterfaceType;
import org.eclipse.kura.net.status.NetworkStatusService;
import org.eclipse.kura.net.status.modem.ModemInterfaceStatus;
import org.eclipse.kura.net.status.modem.Sim;
import org.eclipse.kura.net.status.wifi.WifiChannel;
import org.eclipse.kura.net.status.wifi.WifiInterfaceStatus;
import org.eclipse.kura.net.status.wifi.WifiMode;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiChannelFrequency;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter to convert status-related properties to a
 * {@link GwtNetInterfaceConfig} object.
 *
 */
public class NetworkStatusServiceAdapter {

    private static final Logger logger = LoggerFactory.getLogger(NetworkStatusServiceAdapter.class);

    private final NetworkStatusService networkStatusService;

    public NetworkStatusServiceAdapter() throws GwtKuraException {
        this.networkStatusService = ServiceLocator.getInstance().getService(NetworkStatusService.class);
    }

    public List<String> getNetInterfaces() {
        return this.networkStatusService.getInterfaceNames();
    }

    public void fillWithStatusProperties(String ifName, GwtNetInterfaceConfig gwtConfigToUpdate) {
        Optional<NetworkInterfaceStatus> networkInterfaceInfo = this.networkStatusService.getNetworkStatus(ifName);

        if (networkInterfaceInfo.isPresent()) {
            setCommonStateProperties(gwtConfigToUpdate, networkInterfaceInfo.get());
            setIpv4DhcpClientProperties(gwtConfigToUpdate, networkInterfaceInfo.get());
            setWifiStateProperties(gwtConfigToUpdate, networkInterfaceInfo.get());
            setModemStateProperties(gwtConfigToUpdate, networkInterfaceInfo.get());
        }

    }

    public Optional<NetworkInterfaceType> getNetInterfaceType(String ifName) {
        Optional<NetworkInterfaceStatus> networkInterfaceInfo = this.networkStatusService.getNetworkStatus(ifName);
        Optional<NetworkInterfaceType> ifType = Optional.empty();
        if (networkInterfaceInfo.isPresent()) {
            ifType = Optional.of(networkInterfaceInfo.get().getType());
        }
        return ifType;
    }

    public List<GwtWifiChannelFrequency> getAllSupportedChannels(String ifname) {
        Optional<NetworkInterfaceStatus> netInterface = this.networkStatusService.getNetworkStatus(ifname);
        if (!netInterface.isPresent() || !(netInterface.get() instanceof WifiInterfaceStatus)) {
            return new ArrayList<>();
        }

        WifiInterfaceStatus wifiInterfaceInfo = (WifiInterfaceStatus) netInterface.get();
        List<Integer> wifiChannels = wifiInterfaceInfo.getSupportedChannels();
        List<Long> wifiFrequencies = wifiInterfaceInfo.getSupportedFrequencies();

        List<GwtWifiChannelFrequency> gwtChannels = new ArrayList<>();
        for (int i = 0; i < wifiChannels.size(); i++) {
            Integer channel = wifiChannels.get(i);
            Long freq = wifiFrequencies.get(i);

            gwtChannels.add(new GwtWifiChannelFrequency(channel, freq.intValue()));
        }

        return gwtChannels;
    }

    public String getWifiCountryCode() {
        List<NetworkInterfaceStatus> netInterfaces = this.networkStatusService.getNetworkStatus();

        for (NetworkInterfaceStatus ifaceStatus : netInterfaces) {
            if (ifaceStatus instanceof WifiInterfaceStatus) {
                return ((WifiInterfaceStatus) ifaceStatus).getCountryCode();
            }
        }

        return "";
    }

    @SuppressWarnings("restriction")
    private void setCommonStateProperties(GwtNetInterfaceConfig gwtConfig,
            NetworkInterfaceStatus networkInterfaceStatus) {

        gwtConfig.setHwState(networkInterfaceStatus.getState().name());
        gwtConfig.setHwType(networkInterfaceStatus.getType().name());
        gwtConfig.setHwAddress(NetUtil.hardwareAddressToString(networkInterfaceStatus.getHardwareAddress()));
        gwtConfig.setHwName(networkInterfaceStatus.getName());
        gwtConfig.setHwDriver(networkInterfaceStatus.getDriver());
        gwtConfig.setHwDriverVersion(networkInterfaceStatus.getDriverVersion());
        gwtConfig.setHwFirmware(networkInterfaceStatus.getFirmwareVersion());
        gwtConfig.setHwMTU(networkInterfaceStatus.getMtu());
        networkInterfaceStatus.getUsbNetDevice()
                .ifPresent(usbDevice -> gwtConfig.setHwUsbDevice(usbDevice.getUsbDevicePath()));

        logger.debug("GWT common state properties for interface {}:\n{}\n", gwtConfig.getName(),
                gwtConfig.getProperties());
    }

    private void setIpv4DhcpClientProperties(GwtNetInterfaceConfig gwtConfig,
            NetworkInterfaceStatus networkInterfaceInfo) {

        String ipConfigMode = gwtConfig.getConfigMode();
        if (isDhcpClient(ipConfigMode)) {
            /*
             * An interface can have multiple active addresses, we select just the first
             * one. This is a limit of the current GWT UI.
             */
            networkInterfaceInfo.getInterfaceIp4Addresses().ifPresent(address -> {
                if (!address.getAddresses().isEmpty()) {
                    NetworkInterfaceIpAddress<IP4Address> firstAddress = address.getAddresses().get(0);
                    gwtConfig.setIpAddress(firstAddress.getAddress().getHostAddress());
                    gwtConfig.setSubnetMask(NetworkUtil.getNetmaskStringForm(firstAddress.getPrefix()));
                }
                if (address.getGateway().isPresent()) {
                    gwtConfig.setGateway(address.getGateway().get().getHostAddress());
                }
                gwtConfig.setReadOnlyDnsServers(prettyPrintDnsServers(address.getDnsServerAddresses()));
            });
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

    private void setModemStateProperties(GwtNetInterfaceConfig gwtConfig, NetworkInterfaceStatus networkInterfaceInfo) {
        if (gwtConfig instanceof GwtModemInterfaceConfig && networkInterfaceInfo instanceof ModemInterfaceStatus) {
            GwtModemInterfaceConfig gwtModemConfig = (GwtModemInterfaceConfig) gwtConfig;
            ModemInterfaceStatus modemInterfaceInfo = (ModemInterfaceStatus) networkInterfaceInfo;
            int activeSimIndex = modemInterfaceInfo.getActiveSimIndex();
            Sim activeSim = modemInterfaceInfo.getAvailableSims().get(activeSimIndex);

            gwtModemConfig.setHwState(modemInterfaceInfo.getState().toString());
            gwtModemConfig.setHwSerial(modemInterfaceInfo.getSerialNumber());
            gwtModemConfig.setHwRssi(String.valueOf(modemInterfaceInfo.getRssi()));
            gwtModemConfig.setHwICCID(activeSim != null ? activeSim.getIccid() : "NA");
            gwtModemConfig.setHwIMSI(activeSim != null ? activeSim.getImsi() : "NA");
            gwtModemConfig.setHwRegistration(modemInterfaceInfo.getRegistrationStatus().toString());
            gwtModemConfig.setHwPLMNID(modemInterfaceInfo.getPlmnid());
            gwtModemConfig.setHwNetwork(modemInterfaceInfo.getOperatorName());
            gwtModemConfig.setHwRadio(getModemAccessTechnologies(modemInterfaceInfo));
            gwtModemConfig.setHwBand(getModemBands(modemInterfaceInfo));
            gwtModemConfig.setHwLAC(modemInterfaceInfo.getLac());
            gwtModemConfig.setHwCI(modemInterfaceInfo.getCi());
            gwtModemConfig.setModel(modemInterfaceInfo.getModel());
            gwtModemConfig.setGpsSupported(modemInterfaceInfo.isGpsSupported());
            gwtModemConfig.setHwFirmware(modemInterfaceInfo.getFirmwareVersion());
            gwtModemConfig.setConnectionType(modemInterfaceInfo.getConnectionType().toString());
            gwtModemConfig.setNetworkTechnology(
                    modemInterfaceInfo.getAccessTechnologies().stream().map(Enum::name).collect(Collectors.toList()));

            // this is a duplication because the GwtModemInterfaceConfig is poorly designed
            gwtModemConfig.setIpAddress(gwtConfig.getIpAddress());
            gwtModemConfig.setSubnetMask(gwtConfig.getSubnetMask());
            gwtModemConfig.setGateway(gwtConfig.getGateway());
        }
    }

    private String getModemAccessTechnologies(ModemInterfaceStatus modemInterfaceInfo) {
        StringBuilder sb = new StringBuilder();
        modemInterfaceInfo.getAccessTechnologies().forEach(accessTechnology -> sb.append(accessTechnology).append(","));
        return sb.toString().substring(0, sb.toString().length() - 1);
    }

    private String getModemBands(ModemInterfaceStatus modemInterfaceInfo) {
        StringBuilder sb = new StringBuilder();
        modemInterfaceInfo.getCurrentBands().forEach(band -> sb.append(band).append(","));
        return sb.toString().substring(0, sb.toString().length() - 1);
    }

    private void setWifiStateProperties(GwtNetInterfaceConfig gwtNetInterfaceConfig,
            NetworkInterfaceStatus networkInterfaceInfo) {
        if (gwtNetInterfaceConfig instanceof GwtWifiNetInterfaceConfig
                && networkInterfaceInfo instanceof WifiInterfaceStatus) {
            GwtWifiNetInterfaceConfig gwtWifiNetInterfaceConfig = (GwtWifiNetInterfaceConfig) gwtNetInterfaceConfig;
            WifiInterfaceStatus wifiInterfaceInfo = (WifiInterfaceStatus) networkInterfaceInfo;

            gwtWifiNetInterfaceConfig.setHwState(wifiInterfaceInfo.getState().toString());
            if (wifiInterfaceInfo.getMode() == WifiMode.MASTER) {
                if (Objects.nonNull(gwtWifiNetInterfaceConfig.getAccessPointWifiConfig())) {
                    List<WifiChannel> channels = wifiInterfaceInfo.getChannels();
                    gwtWifiNetInterfaceConfig.getAccessPointWifiConfig().setChannels(convertChannels(channels));
                } else {
                    GwtWifiConfig gwtConfig = new GwtWifiConfig();
                    List<WifiChannel> channels = wifiInterfaceInfo.getChannels();
                    gwtConfig.setChannels(convertChannels(channels));
                    gwtWifiNetInterfaceConfig.setAccessPointWifiConfig(gwtConfig);
                }
            } else if (wifiInterfaceInfo.getMode() == WifiMode.INFRA) {
                AtomicReference<String> rssi = new AtomicReference<>("N/A");
                wifiInterfaceInfo.getActiveWifiAccessPoint()
                        .ifPresent(accessPoint -> rssi.set(String.valueOf(accessPoint.getSignalQuality())));
                gwtWifiNetInterfaceConfig.setHwRssi(rssi.get());
                if (Objects.nonNull(gwtWifiNetInterfaceConfig.getStationWifiConfig())) {
                    List<WifiChannel> channels = wifiInterfaceInfo.getChannels();
                    gwtWifiNetInterfaceConfig.getStationWifiConfig().setChannels(convertChannels(channels));
                } else {
                    GwtWifiConfig gwtConfig = new GwtWifiConfig();
                    List<WifiChannel> channels = wifiInterfaceInfo.getChannels();
                    gwtConfig.setChannels(convertChannels(channels));
                    gwtWifiNetInterfaceConfig.setStationWifiConfig(gwtConfig);
                }
            }
        }
    }

    private List<Integer> convertChannels(List<WifiChannel> channels) {
        List<Integer> intChannels = new ArrayList<>();

        for (WifiChannel channel : channels) {
            intChannels.add(channel.getChannel());
        }

        return intChannels;
    }

}
