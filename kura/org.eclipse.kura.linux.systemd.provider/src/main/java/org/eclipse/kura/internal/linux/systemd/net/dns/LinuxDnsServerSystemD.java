/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.linux.systemd.net.dns;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.internal.linux.net.dns.DnsServerService;
import org.eclipse.kura.linux.net.dns.LinuxDnsServer;

public class LinuxDnsServerSystemD extends LinuxDnsServer implements DnsServerService {

    private static final String BIND9_COMMAND = "bind9";
    private static final String NAMED_COMMAND = "named";
    private static final String SERVICE_MANAGER = "systemctl";
    private static final int SERVICE_STATUS_UNKNOWN = 4;

    private String dnsCommand;

    public LinuxDnsServerSystemD() throws KuraException {
        if (findWithSystemd(NAMED_COMMAND)) {
            dnsCommand = NAMED_COMMAND;
        } else if (findWithSystemd(BIND9_COMMAND))
            dnsCommand = BIND9_COMMAND;
        else {
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, "Unable to find dns service.");
        }
    }

    @Override
    public String getDnsConfigFileName() {
        return "/etc/bind/named.conf";
    }

    @Override
    public String getDnsRfcZonesFileName() {
        return "/etc/named.rfc1912.zones";
    }

    @Override
    public String getDnsServiceName() {
        return "/usr/sbin/named";
    }

    @Override
    public String[] getDnsStartCommand() {
        return new String[] { SYSTEMCTL_COMMAND, "start", dnsCommand };
    }

    @Override
    public String[] getDnsRestartCommand() {
        return new String[] { SYSTEMCTL_COMMAND, "restart", dnsCommand };
    }

    @Override
    public String[] getDnsStopCommand() {
        return new String[] { SYSTEMCTL_COMMAND, "stop", dnsCommand };
    }

    private boolean findWithSystemd(String serviceName) {
        Command chronyStatusCommand = new Command(new String[] { SERVICE_MANAGER, "status", serviceName });
        chronyStatusCommand.setExecuteInAShell(true);

        int exitCode = getCommandExecutorService().execute(chronyStatusCommand).getExitStatus().getExitCode();

        return (exitCode >= 0 && exitCode != SERVICE_STATUS_UNKNOWN);

    }

}
