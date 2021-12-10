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

import java.io.File;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.internal.linux.net.dns.DnsServerService;
import org.eclipse.kura.linux.net.dns.LinuxDnsServer;

public class LinuxDnsServerSystemD extends LinuxDnsServer implements DnsServerService {

    private static final String BIND9_COMMAND = "bind9";
    private static final String NAMED_COMMAND = "named";
    private static final String BIND9_SERVICE_UNIT_LOC = "/etc/systemd/system/bind9.service";
    private static final String NAMED_SERVICE_UNIT_LOC = "/lib/systemd/system/named.service";

    private String dnsCommand;

    public LinuxDnsServerSystemD() throws KuraException {

        if (new File(NAMED_SERVICE_UNIT_LOC).exists()) {
            dnsCommand = NAMED_COMMAND;
        } else if (new File(BIND9_SERVICE_UNIT_LOC).exists()) {
            dnsCommand = BIND9_COMMAND;
        } else {
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

}
