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
package org.eclipse.kura.nm.configuration.writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.iptables.LinuxFirewall;
import org.eclipse.kura.linux.net.iptables.NATRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class FirewallNatConfigWriterTest {

    private FirewallNatConfigWriter writer;
    private List<String> wanInterfaces;
    private List<String> natInterfaces;
    private Exception occurredException;
    private LinuxFirewall mockLinuxFirewall;
    private ArgumentCaptor<Object> appliedRuleCaptor = ArgumentCaptor.forClass(Object.class);

    /*
     * Scenarios
     */

    @Test
    public void shouldApplyNatRulesFromWlan0ToAllWanInterfaces() throws Exception {
        givenWanInterfaces("eth0", "eth1");
        givenNatInterfaces("wlan0");
        givenWriterInstance();

        whenWriteConfiguration();

        thenNoExceptionsOccurred();
        thenNATRuleApplied("wlan0", "eth0");
        thenNATRuleApplied("wlan0", "eth1");
    }

    @Test
    public void shouldNotApplyNatRulesBetweenNatInterfaces() throws Exception {
        givenWanInterfaces("eth0", "eth1");
        givenNatInterfaces("wlan0");
        givenWriterInstance();

        whenWriteConfiguration();

        thenNoExceptionsOccurred();
        thenNATRuleNotApplied("wlan0", "wlan0");
    }

    @Test
    public void shouldNotApplyNatRulesBetweenWanInterfaces() throws Exception {
        givenWanInterfaces("eth0", "eth1");
        givenNatInterfaces("wlan0");
        givenWriterInstance();

        whenWriteConfiguration();

        thenNoExceptionsOccurred();
        thenNATRuleNotApplied("eth0", "eth0");
        thenNATRuleNotApplied("eth0", "eth1");
        thenNATRuleNotApplied("eth1", "eth0");
    }

    @Test
    public void shouldThrowKuraExceptionWhenFirewallFails() throws Exception {
        givenWanInterfaces("eth0", "eth1");
        givenNatInterfaces("wlan0");
        givenWriterInstance();
        givenBrokenFirewall();

        whenWriteConfiguration();

        thenKuraExceptionOccurredWithCode(KuraErrorCode.CONFIGURATION_ERROR);
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenWanInterfaces(String... ifnames) {
        this.wanInterfaces = new LinkedList<>();
        for (String ifname : ifnames) {
            this.wanInterfaces.add(ifname);
        }
    }

    private void givenNatInterfaces(String... ifnames) {
        this.natInterfaces = new LinkedList<>();
        for (String ifname : ifnames) {
            this.natInterfaces.add(ifname);
        }
    }

    private void givenWriterInstance() throws Exception {
        CommandExecutorService mockExecutor = Mockito.mock(CommandExecutorService.class);

        this.mockLinuxFirewall = Mockito.mock(LinuxFirewall.class);

        setPrivateStaticField(LinuxFirewall.class.getDeclaredField("linuxFirewall"), this.mockLinuxFirewall);

        this.writer = new FirewallNatConfigWriter(mockExecutor, this.wanInterfaces, this.natInterfaces);
    }

    private void givenBrokenFirewall() throws Exception {
        Mockito.doThrow(KuraException.class).when(this.mockLinuxFirewall).replaceAllNatRules(Mockito.any());
    }

    /*
     * When
     */

    private void whenWriteConfiguration() {
        try {
            this.writer.writeConfiguration();
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenNoExceptionsOccurred() {
        assertNull(this.occurredException);
    }

    private void thenKuraExceptionOccurredWithCode(KuraErrorCode expectedErrorCode) {
        assertTrue(this.occurredException instanceof KuraException);

        KuraException returnedException = (KuraException) this.occurredException;
        assertEquals(expectedErrorCode, returnedException.getCode());
    }

    @SuppressWarnings("unchecked")
    private void thenNATRuleApplied(String expectedSource, String expectedDestination) throws KuraException {
        Mockito.verify(this.mockLinuxFirewall).replaceAllNatRules((Set<NATRule>) this.appliedRuleCaptor.capture());
        
        assertTrue(
                natRuleExists((Set<NATRule>) this.appliedRuleCaptor.getValue(), expectedSource, expectedDestination));
    }

    @SuppressWarnings("unchecked")
    private void thenNATRuleNotApplied(String expectedSource, String expectedDestination) throws KuraException {
        Mockito.verify(this.mockLinuxFirewall).replaceAllNatRules((Set<NATRule>) this.appliedRuleCaptor.capture());

        assertFalse(
                natRuleExists((Set<NATRule>) this.appliedRuleCaptor.getValue(), expectedSource, expectedDestination));
    }

    /*
     * Utilities
     */

    private void setPrivateStaticField(Field staticField, Object value) throws Exception {
        staticField.setAccessible(true);
        staticField.set(null, value);
    }

    private boolean natRuleExists(Set<NATRule> rules, String source, String destination) {
        for (NATRule rule : rules) {
            if (sourceAndDestinationMatch(rule, source, destination)) {
                assertTrue(rule.isMasquerade());
                return true;
            }
        }

        return false;
    }

    private boolean sourceAndDestinationMatch(NATRule rule, String source, String destination) {
        if (rule.getSourceInterface() != null && rule.getDestinationInterface() != null) {
            if (rule.getSourceInterface().equals(source) && rule.getDestinationInterface().equals(destination)) {
                return true;
            }
        }

        return false;
    }

}
