/*******************************************************************************
 * Copyright (c) 2016, 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.net;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4.FirewallOpenPortConfigIP4Builder;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP6;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4.FirewallPortForwardConfigIP4Builder;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP6;
import org.eclipse.kura.net.firewall.RuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirewallConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(FirewallConfiguration.class);

    public static final String OPEN_PORTS_PROP_NAME = "firewall.open.ports";
    public static final String PORT_FORWARDING_PROP_NAME = "firewall.port.forwarding";
    public static final String NAT_PROP_NAME = "firewall.nat";
    public static final String DFLT_OPEN_PORTS_VALUE = "";
    public static final String DFLT_PORT_FORWARDING_VALUE = "";
    public static final String DFLT_NAT_VALUE = "";

    private List<FirewallOpenPortConfigIP<? extends IPAddress>> openPortConfigs;
    private List<FirewallPortForwardConfigIP<? extends IPAddress>> portForwardConfigs;
    private List<FirewallNatConfig> natConfigs;
    private List<FirewallAutoNatConfig> autoNatConfigs;

    public FirewallConfiguration() {
        this.openPortConfigs = new ArrayList<>();
        this.portForwardConfigs = new ArrayList<>();
        this.natConfigs = new ArrayList<>();
        this.autoNatConfigs = new ArrayList<>();
    }

    public FirewallConfiguration(Map<String, Object> properties) {
        this();
        parseOpenPortRules(properties);
        parsePortForwardingRules(properties);
        parseNatRules(properties);
    }

    private void parseNatRules(Map<String, Object> properties) {
        String propertyValue;
        String[] natRules;
        if (properties.containsKey(getNatPropertyName())) {
            propertyValue = (String) properties.get(getNatPropertyName());
            if (!propertyValue.isEmpty()) {
                natRules = propertyValue.split(";");
                Arrays.asList(natRules).forEach(this::parseNatRule);
            }
        }
    }

    private void parseNatRule(String rule) {
        String[] rulesItems = rule.split(",");
        if (rulesItems.length == 7 && "#".equals(rulesItems[6])) {
            String srcIface = null;
            if (!rulesItems[0].isEmpty()) {
                srcIface = rulesItems[0];
            }
            String dstIface = null;
            if (!rulesItems[1].isEmpty()) {
                dstIface = rulesItems[1];
            }
            String protocol = null;
            if (!rulesItems[2].isEmpty()) {
                protocol = rulesItems[2];
            }
            String src = null;
            if (!rulesItems[3].isEmpty()) {
                src = rulesItems[3];
            }
            String dst = null;
            if (!rulesItems[4].isEmpty()) {
                dst = rulesItems[4];
            }
            boolean masquerade = Boolean.parseBoolean(rulesItems[5]);
            FirewallNatConfig natEntry = new FirewallNatConfig(srcIface, dstIface, protocol, src, dst, masquerade,
                    RuleType.IP_FORWARDING);
            this.natConfigs.add(natEntry);
        }
    }

    private void parsePortForwardingRules(Map<String, Object> properties) {
        String propertyValue;
        String[] portForwardingRules;
        if (properties.containsKey(getPortForwardingPropertyName())) {
            propertyValue = (String) properties.get(getPortForwardingPropertyName());
            if (!propertyValue.isEmpty()) {
                portForwardingRules = propertyValue.split(";");
                Arrays.asList(portForwardingRules).forEach(this::parsePortForwardingRule);
            }
        }
    }

    private void parsePortForwardingRule(String rule) {
        try {
            String[] rulesItems = rule.split(",");
            if (rulesItems.length == 11 && "#".equals(rulesItems[10])) {
                this.portForwardConfigs.add(buildPortForwardConfigIP(rulesItems));
            }
        } catch (Exception e) {
            logger.error("Failed to parse Port Forward Entry", e);
        }
    }

    protected FirewallPortForwardConfigIP<? extends IPAddress> buildPortForwardConfigIP(String[] rulesItems)
            throws UnknownHostException {
        String inboundIface = null;
        if (!rulesItems[0].isEmpty()) {
            inboundIface = rulesItems[0];
        }
        String outboundIface = null;
        if (!rulesItems[1].isEmpty()) {
            outboundIface = rulesItems[1];
        }
        IPAddress address = IPAddress.parseHostAddress(rulesItems[2]);
        NetProtocol protocol = NetProtocol.valueOf(rulesItems[3]);
        int inPort = Integer.parseInt(rulesItems[4]);
        int outPort = Integer.parseInt(rulesItems[5]);
        boolean masquerade = Boolean.parseBoolean(rulesItems[6]);
        String permittedNetwork = null;
        short permittedNetworkMask = 0;
        if (!rulesItems[7].isEmpty()) {
            permittedNetwork = rulesItems[7].split("/")[0];
            permittedNetworkMask = Short.parseShort(rulesItems[7].split("/")[1]);
        } else {
            permittedNetwork = IP4Address.getDefaultAddress().getHostAddress();
        }
        String permittedMAC = null;
        if (!rulesItems[8].isEmpty()) {
            permittedMAC = rulesItems[8];
        }
        String sourcePortRange = null;
        if (!rulesItems[9].isEmpty()) {
            sourcePortRange = rulesItems[9];
        }
        FirewallPortForwardConfigIP4Builder builder = FirewallPortForwardConfigIP4.builder();
        builder.withInboundIface(inboundIface).withOutboundIface(outboundIface).withAddress((IP4Address) address)
                .withProtocol(protocol).withInPort(inPort).withOutPort(outPort).withMasquerade(masquerade)
                .withPermittedNetwork(convertNetworkPairIPv4(permittedNetwork + "/" + permittedNetworkMask))
                .withPermittedMac(permittedMAC).withSourcePortRange(sourcePortRange);

        return builder.build();
    }

    private void parseOpenPortRules(Map<String, Object> properties) {
        String propertyValue;
        String[] openPortRules;
        if (properties.containsKey(getOpenPortsPropertyName())) {
            propertyValue = (String) properties.get(getOpenPortsPropertyName());
            if (!propertyValue.isEmpty()) {
                openPortRules = propertyValue.split(";");
                Arrays.asList(openPortRules).forEach(this::parseOpenPortRule);
            }
        }
    }

    private void parseOpenPortRule(String rule) {
        try {
            String[] ruleItems = rule.split(",");
            if (ruleItems.length == 8 && "#".equals(ruleItems[7])) {
                this.openPortConfigs.add(buildOpenPortConfigIP(ruleItems));
            }
        } catch (Exception e) {
            logger.error("Failed to parse Open Port Entry", e);
        }
    }

    protected FirewallOpenPortConfigIP<? extends IPAddress> buildOpenPortConfigIP(String[] openPortRuleItems)
            throws UnknownHostException {
        FirewallOpenPortConfigIP<IP4Address> openPortEntry = null;
        FirewallOpenPortConfigIP4Builder builder = FirewallOpenPortConfigIP4.builder();
        NetProtocol protocol = NetProtocol.valueOf(openPortRuleItems[1]);
        String permittedNetwork = null;
        short permittedNetworkMask = 0;
        if (!openPortRuleItems[2].isEmpty()) {
            permittedNetwork = openPortRuleItems[2].split("/")[0];
            permittedNetworkMask = Short.parseShort(openPortRuleItems[2].split("/")[1]);
        } else {
            permittedNetwork = IP4Address.getDefaultAddress().getHostAddress();
        }
        String permittedIface = null;
        if (!openPortRuleItems[3].isEmpty()) {
            permittedIface = openPortRuleItems[3];
        }
        String unpermittedIface = null;
        if (!openPortRuleItems[4].isEmpty()) {
            unpermittedIface = openPortRuleItems[4];
        }
        String permittedMAC = null;
        if (!openPortRuleItems[5].isEmpty()) {
            permittedMAC = openPortRuleItems[5];
        }
        String sourcePortRange = null;
        if (!openPortRuleItems[6].isEmpty()) {
            sourcePortRange = openPortRuleItems[6];
        }
        int port = 0;
        String portRange = null;
        if (openPortRuleItems[0].contains(":")) {
            portRange = openPortRuleItems[0];
            builder.withPortRange(portRange).withProtocol(protocol)
                    .withPermittedNetwork(convertNetworkPairIPv4(permittedNetwork + "/" + permittedNetworkMask))
                    .withPermittedInterfaceName(permittedIface).withUnpermittedInterfaceName(unpermittedIface)
                    .withPermittedMac(permittedMAC).withSourcePortRange(sourcePortRange);
            openPortEntry = builder.build();
        } else {
            port = Integer.parseInt(openPortRuleItems[0]);
            builder.withPort(port).withProtocol(protocol)
                    .withPermittedNetwork(convertNetworkPairIPv4(permittedNetwork + "/" + permittedNetworkMask))
                    .withPermittedInterfaceName(permittedIface).withUnpermittedInterfaceName(unpermittedIface)
                    .withPermittedMac(permittedMAC).withSourcePortRange(sourcePortRange);
            openPortEntry = builder.build();
        }
        return openPortEntry;
    }

    public String getOpenPortsPropertyName() {
        return OPEN_PORTS_PROP_NAME;
    }

    public String getPortForwardingPropertyName() {
        return PORT_FORWARDING_PROP_NAME;
    }

    public String getNatPropertyName() {
        return NAT_PROP_NAME;
    }

    private NetworkPair<IP4Address> convertNetworkPairIPv4(String permittedNetwork) throws UnknownHostException {
        if (permittedNetwork == null || permittedNetwork.isEmpty()) {
            return new NetworkPair<>(IP4Address.getDefaultAddress(), (short) 0);
        } else {
            String[] split = permittedNetwork.split("/");
            return new NetworkPair<>((IP4Address) IPAddress.parseHostAddress(split[0]), Short.parseShort(split[1]));
        }
    }

    public void addConfig(NetConfig netConfig) {
        if (netConfig instanceof FirewallOpenPortConfigIP4) {
            this.openPortConfigs.add((FirewallOpenPortConfigIP4) netConfig);
        } else if (netConfig instanceof FirewallPortForwardConfigIP4) {
            this.portForwardConfigs.add((FirewallPortForwardConfigIP4) netConfig);
        } else if (netConfig instanceof FirewallOpenPortConfigIP6) {
            this.openPortConfigs.add((FirewallOpenPortConfigIP6) netConfig);
        } else if (netConfig instanceof FirewallPortForwardConfigIP6) {
            this.portForwardConfigs.add((FirewallPortForwardConfigIP6) netConfig);
        } else if (netConfig instanceof FirewallNatConfig) {
            this.natConfigs.add((FirewallNatConfig) netConfig);
        } else if (netConfig instanceof FirewallAutoNatConfig) {
            this.autoNatConfigs.add((FirewallAutoNatConfig) netConfig);
        }
    }

    public List<NetConfig> getConfigs() {
        List<NetConfig> netConfigs = new ArrayList<>();

        for (FirewallOpenPortConfigIP<? extends IPAddress> openPortConfig : this.openPortConfigs) {
            netConfigs.add(openPortConfig);
        }
        for (FirewallPortForwardConfigIP<? extends IPAddress> portForwardConfig : this.portForwardConfigs) {
            netConfigs.add(portForwardConfig);
        }
        for (FirewallNatConfig natConfig : this.natConfigs) {
            netConfigs.add(natConfig);
        }
        for (FirewallAutoNatConfig autoNatConfig : this.autoNatConfigs) {
            netConfigs.add(autoNatConfig);
        }

        return netConfigs;
    }

    public List<FirewallOpenPortConfigIP<? extends IPAddress>> getOpenPortConfigs() {
        return this.openPortConfigs;
    }

    public List<FirewallPortForwardConfigIP<? extends IPAddress>> getPortForwardConfigs() {
        return this.portForwardConfigs;
    }

    public List<FirewallNatConfig> getNatConfigs() {
        return this.natConfigs;
    }

    public List<FirewallAutoNatConfig> getAutoNatConfigs() {
        return this.autoNatConfigs;
    }

    public Map<String, Object> getConfigurationProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put(getOpenPortsPropertyName(), formOpenPortConfigPropValue());
        props.put(getPortForwardingPropertyName(), formPortForwardConfigPropValue());
        props.put(getNatPropertyName(), formNatConfigPropValue());
        return props;
    }

    private String formOpenPortConfigPropValue() {
        StringBuilder sb = new StringBuilder();
        this.openPortConfigs.forEach(openPortConfig -> {
            String port = openPortConfig.getPortRange();
            if (port == null) {
                port = Integer.toString(openPortConfig.getPort());
            }
            sb.append(port).append(',');
            if (openPortConfig.getProtocol() != null) {
                sb.append(openPortConfig.getProtocol());
            }
            sb.append(',');
            if (openPortConfig.getPermittedNetwork() != null) {
                sb.append(openPortConfig.getPermittedNetwork());
            }
            sb.append(',');
            if (openPortConfig.getPermittedInterfaceName() != null) {
                sb.append(openPortConfig.getPermittedInterfaceName());
            }
            sb.append(',');
            if (openPortConfig.getUnpermittedInterfaceName() != null) {
                sb.append(openPortConfig.getUnpermittedInterfaceName());
            }
            sb.append(',');
            if (openPortConfig.getPermittedMac() != null) {
                sb.append(openPortConfig.getPermittedMac());
            }
            sb.append(',');
            if (openPortConfig.getSourcePortRange() != null) {
                sb.append(openPortConfig.getSourcePortRange());
            }
            sb.append(",#;");
        });
        int ind = sb.lastIndexOf(";");
        if (ind > 0) {
            sb.deleteCharAt(ind);
        }
        return sb.toString();
    }

    private String formPortForwardConfigPropValue() {
        StringBuilder sb = new StringBuilder();
        this.portForwardConfigs.forEach(portForwardConfig -> {
            if (portForwardConfig.getInboundInterface() != null) {
                sb.append(portForwardConfig.getInboundInterface());
            }
            sb.append(',');
            if (portForwardConfig.getOutboundInterface() != null) {
                sb.append(portForwardConfig.getOutboundInterface());
            }
            sb.append(',');
            if (portForwardConfig.getIPAddress() != null) {
                sb.append(portForwardConfig.getIPAddress());
            }
            sb.append(',');
            if (portForwardConfig.getProtocol() != null) {
                sb.append(portForwardConfig.getProtocol());
            }
            sb.append(',');
            sb.append(portForwardConfig.getInPort()).append(',');
            sb.append(portForwardConfig.getOutPort()).append(',');
            sb.append(portForwardConfig.isMasquerade()).append(',');
            if (portForwardConfig.getPermittedNetwork() != null) {
                sb.append(portForwardConfig.getPermittedNetwork());
            }
            sb.append(',');
            if (portForwardConfig.getPermittedMac() != null) {
                sb.append(portForwardConfig.getPermittedMac());
            }
            sb.append(',');
            if (portForwardConfig.getSourcePortRange() != null) {
                sb.append(portForwardConfig.getSourcePortRange());
            }
            sb.append(",#;");
        });
        int ind = sb.lastIndexOf(";");
        if (ind > 0) {
            sb.deleteCharAt(ind);
        }
        return sb.toString();
    }

    private String formNatConfigPropValue() {
        StringBuilder sb = new StringBuilder();
        for (FirewallNatConfig natConfig : this.natConfigs) {
            if (natConfig.getSourceInterface() != null) {
                sb.append(natConfig.getSourceInterface());
            }
            sb.append(',');
            if (natConfig.getDestinationInterface() != null) {
                sb.append(natConfig.getDestinationInterface());
            }
            sb.append(',');
            if (natConfig.getProtocol() != null) {
                sb.append(natConfig.getProtocol());
            }
            sb.append(',');
            if (natConfig.getSource() != null) {
                sb.append(natConfig.getSource());
            }
            sb.append(',');
            if (natConfig.getDestination() != null) {
                sb.append(natConfig.getDestination());
            }
            sb.append(',');
            sb.append(natConfig.isMasquerade()).append(",#;");
        }
        int ind = sb.lastIndexOf(";");
        if (ind > 0) {
            sb.deleteCharAt(ind);
        }
        return sb.toString();
    }
}
