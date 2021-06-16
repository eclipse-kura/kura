/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
import static org.mockito.Matchers.anyObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.internal.floodingprotection.FloodingProtectionConfigurator;
import org.eclipse.kura.net.admin.FirewallConfigurationService;
import org.eclipse.kura.security.FloodingProtectionConfigurationChangeEvent;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

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

    private FloodingProtectionConfigurator floodingProtectionConfigurator;
    private FirewallConfigurationService mockFirewallService;
    private final Map<String, Object> properties = new HashMap<>();

    @Before
    public void setupTests() {
        this.floodingProtectionConfigurator = new FloodingProtectionConfigurator();
        this.mockFirewallService = mock(FirewallConfigurationService.class);
        this.floodingProtectionConfigurator.setFirewallConfigurationService(this.mockFirewallService);
        this.properties.put("flooding.protection.enabled", true);
    }

    @Test
    public void getConfigurationTest() throws KuraException, NoSuchFieldException {
        this.floodingProtectionConfigurator.updated(this.properties);
        ComponentConfiguration config = this.floodingProtectionConfigurator.getConfiguration();

        assertNotNull(config);
        assertEquals("org.eclipse.kura.internal.floodingprotection.FloodingProtectionConfigurator", config.getPid());
        assertTrue((boolean) config.getConfigurationProperties().get("flooding.protection.enabled"));
    }

    @Test
    public void getFloodingProtectionFilterRulesTest() {
        this.floodingProtectionConfigurator.updated(this.properties);
        assertNotNull(this.floodingProtectionConfigurator.getFloodingProtectionFilterRules());
        assertTrue(this.floodingProtectionConfigurator.getFloodingProtectionFilterRules().isEmpty());
    }

    @Test
    public void getFloodingProtectionNatRulesTest() {
        this.floodingProtectionConfigurator.updated(this.properties);
        assertNotNull(this.floodingProtectionConfigurator.getFloodingProtectionNatRules());
        assertTrue(this.floodingProtectionConfigurator.getFloodingProtectionNatRules().isEmpty());
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
    
    @Test(expected = NullPointerException.class)
    public void addFloodingRulesTest() {
        this.floodingProtectionConfigurator = new FloodingProtectionConfigurator();
        this.floodingProtectionConfigurator.setFirewallConfigurationService(mockFirewallService);
        
        ComponentContext mockContext = mock(ComponentContext.class);
        this.properties.put("flooding.protection.enabled", false);
        this.floodingProtectionConfigurator.activate(mockContext, this.properties);
        
        this.properties.put("flooding.protection.enabled", true);
        this.floodingProtectionConfigurator.updated(this.properties);
        
        verify(this.mockFirewallService, times(2)).addFloodingProtectionRules(anyObject());
        
        this.floodingProtectionConfigurator.deactivate(mockContext);
        
        this.floodingProtectionConfigurator.unsetFirewallConfigurationService(mockFirewallService);
        
        this.floodingProtectionConfigurator.updated(this.properties);
    }
}
