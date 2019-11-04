/*******************************************************************************
 * Copyright (c) 2018, 2019 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.linux.systemd.net.dns;

import org.eclipse.kura.internal.linux.net.dns.DnsServerService;
import org.eclipse.kura.linux.net.dns.LinuxDnsServer;

public class LinuxDnsServerSystemD extends LinuxDnsServer implements DnsServerService {

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
        return new String[] { SYSTEMCTL_COMMAND, "start", NAMED };
    }

    @Override
    public String[] getDnsRestartCommand() {
        return new String[] { SYSTEMCTL_COMMAND, "restart", NAMED };
    }

    @Override
    public String[] getDnsStopCommand() {
        return new String[] { SYSTEMCTL_COMMAND, "stop", NAMED };
    }

}
