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
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.eclipse.kura.net.wifi.WifiBgscanModule;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtModemAuthType;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtModemPdpType;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetRouterMode;
import org.eclipse.kura.web.shared.model.GwtWifiBgscanModule;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiRadioMode;
import org.eclipse.kura.web.shared.model.GwtWifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter to convert {@link NetworkConfigurationService} properties to GWT
 * configuration objects, namely:
 * 
 * <ul>
 * <li>GwtNetInterfaceConfig</li>
 * <li>GwtFirewallOpenPortEntry</li>
 * <li>GwtWifiHotspotEntry</li>
 * <li>GwtModemPdpEntry</li>
 * <li>GwtFirewallPortForwardEntry</li>
 * <li>GwtFirewallNatEntry</li>
 * <li>GwtWifiChannelFrequency</li>
 * </ul>
 *
 */
public class NetworkConfigurationServiceAdapter {

    private static final String NETWORK_CONFIGURATION_SERVICE_PID = "org.eclipse.kura.net.admin.NetworkConfigurationService";
    private static final String NA = "N/A";
    private static final Logger logger = LoggerFactory.getLogger(NetworkConfigurationServiceAdapter.class);

    private final NetworkConfigurationServiceProperties properties;

    public NetworkConfigurationServiceAdapter() throws GwtKuraException, KuraException {
        ConfigurationService configurationService = ServiceLocator.getInstance().getService(ConfigurationService.class);
        ComponentConfiguration config = configurationService
                .getComponentConfiguration(NETWORK_CONFIGURATION_SERVICE_PID);
        this.properties = new NetworkConfigurationServiceProperties(config.getConfigurationProperties());
    }

    public List<String> getNetInterfaces() {
        return this.properties.getNetInterfaces();
    }

