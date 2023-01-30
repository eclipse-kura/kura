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
import java.util.Optional;

import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.wifi.WifiBgscanModule;
import org.eclipse.kura.web.server.net2.utils.EnumsParser;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetRouterMode;
import org.eclipse.kura.web.shared.model.GwtWifiBgscanModule;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtNetInterfaceConfigBuilder {

    private static final Logger logger = LoggerFactory.getLogger(GwtNetInterfaceConfigBuilder.class);
    private static final String NA = "N/A";

    private final NetworkConfigurationServiceProperties properties;
    private GwtNetInterfaceConfig gwtConfig;
    private String ifname;

    public GwtNetInterfaceConfigBuilder(Map<String, Object> confServiceProperties) {
        this.properties = new NetworkConfigurationServiceProperties(confServiceProperties);
    }

    public GwtNetInterfaceConfigBuilder forInterface(String ifname) {
        this.ifname = ifname;
        this.gwtConfig = createGwtNetInterfaceConfigSubtype();
        return this;
    }

    public GwtNetInterfaceConfig build() {
        setCommonProperties();
        setIpv4Properties();
        setIpv4DhcpClientProperties();
        setIpv4DhcpServerProperties();
        setRouterMode();
        setWifiProperties();
        setModemProperties();

        return this.gwtConfig;
    }

    private GwtNetInterfaceConfig createGwtNetInterfaceConfigSubtype() {
        if (this.properties.getType(this.ifname).equals(NetInterfaceType.WIFI.name())) {
            return new GwtWifiNetInterfaceConfig();
        }

        if (this.properties.getType(this.ifname).equals(NetInterfaceType.MODEM.name())) {
            return new GwtModemInterfaceConfig();
        }

        return new GwtNetInterfaceConfig();
    }

    private void setCommonProperties() {
        this.gwtConfig.setName(this.ifname);
        this.gwtConfig.setHwName(this.ifname);
        this.gwtConfig.setHwType(this.properties.getType(this.ifname));

        if (this.gwtConfig instanceof GwtWifiNetInterfaceConfig) {
            String wifiMode = EnumsParser.getGwtWifiWirelessMode(this.properties.getWifiMode(this.ifname));
            ((GwtWifiNetInterfaceConfig) gwtConfig).setWirelessMode(wifiMode);
        }
    }

    private void setIpv4Properties() {
        this.gwtConfig.setStatus(EnumsParser.getGwtNetIfStatus(this.properties.getIp4Status(this.ifname)));
        this.gwtConfig.setIpAddress(this.properties.getIp4Address(this.ifname));
        this.gwtConfig.setSubnetMask(this.properties.getIp4Netmask(this.ifname));
        this.gwtConfig.setGateway(this.properties.getIp4Gateway(this.ifname));
        this.gwtConfig.setDnsServers(this.properties.getIp4DnsServers(this.ifname));
    }

    private void setIpv4DhcpClientProperties() {
        if (this.properties.getDhcpClient4Enabled(this.ifname)) {
            this.gwtConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());
        } else {
            this.gwtConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeManual.name());
        }
    }

    private void setIpv4DhcpServerProperties() {
        if (this.properties.getDhcpServer4Enabled(this.ifname)) {
            this.gwtConfig.setRouterDhcpBeginAddress(this.properties.getDhcpServer4RangeStart(this.ifname));
            this.gwtConfig.setRouterDhcpEndAddress(this.properties.getDhcpServer4RangeEnd(this.ifname));
            this.gwtConfig.setRouterDhcpSubnetMask(this.properties.getDhcpServer4Netmask(this.ifname));
            this.gwtConfig.setRouterDhcpDefaultLease(this.properties.getDhcpServer4LeaseTime(this.ifname));
            this.gwtConfig.setRouterDhcpMaxLease(this.properties.getDhcpServer4MaxLeaseTime(this.ifname));
            this.gwtConfig.setRouterDnsPass(this.properties.getDhcpServer4PassDns(this.ifname));
        }
    }

    private void setRouterMode() {
        boolean isDhcpServer = this.properties.getDhcpServer4Enabled(this.ifname);
        boolean isNat = this.properties.getDhcpServer4PassDns(this.ifname);

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
            String wifiMode = EnumsParser.getGwtWifiWirelessMode(this.properties.getWifiMode(this.ifname));

            if (wifiMode.equals(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name())) {
                setWifiMasterProperties();
            }
            
            if (wifiMode.equals(GwtWifiWirelessMode.netWifiWirelessModeStation.name())) {
                setWifiInfraProperties();
            }
        }
    }

    private void setWifiMasterProperties() {
        GwtWifiConfig gwtWifiConfig = new GwtWifiConfig();

        // common wifi properties

        gwtWifiConfig.setWirelessSsid(this.properties.getWifiMasterSsid(this.ifname));
        gwtWifiConfig.setDriver(this.properties.getWifiMasterDriver(this.ifname));
        gwtWifiConfig.setIgnoreSSID(this.properties.getWifiMasterIgnoreSsid(this.ifname));
        gwtWifiConfig.setPassword(new String(this.properties.getWifiMasterPassphrase(this.ifname).getPassword()));
        gwtWifiConfig.setChannels(this.properties.getWifiMasterChannel(this.ifname));
        gwtWifiConfig.setWirelessMode(
                EnumsParser.getGwtWifiWirelessMode(this.properties.getWifiMasterMode(this.ifname)));
        gwtWifiConfig.setSecurity(
                EnumsParser.getGwtWifiSecurity(this.properties.getWifiMasterSecurityType(this.ifname)));
        gwtWifiConfig.setPairwiseCiphers(
                EnumsParser.getGwtWifiCiphers(this.properties.getWifiInfraPairwiseCiphers(this.ifname)));
        gwtWifiConfig.setGroupCiphers(
                EnumsParser.getGwtWifiCiphers(this.properties.getWifiMasterGroupCiphers(this.ifname)));

        // wifi master specific properties

        Optional<String> radioMode = EnumsParser
                .getGwtWifiRadioMode(this.properties.getWifiMasterRadioMode(this.ifname));
        if (radioMode.isPresent()) {
            gwtWifiConfig.setRadioMode(radioMode.get());
        }

        this.gwtConfig.setHwRssi(NA);
        this.gwtConfig.setHwDriver(this.properties.getWifiMasterDriver(this.ifname));
        ((GwtWifiNetInterfaceConfig) this.gwtConfig).setAccessPointWifiConfig(gwtWifiConfig);
        logger.debug("GWT Wifi Master Configuration for interface {}:\n{}\n", this.ifname,
                gwtWifiConfig.getProperties());
    }

    private void setWifiInfraProperties() {
        GwtWifiConfig gwtWifiConfig = new GwtWifiConfig();

        // common wifi properties

        gwtWifiConfig.setWirelessSsid(this.properties.getWifiInfraSsid(this.ifname));
        gwtWifiConfig.setDriver(this.properties.getWifiInfraDriver(this.ifname));
        gwtWifiConfig.setIgnoreSSID(this.properties.getWifiInfraIgnoreSsid(this.ifname));
        gwtWifiConfig.setPassword(new String(this.properties.getWifiInfraPassphrase(this.ifname).getPassword()));
        gwtWifiConfig.setChannels(this.properties.getWifiInfraChannel(this.ifname));
        gwtWifiConfig
                .setWirelessMode(EnumsParser.getGwtWifiWirelessMode(this.properties.getWifiInfraMode(this.ifname)));
        gwtWifiConfig
                .setSecurity(EnumsParser.getGwtWifiSecurity(this.properties.getWifiInfraSecurityType(this.ifname)));
        gwtWifiConfig.setPairwiseCiphers(
                EnumsParser.getGwtWifiCiphers(this.properties.getWifiMasterPairwiseCiphers(this.ifname)));
        gwtWifiConfig.setGroupCiphers(
                EnumsParser.getGwtWifiCiphers(this.properties.getWifiInfraGroupCiphers(this.ifname)));

        // wifi infra specific properties

        setBgScanProperties(gwtWifiConfig, this.properties.getWifiInfraBgscan(this.ifname));
        gwtWifiConfig.setPingAccessPoint(this.properties.getWifiInfraPingAP(this.ifname));

        this.gwtConfig.setHwRssi(NA);
        this.gwtConfig.setHwDriver(this.properties.getWifiInfraDriver(this.ifname));
        ((GwtWifiNetInterfaceConfig) this.gwtConfig).setStationWifiConfig(gwtWifiConfig);
        logger.debug("GWT Wifi Infra Configuration for interface {}:\n{}\n", this.ifname,
                gwtWifiConfig.getProperties());
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

            gwtModemConfig
                    .setAuthType(EnumsParser.getGwtModemAuthType(this.properties.getModemAuthType(this.ifname)));
            gwtModemConfig.setPdpType(EnumsParser.getGwtModemPdpType(this.properties.getModemPdpType(this.ifname)));
            gwtModemConfig.setHwState(
                    EnumsParser.getNetInterfaceState(this.properties.getModemConnectionStatus(this.ifname)));

            gwtModemConfig.setDialString(this.properties.getModemDialString(this.ifname));
            gwtModemConfig.setUsername(this.properties.getModemUsername(this.ifname));
            gwtModemConfig.setPassword(new String(this.properties.getModemPassword(this.ifname).getPassword()));
            gwtModemConfig.setResetTimeout(this.properties.getModemResetTimeout(this.ifname));
            gwtModemConfig.setPersist(this.properties.getModemPersistEnabled(this.ifname));
            gwtModemConfig.setHoldoff(this.properties.getModemHoldoff(this.ifname));
            gwtModemConfig.setPppNum(this.properties.getModemPppNum(this.ifname));
            gwtModemConfig.setMaxFail(this.properties.getModemMaxFail(this.ifname));
            gwtModemConfig.setIdle(this.properties.getModemIdle(this.ifname));
            gwtModemConfig.setActiveFilter(this.properties.getModemActiveFilter(this.ifname));
            gwtModemConfig.setLcpEchoInterval(this.properties.getModemIpcEchoInterval(this.ifname));
            gwtModemConfig.setLcpEchoFailure(this.properties.getModemIpcEchoFailure(this.ifname));
            gwtModemConfig.setGpsEnabled(this.properties.getModemGpsEnabled(this.ifname));
            gwtModemConfig.setDiversityEnabled(this.properties.getModemDiversityEnabled(this.ifname));
            gwtModemConfig.setApn(this.properties.getModemApn(this.ifname));
            gwtModemConfig.setModemId(this.properties.getUsbProductName(this.ifname));
            gwtModemConfig.setManufacturer(this.properties.getUsbVendorName(this.ifname));
            gwtModemConfig.setModel(this.properties.getUsbProductId(this.ifname));
            gwtModemConfig.setHwUsbDevice(this.properties.getUsbDevicePath(this.ifname));
            gwtModemConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());

            logger.debug("GWT Modem Configuration for interface {}:\n{}\n", this.ifname,
                    gwtModemConfig.getProperties());
        }
    }

}
