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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.net.status.NetworkInterfaceType;
import org.eclipse.kura.net.wifi.WifiBgscanModule;
import org.eclipse.kura.web.server.net2.utils.EnumsParser;
import org.eclipse.kura.web.shared.model.Gwt8021xConfig;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetRouterMode;
import org.eclipse.kura.web.shared.model.GwtWifiBgscanModule;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;

public class GwtNetInterfaceConfigBuilder {

    private static final String NA = "N/A";
    private static final Integer DEFAULT_WAN_PRIORITY = -1;
    private static final Integer DEFAULT_MTU = 0;

    private final NetworkConfigurationServiceProperties properties;
    private GwtNetInterfaceConfig gwtConfig;
    private String ifName;
    private NetworkInterfaceType ifType;

    public GwtNetInterfaceConfigBuilder() {
        this.properties = new NetworkConfigurationServiceProperties();
    }

    public GwtNetInterfaceConfigBuilder(Map<String, Object> confServiceProperties) {
        this.properties = new NetworkConfigurationServiceProperties(confServiceProperties);
    }

    public GwtNetInterfaceConfigBuilder forInterface(String ifname) {
        this.ifName = ifname;
        return this;
    }

    public GwtNetInterfaceConfigBuilder forType(NetworkInterfaceType ifType) {
        this.ifType = ifType;
        return this;
    }

    public GwtNetInterfaceConfig build() {
        this.gwtConfig = createGwtNetInterfaceConfigSubtype();
        setCommonProperties();
        setIpv4Properties();
        setIpv6Properties();
        setIpv4DhcpClientProperties();
        setIpv4DhcpServerProperties();
        setRouterMode();
        setWifiProperties();
        setModemProperties();
        set8021xProperties();

        return this.gwtConfig;
    }

    private GwtNetInterfaceConfig createGwtNetInterfaceConfigSubtype() {
        NetworkInterfaceType type = getInterfaceType();
        if (type.equals(NetworkInterfaceType.WIFI)) {
            return new GwtWifiNetInterfaceConfig();
        }

        if (type.equals(NetworkInterfaceType.MODEM)) {
            return new GwtModemInterfaceConfig();
        }

        return new GwtNetInterfaceConfig();
    }

    private NetworkInterfaceType getInterfaceType() {
        NetworkInterfaceType type = NetworkInterfaceType.UNKNOWN;
        if (Objects.nonNull(this.ifType)) {
            return this.ifType;
        } else {
            Optional<String> typeFromProperties = this.properties.getType(this.ifName);
            if (typeFromProperties.isPresent() && !typeFromProperties.get().isEmpty()) {
                type = NetworkInterfaceType.valueOf(typeFromProperties.get());
            }
        }
        return type;
    }

    private void setCommonProperties() {
        this.gwtConfig.setName(this.ifName);
        this.gwtConfig.setHwName(this.ifName);

        Optional<String> interfaceType = this.properties.getType(this.ifName);
        if (interfaceType.isPresent()) {
            this.gwtConfig.setHwType(interfaceType.get());
        }
    }

    private void setIpv4Properties() {
        this.gwtConfig.setStatus(EnumsParser.getGwtNetIfStatus(this.properties.getIp4Status(this.ifName)));

        Optional<Integer> wanPriority = this.properties.getIp4WanPriority(ifName);
        if (wanPriority.isPresent()) {
            this.gwtConfig.setWanPriority(wanPriority.get());
        } else {
            this.gwtConfig.setWanPriority(DEFAULT_WAN_PRIORITY);
        }

        this.gwtConfig.setIpAddress(this.properties.getIp4Address(this.ifName));
        this.gwtConfig.setSubnetMask(this.properties.getIp4Netmask(this.ifName));
        this.gwtConfig.setGateway(this.properties.getIp4Gateway(this.ifName));
        this.gwtConfig.setDnsServers(this.properties.getIp4DnsServers(this.ifName));

        Optional<Integer> mtu = this.properties.getIp4Mtu(this.ifName);
        this.gwtConfig.setMtu(mtu.orElse(DEFAULT_MTU));
    }

