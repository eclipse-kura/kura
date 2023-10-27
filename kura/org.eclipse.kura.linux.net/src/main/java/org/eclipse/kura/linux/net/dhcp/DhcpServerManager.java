/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.dhcp;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.linux.net.dhcp.server.DhcpLinuxTool;
import org.eclipse.kura.linux.net.dhcp.server.DhcpdConfigConverter;
import org.eclipse.kura.linux.net.dhcp.server.DhcpdLeaseReader;
import org.eclipse.kura.linux.net.dhcp.server.DhcpdTool;
import org.eclipse.kura.linux.net.dhcp.server.DnsmasqConfigConverter;
import org.eclipse.kura.linux.net.dhcp.server.DnsmasqLeaseReader;
import org.eclipse.kura.linux.net.dhcp.server.DnsmasqTool;
import org.eclipse.kura.linux.net.dhcp.server.UdhcpdConfigConverter;
import org.eclipse.kura.linux.net.dhcp.server.UdhcpdLeaseReader;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpServerManager {

    private static final Logger logger = LoggerFactory.getLogger(DhcpServerManager.class);

    private static final String FILE_DIR = "/etc/";
    private static final String PID_FILE_DIR = "/var/run/";
    private static final String LEASES_FILE_DIR = "/var/lib/dhcp/";
    private static DhcpServerTool dhcpServerTool = DhcpServerTool.NONE;
    private final DhcpLinuxTool linuxTool;

    static {
        dhcpServerTool = getTool();
    }

    public DhcpServerManager(CommandExecutorService service) {
        if (dhcpServerTool == DhcpServerTool.DNSMASQ) {
            this.linuxTool = new DnsmasqTool(service);
        } else {
            this.linuxTool = new DhcpdTool(service, dhcpServerTool);
        }
    }

    public static DhcpServerTool getTool() {
        if (dhcpServerTool == DhcpServerTool.NONE) {
            if (LinuxNetworkUtil.toolExists(DhcpServerTool.DHCPD.getValue())) {
                dhcpServerTool = DhcpServerTool.DHCPD;
            } else if (LinuxNetworkUtil.toolExists(DhcpServerTool.UDHCPD.getValue())) {
                dhcpServerTool = DhcpServerTool.UDHCPD;
            } else if (LinuxNetworkUtil.toolExists(DhcpServerTool.DNSMASQ.getValue())) {
                dhcpServerTool = DhcpServerTool.DNSMASQ;
            }
        }

        logger.info("Using {} as DHCP server.", dhcpServerTool.getValue());

        return dhcpServerTool;
    }

    public boolean isRunning(String interfaceName) throws KuraException {
        return this.linuxTool.isRunning(interfaceName);
    }

    public boolean enable(String interfaceName) throws KuraException {
        if (isRunning(interfaceName)) {
            logger.error("DHCP server is already running for {}, bringing it down...", interfaceName);
            disable(interfaceName);
        }

        File configFile = new File(DhcpServerManager.getConfigFilename(interfaceName));
        if (configFile.exists()) {

            createLeasesFile(interfaceName);
            CommandStatus status = this.linuxTool.startInterface(interfaceName);

            if (status.getExitStatus().isSuccessful()) {
                logger.debug("DHCP server started.");
                return true;
            } else {
                logger.debug("Can't start DHCP server, config file does not exist: {}", configFile.getAbsolutePath());
            }
        }

        return false;
    }

    private void createLeasesFile(String interfaceName) {
        try {
            FileUtils.touch(new File(DhcpServerManager.getLeasesFilename(interfaceName)));
        } catch (IOException e) {
            logger.error("Cannot create DHCP server leases file", e);
        }
    }

    public boolean disable(String interfaceName) throws KuraException {
        logger.debug("Disable DHCP server for {}", interfaceName);

        return this.linuxTool.disableInterface(interfaceName);
    }

    public static String getConfigFilename(String interfaceName) {
        StringBuilder sb = new StringBuilder(FILE_DIR);
        if (dhcpServerTool == DhcpServerTool.NONE) {
            return sb.toString();
        }
        if (dhcpServerTool == DhcpServerTool.DNSMASQ) {
            sb.append("dnsmasq.d/");
        }
        sb.append(dhcpServerTool.getValue());
        sb.append('-');
        sb.append(interfaceName);
        sb.append(".conf");

        return sb.toString();
    }

    public static String getPidFilename(String interfaceName) {
        StringBuilder sb = new StringBuilder(PID_FILE_DIR);
        if (dhcpServerTool == DhcpServerTool.DHCPD || dhcpServerTool == DhcpServerTool.UDHCPD) {
            sb.append(dhcpServerTool.getValue());
            sb.append('-');
            sb.append(interfaceName);
            sb.append(".pid");
        }
        return sb.toString();
    }

    public static String getLeasesFilename(String interfaceName) {
        StringBuilder sb = new StringBuilder(LEASES_FILE_DIR);
        if (dhcpServerTool == DhcpServerTool.NONE) {
            return sb.toString();
        }
        if (dhcpServerTool == DhcpServerTool.DNSMASQ) {
            sb.append(dhcpServerTool.getValue());
            sb.append(".leases");
            return sb.toString();
        }
        sb.append(dhcpServerTool.getValue());
        sb.append('-');
        sb.append(interfaceName);
        sb.append(".leases");

        return sb.toString();
    }

    public static Optional<DhcpServerConfigConverter> getConfigConverter() {
        if (dhcpServerTool == DhcpServerTool.DHCPD) {
            return Optional.of(new DhcpdConfigConverter());
        } else if (dhcpServerTool == DhcpServerTool.UDHCPD) {
            return Optional.of(new UdhcpdConfigConverter());
        } else if (dhcpServerTool == DhcpServerTool.DNSMASQ) {
            return Optional.of(new DnsmasqConfigConverter());
        }
        return Optional.empty();
    }

    public static Optional<DhcpServerLeaseReader> getLeaseReader() {
        if (dhcpServerTool == DhcpServerTool.DHCPD) {
            return Optional.of(new DhcpdLeaseReader());
        } else if (dhcpServerTool == DhcpServerTool.UDHCPD) {
            return Optional.of(new UdhcpdLeaseReader());
        } else if (dhcpServerTool == DhcpServerTool.DNSMASQ) {
            return Optional.of(new DnsmasqLeaseReader());
        }
        return Optional.empty();
    }
}