    public GwtNetInterfaceConfig getGwtNetInterfaceConfig(String ifname) {
        GwtNetInterfaceConfig gwtConfig = createGwtNetInterfaceConfig(ifname);

        setStateProperties(gwtConfig, ifname);

        setCommonProperties(gwtConfig, ifname);
        setIpv4Properties(gwtConfig, ifname);
        setIpv4DhcpClientProperties(gwtConfig, ifname);
        setIpv4DhcpServerProperties(gwtConfig, ifname);
        setRouterMode(gwtConfig, ifname);

        // TODO: adhoc wifi mode is missing: fetch from status
        // TODO: pairwiseCiphers and groupCiphers need to be get from status
        setWifiMasterProperties(gwtConfig, ifname);
        setWifiInfraProperties(gwtConfig, ifname);
        setModemProperties(gwtConfig, ifname);

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

    // TODO: this will be retrieved by the STATE part
    private void setStateProperties(GwtNetInterfaceConfig gwtConfig, String ifname) {
        gwtConfig.setHwState(NA);
        gwtConfig.setHwAddress(NA); // MAC address
        gwtConfig.setHwDriver(NA);
        gwtConfig.setHwDriverVersion(NA);
        gwtConfig.setHwFirmware(NA);
        // gwtConfig.setHwMTU(99);
        gwtConfig.setHwUsbDevice(NA);
        gwtConfig.setHwSerial(NA);
        gwtConfig.setHwRssi(NA);
    }

    private void setCommonProperties(GwtNetInterfaceConfig gwtConfig, String ifname) {
        gwtConfig.setName(ifname);
        gwtConfig.setHwName(ifname);
        gwtConfig.setHwType(this.properties.getType(ifname));
    }

    private void setIpv4Properties(GwtNetInterfaceConfig gwtConfig, String ifname) {
        gwtConfig.setStatus(this.properties.getIp4Status(ifname));
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
        Optional<String> masterMode = this.properties.getWifiMasterMode(ifname);

        if (masterMode.isPresent()) {
            GwtWifiConfig gwtWifiConfig = new GwtWifiConfig();

            // common wifi properties

            gwtWifiConfig.setWirelessMode(masterMode.get());
            gwtWifiConfig.setWirelessSsid(this.properties.getWifiMasterSsid(ifname));
            gwtWifiConfig.setDriver(this.properties.getWifiMasterDriver(ifname));
            gwtWifiConfig.setSecurity(getWifiSecurityType(this.properties.getWifiMasterSecurityType(ifname)));
            // TODO: get from status
            // gwtWifiConfig.setPairwiseCiphers(NA);
            // gwtWifiConfig.setGroupCiphers(NA);
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
        }
    }

    private void setWifiInfraProperties(GwtNetInterfaceConfig gwtConfig, String ifname) {
        Optional<String> infraMode = this.properties.getWifiInfraMode(ifname);

        if (infraMode.isPresent()) {
            GwtWifiConfig gwtWifiConfig = new GwtWifiConfig();

            // common wifi properties

            gwtWifiConfig.setWirelessMode(infraMode.get());
            gwtWifiConfig.setWirelessSsid(this.properties.getWifiInfraSsid(ifname));
            gwtWifiConfig.setDriver(this.properties.getWifiInfraDriver(ifname));
            gwtWifiConfig.setSecurity(getWifiSecurityType(this.properties.getWifiInfraSecurityType(ifname)));
            // TODO: get from status
            // gwtWifiConfig.setPairwiseCiphers(NA);
            // gwtWifiConfig.setGroupCiphers(NA);
            gwtWifiConfig.setIgnoreSSID(this.properties.getWifiInfraIgnoreSsid(ifname));
            gwtWifiConfig.setPassword(new String(this.properties.getWifiInfraPassphrase(ifname).getPassword()));
            gwtWifiConfig.setChannels(getWifiChannels(this.properties.getWifiInfraChannel(ifname)));

            // wifi infra specific properties

            gwtWifiConfig.setBgscanModule(getBgScanType(this.properties.getWifiInfraBgscan(ifname)));
            // TODO: get from status
            // gwtWifiConfig.setBgscanRssiThreshold(99);
            // gwtWifiConfig.setBgscanShortInterval(99);
            // gwtWifiConfig.setBgscanLongInterval(99);

            gwtWifiConfig.setPingAccessPoint(this.properties.getWifiInfraPingAP(ifname));

            gwtConfig.setHwRssi(NA);
            gwtConfig.setHwDriver(this.properties.getWifiInfraDriver(ifname));
            ((GwtWifiNetInterfaceConfig) gwtConfig).setStationWifiConfig(gwtWifiConfig);
        }
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

    private String getBgScanType(Optional<String> bgScan) {
        if (bgScan.isPresent()) {
            if (bgScan.get().equals(WifiBgscanModule.SIMPLE.name())) {
                return GwtWifiBgscanModule.netWifiBgscanMode_SIMPLE.name();
            }

            if (bgScan.get().equals(WifiBgscanModule.LEARN.name())) {
                return GwtWifiBgscanModule.netWifiBgscanMode_LEARN.name();
            }
        }

        return GwtWifiBgscanModule.netWifiBgscanMode_NONE.name();
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
            
            if(radioMode.get().equals(WifiRadioMode.RADIO_MODE_80211a.name())) {
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
        if (this.properties.getModemEnabled(ifname)) {
            // TODO: get from status
            // gwtModemConfig.setHwSerial(imei);
            // gwtModemConfig.setHwRssi(Integer.toString(rssi));
            // gwtModemConfig.setHwICCID(iccid);
            // gwtModemConfig.setHwIMSI(imsi);
            // gwtModemConfig.setHwRegistration(registration.name());
            // gwtModemConfig.setHwPLMNID(plmnid);
            // gwtModemConfig.setHwNetwork(network);
            // gwtModemConfig.setHwRadio(radio);
            // gwtModemConfig.setHwBand(band);
            // gwtModemConfig.setHwLAC(lac);
            // gwtModemConfig.setHwCI(ci);
            // gwtModemConfig.setModel(sModel);
            // gwtModemConfig.setGpsSupported(gpsSupported);
            // gwtModemConfig.setHwFirmware(firmwareVersion);
            //
            // gwtModemConfig.setHwState(NetInterfaceState.DISCONNECTED.name());

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
            // Those properties are not in configuration, what are those?
            // gwtModemConfig.setProfileID();
            // gwtModemConfig.setDataCompression();
            // gwtModemConfig.setHeaderCompression();
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

}
