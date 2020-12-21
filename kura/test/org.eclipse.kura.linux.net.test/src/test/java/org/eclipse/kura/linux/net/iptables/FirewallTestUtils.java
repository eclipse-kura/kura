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
    protected static List<Command> commandApplyList;

    protected static void setUpMock() {
        executorServiceMock = mock(CommandExecutorService.class);
        commandRestore = new Command(new String[] { "iptables-restore", IptablesConfig.FIREWALL_TMP_CONFIG_FILE_NAME });
        commandRestore.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandRestore)).thenReturn(successStatus);
        commandSave = new Command(new String[] { "iptables-save", ">", IptablesConfig.FIREWALL_CONFIG_FILE_NAME });
        commandSave.setExecuteInAShell(true);
        when(executorServiceMock.execute(commandSave)).thenReturn(successStatus);
        commandSaveTmp = new Command(
                new String[] { "iptables-save", ">", IptablesConfig.FIREWALL_TMP_CONFIG_FILE_NAME });
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
        commandApplyList.stream().forEach(c -> c.setExecuteInAShell(true));
        commandApplyList.stream().forEach(c -> when(executorServiceMock.execute(c)).thenReturn(successStatus));
    }

}
