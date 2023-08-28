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
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4;
import org.eclipse.kura.net.firewall.RuleType;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4.FirewallOpenPortConfigIP4Builder;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4.FirewallPortForwardConfigIP4Builder;
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

    private final List<FirewallOpenPortConfigIP<? extends IPAddress>> openPortConfigs;
    private final List<FirewallPortForwardConfigIP<? extends IPAddress>> portForwardConfigs;
    private final List<FirewallNatConfig> natConfigs;
    private final List<FirewallAutoNatConfig> autoNatConfigs;

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
        String str;
        String[] astr;
        if (properties.containsKey(NAT_PROP_NAME)) {
            str = (String) properties.get(NAT_PROP_NAME);
            if (!str.isEmpty()) {
                astr = str.split(";");
                Arrays.asList(astr).forEach(this::parseNatRule);
            }
        }
    }

    private void parseNatRule(String sop) {
        String[] sa = sop.split(",");
        if (sa.length == 7 && "#".equals(sa[6])) {
            String srcIface = null;
            if (!sa[0].isEmpty()) {
                srcIface = sa[0];
            }
            String dstIface = null;
            if (!sa[1].isEmpty()) {
                dstIface = sa[1];
            }
            String protocol = null;
            if (!sa[2].isEmpty()) {
                protocol = sa[2];
            }
            String src = null;
            if (!sa[3].isEmpty()) {
                src = sa[3];
            }
            String dst = null;
            if (!sa[4].isEmpty()) {
                dst = sa[4];
            }
            boolean masquerade = Boolean.parseBoolean(sa[5]);
            FirewallNatConfig natEntry = new FirewallNatConfig(srcIface, dstIface, protocol, src, dst, masquerade,
                    RuleType.IP_FORWARDING);
            this.natConfigs.add(natEntry);
        }
    }

    private void parsePortForwardingRules(Map<String, Object> properties) {
        String str;
        String[] astr;
        if (properties.containsKey(PORT_FORWARDING_PROP_NAME)) {
            str = (String) properties.get(PORT_FORWARDING_PROP_NAME);
            if (!str.isEmpty()) {
                astr = str.split(";");
                Arrays.asList(astr).forEach(this::parsePortForwardingRule);
            }
        }
    }

    private void parsePortForwardingRule(String sop) {
        try {
            String[] sa = sop.split(",");
            if (sa.length == 11 && "#".equals(sa[10])) {
                String inboundIface = null;
                if (!sa[0].isEmpty()) {
                    inboundIface = sa[0];
                }
                String outboundIface = null;
                if (!sa[1].isEmpty()) {
                    outboundIface = sa[1];
                }
                IP4Address address = (IP4Address) IPAddress.parseHostAddress(sa[2]);
                NetProtocol protocol = NetProtocol.valueOf(sa[3]);
                int inPort = Integer.parseInt(sa[4]);
                int outPort = Integer.parseInt(sa[5]);
                boolean masquerade = Boolean.parseBoolean(sa[6]);
                String permittedNetwork = null;
                short permittedNetworkMask = 0;
                if (!sa[7].isEmpty()) {
                    permittedNetwork = sa[7].split("/")[0];
                    permittedNetworkMask = Short.parseShort(sa[7].split("/")[1]);
                } else {
                    permittedNetwork = "0.0.0.0";
                }
                String permittedMAC = null;
                if (!sa[8].isEmpty()) {
                    permittedMAC = sa[8];
                }
                String sourcePortRange = null;
                if (!sa[9].isEmpty()) {
                    sourcePortRange = sa[9];
                }
                FirewallPortForwardConfigIP4Builder builder = FirewallPortForwardConfigIP4.builder();
                builder.withInboundIface(inboundIface).withOutboundIface(outboundIface).withAddress(address)
                        .withProtocol(protocol).withInPort(inPort).withOutPort(outPort).withMasquerade(masquerade)
                        .withPermittedNetwork(new NetworkPair<>(
                                (IP4Address) IPAddress.parseHostAddress(permittedNetwork), permittedNetworkMask))
                        .withPermittedMac(permittedMAC).withSourcePortRange(sourcePortRange);

                this.portForwardConfigs.add(builder.build());
            }
        } catch (Exception e) {
            logger.error("Failed to parse Port Forward Entry", e);
        }
    }

    private void parseOpenPortRules(Map<String, Object> properties) {
        String str;
        String[] astr;
        if (properties.containsKey(OPEN_PORTS_PROP_NAME)) {
            str = (String) properties.get(OPEN_PORTS_PROP_NAME);
            if (!str.isEmpty()) {
                astr = str.split(";");
                Arrays.asList(astr).forEach(this::parseOpenPortRule);
            }
        }
    }

    private void parseOpenPortRule(String sop) {
        try {
            String[] sa = sop.split(",");
            if (sa.length == 8 && "#".equals(sa[7])) {
                this.openPortConfigs.add(buildOpenPortConfigIP(sa));
            }
        } catch (Exception e) {
            logger.error("Failed to parse Open Port Entry", e);
        }
    }

    private FirewallOpenPortConfigIP<? extends IPAddress> buildOpenPortConfigIP(String[] sa)
            throws UnknownHostException {
        FirewallOpenPortConfigIP<? extends IPAddress> openPortEntry = null;
        FirewallOpenPortConfigIP4Builder builder = FirewallOpenPortConfigIP4.builder();
        NetProtocol protocol = NetProtocol.valueOf(sa[1]);
        String permittedNetwork = "0.0.0.0";
        short permittedNetworkMask = 0;
        if (!sa[2].isEmpty()) {
            permittedNetwork = sa[2].split("/")[0];
            permittedNetworkMask = Short.parseShort(sa[2].split("/")[1]);
        }
        String permittedIface = null;
        if (!sa[3].isEmpty()) {
            permittedIface = sa[3];
        }
        String unpermittedIface = null;
        if (!sa[4].isEmpty()) {
            unpermittedIface = sa[4];
        }
        String permittedMAC = null;
        if (!sa[5].isEmpty()) {
            permittedMAC = sa[5];
        }
        String sourcePortRange = null;
        if (!sa[6].isEmpty()) {
            sourcePortRange = sa[6];
        }
        int port = 0;
        String portRange = null;
        if (sa[0].contains(":")) {
            portRange = sa[0];
            builder.withPortRange(portRange).withProtocol(protocol)
                    .withPermittedNetwork(new NetworkPair<>((IP4Address) IPAddress.parseHostAddress(permittedNetwork),
                            permittedNetworkMask))
                    .withPermittedInterfaceName(permittedIface).withUnpermittedInterfaceName(unpermittedIface)
                    .withPermittedMac(permittedMAC).withSourcePortRange(sourcePortRange);
            openPortEntry = builder.build();
        } else {
            port = Integer.parseInt(sa[0]);
            builder.withPort(port).withProtocol(protocol)
                    .withPermittedNetwork(new NetworkPair<>((IP4Address) IPAddress.parseHostAddress(permittedNetwork),
                            permittedNetworkMask))
                    .withPermittedInterfaceName(permittedIface).withUnpermittedInterfaceName(unpermittedIface)
                    .withPermittedMac(permittedMAC).withSourcePortRange(sourcePortRange);
            openPortEntry = builder.build();
        }
        return openPortEntry;
    }

    public void addConfig(NetConfig netConfig) {
        if (netConfig instanceof FirewallOpenPortConfigIP4) {
            this.openPortConfigs.add((FirewallOpenPortConfigIP4) netConfig);
        } else if (netConfig instanceof FirewallPortForwardConfigIP4) {
            this.portForwardConfigs.add((FirewallPortForwardConfigIP4) netConfig);
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
        props.put(OPEN_PORTS_PROP_NAME, formOpenPortConfigPropValue());
        props.put(PORT_FORWARDING_PROP_NAME, formPortForwardConfigPropValue());
        props.put(NAT_PROP_NAME, formNatConfigPropValue());
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
