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
package org.eclipse.kura.web.server.net2.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.eclipse.kura.net.modem.ModemConnectionStatus;
import org.eclipse.kura.net.wifi.WifiBgscanModule;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtModemAuthType;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtModemPdpType;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetRouterMode;
import org.eclipse.kura.web.shared.model.GwtWifiBgscanModule;
import org.eclipse.kura.web.shared.model.GwtWifiCiphers;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiRadioMode;
import org.eclipse.kura.web.shared.model.GwtWifiSecurity;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter to convert {@link NetworkConfigurationService} properties to a
 * {@link GwtNetInterfaceConfig} object.
 *
 */
public class NetworkConfigurationServiceAdapter {

    private static final String NETWORK_CONFIGURATION_SERVICE_PID = "org.eclipse.kura.net.admin.NetworkConfigurationService";
    private static final String NA = "N/A";
    private static final Logger logger = LoggerFactory.getLogger(NetworkConfigurationServiceAdapter.class);

    private final NetworkConfigurationServiceProperties properties;

    public NetworkConfigurationServiceAdapter() throws GwtKuraException, KuraException {
        ConfigurationService configurationService = ServiceLocator.getInstance().getService(ConfigurationService.class);
        logger.debug("Is the ConfigurationService null? {}", configurationService == null);
        logger.debug(NETWORK_CONFIGURATION_SERVICE_PID);

        ComponentConfiguration config = configurationService
                .getComponentConfiguration(NETWORK_CONFIGURATION_SERVICE_PID);
        logger.debug("Is the config null? {}", config == null);

        this.properties = new NetworkConfigurationServiceProperties(config.getConfigurationProperties());
    }

    public List<String> getNetInterfaces() {
        return this.properties.getNetInterfaces();
    }

    public GwtNetInterfaceConfig getGwtNetInterfaceConfig(String ifname) {
        GwtNetInterfaceConfig gwtConfig = createGwtNetInterfaceConfig(ifname);

        setCommonProperties(gwtConfig, ifname);
        setIpv4Properties(gwtConfig, ifname);
        setIpv4DhcpClientProperties(gwtConfig, ifname);
        setIpv4DhcpServerProperties(gwtConfig, ifname);
        setRouterMode(gwtConfig, ifname);
        setWifiMasterProperties(gwtConfig, ifname);
        setWifiInfraProperties(gwtConfig, ifname);
        setModemProperties(gwtConfig, ifname);

        logger.debug("GWT Network Configuration for interface {}:\n{}\n", ifname, gwtConfig.getProperties());

        return gwtConfig;
    }

    private GwtNetInterfaceConfig createGwtNetInterfaceConfig(String ifname) {
        if (this.properties.getType(ifname).equals(NetInterfaceType.WIFI.name())) {
            return new GwtWifiNetInterfaceConfig();
        }

        if (this.properties.getType(ifname).equals(NetInterfaceType.MODEM.name())) {
            return new GwtModemInterfaceConfig();
        }

        return new GwtNetInterfaceConfig();
    }

    private void setCommonProperties(GwtNetInterfaceConfig gwtConfig, String ifname) {
        gwtConfig.setName(ifname);
        gwtConfig.setHwName(ifname);
        gwtConfig.setHwType(this.properties.getType(ifname));

        String wifiMode = getWifiMode(this.properties.getWifiMode(ifname));
        if (gwtConfig instanceof GwtWifiNetInterfaceConfig) {
            ((GwtWifiNetInterfaceConfig) gwtConfig).setWirelessMode(wifiMode);
        }
    }

    private void setIpv4Properties(GwtNetInterfaceConfig gwtConfig, String ifname) {
        gwtConfig.setStatus(getIp4Status(this.properties.getIp4Status(ifname)));
        gwtConfig.setIpAddress(this.properties.getIp4Address(ifname));
        gwtConfig.setSubnetMask(this.properties.getIp4Netmask(ifname));
        gwtConfig.setGateway(this.properties.getIp4Gateway(ifname));
        // TODO: maybe manipulate to insert \n like in previous networking
        gwtConfig.setReadOnlyDnsServers(this.properties.getIp4DnsServers(ifname));
        gwtConfig.setDnsServers(this.properties.getIp4DnsServers(ifname));
    }

