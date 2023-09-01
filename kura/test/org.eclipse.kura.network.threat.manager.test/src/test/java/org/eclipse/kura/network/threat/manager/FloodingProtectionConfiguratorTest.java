/*******************************************************************************
 * Copyright (c) 2021, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.network.threat.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.internal.floodingprotection.FloodingProtectionConfigurator;
import org.eclipse.kura.net.admin.FirewallConfigurationService;
import org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6;
import org.junit.Before;
import org.junit.Test;

public class FloodingProtectionConfiguratorTest {

    private static final String[] FLOODING_PROTECTION_MANGLE_RULES = {
            "-A prerouting-kura -m conntrack --ctstate INVALID -j DROP",
            "-A prerouting-kura -p tcp ! --syn -m conntrack --ctstate NEW -j DROP",
            "-A prerouting-kura -p tcp -m conntrack --ctstate NEW -m tcpmss ! --mss 536:65535 -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags FIN,SYN FIN,SYN -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags SYN,RST SYN,RST -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags FIN,RST FIN,RST -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags FIN,ACK FIN -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ACK,URG URG -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ACK,FIN FIN -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ACK,PSH PSH -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ALL ALL -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ALL NONE -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ALL FIN,PSH,URG -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ALL SYN,FIN,PSH,URG -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ALL SYN,RST,ACK,FIN,URG -j DROP",
            "-A prerouting-kura -p icmp -j DROP", "-A prerouting-kura -f -j DROP" };

    private static final String[] FLOODING_PROTECTION_MANGLE_RULES_IPV6 = {
            "-A prerouting-kura -m conntrack --ctstate INVALID -j DROP",
            "-A prerouting-kura -p tcp ! --syn -m conntrack --ctstate NEW -j DROP",
            "-A prerouting-kura -p tcp -m conntrack --ctstate NEW -m tcpmss ! --mss 536:65535 -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags FIN,SYN FIN,SYN -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags SYN,RST SYN,RST -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags FIN,RST FIN,RST -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags FIN,ACK FIN -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ACK,URG URG -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ACK,FIN FIN -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ACK,PSH PSH -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ALL ALL -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ALL NONE -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ALL FIN,PSH,URG -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ALL SYN,FIN,PSH,URG -j DROP",
            "-A prerouting-kura -p tcp --tcp-flags ALL SYN,RST,ACK,FIN,URG -j DROP",
            "-A prerouting-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 128 -j DROP",
            "-A prerouting-kura -p ipv6-icmp -m ipv6-icmp --icmpv6-type 129 -j DROP",
            "-A prerouting-kura -m ipv6header --header dst --soft -j DROP",
            "-A prerouting-kura -m ipv6header --header hop --soft -j DROP",
            "-A prerouting-kura -m ipv6header --header route --soft -j DROP",
            "-A prerouting-kura -m ipv6header --header frag --soft -j DROP",
            "-A prerouting-kura -m ipv6header --header auth --soft -j DROP",
            "-A prerouting-kura -m ipv6header --header esp --soft -j DROP",
            "-A prerouting-kura -m ipv6header --header none --soft -j DROP",
            "-A prerouting-kura -m rt --rt-type 0 -j DROP", "-A output-kura -m rt --rt-type 0 -j DROP" };

    private FloodingProtectionConfigurator floodingProtectionConfigurator;
    private FirewallConfigurationService mockFirewallService;
    private FirewallConfigurationServiceIPv6 mockFirewallServiceIPv6;
    private final Map<String, Object> properties = new HashMap<>();

    @Before
    public void setupTests() {
        this.floodingProtectionConfigurator = new FloodingProtectionConfigurator();
        this.mockFirewallService = mock(FirewallConfigurationService.class);
        this.mockFirewallServiceIPv6 = mock(FirewallConfigurationServiceIPv6.class);
        this.floodingProtectionConfigurator.setFirewallConfigurationService(this.mockFirewallService);
        this.floodingProtectionConfigurator.setFirewallConfigurationServiceIPv6(this.mockFirewallServiceIPv6);
        this.properties.put("flooding.protection.enabled", true);
        this.properties.put("flooding.protection.enabled.ipv6", true);
    }

    @Test
    public void getConfigurationTest() throws KuraException, NoSuchFieldException {
        this.floodingProtectionConfigurator.updated(this.properties);
        ComponentConfiguration config = this.floodingProtectionConfigurator.getConfiguration();

        assertNotNull(config);
        assertEquals("org.eclipse.kura.internal.floodingprotection.FloodingProtectionConfigurator", config.getPid());
        assertTrue((boolean) config.getConfigurationProperties().get("flooding.protection.enabled"));
        assertTrue((boolean) config.getConfigurationProperties().get("flooding.protection.enabled.ipv6"));
    }

    @Test
    public void getFloodingProtectionFilterRulesTest() {
        this.floodingProtectionConfigurator.updated(this.properties);
        assertNotNull(this.floodingProtectionConfigurator.getFloodingProtectionFilterRules());
        assertTrue(this.floodingProtectionConfigurator.getFloodingProtectionFilterRules().isEmpty());
    }

    @Test
    public void getFloodingProtectionFilterRulesIPv6Test() {
        this.floodingProtectionConfigurator.updated(this.properties);
        assertNotNull(this.floodingProtectionConfigurator.getFloodingProtectionFilterRulesIPv6());
        assertTrue(this.floodingProtectionConfigurator.getFloodingProtectionFilterRulesIPv6().isEmpty());
    }

    @Test
    public void getFloodingProtectionNatRulesTest() {
        this.floodingProtectionConfigurator.updated(this.properties);
        assertNotNull(this.floodingProtectionConfigurator.getFloodingProtectionNatRules());
        assertTrue(this.floodingProtectionConfigurator.getFloodingProtectionNatRules().isEmpty());
    }

    @Test
    public void getFloodingProtectionNatRulesIPv6Test() {
        this.floodingProtectionConfigurator.updated(this.properties);
        assertNotNull(this.floodingProtectionConfigurator.getFloodingProtectionNatRulesIPv6());
        assertTrue(this.floodingProtectionConfigurator.getFloodingProtectionNatRulesIPv6().isEmpty());
    }

    @Test
    public void getFloodingProtectionMangleRulesTest() {
        this.floodingProtectionConfigurator.updated(this.properties);
        assertNotNull(this.floodingProtectionConfigurator.getFloodingProtectionMangleRules());
        assertFalse(this.floodingProtectionConfigurator.getFloodingProtectionMangleRules().isEmpty());
        assertEquals(17, this.floodingProtectionConfigurator.getFloodingProtectionMangleRules().size());
        assertTrue(this.floodingProtectionConfigurator.getFloodingProtectionMangleRules()
                .containsAll(Arrays.asList(FLOODING_PROTECTION_MANGLE_RULES)));
    }

    @Test
    public void getFloodingProtectionMangleRulesIPv6Test() {
        this.floodingProtectionConfigurator.updated(this.properties);
        assertNotNull(this.floodingProtectionConfigurator.getFloodingProtectionMangleRulesIPv6());
        assertFalse(this.floodingProtectionConfigurator.getFloodingProtectionMangleRulesIPv6().isEmpty());
        assertEquals(26, this.floodingProtectionConfigurator.getFloodingProtectionMangleRulesIPv6().size());
        assertTrue(this.floodingProtectionConfigurator.getFloodingProtectionMangleRulesIPv6()
                .containsAll(Arrays.asList(FLOODING_PROTECTION_MANGLE_RULES_IPV6)));
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNPEWithoutFirewallConfigurationService() {
        this.floodingProtectionConfigurator = new FloodingProtectionConfigurator();
        this.properties.put("flooding.protection.enabled", false);
        this.floodingProtectionConfigurator.activate(this.properties);
    }

    @Test
    public void shouldDeleteAllRules() {
        this.floodingProtectionConfigurator = new FloodingProtectionConfigurator();
        this.floodingProtectionConfigurator.setFirewallConfigurationService(this.mockFirewallService);

        this.properties.put("flooding.protection.enabled", false);
        this.floodingProtectionConfigurator.activate(this.properties);

        verify(this.mockFirewallService, times(1)).addFloodingProtectionRules(new HashSet<>(), new HashSet<>(),
                new HashSet<>());
    }

    @Test
    public void shouldApplyMangleRules() {
        this.floodingProtectionConfigurator = new FloodingProtectionConfigurator();
        this.floodingProtectionConfigurator.setFirewallConfigurationService(this.mockFirewallService);

        this.properties.put("flooding.protection.enabled", true);
        this.floodingProtectionConfigurator.activate(this.properties);

        verify(this.mockFirewallService, times(1)).addFloodingProtectionRules(new HashSet<>(), new HashSet<>(),
                new HashSet<>(Arrays.asList(FLOODING_PROTECTION_MANGLE_RULES)));
    }

    @Test
    public void shouldDeleteAllIPv6Rules() {
        this.floodingProtectionConfigurator = new FloodingProtectionConfigurator();
        this.floodingProtectionConfigurator.setFirewallConfigurationService(this.mockFirewallService);
        this.floodingProtectionConfigurator.setFirewallConfigurationServiceIPv6(this.mockFirewallServiceIPv6);

        this.properties.put("flooding.protection.enabled", false);
        this.properties.put("flooding.protection.enabled.ipv6", false);
        this.floodingProtectionConfigurator.activate(this.properties);

        verify(this.mockFirewallServiceIPv6, times(1)).addFloodingProtectionRules(new HashSet<>(), new HashSet<>(),
                new HashSet<>());
    }

    @Test
    public void shouldApplyMangleIpv6Rules() {
        this.floodingProtectionConfigurator = new FloodingProtectionConfigurator();
        this.floodingProtectionConfigurator.setFirewallConfigurationService(this.mockFirewallService);
        this.floodingProtectionConfigurator.setFirewallConfigurationServiceIPv6(this.mockFirewallServiceIPv6);

        this.properties.put("flooding.protection.enabled", false);
        this.properties.put("flooding.protection.enabled.ipv6", true);
        this.floodingProtectionConfigurator.activate(this.properties);

        verify(this.mockFirewallServiceIPv6, times(1)).addFloodingProtectionRules(new HashSet<>(), new HashSet<>(),
                new HashSet<>(Arrays.asList(FLOODING_PROTECTION_MANGLE_RULES_IPV6)));
    }
}
