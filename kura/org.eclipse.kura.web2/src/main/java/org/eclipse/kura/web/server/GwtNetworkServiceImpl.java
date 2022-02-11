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
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
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
        NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);

        logger.debug("config.getStatus(): {}", GwtSafeHtmlUtils.htmlEscape(config.getStatus()));
        String status = config.getStatus();

        boolean autoConnect = true;
        if (GwtNetIfStatus.netIPv4StatusDisabled.name().equals(status)) {
            autoConnect = false;
        }
        try {
            // Interface status
            NetInterfaceStatus netInterfaceStatus;
            if (config.getStatus().equals(GwtNetIfStatus.netIPv4StatusUnmanaged.name())) {
                netInterfaceStatus = NetInterfaceStatus.netIPv4StatusUnmanaged;
            } else if (config.getStatus().equals(GwtNetIfStatus.netIPv4StatusL2Only.name())) {
                netInterfaceStatus = NetInterfaceStatus.netIPv4StatusL2Only;
            } else if (config.getStatus().equals(GwtNetIfStatus.netIPv4StatusEnabledLAN.name())) {
                netInterfaceStatus = NetInterfaceStatus.netIPv4StatusEnabledLAN;
            } else if (config.getStatus().equals(GwtNetIfStatus.netIPv4StatusEnabledWAN.name())) {
                netInterfaceStatus = NetInterfaceStatus.netIPv4StatusEnabledWAN;
            } else {
                netInterfaceStatus = NetInterfaceStatus.netIPv4StatusDisabled;
            }

            // Set up configs
            List<NetConfig> netConfigs = new ArrayList<>();

            // Initialize NetConfigIP4 object
            NetConfigIP4 netConfig4 = new NetConfigIP4(netInterfaceStatus, autoConnect);

            // build the appropriate NetConfig objects for ethernet type
            if (config.getHwTypeEnum() == GwtNetIfType.ETHERNET || config.getHwTypeEnum() == GwtNetIfType.WIFI
                    || config.getHwTypeEnum() == GwtNetIfType.MODEM) {

                logger.debug("config.getConfigMode(): {}", config.getConfigMode());
                String regexp = "[\\s,;\\n\\t]+";
                if (GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name().equals(config.getConfigMode())) {
                    logger.debug("mode is DHCP");
                    netConfig4.setDhcp(true);
                } else {
                    logger.debug("mode is STATIC");
                    netConfig4.setDhcp(false);

                    if (config.getIpAddress() != null && !config.getIpAddress().isEmpty()) {
                        logger.debug("setting address: {}", config.getIpAddress());
                        netConfig4.setAddress((IP4Address) IPAddress.parseHostAddress(config.getIpAddress()));
                    }

                    if (config.getSubnetMask() != null && !config.getSubnetMask().isEmpty()) {
                        logger.debug("setting subnet mask: {}", config.getSubnetMask());
                        netConfig4.setSubnetMask((IP4Address) IPAddress.parseHostAddress(config.getSubnetMask()));
                    }
                    if (config.getGateway() != null && !config.getGateway().isEmpty()) {
                        logger.debug("setting gateway: {}", config.getGateway());
                        netConfig4.setGateway((IP4Address) IPAddress.parseHostAddress(config.getGateway()));
                    }
                }

                String[] dnsServersString = config.getDnsServers().split(regexp);
                if (dnsServersString != null && dnsServersString.length > 0) {
                    IP4Address dnsServer;
                    List<IP4Address> dnsServers = new ArrayList<>();
                    for (String dnsEntry : dnsServersString) {
                        if (!dnsEntry.trim().isEmpty()) {
                            logger.debug("setting DNS: {}", dnsEntry);
                            dnsServer = (IP4Address) IPAddress.parseHostAddress(dnsEntry);
                            dnsServers.add(dnsServer);
                        }
                    }
                    netConfig4.setDnsServers(dnsServers);
                }

                netConfigs.add(netConfig4);

                // TODO - add IPv6 support later...

                // Set up DHCP and NAT
                if (GwtNetIfConfigMode.netIPv4ConfigModeManual.name().equals(config.getConfigMode())) {
                    List<NetConfig> dhcpConfigs = getDhcpConfig(config); // <--
                    if (dhcpConfigs != null) {
                        logger.debug("Adding dhcp and/or nat configs to interface update config");
                        netConfigs.addAll(dhcpConfigs);
                    }
                }

                if (config.getHwTypeEnum() == GwtNetIfType.ETHERNET) {
                    nas.updateEthernetInterfaceConfig(GwtSafeHtmlUtils.htmlEscape(config.getName()), autoConnect,
                            config.getHwMTU(), netConfigs);
                }
            }

            if (config.getHwTypeEnum() == GwtNetIfType.WIFI) {

                if (config instanceof GwtWifiNetInterfaceConfig) {
                    GwtWifiConfig gwtWifiConfig = ((GwtWifiNetInterfaceConfig) config).getActiveWifiConfig();

                    if (gwtWifiConfig != null) {
                        WifiConfig wifiConfig = getWifiConfig(gwtWifiConfig);

                        String passKey = new String(wifiConfig.getPasskey().getPassword());
                        if (passKey != null && passKey.equals(PASSWORD_PLACEHOLDER)) {

                            List<GwtNetInterfaceConfig> result = privateFindNetInterfaceConfigurations();
                            for (GwtNetInterfaceConfig netConfig : result) {
                                if (netConfig instanceof GwtWifiNetInterfaceConfig
                                        && config.getName().equals(((GwtWifiNetInterfaceConfig) netConfig).getName())) {
                                    GwtWifiNetInterfaceConfig oldWifiConfig = (GwtWifiNetInterfaceConfig) netConfig;
                                    GwtWifiConfig oldGwtWifiConfig;
                                    if (gwtWifiConfig.getWirelessMode()
                                            .equals(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name())) {
                                        oldGwtWifiConfig = oldWifiConfig.getAccessPointWifiConfig();
                                    } else {
                                        oldGwtWifiConfig = oldWifiConfig.getStationWifiConfig();
                                    }

                                    if (oldGwtWifiConfig != null) {
                                        wifiConfig.setPasskey(
                                                GwtSafeHtmlUtils.htmlUnescape(oldGwtWifiConfig.getPassword()));
                                    }
                                }
                            }
                        } else if (passKey != null && wifiConfig.getMode() == WifiMode.MASTER) {
                            validateUserPassword(passKey);
                        }

                        netConfigs.add(wifiConfig);
                        nas.updateWifiInterfaceConfig(GwtSafeHtmlUtils.htmlEscape(config.getName()), autoConnect, null,
                                netConfigs);
                    }
                }
            } else if (config.getHwTypeEnum() == GwtNetIfType.MODEM) {
                if (config instanceof GwtModemInterfaceConfig) {
                    GwtModemInterfaceConfig gwtModemConfig = (GwtModemInterfaceConfig) config;

                    ModemConfig modemConfig = new ModemConfig();

                    String serialNum = gwtModemConfig.getHwSerial();
                    String modemId = gwtModemConfig.getModemId();
                    int pppNum = gwtModemConfig.getPppNum();

                    // modem enabled/disabled
                    if (netInterfaceStatus.equals(NetInterfaceStatus.netIPv4StatusEnabledWAN)) {
                        modemConfig.setEnabled(true);
                    } else {
                        modemConfig.setEnabled(false);
                    }

                    modemConfig.setApn(gwtModemConfig.getApn());
                    modemConfig.setPppNumber(gwtModemConfig.getPppNum());
                    modemConfig.setDataCompression(gwtModemConfig.getDataCompression());
                    modemConfig.setDialString(gwtModemConfig.getDialString());
                    modemConfig.setHeaderCompression(gwtModemConfig.getHeaderCompression());

                    String passKey = GwtSafeHtmlUtils.htmlUnescape(gwtModemConfig.getPassword());
                    if (passKey != null && passKey.equals(PASSWORD_PLACEHOLDER)) {
                        List<GwtNetInterfaceConfig> result = privateFindNetInterfaceConfigurations();
                        for (GwtNetInterfaceConfig netConfig : result) {
                            if (netConfig instanceof GwtModemInterfaceConfig) {
                                GwtModemInterfaceConfig oldModemConfig = (GwtModemInterfaceConfig) netConfig;
                                if (gwtModemConfig.getName().equals(oldModemConfig.getName())) {
                                    modemConfig.setPassword(oldModemConfig.getPassword());
                                }
                            }
                        }
                    } else if (passKey != null) {
                        modemConfig.setPassword(passKey);
                    }

                    modemConfig.setUsername(gwtModemConfig.getUsername());
                    modemConfig.setResetTimeout(gwtModemConfig.getResetTimeout());
                    modemConfig.setPersist(gwtModemConfig.isPersist());
                    modemConfig.setMaxFail(gwtModemConfig.getMaxFail());
                    modemConfig.setIdle(gwtModemConfig.getIdle());
                    modemConfig.setActiveFilter(gwtModemConfig.getActiveFilter());
                    modemConfig.setLcpEchoInterval(gwtModemConfig.getLcpEchoInterval());
                    modemConfig.setLcpEchoFailure(gwtModemConfig.getLcpEchoFailure());
                    modemConfig.setGpsEnabled(gwtModemConfig.isGpsEnabled());
                    modemConfig.setDiversityEnabled(gwtModemConfig.isDiversityEnabled());

                    GwtModemAuthType authType = gwtModemConfig.getAuthType();
                    if (authType != null) {
                        if (authType.equals(GwtModemAuthType.netModemAuthNONE)) {
                            modemConfig.setAuthType(ModemConfig.AuthType.NONE);
                        } else if (authType.equals(GwtModemAuthType.netModemAuthAUTO)) {
                            modemConfig.setAuthType(ModemConfig.AuthType.AUTO);
                        } else if (authType.equals(GwtModemAuthType.netModemAuthCHAP)) {
                            modemConfig.setAuthType(ModemConfig.AuthType.CHAP);
                        } else if (authType.equals(GwtModemAuthType.netModemAuthPAP)) {
                            modemConfig.setAuthType(ModemConfig.AuthType.PAP);
                        }
                    }

                    GwtModemPdpType pdpType = gwtModemConfig.getPdpType();
                    if (pdpType != null) {
                        if (pdpType.equals(GwtModemPdpType.netModemPdpIP)) {
                            modemConfig.setPdpType(ModemConfig.PdpType.IP);
                        } else if (pdpType.equals(GwtModemPdpType.netModemPdpIPv6)) {
                            modemConfig.setPdpType(ModemConfig.PdpType.IPv6);
                        } else if (pdpType.equals(GwtModemPdpType.netModemPdpPPP)) {
                            modemConfig.setPdpType(ModemConfig.PdpType.PPP);
                        } else {
                            modemConfig.setPdpType(ModemConfig.PdpType.UNKNOWN);
                        }
                    }

                    netConfigs.add(modemConfig);

                    nas.updateModemInterfaceConfig(config.getName(), serialNum, modemId, pppNum, autoConnect, -1,
                            netConfigs);
                }
            } else {
                // TODO - more types
            }

        } catch (Exception e) {
            logger.warn("Failed", e);
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
            logger.error("Failed", t);
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
            logger.warn("Failed", e);
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
            logger.warn("Failed", e);
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    // -------------------------------------------------------------------------------------
    //
    // Private Methods
    //
    // -------------------------------------------------------------------------------------

    private List<NetConfig> getDhcpConfig(GwtNetInterfaceConfig config) throws KuraException {
        // Setup the DHCP and NAT if necessary
        String routerMode = config.getRouterMode();
        if (routerMode.equals(GwtNetRouterMode.netRouterOff.name())) {
            logger.debug("DCHP and NAT are disabled");
            return null;
        } else if (routerMode.equals(GwtNetRouterMode.netRouterDchp.name())
                || routerMode.equals(GwtNetRouterMode.netRouterDchpNat.name())
                || routerMode.equals(GwtNetRouterMode.netRouterNat.name())) {
            try {
                List<NetConfig> netConfigs = new ArrayList<>();

                if (routerMode.equals(GwtNetRouterMode.netRouterDchp.name())
                        || routerMode.equals(GwtNetRouterMode.netRouterDchpNat.name())) {
                    int defaultLeaseTime = config.getRouterDhcpDefaultLease();
                    int maximumLeaseTime = config.getRouterDhcpMaxLease();
                    IP4Address routerAddress = (IP4Address) IPAddress.parseHostAddress(config.getIpAddress());
                    IP4Address rangeStart = (IP4Address) IPAddress.parseHostAddress(config.getRouterDhcpBeginAddress());
                    IP4Address rangeEnd = (IP4Address) IPAddress.parseHostAddress(config.getRouterDhcpEndAddress());
                    boolean passDns = config.getRouterDnsPass();

                    IP4Address subnetMask = (IP4Address) IPAddress.parseHostAddress(config.getRouterDhcpSubnetMask());
                    IP4Address subnet = (IP4Address) IPAddress.parseHostAddress(
                            NetworkUtil.calculateNetwork(config.getIpAddress(), config.getSubnetMask()));
                    short prefix = NetworkUtil.getNetmaskShortForm(subnetMask.getHostAddress());

                    // Use our IP as the DNS server and we'll use named to proxy DNS queries
                    List<IP4Address> dnsServers = new ArrayList<>();
                    dnsServers.add((IP4Address) IPAddress.parseHostAddress(config.getIpAddress()));

                    logger.debug("DhcpServerConfigIP4 - start: {}, end: {}, prefix: {}, subnet: {}, subnetMask: {}",
                            new Object[] { rangeStart.getHostAddress(), rangeEnd.getHostAddress(), prefix,
                                    subnet.getHostAddress(), subnetMask.getHostAddress() });
                    try {
                        DhcpServerCfg dhcpServerCfg = new DhcpServerCfg(config.getName(), true, defaultLeaseTime,
                                maximumLeaseTime, passDns);
                        DhcpServerCfgIP4 dhcpServerCfgIP4 = new DhcpServerCfgIP4(subnet, subnetMask, prefix,
                                routerAddress, rangeStart, rangeEnd, dnsServers);
                        netConfigs.add(new DhcpServerConfigIP4(dhcpServerCfg, dhcpServerCfgIP4));
                    } catch (KuraException e) {
                        logger.error(
                                "Failed to create new DhcpServerConfigIP4 object. Please verify that DHCP pool IP addresses (see below) are in the {} subnet.",
                                subnet.getHostAddress());
                        logger.error("DHCP Pool: range from {} to {}", rangeStart.getHostAddress(),
                                rangeEnd.getHostAddress());
                        logger.error("Exception: ", e);
                    }
                }

                if (routerMode.equals(GwtNetRouterMode.netRouterDchpNat.name())
                        || routerMode.equals(GwtNetRouterMode.netRouterNat.name())) {

                    /*
                     * IPAddress m_sourceNetwork; //192.168.1.0
                     * IPAddress m_netmask; //255.255.255.0
                     * String m_sourceInterface; //eth0
                     * String m_destinationInterface; //ppp0 or something similar
                     * boolean m_masquerade; //yes
                     */

                    String sourceInterface = config.getName();
                    String destinationInterface = "unknown";                        // dynamic and defined at runtime
                    boolean masquerade = true;

                    FirewallAutoNatConfig natConfig = new FirewallAutoNatConfig(sourceInterface, destinationInterface,
                            masquerade);
                    netConfigs.add(natConfig);
                }

                return netConfigs;
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
            }
        } else {
            logger.error("Unsupported routerMode: {}", routerMode);
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "Unsupported routerMode: " + routerMode);
        }
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

    private WifiBgscan getWifiConfigBgscan(GwtWifiConfig gwtWifiConfig, String bgscanModule) {
        WifiBgscanModule wifiBgscanModule = null;
        if (GwtWifiBgscanModule.netWifiBgscanMode_NONE.name().equals(bgscanModule)) {
            wifiBgscanModule = WifiBgscanModule.NONE;
        } else if (GwtWifiBgscanModule.netWifiBgscanMode_SIMPLE.name().equals(bgscanModule)) {
            wifiBgscanModule = WifiBgscanModule.SIMPLE;
        } else if (GwtWifiBgscanModule.netWifiBgscanMode_LEARN.name().equals(bgscanModule)) {
            wifiBgscanModule = WifiBgscanModule.LEARN;
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
        } else if (GwtWifiCiphers.netWifiCiphers_CCMP.name().equals(ciphers)) {
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
