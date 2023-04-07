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

import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.web.server.net2.utils.EnumsParser;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetRouterMode;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;

public class NetworkConfigurationServicePropertiesBuilder {

    private final GwtNetInterfaceConfig gwtConfig;
    private final NetworkConfigurationServiceProperties properties;
    private final String ifname;

    public NetworkConfigurationServicePropertiesBuilder(GwtNetInterfaceConfig gwtConfig) {
        this.gwtConfig = gwtConfig;
        this.properties = new NetworkConfigurationServiceProperties();
        this.ifname = this.gwtConfig.getName();
    }

    public Map<String, Object> build() {
        setCommonProperties();
        setIpv4Properties();
        setIpv4DhcpClientProperties();
        setIpv4DhcpServerProperties();

        switch (this.gwtConfig.getStatusEnum()) {
            case netIPv4StatusDisabled:
                break;
            case netIPv4StatusUnmanaged:
                break;
            default:
                setWifiProperties();
                setModemProperties();
                break;
        }

        return this.properties.getProperties();
    }

    private void setCommonProperties() {
        this.properties.setType(this.ifname, this.gwtConfig.getHwType());

        if (this.gwtConfig instanceof GwtWifiNetInterfaceConfig) {
            String wifiMode = EnumsParser
                    .getWifiMode(Optional.ofNullable(((GwtWifiNetInterfaceConfig) this.gwtConfig).getWirelessMode()));
            this.properties.setWifiMode(this.ifname, wifiMode);
        }
    }

    private void setIpv4Properties() {
        this.properties.setIp4Status(this.ifname,
                EnumsParser.getNetInterfaceStatus(Optional.ofNullable(this.gwtConfig.getStatus())));

        boolean isManual = this.gwtConfig.getConfigMode().equals(GwtNetIfConfigMode.netIPv4ConfigModeManual.name());
        boolean isWan = this.gwtConfig.getStatus().equals(GwtNetIfStatus.netIPv4StatusEnabledWAN.name());

        if (isWan) {
            if (!Objects.isNull(this.gwtConfig.getWanPriority())) {
                this.properties.setIp4WanPriority(ifname, this.gwtConfig.getWanPriority());
            }

            this.properties.setIp4DnsServers(this.ifname, this.gwtConfig.getDnsServers());
        }

        if (isManual) {
            this.properties.setIp4Address(this.ifname, this.gwtConfig.getIpAddress());
            this.properties.setIp4Netmask(this.ifname, this.gwtConfig.getSubnetMask());
        }

        if (isManual && isWan) {
            this.properties.setIp4Gateway(this.ifname, this.gwtConfig.getGateway());
        }
    }

    private void setIpv4DhcpClientProperties() {
        switch (this.gwtConfig.getConfigModeEnum()) {
            case netIPv4ConfigModeDHCP:
                this.properties.setDhcpClient4Enabled(this.ifname, true);
                break;
            case netIPv4ConfigModeManual:
                this.properties.setDhcpClient4Enabled(this.ifname, false);
                break;
            default:
                break;
        }
    }

    private void setIpv4DhcpServerProperties() {
        boolean isManualAddress = this.gwtConfig.getConfigModeEnum() == GwtNetIfConfigMode.netIPv4ConfigModeManual;
        boolean isDhcpServer = isManualAddress
                && this.gwtConfig.getRouterModeEnum() != GwtNetRouterMode.netRouterOff
                && this.gwtConfig.getRouterModeEnum() != GwtNetRouterMode.netRouterNat;
        boolean isNatEnabled = isManualAddress
                && (this.gwtConfig.getRouterModeEnum() == GwtNetRouterMode.netRouterNat
                        || this.gwtConfig.getRouterModeEnum() == GwtNetRouterMode.netRouterDchpNat);

        this.properties.setDhcpServer4Enabled(this.ifname, isDhcpServer);
        this.properties.setNatEnabled(this.ifname, isNatEnabled);

        if (isDhcpServer) {
            this.properties.setDhcpServer4RangeStart(this.ifname, this.gwtConfig.getRouterDhcpBeginAddress());
            this.properties.setDhcpServer4RangeEnd(this.ifname, this.gwtConfig.getRouterDhcpEndAddress());
            this.properties.setDhcpServer4Netmask(this.ifname, this.gwtConfig.getRouterDhcpSubnetMask());
            this.properties.setDhcpServer4LeaseTime(this.ifname, this.gwtConfig.getRouterDhcpDefaultLease());
            this.properties.setDhcpServer4MaxLeaseTime(this.ifname, this.gwtConfig.getRouterDhcpMaxLease());
            this.properties.setDhcpServer4PassDns(this.ifname, this.gwtConfig.getRouterDnsPass());
        }
    }

