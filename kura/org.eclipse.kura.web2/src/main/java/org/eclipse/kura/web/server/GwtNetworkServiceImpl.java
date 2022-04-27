/*******************************************************************************
 * Copyright (c) 2011, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server;

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
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkAdminService;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.dhcp.DhcpLease;
import org.eclipse.kura.net.dhcp.DhcpServerCfg;
import org.eclipse.kura.net.dhcp.DhcpServerCfgIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4;
import org.eclipse.kura.net.firewall.RuleType;
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
import org.eclipse.kura.web.Console;
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
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.validator.PasswordStrengthValidators;
import org.eclipse.kura.web.shared.validator.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtNetworkServiceImpl extends OsgiRemoteServiceServlet implements GwtNetworkService {

    private static final long serialVersionUID = -4188750359099902616L;
    private static final Logger logger = LoggerFactory.getLogger(GwtNetworkServiceImpl.class);
    private static final String ENABLED = "enabled";

    @Override
    public List<GwtNetInterfaceConfig> findNetInterfaceConfigurations() throws GwtKuraException {
        List<GwtNetInterfaceConfig> result = privateFindNetInterfaceConfigurations();

        for (GwtNetInterfaceConfig netConfig : result) {
            if (netConfig instanceof GwtWifiNetInterfaceConfig) {
                GwtWifiNetInterfaceConfig wifiConfig = (GwtWifiNetInterfaceConfig) netConfig;
                GwtWifiConfig gwtAPWifiConfig = wifiConfig.getAccessPointWifiConfig();
                if (gwtAPWifiConfig != null) {
                    gwtAPWifiConfig.setPassword(PASSWORD_PLACEHOLDER);
                }

                GwtWifiConfig gwtStationWifiConfig = wifiConfig.getStationWifiConfig();
                if (gwtStationWifiConfig != null) {
                    gwtStationWifiConfig.setPassword(PASSWORD_PLACEHOLDER);
                }
            } else if (netConfig instanceof GwtModemInterfaceConfig) {
                GwtModemInterfaceConfig modemConfig = (GwtModemInterfaceConfig) netConfig;
                modemConfig.setPassword(PASSWORD_PLACEHOLDER);
            }
        }
        return result;
    }

    private List<GwtNetInterfaceConfig> privateFindNetInterfaceConfigurations() throws GwtKuraException {

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
                    .getNetworkInterfaceConfigs()) {
                logger.debug("Getting config for {} with type {}", netIfConfig.getName(), netIfConfig.getType());

                logger.debug("Interface State: {}", netIfConfig.getState());

                gwtNetConfig = createGwtNetConfig(netIfConfig);

                gwtNetConfig.setName(netIfConfig.getName());
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
                                        ((NetConfigIP4) netConfig).getStatus().toString());

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
                                    StringBuffer sb = new StringBuffer();
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
                                StringBuffer sb = new StringBuffer();
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
                            // Note that this section is used to configure both a station config and an access point
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
                                if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA) {
                                    gwtWifiConfig.setSecurity(GwtWifiSecurity.netWifiSecurityWPA.name());
                                } else if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA2) {
                                    gwtWifiConfig.setSecurity(GwtWifiSecurity.netWifiSecurityWPA2.name());
                                } else if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA_WPA2) {
                                    gwtWifiConfig.setSecurity(GwtWifiSecurity.netWifiSecurityWPA_WPA2.name());
                                } else if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WEP) {
                                    gwtWifiConfig.setSecurity(GwtWifiSecurity.netWifiSecurityWEP.name());
                                } else {
                                    gwtWifiConfig.setSecurity(GwtWifiSecurity.netWifiSecurityNONE.name());
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
                                        alChannels.add(new Integer(channel));
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
                                    if (wifiClientMonitorService != null) {
                                        if (wifiConfig.getMode().equals(WifiMode.INFRA)) {
                                            if (gwtNetConfig.getStatus()
                                                    .equals(GwtNetIfStatus.netIPv4StatusDisabled.name())
                                                    || gwtNetConfig.getStatus()
                                                            .equals(GwtNetIfStatus.netIPv4StatusUnmanaged.name())) {
                                                gwtNetConfig.setHwRssi("N/A");
                                            } else {
                                                try {
                                                    int rssi = wifiClientMonitorService.getSignalLevel(
                                                            netIfConfig.getName(), wifiConfig.getSSID());
                                                    logger.debug("Setting Received Signal Strength to {}", rssi);
                                                    gwtNetConfig.setHwRssi(Integer.toString(rssi));
                                                } catch (KuraException e) {
                                                    logger.warn("Failed", e);
                                                }
                                            }
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
                                                int rssi = cellModemService.getSignalStrength();
                                                logger.debug("Setting Received Signal Strength to {}", rssi);
                                                gwtModemConfig.setHwRssi(Integer.toString(rssi));
                                            } catch (KuraException e) {
                                                logger.warn("Failed to get Received Signal Strength from modem", e);
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

                                gwtModemConfig.setUsername(modemConfig.getUsername());

                                gwtModemConfig.setPassword(modemConfig.getPasswordAsPassword().toString());

                                gwtModemConfig.setPppNum(modemConfig.getPppNumber());

                                gwtModemConfig.setResetTimeout(modemConfig.getResetTimeout());

                                gwtModemConfig.setPersist(modemConfig.isPersist());

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
                                logger.debug("Setting up DhcpServerConfigIP4: {}",
                                        ((DhcpServerConfigIP4) netConfig).toString());

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

    private GwtNetInterfaceConfig createGwtNetConfig(
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

    @Override
    public void updateNetInterfaceConfigurations(GwtXSRFToken xsrfToken, GwtNetInterfaceConfig config)
            throws GwtKuraException {

        checkXSRFToken(xsrfToken);
        ConfigurationService configurationService = ServiceLocator.getInstance().getService(ConfigurationService.class);
        Map<String, Object> properties = new HashMap<>();
        StringBuilder basePropName = new StringBuilder("net.interface.");
        basePropName.append(config.getName()).append(".config.");

        String status = config.getStatus();
        if (logger.isDebugEnabled()) {
            logger.debug("config.getStatus(): {}", GwtSafeHtmlUtils.htmlEscape(status));
        }
        try {
            NetInterfaceStatus netInterfaceStatus = getNetInterfaceStatus(status);
            properties.put(basePropName.toString() + "ip4.status", netInterfaceStatus.name());

            if (config.getHwTypeEnum() == GwtNetIfType.ETHERNET || config.getHwTypeEnum() == GwtNetIfType.WIFI
                    || config.getHwTypeEnum() == GwtNetIfType.MODEM) {
                fillIp4AndDhcpProperties(config, properties, basePropName.toString());
            }

            if (config.getHwTypeEnum() == GwtNetIfType.WIFI && config instanceof GwtWifiNetInterfaceConfig) {
                GwtWifiConfig gwtWifiConfig = ((GwtWifiNetInterfaceConfig) config).getActiveWifiConfig();
                if (gwtWifiConfig != null) {
                    fillWifiProperties(gwtWifiConfig, properties, basePropName.toString(), config.getName());
                }
            } else if (config.getHwTypeEnum() == GwtNetIfType.MODEM && config instanceof GwtModemInterfaceConfig) {
                GwtModemInterfaceConfig gwtModemConfig = (GwtModemInterfaceConfig) config;
                fillModemProperties(gwtModemConfig, properties, basePropName.toString(), netInterfaceStatus);
            }

            configurationService.updateConfiguration("org.eclipse.kura.net.admin.NetworkConfigurationService",
                    properties, true);

        } catch (Exception e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public ArrayList<GwtFirewallOpenPortEntry> findDeviceFirewallOpenPorts(GwtXSRFToken xsrfToken)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);
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

    @Override
    public ArrayList<GwtWifiHotspotEntry> findWifiHotspots(GwtXSRFToken xsrfToken, String interfaceName,
            String wirelessSsid) throws GwtKuraException {

        checkXSRFToken(xsrfToken);
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);
        List<GwtWifiHotspotEntry> gwtWifiHotspotsEntries = new ArrayList<>();

        try {
            List<WifiHotspotInfo> wifiHotspotInfoList = nas.getWifiHotspotList(interfaceName);
            if (wifiHotspotInfoList != null) {
                for (WifiHotspotInfo wifiHotspotInfo : wifiHotspotInfoList) {
                    String ssid = wifiHotspotInfo.getSsid();
                    // if(!ssid.matches("[0-9A-Za-z/.@*#:\\ \\_\\-]+")){
                    // ssid= null;
                    // }
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

    @Override
    public List<GwtModemPdpEntry> findPdpContextInfo(GwtXSRFToken xsrfToken, String interfaceName)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

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

    @Override
    public boolean verifyWifiCredentials(GwtXSRFToken xsrfToken, String interfaceName, GwtWifiConfig gwtWifiConfig)
            throws GwtKuraException {

        checkXSRFToken(xsrfToken);
        if (interfaceName == null || gwtWifiConfig == null) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_NULL_ARGUMENT);
        }
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        WifiConfig wifiConfig = getWifiConfig(gwtWifiConfig);
        return nas.verifyWifiCredentials(interfaceName, wifiConfig, 60);
    }

    @Override
    public ArrayList<GwtFirewallPortForwardEntry> findDeviceFirewallPortForwards(GwtXSRFToken xsrfToken)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);
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

    @Override
    public ArrayList<GwtFirewallNatEntry> findDeviceFirewallNATs(GwtXSRFToken xsrfToken) throws GwtKuraException {

        checkXSRFToken(xsrfToken);
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

    // -------------------------------------------------------------------------------------
    //
    // Private Methods
    //
    // -------------------------------------------------------------------------------------

    private NetInterfaceStatus getNetInterfaceStatus(String status) {
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

    private void fillIp4AndDhcpProperties(GwtNetInterfaceConfig config, Map<String, Object> properties,
            String basePropName) throws UnknownHostException, KuraException {
        logger.debug("config.getConfigMode(): {}", config.getConfigMode());
        String dhcpClient4PropName = basePropName + "dhcpClient4.enabled";
        if (GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name().equals(config.getConfigMode())) {
            logger.debug("mode is DHCP");
            properties.put(dhcpClient4PropName, true);
        } else {
            logger.debug("mode is STATIC");
            properties.put(dhcpClient4PropName, false);

            if (config.getIpAddress() != null && !config.getIpAddress().isEmpty()) {
                logger.debug("setting address: {}", config.getIpAddress());
                String addressPropName = basePropName + "ip4.address";
                properties.put(addressPropName,
                        ((IP4Address) IPAddress.parseHostAddress(config.getIpAddress())).getHostAddress());
            }

            if (config.getSubnetMask() != null && !config.getSubnetMask().isEmpty()) {
                logger.debug("setting subnet mask: {}", config.getSubnetMask());
                String prefixPropName = basePropName + "ip4.prefix";
                short prefix = NetworkUtil.getNetmaskShortForm(
                        ((IP4Address) IPAddress.parseHostAddress(config.getSubnetMask())).getHostAddress());
                properties.put(prefixPropName, prefix);
            }
            if (config.getGateway() != null && !config.getGateway().isEmpty()) {
                logger.debug("setting gateway: {}", config.getGateway());
                String gatewayPropName = basePropName + "ip4.gateway";
                properties.put(gatewayPropName,
                        ((IP4Address) IPAddress.parseHostAddress(config.getGateway())).getHostAddress());
            }
        }

        fillDnsServers(config, properties, basePropName);

        if (GwtNetIfConfigMode.netIPv4ConfigModeManual.name().equals(config.getConfigMode())) {
            fillDhcpAndNatProperties(config, properties, basePropName);
        }
    }

    private void fillDnsServers(GwtNetInterfaceConfig config, Map<String, Object> properties, String basePropName) {
        String regexp = "[\\s,;\\n\\t]+";
        String dnsServerPropName = basePropName + "ip4.dnsServers";
        String[] dnsServersString = config.getDnsServers().split(regexp);
        if (dnsServersString != null && dnsServersString.length > 0) {
            StringBuilder dnsServersBuilder = new StringBuilder();
            try {
                for (String dns : Arrays.asList(dnsServersString)) {
                    dnsServersBuilder.append(((IP4Address) IPAddress.parseHostAddress(dns)).getHostAddress())
                            .append(",");
                }
                properties.put(dnsServerPropName,
                        dnsServersBuilder.toString().substring(0, dnsServersBuilder.toString().length() - 1));
            } catch (UnknownHostException e) {
                logger.warn("Failed to parse dns server address", e);
                properties.put(dnsServerPropName, "");
            }
        } else {
            properties.put(dnsServerPropName, "");
        }
    }

    private void fillModemProperties(GwtModemInterfaceConfig gwtModemConfig, Map<String, Object> properties,
            String basePropName, NetInterfaceStatus netInterfaceStatus) throws KuraException, GwtKuraException {

        Boolean enabled = netInterfaceStatus.equals(NetInterfaceStatus.netIPv4StatusEnabledWAN);
        properties.put(basePropName + ENABLED, enabled);

        properties.put(basePropName + "apn", gwtModemConfig.getApn());
        properties.put(basePropName + "dialString", gwtModemConfig.getDialString());

        fillModemPassword(gwtModemConfig, properties, basePropName);

        properties.put(basePropName + "username", gwtModemConfig.getUsername());
        properties.put(basePropName + "resetTimeout", gwtModemConfig.getResetTimeout());
        properties.put(basePropName + "persist", gwtModemConfig.isPersist());
        properties.put(basePropName + "maxFail", gwtModemConfig.getMaxFail());
        properties.put(basePropName + "idle", gwtModemConfig.getIdle());
        properties.put(basePropName + "activeFilter", gwtModemConfig.getActiveFilter());
        properties.put(basePropName + "lcpEchoInterval", gwtModemConfig.getLcpEchoInterval());
        properties.put(basePropName + "lcpEchoFailure", gwtModemConfig.getLcpEchoFailure());
        properties.put(basePropName + "gpsEnabled", gwtModemConfig.isGpsEnabled());
        properties.put(basePropName + "diversityEnabled", gwtModemConfig.isDiversityEnabled());

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

    private void fillModemPassword(GwtModemInterfaceConfig gwtModemConfig, Map<String, Object> properties,
            String basePropName) throws GwtKuraException, KuraException {
        String passKey = GwtSafeHtmlUtils.htmlUnescape(gwtModemConfig.getPassword());
        if (passKey != null && passKey.equals(PASSWORD_PLACEHOLDER)) {
            List<GwtNetInterfaceConfig> result = privateFindNetInterfaceConfigurations();
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
            CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
            char[] passphrase = cryptoService.encryptAes(passKey.toCharArray());
            properties.put(basePropName + "password", new Password(passphrase));
        }
    }

    private void fillDhcpAndNatProperties(GwtNetInterfaceConfig config, Map<String, Object> properties,
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

    private void fillDhcpServerProperties(GwtNetInterfaceConfig config, Map<String, Object> properties,
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

    @Override
    public void updateDeviceFirewallOpenPorts(GwtXSRFToken xsrfToken, List<GwtFirewallOpenPortEntry> entries)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        List<FirewallOpenPortConfigIP<? extends IPAddress>> firewallOpenPortConfigIPs = new ArrayList<>();
        logger.debug("updating open ports");

        try {
            for (GwtFirewallOpenPortEntry entry : entries) {
                String network = null;
                String prefix = null;

                if (entry.getPermittedNetwork() != null) {
                    String[] parts = entry.getPermittedNetwork().split("/");
                    network = parts[0];
                    prefix = parts[1];
                }

                FirewallOpenPortConfigIP<IP4Address> firewallOpenPortConfigIP = new FirewallOpenPortConfigIP4();

                if (entry.getPortRange().indexOf(':') != -1) {
                    String[] parts = entry.getPortRange().split(":");
                    if (Integer.valueOf(parts[0].trim()) < Integer.valueOf(parts[1].trim())) {
                        firewallOpenPortConfigIP.setPortRange(entry.getPortRange());
                    } else {
                        throw new KuraException(KuraErrorCode.BAD_REQUEST);
                    }
                } else {
                    firewallOpenPortConfigIP.setPort(Integer.parseInt(entry.getPortRange()));
                }
                firewallOpenPortConfigIP
                        .setProtocol(NetProtocol.valueOf(GwtSafeHtmlUtils.htmlEscape(entry.getProtocol())));
                if (network != null && prefix != null) {
                    firewallOpenPortConfigIP.setPermittedNetwork(new NetworkPair<>(
                            (IP4Address) IPAddress.parseHostAddress(network), Short.parseShort(prefix)));
                }
                firewallOpenPortConfigIP
                        .setPermittedInterfaceName(GwtSafeHtmlUtils.htmlEscape(entry.getPermittedInterfaceName()));
                firewallOpenPortConfigIP
                        .setUnpermittedInterfaceName(GwtSafeHtmlUtils.htmlEscape(entry.getUnpermittedInterfaceName()));
                firewallOpenPortConfigIP.setPermittedMac(GwtSafeHtmlUtils.htmlEscape(entry.getPermittedMAC()));
                firewallOpenPortConfigIP.setSourcePortRange(GwtSafeHtmlUtils.htmlEscape(entry.getSourcePortRange()));

                logger.debug("adding open port entry for {}", entry.getPortRange());
                firewallOpenPortConfigIPs.add(firewallOpenPortConfigIP);
            }

            nas.setFirewallOpenPortConfiguration(firewallOpenPortConfigIPs);
        } catch (KuraException | NumberFormatException | UnknownHostException e) {
            logger.warn("Exception while updating firewall open ports", e);
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public void updateDeviceFirewallPortForwards(GwtXSRFToken xsrfToken, List<GwtFirewallPortForwardEntry> entries)
            throws GwtKuraException {

        logger.debug("updateDeviceFirewallPortForwards() :: updating port forward entries");
        checkXSRFToken(xsrfToken);
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        List<FirewallPortForwardConfigIP<? extends IPAddress>> firewallPortForwardConfigIPs = new ArrayList<>();

        try {
            for (GwtFirewallPortForwardEntry entry : entries) {
                String network = null;
                String prefix = null;

                if (entry.getPermittedNetwork() != null) {
                    String[] parts = entry.getPermittedNetwork().split("/");
                    network = parts[0];
                    prefix = parts[1];
                }

                FirewallPortForwardConfigIP<IP4Address> firewallPortForwardConfigIP = new FirewallPortForwardConfigIP4();
                firewallPortForwardConfigIP
                        .setInboundInterface(GwtSafeHtmlUtils.htmlEscape(entry.getInboundInterface()));
                firewallPortForwardConfigIP
                        .setOutboundInterface(GwtSafeHtmlUtils.htmlEscape(entry.getOutboundInterface()));
                firewallPortForwardConfigIP.setAddress(
                        (IP4Address) IPAddress.parseHostAddress(GwtSafeHtmlUtils.htmlEscape(entry.getAddress())));
                firewallPortForwardConfigIP
                        .setProtocol(NetProtocol.valueOf(GwtSafeHtmlUtils.htmlEscape(entry.getProtocol())));
                firewallPortForwardConfigIP.setInPort(entry.getInPort());
                firewallPortForwardConfigIP.setOutPort(entry.getOutPort());
                boolean masquerade = entry.getMasquerade().equals("yes") ? true : false;
                firewallPortForwardConfigIP.setMasquerade(masquerade);
                if (network != null && prefix != null) {
                    firewallPortForwardConfigIP.setPermittedNetwork(new NetworkPair<>(
                            (IP4Address) IPAddress.parseHostAddress(network), Short.parseShort(prefix)));
                }
                firewallPortForwardConfigIP.setPermittedMac(GwtSafeHtmlUtils.htmlEscape(entry.getPermittedMAC()));
                firewallPortForwardConfigIP.setSourcePortRange(GwtSafeHtmlUtils.htmlEscape(entry.getSourcePortRange()));

                logger.debug("adding port forward entry for inbound iface {} - port {}",
                        GwtSafeHtmlUtils.htmlEscape(entry.getInboundInterface()), entry.getInPort());
                firewallPortForwardConfigIPs.add(firewallPortForwardConfigIP);
            }

            nas.setFirewallPortForwardingConfiguration(firewallPortForwardConfigIPs);
        } catch (KuraException | NumberFormatException | UnknownHostException e) {
            logger.warn("Exception while updating firewall port forwards", e);
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public void updateDeviceFirewallNATs(GwtXSRFToken xsrfToken, List<GwtFirewallNatEntry> entries)
            throws GwtKuraException {

        logger.debug("updateDeviceFirewallNATs() :: updating NAT entries");
        checkXSRFToken(xsrfToken);
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        List<FirewallNatConfig> firewallNatConfigs = new ArrayList<>();

        for (GwtFirewallNatEntry entry : entries) {

            String srcNetwork = GwtSafeHtmlUtils.htmlEscape(entry.getSourceNetwork());
            String dstNetwork = GwtSafeHtmlUtils.htmlEscape(entry.getDestinationNetwork());
            if (srcNetwork == null || "".equals(srcNetwork)) {
                srcNetwork = "0.0.0.0/0";
            }
            if (dstNetwork == null || "".equals(dstNetwork)) {
                dstNetwork = "0.0.0.0/0";
            }

            boolean masquerade = entry.getMasquerade().equals("yes");

            FirewallNatConfig firewallNatConfig = new FirewallNatConfig(
                    GwtSafeHtmlUtils.htmlEscape(entry.getInInterface()),
                    GwtSafeHtmlUtils.htmlEscape(entry.getOutInterface()),
                    GwtSafeHtmlUtils.htmlEscape(entry.getProtocol()), srcNetwork, dstNetwork, masquerade,
                    RuleType.IP_FORWARDING);

            firewallNatConfigs.add(firewallNatConfig);
        }

        try {
            nas.setFirewallNatConfiguration(firewallNatConfigs);
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public void renewDhcpLease(GwtXSRFToken xsrfToken, String interfaceName) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        try {
            nas.renewDhcpLease(GwtSafeHtmlUtils.htmlEscape(interfaceName));
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    private WifiConfig getWifiConfig(GwtWifiConfig gwtWifiConfig) throws GwtKuraException {

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

    // private WifiConfig getWifiConfig(GwtWifiConfig gwtWifiConfig) throws GwtKuraException {
    private void fillWifiProperties(GwtWifiConfig gwtWifiConfig, Map<String, Object> properties, String basePropName,
            String interfaceName) throws GwtKuraException {
        // net.interface.wlp1s0.config.dhcpClient4.enabled<Boolean> = false
        // net.interface.wlp1s0.config.dhcpClient6.enabled<Boolean> = false
        // net.interface.wlp1s0.config.ip4.address<String> =
        // net.interface.wlp1s0.config.ip4.dnsServers<String> =
        // net.interface.wlp1s0.config.ip4.gateway<String> =
        // net.interface.wlp1s0.config.ip4.prefix<Short> = 24
        // net.interface.wlp1s0.config.ip4.status<String> = netIPv4StatusDisabled
        // net.interface.wlp1s0.config.ip6.dnsServers<String> =
        // net.interface.wlp1s0.config.ip6.status<String> = netIPv6StatusDisabled
        // net.interface.wlp1s0.config.wifi.infra.bgscan<String> =
        // net.interface.wlp1s0.config.wifi.infra.channel<String> = 1
        // net.interface.wlp1s0.config.wifi.infra.driver<String> = nl80211
        // net.interface.wlp1s0.config.wifi.infra.groupCiphers<String> = CCMP_TKIP
        // net.interface.wlp1s0.config.wifi.infra.ignoreSSID<Boolean> = false
        // net.interface.wlp1s0.config.wifi.infra.mode<String> = INFRA
        // net.interface.wlp1s0.config.wifi.infra.pairwiseCiphers<String> = CCMP_TKIP
        // net.interface.wlp1s0.config.wifi.infra.passphrase<String> =
        // net.interface.wlp1s0.config.wifi.infra.pingAccessPoint<Boolean> = false
        // net.interface.wlp1s0.config.wifi.infra.radioMode<String> = RADIO_MODE_80211b
        // net.interface.wlp1s0.config.wifi.infra.securityType<String> = SECURITY_NONE
        // net.interface.wlp1s0.config.wifi.infra.ssid<String> =
        // net.interface.wlp1s0.config.wifi.master.bgscan<String> =
        // net.interface.wlp1s0.config.wifi.master.channel<String> = 1
        // net.interface.wlp1s0.config.wifi.master.driver<String> = nl80211
        // net.interface.wlp1s0.config.wifi.master.groupCiphers<String> = CCMP_TKIP
        // net.interface.wlp1s0.config.wifi.master.ignoreSSID<Boolean> = false
        // net.interface.wlp1s0.config.wifi.master.mode<String> = MASTER
        // net.interface.wlp1s0.config.wifi.master.pairwiseCiphers<String> = CCMP
        // net.interface.wlp1s0.config.wifi.master.passphrase<String> =
        // qAHZ6ajx/QrkiLqh-F9x5ZNdnvw08Kl3TgU3um1FedDKuyKdB
        // net.interface.wlp1s0.config.wifi.master.pingAccessPoint<Boolean> = false
        // net.interface.wlp1s0.config.wifi.master.radioMode<String> = RADIO_MODE_80211g
        // net.interface.wlp1s0.config.wifi.master.securityType<String> = SECURITY_WPA2
        // net.interface.wlp1s0.config.wifi.master.ssid<String> = kura_gateway_0
        // net.interface.wlp1s0.config.wifi.mode<String> = MASTER
        // net.interface.wlp1s0.type<String> = WIFI

        // WifiConfig wifiConfig = new WifiConfig();
        StringBuilder wifiBasePropName = new StringBuilder(basePropName).append("wifi.");

        // WifiMode wifiMode = getWifiConfigWirelessMode(gwtWifiConfig.getWirelessMode());
        // wifiConfig.setMode(wifiMode);
        String mode = gwtWifiConfig.getWirelessMode();
        String wifiMode = WifiMode.UNKNOWN.name();
        StringBuilder wifiModeBasePropName = new StringBuilder(wifiBasePropName);
        if (mode != null && mode.equals(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name())) {
            wifiMode = WifiMode.MASTER.name();
            wifiModeBasePropName.append("master.");
        } else if (mode != null && mode.equals(GwtWifiWirelessMode.netWifiWirelessModeStation.name())) {
            wifiMode = WifiMode.INFRA.name();
            wifiModeBasePropName.append("infra.");
        } else if (mode != null && mode.equals(GwtWifiWirelessMode.netWifiWirelessModeAdHoc.name())) {
            // ????
            wifiMode = WifiMode.ADHOC.name();
        } else {
            // ????
            wifiMode = WifiMode.UNKNOWN.name();
        }
        properties.put(wifiBasePropName.append("mode").toString(), wifiMode);

        // wifiConfig.setSSID(GwtSafeHtmlUtils.htmlUnescape(gwtWifiConfig.getWirelessSsid()));
        properties.put(wifiModeBasePropName.toString() + "ssid",
                GwtSafeHtmlUtils.htmlUnescape(gwtWifiConfig.getWirelessSsid()));

        // wifiConfig.setDriver(gwtWifiConfig.getDriver());
        properties.put(wifiModeBasePropName.toString() + "driver", gwtWifiConfig.getDriver());

        // WifiRadioMode wifiRadioMode = getWifiConfigRadioMode(gwtWifiConfig.getRadioModeEnum());
        // wifiConfig.setRadioMode(wifiRadioMode);
        properties.put(wifiModeBasePropName.toString() + "radioMode",
                getWifiConfigRadioMode(gwtWifiConfig.getRadioModeEnum()).name());

        // ????
        // String hardwareMode = gwtWifiConfig.getRadioModeEnum().getRadioMode();
        // wifiConfig.setHardwareMode(hardwareMode);

        int[] wifiConfigChannels = getWifiConfigChannels(gwtWifiConfig.getChannels());
        String wifiConfigChannelsPropName = wifiModeBasePropName.toString() + "channel";
        if (wifiConfigChannels.length > 0) {
            StringBuilder wifiConfigChannelsStringBuilder = new StringBuilder();
            for (int i = 0; i < wifiConfigChannels.length; i++) {
                wifiConfigChannelsStringBuilder.append(String.valueOf(wifiConfigChannels[i]));
                if (i != wifiConfigChannels.length - 1) {
                    wifiConfigChannelsStringBuilder.append(" ");
                }
            }
            properties.put(wifiConfigChannelsPropName, wifiConfigChannelsStringBuilder.toString());
            // wifiConfig.setChannels(wifiConfigChannels);
        }

        WifiSecurity wifiSecurity = getWifiConfigSecurity(gwtWifiConfig.getSecurity());
        properties.put(wifiModeBasePropName.toString() + "securityType", wifiSecurity.name());
        // wifiConfig.setSecurity(wifiSecurity);

        WifiCiphers wifiPairwiseCiphers = getWifiConfigCiphers(gwtWifiConfig.getPairwiseCiphers());
        if (wifiPairwiseCiphers != null) {
            properties.put(wifiModeBasePropName.toString() + "pairwiseCiphers", wifiPairwiseCiphers.name());
            // wifiConfig.setPairwiseCiphers(wifiPairwiseCiphers);
        }

        WifiCiphers wifiGroupCiphers = getWifiConfigCiphers(gwtWifiConfig.getGroupCiphers());
        if (wifiGroupCiphers != null) {
            properties.put(wifiModeBasePropName.toString() + "groupCiphers", wifiGroupCiphers.name());
            // wifiConfig.setGroupCiphers(wifiGroupCiphers);
        }

        WifiBgscan wifiBgscan = getWifiConfigBgscan(gwtWifiConfig, gwtWifiConfig.getBgscanModule());
        if (wifiBgscan.getModule().equals(WifiBgscanModule.NONE)) {
            properties.put(wifiModeBasePropName.toString() + "bgscan", null);
        } else {
            String wifiBgscanString = wifiBgscan.getModule().name().toLowerCase() + ":" + wifiBgscan.getShortInterval()
                    + ":" + wifiBgscan.getRssiThreshold() + ":" + wifiBgscan.getLongInterval();
            properties.put(wifiModeBasePropName.toString() + "bgscan", wifiBgscanString);
        }
        // wifiConfig.setBgscan(wifiBgscan);

        // wifiConfig.setPasskey(GwtSafeHtmlUtils.htmlUnescape(gwtWifiConfig.getPassword()));
        // CryptoService cs = ServiceLocator.getInstance().getService(CryptoService.class);
        // try {
        // char[] passphrase = cs.encryptAes(GwtSafeHtmlUtils.htmlUnescape(gwtWifiConfig.getPassword()).toCharArray());
        // properties.put(wifiModeBasePropName.append("passphrase").toString(), new Password(passphrase));
        // } catch (KuraException e) {
        // throw new GwtKuraException(GwtKuraErrorCode.WARNING, e); // change error code. Add a new one?
        // }

        String passKey = GwtSafeHtmlUtils.htmlUnescape(gwtWifiConfig.getPassword());
        if (passKey != null && passKey.equals(PASSWORD_PLACEHOLDER)) {

            List<GwtNetInterfaceConfig> result = privateFindNetInterfaceConfigurations();
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
                    // try {
                    if (oldGwtWifiConfig != null) {
                        // CryptoService cs = ServiceLocator.getInstance().getService(CryptoService.class);
                        // char[] passphrase = cs.encryptAes(
                        // GwtSafeHtmlUtils.htmlUnescape(oldGwtWifiConfig.getPassword()).toCharArray());
                        properties.put(wifiModeBasePropName.toString() + "passphrase",
                                new Password(GwtSafeHtmlUtils.htmlUnescape(oldGwtWifiConfig.getPassword())));
                        // wifiConfig.setPasskey(GwtSafeHtmlUtils.htmlUnescape(oldGwtWifiConfig.getPassword()));
                    }
                    // } catch (KuraException e) {
                    // throw new GwtKuraException(GwtKuraErrorCode.WARNING, e); // change error code. Add a new one?
                    // }
                }
            }
        } else if (passKey != null && wifiMode.equalsIgnoreCase(WifiMode.MASTER.name())) {
            validateUserPassword(passKey);
            try {
                CryptoService cs = ServiceLocator.getInstance().getService(CryptoService.class);
                char[] passphrase = cs.encryptAes(passKey.toCharArray());
                properties.put(wifiModeBasePropName.toString() + "passphrase", new Password(passphrase));
            } catch (KuraException e) {
                throw new GwtKuraException(GwtKuraErrorCode.WARNING, e); // change error code. Add a new one?
            }
        } else {
            try {
                CryptoService cs = ServiceLocator.getInstance().getService(CryptoService.class);
                char[] passphrase = cs.encryptAes(passKey.toCharArray());
                properties.put(wifiModeBasePropName.toString() + "passphrase", new Password(passphrase));
            } catch (KuraException e) {
                throw new GwtKuraException(GwtKuraErrorCode.WARNING, e); // change error code. Add a new one?
            }
        }

        // wifiConfig.setPingAccessPoint(gwtWifiConfig.pingAccessPoint());
        properties.put(wifiModeBasePropName.toString() + "pingAccessPoint", gwtWifiConfig.pingAccessPoint());

        // wifiConfig.setIgnoreSSID(gwtWifiConfig.ignoreSSID());
        properties.put(wifiModeBasePropName.toString() + "ignoreSSID", gwtWifiConfig.ignoreSSID());

        // ????
        // wifiConfig.setBroadcast(!gwtWifiConfig.ignoreSSID());

        // wifiConfig.setWifiCountryCode(gwtWifiConfig.getCountryCode());

        // return wifiConfig;
    }

    private WifiBgscan getWifiConfigBgscan(GwtWifiConfig gwtWifiConfig, String bgscanModule) {
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

    private WifiCiphers getWifiConfigCiphers(String ciphers) {
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

    private WifiSecurity getWifiConfigSecurity(String security) {
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

    private int[] getWifiConfigChannels(List<Integer> alChannels) {
        if (alChannels == null) {
            return new int[0];
        }
        int[] channels = new int[alChannels.size()];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = alChannels.get(i).intValue();
        }
        return channels;
    }

    private WifiRadioMode getWifiConfigRadioMode(GwtWifiRadioMode radioMode) throws GwtKuraException {
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

    private WifiMode getWifiConfigWirelessMode(String mode) {
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

    private <T> T withCellularModem(final String interfaceName, final ModemManagerService modemManagerService,
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

    private boolean isModemForNetworkInterface(String ifaceName, CellularModem modemService) {
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

    @Override
    public List<GwtWifiChannelFrequency> findFrequencies(GwtXSRFToken xsrfToken, String interfaceName,
            GwtWifiRadioMode radioMode) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

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

    @Override
    public String getWifiCountryCode(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        logger.info("Get Wifi Country Code impl");
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        try {
            return nas.getWifiCountryCode();
        } catch (KuraException e) {
            logger.error("Get Wifi Country Code exception");
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public boolean isIEEE80211ACSupported(GwtXSRFToken xsrfToken, String ifaceName) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        try {
            return nas.isWifiIEEE80211AC(ifaceName);
        } catch (KuraException e) {
            logger.error("Ieee80211ac support exception");
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    private void validateUserPassword(final String password) throws GwtKuraException {
        final List<Validator<String>> validators = PasswordStrengthValidators
                .fromConfig(Console.getConsoleOptions().getUserOptions());

        final List<String> errors = new ArrayList<>();

        for (final Validator<String> validator : validators) {
            validator.validate(password, errors::add);
        }

        if (!errors.isEmpty()) {
            logger.warn("password strenght requirements not satisfied: {}", errors);
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
        }
    }

    @Override
    public List<String> getDhcpLeases(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        List<String> dhcpLease = new ArrayList<>();

        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
        try {
            List<DhcpLease> leases = nas.getDhcpLeases();

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
}
