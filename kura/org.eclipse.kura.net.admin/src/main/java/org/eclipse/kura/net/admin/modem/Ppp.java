/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.admin.modem;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.linux.net.ppp.PppLinux;
import org.eclipse.kura.linux.net.util.LinuxIfconfig;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ppp implements IModemLinkService {

    private static final Logger logger = LoggerFactory.getLogger(Ppp.class);

    private static final Object LOCK = new Object();
    private final String iface;
    private final String port;
    private final CommandExecutorService executorService;
    private final PppLinux pppLinux;
    private final LinuxNetworkUtil linuxNetworkUtil;

    public Ppp(String iface, String port, CommandExecutorService executorService) {
        this.iface = iface;
        this.port = port;
        this.executorService = executorService;
        this.pppLinux = new PppLinux(executorService);
        this.linuxNetworkUtil = new LinuxNetworkUtil(executorService);
    }

    @Override
    public void connect() throws KuraException {
        this.pppLinux.connect(this.iface, this.port);
    }

    @Override
    public void disconnect() throws KuraException {

        logger.info("disconnecting :: stopping PPP monitor ...");
        this.pppLinux.disconnect(this.iface, this.port);

        try {
            LinuxDns linuxDns = LinuxDns.getInstance();
            if (linuxDns.isPppDnsSet()) {
                linuxDns.unsetPppDns(this.executorService);
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public String getIPaddress() throws KuraException {
        String ipAddress = null;
        LinuxIfconfig ifconfig = this.linuxNetworkUtil.getInterfaceConfiguration(this.iface);
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
        synchronized (LOCK) {
            final PppState pppState;
            final boolean pppdRunning = this.pppLinux.isPppProcessRunning(this.iface, this.port);
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
