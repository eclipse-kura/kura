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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.core.util.NetUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddress;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkInterfaceType;
import org.eclipse.kura.net.status.NetworkStatusService;
import org.eclipse.kura.net.status.modem.ModemInterfaceStatus;
import org.eclipse.kura.net.status.modem.Sim;
import org.eclipse.kura.net.status.wifi.WifiAccessPoint;
import org.eclipse.kura.net.status.wifi.WifiChannel;
import org.eclipse.kura.net.status.wifi.WifiInterfaceStatus;
import org.eclipse.kura.net.status.wifi.WifiMode;
import org.eclipse.kura.net.status.wifi.WifiSecurity;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiChannelFrequency;
import org.eclipse.kura.web.shared.model.GwtWifiCiphers;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiHotspotEntry;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter to convert status-related properties to a
 * {@link GwtNetInterfaceConfig} object.
 *
 */
public class NetworkStatusServiceAdapter {

    private final static String EMPTY_STRING = "";

    private final Logger logger = LoggerFactory.getLogger(NetworkStatusServiceAdapter.class);
    private final NetworkStatusService networkStatusService;

    public NetworkStatusServiceAdapter() throws GwtKuraException {
        this.networkStatusService = ServiceLocator.getInstance().getService(NetworkStatusService.class);
    }

    public List<String> getNetInterfaces() throws KuraException {
        return this.networkStatusService.getInterfaceIds();
    }

    public void fillWithStatusProperties(String ifName, GwtNetInterfaceConfig gwtConfigToUpdate) throws KuraException {
        Optional<NetworkInterfaceStatus> networkInterfaceInfo = this.networkStatusService.getNetworkStatus(ifName);

        if (networkInterfaceInfo.isPresent()) {
            setCommonStateProperties(gwtConfigToUpdate, networkInterfaceInfo.get());
            setIpv4DhcpClientProperties(gwtConfigToUpdate, networkInterfaceInfo.get());
            setIpv6StatusProperties(gwtConfigToUpdate, networkInterfaceInfo.get());
            setWifiStateProperties(gwtConfigToUpdate, networkInterfaceInfo.get());
            setModemStateProperties(gwtConfigToUpdate, networkInterfaceInfo.get());
        }

    }

    public Optional<NetworkInterfaceType> getNetInterfaceType(String ifName) throws KuraException {
        Optional<NetworkInterfaceStatus> networkInterfaceInfo = this.networkStatusService.getNetworkStatus(ifName);
        Optional<NetworkInterfaceType> ifType = Optional.empty();
        if (networkInterfaceInfo.isPresent()) {
            ifType = Optional.of(networkInterfaceInfo.get().getType());
        }
        return ifType;
    }

    public List<GwtWifiChannelFrequency> getAllSupportedChannels(String ifname) throws KuraException {
        Optional<NetworkInterfaceStatus> netInterface = this.networkStatusService.getNetworkStatus(ifname);
        if (!netInterface.isPresent() || !(netInterface.get() instanceof WifiInterfaceStatus)) {
            return new ArrayList<>();
        }

        WifiInterfaceStatus wifiInterfaceInfo = (WifiInterfaceStatus) netInterface.get();
        List<WifiChannel> channels = wifiInterfaceInfo.getChannels();

        List<GwtWifiChannelFrequency> gwtChannels = new ArrayList<>();

        for (WifiChannel channel : channels) {
            GwtWifiChannelFrequency gwtChannel = new GwtWifiChannelFrequency(channel.getChannel(),
                    channel.getFrequency());

            Optional<Boolean> isDisabled = channel.getDisabled();
            Optional<Boolean> isNoInitiatingRadiation = channel.getNoInitiatingRadiation();
            Optional<Boolean> isRadarDetection = channel.getRadarDetection();

            if (isDisabled.isPresent()) {
                gwtChannel.setDisabled(isDisabled.get().booleanValue());
            }

            if (isNoInitiatingRadiation.isPresent()) {
                gwtChannel.setNoIrradiation(isNoInitiatingRadiation.get().booleanValue());
            }

            if (isRadarDetection.isPresent()) {
                gwtChannel.setRadarDetection(isRadarDetection.get().booleanValue());
            }

            gwtChannels.add(gwtChannel);
        }

        return gwtChannels;
    }

    public String getWifiCountryCode() throws KuraException {
        List<String> interfaceNames = this.networkStatusService.getInterfaceIds();
        for (String interfaceName : interfaceNames) {
            try {
                Optional<NetworkInterfaceStatus> ifaceStatus = this.networkStatusService
                        .getNetworkStatus(interfaceName);
                if (ifaceStatus.isPresent() && ifaceStatus.get() instanceof WifiInterfaceStatus) {
                    return ((WifiInterfaceStatus) ifaceStatus.get()).getCountryCode();
                }
            } catch (KuraException e) {
                this.logger.error("Cannot retrieve wifi country code", e);
            }
        }

        return "";
    }

