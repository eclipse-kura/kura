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
package org.eclipse.kura.net.admin.ipv6;

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.net.UnknownHostException;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.ObjectFactory;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.core.net.FirewallConfiguration;
import org.eclipse.kura.core.net.FirewallConfigurationIPv6;
import org.eclipse.kura.linux.net.iptables.AbstractLinuxFirewall;
import org.eclipse.kura.linux.net.iptables.LinuxFirewallIPv6;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.admin.AbstractFirewallConfigurationServiceImpl;
import org.eclipse.kura.net.configuration.NetworkConfigurationMessages;
import org.eclipse.kura.net.configuration.NetworkConfigurationPropertyNames;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP6;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP6.FirewallOpenPortConfigIP6Builder;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP6;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP6.FirewallPortForwardConfigIP6Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirewallConfigurationServiceIPv6Impl extends
        AbstractFirewallConfigurationServiceImpl<IP6Address, FirewallOpenPortConfigIP6Builder, FirewallPortForwardConfigIP6Builder>
        implements FirewallConfigurationServiceIPv6, SelfConfiguringComponent {

    private static final Logger logger = LoggerFactory.getLogger(FirewallConfigurationServiceIPv6Impl.class);

    @Override
    protected FirewallConfigurationIPv6 buildFirewallConfigurationFromProperties(Map<String, Object> properties) {
        return new FirewallConfigurationIPv6(properties);
    }

    @Override
    protected FirewallConfigurationIPv6 buildFirewallConfiguration() {
        return new FirewallConfigurationIPv6();
    }

    @Override
    protected FirewallOpenPortConfigIP6Builder getOpenPortConfigIPBuilder() {
        return FirewallOpenPortConfigIP6.builder();
    }

    @Override
    protected FirewallPortForwardConfigIP6Builder getPortForwardConfigIPBuilder() {
        return FirewallPortForwardConfigIP6.builder();
    }

    @Override
    protected IP6Address getDefaultAddress() throws UnknownHostException {
        return IP6Address.getDefaultAddress();
    }

    @Override
    protected IP6Address getIPAddress(String address) throws UnknownHostException {
        return (IP6Address) IPAddress.parseHostAddress(address);
    }

    @Override
    protected AbstractLinuxFirewall getLinuxFirewall() {
        if (this.firewall == null) {
            this.firewall = LinuxFirewallIPv6.getInstance(this.executorService);
        }

        return this.firewall;
    }

    @Override
    public ComponentConfiguration getConfiguration() throws KuraException {
        logger.debug("getConfiguration()");
        Map<String, Object> firewallConfigurationProperties = getFirewallConfiguration().getConfigurationProperties();
        firewallConfigurationProperties.put(KURA_SERVICE_PID, PID);
        firewallConfigurationProperties.put(SERVICE_PID, PID);
        return new ComponentConfigurationImpl(PID, getDefinition(), firewallConfigurationProperties);
    }

    @Override
    protected Tocd getDefinition() {
        ObjectFactory objectFactory = new ObjectFactory();
        Tocd tocd = objectFactory.createTocd();

        tocd.setName("FirewallConfigurationServiceIPv6");
        tocd.setId("org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6");
        tocd.setDescription("Firewall Configuration Service IPV6");

        Tad tad = objectFactory.createTad();
        tad.setId(FirewallConfigurationIPv6.OPEN_PORTS_IPV6_PROP_NAME);
        tad.setName(FirewallConfigurationIPv6.OPEN_PORTS_IPV6_PROP_NAME);
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(true);
        tad.setDefault(FirewallConfiguration.DFLT_OPEN_PORTS_VALUE);
        tad.setDescription(
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.FIREWALL_OPEN_PORTS));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(FirewallConfigurationIPv6.PORT_FORWARDING_IPV6_PROP_NAME);
        tad.setName(FirewallConfigurationIPv6.PORT_FORWARDING_IPV6_PROP_NAME);
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(true);
        tad.setDefault(FirewallConfiguration.DFLT_PORT_FORWARDING_VALUE);
        tad.setDescription(
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.FIREWALL_PORT_FORWARDING));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(FirewallConfigurationIPv6.NAT_IPV6_PROP_NAME);
        tad.setName(FirewallConfigurationIPv6.NAT_IPV6_PROP_NAME);
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(true);
        tad.setDefault(FirewallConfiguration.DFLT_NAT_VALUE);
        tad.setDescription(NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.FIREWALL_NAT));
        tocd.addAD(tad);

        return tocd;
    }
}
