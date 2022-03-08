/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern PPP_INRERFACE_PATTERN = Pattern.compile("ppp([0-9]+)");

    private static final Object LOCK = new Object();
    private final String interfaceName;
    private final String pppInterfaceName;
    private final int pppInterfaceNumber;
    private final String port;
    private final CommandExecutorService executorService;
    private final PppLinux pppLinux;
    private final LinuxNetworkUtil linuxNetworkUtil;

    public Ppp(String interfaceName, String pppInterfaceName, String port, CommandExecutorService executorService) {
        this.interfaceName = interfaceName;
        this.pppInterfaceName = pppInterfaceName;
        final Matcher matcher = PPP_INRERFACE_PATTERN.matcher(pppInterfaceName);
        if (matcher.matches()) {
            this.pppInterfaceNumber = Integer.parseInt(matcher.group(1));
        } else {
            throw new IllegalArgumentException("Cannot determine ppp interface number");
        }
        this.port = port;
        this.executorService = executorService;
        this.pppLinux = new PppLinux(executorService);
        this.linuxNetworkUtil = new LinuxNetworkUtil(executorService);
    }

    @Override
    public void connect() throws KuraException {
        // stop for existing pppd instances for the given interfaceName and port and possibly another ppp interface
        if (pppLinux.isPppProcessRunning(interfaceName, port)) {
            logger.info("found existing pppd instance for {} {}, stopping it", interfaceName, port);
            pppLinux.disconnect(interfaceName, port);
        }

        this.pppLinux.connect(this.interfaceName, this.port, OptionalInt.of(this.pppInterfaceNumber));
    }

    @Override
    public void disconnect() throws KuraException {

        logger.info("disconnecting :: stopping PPP monitor ...");
        this.pppLinux.disconnect(this.interfaceName, this.port, OptionalInt.of(this.pppInterfaceNumber));

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
        LinuxIfconfig ifconfig = this.linuxNetworkUtil.getInterfaceConfiguration(this.pppInterfaceName);
        if (ifconfig != null) {
            ipAddress = ifconfig.getInetAddress();
        }
        return ipAddress;
    }

    @Override
    public String getIfaceName() {
        return this.interfaceName;
    }

    @Override
    public PppState getPppState() throws KuraException {
        synchronized (LOCK) {
            final PppState pppState;
            final boolean pppdRunning = this.pppLinux.isPppProcessRunning(this.interfaceName, this.port,
                    OptionalInt.of(this.pppInterfaceNumber));
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