    private void setIpv6Properties() {
        Optional<String> status = this.properties.getIp6Status(this.ifName);
        if (status.isPresent()) {
            this.gwtConfig.setIpv6Status(status.get());
        }

        Optional<Integer> priority = this.properties.getIp6WanPriority(this.ifName);
        if (priority.isPresent()) {
            this.gwtConfig.setIpv6WanPriority(priority.get());
        } else {
            this.gwtConfig.setIpv6WanPriority(DEFAULT_WAN_PRIORITY);
        }

        Optional<String> configure = this.properties.getIp6AddressMethod(this.ifName);
        if (configure.isPresent()) {
            this.gwtConfig.setIpv6ConfigMode(configure.get());
        }

        Optional<String> autoconfiguration = this.properties.getIp6AddressGenMode(this.ifName);
        if (autoconfiguration.isPresent()) {
            this.gwtConfig.setIpv6AutoconfigurationMode(autoconfiguration.get());
        }

        this.gwtConfig.setIpv6Address(this.properties.getIp6Address(this.ifName));

        Optional<Integer> netmask = this.properties.getIp6Netmask(this.ifName);
        if (netmask.isPresent()) {
            this.gwtConfig.setIpv6SubnetMask(netmask.get());
        }

        this.gwtConfig.setIpv6Gateway(this.properties.getIp6Gateway(this.ifName));
        this.gwtConfig.setIpv6DnsServers(this.properties.getIp6DnsServers(this.ifName));

        Optional<String> privacy = this.properties.getIp6Privacy(this.ifName);
        if (privacy.isPresent()) {
            this.gwtConfig.setIpv6Privacy(privacy.get());
        }

        Optional<Integer> mtu = this.properties.getIp6Mtu(this.ifName);
        this.gwtConfig.setIpv6Mtu(mtu.orElse(DEFAULT_MTU));
    }

