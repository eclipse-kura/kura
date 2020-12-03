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
