/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.net.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.core.net.FirewallConfiguration;
import org.eclipse.kura.linux.net.iptables.LinuxFirewall;
import org.eclipse.kura.linux.net.iptables.LocalRule;
import org.eclipse.kura.linux.net.iptables.NATRule;
import org.eclipse.kura.linux.net.iptables.PortForwardRule;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;


public class FirewallConfigurationServiceImplTest {

    @Test
    public void testActivateNullProps() {
        // only test logging

        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected LinuxFirewall getLinuxFirewall() {
                return null;
            }
        };

        ComponentContext componentContext = mock(ComponentContext.class);

        Map<String, Object> properties = null;

        svc.activate(componentContext, properties);
    }

    @Test
    public void testActivate() {
        // only test logging

        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected LinuxFirewall getLinuxFirewall() {
                return null;
            }
        };

        ComponentContext componentContext = mock(ComponentContext.class);

        Map<String, Object> properties = new HashMap<>();
        properties.put("key", "value");

        svc.activate(componentContext, properties);
    }

    @Test
    public void testGetConfiguration() throws KuraException {
        // test rules conversion into configuration properties

        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected Set<LocalRule> getLocalRules() throws KuraException {
                Set<LocalRule> result = new HashSet<>();

                IP4Address ipAddress = null;
                try {
                    ipAddress = (IP4Address) IP4Address.parseHostAddress("10.10.1.0");
                } catch (UnknownHostException e) {
                }
                NetworkPair<IP4Address> permittedNetwork = new NetworkPair<IP4Address>(ipAddress, (short) 24);
                String permittedIface = "eth0";
                String unpermittedIface = "wlan0";
                String permittedMac = null;
                String srcPorts = null;
                LocalRule rule = new LocalRule("1100:1200", "tcp", permittedNetwork, permittedIface, unpermittedIface,
                        permittedMac, srcPorts);
                result.add(rule);
                rule = new LocalRule(1300, "tcp", permittedNetwork, permittedIface, unpermittedIface, permittedMac,
                        srcPorts);
                result.add(rule);

                return result;
            }

            @Override
            protected Set<PortForwardRule> getPortForwardRules() throws KuraException {
                Set<PortForwardRule> result = new HashSet<>();

                PortForwardRule rule = new PortForwardRule("wlan0", "eth0", "10.10.1.15", "tcp", 1234, 2345, true,
                        "10.10.1.0", 24, null, null);
                result.add(rule);

                return result;
            }

            @Override
            protected Set<NATRule> getAutoNatRules() throws KuraException {
                Set<NATRule> result = new HashSet<>();
                return result;
            }

            @Override
            protected Set<NATRule> getNatRules() throws KuraException {
                Set<NATRule> result = new HashSet<>();

                NATRule rule = new NATRule("wlan0", "eth0", "tcp", null, "10.10.1.0/24", true);
                result.add(rule);

                return result;
            }
        };

        ComponentConfiguration configuration = svc.getConfiguration();

        Map<String, Object> properties = configuration.getConfigurationProperties();

        assertNotNull(properties);
        assertEquals(3, properties.size());

        assertTrue(properties.containsKey("firewall.open.ports"));
        String ports = (String) properties.get("firewall.open.ports");
        assertNotNull(ports);
        assertTrue(ports.contains("1300,tcp,10.10.1.0/24,eth0,wlan0,,,#"));
        assertTrue(ports.contains("1100:1200,tcp,10.10.1.0/24,eth0,wlan0,,,#"));

        assertTrue(properties.containsKey("firewall.nat"));
        String nat = (String) properties.get("firewall.nat");
        assertNotNull(nat);
        assertTrue(nat.contains("wlan0,eth0,tcp,,10.10.1.0/24,true,#"));

        assertTrue(properties.containsKey("firewall.port.forwarding"));
        String fwd = (String) properties.get("firewall.port.forwarding");
        assertNotNull(fwd);
        assertTrue(fwd.contains("wlan0,eth0,10.10.1.15,tcp,1234,2345,true,10.10.1.0/24,,,#"));
    }

    @Test
    public void testGetFirewallConfiguration() throws KuraException {
        // test 'raw' configuration retrieval

        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected Set<LocalRule> getLocalRules() throws KuraException {
                Set<LocalRule> result = new HashSet<>();

                IP4Address ipAddress = null;
                try {
                    ipAddress = (IP4Address) IP4Address.parseHostAddress("10.10.1.0");
                } catch (UnknownHostException e) {
                }
                NetworkPair<IP4Address> permittedNetwork = new NetworkPair<IP4Address>(ipAddress, (short) 24);
                String permittedIface = "eth0";
                String unpermittedIface = "wlan0";
                String permittedMac = null;
                String srcPorts = null;
                LocalRule rule = new LocalRule("1100:1200", "tcp", permittedNetwork, permittedIface, unpermittedIface,
                        permittedMac, srcPorts);
                result.add(rule);
                rule = new LocalRule(1300, "tcp", permittedNetwork, permittedIface, unpermittedIface, permittedMac,
                        srcPorts);
                result.add(rule);

                return result;
            }

            @Override
            protected Set<PortForwardRule> getPortForwardRules() throws KuraException {
                Set<PortForwardRule> result = new HashSet<>();

                PortForwardRule rule = new PortForwardRule("wlan0", "eth0", "10.10.1.15", "tcp", 1234, 2345, true,
                        "10.10.1.0", 24, null, null);
                result.add(rule);

                return result;
            }

            @Override
            protected Set<NATRule> getAutoNatRules() throws KuraException {
                Set<NATRule> result = new HashSet<>();

                NATRule rule = new NATRule("wlan0", "eth0", "tcp", null, "10.10.1.0/24", true);
                result.add(rule);

                return result;
            }

            @Override
            protected Set<NATRule> getNatRules() throws KuraException {
                Set<NATRule> result = new HashSet<>();

                NATRule rule = new NATRule("wlan0", "eth0", "tcp", null, "10.10.1.0/24", true);
                result.add(rule);

                return result;
            }
        };

        FirewallConfiguration configuration = svc.getFirewallConfiguration();

        List<FirewallAutoNatConfig> autoNatConfigs = configuration.getAutoNatConfigs();
        assertEquals(1, autoNatConfigs.size());
        FirewallAutoNatConfig autoNat = autoNatConfigs.get(0);
        assertEquals("wlan0", autoNat.getSourceInterface());
        assertEquals("eth0", autoNat.getDestinationInterface());
        assertTrue(autoNat.isMasquerade());

        List<FirewallNatConfig> natConfigs = configuration.getNatConfigs();
        assertEquals(1, natConfigs.size());
        FirewallNatConfig nat = natConfigs.get(0);
        assertEquals("10.10.1.0/24", nat.getDestination());
        assertEquals("eth0", nat.getDestinationInterface());
        assertEquals("tcp", nat.getProtocol());
        assertNull(nat.getSource());
        assertEquals("wlan0", nat.getSourceInterface());

        List<FirewallOpenPortConfigIP<? extends IPAddress>> portConfigs = configuration.getOpenPortConfigs();
        assertEquals(2, portConfigs.size());

        FirewallOpenPortConfigIP<? extends IPAddress> port = portConfigs.get(0);
        assertEquals("eth0", port.getPermittedInterfaceName());
        assertNull(port.getPermittedMac());
        assertEquals("10.10.1.0", port.getPermittedNetwork().getIpAddress().getHostAddress());
        assertEquals(24, port.getPermittedNetwork().getPrefix());
        assertEquals(1300, port.getPort());
        assertNull(port.getPortRange());
        assertEquals(NetProtocol.tcp, port.getProtocol());
        assertNull(port.getSourcePortRange());
        assertEquals("wlan0", port.getUnpermittedInterfaceName());

        port = portConfigs.get(1);
        assertEquals("eth0", port.getPermittedInterfaceName());
        assertNull(port.getPermittedMac());
        assertEquals("10.10.1.0", port.getPermittedNetwork().getIpAddress().getHostAddress());
        assertEquals(24, port.getPermittedNetwork().getPrefix());
        assertEquals(-1, port.getPort());
        assertEquals("1100:1200", port.getPortRange());
        assertEquals(NetProtocol.tcp, port.getProtocol());
        assertNull(port.getSourcePortRange());
        assertEquals("wlan0", port.getUnpermittedInterfaceName());

        List<FirewallPortForwardConfigIP<? extends IPAddress>> forwardConfigs = configuration.getPortForwardConfigs();
        assertEquals(1, forwardConfigs.size());

        FirewallPortForwardConfigIP<? extends IPAddress> fwd = forwardConfigs.get(0);
        assertEquals("10.10.1.15", fwd.getAddress().getHostAddress());
        assertEquals("wlan0", fwd.getInboundInterface());
        assertEquals(1234, fwd.getInPort());
        assertEquals("eth0", fwd.getOutboundInterface());
        assertEquals(2345, fwd.getOutPort());
        assertNull(fwd.getPermittedMac());
        assertEquals("10.10.1.0", fwd.getPermittedNetwork().getIpAddress().getHostAddress());
        assertEquals(24, fwd.getPermittedNetwork().getPrefix());
        assertEquals(NetProtocol.tcp, fwd.getProtocol());
        assertNull(fwd.getSourcePortRange());
    }

    @Test
    public void testUpdateFailureHandler() {
        // test updated() that fails everywhere

        boolean[] called = { false, false, false };

        Map<String, Object> properties = new HashMap<>();

        properties.put("firewall.open.ports",
                "1300,tcp,10.10.1.0/24,eth0,wlan0,,,#" + ";1100:1200,tcp,10.10.1.0/24,eth0,wlan0,,,#");

        properties.put("firewall.nat", "wlan0,eth0,tcp,,10.10.1.0/24,true,#");

        properties.put("firewall.port.forwarding", "wlan0,eth0,10.10.1.15,tcp,1234,2345,true,10.10.1.0/24,,,#");

        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected void deleteAllLocalRules() throws KuraException {
                called[0] = true;

                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR);
            }

            @Override
            protected void deleteAllPortForwardRules() throws KuraException {
                called[1] = true;

                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR);
            }

            @Override
            protected void deleteAllNatRules() throws KuraException {
                called[2] = true;

                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR);
            }
        };

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        svc.setEventAdmin(eventAdminMock);

        svc.updated(properties);

        verify(eventAdminMock, times(1)).postEvent(anyObject());

        for (int i = 0; i < called.length; i++) {
            assertTrue("Expected call " + i, called[i]);
        }
    }

    @Test
    public void testUpdateHandler() {
        // test updated() that actually applies new configuration from properties

        boolean[] called = { false, false, false, false, false, false };

        Map<String, Object> properties = new HashMap<>();

        properties.put("firewall.open.ports",
                "1300,tcp,10.10.1.0/24,eth0,wlan0,,,#" + ";1100:1200,tcp,10.10.1.0/24,eth0,wlan0,,,#");

        properties.put("firewall.nat", "wlan0,eth0,tcp,,10.10.1.0/24,true,#");

        properties.put("firewall.port.forwarding", "wlan0,eth0,10.10.1.15,tcp,1234,2345,true,10.10.1.0/24,,,#");

        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected void addLocalRules(ArrayList<LocalRule> localRules) throws KuraException {
                assertEquals(2, localRules.size());

                LocalRule localRule = localRules.get(0);
                assertEquals("eth0", localRule.getPermittedInterfaceName());
                assertNull(localRule.getPermittedMAC());
                assertEquals("10.10.1.0", localRule.getPermittedNetwork().getIpAddress().getHostAddress());
                assertEquals(24, localRule.getPermittedNetwork().getPrefix());
                assertEquals(1300, localRule.getPort());
                assertNull(localRule.getPortRange());
                assertEquals("tcp", localRule.getProtocol());
                assertNull(localRule.getSourcePortRange());
                assertEquals("wlan0", localRule.getUnpermittedInterfaceName());

                localRule = localRules.get(1);
                assertEquals("eth0", localRule.getPermittedInterfaceName());
                assertNull(localRule.getPermittedMAC());
                assertEquals("10.10.1.0", localRule.getPermittedNetwork().getIpAddress().getHostAddress());
                assertEquals(24, localRule.getPermittedNetwork().getPrefix());
                assertEquals(-1, localRule.getPort());
                assertEquals("1100:1200", localRule.getPortRange());
                assertEquals("tcp", localRule.getProtocol());
                assertNull(localRule.getSourcePortRange());
                assertEquals("wlan0", localRule.getUnpermittedInterfaceName());

                called[0] = true;
            }

            @Override
            protected void addNatRules(ArrayList<NATRule> natRules) throws KuraException {
                assertEquals(1, natRules.size());

                NATRule nat = natRules.get(0);
                assertEquals("10.10.1.0/24", nat.getDestination());
                assertEquals("eth0", nat.getDestinationInterface());
                assertEquals("tcp", nat.getProtocol());
                assertNull(nat.getSource());
                assertEquals("wlan0", nat.getSourceInterface());

                called[1] = true;
            }

            @Override
            protected void addPortForwardRules(ArrayList<PortForwardRule> portForwardRules) throws KuraException {
                assertEquals(1, portForwardRules.size());

                PortForwardRule fwd = portForwardRules.get(0);
                assertEquals("10.10.1.15", fwd.getAddress());
                assertEquals("wlan0", fwd.getInboundIface());
                assertEquals(1234, fwd.getInPort());
                assertEquals("eth0", fwd.getOutboundIface());
                assertEquals(2345, fwd.getOutPort());
                assertNull(fwd.getPermittedMAC());
                assertEquals("10.10.1.0", fwd.getPermittedNetwork());
                assertEquals(24, fwd.getPermittedNetworkMask());
                assertEquals("tcp", fwd.getProtocol());
                assertNull(fwd.getSourcePortRange());

                called[2] = true;
            }

            @Override
            protected void deleteAllLocalRules() throws KuraException {
                called[3] = true;
            }

            @Override
            protected void deleteAllPortForwardRules() throws KuraException {
                called[4] = true;
            }

            @Override
            protected void deleteAllNatRules() throws KuraException {
                called[5] = true;
            }
        };

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        svc.setEventAdmin(eventAdminMock);

        svc.updated(properties);

        verify(eventAdminMock, times(1)).postEvent(anyObject());

        for (int i = 0; i < called.length; i++) {
            assertTrue("Expected call " + i, called[i]);
        }
    }

    @Test
    public void testSetFirewallOpenPortConfiguration() throws KuraException {
        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected void deleteAllLocalRules() throws KuraException {
                // do nothing
            }

            @Override
            protected void addLocalRules(ArrayList<LocalRule> localRules) throws KuraException {
                assertEquals(1, localRules.size());

                LocalRule rule = localRules.get(0);
                assertEquals("0.0.0.0", rule.getPermittedNetwork().getIpAddress().getHostAddress());
                assertEquals(0, rule.getPermittedNetwork().getPrefix());
            }
        };

        List<FirewallOpenPortConfigIP<? extends IPAddress>> firewallConfiguration = new ArrayList<>();
        FirewallOpenPortConfigIP4 port = new FirewallOpenPortConfigIP4(1234, NetProtocol.tcp, null, null, null, null,
                null);
        firewallConfiguration.add(port);

        svc.setFirewallOpenPortConfiguration(firewallConfiguration);

    }

}
