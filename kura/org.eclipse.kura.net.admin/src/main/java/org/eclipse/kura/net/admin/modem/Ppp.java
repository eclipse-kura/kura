/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.linux.net.ppp.PppLinux;
import org.eclipse.kura.linux.net.util.LinuxIfconfig;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ppp implements IModemLinkService {

    private static final Logger s_logger = LoggerFactory.getLogger(Ppp.class);

    private static final Object lock = new Object();
    private final String iface;
    private final String port;

    public Ppp(String iface, String port) {
        this.iface = iface;
        this.port = port;
    }

    @Override
    public void connect() throws KuraException {
        PppLinux.connect(this.iface, this.port);
    }

    @Override
    public void disconnect() throws KuraException {

        s_logger.info("disconnecting :: stopping PPP monitor ...");
        PppLinux.disconnect(this.iface, this.port);

        try {
            LinuxDns linuxDns = LinuxDns.getInstance();
            if (linuxDns.isPppDnsSet()) {
                linuxDns.unsetPppDns();
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public String getIPaddress() throws KuraException {
        String ipAddress = null;
        LinuxIfconfig ifconfig = LinuxNetworkUtil.getInterfaceConfiguration(this.iface);
        if (ifconfig != null) {
            ipAddress = ifconfig.getInetAddress();
        }
        return ipAddress;
    }

    @Override
    public String getIfaceName() {
        return this.iface;
    }

    @Override
    public PppState getPppState() throws KuraException {
        synchronized (lock) {
            final PppState pppState;
            final boolean pppdRunning = PppLinux.isPppProcessRunning(this.iface, this.port);
            final String ip = getIPaddress();

            if (pppdRunning && ip != null) {
                pppState = PppState.CONNECTED;
            } else if (pppdRunning) {
                pppState = PppState.IN_PROGRESS;
            } else {
                pppState = PppState.NOT_CONNECTED;
            }
            return pppState;
        }
    }
}