    private void setIpv4DhcpClientProperties() {
        if (this.properties.getDhcpClient4Enabled(this.ifName)) {
            this.gwtConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());
        } else {
            this.gwtConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeManual.name());
        }
    }

    private void setIpv4DhcpServerProperties() {
        if (this.properties.getDhcpServer4Enabled(this.ifName)) {
            this.gwtConfig.setRouterDhcpBeginAddress(this.properties.getDhcpServer4RangeStart(this.ifName));
            this.gwtConfig.setRouterDhcpEndAddress(this.properties.getDhcpServer4RangeEnd(this.ifName));
            this.gwtConfig.setRouterDhcpSubnetMask(this.properties.getDhcpServer4Netmask(this.ifName));
            this.gwtConfig.setRouterDhcpDefaultLease(this.properties.getDhcpServer4LeaseTime(this.ifName));
            this.gwtConfig.setRouterDhcpMaxLease(this.properties.getDhcpServer4MaxLeaseTime(this.ifName));
            this.gwtConfig.setRouterDnsPass(this.properties.getDhcpServer4PassDns(this.ifName));
        }
    }

    private void setRouterMode() {
        boolean isDhcpServer = this.properties.getDhcpServer4Enabled(this.ifName);
        boolean isNat = this.properties.getNatEnabled(this.ifName);

        if (isDhcpServer && isNat) {
            this.gwtConfig.setRouterMode(GwtNetRouterMode.netRouterDchpNat.name());
        } else if (isDhcpServer) {
            this.gwtConfig.setRouterMode(GwtNetRouterMode.netRouterDchp.name());
        } else if (isNat) {
            this.gwtConfig.setRouterMode(GwtNetRouterMode.netRouterNat.name());
        } else {
            this.gwtConfig.setRouterMode(GwtNetRouterMode.netRouterOff.name());
        }
    }

    private void setWifiProperties() {
        if (this.gwtConfig instanceof GwtWifiNetInterfaceConfig) {
            String wifiMode = EnumsParser.getGwtWifiWirelessMode(this.properties.getWifiMode(this.ifName));
            ((GwtWifiNetInterfaceConfig) gwtConfig).setWirelessMode(wifiMode);

            setWifiMasterProperties();
            setWifiInfraProperties();

            if (wifiMode.equals(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name())) {
                this.gwtConfig.setHwRssi(NA);
                this.gwtConfig.setHwDriver(this.properties.getWifiMasterDriver(this.ifName));
            }

            if (wifiMode.equals(GwtWifiWirelessMode.netWifiWirelessModeStation.name())) {
                this.gwtConfig.setHwRssi(NA);
                this.gwtConfig.setHwDriver(this.properties.getWifiInfraDriver(this.ifName));
            }
        }
    }

    private void setWifiMasterProperties() {
        GwtWifiConfig gwtWifiConfig = new GwtWifiConfig();

        // common wifi properties

        gwtWifiConfig.setWirelessSsid(this.properties.getWifiMasterSsid(this.ifName));
        gwtWifiConfig.setDriver(this.properties.getWifiMasterDriver(this.ifName));
        gwtWifiConfig.setIgnoreSSID(this.properties.getWifiMasterIgnoreSsid(this.ifName));
        gwtWifiConfig.setPassword(new String(this.properties.getWifiMasterPassphrase(this.ifName).getPassword()));
        gwtWifiConfig.setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name());
        gwtWifiConfig
                .setSecurity(EnumsParser.getGwtWifiSecurity(this.properties.getWifiMasterSecurityType(this.ifName)));
        gwtWifiConfig.setPairwiseCiphers(
                EnumsParser.getGwtWifiCiphers(this.properties.getWifiMasterPairwiseCiphers(this.ifName)));
        gwtWifiConfig
                .setGroupCiphers(EnumsParser.getGwtWifiCiphers(this.properties.getWifiMasterGroupCiphers(this.ifName)));
        gwtWifiConfig.setChannels(this.properties.getWifiMasterChannel(this.ifName));

        // wifi master specific properties

        Optional<String> radioMode = EnumsParser
                .getGwtWifiRadioMode(this.properties.getWifiMasterRadioMode(this.ifName));
        if (radioMode.isPresent()) {
            gwtWifiConfig.setRadioMode(radioMode.get());
        }

        ((GwtWifiNetInterfaceConfig) this.gwtConfig).setAccessPointWifiConfig(gwtWifiConfig);
    }

    private void setWifiInfraProperties() {
        GwtWifiConfig gwtWifiConfig = new GwtWifiConfig();

        // common wifi properties

        gwtWifiConfig.setWirelessSsid(this.properties.getWifiInfraSsid(this.ifName));
        gwtWifiConfig.setDriver(this.properties.getWifiInfraDriver(this.ifName));
        gwtWifiConfig.setIgnoreSSID(this.properties.getWifiInfraIgnoreSsid(this.ifName));
        gwtWifiConfig.setPassword(new String(this.properties.getWifiInfraPassphrase(this.ifName).getPassword()));
        gwtWifiConfig.setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeStation.name());
        gwtWifiConfig
                .setSecurity(EnumsParser.getGwtWifiSecurity(this.properties.getWifiInfraSecurityType(this.ifName)));
        gwtWifiConfig.setPairwiseCiphers(
                EnumsParser.getGwtWifiCiphers(this.properties.getWifiInfraPairwiseCiphers(this.ifName)));
        gwtWifiConfig
                .setGroupCiphers(EnumsParser.getGwtWifiCiphers(this.properties.getWifiInfraGroupCiphers(this.ifName)));
        gwtWifiConfig.setChannels(this.properties.getWifiInfraChannel(this.ifName));

        // wifi infra specific properties

        Optional<String> radioMode = EnumsParser
                .getGwtWifiRadioMode(this.properties.getWifiInfraRadioMode(this.ifName));
        if (radioMode.isPresent()) {
            gwtWifiConfig.setRadioMode(radioMode.get());
        }

        setBgScanProperties(gwtWifiConfig, this.properties.getWifiInfraBgscan(this.ifName));
        gwtWifiConfig.setPingAccessPoint(this.properties.getWifiInfraPingAP(this.ifName));

        ((GwtWifiNetInterfaceConfig) this.gwtConfig).setStationWifiConfig(gwtWifiConfig);
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

    private void setModemProperties() {
        if (this.gwtConfig instanceof GwtModemInterfaceConfig) {
            GwtModemInterfaceConfig gwtModemConfig = (GwtModemInterfaceConfig) this.gwtConfig;

            gwtModemConfig.setAuthType(EnumsParser.getGwtModemAuthType(this.properties.getModemAuthType(this.ifName)));
            gwtModemConfig.setPdpType(EnumsParser.getGwtModemPdpType(this.properties.getModemPdpType(this.ifName)));

            gwtModemConfig.setDialString(this.properties.getModemDialString(this.ifName));
            gwtModemConfig.setUsername(this.properties.getModemUsername(this.ifName));
            gwtModemConfig.setPassword(new String(this.properties.getModemPassword(this.ifName).getPassword()));
            gwtModemConfig.setResetTimeout(this.properties.getModemResetTimeout(this.ifName));
            gwtModemConfig.setPersist(this.properties.getModemPersistEnabled(this.ifName));
            gwtModemConfig.setHoldoff(this.properties.getModemHoldoff(this.ifName));
            gwtModemConfig.setPppNum(this.properties.getModemPppNum(this.ifName));
            gwtModemConfig.setMaxFail(this.properties.getModemMaxFail(this.ifName));
            gwtModemConfig.setIdle(this.properties.getModemIdle(this.ifName));
            gwtModemConfig.setActiveFilter(this.properties.getModemActiveFilter(this.ifName));
            gwtModemConfig.setLcpEchoInterval(this.properties.getModemLpcEchoInterval(this.ifName));
            gwtModemConfig.setLcpEchoFailure(this.properties.getModemLpcEchoFailure(this.ifName));
            gwtModemConfig.setGpsEnabled(this.properties.getModemGpsEnabled(this.ifName));
            gwtModemConfig.setDiversityEnabled(this.properties.getModemDiversityEnabled(this.ifName));
            gwtModemConfig.setApn(this.properties.getModemApn(this.ifName));
            gwtModemConfig.setModemId(this.properties.getUsbProductName(this.ifName));
            gwtModemConfig.setManufacturer(this.properties.getUsbVendorName(this.ifName));
            gwtModemConfig.setModel(this.properties.getUsbProductId(this.ifName));
            gwtModemConfig.setHwUsbDevice(this.properties.getUsbDevicePath(this.ifName));
            gwtModemConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());
        }
    }

    private void set8021xProperties() {

        Gwt8021xConfig gwt8021xConfig = this.gwtConfig.get8021xConfig();

        gwt8021xConfig.setEap(this.properties.get8021xEap(this.ifName));
        gwt8021xConfig.setInnerAuth(this.properties.get8021xInnerAuth(this.ifName));
        gwt8021xConfig.setIdentity(this.properties.get8021xIdentity(this.ifName));
        gwt8021xConfig.setPassword(new String(this.properties.get8021xPassword(this.ifName).getPassword()));
        gwt8021xConfig.setKeystorePid(this.properties.get8021xKeystorePid(this.ifName));
        gwt8021xConfig.setCaCertName(this.properties.get8021xCaCertName(this.ifName));
        gwt8021xConfig.setPublicPrivateKeyPairName(this.properties.get8021xPublicPrivateKeyPairName(this.ifName));

    }

}
