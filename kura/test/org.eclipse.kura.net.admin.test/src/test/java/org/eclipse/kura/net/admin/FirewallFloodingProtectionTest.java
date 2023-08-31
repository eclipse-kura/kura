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
package org.eclipse.kura.net.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.iptables.LinuxFirewall;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

public class FirewallFloodingProtectionTest {

    private final String[] floodingFilterRules = { "first filter rule", "second filter rule" };
    private final String[] floodingNatRules = { "first nat rule", "second nat rule" };
    private final String[] floodingMangleRules = { "first mangle rule", "second mangle rule" };
    private final Set<String> filterRules = new HashSet<>(Arrays.asList(floodingFilterRules));
    private final Set<String> natRules = new HashSet<>(Arrays.asList(floodingNatRules));
    private final Set<String> mangleRules = new HashSet<>(Arrays.asList(floodingMangleRules));

    private LinuxFirewall mockFirewall;
    private FirewallConfigurationServiceImpl firewallService;

    @Test
    public void shouldAddFloodingProtectionRulesOnlyToMangleTableTest() {
        givenFirewallConfigurationServiceImpl();

        whenFirewallConfigurationServiceImplIsActivated();
        whenFloodingProtectionRulesAreAddedToMangleTable();

        thenSetAdditionalRulesToMangleOnlyIsCalled();
    }

    @Test
    public void shouldAddFloodingProtectionRulesToAllTablesTest() {
        givenFirewallConfigurationServiceImpl();

        whenFirewallConfigurationServiceImplIsActivated();
        whenFloodingProtectionRulesAreAddedTable();

        thenSetAdditionalRulesIsCalled();
    }

    private void givenFirewallConfigurationServiceImpl() {
        this.mockFirewall = mock(LinuxFirewall.class);
        this.firewallService = new FirewallConfigurationServiceImpl() {

            @Override
            protected LinuxFirewall getLinuxFirewall() {
                return mockFirewall;
            }

            @Override
            public synchronized void updated(Map<String, Object> properties) {
            }
        };
    }

    private void whenFirewallConfigurationServiceImplIsActivated() {
        ComponentContext mockContext = mock(ComponentContext.class);
        firewallService.activate(mockContext, new HashMap<String, Object>());
    }

    private void whenFloodingProtectionRulesAreAddedToMangleTable() {
        firewallService.addFloodingProtectionRules(this.mangleRules);
    }

    private void whenFloodingProtectionRulesAreAddedTable() {
        firewallService.addFloodingProtectionRules(this.filterRules, this.natRules, this.mangleRules);
    }

    private void thenSetAdditionalRulesToMangleOnlyIsCalled() {
        try {
            verify(mockFirewall, times(1)).setAdditionalRules(new HashSet<String>(), new HashSet<String>(),
                    this.mangleRules);
        } catch (KuraException e) {
            assert (false);
        }
    }

    private void thenSetAdditionalRulesIsCalled() {
        try {
            verify(mockFirewall, times(1)).setAdditionalRules(this.filterRules, this.natRules, this.mangleRules);
        } catch (KuraException e) {
            assert (false);
        }
    }
}