    private void setWifiProperties() {
        if (this.gwtConfig instanceof GwtWifiNetInterfaceConfig) {
            String wifiMode = EnumsParser
                    .getWifiMode(Optional.ofNullable(((GwtWifiNetInterfaceConfig) this.gwtConfig).getWirelessMode()));

            this.properties.setWifiMode(this.ifname, wifiMode);

            if (wifiMode.equals(WifiMode.MASTER.name())) {
                setWifiMasterProperties();
            }

            if (wifiMode.equals(WifiMode.INFRA.name())) {
                setWifiInfraProperties();
            }
        }
    }

    private void setWifiMasterProperties() {
        GwtWifiConfig gwtWifiConfig = ((GwtWifiNetInterfaceConfig) this.gwtConfig).getAccessPointWifiConfig();

        // common wifi properties

        this.properties.setWifiMasterSsid(this.ifname, gwtWifiConfig.getWirelessSsid());
        this.properties.setWifiMasterIgnoreSsid(this.ifname, gwtWifiConfig.ignoreSSID());
        this.properties.setWifiMasterPassphrase(this.ifname, gwtWifiConfig.getPassword());
        this.properties.setWifiMasterChannel(this.ifname, gwtWifiConfig.getChannels());

        this.properties.setWifiMasterMode(this.ifname,
                EnumsParser.getWifiMode(Optional.ofNullable(gwtWifiConfig.getWirelessMode())));
        this.properties.setWifiMasterSecurityType(this.ifname,
                EnumsParser.getWifiSecurity(Optional.ofNullable(gwtWifiConfig.getSecurity())));
        this.properties.setWifiMasterPairwiseCiphers(this.ifname,
                EnumsParser.getWifiCiphers(Optional.ofNullable(gwtWifiConfig.getPairwiseCiphers())));
        this.properties.setWifiMasterGroupCiphers(this.ifname,
                EnumsParser.getWifiCiphers(Optional.ofNullable(gwtWifiConfig.getGroupCiphers())));

        // wifi master specific properties
        this.properties.setWifiMasterRadioMode(this.ifname,
                EnumsParser.getWifiRadioMode(Optional.ofNullable(gwtWifiConfig.getRadioMode())));

    }

    private void setWifiInfraProperties() {
        GwtWifiConfig gwtWifiConfig = ((GwtWifiNetInterfaceConfig) this.gwtConfig).getStationWifiConfig();

        // common wifi properties

        this.properties.setWifiInfraSsid(this.ifname, gwtWifiConfig.getWirelessSsid());
        this.properties.setWifiInfraIgnoreSsid(this.ifname, gwtWifiConfig.ignoreSSID());
        this.properties.setWifiInfraPassphrase(this.ifname, gwtWifiConfig.getPassword());
        this.properties.setWifiInfraChannel(this.ifname, gwtWifiConfig.getChannels());

        this.properties.setWifiInfraMode(this.ifname,
                EnumsParser.getWifiMode(Optional.ofNullable(gwtWifiConfig.getWirelessMode())));
        this.properties.setWifiInfraSecurityType(this.ifname,
                EnumsParser.getWifiSecurity(Optional.ofNullable(gwtWifiConfig.getSecurity())));
        this.properties.setWifiInfraPairwiseCiphers(this.ifname,
                EnumsParser.getWifiCiphers(Optional.ofNullable(gwtWifiConfig.getPairwiseCiphers())));
        this.properties.setWifiInfraGroupCiphers(this.ifname,
                EnumsParser.getWifiCiphers(Optional.ofNullable(gwtWifiConfig.getGroupCiphers())));
        this.properties.setWifiInfraRadioMode(this.ifname,
                EnumsParser.getWifiRadioMode(Optional.ofNullable(gwtWifiConfig.getRadioMode())));

        // wifi infra specific properties

        setBgScanProperties(gwtWifiConfig, gwtWifiConfig.getBgscanModule());
        this.properties.setWifiInfraPingAP(this.ifname, gwtWifiConfig.pingAccessPoint());
    }

