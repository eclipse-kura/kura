/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     3 Port d.o.o.
 *******************************************************************************/
package org.eclipse.kura.internal.linux.sysv.net.dns;

import java.io.File;

import org.eclipse.kura.internal.linux.net.dns.DnsServerService;
import org.eclipse.kura.linux.net.dns.LinuxDnsServer;

public class LinuxDnsServerSysV extends LinuxDnsServer implements DnsServerService {

    private static final String BIND9_COMMAND = "/etc/init.d/bind9";
    private static final String NAMED_COMMAND = "/etc/init.d/named";

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
        return new String[] { getCommandName(), "start" };
    }

    @Override
    public String[] getDnsRestartCommand() {
        return new String[] { getCommandName(), "restart" };
    }

    @Override
    public String[] getDnsStopCommand() {
        return new String[] { getCommandName(), "stop" };
    }

    private String getCommandName() {
        return new File(NAMED_COMMAND).exists() ? NAMED_COMMAND : BIND9_COMMAND;
    }
}
