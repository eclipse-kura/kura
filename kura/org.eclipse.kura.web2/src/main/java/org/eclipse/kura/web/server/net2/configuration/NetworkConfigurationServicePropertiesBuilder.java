/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Areti
 *******************************************************************************/
package org.eclipse.kura.web.server.net2.configuration;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.web.server.net2.utils.EnumsParser;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.Gwt8021xConfig;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetRouterMode;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkConfigurationServicePropertiesBuilder {

    private static final Integer DEFAULT_WAN_PRIORITY = -1;
    private static final Integer DEFAULT_MTU = 0;
    private static final Integer DEFAULT_PROMISC = -1;

    private final GwtNetInterfaceConfig gwtConfig;
    private final NetworkConfigurationServiceProperties properties;
    private final String ifname;

    private static final Logger logger = LoggerFactory.getLogger(NetworkConfigurationServicePropertiesBuilder.class);

    private final GwtNetInterfaceConfig oldGwtNetInterfaceConfig;

    public NetworkConfigurationServicePropertiesBuilder(GwtNetInterfaceConfig gwtConfig)
            throws GwtKuraException, KuraException {
        this.gwtConfig = gwtConfig;
        this.gwtConfig.setUnescaped(true);
        this.properties = new NetworkConfigurationServiceProperties();
        this.ifname = this.gwtConfig.getName();
        this.oldGwtNetInterfaceConfig = getConfigsAndStatuses(this.gwtConfig.getName());
    }

    public Map<String, Object> build() throws GwtKuraException {
        setCommonProperties();
        setIpv4Properties();
        setIpv6Properties();
        setIpv4DhcpClientProperties();
        setIpv4DhcpServerProperties();
        set8021xConfig();

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

        // Manage GPS independently of device ip status
        setModemGpsProperties();

        setAdvancedProperties();

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
            if (Objects.nonNull(this.gwtConfig.getWanPriority())) {
                this.properties.setIp4WanPriority(this.ifname, this.gwtConfig.getWanPriority());
            } else {
                this.properties.setIp4WanPriority(this.ifname, DEFAULT_WAN_PRIORITY);
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

    private void setIpv6Properties() {
        this.properties.setIp6Status(this.ifname, this.gwtConfig.getIpv6Status());

        if (this.gwtConfig.getIpv6Status().equals("netIPv6StatusDisabled")
                || this.gwtConfig.getIpv6Status().equals("netIPv6StatusUnmanaged")) {
            return;
        }

        this.properties.setIp6AddressMethod(this.ifname, this.gwtConfig.getIpv6ConfigMode());

        boolean isManual = this.gwtConfig.getIpv6ConfigMode().equals("netIPv6MethodManual");
        boolean isWan = this.gwtConfig.getIpv6Status().equals("netIPv6StatusEnabledWAN");
        boolean isAuto = this.gwtConfig.getIpv6ConfigMode().equals("netIPv6MethodAuto");

        if (isWan) {
            if (Objects.nonNull(this.gwtConfig.getIpv6WanPriority())) {
                this.properties.setIp6WanPriority(this.ifname, this.gwtConfig.getIpv6WanPriority());
            } else {
                this.properties.setIp6WanPriority(this.ifname, DEFAULT_WAN_PRIORITY);
            }

            this.properties.setIp6DnsServers(this.ifname, this.gwtConfig.getIpv6DnsServers());
        }

        if (isManual) {
            this.properties.setIp6Address(this.ifname, this.gwtConfig.getIpv6Address());
            this.properties.setIp6Netmask(this.ifname, this.gwtConfig.getIpv6SubnetMask());
        }

        if (isManual && isWan) {
            this.properties.setIp6Gateway(this.ifname, this.gwtConfig.getIpv6Gateway());
        }

        if (isAuto) {
            this.properties.setIp6AddressGenMode(this.ifname, this.gwtConfig.getIpv6AutoconfigurationMode());
            this.properties.setIp6Privacy(this.ifname, this.gwtConfig.getIpv6Privacy());
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
        boolean isDhcpServer = isManualAddress && this.gwtConfig.getRouterModeEnum() != GwtNetRouterMode.netRouterOff
                && this.gwtConfig.getRouterModeEnum() != GwtNetRouterMode.netRouterNat;
        boolean isNatEnabled = isManualAddress && (this.gwtConfig.getRouterModeEnum() == GwtNetRouterMode.netRouterNat
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

    private void setWifiProperties() throws GwtKuraException {
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

    private void setWifiMasterProperties() throws GwtKuraException {
        GwtWifiConfig gwtWifiConfig = ((GwtWifiNetInterfaceConfig) this.gwtConfig).getAccessPointWifiConfig();
        gwtWifiConfig.setUnescaped(true);

        // common wifi properties

        this.properties.setWifiMasterSsid(this.ifname, gwtWifiConfig.getWirelessSsid());
        this.properties.setWifiMasterIgnoreSsid(this.ifname, gwtWifiConfig.ignoreSSID());

        if (gwtWifiConfig.getPassword() != null) {
            if (GwtServerUtil.PASSWORD_PLACEHOLDER.equals(gwtWifiConfig.getPassword())
                    && this.oldGwtNetInterfaceConfig instanceof GwtWifiNetInterfaceConfig) {
                GwtWifiNetInterfaceConfig gwtWifiNetInterfaceConfig = (GwtWifiNetInterfaceConfig) this.oldGwtNetInterfaceConfig;
                gwtWifiNetInterfaceConfig.setUnescaped(true);

                GwtWifiConfig gwtApConfig = gwtWifiNetInterfaceConfig.getAccessPointWifiConfig();
                gwtApConfig.setUnescaped(true);

                this.properties.setWifiMasterPassphrase(this.ifname, gwtApConfig.getPassword());
            } else {
                GwtServerUtil.validateUserPassword(gwtWifiConfig.getPassword());
                this.properties.setWifiMasterPassphrase(this.ifname, gwtWifiConfig.getPassword());
            }
        }

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

    private void set8021xConfig() throws GwtKuraException {
        if (this.gwtConfig.get8021xConfig() == null || !(this.gwtConfig instanceof GwtWifiNetInterfaceConfig)) {
            return;
        }

        logger.info("setting 802-1x config");

        if (this.gwtConfig.get8021xConfig().getEap() != null && !this.gwtConfig.get8021xConfig().getEap().isEmpty()) {
            this.properties.set8021xEap(this.ifname, this.gwtConfig.get8021xConfig().getEap());
        }

        if (isValidStringProperty(this.gwtConfig.get8021xConfig().getInnerAuth())) {
            this.properties.set8021xInnerAuth(this.ifname, this.gwtConfig.get8021xConfig().getInnerAuth());
        }

        if (isValidStringProperty(this.gwtConfig.get8021xConfig().getUsername())) {
            this.properties.set8021xIdentity(this.ifname, this.gwtConfig.get8021xConfig().getUsername());
        }

        if (isValidStringProperty(this.gwtConfig.get8021xConfig().getPassword())) {
            String password8021x = this.gwtConfig.get8021xConfig().getPassword();

            if (GwtServerUtil.PASSWORD_PLACEHOLDER.equals(password8021x)
                    && this.oldGwtNetInterfaceConfig instanceof GwtWifiNetInterfaceConfig) {
                GwtWifiNetInterfaceConfig gwtWifiNetInterfaceConfig = (GwtWifiNetInterfaceConfig) this.oldGwtNetInterfaceConfig;

                Gwt8021xConfig gwt8021xConfig = gwtWifiNetInterfaceConfig.get8021xConfig();

                this.properties.set8021xPassword(this.ifname, gwt8021xConfig.getPassword());
            } else {
                GwtServerUtil.validateUserPassword(password8021x);
                this.properties.set8021xPassword(this.ifname, password8021x);
            }
        }

        set8021xCertificatesAndPublicPrivateKeyPairs();
        logger.info("DONE - setting 802-1x config");
    }

    private boolean isValidStringProperty(String value) {
        return value != null && !value.isEmpty();
    }

    private void set8021xCertificatesAndPublicPrivateKeyPairs() {
        if (this.gwtConfig.get8021xConfig().getKeystorePid() != null) {
            this.properties.set8021xKeystorePid(this.ifname, this.gwtConfig.get8021xConfig().getKeystorePid());
        }

        if (this.gwtConfig.get8021xConfig().getCaCertName() != null) {
            this.properties.set8021xCaCertName(this.ifname, this.gwtConfig.get8021xConfig().getCaCertName());
        }

        if (this.gwtConfig.get8021xConfig().getPublicPrivateKeyPairName() != null) {
            this.properties.set8021xPublicPrivateKeyPairName(this.ifname,
                    this.gwtConfig.get8021xConfig().getPublicPrivateKeyPairName());
            this.properties.set8021xClientCertName(this.ifname,
                    this.gwtConfig.get8021xConfig().getPublicPrivateKeyPairName());
        }
    }

    private void setWifiInfraProperties() {
        GwtWifiConfig gwtWifiConfig = ((GwtWifiNetInterfaceConfig) this.gwtConfig).getStationWifiConfig();
        gwtWifiConfig.setUnescaped(true);

        // common wifi properties

        this.properties.setWifiInfraSsid(this.ifname, gwtWifiConfig.getWirelessSsid());
        this.properties.setWifiInfraIgnoreSsid(this.ifname, gwtWifiConfig.ignoreSSID());

        if (gwtWifiConfig.getPassword() != null) {
            if (GwtServerUtil.PASSWORD_PLACEHOLDER.equals(gwtWifiConfig.getPassword())
                    && this.oldGwtNetInterfaceConfig instanceof GwtWifiNetInterfaceConfig) {
                GwtWifiNetInterfaceConfig gwtWifiNetInterfaceConfig = (GwtWifiNetInterfaceConfig) this.oldGwtNetInterfaceConfig;
                gwtWifiNetInterfaceConfig.setUnescaped(true);

                GwtWifiConfig gwtStationConfig = gwtWifiNetInterfaceConfig.getStationWifiConfig();
                gwtStationConfig.setUnescaped(true);

                this.properties.setWifiInfraPassphrase(this.ifname, gwtStationConfig.getPassword());
            } else {
                this.properties.setWifiInfraPassphrase(this.ifname, gwtWifiConfig.getPassword());
            }
        }

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

            this.properties.setModemAuthType(this.ifname,
                    EnumsParser.getAuthType(Optional.ofNullable(gwtModemConfig.getAuthType())));
            this.properties.setModemPdpType(this.ifname,
                    EnumsParser.getPdpType(Optional.ofNullable(gwtModemConfig.getPdpType())));
            this.properties.setModemDialString(this.ifname, gwtModemConfig.getDialString());
            this.properties.setModemUsername(this.ifname, gwtModemConfig.getUsername());

            if (gwtModemConfig.getPassword() != null) {
                if (GwtServerUtil.PASSWORD_PLACEHOLDER.equals(gwtModemConfig.getPassword())
                        && this.oldGwtNetInterfaceConfig instanceof GwtModemInterfaceConfig) {

                    GwtModemInterfaceConfig gwtModemInterfaceConfig = (GwtModemInterfaceConfig) this.oldGwtNetInterfaceConfig;
                    gwtModemInterfaceConfig.setUnescaped(true);

                    this.properties.setModemPassword(this.ifname, gwtModemInterfaceConfig.getPassword());
                } else {
                    this.properties.setModemPassword(this.ifname, gwtModemConfig.getPassword());
                }
            }

            this.properties.setModemResetTimeout(this.ifname, gwtModemConfig.getResetTimeout());
            this.properties.setModemPersistEnabled(this.ifname, gwtModemConfig.isPersist());
            this.properties.setModemHoldoff(this.ifname, gwtModemConfig.getHoldoff());
            this.properties.setModemPppNum(this.ifname, gwtModemConfig.getPppNum());
            this.properties.setModemMaxFail(this.ifname, gwtModemConfig.getMaxFail());
            this.properties.setModemIdle(this.ifname, gwtModemConfig.getIdle());
            this.properties.setModemActiveFilter(this.ifname, gwtModemConfig.getActiveFilter());
            this.properties.setModemLpcEchoInterval(this.ifname, gwtModemConfig.getLcpEchoInterval());
            this.properties.setModemLpcEchoFailure(this.ifname, gwtModemConfig.getLcpEchoFailure());
            this.properties.setModemDiversityEnabled(this.ifname, gwtModemConfig.isDiversityEnabled());
            this.properties.setModemApn(this.ifname, gwtModemConfig.getApn());
            this.properties.setUsbProductName(this.ifname, gwtModemConfig.getModemId());
            this.properties.setUsbVendorName(this.ifname, gwtModemConfig.getManufacturer());
            this.properties.setUsbProductId(this.ifname, gwtModemConfig.getModel());
            this.properties.setUsbDevicePath(this.ifname, gwtModemConfig.getHwUsbDevice());
        }
    }

    private void setModemGpsProperties() {
        if (this.gwtConfig instanceof GwtModemInterfaceConfig) {
            GwtModemInterfaceConfig gwtModemConfig = (GwtModemInterfaceConfig) this.gwtConfig;

            this.properties.setModemGpsEnabled(this.ifname, gwtModemConfig.isGpsEnabled());
        }
    }

    private void setAdvancedProperties() {
        this.properties.setIp4Mtu(this.ifname,
                Objects.nonNull(this.gwtConfig.getMtu()) ? this.gwtConfig.getMtu() : DEFAULT_MTU);
        this.properties.setIp6Mtu(this.ifname,
                Objects.nonNull(this.gwtConfig.getIpv6Mtu()) ? this.gwtConfig.getIpv6Mtu() : DEFAULT_MTU);
        this.properties.setPromisc(this.ifname,
        		Objects.nonNull(this.gwtConfig.getPromisc()) ? this.gwtConfig.getPromisc() : DEFAULT_PROMISC);
    }

    private static GwtNetInterfaceConfig getConfigsAndStatuses(String ifName) throws GwtKuraException, KuraException {
        NetworkConfigurationServiceAdapter configuration = new NetworkConfigurationServiceAdapter();
        return configuration.getGwtNetInterfaceConfig(ifName);
    }

}
