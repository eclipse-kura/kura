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
 ******************************************************************************/
package org.eclipse.kura.core.net;

import java.net.UnknownHostException;
import java.util.Map;

import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP6;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP6.FirewallOpenPortConfigIP6Builder;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP6;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP6.FirewallPortForwardConfigIP6Builder;

public class FirewallConfigurationIPv6 extends FirewallConfiguration {

    public static final String OPEN_PORTS_IPV6_PROP_NAME = "firewall.ipv6.open.ports";
    public static final String PORT_FORWARDING_IPV6_PROP_NAME = "firewall.ipv6.port.forwarding";
    public static final String NAT_IPV6_PROP_NAME = "firewall.ipv6.nat";

    public FirewallConfigurationIPv6() {
        super();
    }

    public FirewallConfigurationIPv6(Map<String, Object> properties) {
        super(properties);
    }

    @Override
    public String getOpenPortsPropertyName() {
        return OPEN_PORTS_IPV6_PROP_NAME;
    }

    @Override
    public String getPortForwardingPropertyName() {
        return PORT_FORWARDING_IPV6_PROP_NAME;
    }

    @Override
    public String getNatPropertyName() {
        return NAT_IPV6_PROP_NAME;
    }

    @Override
    protected FirewallOpenPortConfigIP<? extends IPAddress> buildOpenPortConfigIP(String[] openPortRuleItems)
            throws UnknownHostException {
        FirewallOpenPortConfigIP<IP6Address> openPortEntry = null;
        FirewallOpenPortConfigIP6Builder builder = FirewallOpenPortConfigIP6.builder();
        NetProtocol protocol = NetProtocol.valueOf(openPortRuleItems[1]);
        String permittedNetwork = null;
        short permittedNetworkMask = 0;
        if (!openPortRuleItems[2].isEmpty()) {
            permittedNetwork = openPortRuleItems[2].split("/")[0];
            permittedNetworkMask = Short.parseShort(openPortRuleItems[2].split("/")[1]);
        } else {
            permittedNetwork = IP6Address.getDefaultAddress().getHostAddress();
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
                    .withPermittedNetwork(convertNetworkPairIPv6(permittedNetwork + "/" + permittedNetworkMask))
                    .withPermittedInterfaceName(permittedIface).withUnpermittedInterfaceName(unpermittedIface)
                    .withPermittedMac(permittedMAC).withSourcePortRange(sourcePortRange);
            openPortEntry = builder.build();
        } else {
            port = Integer.parseInt(openPortRuleItems[0]);
            builder.withPort(port).withProtocol(protocol)
                    .withPermittedNetwork(convertNetworkPairIPv6(permittedNetwork + "/" + permittedNetworkMask))
                    .withPermittedInterfaceName(permittedIface).withUnpermittedInterfaceName(unpermittedIface)
                    .withPermittedMac(permittedMAC).withSourcePortRange(sourcePortRange);
            openPortEntry = builder.build();
        }
        return openPortEntry;
    }

    @Override
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
            permittedNetwork = IP6Address.getDefaultAddress().getHostAddress();
        }
        String permittedMAC = null;
        if (!rulesItems[8].isEmpty()) {
            permittedMAC = rulesItems[8];
        }
        String sourcePortRange = null;
        if (!rulesItems[9].isEmpty()) {
            sourcePortRange = rulesItems[9];
        }
        FirewallPortForwardConfigIP6Builder builder = FirewallPortForwardConfigIP6.builder();
        builder.withInboundIface(inboundIface).withOutboundIface(outboundIface).withAddress((IP6Address) address)
                .withProtocol(protocol).withInPort(inPort).withOutPort(outPort).withMasquerade(masquerade)
                .withPermittedNetwork(convertNetworkPairIPv6(permittedNetwork + "/" + permittedNetworkMask))
                .withPermittedMac(permittedMAC).withSourcePortRange(sourcePortRange);

        return builder.build();
    }

    private NetworkPair<IP6Address> convertNetworkPairIPv6(String permittedNetwork) throws UnknownHostException {
        if (permittedNetwork == null || permittedNetwork.isEmpty()) {
            return new NetworkPair<>(IP6Address.getDefaultAddress(), (short) 0);
        } else {
            String[] split = permittedNetwork.split("/");
            return new NetworkPair<>((IP6Address) IPAddress.parseHostAddress(split[0]), Short.parseShort(split[1]));
        }
    }
}