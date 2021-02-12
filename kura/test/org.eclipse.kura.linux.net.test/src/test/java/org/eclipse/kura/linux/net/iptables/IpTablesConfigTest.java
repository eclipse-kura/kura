/*******************************************************************************
 * Copyright (c) 2020, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.iptables;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkPair;
import org.junit.Test;

public class IpTablesConfigTest extends FirewallTestUtils {

    @Test
    public void clearAllChainsTest() throws KuraException {
        setUpMock();
        IptablesConfig iptablesConfig = new IptablesConfig(executorServiceMock);
        iptablesConfig.clearAllChains();

        verify(executorServiceMock, times(1)).execute(commandRestore);
    }

    @Test
    public void applyBlockPolicyTest() throws KuraException {
        setUpMock();
        IptablesConfig iptablesConfig = new IptablesConfig(executorServiceMock);
        iptablesConfig.applyBlockPolicy();

        verify(executorServiceMock, times(1)).execute(commandRestore);
    }

    @Test
    public void clearAllKuraChainsTest() throws KuraException {
        setUpMock();
        IptablesConfig iptablesConfig = new IptablesConfig(executorServiceMock);
        iptablesConfig.clearAllKuraChains();

        verify(executorServiceMock, times(1)).execute(commandFlushInputFilter);
        verify(executorServiceMock, times(1)).execute(commandFlushOutputFilter);
        verify(executorServiceMock, times(1)).execute(commandFlushForwardFilter);
        verify(executorServiceMock, times(1)).execute(commandFlushInputNat);
        verify(executorServiceMock, times(1)).execute(commandFlushOutputNat);
        verify(executorServiceMock, times(1)).execute(commandFlushPreroutingNat);
        verify(executorServiceMock, times(1)).execute(commandFlushPostroutingNat);
    }

    @Test
    public void saveTest() throws KuraException {
        setUpMock();
        IptablesConfig iptablesConfig = new IptablesConfig(executorServiceMock);
        iptablesConfig.save();

        verify(executorServiceMock, times(1)).execute(commandSave);
    }

    @Test
    public void saveWithFilenameTest() throws KuraException {
        setUpMock();
        IptablesConfig iptablesConfig = new IptablesConfig(executorServiceMock);
        iptablesConfig.save(IptablesConfig.FIREWALL_TMP_CONFIG_FILE_NAME);

        verify(executorServiceMock, times(1)).execute(commandSaveTmp);
    }

    @Test
    public void restoreWithFilenameTest() throws KuraException {
        setUpMock();
        IptablesConfig iptablesConfig = new IptablesConfig(executorServiceMock);
        iptablesConfig.restore(IptablesConfig.FIREWALL_TMP_CONFIG_FILE_NAME);

        verify(executorServiceMock, times(1)).execute(commandRestore);
    }

    @Test
    public void saveKuraChainsTest() throws KuraException, IOException {
        setUpMock();
        Set<LocalRule> localRules = new LinkedHashSet<>();
        localRules.add(new LocalRule(5400, "tcp",
                new NetworkPair<>((IP4Address) IPAddress.parseHostAddress("0.0.0.0"), (short) 0), "eth0", null,
                "00:11:22:33:44:55:66", "10100:10200"));

        Set<PortForwardRule> portForwardRules = new LinkedHashSet<>();
        portForwardRules.add(new PortForwardRule("eth0", "eth1", "172.16.0.1", "tcp", 3040, 4050, true, "172.16.0.100",
                32, "00:11:22:33:44:55:66", "10100:10200"));

        Set<NATRule> autoNatRules = new LinkedHashSet<>();
        autoNatRules.add(new NATRule("eth2", "eth3", true));

        Set<NATRule> natRules = new LinkedHashSet<>();
        natRules.add(new NATRule("eth4", "eth5", "tcp", "172.16.0.1/24", "172.16.0.2/24", true));

        IptablesConfig iptablesConfig = new IptablesConfig(localRules, portForwardRules, autoNatRules, natRules, true,
                executorServiceMock);

        try {
            iptablesConfig.saveKuraChains();
        } catch (KuraIOException e) {
            // do nothing...
        }

        AtomicBoolean isLocalRulePresent = new AtomicBoolean(false);
        AtomicBoolean[] isPortForwardRulePresent = { new AtomicBoolean(false), new AtomicBoolean(false),
                new AtomicBoolean(false), new AtomicBoolean(false) };
        AtomicBoolean[] isAutoNatRulePresent = { new AtomicBoolean(false), new AtomicBoolean(false),
                new AtomicBoolean(false) };
        AtomicBoolean[] isNatRulePresent = { new AtomicBoolean(false), new AtomicBoolean(false),
                new AtomicBoolean(false) };
        try (Stream<String> lines = Files.lines(Paths.get(IptablesConfig.FIREWALL_TMP_CONFIG_FILE_NAME))) {
            lines.forEach(line -> {
                line = line.trim();
                switch (line) {
                case "-A input-kura -p tcp -s 0.0.0.0/0 -i eth0 -m mac --mac-source 00:11:22:33:44:55:66 --sport 10100:10200 --dport 5400 -j ACCEPT":
                    isLocalRulePresent.set(true);
                    break;
                case "-A forward-kura -s 172.16.0.100/32 -d 172.16.0.1/32 -i eth0 -o eth1 -p tcp -m tcp -m mac --mac-source 00:11:22:33:44:55:66 --sport 10100:10200 -j ACCEPT":
                    isPortForwardRulePresent[0].set(true);
                    break;
                case "-A forward-kura -s 172.16.0.1/32 -i eth1 -o eth0 -p tcp -m state --state RELATED,ESTABLISHED -j ACCEPT":
                    isPortForwardRulePresent[1].set(true);
                    break;
                case "-A prerouting-kura -s 172.16.0.100/32 -i eth0 -p tcp -m mac --mac-source 00:11:22:33:44:55:66 -m tcp --sport 10100:10200 --dport 3040 -j DNAT --to-destination 172.16.0.1:4050":
                    isPortForwardRulePresent[2].set(true);
                    break;
                case "-A postrouting-kura -s 172.16.0.100/32 -d 172.16.0.1/32 -o eth1 -p tcp -j MASQUERADE":
                    isPortForwardRulePresent[3].set(true);
                    break;
                case "-A forward-kura -i eth2 -o eth3 -j ACCEPT":
                    isAutoNatRulePresent[0].set(true);
                    break;
                case "-A forward-kura -i eth3 -o eth2 -m state --state RELATED,ESTABLISHED -j ACCEPT":
                    isAutoNatRulePresent[1].set(true);
                    break;
                case "-A postrouting-kura -o eth3 -j MASQUERADE":
                    isAutoNatRulePresent[2].set(true);
                    break;
                case "-A postrouting-kura -s 172.16.0.1/24 -d 172.16.0.2/24 -o eth5 -p tcp -j MASQUERADE":
                    isNatRulePresent[0].set(true);
                    break;
                case "-A forward-kura -s 172.16.0.1/24 -d 172.16.0.2/24 -i eth4 -o eth5 -p tcp -m tcp -j ACCEPT":
                    isNatRulePresent[1].set(true);
                    break;
                case "-A forward-kura -s 172.16.0.2/24 -i eth5 -o eth4 -p tcp -m state --state RELATED,ESTABLISHED -j ACCEPT":
                    isNatRulePresent[2].set(true);
                    break;
                }

            });
        } catch (IOException e) {
            throw new KuraIOException(e, "save() :: failed to save rules on file");
        }
        assertTrue(isLocalRulePresent.get());
        assertTrue(isPortForwardRulePresent[0].get() && isPortForwardRulePresent[1].get()
                && isPortForwardRulePresent[2].get() && isPortForwardRulePresent[3].get());
        assertTrue(isAutoNatRulePresent[0].get() && isAutoNatRulePresent[1].get() && isAutoNatRulePresent[2].get());
        assertTrue(isNatRulePresent[0].get() && isNatRulePresent[1].get() && isNatRulePresent[2].get());

        File configFile = new File(IptablesConfig.FIREWALL_TMP_CONFIG_FILE_NAME);
        Files.deleteIfExists(configFile.toPath());

    }

    @Test
    public void applyRulesTest() {
        setUpMock();

        String[] mangleRulesArray = { "-A prerouting-kura -m conntrack --ctstate INVALID -j DROP",
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
        Set<String> mangleRules = new HashSet<String>(Arrays.asList(mangleRulesArray));

        // These rules are fake...
        String[] filterRulesArray = { "-A input-kura -p tcp -f -j DROP" };
        Set<String> filterRules = new HashSet<String>(Arrays.asList(filterRulesArray));

        // Also these ones...
        String[] natRulesArray = { "-A prerouting-kura -p tcp -f -j DROP" };
        Set<String> natRules = new HashSet<String>(Arrays.asList(natRulesArray));

        IptablesConfig iptablesConfig = new IptablesConfig(new LinkedHashSet<>(), new LinkedHashSet<>(),
                new LinkedHashSet<>(), new LinkedHashSet<>(), false, executorServiceMock);
        iptablesConfig.setAdditionalFilterRules(filterRules);
        iptablesConfig.setAdditionalNatRules(natRules);
        iptablesConfig.setAdditionalMangleRules(mangleRules);
        iptablesConfig.applyRules();

        for (Command c : commandApplyList) {
            verify(executorServiceMock, times(1)).execute(c);
        }
    }

}
