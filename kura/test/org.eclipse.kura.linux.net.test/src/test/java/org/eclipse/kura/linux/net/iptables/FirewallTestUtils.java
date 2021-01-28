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
 ******************************************************************************/
package org.eclipse.kura.linux.net.iptables;

import static org.mockito.Mockito.mock;
/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
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
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;

public class FirewallTestUtils {

    protected static CommandExecutorService executorServiceMock;
    protected static final CommandStatus successStatus = new CommandStatus(new Command(new String[] {}),
            new LinuxExitStatus(0));
    protected static Command commandRestore;
    protected static Command commandSave;
    protected static Command commandSaveTmp;
    protected static Command commandFlushInputFilter;
    protected static Command commandFlushOutputFilter;
    protected static Command commandFlushForwardFilter;
    protected static Command commandFlushInputNat;
    protected static Command commandFlushOutputNat;
    protected static Command commandFlushPreroutingNat;
    protected static Command commandFlushPostroutingNat;
    protected static Command commandFlushPreroutingMangle;
    protected static Command commandFlushPostroutingMangle;
    protected static Command commandFlushInputMangle;
    protected static Command commandFlushOutputMangle;
    protected static Command commandFlushForwardMangle;
    protected static Command commandIcmpAccept1;
    protected static Command commandIcmpAccept2;
    protected static List<Command> commandApplyList;
    protected static List<Command> testCommandList;

