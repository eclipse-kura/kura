/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Jens Reimann <jreimann@redhat.com>
 *******************************************************************************/
package org.eclipse.kura.web.server.net;

import static org.eclipse.kura.web.server.util.GwtServerUtil.PASSWORD_PLACEHOLDER;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.core.util.NetUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkAdminService;
import org.eclipse.kura.net.dhcp.DhcpLease;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4;
import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.eclipse.kura.net.modem.ModemConnectionStatus;
import org.eclipse.kura.net.modem.ModemInterface;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;
import org.eclipse.kura.net.modem.ModemManagerService;
import org.eclipse.kura.net.modem.ModemManagerService.ModemFunction;
import org.eclipse.kura.net.modem.ModemPdpContext;
import org.eclipse.kura.net.modem.ModemPdpContextType;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.wifi.WifiBgscan;
import org.eclipse.kura.net.wifi.WifiBgscanModule;
import org.eclipse.kura.net.wifi.WifiChannel;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiClientMonitorService;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiHotspotInfo;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.server.util.KuraExceptionHandler;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.GwtSafeHtmlUtils;
import org.eclipse.kura.web.shared.model.GwtDhcpLease;
import org.eclipse.kura.web.shared.model.GwtFirewallNatEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallOpenPortEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallPortForwardEntry;
import org.eclipse.kura.web.shared.model.GwtModemAuthType;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtModemPdpEntry;
import org.eclipse.kura.web.shared.model.GwtModemPdpType;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetIfType;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetRouterMode;
import org.eclipse.kura.web.shared.model.GwtWifiBgscanModule;
import org.eclipse.kura.web.shared.model.GwtWifiChannelFrequency;
import org.eclipse.kura.web.shared.model.GwtWifiCiphers;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiHotspotEntry;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiRadioMode;
import org.eclipse.kura.web.shared.model.GwtWifiSecurity;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class GwtNetworkServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(GwtNetworkServiceImpl.class);
    private static final String FIREWALL_CONFIGURATION_SERVICE_PID = "org.eclipse.kura.net.admin.FirewallConfigurationService";
    private static final String ENABLED = "enabled";
    private static final String UNKNOWN_NETWORK = "0.0.0.0/0";

    public static List<GwtNetInterfaceConfig> findNetInterfaceConfigurations(boolean recompute)
            throws GwtKuraException {
        List<GwtNetInterfaceConfig> result = privateFindNetInterfaceConfigurations(recompute);

        return GwtServerUtil.replaceNetworkConfigListSensitivePasswordsWithPlaceholder(result);
    }

    private static List<GwtNetInterfaceConfig> privateFindNetInterfaceConfigurations(boolean recompute)
            throws GwtKuraException {

        logger.debug("Starting");

        List<GwtNetInterfaceConfig> gwtNetConfigs = new ArrayList<>();
        NetworkAdminService nas = null;
        try {
            nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        } catch (Exception t) {
            logger.warn("Exception", t);
            return gwtNetConfigs;
        }

        ModemManagerService modemManagerService = null;
        try {
            modemManagerService = ServiceLocator.getInstance().getService(ModemManagerService.class);
        } catch (Exception t) {
            logger.warn("{ModemManagerService} Exception", t);
        }

        WifiClientMonitorService wifiClientMonitorService = null;
        try {
            wifiClientMonitorService = ServiceLocator.getInstance().getService(WifiClientMonitorService.class);
        } catch (Exception t) {
            logger.warn("{WifiClientMonitorService} Exception", t);
        }

        try {

            GwtNetInterfaceConfig gwtNetConfig;
            for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netIfConfig : nas
                    .getNetworkInterfaceConfigs(recompute)) {
                logger.debug("Getting config for {} with type {}", netIfConfig.getName(), netIfConfig.getType());

                logger.debug("Interface State: {}", netIfConfig.getState());

                gwtNetConfig = createGwtNetConfig(netIfConfig);

                gwtNetConfig.setName(netIfConfig.getName());
                gwtNetConfig.setInterfaceName(netIfConfig.getName());
                gwtNetConfig.setHwName(netIfConfig.getName());
                if (netIfConfig.getType() != null) {
                    gwtNetConfig.setHwType(netIfConfig.getType().name());
                }
                if (netIfConfig.getState() != null) {
                    gwtNetConfig.setHwState(netIfConfig.getState().name());
                }
                logger.debug("MAC: {}", NetUtil.hardwareAddressToString(netIfConfig.getHardwareAddress()));
                gwtNetConfig.setHwAddress(NetUtil.hardwareAddressToString(netIfConfig.getHardwareAddress()));
                gwtNetConfig.setHwDriver(netIfConfig.getDriver());
                gwtNetConfig.setHwDriverVersion(netIfConfig.getDriverVersion());
                gwtNetConfig.setHwFirmware(netIfConfig.getFirmwareVersion());
                gwtNetConfig.setHwMTU(netIfConfig.getMTU());
                if (netIfConfig.getUsbDevice() != null) {
                    gwtNetConfig.setHwUsbDevice(netIfConfig.getUsbDevice().getUsbDevicePath());
                } else {
                    gwtNetConfig.setHwUsbDevice("N/A");
                }

                NetInterfaceAddressConfig addressConfig = ((AbstractNetInterface<?>) netIfConfig)
                        .getNetInterfaceAddressConfig();
                if (addressConfig != null) {
                    // current status - not configuration!
                    if (addressConfig.getAddress() != null) {
                        logger.debug("current address: {}", addressConfig.getAddress().getHostAddress());
                    }
                    if (addressConfig.getNetworkPrefixLength() >= 0 && addressConfig.getNetworkPrefixLength() <= 32) {
                        logger.debug("current prefix length: {}", addressConfig.getNetworkPrefixLength());
                    }
                    if (addressConfig.getNetmask() != null) {
                        logger.debug("current netmask: {}", addressConfig.getNetmask().getHostAddress());
                    }

                    List<NetConfig> netConfigs = addressConfig.getConfigs();
                    if (netConfigs != null) {
                        boolean isNatEnabled = false;
                        boolean isDhcpServerEnabled = false;

                        for (NetConfig netConfig : netConfigs) {
                            if (netConfig instanceof NetConfigIP4) {
                                logger.debug("Setting up NetConfigIP4 with status {}",
                                        ((NetConfigIP4) netConfig).getStatus());

                                // we are enabled - for LAN or WAN?
                                if (((NetConfigIP4) netConfig)
                                        .getStatus() == NetInterfaceStatus.netIPv4StatusEnabledLAN) {
                                    gwtNetConfig.setStatus(GwtNetIfStatus.netIPv4StatusEnabledLAN.name());
                                } else if (((NetConfigIP4) netConfig)
                                        .getStatus() == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
                                    gwtNetConfig.setStatus(GwtNetIfStatus.netIPv4StatusEnabledWAN.name());
                                } else if (((NetConfigIP4) netConfig)
                                        .getStatus() == NetInterfaceStatus.netIPv4StatusUnmanaged) {
                                    gwtNetConfig.setStatus(GwtNetIfStatus.netIPv4StatusUnmanaged.name());
                                } else if (((NetConfigIP4) netConfig)
                                        .getStatus() == NetInterfaceStatus.netIPv4StatusL2Only) {
                                    gwtNetConfig.setStatus(GwtNetIfStatus.netIPv4StatusL2Only.name());
                                } else {
                                    gwtNetConfig.setStatus(GwtNetIfStatus.netIPv4StatusDisabled.name());
                                }

                                if (((NetConfigIP4) netConfig).isDhcp() || netIfConfig.isLoopback()) {
                                    if (((NetConfigIP4) netConfig).isDhcp()) {
                                        gwtNetConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());
                                    }

                                    if (netIfConfig.isLoopback()) {
                                        gwtNetConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeManual.name());
                                    }

                                    // since DHCP or loopback - populate current data
                                    if (addressConfig.getAddress() != null) {
                                        gwtNetConfig.setIpAddress(addressConfig.getAddress().getHostAddress());
                                    } else {
                                        gwtNetConfig.setIpAddress("");
                                    }
                                    if (addressConfig.getNetworkPrefixLength() > 0
                                            && addressConfig.getNetworkPrefixLength() <= 32) {
                                        gwtNetConfig.setSubnetMask(NetworkUtil
                                                .getNetmaskStringForm(addressConfig.getNetworkPrefixLength()));
                                    } else {
                                        if (addressConfig.getNetmask() != null) {
                                            gwtNetConfig.setSubnetMask(addressConfig.getNetmask().getHostAddress());
                                        } else {
                                            gwtNetConfig.setSubnetMask("");
                                        }
                                    }
                                    if (addressConfig.getGateway() != null) {
                                        gwtNetConfig.setGateway(addressConfig.getGateway().getHostAddress());
                                    } else {
                                        gwtNetConfig.setGateway("");
                                    }

                                    // DHCP supplied DNS servers
                                    StringBuilder sb = new StringBuilder();
                                    List<? extends IPAddress> dnsServers = addressConfig.getDnsServers();
                                    if (dnsServers != null && !dnsServers.isEmpty()) {
                                        String sep = "";
                                        for (IPAddress dnsServer : dnsServers) {
                                            sb.append(sep).append(dnsServer.getHostAddress());
                                            sep = "\n";
                                        }

                                        logger.debug("DNS Servers: {}", sb);
                                        gwtNetConfig.setReadOnlyDnsServers(sb.toString());
                                    } else {
                                        logger.debug("DNS Servers: [empty String]");
                                        gwtNetConfig.setReadOnlyDnsServers("");
                                    }
                                } else {
                                    gwtNetConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeManual.name());

                                    // since STATIC - populate with configured values
                                    // TODO - should we throw an error if current state doesn't match configuration?
                                    if (((NetConfigIP4) netConfig).getAddress() != null) {
                                        gwtNetConfig
                                                .setIpAddress(((NetConfigIP4) netConfig).getAddress().getHostAddress());
                                    } else {
                                        gwtNetConfig.setIpAddress("");
                                    }
                                    if (((NetConfigIP4) netConfig).getSubnetMask() != null) {
                                        gwtNetConfig.setSubnetMask(
                                                ((NetConfigIP4) netConfig).getSubnetMask().getHostAddress());
                                    } else {
                                        gwtNetConfig.setSubnetMask("");
                                    }
                                    if (((NetConfigIP4) netConfig).getGateway() != null) {
                                        logger.debug("Gateway for {} is: {}", netIfConfig.getName(),
                                                ((NetConfigIP4) netConfig).getGateway().getHostAddress());
                                        gwtNetConfig
                                                .setGateway(((NetConfigIP4) netConfig).getGateway().getHostAddress());
                                    } else {
                                        gwtNetConfig.setGateway("");
                                    }
                                }

                                // Custom DNS servers
                                StringBuilder sb = new StringBuilder();
                                List<IP4Address> dnsServers = ((NetConfigIP4) netConfig).getDnsServers();
                                if (dnsServers != null && !dnsServers.isEmpty()) {
                                    for (IP4Address dnsServer : dnsServers) {
                                        if (!dnsServer.getHostAddress().equals("127.0.0.1")) {
                                            sb.append(' ').append(dnsServer.getHostAddress());
                                        }
                                    }
                                    logger.debug("DNS Servers: {}", sb);
                                    gwtNetConfig.setDnsServers(sb.toString().trim());
                                } else {
                                    logger.debug("DNS Servers: [empty String]");
                                    gwtNetConfig.setDnsServers("");
                                }
                            }

                            // The NetConfigIP4 section above should also apply for a wireless interface
                            // Note that this section is used to configure both a station config and an
                            // access point
                            // config
                            if (netConfig instanceof WifiConfig) {
                                logger.debug("Setting up WifiConfigIP4");

                                WifiConfig wifiConfig = (WifiConfig) netConfig;
                                GwtWifiConfig gwtWifiConfig = new GwtWifiConfig();

                                // mode
                                if (wifiConfig.getMode() == WifiMode.MASTER) {
                                    gwtWifiConfig
                                            .setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name());

                                    // set as the access point config for this interface
                                    ((GwtWifiNetInterfaceConfig) gwtNetConfig).setAccessPointWifiConfig(gwtWifiConfig);
                                } else if (wifiConfig.getMode() == WifiMode.INFRA) {
                                    gwtWifiConfig
                                            .setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeStation.name());

                                    // set as the station config for this interface
                                    ((GwtWifiNetInterfaceConfig) gwtNetConfig).setStationWifiConfig(gwtWifiConfig);
                                } else if (wifiConfig.getMode() == WifiMode.ADHOC) {
                                    gwtWifiConfig.setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeAdHoc.name());

                                    // set as the adhoc config for this interface
                                    ((GwtWifiNetInterfaceConfig) gwtNetConfig).setAdhocWifiConfig(gwtWifiConfig);
                                }

                                // ssid
                                gwtWifiConfig.setWirelessSsid(wifiConfig.getSSID());

                                // driver
                                gwtWifiConfig.setDriver(wifiConfig.getDriver());

                                // security
                                switch (wifiConfig.getSecurity()) {
                                    case SECURITY_WPA:
                                        gwtWifiConfig.setSecurity(GwtWifiSecurity.netWifiSecurityWPA.name());
                                        break;
                                    case SECURITY_WPA2:
                                        gwtWifiConfig.setSecurity(GwtWifiSecurity.netWifiSecurityWPA2.name());
                                        break;
                                    case SECURITY_WPA_WPA2:
                                        gwtWifiConfig.setSecurity(GwtWifiSecurity.netWifiSecurityWPA_WPA2.name());
                                        break;
                                    case SECURITY_WEP:
                                        gwtWifiConfig.setSecurity(GwtWifiSecurity.netWifiSecurityWEP.name());
                                        break;
                                    case SECURITY_NONE:
                                    default:
                                        gwtWifiConfig.setSecurity(GwtWifiSecurity.netWifiSecurityNONE.name());
                                        break;
                                }

                                if (wifiConfig.getPairwiseCiphers() == WifiCiphers.CCMP_TKIP) {
                                    gwtWifiConfig.setPairwiseCiphers(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name());
                                } else if (wifiConfig.getPairwiseCiphers() == WifiCiphers.TKIP) {
                                    gwtWifiConfig.setPairwiseCiphers(GwtWifiCiphers.netWifiCiphers_TKIP.name());
                                } else if (wifiConfig.getPairwiseCiphers() == WifiCiphers.CCMP) {
                                    gwtWifiConfig.setPairwiseCiphers(GwtWifiCiphers.netWifiCiphers_CCMP.name());
                                }

                                if (wifiConfig.getGroupCiphers() == WifiCiphers.CCMP_TKIP) {
                                    gwtWifiConfig.setGroupCiphers(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name());
                                } else if (wifiConfig.getGroupCiphers() == WifiCiphers.TKIP) {
                                    gwtWifiConfig.setGroupCiphers(GwtWifiCiphers.netWifiCiphers_TKIP.name());
                                } else if (wifiConfig.getGroupCiphers() == WifiCiphers.CCMP) {
                                    gwtWifiConfig.setGroupCiphers(GwtWifiCiphers.netWifiCiphers_CCMP.name());
                                }

                                // bgscan
                                WifiBgscan wifiBgscan = wifiConfig.getBgscan();
                                if (wifiBgscan != null) {
                                    if (wifiBgscan.getModule() == WifiBgscanModule.NONE) {
                                        gwtWifiConfig
                                                .setBgscanModule(GwtWifiBgscanModule.netWifiBgscanMode_NONE.name());
                                    } else if (wifiBgscan.getModule() == WifiBgscanModule.SIMPLE) {
                                        gwtWifiConfig
                                                .setBgscanModule(GwtWifiBgscanModule.netWifiBgscanMode_SIMPLE.name());
                                    } else if (wifiBgscan.getModule() == WifiBgscanModule.LEARN) {
                                        gwtWifiConfig
                                                .setBgscanModule(GwtWifiBgscanModule.netWifiBgscanMode_LEARN.name());
                                    }
                                    gwtWifiConfig.setBgscanRssiThreshold(wifiBgscan.getRssiThreshold());
                                    gwtWifiConfig.setBgscanShortInterval(wifiBgscan.getShortInterval());
                                    gwtWifiConfig.setBgscanLongInterval(wifiBgscan.getLongInterval());
                                }

                                // ping access point?
                                gwtWifiConfig.setPingAccessPoint(wifiConfig.pingAccessPoint());

                                // ignore SSID?
                                gwtWifiConfig.setIgnoreSSID(wifiConfig.ignoreSSID());

                                // passkey
                                Password psswd = wifiConfig.getPasskey();
                                if (psswd != null) {
                                    String password = new String(psswd.getPassword());
                                    gwtWifiConfig.setPassword(password);
                                }

                                // channel
                                int[] channels = wifiConfig.getChannels();
                                if (channels != null) {
                                    ArrayList<Integer> alChannels = new ArrayList<>();
                                    for (int channel : channels) {
                                        alChannels.add(channel);
                                    }
                                    gwtWifiConfig.setChannels(alChannels);
                                }

                                // radio mode
                                GwtWifiRadioMode gwtWifiRadioMode = null;
                                if (wifiConfig.getRadioMode() != null) {
                                    switch (wifiConfig.getRadioMode()) {
                                        case RADIO_MODE_80211a:
                                            gwtWifiRadioMode = GwtWifiRadioMode.netWifiRadioModeA;
                                            break;
                                        case RADIO_MODE_80211b:
                                            gwtWifiRadioMode = GwtWifiRadioMode.netWifiRadioModeB;
                                            break;
                                        case RADIO_MODE_80211g:
                                            gwtWifiRadioMode = GwtWifiRadioMode.netWifiRadioModeBG;
                                            break;
                                        case RADIO_MODE_80211nHT20:
                                        case RADIO_MODE_80211nHT40above:
                                        case RADIO_MODE_80211nHT40below:
                                            gwtWifiRadioMode = GwtWifiRadioMode.netWifiRadioModeBGN;
                                            break;
                                        case RADIO_MODE_80211_AC:
                                            gwtWifiRadioMode = GwtWifiRadioMode.netWifiRadioModeANAC;
                                            break;
                                        default:
                                            break;
                                    }
                                }
                                if (gwtWifiRadioMode != null) {
                                    gwtWifiConfig.setRadioMode(gwtWifiRadioMode.name());
                                }

                                // set the currently active mode based on the address config
                                WifiMode activeWirelessMode = ((WifiInterfaceAddressConfig) addressConfig).getMode();
                                if (activeWirelessMode == WifiMode.MASTER) {
                                    ((GwtWifiNetInterfaceConfig) gwtNetConfig)
                                            .setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name());
                                    gwtNetConfig.setHwRssi("N/A");
                                } else if (activeWirelessMode == WifiMode.INFRA) {
                                    ((GwtWifiNetInterfaceConfig) gwtNetConfig)
                                            .setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeStation.name());
                                    if (wifiClientMonitorService != null
                                            && wifiConfig.getMode().equals(WifiMode.INFRA)) {
                                        if (gwtNetConfig.getStatus().equals(GwtNetIfStatus.netIPv4StatusDisabled.name())
                                                || gwtNetConfig.getStatus()
                                                        .equals(GwtNetIfStatus.netIPv4StatusUnmanaged.name())) {
                                            gwtNetConfig.setHwRssi("N/A");
                                        } else {
                                            readRssi(wifiClientMonitorService, gwtNetConfig, netIfConfig.getName(),
                                                    wifiConfig.getSSID(), recompute);
                                        }
                                    }
                                } else if (activeWirelessMode == WifiMode.ADHOC) {
                                    ((GwtWifiNetInterfaceConfig) gwtNetConfig)
                                            .setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeAdHoc.name());
                                    gwtNetConfig.setHwRssi("N/A");
                                } else {
                                    ((GwtWifiNetInterfaceConfig) gwtNetConfig)
                                            .setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeDisabled.name());
                                    gwtNetConfig.setHwRssi("N/A");
                                }
                            }

                            if (netConfig instanceof ModemConfig) {
                                logger.debug("Setting up ModemConfig");

                                ModemConfig modemConfig = (ModemConfig) netConfig;
                                GwtModemInterfaceConfig gwtModemConfig = (GwtModemInterfaceConfig) gwtNetConfig;

                                // gwtModemConfig.setHwSerial(((ModemInterface)netIfConfig).getSerialNumber());

                                if (modemManagerService != null) {
                                    UsbDevice usbDevice = netIfConfig.getUsbDevice();
                                    String modemServiceId = null;
                                    if (usbDevice != null) {
                                        modemServiceId = netIfConfig.getUsbDevice().getUsbPort();
                                    }

                                    if (modemServiceId != null) {
                                        final GwtNetInterfaceConfig currentConfig = gwtNetConfig;

                                        modemManagerService.withModemService(modemServiceId, m -> {
                                            if (!m.isPresent()) {
                                                return (Void) null;
                                            }

                                            final CellularModem cellModemService = m.get();

                                            try {
                                                String imei = cellModemService.getSerialNumber();
                                                logger.debug("Setting IMEI/MEID to {}", imei);
                                                gwtModemConfig.setHwSerial(imei);
                                            } catch (KuraException e) {
                                                logger.warn("Failed to get IMEI from modem", e);
                                            }

                                            try {
                                                int rssi = cellModemService.getSignalStrength(recompute);
                                                logger.debug("Setting Received Signal Strength to {}", rssi);
                                                gwtModemConfig.setHwRssi(Integer.toString(rssi));
                                            } catch (KuraException e) {
                                                logger.warn("Failed to get Received Signal Strength from modem", e);
                                            }

                                            try {
                                                String iccid = cellModemService.getIntegratedCirquitCardId(recompute);
                                                logger.debug("Setting ICCID to {}", iccid);
                                                gwtModemConfig.setHwICCID(iccid);
                                            } catch (KuraException e) {
                                                logger.warn("Failed to get ICCID from modem", e);
                                            }

                                            try {
                                                String imsi = cellModemService.getMobileSubscriberIdentity(recompute);
                                                logger.debug("Setting IMSI to {}", imsi);
                                                gwtModemConfig.setHwIMSI(imsi);
                                            } catch (KuraException e) {
                                                logger.warn("Failed to get IMSI from modem", e);
                                            }

                                            try {
                                                ModemRegistrationStatus registration = cellModemService
                                                        .getRegistrationStatus(recompute);
                                                logger.debug("Setting Registration Status to {}", registration.name());
                                                gwtModemConfig.setHwRegistration(registration.name());
                                            } catch (KuraException e) {
                                                logger.warn("Failed to get Registration from modem", e);
                                            }

                                            try {
                                                String plmnid = cellModemService.getPLMNID();
                                                logger.debug("Setting PLMNID to {}", plmnid);
                                                gwtModemConfig.setHwPLMNID(plmnid);
                                            } catch (KuraException e) {
                                                logger.warn("Failed to get PLMNID from modem", e);
                                            }

                                            try {
                                                String network = cellModemService.getNetworkName();
                                                logger.debug("Setting Network to {}", network);
                                                gwtModemConfig.setHwNetwork(network);
                                            } catch (KuraException e) {
                                                logger.warn("Failed to get Network from modem", e);
                                            }

                                            try {
                                                String radio = cellModemService.getRadio();
                                                logger.debug("Setting Radio to {}", radio);
                                                gwtModemConfig.setHwRadio(radio);
                                            } catch (KuraException e) {
                                                logger.warn("Failed to get Radio from modem", e);
                                            }

                                            try {
                                                String band = cellModemService.getBand();
                                                logger.debug("Setting Band to {}", band);
                                                gwtModemConfig.setHwBand(band);
                                            } catch (KuraException e) {
                                                logger.warn("Failed to get Band from modem", e);
                                            }

                                            try {
                                                String lac = cellModemService.getLAC();
                                                logger.debug("Setting Band to {}", lac);
                                                gwtModemConfig.setHwLAC(lac);
                                            } catch (KuraException e) {
                                                logger.warn("Failed to get LAC from modem", e);
                                            }

                                            try {
                                                String ci = cellModemService.getCI();
                                                logger.debug("Setting CI to {}", ci);
                                                gwtModemConfig.setHwCI(ci);
                                            } catch (KuraException e) {
                                                logger.warn("Failed to get CI from modem", e);
                                            }

                                            try {
                                                String sModel = cellModemService.getModel();
                                                ((GwtModemInterfaceConfig) currentConfig).setModel(sModel);
                                            } catch (KuraException e) {
                                                logger.warn("Failed to get model information from modem", e);
                                            }

                                            try {
                                                boolean gpsSupported = cellModemService.isGpsSupported();
                                                logger.debug("Setting GPS supported to {}", gpsSupported);
                                                ((GwtModemInterfaceConfig) currentConfig).setGpsSupported(gpsSupported);
                                            } catch (KuraException e) {
                                                logger.warn("Failed to get GPS supported from modem", e);
                                            }

                                            try {
                                                String firmwareVersion = cellModemService.getFirmwareVersion();
                                                logger.debug("Setting firwmare version to {}", firmwareVersion);
                                                ((GwtModemInterfaceConfig) currentConfig)
                                                        .setHwFirmware(firmwareVersion);
                                            } catch (KuraException e) {
                                                logger.warn("Failed to get firmware version from modem", e);
                                            }

                                            return (Void) null;
                                        });
                                    }
                                }

                                // set as DHCP - populate current address
                                gwtModemConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());
                                if (addressConfig.getAddress() != null) {
                                    gwtModemConfig.setIpAddress(addressConfig.getAddress().getHostAddress());
                                }
                                if (addressConfig.getNetmask() != null) {
                                    gwtModemConfig.setSubnetMask(addressConfig.getNetmask().getHostAddress());
                                }

                                gwtModemConfig.setDialString(modemConfig.getDialString());

                                AuthType authType = modemConfig.getAuthType();
                                if (authType == AuthType.AUTO) {
                                    gwtModemConfig.setAuthType(GwtModemAuthType.netModemAuthAUTO);
                                } else if (authType == AuthType.CHAP) {
                                    gwtModemConfig.setAuthType(GwtModemAuthType.netModemAuthCHAP);
                                } else if (authType == AuthType.PAP) {
                                    gwtModemConfig.setAuthType(GwtModemAuthType.netModemAuthPAP);
                                } else {
                                    gwtModemConfig.setAuthType(GwtModemAuthType.netModemAuthNONE);
                                }

                                // Redefine interface name as ppp name
                                gwtModemConfig.setInterfaceName("ppp" + modemConfig.getPppNumber());

                                gwtModemConfig.setUsername(modemConfig.getUsername());

                                gwtModemConfig.setPassword(modemConfig.getPasswordAsPassword().toString());

                                gwtModemConfig.setPppNum(modemConfig.getPppNumber());

                                gwtModemConfig.setResetTimeout(modemConfig.getResetTimeout());

                                gwtModemConfig.setPersist(modemConfig.isPersist());

                                gwtModemConfig.setHoldoff(modemConfig.getHoldoff());

                                gwtModemConfig.setMaxFail(modemConfig.getMaxFail());

                                gwtModemConfig.setIdle(modemConfig.getIdle());

                                gwtModemConfig.setActiveFilter(modemConfig.getActiveFilter());

                                gwtModemConfig.setLcpEchoInterval(modemConfig.getLcpEchoInterval());

                                gwtModemConfig.setLcpEchoFailure(modemConfig.getLcpEchoFailure());

                                gwtModemConfig.setGpsEnabled(modemConfig.isGpsEnabled());
                                gwtModemConfig.setDiversityEnabled(modemConfig.isDiversityEnabled());

                                gwtModemConfig.setProfileID(modemConfig.getProfileID());

                                PdpType pdpType = modemConfig.getPdpType();
                                if (pdpType == PdpType.IP) {
                                    gwtModemConfig.setPdpType(GwtModemPdpType.netModemPdpIP);
                                } else if (pdpType == PdpType.PPP) {
                                    gwtModemConfig.setPdpType(GwtModemPdpType.netModemPdpPPP);
                                } else if (pdpType == PdpType.IPv6) {
                                    gwtModemConfig.setPdpType(GwtModemPdpType.netModemPdpIPv6);
                                } else {
                                    gwtModemConfig.setPdpType(GwtModemPdpType.netModemPdpUnknown);
                                }

                                gwtModemConfig.setApn(modemConfig.getApn());

                                gwtModemConfig.setDataCompression(modemConfig.getDataCompression());

                                gwtModemConfig.setHeaderCompression(modemConfig.getHeaderCompression());

                                ModemConnectionStatus connectionStatus = ((ModemInterfaceAddressConfig) addressConfig)
                                        .getConnectionStatus();
                                if (connectionStatus == ModemConnectionStatus.DISCONNECTED) {
                                    gwtModemConfig.setHwState(NetInterfaceState.DISCONNECTED.name());
                                } else if (connectionStatus == ModemConnectionStatus.CONNECTING) {
                                    gwtModemConfig.setHwState(NetInterfaceState.IP_CONFIG.name());
                                } else if (connectionStatus == ModemConnectionStatus.CONNECTED) {
                                    gwtModemConfig.setHwState(NetInterfaceState.ACTIVATED.name());
                                } else {
                                    gwtModemConfig.setHwState(NetInterfaceState.UNKNOWN.name());
                                }

                                gwtModemConfig.setConnectionType(
                                        ((ModemInterfaceAddressConfig) addressConfig).getConnectionType().name());
                            }

                            if (netConfig instanceof DhcpServerConfigIP4) {
                                logger.debug("Setting up DhcpServerConfigIP4: {} to {}",
                                        ((DhcpServerConfigIP4) netConfig).getRangeStart().getHostAddress(),
                                        ((DhcpServerConfigIP4) netConfig).getRangeEnd().getHostAddress());
                                logger.debug("Setting up DhcpServerConfigIP4: {}", netConfig);

                                isDhcpServerEnabled = ((DhcpServerConfigIP4) netConfig).isEnabled();

                                gwtNetConfig.setRouterDhcpBeginAddress(
                                        ((DhcpServerConfigIP4) netConfig).getRangeStart().getHostAddress());
                                gwtNetConfig.setRouterDhcpEndAddress(
                                        ((DhcpServerConfigIP4) netConfig).getRangeEnd().getHostAddress());
                                gwtNetConfig.setRouterDhcpSubnetMask(
                                        ((DhcpServerConfigIP4) netConfig).getSubnetMask().getHostAddress());
                                gwtNetConfig.setRouterDhcpDefaultLease(
                                        ((DhcpServerConfigIP4) netConfig).getDefaultLeaseTime());
                                gwtNetConfig
                                        .setRouterDhcpMaxLease(((DhcpServerConfigIP4) netConfig).getMaximumLeaseTime());
                                gwtNetConfig.setRouterDnsPass(((DhcpServerConfigIP4) netConfig).isPassDns());
                            }

                            if (netConfig instanceof FirewallAutoNatConfig) {
                                logger.debug("Setting up FirewallAutoNatConfig");

                                isNatEnabled = true;
                            }

                            // TODO - only dealing with IPv4 right now
                        }

                        // set up the DHCP and NAT config
                        if (isDhcpServerEnabled && isNatEnabled) {
                            logger.debug("setting router mode to DHCP and NAT");
                            gwtNetConfig.setRouterMode(GwtNetRouterMode.netRouterDchpNat.name());
                        } else if (isDhcpServerEnabled && !isNatEnabled) {
                            logger.debug("setting router mode to DHCP only");
                            gwtNetConfig.setRouterMode(GwtNetRouterMode.netRouterDchp.name());
                        } else if (!isDhcpServerEnabled && isNatEnabled) {
                            logger.debug("setting router mode to NAT only");
                            gwtNetConfig.setRouterMode(GwtNetRouterMode.netRouterNat.name());
                        } else {
                            logger.debug("setting router mode to disabled");
                            gwtNetConfig.setRouterMode(GwtNetRouterMode.netRouterOff.name());
                        }
                    }
                }

                gwtNetConfigs.add(gwtNetConfig);
            }
        } catch (Throwable t) {
            KuraExceptionHandler.handle(t);
        }

        logger.debug("Returning");
        return gwtNetConfigs;
    }

    private static void readRssi(WifiClientMonitorService wifiClientMonitorService, GwtNetInterfaceConfig gwtNetConfig,
            String interfaceName, String ssid, boolean recompute) {
        try {
            int rssi = wifiClientMonitorService.getSignalLevel(interfaceName, ssid, recompute);
            logger.debug("Setting Received Signal Strength to {}", rssi);
            gwtNetConfig.setHwRssi(Integer.toString(rssi));
        } catch (KuraException e) {
            logger.warn("Failed", e);
        }
    }

    private static GwtNetInterfaceConfig createGwtNetConfig(
            NetInterfaceConfig<? extends NetInterfaceAddressConfig> netIfConfig) {
        GwtNetInterfaceConfig gwtNetConfig;
        if (netIfConfig.getType() == NetInterfaceType.WIFI) {
            gwtNetConfig = new GwtWifiNetInterfaceConfig();
        } else if (netIfConfig.getType() == NetInterfaceType.MODEM) {
            gwtNetConfig = new GwtModemInterfaceConfig();
            ((GwtModemInterfaceConfig) gwtNetConfig).setModemId(((ModemInterface) netIfConfig).getModemIdentifier());
            ((GwtModemInterfaceConfig) gwtNetConfig).setManufacturer(((ModemInterface) netIfConfig).getManufacturer());
            ((GwtModemInterfaceConfig) gwtNetConfig).setModel(((ModemInterface) netIfConfig).getModel());

            List<String> technologyList = new ArrayList<>();
            List<ModemTechnologyType> technologyTypes = ((ModemInterface) netIfConfig).getTechnologyTypes();
            if (technologyTypes != null) {
                for (ModemTechnologyType techType : technologyTypes) {
                    technologyList.add(techType.name());
                }
            }
            ((GwtModemInterfaceConfig) gwtNetConfig).setNetworkTechnology(technologyList);
        } else {
            gwtNetConfig = new GwtNetInterfaceConfig();
            gwtNetConfig.setHwRssi("N/A");
        }
        return gwtNetConfig;
    }

    public static void updateNetInterfaceConfigurations(GwtNetInterfaceConfig config) throws GwtKuraException {
        ConfigurationService configurationService = ServiceLocator.getInstance().getService(ConfigurationService.class);
        Map<String, Object> properties = new HashMap<>();
        String basePropName = new StringBuilder("net.interface.").append(config.getName()).append(".config.")
                .toString();

        String status = config.getStatus();
        if (logger.isDebugEnabled()) {
            logger.debug("config.getStatus(): {}", GwtSafeHtmlUtils.htmlEscape(status));
        }
        try {
            NetInterfaceStatus netInterfaceStatus = getNetInterfaceStatus(status);
            properties.put(basePropName + "ip4.status", netInterfaceStatus.name());

            if (config.getHwTypeEnum() == GwtNetIfType.ETHERNET || config.getHwTypeEnum() == GwtNetIfType.WIFI
                    || config.getHwTypeEnum() == GwtNetIfType.MODEM) {
                fillIp4AndDhcpProperties(config, properties, basePropName);
            }

            if (config.getHwTypeEnum() == GwtNetIfType.WIFI && config instanceof GwtWifiNetInterfaceConfig) {
                GwtWifiConfig gwtWifiConfig = ((GwtWifiNetInterfaceConfig) config).getActiveWifiConfig();
                if (gwtWifiConfig != null) {
                    fillWifiProperties(gwtWifiConfig, properties, basePropName, config.getName());
                }
            } else if (config.getHwTypeEnum() == GwtNetIfType.MODEM && config instanceof GwtModemInterfaceConfig) {
                GwtModemInterfaceConfig gwtModemConfig = (GwtModemInterfaceConfig) config;
                fillModemProperties(gwtModemConfig, properties, basePropName, netInterfaceStatus);
            }

            configurationService.updateConfiguration("org.eclipse.kura.net.admin.NetworkConfigurationService",
                    properties, true);

        } catch (UnknownHostException | KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static ArrayList<GwtFirewallOpenPortEntry> findDeviceFirewallOpenPorts() throws GwtKuraException {
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        List<GwtFirewallOpenPortEntry> gwtOpenPortEntries = new ArrayList<>();

        try {
            List<NetConfig> firewallConfigs = nas.getFirewallConfiguration();
            if (firewallConfigs != null && !firewallConfigs.isEmpty()) {
                for (NetConfig netConfig : firewallConfigs) {
                    if (netConfig instanceof FirewallOpenPortConfigIP4) {
                        logger.debug("findDeviceFirewallOpenPorts() :: adding new Open Port Entry: {}",
                                ((FirewallOpenPortConfigIP4) netConfig).getPort());
                        GwtFirewallOpenPortEntry entry = new GwtFirewallOpenPortEntry();
                        if (((FirewallOpenPortConfigIP4) netConfig).getPortRange() != null) {
                            entry.setPortRange(((FirewallOpenPortConfigIP4) netConfig).getPortRange());
                        } else {
                            entry.setPortRange(String.valueOf(((FirewallOpenPortConfigIP4) netConfig).getPort()));
                        }
                        entry.setProtocol(((FirewallOpenPortConfigIP4) netConfig).getProtocol().toString());
                        entry.setPermittedNetwork(((FirewallOpenPortConfigIP4) netConfig).getPermittedNetwork()
                                .getIpAddress().getHostAddress() + "/"
                                + ((FirewallOpenPortConfigIP4) netConfig).getPermittedNetwork().getPrefix());
                        entry.setPermittedInterfaceName(
                                ((FirewallOpenPortConfigIP4) netConfig).getPermittedInterfaceName());
                        entry.setUnpermittedInterfaceName(
                                ((FirewallOpenPortConfigIP4) netConfig).getUnpermittedInterfaceName());
                        entry.setPermittedMAC(((FirewallOpenPortConfigIP4) netConfig).getPermittedMac());
                        entry.setSourcePortRange(((FirewallOpenPortConfigIP4) netConfig).getSourcePortRange());

                        gwtOpenPortEntries.add(entry);
                    }
                }
            }

            return new ArrayList<>(gwtOpenPortEntries);

        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static ArrayList<GwtWifiHotspotEntry> findWifiHotspots(String interfaceName, String wirelessSsid)
            throws GwtKuraException {
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);
        List<GwtWifiHotspotEntry> gwtWifiHotspotsEntries = new ArrayList<>();

        try {
            List<WifiHotspotInfo> wifiHotspotInfoList = nas.getWifiHotspotList(interfaceName);
            if (wifiHotspotInfoList != null) {
                for (WifiHotspotInfo wifiHotspotInfo : wifiHotspotInfoList) {
                    String ssid = wifiHotspotInfo.getSsid();

                    if (wifiHotspotInfo.getChannel() <= systemService.getKuraWifiTopChannel() && ssid != null
                            && !ssid.equals(wirelessSsid)) {
                        GwtWifiHotspotEntry gwtWifiHotspotEntry = new GwtWifiHotspotEntry();
                        gwtWifiHotspotEntry.setMacAddress(wifiHotspotInfo.getMacAddress());
                        gwtWifiHotspotEntry.setSSID(ssid);
                        gwtWifiHotspotEntry.setsignalStrength(wifiHotspotInfo.getSignalLevel());
                        gwtWifiHotspotEntry.setChannel(wifiHotspotInfo.getChannel());
                        gwtWifiHotspotEntry.setFrequency(wifiHotspotInfo.getFrequency());

                        if (wifiHotspotInfo.getSecurity() == WifiSecurity.NONE
                                || wifiHotspotInfo.getSecurity() == WifiSecurity.SECURITY_NONE) {
                            gwtWifiHotspotEntry.setSecurity("None");
                        } else if (wifiHotspotInfo.getSecurity() == WifiSecurity.SECURITY_WEP) {
                            gwtWifiHotspotEntry.setSecurity("WEP");
                        } else if (wifiHotspotInfo.getSecurity() == WifiSecurity.SECURITY_WPA) {
                            gwtWifiHotspotEntry.setSecurity("WPA");
                        } else if (wifiHotspotInfo.getSecurity() == WifiSecurity.SECURITY_WPA2) {
                            gwtWifiHotspotEntry.setSecurity("WPA2");
                        } else if (wifiHotspotInfo.getSecurity() == WifiSecurity.SECURITY_WPA_WPA2) {
                            gwtWifiHotspotEntry.setSecurity("WPA/WPA2");
                        }

                        GwtWifiCiphers gwtPairCiphers = null;
                        GwtWifiCiphers gwtGroupCiphers = null;
                        EnumSet<WifiSecurity> pairCiphers = wifiHotspotInfo.getPairCiphers();
                        Iterator<WifiSecurity> itPairCiphers = pairCiphers.iterator();
                        while (itPairCiphers.hasNext()) {
                            WifiSecurity cipher = itPairCiphers.next();
                            if (gwtPairCiphers == null) {
                                if (cipher == WifiSecurity.PAIR_TKIP) {
                                    gwtPairCiphers = GwtWifiCiphers.netWifiCiphers_TKIP;
                                } else if (cipher == WifiSecurity.PAIR_CCMP) {
                                    gwtPairCiphers = GwtWifiCiphers.netWifiCiphers_CCMP;
                                }
                            } else if (gwtPairCiphers == GwtWifiCiphers.netWifiCiphers_TKIP) {
                                if (cipher == WifiSecurity.PAIR_CCMP) {
                                    gwtPairCiphers = GwtWifiCiphers.netWifiCiphers_CCMP_TKIP;
                                }
                            } else if (gwtPairCiphers == GwtWifiCiphers.netWifiCiphers_CCMP) {
                                if (cipher == WifiSecurity.PAIR_TKIP) {
                                    gwtPairCiphers = GwtWifiCiphers.netWifiCiphers_CCMP_TKIP;
                                }
                            }
                        }

                        EnumSet<WifiSecurity> groupCiphers = wifiHotspotInfo.getGroupCiphers();
                        Iterator<WifiSecurity> itGroupCiphers = groupCiphers.iterator();
                        while (itGroupCiphers.hasNext()) {
                            WifiSecurity cipher = itGroupCiphers.next();
                            if (gwtGroupCiphers == null) {
                                if (cipher == WifiSecurity.GROUP_TKIP) {
                                    gwtGroupCiphers = GwtWifiCiphers.netWifiCiphers_TKIP;
                                } else if (cipher == WifiSecurity.GROUP_CCMP) {
                                    gwtGroupCiphers = GwtWifiCiphers.netWifiCiphers_CCMP;
                                }
                            } else if (gwtGroupCiphers == GwtWifiCiphers.netWifiCiphers_TKIP) {
                                if (cipher == WifiSecurity.GROUP_CCMP) {
                                    gwtGroupCiphers = GwtWifiCiphers.netWifiCiphers_CCMP_TKIP;
                                }
                            } else if (gwtGroupCiphers == GwtWifiCiphers.netWifiCiphers_CCMP) {
                                if (cipher == WifiSecurity.GROUP_TKIP) {
                                    gwtGroupCiphers = GwtWifiCiphers.netWifiCiphers_CCMP_TKIP;
                                }
                            }
                        }

                        if (gwtPairCiphers != null) {
                            gwtWifiHotspotEntry.setPairwiseCiphers(gwtPairCiphers.name());
                        }
                        if (gwtGroupCiphers != null) {
                            gwtWifiHotspotEntry.setGroupCiphers(gwtGroupCiphers.name());
                        }

                        gwtWifiHotspotsEntries.add(gwtWifiHotspotEntry);
                    }
                }
            }
        } catch (Throwable t) {
            KuraExceptionHandler.handle(t);
        }

        return new ArrayList<>(gwtWifiHotspotsEntries);
    }

    public static List<GwtModemPdpEntry> findPdpContextInfo(String interfaceName) throws GwtKuraException {
        ModemManagerService mms = ServiceLocator.getInstance().getService(ModemManagerService.class);

        try {
            return withCellularModem(interfaceName, mms, m -> {
                List<GwtModemPdpEntry> gwtModemPdpEntries = new ArrayList<>();

                if (!m.isPresent()) {
                    return gwtModemPdpEntries;
                }

                final CellularModem modem = m.get();
                List<ModemPdpContext> pdpContextInfo = modem.getPdpContextInfo();
                GwtModemPdpEntry gwtModemPdpEntry;
                int contextInd = 1;
                int firstAvailableContextNum = 0;
                for (ModemPdpContext pdpContextEntry : pdpContextInfo) {
                    gwtModemPdpEntry = new GwtModemPdpEntry();
                    gwtModemPdpEntry.setContextNumber(pdpContextEntry.getNumber());
                    gwtModemPdpEntry.setPdpType(pdpContextEntry.getType().getValue());
                    gwtModemPdpEntry.setApn(pdpContextEntry.getApn());
                    gwtModemPdpEntries.add(gwtModemPdpEntry);
                    if (firstAvailableContextNum == 0 && contextInd < pdpContextEntry.getNumber()) {
                        firstAvailableContextNum = contextInd;
                    }
                    contextInd++;
                }
                if (firstAvailableContextNum == 0) {
                    firstAvailableContextNum = contextInd;
                }
                gwtModemPdpEntry = new GwtModemPdpEntry();
                gwtModemPdpEntry.setContextNumber(firstAvailableContextNum);
                gwtModemPdpEntry.setPdpType(ModemPdpContextType.IP.getValue());
                gwtModemPdpEntry.setApn("Please provide APN for this new PDP context ...");
                gwtModemPdpEntries.add(gwtModemPdpEntry);

                return gwtModemPdpEntries;
            });
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.WARNING, e);
        }
    }

    public static boolean verifyWifiCredentials(String interfaceName, GwtWifiConfig gwtWifiConfig)
            throws GwtKuraException {
        if (interfaceName == null || gwtWifiConfig == null) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_NULL_ARGUMENT);
        }
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        WifiConfig wifiConfig = getWifiConfig(gwtWifiConfig);
        return nas.verifyWifiCredentials(interfaceName, wifiConfig, 60);
    }

    public static ArrayList<GwtFirewallPortForwardEntry> findDeviceFirewallPortForwards() throws GwtKuraException {
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        List<GwtFirewallPortForwardEntry> gwtPortForwardEntries = new ArrayList<>();

        try {
            List<NetConfig> firewallConfigs = nas.getFirewallConfiguration();
            if (firewallConfigs != null && !firewallConfigs.isEmpty()) {
                for (NetConfig netConfig : firewallConfigs) {
                    if (netConfig instanceof FirewallPortForwardConfigIP4) {
                        logger.debug("findDeviceFirewallPortForwards() :: adding new Port Forward Entry");
                        GwtFirewallPortForwardEntry entry = new GwtFirewallPortForwardEntry();
                        entry.setInboundInterface(((FirewallPortForwardConfigIP4) netConfig).getInboundInterface());
                        entry.setOutboundInterface(((FirewallPortForwardConfigIP4) netConfig).getOutboundInterface());
                        entry.setAddress(((FirewallPortForwardConfigIP4) netConfig).getAddress().getHostAddress());
                        entry.setProtocol(((FirewallPortForwardConfigIP4) netConfig).getProtocol().toString());
                        entry.setInPort(((FirewallPortForwardConfigIP4) netConfig).getInPort());
                        entry.setOutPort(((FirewallPortForwardConfigIP4) netConfig).getOutPort());
                        String masquerade = ((FirewallPortForwardConfigIP4) netConfig).isMasquerade() ? "yes" : "no";
                        entry.setMasquerade(masquerade);
                        entry.setPermittedNetwork(
                                ((FirewallPortForwardConfigIP4) netConfig).getPermittedNetwork().toString());
                        entry.setPermittedMAC(((FirewallPortForwardConfigIP4) netConfig).getPermittedMac());
                        entry.setSourcePortRange(((FirewallPortForwardConfigIP4) netConfig).getSourcePortRange());

                        gwtPortForwardEntries.add(entry);
                    }
                }
            }

            return new ArrayList<>(gwtPortForwardEntries);

        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static ArrayList<GwtFirewallNatEntry> findDeviceFirewallNATs() throws GwtKuraException {
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        List<GwtFirewallNatEntry> gwtNatEntries = new ArrayList<>();

        try {
            List<NetConfig> firewallConfigs = nas.getFirewallConfiguration();
            if (firewallConfigs != null && !firewallConfigs.isEmpty()) {
                for (NetConfig netConfig : firewallConfigs) {
                    if (netConfig instanceof FirewallNatConfig) {
                        logger.debug("findDeviceFirewallNATs() :: adding new NAT Entry");
                        GwtFirewallNatEntry entry = new GwtFirewallNatEntry();
                        entry.setInInterface(((FirewallNatConfig) netConfig).getSourceInterface());
                        entry.setOutInterface(((FirewallNatConfig) netConfig).getDestinationInterface());
                        entry.setProtocol(((FirewallNatConfig) netConfig).getProtocol());
                        entry.setSourceNetwork(((FirewallNatConfig) netConfig).getSource());
                        entry.setDestinationNetwork(((FirewallNatConfig) netConfig).getDestination());
                        String masquerade = ((FirewallNatConfig) netConfig).isMasquerade() ? "yes" : "no";
                        entry.setMasquerade(masquerade);
                        gwtNatEntries.add(entry);
                    }
                }
            }

            return new ArrayList<>(gwtNatEntries);

        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static void updateDeviceFirewallOpenPorts(List<GwtFirewallOpenPortEntry> entries) throws GwtKuraException {
        ConfigurationService configurationService = ServiceLocator.getInstance().getService(ConfigurationService.class);
        Map<String, Object> properties = new HashMap<>();
        String openPortsPropName = "firewall.open.ports";
        StringBuilder openPorts = new StringBuilder();

        try {
            for (GwtFirewallOpenPortEntry entry : entries) {
                openPorts.append(entry.getPortRange()).append(",");
                openPorts.append(entry.getProtocol()).append(",");
                if (entry.getPermittedNetwork() == null || entry.getPermittedNetwork().equals(UNKNOWN_NETWORK)) {
                    openPorts.append(UNKNOWN_NETWORK);
                } else {
                    appendNetwork(entry.getPermittedNetwork(), openPorts);
                }
                openPorts.append(",");
                if (entry.getPermittedInterfaceName() != null) {
                    openPorts.append(entry.getPermittedInterfaceName());
                }
                openPorts.append(",");
                if (entry.getUnpermittedInterfaceName() != null) {
                    openPorts.append(entry.getUnpermittedInterfaceName());
                }
                openPorts.append(",");
                if (entry.getPermittedMAC() != null) {
                    openPorts.append(entry.getPermittedMAC());
                }
                openPorts.append(",");
                if (entry.getSourcePortRange() != null) {
                    openPorts.append(entry.getSourcePortRange());
                }
                openPorts.append(",").append("#").append(";");
            }

            properties.put(openPortsPropName, openPorts.toString());
            configurationService.updateConfiguration(FIREWALL_CONFIGURATION_SERVICE_PID, properties, true);
        } catch (KuraException | UnknownHostException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static void updateDeviceFirewallPortForwards(List<GwtFirewallPortForwardEntry> entries)
            throws GwtKuraException {
        ConfigurationService configurationService = ServiceLocator.getInstance().getService(ConfigurationService.class);
        Map<String, Object> properties = new HashMap<>();
        String portForwardingPropName = "firewall.port.forwarding";
        StringBuilder portForwarding = new StringBuilder();

        try {
            for (GwtFirewallPortForwardEntry entry : entries) {
                portForwarding.append(entry.getInboundInterface()).append(",");
                portForwarding.append(entry.getOutboundInterface()).append(",");
                portForwarding.append(((IP4Address) IPAddress.parseHostAddress(entry.getAddress())).getHostAddress())
                        .append(",");
                portForwarding.append(entry.getProtocol()).append(",");
                portForwarding.append(entry.getInPort()).append(",");
                portForwarding.append(entry.getOutPort()).append(",");
                if (entry.getMasquerade().equals("yes")) {
                    portForwarding.append("true");
                } else {
                    portForwarding.append("false");
                }
                portForwarding.append(",");
                if (entry.getPermittedNetwork() == null || entry.getPermittedNetwork().equals(UNKNOWN_NETWORK)) {
                    portForwarding.append(UNKNOWN_NETWORK);
                } else {
                    appendNetwork(entry.getPermittedNetwork(), portForwarding);
                }
                portForwarding.append(",");
                if (entry.getPermittedMAC() != null) {
                    portForwarding.append(entry.getPermittedMAC());
                }
                portForwarding.append(",");
                if (entry.getSourcePortRange() != null) {
                    portForwarding.append(entry.getSourcePortRange());
                }
                portForwarding.append(",").append("#").append(";");
            }

            properties.put(portForwardingPropName, portForwarding.toString());
            configurationService.updateConfiguration(FIREWALL_CONFIGURATION_SERVICE_PID, properties, true);
        } catch (KuraException | UnknownHostException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static void updateDeviceFirewallNATs(List<GwtFirewallNatEntry> entries) throws GwtKuraException {
        ConfigurationService configurationService = ServiceLocator.getInstance().getService(ConfigurationService.class);
        Map<String, Object> properties = new HashMap<>();
        String natPropName = "firewall.nat";
        StringBuilder nat = new StringBuilder();

        try {
            for (GwtFirewallNatEntry entry : entries) {
                nat.append(entry.getInInterface()).append(",");
                nat.append(entry.getOutInterface()).append(",");
                nat.append(entry.getProtocol()).append(",");
                if (UNKNOWN_NETWORK.equals(entry.getSourceNetwork())) {
                    nat.append(UNKNOWN_NETWORK);
                } else {
                    appendNetwork(entry.getSourceNetwork(), nat);
                }
                nat.append(",");
                if (UNKNOWN_NETWORK.equals(entry.getDestinationNetwork())) {
                    nat.append(UNKNOWN_NETWORK);
                } else {
                    appendNetwork(entry.getDestinationNetwork(), nat);
                }
                nat.append(",");
                if (entry.getMasquerade().equals("yes")) {
                    nat.append("true");
                } else {
                    nat.append("false");
                }
                nat.append(",").append("#").append(";");
            }

            properties.put(natPropName, nat.toString());
            configurationService.updateConfiguration(FIREWALL_CONFIGURATION_SERVICE_PID, properties, true);
        } catch (KuraException | UnknownHostException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static void renewDhcpLease(String interfaceName) throws GwtKuraException {
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        try {
            nas.renewDhcpLease(GwtSafeHtmlUtils.htmlEscape(interfaceName));
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static List<GwtWifiChannelFrequency> findFrequencies(String interfaceName, GwtWifiRadioMode radioMode)
            throws GwtKuraException {

        logger.debug("Find Frequency Network Service impl");
        List<GwtWifiChannelFrequency> channels = new ArrayList<>();

        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        try {
            List<WifiChannel> channelFrequencies = nas.getWifiFrequencies(interfaceName);
            boolean hasSystemDFS = nas.isWifiDFS(interfaceName);

            for (WifiChannel channelFreq : channelFrequencies) {
                if (logger.isDebugEnabled())
                    logger.debug(channelFreq.toString());

                boolean channelIsfive5Ghz = channelFreq.getFrequency() > 2501;

                if (radioMode.isFiveGhz() && channelIsfive5Ghz || radioMode.isTwoDotFourGhz() && !channelIsfive5Ghz) {

                    if (Boolean.TRUE.equals(channelFreq.isRadarDetection()) && !hasSystemDFS) {
                        continue;
                    }

                    GwtWifiChannelFrequency channelFrequency = new GwtWifiChannelFrequency();

                    channelFrequency.setChannel(channelFreq.getChannel());
                    channelFrequency.setFrequency(channelFreq.getFrequency());
                    channelFrequency.setNoIrradiation(channelFreq.isNoInitiatingRadiation());
                    channelFrequency.setRadarDetection(channelFreq.isRadarDetection());
                    channelFrequency.setDisabled(channelFreq.isDisabled());

                    channels.add(channelFrequency);

                    logger.debug("Found {} - {} Mhz", channelFrequency.getChannel(), channelFrequency.getFrequency());
                }

            }
            return channels;
        } catch (KuraException e) {
            logger.error("Find Frequency exception");
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static String getWifiCountryCode() throws GwtKuraException {

        logger.info("Get Wifi Country Code impl");
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        try {
            return nas.getWifiCountryCode();
        } catch (KuraException e) {
            logger.error("Get Wifi Country Code exception");
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static boolean isIEEE80211ACSupported(String ifaceName) throws GwtKuraException {
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        try {
            return nas.isWifiIEEE80211AC(ifaceName);
        } catch (KuraException e) {
            logger.error("Ieee80211ac support exception");
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static List<String> getDhcpLeases(String interfaceName) throws GwtKuraException {
        List<String> dhcpLease = new ArrayList<>();

        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        try {
            List<DhcpLease> leases = nas.getDhcpLeases(interfaceName);

            for (DhcpLease dl : leases) {
                GwtDhcpLease dhcp = new GwtDhcpLease();
                dhcp.setMacAddress(dl.getMacAddress());
                dhcp.setIpAddress(dl.getIpAddress());
                dhcp.setHostname(dl.getHostname());
                dhcpLease.add(dhcp.toString());
            }
            return dhcpLease;
        } catch (KuraException e) {
            logger.error("Find Dhcp Lease List Exception");
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    // -------------------------------------------------------------------------------------
    //
    // Private Methods
    //
    // -------------------------------------------------------------------------------------

    private static NetInterfaceStatus getNetInterfaceStatus(String status) {
        NetInterfaceStatus netInterfaceStatus = NetInterfaceStatus.netIPv4StatusDisabled;
        if (status.equals(GwtNetIfStatus.netIPv4StatusUnmanaged.name())) {
            netInterfaceStatus = NetInterfaceStatus.netIPv4StatusUnmanaged;
        } else if (status.equals(GwtNetIfStatus.netIPv4StatusL2Only.name())) {
            netInterfaceStatus = NetInterfaceStatus.netIPv4StatusL2Only;
        } else if (status.equals(GwtNetIfStatus.netIPv4StatusEnabledLAN.name())) {
            netInterfaceStatus = NetInterfaceStatus.netIPv4StatusEnabledLAN;
        } else if (status.equals(GwtNetIfStatus.netIPv4StatusEnabledWAN.name())) {
            netInterfaceStatus = NetInterfaceStatus.netIPv4StatusEnabledWAN;
        }
        return netInterfaceStatus;
    }

    private static void fillIp4AndDhcpProperties(GwtNetInterfaceConfig config, Map<String, Object> properties,
            String basePropName) throws UnknownHostException, KuraException {
        logger.debug("config.getConfigMode(): {}", config.getConfigMode());
        String dhcpClient4PropName = basePropName + "dhcpClient4.enabled";
        String addressPropName = basePropName + "ip4.address";
        String prefixPropName = basePropName + "ip4.prefix";
        String gatewayPropName = basePropName + "ip4.gateway";
        if (GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name().equals(config.getConfigMode())) {
            logger.debug("mode is DHCP");
            properties.put(dhcpClient4PropName, true);
            properties.put(addressPropName, "");
            properties.put(gatewayPropName, "");
        } else {
            logger.debug("mode is STATIC");
            properties.put(dhcpClient4PropName, false);

            if (config.getIpAddress() != null && !config.getIpAddress().isEmpty()) {
                logger.debug("setting address: {}", config.getIpAddress());
                properties.put(addressPropName,
                        ((IP4Address) IPAddress.parseHostAddress(config.getIpAddress())).getHostAddress());
            } else {
                properties.put(addressPropName, "");
            }

            if (config.getSubnetMask() != null && !config.getSubnetMask().isEmpty()) {
                logger.debug("setting subnet mask: {}", config.getSubnetMask());
                short prefix = NetworkUtil.getNetmaskShortForm(
                        ((IP4Address) IPAddress.parseHostAddress(config.getSubnetMask())).getHostAddress());
                properties.put(prefixPropName, prefix);
            }

            if (config.getGateway() != null && !config.getGateway().isEmpty()) {
                logger.debug("setting gateway: {}", config.getGateway());
                properties.put(gatewayPropName,
                        ((IP4Address) IPAddress.parseHostAddress(config.getGateway())).getHostAddress());
            } else {
                properties.put(gatewayPropName, "");
            }
        }

        fillDnsServers(config, properties, basePropName);

        if (GwtNetIfConfigMode.netIPv4ConfigModeManual.name().equals(config.getConfigMode())) {
            fillDhcpAndNatProperties(config, properties, basePropName);
        }
    }

    private static void fillDnsServers(GwtNetInterfaceConfig config, Map<String, Object> properties,
            String basePropName) throws UnknownHostException {
        String regexp = "[\\s,;\\n\\t]+";
        String dnsServerPropName = basePropName + "ip4.dnsServers";
        List<String> dnsServers = Arrays.asList(config.getDnsServers().split(regexp));
        if (getNetInterfaceStatus(config.getStatus()) == NetInterfaceStatus.netIPv4StatusEnabledWAN
                && dnsServers != null && !dnsServers.isEmpty()) {
            StringBuilder dnsServersBuilder = new StringBuilder();
            for (String dns : dnsServers) {
                if (!dns.trim().isEmpty()) {
                    dnsServersBuilder.append(((IP4Address) IPAddress.parseHostAddress(dns)).getHostAddress())
                            .append(",");
                }
            }
            if (dnsServersBuilder.length() > 0) {
                properties.put(dnsServerPropName,
                        dnsServersBuilder.toString().substring(0, dnsServersBuilder.toString().length() - 1));
            } else {
                properties.put(dnsServerPropName, "");
            }
        } else {
            properties.put(dnsServerPropName, "");
        }
    }

    private static void fillModemProperties(GwtModemInterfaceConfig gwtModemConfig, Map<String, Object> properties,
            String basePropName, NetInterfaceStatus netInterfaceStatus) throws GwtKuraException {

        Boolean enabled = netInterfaceStatus.equals(NetInterfaceStatus.netIPv4StatusEnabledWAN);
        properties.put(basePropName + ENABLED, enabled);

        properties.put(basePropName + "apn", gwtModemConfig.getApn());
        properties.put(basePropName + "dialString", gwtModemConfig.getDialString());

        fillModemPassword(gwtModemConfig, properties, basePropName);

        properties.put(basePropName + "username", gwtModemConfig.getUsername());
        properties.put(basePropName + "resetTimeout", gwtModemConfig.getResetTimeout());
        properties.put(basePropName + "persist", gwtModemConfig.isPersist());
        properties.put(basePropName + "holdoff", gwtModemConfig.getHoldoff());
        properties.put(basePropName + "maxFail", gwtModemConfig.getMaxFail());
        properties.put(basePropName + "idle", gwtModemConfig.getIdle());
        properties.put(basePropName + "activeFilter", gwtModemConfig.getActiveFilter());
        properties.put(basePropName + "lcpEchoInterval", gwtModemConfig.getLcpEchoInterval());
        properties.put(basePropName + "lcpEchoFailure", gwtModemConfig.getLcpEchoFailure());
        properties.put(basePropName + "gpsEnabled", gwtModemConfig.isGpsEnabled());
        properties.put(basePropName + "diversityEnabled", gwtModemConfig.isDiversityEnabled());

        fillModemAuthType(gwtModemConfig, properties, basePropName);
        fillModemPdpType(gwtModemConfig, properties, basePropName);
    }

    private static void fillModemPdpType(GwtModemInterfaceConfig gwtModemConfig, Map<String, Object> properties,
            String basePropName) {
        GwtModemPdpType pdpType = gwtModemConfig.getPdpType();
        if (pdpType != null) {
            String pdpTypePropName = basePropName + "pdpType";
            if (pdpType.equals(GwtModemPdpType.netModemPdpIP)) {
                properties.put(pdpTypePropName, ModemConfig.PdpType.IP.name());
            } else if (pdpType.equals(GwtModemPdpType.netModemPdpIPv6)) {
                properties.put(pdpTypePropName, ModemConfig.PdpType.IPv6.name());
            } else if (pdpType.equals(GwtModemPdpType.netModemPdpPPP)) {
                properties.put(pdpTypePropName, ModemConfig.PdpType.PPP.name());
            } else {
                properties.put(pdpTypePropName, ModemConfig.PdpType.UNKNOWN.name());
            }
        }
    }

    private static void fillModemAuthType(GwtModemInterfaceConfig gwtModemConfig, Map<String, Object> properties,
            String basePropName) {
        GwtModemAuthType authType = gwtModemConfig.getAuthType();
        if (authType != null) {
            String authTypePropName = basePropName + "authType";
            if (authType.equals(GwtModemAuthType.netModemAuthNONE)) {
                properties.put(authTypePropName, ModemConfig.AuthType.NONE.name());
            } else if (authType.equals(GwtModemAuthType.netModemAuthAUTO)) {
                properties.put(authTypePropName, ModemConfig.AuthType.AUTO.name());
            } else if (authType.equals(GwtModemAuthType.netModemAuthCHAP)) {
                properties.put(authTypePropName, ModemConfig.AuthType.CHAP.name());
            } else if (authType.equals(GwtModemAuthType.netModemAuthPAP)) {
                properties.put(authTypePropName, ModemConfig.AuthType.PAP.name());
            }
        }
    }

    private static void fillModemPassword(GwtModemInterfaceConfig gwtModemConfig, Map<String, Object> properties,
            String basePropName) throws GwtKuraException {
        String passKey = GwtSafeHtmlUtils.htmlUnescape(gwtModemConfig.getPassword());
        if (passKey != null && passKey.equals(PASSWORD_PLACEHOLDER)) {
            List<GwtNetInterfaceConfig> result = privateFindNetInterfaceConfigurations(false);
            for (GwtNetInterfaceConfig netConfig : result) {
                if (netConfig instanceof GwtModemInterfaceConfig) {
                    GwtModemInterfaceConfig oldModemConfig = (GwtModemInterfaceConfig) netConfig;
                    if (gwtModemConfig.getName().equals(oldModemConfig.getName())) {
                        properties.put(basePropName + "password",
                                new Password(GwtSafeHtmlUtils.htmlUnescape(oldModemConfig.getPassword())));
                    }
                }
            }
        } else if (passKey != null) {
            properties.put(basePropName + "password", new Password(passKey));
        }
    }

    private static void fillDhcpAndNatProperties(GwtNetInterfaceConfig config, Map<String, Object> properties,
            String basePropName) throws KuraException, UnknownHostException {
        String routerMode = config.getRouterMode();
        String natEnabledPropName = basePropName + "nat.enabled";
        StringBuilder dhcpServer4PropName = new StringBuilder(basePropName).append("dhcpServer4.");
        if (routerMode.equals(GwtNetRouterMode.netRouterOff.name())) {
            logger.debug("DCHP and NAT are disabled");
            properties.put(dhcpServer4PropName.toString() + ENABLED, false);
            properties.put(natEnabledPropName, false);
        } else if (routerMode.equals(GwtNetRouterMode.netRouterDchp.name())) {
            logger.debug("DCHP is enabled");
            fillDhcpServerProperties(config, properties, basePropName);
            properties.put(natEnabledPropName, false);
        } else if (routerMode.equals(GwtNetRouterMode.netRouterDchpNat.name())) {
            logger.debug("DCHP and NAT is enabled");
            fillDhcpServerProperties(config, properties, basePropName);
            properties.put(natEnabledPropName, true);
        } else if (routerMode.equals(GwtNetRouterMode.netRouterNat.name())) {
            logger.debug("NAT is enabled");
            properties.put(dhcpServer4PropName.toString() + ENABLED, false);
            properties.put(natEnabledPropName, true);
        } else {
            logger.error("Unsupported routerMode: {}", routerMode);
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "Unsupported routerMode: " + routerMode);
        }
    }

    private static void fillDhcpServerProperties(GwtNetInterfaceConfig config, Map<String, Object> properties,
            String basePropName) throws UnknownHostException {
        StringBuilder dhcpServer4PropName = new StringBuilder(basePropName).append("dhcpServer4.");
        properties.put(dhcpServer4PropName.toString() + ENABLED, true);
        properties.put(dhcpServer4PropName.toString() + "defaultLeaseTime", config.getRouterDhcpDefaultLease());
        properties.put(dhcpServer4PropName.toString() + "maxLeaseTime", config.getRouterDhcpMaxLease());
        properties.put(dhcpServer4PropName.toString() + "rangeStart",
                ((IP4Address) IPAddress.parseHostAddress(config.getRouterDhcpBeginAddress())).getHostAddress());
        properties.put(dhcpServer4PropName.toString() + "rangeEnd",
                ((IP4Address) IPAddress.parseHostAddress(config.getRouterDhcpEndAddress())).getHostAddress());
        properties.put(dhcpServer4PropName.toString() + "passDns", config.getRouterDnsPass());

        IP4Address subnetMask = (IP4Address) IPAddress.parseHostAddress(config.getRouterDhcpSubnetMask());
        short prefix = NetworkUtil.getNetmaskShortForm(subnetMask.getHostAddress());
        properties.put(dhcpServer4PropName.toString() + "prefix", prefix);

        List<IP4Address> dnsServers = new ArrayList<>();
        dnsServers.add((IP4Address) IPAddress.parseHostAddress(config.getIpAddress()));
        StringBuilder dnsServersBuilder = new StringBuilder();
        dnsServers.forEach(dns -> dnsServersBuilder.append(dns.getHostAddress()).append(","));
        properties.put(dhcpServer4PropName.toString() + "ip4.dnsServers",
                dnsServersBuilder.toString().substring(0, dnsServersBuilder.toString().length() - 1));
    }

    @SuppressWarnings("deprecation")
    private static WifiConfig getWifiConfig(GwtWifiConfig gwtWifiConfig) throws GwtKuraException {

        WifiConfig wifiConfig = new WifiConfig();

        WifiMode wifiMode = getWifiConfigWirelessMode(gwtWifiConfig.getWirelessMode());
        wifiConfig.setMode(wifiMode);

        wifiConfig.setSSID(GwtSafeHtmlUtils.htmlUnescape(gwtWifiConfig.getWirelessSsid()));

        wifiConfig.setDriver(gwtWifiConfig.getDriver());

        WifiRadioMode wifiRadioMode = getWifiConfigRadioMode(gwtWifiConfig.getRadioModeEnum());
        wifiConfig.setRadioMode(wifiRadioMode);

        String hardwareMode = gwtWifiConfig.getRadioModeEnum().getRadioMode();
        wifiConfig.setHardwareMode(hardwareMode);

        int[] wifiConfigChannels = getWifiConfigChannels(gwtWifiConfig.getChannels());
        if (wifiConfigChannels.length > 0) {
            wifiConfig.setChannels(wifiConfigChannels);
        }

        WifiSecurity wifiSecurity = getWifiConfigSecurity(gwtWifiConfig.getSecurity());
        wifiConfig.setSecurity(wifiSecurity);

        WifiCiphers wifiPairwiseCiphers = getWifiConfigCiphers(gwtWifiConfig.getPairwiseCiphers());
        if (wifiPairwiseCiphers != null) {
            wifiConfig.setPairwiseCiphers(wifiPairwiseCiphers);
        }

        WifiCiphers wifiGroupCiphers = getWifiConfigCiphers(gwtWifiConfig.getGroupCiphers());
        if (wifiGroupCiphers != null) {
            wifiConfig.setGroupCiphers(wifiGroupCiphers);
        }

        WifiBgscan wifiBgscan = getWifiConfigBgscan(gwtWifiConfig, gwtWifiConfig.getBgscanModule());
        wifiConfig.setBgscan(wifiBgscan);

        wifiConfig.setPasskey(GwtSafeHtmlUtils.htmlUnescape(gwtWifiConfig.getPassword()));

        wifiConfig.setPingAccessPoint(gwtWifiConfig.pingAccessPoint());

        wifiConfig.setIgnoreSSID(gwtWifiConfig.ignoreSSID());

        wifiConfig.setBroadcast(!gwtWifiConfig.ignoreSSID());

        wifiConfig.setWifiCountryCode(gwtWifiConfig.getCountryCode());

        return wifiConfig;
    }

    private static void fillWifiProperties(GwtWifiConfig gwtWifiConfig, Map<String, Object> properties,
            String basePropName, String interfaceName) throws KuraException, GwtKuraException {
        StringBuilder wifiBasePropName = new StringBuilder(basePropName).append("wifi.");

        String mode = gwtWifiConfig.getWirelessMode();
        String wifiMode;
        StringBuilder wifiModeBasePropName = new StringBuilder(wifiBasePropName);
        if (mode != null && mode.equals(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name())) {
            wifiMode = WifiMode.MASTER.name();
            wifiModeBasePropName.append("master.");
        } else if (mode != null && mode.equals(GwtWifiWirelessMode.netWifiWirelessModeStation.name())) {
            wifiMode = WifiMode.INFRA.name();
            wifiModeBasePropName.append("infra.");
        } else if (mode != null && mode.equals(GwtWifiWirelessMode.netWifiWirelessModeAdHoc.name())) {
            properties.put(wifiBasePropName.toString() + "mode", WifiMode.ADHOC.name());
            return;
        } else {
            properties.put(wifiBasePropName.toString() + "mode", WifiMode.UNKNOWN.name());
            return;
        }
        properties.put(wifiBasePropName.toString() + "mode", wifiMode);

        properties.put(wifiModeBasePropName.toString() + "ssid",
                GwtSafeHtmlUtils.htmlUnescape(gwtWifiConfig.getWirelessSsid()));
        properties.put(wifiModeBasePropName.toString() + "driver", gwtWifiConfig.getDriver());
        properties.put(wifiModeBasePropName.toString() + "radioMode",
                getWifiConfigRadioMode(gwtWifiConfig.getRadioModeEnum()).name());

        fillWifiChannelsProperties(gwtWifiConfig, properties, wifiModeBasePropName.toString());

        WifiSecurity wifiSecurity = getWifiConfigSecurity(gwtWifiConfig.getSecurity());
        properties.put(wifiModeBasePropName.toString() + "securityType", wifiSecurity.name());

        WifiCiphers wifiPairwiseCiphers = getWifiConfigCiphers(gwtWifiConfig.getPairwiseCiphers());
        if (wifiPairwiseCiphers != null) {
            properties.put(wifiModeBasePropName.toString() + "pairwiseCiphers", wifiPairwiseCiphers.name());
        }

        WifiCiphers wifiGroupCiphers = getWifiConfigCiphers(gwtWifiConfig.getGroupCiphers());
        if (wifiGroupCiphers != null) {
            properties.put(wifiModeBasePropName.toString() + "groupCiphers", wifiGroupCiphers.name());
        }

        WifiBgscan wifiBgscan = getWifiConfigBgscan(gwtWifiConfig, gwtWifiConfig.getBgscanModule());
        if (wifiBgscan.getModule().equals(WifiBgscanModule.NONE)) {
            properties.put(wifiModeBasePropName.toString() + "bgscan", null);
        } else {
            String wifiBgscanString = wifiBgscan.getModule().name().toLowerCase() + ":" + wifiBgscan.getShortInterval()
                    + ":" + wifiBgscan.getRssiThreshold() + ":" + wifiBgscan.getLongInterval();
            properties.put(wifiModeBasePropName.toString() + "bgscan", wifiBgscanString);
        }

        fillWifiPassphrase(gwtWifiConfig, properties, wifiModeBasePropName.toString(), interfaceName, mode);

        properties.put(wifiModeBasePropName.toString() + "pingAccessPoint", gwtWifiConfig.pingAccessPoint());
        properties.put(wifiModeBasePropName.toString() + "ignoreSSID", gwtWifiConfig.ignoreSSID());
    }

    private static void fillWifiChannelsProperties(GwtWifiConfig gwtWifiConfig, Map<String, Object> properties,
            String wifiModeBasePropName) {
        int[] wifiConfigChannels = getWifiConfigChannels(gwtWifiConfig.getChannels());
        if (wifiConfigChannels.length > 0) {
            StringBuilder wifiConfigChannelsStringBuilder = new StringBuilder();
            for (int i = 0; i < wifiConfigChannels.length; i++) {
                wifiConfigChannelsStringBuilder.append(String.valueOf(wifiConfigChannels[i]));
                if (i != wifiConfigChannels.length - 1) {
                    wifiConfigChannelsStringBuilder.append(" ");
                }
            }
            properties.put(wifiModeBasePropName + "channel", wifiConfigChannelsStringBuilder.toString());
        }
    }

    private static void fillWifiPassphrase(GwtWifiConfig gwtWifiConfig, Map<String, Object> properties,
            String wifiModeBasePropName, String interfaceName, String mode) throws GwtKuraException {

        GwtWifiSecurity security = gwtWifiConfig.getSecurityEnum();
        String passKey = GwtSafeHtmlUtils.htmlUnescape(gwtWifiConfig.getPassword());
        String wifiPassphrasePropName = wifiModeBasePropName + "passphrase";

        if (security == GwtWifiSecurity.netWifiSecurityNONE) {
            properties.put(wifiPassphrasePropName, null);
            return;
        }

        String wirelessSSID = gwtWifiConfig.getWirelessSsid();

        if (isPlaceholder(passKey, security)) {
            Optional<GwtWifiConfig> oldGwtWifiConfig = wirelessSSID == null ? getOldGwtWifiConfig(interfaceName, mode)
                    : getOldGwtWifiConfigBySSID(wirelessSSID, interfaceName, mode);

            if (oldGwtWifiConfig.isPresent()) {
                properties.put(wifiPassphrasePropName,
                        new Password(GwtSafeHtmlUtils.htmlUnescape(oldGwtWifiConfig.get().getPassword())));
            } else {
                throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
            }

        } else if (passKey != null && mode.equals(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name())) {
            GwtServerUtil.validateUserPassword(passKey);
            properties.put(wifiPassphrasePropName, new Password(passKey));
        } else if (passKey != null) {
            properties.put(wifiPassphrasePropName, new Password(passKey));
        }
    }

    private static boolean isPlaceholder(String passKey, GwtWifiSecurity security) {
        return passKey != null && (passKey.equals(PASSWORD_PLACEHOLDER)
                || (security != GwtWifiSecurity.netWifiSecurityNONE && passKey.isEmpty()));
    }

    private static Optional<GwtWifiConfig> getOldGwtWifiConfigBySSID(String wirelessSSID, String interfaceName,
            String mode) throws GwtKuraException {
        Optional<GwtWifiConfig> config = Optional.empty();
        List<GwtNetInterfaceConfig> result = privateFindNetInterfaceConfigurations(false);
        for (GwtNetInterfaceConfig netConfig : result) {
            if (netConfig instanceof GwtWifiNetInterfaceConfig
                    && interfaceName.equals(((GwtWifiNetInterfaceConfig) netConfig).getName())) {
                GwtWifiNetInterfaceConfig oldWifiConfig = (GwtWifiNetInterfaceConfig) netConfig;
                GwtWifiConfig oldGwtWifiConfig;
                if (mode.equals(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name())) {
                    oldGwtWifiConfig = oldWifiConfig.getAccessPointWifiConfig();
                } else {
                    oldGwtWifiConfig = oldWifiConfig.getStationWifiConfig();
                }

                if (oldGwtWifiConfig != null && oldGwtWifiConfig.getWirelessSsid().equals(wirelessSSID)) {
                    config = Optional.of(oldGwtWifiConfig);
                    break;
                }
            }
        }
        return config;
    }

    private static Optional<GwtWifiConfig> getOldGwtWifiConfig(String interfaceName, String mode)
            throws GwtKuraException {
        Optional<GwtWifiConfig> config = Optional.empty();
        List<GwtNetInterfaceConfig> result = privateFindNetInterfaceConfigurations(false);
        for (GwtNetInterfaceConfig netConfig : result) {
            if (netConfig instanceof GwtWifiNetInterfaceConfig
                    && interfaceName.equals(((GwtWifiNetInterfaceConfig) netConfig).getName())) {
                GwtWifiNetInterfaceConfig oldWifiConfig = (GwtWifiNetInterfaceConfig) netConfig;
                GwtWifiConfig oldGwtWifiConfig;
                if (mode.equals(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name())) {
                    oldGwtWifiConfig = oldWifiConfig.getAccessPointWifiConfig();
                } else {
                    oldGwtWifiConfig = oldWifiConfig.getStationWifiConfig();
                }

                if (oldGwtWifiConfig != null) {
                    config = Optional.of(oldGwtWifiConfig);
                    break;
                }
            }
        }
        return config;
    }

    private static WifiBgscan getWifiConfigBgscan(GwtWifiConfig gwtWifiConfig, String bgscanModule) {
        WifiBgscanModule wifiBgscanModule = null;
        if (GwtWifiBgscanModule.netWifiBgscanMode_SIMPLE.name().equals(bgscanModule)) {
            wifiBgscanModule = WifiBgscanModule.SIMPLE;
        } else if (GwtWifiBgscanModule.netWifiBgscanMode_LEARN.name().equals(bgscanModule)) {
            wifiBgscanModule = WifiBgscanModule.LEARN;
        } else {
            wifiBgscanModule = WifiBgscanModule.NONE;
        }

        int bgscanRssiThreshold = gwtWifiConfig.getBgscanRssiThreshold();
        int bgscanShortInterval = gwtWifiConfig.getBgscanShortInterval();
        int bgscanLongInterval = gwtWifiConfig.getBgscanLongInterval();

        return new WifiBgscan(wifiBgscanModule, bgscanShortInterval, bgscanRssiThreshold, bgscanLongInterval);
    }

    private static WifiCiphers getWifiConfigCiphers(String ciphers) {
        WifiCiphers wifiCiphers = null;
        if (GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name().equals(ciphers)) {
            wifiCiphers = WifiCiphers.CCMP_TKIP;
        } else if (GwtWifiCiphers.netWifiCiphers_TKIP.name().equals(ciphers)) {
            wifiCiphers = WifiCiphers.TKIP;
        } else {
            wifiCiphers = WifiCiphers.CCMP;
        }
        return wifiCiphers;
    }

    private static WifiSecurity getWifiConfigSecurity(String security) {
        WifiSecurity wifiSecurity;

        if (GwtWifiSecurity.netWifiSecurityWPA.name().equals(security)) {
            wifiSecurity = WifiSecurity.SECURITY_WPA;
        } else if (GwtWifiSecurity.netWifiSecurityWPA2.name().equals(security)) {
            wifiSecurity = WifiSecurity.SECURITY_WPA2;
        } else if (GwtWifiSecurity.netWifiSecurityWPA_WPA2.name().equals(security)) {
            wifiSecurity = WifiSecurity.SECURITY_WPA_WPA2;
        } else if (GwtWifiSecurity.netWifiSecurityWEP.name().equals(security)) {
            wifiSecurity = WifiSecurity.SECURITY_WEP;
        } else {
            wifiSecurity = WifiSecurity.SECURITY_NONE;
        }
        return wifiSecurity;
    }

    private static int[] getWifiConfigChannels(List<Integer> alChannels) {
        if (alChannels == null) {
            return new int[0];
        }
        int[] channels = new int[alChannels.size()];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = alChannels.get(i).intValue();
        }
        return channels;
    }

    private static WifiRadioMode getWifiConfigRadioMode(GwtWifiRadioMode radioMode) throws GwtKuraException {
        WifiRadioMode wifiRadioMode;

        switch (radioMode) {
            case netWifiRadioModeA:
                wifiRadioMode = WifiRadioMode.RADIO_MODE_80211a;
                break;
            case netWifiRadioModeB:
                wifiRadioMode = WifiRadioMode.RADIO_MODE_80211b;
                break;
            case netWifiRadioModeBG:
                wifiRadioMode = WifiRadioMode.RADIO_MODE_80211g;
                break;
            case netWifiRadioModeBGN:
                wifiRadioMode = WifiRadioMode.RADIO_MODE_80211nHT20;
                break;
            case netWifiRadioModeANAC:
                wifiRadioMode = WifiRadioMode.RADIO_MODE_80211_AC;
                break;

            default:
                throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
        }

        return wifiRadioMode;
    }

    private static WifiMode getWifiConfigWirelessMode(String mode) {
        WifiMode wifiMode;
        if (mode != null && mode.equals(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name())) {
            wifiMode = WifiMode.MASTER;
        } else if (mode != null && mode.equals(GwtWifiWirelessMode.netWifiWirelessModeStation.name())) {
            wifiMode = WifiMode.INFRA;
        } else if (mode != null && mode.equals(GwtWifiWirelessMode.netWifiWirelessModeAdHoc.name())) {
            wifiMode = WifiMode.ADHOC;
        } else {
            wifiMode = WifiMode.UNKNOWN;
        }
        return wifiMode;
    }

    private static <T> T withCellularModem(final String interfaceName, final ModemManagerService modemManagerService,
            final ModemFunction<Optional<CellularModem>, T> function) throws KuraException {

        final Optional<T> result = modemManagerService.withModemService(interfaceName, m -> {
            if (m.isPresent()) {
                return Optional.of(function.apply(m));
            }
            return Optional.empty();
        });

        if (result.isPresent()) {
            return result.get();
        }

        return modemManagerService.withAllModemServices(m -> {
            for (CellularModem modem : m) {
                if (isModemForNetworkInterface(interfaceName, modem)) {
                    return function.apply(Optional.of(modem));
                }
            }
            return function.apply(Optional.empty());
        });
    }

    private static boolean isModemForNetworkInterface(String ifaceName, CellularModem modemService) {
        List<NetConfig> netConfigs = modemService.getConfiguration();
        for (NetConfig netConfig : netConfigs) {
            if (netConfig instanceof ModemConfig) {
                ModemConfig modemConfig = (ModemConfig) netConfig;
                String interfaceName = "ppp" + modemConfig.getPppNumber();
                if (interfaceName.equals(ifaceName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void appendNetwork(String address, StringBuilder stringBuilder) throws UnknownHostException {
        String[] networkAddress = address.split("/");
        if (networkAddress.length >= 2) {
            stringBuilder.append(((IP4Address) IPAddress.parseHostAddress(networkAddress[0])).getHostAddress())
                    .append("/").append(networkAddress[1]);
        }
    }

}
