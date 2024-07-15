/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.FirewallConfigurationService;
import org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP6;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP6;
import org.eclipse.kura.net.status.NetworkInterfaceType;
import org.eclipse.kura.web.server.net2.configuration.NetworkConfigurationServiceAdapter;
import org.eclipse.kura.web.server.net2.status.NetworkStatusServiceAdapter;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtFirewallNatEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallOpenPortEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallPortForwardEntry;
import org.eclipse.kura.web.shared.model.GwtModemPdpEntry;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiChannelFrequency;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiHotspotEntry;
import org.eclipse.kura.web.shared.model.GwtWifiRadioMode;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtNetworkServiceImpl extends OsgiRemoteServiceServlet implements GwtNetworkService {

    private static final long serialVersionUID = -4188750359099902616L;
    private static final String FIREWALL_CONFIGURATION_SERVICE_PID = "org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6";
    private static final String UNKNOWN_NETWORK_IP6_SHORT = "::/0";
    private static final String UNKNOWN_NETWORK_IP6_LONG = "0:0:0:0:0:0:0:0/0";
    private static final String UNKNOWN_NETWORK_IP4 = "0.0.0.0/0";
    private static final Logger logger = LoggerFactory.getLogger(GwtNetworkServiceImpl.class);

    @Override
    public List<GwtNetInterfaceConfig> findNetInterfaceConfigurations(boolean recompute) throws GwtKuraException {
        try {
            List<GwtNetInterfaceConfig> result = getConfigsAndStatuses();

            return GwtServerUtil.replaceNetworkConfigListSensitivePasswordsWithPlaceholder(result);

        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public void updateNetInterfaceConfigurations(GwtXSRFToken xsrfToken, GwtNetInterfaceConfig config)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        try {
            NetworkConfigurationServiceAdapter adapter = new NetworkConfigurationServiceAdapter();

            adapter.updateConfiguration(config);
        } catch (GwtKuraException | KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public ArrayList<GwtWifiHotspotEntry> findWifiHotspots(GwtXSRFToken xsrfToken, String interfaceName,
            String wirelessSsid, boolean recompute) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        try {
            List<GwtWifiHotspotEntry> aps = new NetworkStatusServiceAdapter().findWifiHotspots(interfaceName,
                    recompute);
            logger.debug("Found APs: {}", aps);
            return new ArrayList<>(aps);
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public List<GwtModemPdpEntry> findPdpContextInfo(GwtXSRFToken xsrfToken, String interfaceName)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        logger.debug("Method findPdpContextInfo not implemented yet. Returning empty list.");
        return new ArrayList<>();
    }

    @Override
    public boolean verifyWifiCredentials(GwtXSRFToken xsrfToken, String interfaceName, GwtWifiConfig gwtWifiConfig)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        logger.debug("Method verifyWifiCredentials not implemented yet. Returning false.");
        return false;
    }

    @Override
    public void renewDhcpLease(GwtXSRFToken xsrfToken, String interfaceName) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        logger.debug("Method renewDhcpLease not implemented yet.");
    }

    @Override
    public List<GwtWifiChannelFrequency> findFrequencies(GwtXSRFToken xsrfToken, String interfaceName,
            GwtWifiRadioMode radioMode) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        try {
            NetworkStatusServiceAdapter status = new NetworkStatusServiceAdapter();

            List<GwtWifiChannelFrequency> allSupportedChannels = status.getAllSupportedChannels(interfaceName);
            List<GwtWifiChannelFrequency> displayedChannels = new ArrayList<>();

            for (GwtWifiChannelFrequency supportedChannel : allSupportedChannels) {
                boolean channelIsfive5Ghz = supportedChannel.getFrequency() > 2501;
                boolean isAutomaticChannelSelection = supportedChannel.getFrequency() == 0;

                if (radioMode.isFiveGhz() && channelIsfive5Ghz || radioMode.isTwoDotFourGhz() && !channelIsfive5Ghz
                        || isAutomaticChannelSelection) {
                    displayedChannels.add(supportedChannel);
                }
            }

            if (logger.isDebugEnabled()) {
                StringBuilder toDisplay = new StringBuilder();
                for (GwtWifiChannelFrequency channel : displayedChannels) {
                    toDisplay.append(channel.getChannel());
                    toDisplay.append(" ");
                }
                logger.debug("Find frequencies for {}/{}: {}", interfaceName, radioMode.name(),
                        toDisplay.toString().trim());
            }

            return displayedChannels;
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public String getWifiCountryCode(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        try {
            NetworkStatusServiceAdapter status = new NetworkStatusServiceAdapter();
            return status.getWifiCountryCode();
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public boolean isIEEE80211ACSupported(GwtXSRFToken xsrfToken, String ifaceName) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        logger.debug("Method isIEEE80211ACSupported not implemented yet. Returning true.");
        return true;
    }

    @Override
    public List<String> getDhcpLeases(GwtXSRFToken xsrfToken, String interfaceName) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        logger.debug("Method getDhcpLeases not implemented yet. Returning empty list.");
        return new ArrayList<>();
    }

    @Override
    public List<GwtFirewallOpenPortEntry> findDeviceFirewallOpenPorts(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        FirewallConfigurationService fcs = ServiceLocator.getInstance().getService(FirewallConfigurationService.class);
        List<GwtFirewallOpenPortEntry> gwtOpenPortEntries = new ArrayList<>();

        try {
            List<NetConfig> firewallConfigs = fcs.getFirewallConfiguration().getConfigs();
            if (firewallConfigs == null || firewallConfigs.isEmpty()) {
                return new ArrayList<>();
            }
            for (NetConfig netConfig : firewallConfigs) {
                if (!(netConfig instanceof FirewallOpenPortConfigIP4)) {
                    continue;
                }
                FirewallOpenPortConfigIP4 firewallOpenPortConfigIP4 = (FirewallOpenPortConfigIP4) netConfig;
                logger.debug("findDeviceFirewallOpenPorts() :: adding new Open Port Entry: {}",
                        firewallOpenPortConfigIP4.getPort());
                gwtOpenPortEntries.add(convertToGwtFirewallOpenPortEntry(firewallOpenPortConfigIP4));
            }

            return new ArrayList<>(gwtOpenPortEntries);

        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public List<GwtFirewallOpenPortEntry> findDeviceFirewallOpenPortsIPv6(GwtXSRFToken xsrfToken)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        Optional<FirewallConfigurationServiceIPv6> fcs = Optional
                .ofNullable(ServiceLocator.getInstance().getService(FirewallConfigurationServiceIPv6.class));
        List<GwtFirewallOpenPortEntry> gwtOpenPortEntries = new ArrayList<>();

        if (!fcs.isPresent()) {
            return new ArrayList<>();
        }
        try {
            List<NetConfig> firewallConfigs = fcs.get().getFirewallConfiguration().getConfigs();
            if (firewallConfigs == null || firewallConfigs.isEmpty()) {
                return new ArrayList<>();
            }
            for (NetConfig netConfig : firewallConfigs) {
                if (!(netConfig instanceof FirewallOpenPortConfigIP6)) {
                    continue;
                }
                FirewallOpenPortConfigIP6 firewallOpenPortConfigIP6 = (FirewallOpenPortConfigIP6) netConfig;
                logger.debug("findDeviceFirewallOpenPorts() :: adding new Open Port Entry: {}",
                        firewallOpenPortConfigIP6.getPort());
                gwtOpenPortEntries.add(convertToGwtFirewallOpenPortEntry(firewallOpenPortConfigIP6));
            }
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
        return new ArrayList<>(gwtOpenPortEntries);

//        if (isNet2()) {
//            return org.eclipse.kura.web.server.net2.GwtNetworkServiceImpl.findDeviceFirewallOpenPortsIPv6();
//        } else {
//            throw new GwtKuraException(GwtKuraErrorCode.OPERATION_NOT_SUPPORTED);
//        }
    }

    @Override
    public List<GwtFirewallPortForwardEntry> findDeviceFirewallPortForwards(GwtXSRFToken xsrfToken)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        FirewallConfigurationService fcs = ServiceLocator.getInstance().getService(FirewallConfigurationService.class);
        List<GwtFirewallPortForwardEntry> gwtPortForwardEntries = new ArrayList<>();

        try {
            List<NetConfig> firewallConfigs = fcs.getFirewallConfiguration().getConfigs();
            if (firewallConfigs != null && !firewallConfigs.isEmpty()) {
                for (NetConfig netConfig : firewallConfigs) {
                    if (netConfig instanceof FirewallPortForwardConfigIP4) {
                        logger.debug("findDeviceFirewallPortForwards() :: adding new Port Forward Entry");
                        FirewallPortForwardConfigIP4 firewallPortForwardConfigIP4 = (FirewallPortForwardConfigIP4) netConfig;
                        gwtPortForwardEntries.add(convertToGwtFirewallPortForwardEntry(firewallPortForwardConfigIP4));
                    }
                }
            }

            return new ArrayList<>(gwtPortForwardEntries);

        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public List<GwtFirewallPortForwardEntry> findDeviceFirewallPortForwardsIPv6(GwtXSRFToken xsrfToken)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        Optional<FirewallConfigurationServiceIPv6> fcs = Optional
                .ofNullable(ServiceLocator.getInstance().getService(FirewallConfigurationServiceIPv6.class));
        List<GwtFirewallPortForwardEntry> gwtPortForwardEntries = new ArrayList<>();

        if (fcs.isPresent()) {
            try {
                List<NetConfig> firewallConfigs = fcs.get().getFirewallConfiguration().getConfigs();
                if (firewallConfigs != null && !firewallConfigs.isEmpty()) {
                    for (NetConfig netConfig : firewallConfigs) {
                        if (netConfig instanceof FirewallPortForwardConfigIP6) {
                            logger.debug("findDeviceFirewallPortForwards() :: adding new Port Forward Entry");
                            FirewallPortForwardConfigIP6 firewallPortForwardConfigIP6 = (FirewallPortForwardConfigIP6) netConfig;
                            gwtPortForwardEntries
                                    .add(convertToGwtFirewallPortForwardEntry(firewallPortForwardConfigIP6));
                        }
                    }
                }
            } catch (KuraException e) {
                throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
            }
        }
        return new ArrayList<>(gwtPortForwardEntries);
    }

    @Override
    public List<GwtFirewallNatEntry> findDeviceFirewallNATs(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        FirewallConfigurationService fcs = ServiceLocator.getInstance().getService(FirewallConfigurationService.class);
        List<GwtFirewallNatEntry> gwtNatEntries = new ArrayList<>();

        try {
            List<NetConfig> firewallConfigs = fcs.getFirewallConfiguration().getConfigs();
            if (firewallConfigs != null && !firewallConfigs.isEmpty()) {
                for (NetConfig netConfig : firewallConfigs) {
                    if (netConfig instanceof FirewallNatConfig) {
                        logger.debug("findDeviceFirewallNATs() :: adding new NAT Entry");
                        FirewallNatConfig firewallNatConfig = (FirewallNatConfig) netConfig;
                        gwtNatEntries.add(convertToGwtFirewallNatEntry(firewallNatConfig));
                    }
                }
            }

            return new ArrayList<>(gwtNatEntries);

        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public List<GwtFirewallNatEntry> findDeviceFirewallNATsIPv6(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        Optional<FirewallConfigurationServiceIPv6> fcs = Optional
                .ofNullable(ServiceLocator.getInstance().getService(FirewallConfigurationServiceIPv6.class));
        List<GwtFirewallNatEntry> gwtNatEntries = new ArrayList<>();

        if (fcs.isPresent()) {
            try {
                List<NetConfig> firewallConfigs = fcs.get().getFirewallConfiguration().getConfigs();
                if (firewallConfigs != null && !firewallConfigs.isEmpty()) {
                    for (NetConfig netConfig : firewallConfigs) {
                        if (netConfig instanceof FirewallNatConfig) {
                            logger.debug("findDeviceFirewallNATs() :: adding new NAT Entry");
                            FirewallNatConfig firewallNatConfig = (FirewallNatConfig) netConfig;
                            gwtNatEntries.add(convertToGwtFirewallNatEntry(firewallNatConfig));
                        }
                    }
                }
            } catch (KuraException e) {
                throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
            }
        }
        return new ArrayList<>(gwtNatEntries);
    }

    @Override
    public void updateDeviceFirewallOpenPorts(GwtXSRFToken xsrfToken, List<GwtFirewallOpenPortEntry> entries)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        ConfigurationService configurationService = ServiceLocator.getInstance().getService(ConfigurationService.class);
        Map<String, Object> properties = new HashMap<>();
        String openPortsPropName = "firewall.open.ports";
        StringBuilder openPorts = new StringBuilder();

        try {
            for (GwtFirewallOpenPortEntry entry : entries) {
                openPorts.append(entry.getPortRange()).append(",");
                openPorts.append(entry.getProtocol()).append(",");
                if (entry.getPermittedNetwork() == null || entry.getPermittedNetwork().equals(UNKNOWN_NETWORK_IP4)) {
                    openPorts.append(UNKNOWN_NETWORK_IP4);
                } else {
                    appendIP4Network(entry.getPermittedNetwork(), openPorts);
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

    @Override
    public void updateDeviceFirewallOpenPortsIPv6(GwtXSRFToken xsrfToken, List<GwtFirewallOpenPortEntry> entries)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        ConfigurationService configurationService = ServiceLocator.getInstance().getService(ConfigurationService.class);
        Map<String, Object> properties = new HashMap<>();
        String openPortsPropName = "firewall.ipv6.open.ports";
        StringBuilder openPorts = new StringBuilder();

        try {
            for (GwtFirewallOpenPortEntry entry : entries) {
                openPorts.append(entry.getPortRange()).append(",");
                openPorts.append(entry.getProtocol()).append(",");
                if (entry.getPermittedNetwork() == null
                        || entry.getPermittedNetwork().equals(UNKNOWN_NETWORK_IP6_LONG)) {
                    openPorts.append(UNKNOWN_NETWORK_IP6_SHORT);
                } else {
                    appendIP4Network(entry.getPermittedNetwork(), openPorts);
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

    @Override
    public void updateDeviceFirewallPortForwards(GwtXSRFToken xsrfToken, List<GwtFirewallPortForwardEntry> entries)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

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
                if (entry.getPermittedNetwork() == null || entry.getPermittedNetwork().equals(UNKNOWN_NETWORK_IP4)) {
                    portForwarding.append(UNKNOWN_NETWORK_IP4);
                } else {
                    appendIP4Network(entry.getPermittedNetwork(), portForwarding);
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

    @Override
    public void updateDeviceFirewallPortForwardsIPv6(GwtXSRFToken xsrfToken, List<GwtFirewallPortForwardEntry> entries)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        ConfigurationService configurationService = ServiceLocator.getInstance().getService(ConfigurationService.class);
        Map<String, Object> properties = new HashMap<>();
        String portForwardingPropName = "firewall.ipv6.port.forwarding";
        StringBuilder portForwarding = new StringBuilder();

        try {
            for (GwtFirewallPortForwardEntry entry : entries) {
                portForwarding.append(entry.getInboundInterface()).append(",");
                portForwarding.append(entry.getOutboundInterface()).append(",");
                portForwarding.append(((IP6Address) IPAddress.parseHostAddress(entry.getAddress())).getHostAddress())
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
                if (entry.getPermittedNetwork() == null
                        || entry.getPermittedNetwork().equals(UNKNOWN_NETWORK_IP6_LONG)) {
                    portForwarding.append(UNKNOWN_NETWORK_IP6_SHORT);
                } else {
                    appendIP4Network(entry.getPermittedNetwork(), portForwarding);
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

    @Override
    public void updateDeviceFirewallNATs(GwtXSRFToken xsrfToken, List<GwtFirewallNatEntry> entries)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        ConfigurationService configurationService = ServiceLocator.getInstance().getService(ConfigurationService.class);
        Map<String, Object> properties = new HashMap<>();
        String natPropName = "firewall.nat";
        StringBuilder nat = new StringBuilder();

        try {
            for (GwtFirewallNatEntry entry : entries) {
                nat.append(entry.getInInterface()).append(",");
                nat.append(entry.getOutInterface()).append(",");
                nat.append(entry.getProtocol()).append(",");
                if (UNKNOWN_NETWORK_IP4.equals(entry.getSourceNetwork())) {
                    nat.append(UNKNOWN_NETWORK_IP4);
                } else {
                    appendIP4Network(entry.getSourceNetwork(), nat);
                }
                nat.append(",");
                if (UNKNOWN_NETWORK_IP4.equals(entry.getDestinationNetwork())) {
                    nat.append(UNKNOWN_NETWORK_IP4);
                } else {
                    appendIP4Network(entry.getDestinationNetwork(), nat);
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

    @Override
    public void updateDeviceFirewallNATsIPv6(GwtXSRFToken xsrfToken, List<GwtFirewallNatEntry> entries)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        ConfigurationService configurationService = ServiceLocator.getInstance().getService(ConfigurationService.class);
        Map<String, Object> properties = new HashMap<>();
        String natPropName = "firewall.ipv6.nat";
        StringBuilder nat = new StringBuilder();

        try {
            for (GwtFirewallNatEntry entry : entries) {
                nat.append(entry.getInInterface()).append(",");
                nat.append(entry.getOutInterface()).append(",");
                nat.append(entry.getProtocol()).append(",");
                if (UNKNOWN_NETWORK_IP6_LONG.equals(entry.getSourceNetwork())) {
                    nat.append(UNKNOWN_NETWORK_IP6_SHORT);
                } else {
                    appendIP4Network(entry.getSourceNetwork(), nat);
                }
                nat.append(",");
                if (UNKNOWN_NETWORK_IP6_LONG.equals(entry.getDestinationNetwork())) {
                    nat.append(UNKNOWN_NETWORK_IP6_SHORT);
                } else {
                    appendIP4Network(entry.getDestinationNetwork(), nat);
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

    private static List<GwtNetInterfaceConfig> getConfigsAndStatuses() throws GwtKuraException, KuraException {
        NetworkConfigurationServiceAdapter configuration = new NetworkConfigurationServiceAdapter();
        NetworkStatusServiceAdapter status = new NetworkStatusServiceAdapter();

        List<String> configuredInterfaceNames = configuration.getConfiguredNetworkInterfaceNames();
        List<String> systemInterfaceNames = status.getNetInterfaces();

        List<GwtNetInterfaceConfig> result = new LinkedList<>();
        for (String ifName : systemInterfaceNames) {
            if (configuredInterfaceNames.contains(ifName)) {
                GwtNetInterfaceConfig gwtConfig = configuration.getGwtNetInterfaceConfig(ifName);
                status.fillWithStatusProperties(ifName, gwtConfig);
                result.add(gwtConfig);
            } else {
                Optional<NetworkInterfaceType> ifType = status.getNetInterfaceType(ifName);
                if (ifType.isPresent()) {
                    GwtNetInterfaceConfig gwtConfig = configuration.getDefaultGwtNetInterfaceConfig(ifName,
                            ifType.get());
                    status.fillWithStatusProperties(ifName, gwtConfig);
                    result.add(gwtConfig);
                } else {
                    logger.warn("Cannot create configuration for {}", ifName);
                }
            }
        }

        return result;
    }

    private static GwtFirewallOpenPortEntry convertToGwtFirewallOpenPortEntry(
            FirewallOpenPortConfigIP<? extends IPAddress> firewallOpenPortConfigIP) {
        GwtFirewallOpenPortEntry entry = new GwtFirewallOpenPortEntry();
        if (firewallOpenPortConfigIP.getPortRange() != null) {
            entry.setPortRange(firewallOpenPortConfigIP.getPortRange());
        } else {
            entry.setPortRange(String.valueOf(firewallOpenPortConfigIP.getPort()));
        }
        entry.setProtocol(firewallOpenPortConfigIP.getProtocol().toString());
        entry.setPermittedNetwork(firewallOpenPortConfigIP.getPermittedNetwork()
                .getIpAddress().getHostAddress() + "/"
                + firewallOpenPortConfigIP.getPermittedNetwork().getPrefix());
        entry.setPermittedInterfaceName(
                firewallOpenPortConfigIP.getPermittedInterfaceName());
        entry.setUnpermittedInterfaceName(
                firewallOpenPortConfigIP.getUnpermittedInterfaceName());
        entry.setPermittedMAC(firewallOpenPortConfigIP.getPermittedMac());
        entry.setSourcePortRange(firewallOpenPortConfigIP.getSourcePortRange());
        return entry;
    }

    private static GwtFirewallPortForwardEntry convertToGwtFirewallPortForwardEntry(
            FirewallPortForwardConfigIP<? extends IPAddress> firewallPortForwardConfigIP) {
        GwtFirewallPortForwardEntry entry = new GwtFirewallPortForwardEntry();
        entry.setInboundInterface(firewallPortForwardConfigIP.getInboundInterface());
        entry.setOutboundInterface(firewallPortForwardConfigIP.getOutboundInterface());
        entry.setAddress(firewallPortForwardConfigIP.getIPAddress().getHostAddress());
        entry.setProtocol(firewallPortForwardConfigIP.getProtocol().toString());
        entry.setInPort(firewallPortForwardConfigIP.getInPort());
        entry.setOutPort(firewallPortForwardConfigIP.getOutPort());
        String masquerade = firewallPortForwardConfigIP.isMasquerade() ? "yes" : "no";
        entry.setMasquerade(masquerade);
        entry.setPermittedNetwork(
                firewallPortForwardConfigIP.getPermittedNetwork().toString());
        entry.setPermittedMAC(firewallPortForwardConfigIP.getPermittedMac());
        entry.setSourcePortRange(firewallPortForwardConfigIP.getSourcePortRange());
        return entry;
    }

    private static GwtFirewallNatEntry convertToGwtFirewallNatEntry(FirewallNatConfig firewallNatConfig) {
        GwtFirewallNatEntry entry = new GwtFirewallNatEntry();
        entry.setInInterface(firewallNatConfig.getSourceInterface());
        entry.setOutInterface(firewallNatConfig.getDestinationInterface());
        entry.setProtocol(firewallNatConfig.getProtocol());
        entry.setSourceNetwork(firewallNatConfig.getSource());
        entry.setDestinationNetwork(firewallNatConfig.getDestination());
        String masquerade = firewallNatConfig.isMasquerade() ? "yes" : "no";
        entry.setMasquerade(masquerade);
        return entry;
    }

    private static void appendIP4Network(String address, StringBuilder stringBuilder) throws UnknownHostException {
        String[] networkAddress = address.split("/");
        if (networkAddress.length >= 2) {
            stringBuilder.append(((IP4Address) IPAddress.parseHostAddress(networkAddress[0])).getHostAddress())
                    .append("/").append(networkAddress[1]);
        }
    }

}