    private void setBgScanProperties(GwtWifiConfig gwtWifiConfig, String gwtBgScanModule) {
        if (gwtBgScanModule != null) {
            StringBuilder bgScanProperty = new StringBuilder();

            bgScanProperty.append(gwtBgScanModule);
            bgScanProperty.append(":");
            bgScanProperty.append(gwtWifiConfig.getBgscanShortInterval());
            bgScanProperty.append(":");
            bgScanProperty.append(gwtWifiConfig.getBgscanRssiThreshold());
            bgScanProperty.append(":");
            bgScanProperty.append(gwtWifiConfig.getBgscanLongInterval());

            this.properties.setWifiInfraBgscan(this.ifname, bgScanProperty.toString());
        }
    }

    private void setModemProperties() {
        if (this.gwtConfig instanceof GwtModemInterfaceConfig) {
            GwtModemInterfaceConfig gwtModemConfig = (GwtModemInterfaceConfig) this.gwtConfig;

            this.properties.setModemAuthType(this.ifname, Optional.ofNullable(gwtModemConfig.getAuthType().name()));
            this.properties.setModemPdpType(this.ifname, Optional.ofNullable(gwtModemConfig.getPdpType().name()));
            this.properties.setModemConnectionStatus(this.ifname, Optional.ofNullable(gwtModemConfig.getHwState()));
            this.properties.setModemDialString(this.ifname, gwtModemConfig.getDialString());
            this.properties.setModemUsername(this.ifname, gwtModemConfig.getUsername());
            this.properties.setModemPassword(this.ifname, gwtModemConfig.getPassword());
            this.properties.setModemResetTimeout(this.ifname, gwtModemConfig.getResetTimeout());
            this.properties.setModemPersistEnabled(this.ifname, gwtModemConfig.isPersist());
            this.properties.setModemHoldoff(this.ifname, gwtModemConfig.getHoldoff());
            this.properties.setModemPppNum(this.ifname, gwtModemConfig.getPppNum());
            this.properties.setModemMaxFail(this.ifname, gwtModemConfig.getMaxFail());
            this.properties.setModemIdle(this.ifname, gwtModemConfig.getIdle());
            this.properties.setModemActiveFilter(this.ifname, gwtModemConfig.getActiveFilter());
            this.properties.setModemLpcEchoInterval(ifname, gwtModemConfig.getLcpEchoInterval());
            this.properties.setModemLpcEchoFailure(this.ifname, gwtModemConfig.getLcpEchoFailure());
            this.properties.setModemGpsEnabled(this.ifname, gwtModemConfig.isGpsEnabled());
            this.properties.setModemDiversityEnabled(this.ifname, gwtModemConfig.isDiversityEnabled());
            this.properties.setModemApn(this.ifname, gwtModemConfig.getApn());
            this.properties.setUsbProductName(this.ifname, gwtModemConfig.getModemId());
            this.properties.setUsbVendorName(this.ifname, gwtModemConfig.getManufacturer());
            this.properties.setUsbProductId(this.ifname, gwtModemConfig.getModel());
            this.properties.setUsbDevicePath(this.ifname, gwtModemConfig.getHwUsbDevice());
        }
    }

}
