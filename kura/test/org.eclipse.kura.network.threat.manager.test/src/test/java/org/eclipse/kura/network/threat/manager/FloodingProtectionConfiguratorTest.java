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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.internal.floodingprotection.FloodingProtectionConfigurator;
import org.eclipse.kura.internal.floodingprotection.FloodingProtectionOptions;
import org.eclipse.kura.net.admin.FirewallConfigurationService;
import org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6;
import org.junit.After;
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

    private static final String FRAG_LOW_THR_IPV4_NAME = "/tmp/ipfrag_low_thresh";
    private static final String FRAG_HIGH_THR_IPV4_NAME = "/tmp/ipfrag_high_thresh";
    private static final String FRAG_LOW_THR_IPV6_NAME = "/tmp/nf_conntrack_frag6_low_thresh";
    private static final String FRAG_HIGH_THR_IPV6_NAME = "/tmp/nf_conntrack_frag6_high_thresh";
    private static final int FRAG_LOW_THR_DEFAULT = 3 * 1024 * 1024;
    private static final int FRAG_HIGH_THR_DEFAULT = 4 * 1024 * 1024;

    private FloodingProtectionConfigurator floodingProtectionConfigurator;
    private FirewallConfigurationService mockFirewallService;
    private FirewallConfigurationServiceIPv6 mockFirewallServiceIPv6;
    private final Map<String, Object> properties = new HashMap<>();
    private ComponentConfiguration config;
    private Set<String> rules;

    @Test
    public void shouldGetConfiguration() throws KuraException {
        givenFloodingProtectionConfigurator();

        whenFloodingProtectionConfiguratorIsActivated(true, true);
        whenFloodingProtectionConfigurationIsRetrieved();

        thenConfigurationIsCorrect(true, true);
    }

    @Test
    public void shouldGetFloodingProtectionFilterRules() {
        givenFloodingProtectionConfigurator();

        whenFloodingProtectionConfiguratorIsActivated(true, true);
        whenFirewallFilterRulesAreRetrieved();

        thenFirewallRulesAre(new HashSet<String>());
    }

    @Test
    public void shouldGetFloodingProtectionFilterRulesIPv6() {
        givenFloodingProtectionConfigurator();

        whenFloodingProtectionConfiguratorIsActivated(true, true);
        whenFirewallFilterRulesIPv6AreRetrieved();

        thenFirewallRulesAre(new HashSet<String>());
    }

    @Test
    public void shouldGetFloodingProtectionNatRules() {
        givenFloodingProtectionConfigurator();

        whenFloodingProtectionConfiguratorIsActivated(true, true);
        whenFirewallNatRulesAreRetrieved();

        thenFirewallRulesAre(new HashSet<String>());
    }

    @Test
    public void shouldGetFloodingProtectionNatRulesIPv6() {
        givenFloodingProtectionConfigurator();

        whenFloodingProtectionConfiguratorIsActivated(true, true);
        whenFirewallNatRulesIPv6AreRetrieved();

        thenFirewallRulesAre(new HashSet<String>());
    }

    @Test
    public void shouldGetFloodingProtectionMangleRules() {
        givenFloodingProtectionConfigurator();

        whenFloodingProtectionConfiguratorIsActivated(true, true);
        whenFirewallMangleRulesAreRetrieved();

        thenFirewallRulesAre(new HashSet<String>(Arrays.asList(FLOODING_PROTECTION_MANGLE_RULES)));
    }

    @Test
    public void shouldGetFloodingProtectionMangleRulesIPv6() {
        givenFloodingProtectionConfigurator();

        whenFloodingProtectionConfiguratorIsActivated(true, true);
        whenFirewallMangleRulesIPv6AreRetrieved();

        thenFirewallRulesAre(new HashSet<String>(Arrays.asList(FLOODING_PROTECTION_MANGLE_RULES_IPV6)));
    }

    @Test
    public void shouldDeleteAllRules() {
        givenFloodingProtectionConfigurator();

        whenFloodingProtectionConfiguratorIsActivated(false, true);

        thenFirewallRulesAreApplied(new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    @Test
    public void shouldApplyMangleRules() {
        givenFloodingProtectionConfigurator();

        whenFloodingProtectionConfiguratorIsActivated(true, true);

        thenFirewallRulesAreApplied(new HashSet<>(), new HashSet<>(),
                new HashSet<>(Arrays.asList(FLOODING_PROTECTION_MANGLE_RULES)));
    }

    @Test
    public void shouldDeleteAllIPv6Rules() {
        givenFloodingProtectionConfigurator();

        whenFloodingProtectionConfiguratorIsActivated(true, false);

        thenFirewallRulesIPv6AreApplied(new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    @Test
    public void shouldApplyMangleIpv6Rules() {
        givenFloodingProtectionConfigurator();

        whenFloodingProtectionConfiguratorIsActivated(true, true);

        thenFirewallRulesIPv6AreApplied(new HashSet<>(), new HashSet<>(),
                new HashSet<>(Arrays.asList(FLOODING_PROTECTION_MANGLE_RULES_IPV6)));
    }

    @Test
    public void shouldEnableFragmentFiltering() throws IOException {
        givenFragmentFilteringFiles();
        givenFloodingProtectionConfiguratorWithFragmentFiltering();

        whenFloodingProtectionConfiguratorIsActivated(true, false);

        thenFragmentFilteringFilesContain(FRAG_LOW_THR_IPV4_NAME, "0", FRAG_HIGH_THR_IPV4_NAME, "0");
    }

    @Test
    public void shouldDisableFragmentFiltering() throws IOException {
        givenFragmentFilteringFiles();
        givenFloodingProtectionConfiguratorWithFragmentFiltering();

        whenFloodingProtectionConfiguratorIsActivated(false, false);

        thenFragmentFilteringFilesContain(FRAG_LOW_THR_IPV4_NAME, Integer.toString(FRAG_LOW_THR_DEFAULT),
                FRAG_HIGH_THR_IPV4_NAME, Integer.toString(FRAG_HIGH_THR_DEFAULT));
    }

    @Test
    public void shouldEnableFragmentFilteringIPv6() throws IOException {
        givenFragmentFilteringFiles();
        givenFloodingProtectionConfiguratorWithFragmentFiltering();

        whenFloodingProtectionConfiguratorIsActivated(false, true);

        thenFragmentFilteringFilesContain(FRAG_LOW_THR_IPV6_NAME, "0", FRAG_HIGH_THR_IPV6_NAME, "0");
    }

    @Test
    public void shouldDisableFragmentFilteringIPv6() throws IOException {
        givenFragmentFilteringFiles();
        givenFloodingProtectionConfiguratorWithFragmentFiltering();

        whenFloodingProtectionConfiguratorIsActivated(false, false);

        thenFragmentFilteringFilesContain(FRAG_LOW_THR_IPV6_NAME, Integer.toString(FRAG_LOW_THR_DEFAULT),
                FRAG_HIGH_THR_IPV6_NAME, Integer.toString(FRAG_HIGH_THR_DEFAULT));
    }

    private void givenFloodingProtectionConfigurator() {
        this.floodingProtectionConfigurator = new FloodingProtectionConfigurator();
        this.mockFirewallService = mock(FirewallConfigurationService.class);
        this.mockFirewallServiceIPv6 = mock(FirewallConfigurationServiceIPv6.class);
        this.floodingProtectionConfigurator.setFirewallConfigurationService(this.mockFirewallService);
        this.floodingProtectionConfigurator.setFirewallConfigurationServiceIPv6(this.mockFirewallServiceIPv6);
    }

    private void givenFloodingProtectionConfiguratorWithFragmentFiltering() {
        this.floodingProtectionConfigurator = new FloodingProtectionConfigurator() {

            @Override
            public FloodingProtectionOptions buildFloodingProtectionOptions(Map<String, Object> properties) {
                return new FloodingProtectionOptions(properties) {

                    @Override
                    public String getFragmentLowThresholdIPv4FileName() {
                        return FRAG_LOW_THR_IPV4_NAME;
                    }

                    @Override
                    public String getFragmentHighThresholdIPv4FileName() {
                        return FRAG_HIGH_THR_IPV4_NAME;
                    }

                    @Override
                    public String getFragmentLowThresholdIPv6FileName() {
                        return FRAG_LOW_THR_IPV6_NAME;
                    }

                    @Override
                    public String getFragmentHighThresholdIPv6FileName() {
                        return FRAG_HIGH_THR_IPV6_NAME;
                    }
                };
            }
        };
        this.mockFirewallService = mock(FirewallConfigurationService.class);
        this.mockFirewallServiceIPv6 = mock(FirewallConfigurationServiceIPv6.class);
        this.floodingProtectionConfigurator.setFirewallConfigurationService(this.mockFirewallService);
        this.floodingProtectionConfigurator.setFirewallConfigurationServiceIPv6(this.mockFirewallServiceIPv6);
    }

    private void givenFragmentFilteringFiles() throws IOException {
        createEmptyFilesIfNeeded(FRAG_LOW_THR_IPV4_NAME);
        createEmptyFilesIfNeeded(FRAG_HIGH_THR_IPV4_NAME);
        createEmptyFilesIfNeeded(FRAG_LOW_THR_IPV6_NAME);
        createEmptyFilesIfNeeded(FRAG_HIGH_THR_IPV6_NAME);
    }

    private void whenFloodingProtectionConfiguratorIsActivated(boolean enableIPv4, boolean enableIPv6) {
        this.properties.put("flooding.protection.enabled", enableIPv4);
        this.properties.put("flooding.protection.enabled.ipv6", enableIPv6);
        this.floodingProtectionConfigurator.activate(this.properties);
    }

    private void whenFloodingProtectionConfigurationIsRetrieved() throws KuraException {
        this.config = this.floodingProtectionConfigurator.getConfiguration();
    }

    private void whenFirewallFilterRulesAreRetrieved() {
        this.rules = this.floodingProtectionConfigurator.getFloodingProtectionFilterRules();
    }

    private void whenFirewallFilterRulesIPv6AreRetrieved() {
        this.rules = this.floodingProtectionConfigurator.getFloodingProtectionFilterRulesIPv6();
    }

    private void whenFirewallNatRulesAreRetrieved() {
        this.rules = this.floodingProtectionConfigurator.getFloodingProtectionNatRules();
    }

    private void whenFirewallNatRulesIPv6AreRetrieved() {
        this.rules = this.floodingProtectionConfigurator.getFloodingProtectionNatRulesIPv6();
    }

    private void whenFirewallMangleRulesAreRetrieved() {
        this.rules = this.floodingProtectionConfigurator.getFloodingProtectionMangleRules();
    }

    private void whenFirewallMangleRulesIPv6AreRetrieved() {
        this.rules = this.floodingProtectionConfigurator.getFloodingProtectionMangleRulesIPv6();
    }

    private void thenConfigurationIsCorrect(boolean enableIPv4, boolean enableIPv6) {
        assertNotNull(config);
        assertEquals("org.eclipse.kura.internal.floodingprotection.FloodingProtectionConfigurator", config.getPid());
        if (enableIPv4) {
            assertTrue((boolean) config.getConfigurationProperties().get("flooding.protection.enabled"));
        } else {
            assertFalse((boolean) config.getConfigurationProperties().get("flooding.protection.enabled"));
        }
        if (enableIPv6) {
            assertTrue((boolean) config.getConfigurationProperties().get("flooding.protection.enabled.ipv6"));
        } else {
            assertFalse((boolean) config.getConfigurationProperties().get("flooding.protection.enabled.ipv6"));
        }
    }

    private void thenFirewallRulesAre(Set<String> expectedRules) {
        assertNotNull(this.rules);
        assertEquals(expectedRules, this.rules);
    }

    private void thenFirewallRulesAreApplied(Set<String> filterRules, Set<String> natRules, Set<String> mangleRules) {
        verify(this.mockFirewallService, times(1)).addFloodingProtectionRules(filterRules, natRules, mangleRules);
    }

    private void thenFirewallRulesIPv6AreApplied(Set<String> filterRules, Set<String> natRules,
            Set<String> mangleRules) {
        verify(this.mockFirewallServiceIPv6, times(1)).addFloodingProtectionRules(filterRules, natRules, mangleRules);
    }

    private void thenFragmentFilteringFilesContain(String lowThresholdFilenane, String lowThreshold,
            String highThresholdFilenane, String highThreshold) throws IOException {
        assertTrue(fileContains(lowThresholdFilenane, lowThreshold));
        assertTrue(fileContains(highThresholdFilenane, highThreshold));
    }

    private void createEmptyFilesIfNeeded(String path) throws IOException {
        if (!Files.exists(Paths.get(path))) {
            Files.createFile(Paths.get(path));
        }
    }

    private boolean fileContains(String path, String value) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path));
        return lines.size() == 1 && lines.get(0).equals(value);
    }

    @After
    public void deleteFiles() throws IOException {
        deleteFile(FRAG_LOW_THR_IPV4_NAME);
        deleteFile(FRAG_HIGH_THR_IPV4_NAME);
        deleteFile(FRAG_LOW_THR_IPV6_NAME);
        deleteFile(FRAG_HIGH_THR_IPV6_NAME);
    }

    private void deleteFile(String path) throws IOException {
        if (Files.exists(Paths.get(path))) {
            Files.delete(Paths.get(path));
        }
    }
}