    private void setIpv4DhcpClientProperties(GwtNetInterfaceConfig gwtConfig, String ifname) {
        if (this.properties.getDhcpClient4Enabled(ifname)) {
            gwtConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());
        } else {
            gwtConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeManual.name());
        }
    }

    private void setIpv4DhcpServerProperties(GwtNetInterfaceConfig gwtConfig, String ifname) {
        if (this.properties.getDhcpServer4Enabled(ifname)) {
            gwtConfig.setRouterDhcpBeginAddress(this.properties.getDhcpServer4RangeStart(ifname));
            gwtConfig.setRouterDhcpEndAddress(this.properties.getDhcpServer4RangeEnd(ifname));
            gwtConfig.setRouterDhcpSubnetMask(this.properties.getDhcpServer4Netmask(ifname));
            gwtConfig.setRouterDhcpDefaultLease(this.properties.getDhcpServer4LeaseTime(ifname));
            gwtConfig.setRouterDhcpMaxLease(this.properties.getDhcpServer4MaxLeaseTime(ifname));
            gwtConfig.setRouterDnsPass(this.properties.getDhcpServer4PassDns(ifname));
        }
    }

    private void setRouterMode(GwtNetInterfaceConfig gwtConfig, String ifname) {
        boolean isDhcpServer = this.properties.getDhcpServer4Enabled(ifname);
        boolean isNat = this.properties.getDhcpServer4PassDns(ifname);

        if (isDhcpServer && isNat) {
            gwtConfig.setRouterMode(GwtNetRouterMode.netRouterDchpNat.name());
        } else if (isDhcpServer) {
            gwtConfig.setRouterMode(GwtNetRouterMode.netRouterDchp.name());
        } else if (isNat) {
            gwtConfig.setRouterMode(GwtNetRouterMode.netRouterNat.name());
        } else {
            gwtConfig.setRouterMode(GwtNetRouterMode.netRouterOff.name());
        }
    }

    private void setWifiMasterProperties(GwtNetInterfaceConfig gwtConfig, String ifname) {
        String wifiMode = getWifiMode(this.properties.getWifiMode(ifname));

        if (wifiMode.equals(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name())
                && gwtConfig instanceof GwtWifiNetInterfaceConfig) {
            GwtWifiConfig gwtWifiConfig = new GwtWifiConfig();

            // common wifi properties

            gwtWifiConfig.setWirelessMode(getWifiMode(this.properties.getWifiMasterMode(ifname)));
            gwtWifiConfig.setWirelessSsid(this.properties.getWifiMasterSsid(ifname));
            gwtWifiConfig.setDriver(this.properties.getWifiMasterDriver(ifname));
            gwtWifiConfig.setSecurity(getWifiSecurityType(this.properties.getWifiMasterSecurityType(ifname)));
            gwtWifiConfig.setPairwiseCiphers(getWifiCiphers(this.properties.getWifiInfraPairwiseCiphers(ifname)));
            gwtWifiConfig.setGroupCiphers(getWifiCiphers(this.properties.getWifiMasterGroupCiphers(ifname)));
            gwtWifiConfig.setIgnoreSSID(this.properties.getWifiMasterIgnoreSsid(ifname));
            gwtWifiConfig.setPassword(new String(this.properties.getWifiMasterPassphrase(ifname).getPassword()));
            gwtWifiConfig.setChannels(getWifiChannels(this.properties.getWifiMasterChannel(ifname)));

            // wifi master specific properties

            Optional<String> radioMode = getWifiRadioMode(this.properties.getWifiMasterRadioMode(ifname));
            if (radioMode.isPresent()) {
                gwtWifiConfig.setRadioMode(radioMode.get());
            }

            gwtConfig.setHwRssi(NA);
            gwtConfig.setHwDriver(this.properties.getWifiMasterDriver(ifname));
            ((GwtWifiNetInterfaceConfig) gwtConfig).setAccessPointWifiConfig(gwtWifiConfig);
            logger.debug("GWT Wifi Master Configuration for interface {}:\n{}\n", ifname,
                    gwtWifiConfig.getProperties());
        }
    }

    private void setWifiInfraProperties(GwtNetInterfaceConfig gwtConfig, String ifname) {
        String wifiMode = getWifiMode(this.properties.getWifiInfraMode(ifname));

        if (wifiMode.equals(GwtWifiWirelessMode.netWifiWirelessModeStation.name())
                && gwtConfig instanceof GwtWifiNetInterfaceConfig) {
            GwtWifiConfig gwtWifiConfig = new GwtWifiConfig();

            // common wifi properties

            gwtWifiConfig.setWirelessMode(getWifiMode(this.properties.getWifiInfraMode(ifname)));
            gwtWifiConfig.setWirelessSsid(this.properties.getWifiInfraSsid(ifname));
            gwtWifiConfig.setDriver(this.properties.getWifiInfraDriver(ifname));
            gwtWifiConfig.setSecurity(getWifiSecurityType(this.properties.getWifiInfraSecurityType(ifname)));
            gwtWifiConfig.setPairwiseCiphers(getWifiCiphers(this.properties.getWifiMasterPairwiseCiphers(ifname)));
            gwtWifiConfig.setGroupCiphers(getWifiCiphers(this.properties.getWifiInfraGroupCiphers(ifname)));
            gwtWifiConfig.setIgnoreSSID(this.properties.getWifiInfraIgnoreSsid(ifname));
            gwtWifiConfig.setPassword(new String(this.properties.getWifiInfraPassphrase(ifname).getPassword()));
            gwtWifiConfig.setChannels(getWifiChannels(this.properties.getWifiInfraChannel(ifname)));

            // wifi infra specific properties

            setBgScanProperties(gwtWifiConfig, this.properties.getWifiInfraBgscan(ifname));
            gwtWifiConfig.setPingAccessPoint(this.properties.getWifiInfraPingAP(ifname));

            gwtConfig.setHwRssi(NA);
            gwtConfig.setHwDriver(this.properties.getWifiInfraDriver(ifname));
            ((GwtWifiNetInterfaceConfig) gwtConfig).setStationWifiConfig(gwtWifiConfig);
            logger.debug("GWT Wifi Infra Configuration for interface {}:\n{}\n", ifname, gwtWifiConfig.getProperties());
        }
    }

    private String getIp4Status(Optional<String> ip4Status) {
        if (ip4Status.isPresent()) {
            if (ip4Status.get().equals(NetInterfaceStatus.netIPv4StatusEnabledLAN.name())) {
                return GwtNetIfStatus.netIPv4StatusEnabledLAN.name();
            }

            if (ip4Status.get().equals(NetInterfaceStatus.netIPv4StatusEnabledWAN.name())) {
                return GwtNetIfStatus.netIPv4StatusEnabledWAN.name();
            }

            if (ip4Status.get().equals(NetInterfaceStatus.netIPv4StatusL2Only.name())) {
                return GwtNetIfStatus.netIPv4StatusL2Only.name();
            }

            if (ip4Status.get().equals(NetInterfaceStatus.netIPv4StatusUnmanaged.name())) {
                return GwtNetIfStatus.netIPv4StatusUnmanaged.name();
            }
        }

        return GwtNetIfStatus.netIPv4StatusDisabled.name();
    }

    private String getWifiMode(Optional<String> wifiMode) {
        if (wifiMode.isPresent()) {
            if (wifiMode.get().equals(WifiMode.MASTER.name())) {
                return GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name();
            }

            if (wifiMode.get().equals(WifiMode.INFRA.name())) {
                return GwtWifiWirelessMode.netWifiWirelessModeStation.name();
            }

            if (wifiMode.get().equals(WifiMode.ADHOC.name())) {
                return GwtWifiWirelessMode.netWifiWirelessModeAdHoc.name();
            }
        }

        return GwtWifiWirelessMode.netWifiWirelessModeDisabled.name();
    }

    private String getWifiSecurityType(Optional<String> securityType) {
        if (securityType.isPresent()) {
            if (securityType.get().equals(WifiSecurity.SECURITY_WEP.name())) {
                return GwtWifiSecurity.netWifiSecurityWEP.name();
            }

            if (securityType.get().equals(WifiSecurity.SECURITY_WPA.name())) {
                return GwtWifiSecurity.netWifiSecurityWPA.name();
            }

            if (securityType.get().equals(WifiSecurity.SECURITY_WPA2.name())) {
                return GwtWifiSecurity.netWifiSecurityWPA2.name();
            }

            if (securityType.get().equals(WifiSecurity.SECURITY_WPA_WPA2.name())) {
                return GwtWifiSecurity.netWifiSecurityWPA_WPA2.name();
            }
        }

        return GwtWifiSecurity.netWifiSecurityNONE.name();
    }

    private String getWifiCiphers(Optional<String> pairwiseCiphers) {
        if (pairwiseCiphers.isPresent()) {
            if (pairwiseCiphers.get().equals(WifiCiphers.CCMP.name())) {
                return GwtWifiCiphers.netWifiCiphers_CCMP.name();
            }

            if (pairwiseCiphers.get().equals(WifiCiphers.TKIP.name())) {
                return GwtWifiCiphers.netWifiCiphers_TKIP.name();
            }

            if (pairwiseCiphers.get().equals(WifiCiphers.CCMP_TKIP.name())) {
                return GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name();
            }
        }

        return GwtWifiCiphers.netWifiCiphers_NONE.name();
    }

    private void setBgScanProperties(GwtWifiConfig gwtWifiConfig, Optional<String> bgScan) {
        String bgScanMode = GwtWifiBgscanModule.netWifiBgscanMode_NONE.name();

        if (bgScan.isPresent()) {
            String[] bgScanParameters = bgScan.get().split(":");

            if (bgScanParameters.length == 4) {

                if (bgScanParameters[0].equals(WifiBgscanModule.SIMPLE.name())) {
                    bgScanMode = GwtWifiBgscanModule.netWifiBgscanMode_SIMPLE.name();
                }

                if (bgScanParameters[0].equals(WifiBgscanModule.LEARN.name())) {
                    bgScanMode = GwtWifiBgscanModule.netWifiBgscanMode_LEARN.name();
                }

                gwtWifiConfig.setBgscanShortInterval(Integer.parseInt(bgScanParameters[1]));
                gwtWifiConfig.setBgscanRssiThreshold(Integer.parseInt(bgScanParameters[2]));
                gwtWifiConfig.setBgscanLongInterval(Integer.parseInt(bgScanParameters[3]));
            }
        }

        gwtWifiConfig.setBgscanModule(bgScanMode);
    }

    private List<Integer> getWifiChannels(String channelValue) {
        List<Integer> channels = new ArrayList<>();

        String[] split = channelValue.split(" ");

        for (String channel : split) {
            if (!channel.trim().isEmpty()) {
                try {
                    channels.add(Integer.parseInt(channel.trim()));
                } catch (NumberFormatException e) {
                    logger.error("Error parsing channel property '" + channelValue + "'", e);
                }
            }
        }

        return channels;
    }

    private Optional<String> getWifiRadioMode(Optional<String> radioMode) {
        if (radioMode.isPresent()) {
            if (radioMode.get().equals(WifiRadioMode.RADIO_MODE_80211_AC.name())) {
                return Optional.of(GwtWifiRadioMode.netWifiRadioModeANAC.name());
            }

            if (radioMode.get().equals(WifiRadioMode.RADIO_MODE_80211a.name())) {
                return Optional.of(GwtWifiRadioMode.netWifiRadioModeA.name());
            }

            if (radioMode.get().equals(WifiRadioMode.RADIO_MODE_80211b.name())) {
                return Optional.of(GwtWifiRadioMode.netWifiRadioModeB.name());
            }

            if (radioMode.get().equals(WifiRadioMode.RADIO_MODE_80211g.name())) {
                return Optional.of(GwtWifiRadioMode.netWifiRadioModeBG.name());
            }

            if (radioMode.get().equals(WifiRadioMode.RADIO_MODE_80211nHT20.name()) ||
                    radioMode.get().equals(WifiRadioMode.RADIO_MODE_80211nHT40above.name()) ||
                    radioMode.get().equals(WifiRadioMode.RADIO_MODE_80211nHT40below.name())) {
                return Optional.of(GwtWifiRadioMode.netWifiRadioModeBGN.name());
            }
        }

        return Optional.empty();
    }

    private void setModemProperties(GwtNetInterfaceConfig gwtConfig, String ifname) {
        if (this.properties.getModemEnabled(ifname) && gwtConfig instanceof GwtModemInterfaceConfig) {
            GwtModemInterfaceConfig gwtModemConfig = (GwtModemInterfaceConfig) gwtConfig;

            gwtModemConfig.setDialString(this.properties.getModemDialString(ifname));
            gwtModemConfig.setAuthType(getModemAuthType(this.properties.getModemAuthType(ifname)));
            gwtModemConfig.setUsername(this.properties.getModemUsername(ifname));
            gwtModemConfig.setPassword(new String(this.properties.getModemPassword(ifname).getPassword()));
            gwtModemConfig.setResetTimeout(this.properties.getModemResetTimeout(ifname));
            gwtModemConfig.setPersist(this.properties.getModemPersistEnabled(ifname));
            gwtModemConfig.setHoldoff(this.properties.getModemHoldoff(ifname));
            gwtModemConfig.setPppNum(this.properties.getModemPppNum(ifname));
            gwtModemConfig.setMaxFail(this.properties.getModemMaxFail(ifname));
            gwtModemConfig.setIdle(this.properties.getModemIdle(ifname));
            gwtModemConfig.setActiveFilter(this.properties.getModemActiveFilter(ifname));
            gwtModemConfig.setLcpEchoInterval(this.properties.getModemIpcEchoInterval(ifname));
            gwtModemConfig.setLcpEchoFailure(this.properties.getModemIpcEchoFailure(ifname));
            gwtModemConfig.setGpsEnabled(this.properties.getModemGpsEnabled(ifname));
            gwtModemConfig.setDiversityEnabled(this.properties.getModemDiversityEnabled(ifname));
            gwtModemConfig.setPdpType(getModemPdpType(this.properties.getModemPdpType(ifname)));
            gwtModemConfig.setApn(this.properties.getModemApn(ifname));
            gwtModemConfig.setHwState(getModemConnectionState(this.properties.getModemConnectionStatus(ifname)));
            // Those properties are not in configuration, what are those?
            // gwtModemConfig.setProfileID();
            // gwtModemConfig.setDataCompression();
            // gwtModemConfig.setHeaderCompression();

            // TODO: remove this
            // copy properties from IPv4 Config
            gwtModemConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());
            gwtModemConfig.setIpAddress("10.200.12.12");
            gwtModemConfig.setSubnetMask("255.255.255.0");
            gwtModemConfig.setGateway("10.200.12.1");
            gwtModemConfig.setStatus(gwtConfig.getStatus());

            gwtModemConfig.setModemId(this.properties.getUsbProductName(ifname));
            gwtModemConfig.setManufacturer(this.properties.getUsbVendorName(ifname));
            gwtModemConfig.setModel(this.properties.getUsbProductId(ifname));
            gwtModemConfig.setHwUsbDevice(this.properties.getUsbDevicePath(ifname));

            logger.debug("GWT Modem Configuration for interface {}:\n{}\n", ifname, gwtModemConfig.getProperties());
        }
    }

    private GwtModemAuthType getModemAuthType(Optional<String> authType) {
        if (authType.isPresent()) {
            if (authType.get().equals(AuthType.AUTO.name())) {
                return GwtModemAuthType.netModemAuthAUTO;
            }

            if (authType.get().equals(AuthType.CHAP.name())) {
                return GwtModemAuthType.netModemAuthCHAP;
            }

            if (authType.get().equals(AuthType.PAP.name())) {
                return GwtModemAuthType.netModemAuthPAP;
            }
        }

        return GwtModemAuthType.netModemAuthNONE;
    }

    private GwtModemPdpType getModemPdpType(Optional<String> pdpType) {
        if (pdpType.isPresent()) {
            if (pdpType.get().equals(PdpType.IP.name())) {
                return GwtModemPdpType.netModemPdpIP;
            }

            if (pdpType.get().equals(PdpType.PPP.name())) {
                return GwtModemPdpType.netModemPdpPPP;
            }

            if (pdpType.get().equals(PdpType.IPv6.name())) {
                return GwtModemPdpType.netModemPdpIPv6;
            }
        }

        return GwtModemPdpType.netModemPdpUnknown;
    }

    private String getModemConnectionState(Optional<String> connectionStatus) {
        if (connectionStatus.isPresent()) {
            if (connectionStatus.get().equals(ModemConnectionStatus.CONNECTED.name())) {
                return NetInterfaceState.ACTIVATED.name();
            }

            if (connectionStatus.get().equals(ModemConnectionStatus.CONNECTING.name())) {
                return NetInterfaceState.IP_CONFIG.name();
            }

            if (connectionStatus.get().equals(ModemConnectionStatus.DISCONNECTED.name())) {
                return NetInterfaceState.DISCONNECTED.name();
            }
        }

        return NetInterfaceState.UNKNOWN.name();
    }

}