    public List<GwtWifiHotspotEntry> findWifiHotspots(String interfaceName, boolean recompute) throws KuraException {
        List<GwtWifiHotspotEntry> result = new ArrayList<>();

        Optional<NetworkInterfaceStatus> ifStatus = this.networkStatusService.getNetworkStatus(interfaceName,
                recompute);

        if (ifStatus.isPresent() && ifStatus.get() instanceof WifiInterfaceStatus) {
            WifiInterfaceStatus wifiStatus = (WifiInterfaceStatus) ifStatus.get();

            for (WifiAccessPoint ap : wifiStatus.getAvailableWifiAccessPoints()) {
                GwtWifiHotspotEntry entry = new GwtWifiHotspotEntry();
                entry.setChannel(ap.getChannel().getChannel());
                entry.setFrequency(ap.getChannel().getFrequency());
                entry.setMacAddress(NetworkUtil.macToString(ap.getHardwareAddress()));
                entry.setSSID(ap.getSsid());
                entry.setsignalStrength(ap.getSignalStrength());

                // because of GwtWifiHotspotEntry interface does not discriminate RSN/WPA,
                // order here is important:
                // we assume that WPA security has priority over RSN one
                entry.setGroupCiphers(GwtWifiCiphers.netWifiCiphers_NONE.name());
                entry.setPairwiseCiphers(GwtWifiCiphers.netWifiCiphers_NONE.name());
                entry.setSecurity(GwtWifiSecurity.netWifiSecurityNONE.name());
                parseAndSetWifiSecurity(entry, ap.getRsnSecurity());
                parseAndSetWifiSecurity(entry, ap.getWpaSecurity());

                result.add(entry);
            }
        }

        return result;
    }