    protected static void setUpMock() {
        executorServiceMock = mock(CommandExecutorService.class);
        commandRestore = new Command(
                new String[] { "iptables-restore", IptablesConfigConstants.FIREWALL_TMP_CONFIG_FILE_NAME });
        commandRestore.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandRestore)).thenReturn(successStatus);
        commandSave = new Command(
                new String[] { "iptables-save", ">", IptablesConfigConstants.FIREWALL_CONFIG_FILE_NAME });
        commandSave.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandSave)).thenReturn(successStatus);
        commandSaveTmp = new Command(
                new String[] { "iptables-save", ">", IptablesConfigConstants.FIREWALL_TMP_CONFIG_FILE_NAME });
        commandSaveTmp.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandSaveTmp)).thenReturn(successStatus);
        commandFlushInputFilter = new Command(new String[] { "iptables", "-F", "input-kura", "-t", "filter" });
        commandFlushInputFilter.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandFlushInputFilter)).thenReturn(successStatus);
        commandFlushOutputFilter = new Command(new String[] { "iptables", "-F", "output-kura", "-t", "filter" });
        commandFlushOutputFilter.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandFlushOutputFilter)).thenReturn(successStatus);
        commandFlushForwardFilter = new Command(new String[] { "iptables", "-F", "forward-kura", "-t", "filter" });
        commandFlushForwardFilter.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandFlushForwardFilter)).thenReturn(successStatus);
        commandFlushInputNat = new Command(new String[] { "iptables", "-F", "input-kura", "-t", "nat" });
        commandFlushInputNat.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandFlushInputNat)).thenReturn(successStatus);
        commandFlushOutputNat = new Command(new String[] { "iptables", "-F", "output-kura", "-t", "nat" });
        commandFlushOutputNat.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandFlushOutputNat)).thenReturn(successStatus);
        commandFlushPreroutingNat = new Command(new String[] { "iptables", "-F", "prerouting-kura", "-t", "nat" });
        commandFlushPreroutingNat.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandFlushPreroutingNat)).thenReturn(successStatus);
        commandFlushPostroutingNat = new Command(new String[] { "iptables", "-F", "postrouting-kura", "-t", "nat" });
        commandFlushPostroutingNat.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandFlushPostroutingNat)).thenReturn(successStatus);
        commandFlushPreroutingMangle = new Command(
                new String[] { "iptables", "-F", "prerouting-kura", "-t", "mangle" });
        commandFlushPreroutingMangle.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandFlushPreroutingMangle)).thenReturn(successStatus);
        commandFlushPostroutingMangle = new Command(
                new String[] { "iptables", "-F", "postrouting-kura", "-t", "mangle" });
        commandFlushPostroutingMangle.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandFlushPostroutingMangle)).thenReturn(successStatus);
        commandFlushInputMangle = new Command(new String[] { "iptables", "-F", "input-kura", "-t", "mangle" });
        commandFlushInputMangle.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandFlushInputMangle)).thenReturn(successStatus);
        commandFlushOutputMangle = new Command(new String[] { "iptables", "-F", "output-kura", "-t", "mangle" });
        commandFlushOutputMangle.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandFlushOutputMangle)).thenReturn(successStatus);
        commandFlushForwardMangle = new Command(new String[] { "iptables", "-F", "forward-kura", "-t", "mangle" });
        commandFlushForwardMangle.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandFlushForwardMangle)).thenReturn(successStatus);
        commandIcmpAccept1 = new Command(
                "iptables -A input-kura -p icmp -m icmp --icmp-type 8 -m state --state NEW,RELATED,ESTABLISHED -j ACCEPT -t filter"
                        .split(" "));
        commandIcmpAccept1.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandIcmpAccept1)).thenReturn(successStatus);
        commandIcmpAccept2 = new Command(
                "iptables -A output-kura -p icmp -m icmp --icmp-type 0 -m state --state RELATED,ESTABLISHED -j ACCEPT -t filter"
                        .split(" "));
        commandIcmpAccept2.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandIcmpAccept2)).thenReturn(successStatus);
        commandApplyList = new ArrayList<>();
        commandApplyList.add(new Command("iptables -P INPUT DROP".split(" ")));
        commandApplyList.add(new Command("iptables -P FORWARD DROP".split(" ")));
        commandApplyList.add(new Command("iptables -N input-kura -t filter".split(" ")));
        commandApplyList.add(new Command("iptables -N output-kura -t filter".split(" ")));
        commandApplyList.add(new Command("iptables -N forward-kura -t filter".split(" ")));
        commandApplyList.add(new Command("iptables -N input-kura -t nat".split(" ")));
        commandApplyList.add(new Command("iptables -N output-kura -t nat".split(" ")));
        commandApplyList.add(new Command("iptables -N prerouting-kura -t nat".split(" ")));
        commandApplyList.add(new Command("iptables -N postrouting-kura -t nat".split(" ")));
        commandApplyList.add(new Command("iptables -C INPUT -j input-kura -t filter".split(" ")));
        commandApplyList.add(new Command("iptables -C OUTPUT -j output-kura -t filter".split(" ")));
        commandApplyList.add(new Command("iptables -C FORWARD -j forward-kura -t filter".split(" ")));
        commandApplyList.add(new Command("iptables -C INPUT -j input-kura -t nat".split(" ")));
        commandApplyList.add(new Command("iptables -C OUTPUT -j output-kura -t nat".split(" ")));
        commandApplyList.add(new Command("iptables -C PREROUTING -j prerouting-kura -t nat".split(" ")));
        commandApplyList.add(new Command("iptables -C POSTROUTING -j postrouting-kura -t nat".split(" ")));
        commandApplyList.add(new Command("iptables -A input-kura -j RETURN".split(" ")));
        commandApplyList.add(new Command("iptables -A output-kura -j RETURN".split(" ")));
        commandApplyList.add(new Command("iptables -A forward-kura -j RETURN".split(" ")));
        commandApplyList.add(new Command("iptables -A input-kura -j RETURN -t nat".split(" ")));
        commandApplyList.add(new Command("iptables -A output-kura -j RETURN -t nat".split(" ")));
        commandApplyList.add(new Command("iptables -A prerouting-kura -j RETURN -t nat".split(" ")));
        commandApplyList.add(new Command("iptables -A postrouting-kura -j RETURN -t nat".split(" ")));
        commandApplyList.add(new Command("iptables -A input-kura -j RETURN -t mangle".split(" ")));
        commandApplyList.add(new Command("iptables -A output-kura -j RETURN -t mangle".split(" ")));
        commandApplyList.add(new Command("iptables -A forward-kura -j RETURN -t mangle".split(" ")));
        commandApplyList.add(new Command("iptables -A prerouting-kura -j RETURN -t mangle".split(" ")));
        commandApplyList.add(new Command("iptables -A postrouting-kura -j RETURN -t mangle".split(" ")));
        commandApplyList.add(new Command("iptables -A input-kura -i lo -j ACCEPT -t filter".split(" ")));
        commandApplyList.add(new Command(
                "iptables -A input-kura -m state --state RELATED,ESTABLISHED -j ACCEPT -t filter".split(" ")));
        commandApplyList.add(new Command(
                "iptables -A input-kura -p icmp -m icmp --icmp-type 8 -m state --state NEW,RELATED,ESTABLISHED -j DROP -t filter"
                        .split(" ")));
        commandApplyList.add(new Command(
                "iptables -A output-kura -p icmp -m icmp --icmp-type 0 -m state --state RELATED,ESTABLISHED -j DROP -t filter"
                        .split(" ")));
        commandApplyList.add(
                new Command("iptables -t mangle -A prerouting-kura -m conntrack --ctstate INVALID -j DROP".split(" ")));
        commandApplyList.add(new Command(
                "iptables -t mangle -A prerouting-kura -p tcp ! --syn -m conntrack --ctstate NEW -j DROP".split(" ")));
        commandApplyList.add(new Command(
                "iptables -t mangle -A prerouting-kura -p tcp -m conntrack --ctstate NEW -m tcpmss ! --mss 536:65535 -j DROP"
                        .split(" ")));
        commandApplyList.add(new Command(
                "iptables -t mangle -A prerouting-kura -p tcp --tcp-flags FIN,SYN FIN,SYN -j DROP".split(" ")));
        commandApplyList.add(new Command(
                "iptables -t mangle -A prerouting-kura -p tcp --tcp-flags SYN,RST SYN,RST -j DROP".split(" ")));
        commandApplyList.add(new Command(
                "iptables -t mangle -A prerouting-kura -p tcp --tcp-flags FIN,RST FIN,RST -j DROP".split(" ")));
        commandApplyList.add(
                new Command("iptables -t mangle -A prerouting-kura -p tcp --tcp-flags FIN,ACK FIN -j DROP".split(" ")));
        commandApplyList.add(
                new Command("iptables -t mangle -A prerouting-kura -p tcp --tcp-flags ACK,URG URG -j DROP".split(" ")));
        commandApplyList.add(
                new Command("iptables -t mangle -A prerouting-kura -p tcp --tcp-flags ACK,FIN FIN -j DROP".split(" ")));
        commandApplyList.add(
                new Command("iptables -t mangle -A prerouting-kura -p tcp --tcp-flags ACK,PSH PSH -j DROP".split(" ")));
        commandApplyList.add(
                new Command("iptables -t mangle -A prerouting-kura -p tcp --tcp-flags ALL ALL -j DROP".split(" ")));
        commandApplyList.add(
                new Command("iptables -t mangle -A prerouting-kura -p tcp --tcp-flags ALL NONE -j DROP".split(" ")));
        commandApplyList.add(new Command(
                "iptables -t mangle -A prerouting-kura -p tcp --tcp-flags ALL FIN,PSH,URG -j DROP".split(" ")));
        commandApplyList.add(new Command(
                "iptables -t mangle -A prerouting-kura -p tcp --tcp-flags ALL SYN,FIN,PSH,URG -j DROP".split(" ")));
        commandApplyList.add(new Command(
                "iptables -t mangle -A prerouting-kura -p tcp --tcp-flags ALL SYN,RST,ACK,FIN,URG -j DROP".split(" ")));
        commandApplyList.add(new Command("iptables -t mangle -A prerouting-kura -p icmp -j DROP".split(" ")));
        commandApplyList.add(new Command("iptables -t mangle -A prerouting-kura -f -j DROP".split(" ")));

        commandApplyList.add(new Command(
                "iptables -t filter -A input-kura -p tcp -m connlimit --connlimit-above 111 -j REJECT --reject-with tcp-reset"
                        .split(" ")));
        commandApplyList.add(new Command(
                "iptables -t filter -A input-kura -p tcp --tcp-flags RST RST -m limit --limit 2/s --limit-burst 2 -j ACCEPT"
                        .split(" ")));
        commandApplyList
                .add(new Command("iptables -t filter -A input-kura -p tcp --tcp-flags RST RST -j DROP".split(" ")));
        commandApplyList.add(new Command(
                "iptables -t filter -A input-kura -p tcp -m conntrack --ctstate NEW -m limit --limit 60/s --limit-burst 20 -j ACCEPT"
                        .split(" ")));
        commandApplyList.add(
                new Command("iptables -t filter -A input-kura -p tcp -m conntrack --ctstate NEW -j DROP".split(" ")));
        commandApplyList.stream().forEach(c -> c.setExecuteInAShell(true));
        commandApplyList.stream().forEach(c -> when(executorServiceMock.execute(c)).thenReturn(successStatus));

        testCommandList = new ArrayList<>();
        testCommandList.add(new Command("iptables -t nat -A postrouting-kura -o eth1 -j MASQUERADE".split(" ")));
        testCommandList.add(new Command(
                "iptables -A input-kura -p tcp -s 0.0.0.0/0 -i eth0 -m mac --mac-source 00:11:22:33:44:55:66 --sport 10100:10200 --dport 5400 -j ACCEPT"
                        .split(" ")));
        testCommandList.add(new Command(
                "iptables -A forward-kura -i eth1 -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT".split(" ")));
        testCommandList.add(new Command("iptables -A forward-kura -i eth0 -o eth1 -j ACCEPT".split(" ")));
        testCommandList.add(new Command(
                "iptables -A forward-kura -s 172.16.0.100/32 -d 172.16.0.1/32 -i eth0 -o eth1 -p tcp -m tcp -m mac --mac-source 00:11:22:33:44:55:66 --sport 10100:10200 -j ACCEPT"
                        .split(" ")));
        testCommandList.add(new Command(
                "iptables -A forward-kura -s 172.16.0.1/32 -i eth1 -o eth0 -p tcp -m state --state RELATED,ESTABLISHED -j ACCEPT"
                        .split(" ")));
        testCommandList.add(new Command(
                "iptables -t nat -A prerouting-kura -s 172.16.0.100/32 -i eth0 -p tcp -m mac --mac-source 00:11:22:33:44:55:66 -m tcp --sport 10100:10200 --dport 3040 -j DNAT --to-destination 172.16.0.1:4050"
                        .split(" ")));
        testCommandList.add(new Command(
                "iptables -t nat -A postrouting-kura -s 172.16.0.100/32 -d 172.16.0.1/32 -o eth1 -p tcp -j MASQUERADE"
                        .split(" ")));
        testCommandList.add(new Command(
                "iptables -A forward-kura -s 172.16.0.1/32 -d 172.16.0.2/32 -i eth0 -o eth1 -p tcp -m tcp -j ACCEPT"
                        .split(" ")));
        testCommandList.add(new Command(
                "iptables -A forward-kura -s 172.16.0.2/32 -i eth1 -o eth0 -p tcp -m state --state RELATED,ESTABLISHED -j ACCEPT"
                        .split(" ")));
        testCommandList.add(new Command(
                "iptables -t nat -A postrouting-kura -s 172.16.0.1/32 -d 172.16.0.2/32 -o eth1 -p tcp -j MASQUERADE"
                        .split(" ")));
        testCommandList.add(new Command(
                "iptables -A input-kura -p tcp -s 0.0.0.0/0 -i eth0 -m mac --mac-source 00:11:22:33:44:55:66 --sport 10100:10200 --dport 5400 -j ACCEPT"
                        .split(" ")));
        testCommandList.stream().forEach(c -> c.setExecuteInAShell(true));
        testCommandList.stream().forEach(c -> when(executorServiceMock.execute(c)).thenReturn(successStatus));
    }

}