    @SuppressWarnings("restriction")
    private void setCommonStateProperties(GwtNetInterfaceConfig gwtConfig,
            NetworkInterfaceStatus networkInterfaceStatus) {

        gwtConfig.setName(networkInterfaceStatus.getInterfaceId());
        gwtConfig.setInterfaceName(networkInterfaceStatus.getInterfaceName());
        gwtConfig.setHwState(networkInterfaceStatus.getState().name());
        gwtConfig.setHwType(networkInterfaceStatus.getType().name());
        gwtConfig.setHwAddress(NetUtil.hardwareAddressToString(networkInterfaceStatus.getHardwareAddress()));
        gwtConfig.setHwName(networkInterfaceStatus.getInterfaceId());
        gwtConfig.setHwDriver(networkInterfaceStatus.getDriver());
        gwtConfig.setHwDriverVersion(networkInterfaceStatus.getDriverVersion());
        gwtConfig.setHwFirmware(networkInterfaceStatus.getFirmwareVersion());
        gwtConfig.setHwMTU(networkInterfaceStatus.getMtu());
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

                String gatewayAddress = address.getGateway().isPresent() ? address.getGateway().get().getHostAddress()
                        : EMPTY_STRING;
                gwtConfig.setGateway(gatewayAddress);

                gwtConfig.setReadOnlyDnsServers(prettyPrintDnsServers(address.getDnsServerAddresses()));
            });
        }
    }

    private boolean isDhcpClient(String ipConfigMode) {
        return ipConfigMode != null && ipConfigMode.equals(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());
    }

    private void setIpv6StatusProperties(GwtNetInterfaceConfig gwtConfig, NetworkInterfaceStatus networkInterfaceInfo) {

        String ipConfigMode = gwtConfig.getIpv6ConfigMode();
        if (isIpv6AutoConfig(ipConfigMode)) {
            /*
             * An interface can have multiple active addresses, we select just the first
             * one. This is a limit of the current GWT UI.
             */
            networkInterfaceInfo.getInterfaceIp6Addresses().ifPresent(address -> {
                if (!address.getAddresses().isEmpty()) {
                    NetworkInterfaceIpAddress<IP6Address> firstAddress = address.getAddresses().get(0);
                    gwtConfig.setIpv6Address(firstAddress.getAddress().getHostAddress());
                    gwtConfig.setIpv6SubnetMask((int) firstAddress.getPrefix());
                }

                String gatewayAddress = address.getGateway().isPresent() ? address.getGateway().get().getHostAddress()
                        : EMPTY_STRING;
                gwtConfig.setIpv6Gateway(gatewayAddress);

                gwtConfig.setIpv6ReadOnlyDnsServers(prettyPrintDnsServers(address.getDnsServerAddresses()));
            });
        }
    }

    private boolean isIpv6AutoConfig(String ipConfigMode) {
        return ipConfigMode != null
                && (ipConfigMode.equals("netIPv6MethodAuto") || ipConfigMode.equals("netIPv6MethodDhcp"));
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
            Sim activeSim = null;

            List<Sim> availableSims = modemInterfaceInfo.getAvailableSims();
            for (Sim sim : availableSims) {
                if (sim.isActive() && sim.isPrimary()) {
                    activeSim = sim;
                }
            }

            gwtModemConfig.setHwState(modemInterfaceInfo.getConnectionStatus().name());
            gwtModemConfig.setHwSerial(modemInterfaceInfo.getSerialNumber());
            gwtModemConfig.setHwRssi(String.valueOf(modemInterfaceInfo.getSignalStrength()));
            gwtModemConfig.setHwICCID(activeSim != null ? activeSim.getIccid() : "NA");
            gwtModemConfig.setHwIMSI(activeSim != null ? activeSim.getImsi() : "NA");
            gwtModemConfig.setHwRegistration(modemInterfaceInfo.getRegistrationStatus().toString());
            gwtModemConfig.setHwNetwork(modemInterfaceInfo.getOperatorName());
            gwtModemConfig.setHwRadio(getModemAccessTechnologies(modemInterfaceInfo));
            gwtModemConfig.setHwBand(getModemBands(modemInterfaceInfo));
            gwtModemConfig.setModel(ellipsis(modemInterfaceInfo.getModel(), 40));
            gwtModemConfig.setManufacturer(ellipsis(modemInterfaceInfo.getManufacturer(), 20));
            gwtModemConfig.setModemId(modemInterfaceInfo.getModel());
            gwtModemConfig.setGpsSupported(modemInterfaceInfo.isGpsSupported());
            gwtModemConfig.setHwFirmware(modemInterfaceInfo.getFirmwareVersion());
            gwtModemConfig.setConnectionType(modemInterfaceInfo.getConnectionType().toString());
            gwtModemConfig.setNetworkTechnology(
                    modemInterfaceInfo.getAccessTechnologies().stream().map(Enum::name).collect(Collectors.toList()));
            gwtModemConfig.setHwRssi(String.valueOf(modemInterfaceInfo.getSignalStrength()));

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

    private String ellipsis(String text, int length) {
        if (text.length() <= length) {
            return text;
        } else {
            return text.substring(0, length) + "...";
        }
    }

    private void setWifiStateProperties(GwtNetInterfaceConfig gwtNetInterfaceConfig,
            NetworkInterfaceStatus networkInterfaceInfo) {
        if (gwtNetInterfaceConfig instanceof GwtWifiNetInterfaceConfig
                && networkInterfaceInfo instanceof WifiInterfaceStatus) {
            GwtWifiNetInterfaceConfig gwtWifiNetInterfaceConfig = (GwtWifiNetInterfaceConfig) gwtNetInterfaceConfig;
            WifiInterfaceStatus wifiInterfaceInfo = (WifiInterfaceStatus) networkInterfaceInfo;
            gwtWifiNetInterfaceConfig.setHwState(wifiInterfaceInfo.getState().toString());

            if (wifiInterfaceInfo.getMode() == WifiMode.MASTER) {
                setWifiMasterStateProperties(gwtWifiNetInterfaceConfig, wifiInterfaceInfo);
            } else if (wifiInterfaceInfo.getMode() == WifiMode.INFRA) {
                setWifiInfraStateProperties(gwtWifiNetInterfaceConfig, wifiInterfaceInfo);
            }
        }
    }

    private void setWifiMasterStateProperties(GwtWifiNetInterfaceConfig gwtWifiNetInterfaceConfig,
            WifiInterfaceStatus wifiInterfaceInfo) {
        GwtWifiConfig gwtConfig;
        if (Objects.nonNull(gwtWifiNetInterfaceConfig.getAccessPointWifiConfig())) {
            gwtConfig = gwtWifiNetInterfaceConfig.getAccessPointWifiConfig();
        } else {
            gwtConfig = gwtWifiNetInterfaceConfig.getActiveWifiConfig();
            gwtWifiNetInterfaceConfig.setAccessPointWifiConfig(gwtConfig);
        }

        ChannelsBuilder channelsBuilder = new ChannelsBuilder();
        Optional<WifiAccessPoint> activeAP = wifiInterfaceInfo.getActiveWifiAccessPoint();
        if (activeAP.isPresent()) {
            channelsBuilder.setActiveChannel(activeAP.get().getChannel());
        } else {
            List<Integer> activeChannel = gwtConfig.getChannels();
            if (activeChannel != null && activeChannel.size() == 1) {
                channelsBuilder.setActiveChannel(activeChannel.get(0));
            }
        }
        channelsBuilder.addChannels(wifiInterfaceInfo.getChannels());
        gwtConfig.setChannels(channelsBuilder.getChannelsIntegers());
    }

    private void setWifiInfraStateProperties(GwtWifiNetInterfaceConfig gwtWifiNetInterfaceConfig,
            WifiInterfaceStatus wifiInterfaceInfo) {
        AtomicReference<String> rssi = new AtomicReference<>("N/A");
        wifiInterfaceInfo.getActiveWifiAccessPoint()
                .ifPresent(accessPoint -> rssi.set(String.valueOf(accessPoint.getSignalStrength())));
        gwtWifiNetInterfaceConfig.setHwRssi(rssi.get());

        GwtWifiConfig gwtConfig;

        if (Objects.nonNull(gwtWifiNetInterfaceConfig.getStationWifiConfig())) {
            gwtConfig = gwtWifiNetInterfaceConfig.getStationWifiConfig();
        } else {
            gwtConfig = gwtWifiNetInterfaceConfig.getActiveWifiConfig();
            gwtWifiNetInterfaceConfig.setStationWifiConfig(gwtConfig);
        }

        ChannelsBuilder channelsBuilder = new ChannelsBuilder();
        List<Integer> activeChannel = gwtConfig.getChannels();
        if (activeChannel != null && activeChannel.size() == 1) {
            channelsBuilder.setActiveChannel(activeChannel.get(0));
        }
        channelsBuilder.addChannels(wifiInterfaceInfo.getChannels());

        gwtConfig.setChannels(channelsBuilder.getChannelsIntegers());
    }

    private void parseAndSetWifiSecurity(GwtWifiHotspotEntry entryToModify, Set<WifiSecurity> supportedSecurity) {
        boolean isGroupCCMP = supportedSecurity.contains(WifiSecurity.GROUP_CCMP);
        boolean isGroupTKIP = supportedSecurity.contains(WifiSecurity.GROUP_TKIP);
        boolean isPairCCMP = supportedSecurity.contains(WifiSecurity.PAIR_CCMP);
        boolean isPairTKIP = supportedSecurity.contains(WifiSecurity.PAIR_TKIP);

        if (isGroupCCMP && isGroupTKIP) {
            entryToModify.setGroupCiphers(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name());
        } else if (isGroupCCMP) {
            entryToModify.setGroupCiphers(GwtWifiCiphers.netWifiCiphers_CCMP.name());
        } else if (isGroupTKIP) {
            entryToModify.setGroupCiphers(GwtWifiCiphers.netWifiCiphers_TKIP.name());
        }

        if (isPairCCMP && isPairTKIP) {
            entryToModify.setPairwiseCiphers(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name());
        } else if (isPairCCMP) {
            entryToModify.setPairwiseCiphers(GwtWifiCiphers.netWifiCiphers_CCMP.name());
        } else if (isPairTKIP) {
            entryToModify.setPairwiseCiphers(GwtWifiCiphers.netWifiCiphers_TKIP.name());
        }

        entryToModify.setSecurity(wifiSecurityCollectionToString(supportedSecurity));
    }

    private String wifiSecurityCollectionToString(Set<WifiSecurity> wifiSecurities) {
        StringBuilder prettyPrint = new StringBuilder();

        for (WifiSecurity wifiSecurity : wifiSecurities) {
            String secString = wifiSecurity.name();
            if (secString.contains("KEY_MGMT")) {
                prettyPrint.append(secString.replace("KEY_MGMT_", ""));
                prettyPrint.append(", ");
            }
        }

        if (prettyPrint.length() > 2) {
            prettyPrint.deleteCharAt(prettyPrint.length() - 1);
            prettyPrint.deleteCharAt(prettyPrint.length() - 1);
        }

        return prettyPrint.toString();
    }

    private class ChannelsBuilder {

        List<WifiChannel> channels;

        public ChannelsBuilder() {
            this.channels = new LinkedList<>();
        }

        public ChannelsBuilder setActiveChannel(WifiChannel channel) {
            this.channels.add(0, channel);
            return this;
        }

        public ChannelsBuilder setActiveChannel(int channel) {
            this.channels.add(0, WifiChannel.builder(channel, 0).build());
            return this;
        }

        public ChannelsBuilder addChannels(List<WifiChannel> otherChannels) {
            this.channels.addAll(otherChannels);
            return this;
        }

        public List<Integer> getChannelsIntegers() {
            List<Integer> intChannels = new ArrayList<>();
            this.channels.forEach(channel -> intChannels.add(channel.getChannel()));
            return intChannels;
        }
    }

}
