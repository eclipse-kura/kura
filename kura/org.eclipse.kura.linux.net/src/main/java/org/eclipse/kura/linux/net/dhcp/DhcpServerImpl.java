/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.dhcp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.dhcp.DhcpServer;
import org.eclipse.kura.net.dhcp.DhcpServerCfg;
import org.eclipse.kura.net.dhcp.DhcpServerCfgIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpServerImpl implements DhcpServer {

    private static final Logger logger = LoggerFactory.getLogger(DhcpServerImpl.class);

    private static final String FILE_DIR = "/etc/";
    private static final String PID_FILE_DIR = "/var/run/";

    private final String interfaceName;
    private DhcpServerConfig4 dhcpServerConfig4;

    private final String configFileName;
    private final String pidFileName;
    private final String persistentConfigFileName;
    private final String persistentPidFilename;

    DhcpServerImpl(String interfaceName, boolean enabled, boolean passDns) throws KuraException {
        this.interfaceName = interfaceName;

        StringBuilder sb = new StringBuilder();
        sb.append("dhcpd-").append(interfaceName).append(".conf");
        this.configFileName = sb.toString();

        sb = new StringBuilder();
        sb.append("dhcpd-").append(interfaceName).append(".pid");
        this.pidFileName = sb.toString();

        this.persistentConfigFileName = FILE_DIR + this.configFileName;
        this.persistentPidFilename = PID_FILE_DIR + this.pidFileName;

        readConfig(enabled, passDns);
    }

    private void readConfig(boolean enabled, boolean passDns) throws KuraException {
        // TODO
        File configFile = new File(this.persistentConfigFileName);
        if (configFile.exists()) {

            logger.debug("initing DHCP Server configuration for {}", this.interfaceName);
            // parse the file
            try (FileReader fr = new FileReader(configFile); BufferedReader br = new BufferedReader(fr)) {
                IP4Address subnet = null;
                IP4Address netmask = null;
                IP4Address router = null;
                String ifaceName = null;
                int defaultLeaseTime = -1;
                int maxLeaseTime = -1;
                IP4Address rangeStart = null;
                IP4Address rangeEnd = null;
                ArrayList<IP4Address> dnsList = new ArrayList<>();
                String line;
                while ((line = br.readLine()) != null) {
                    // TODO - really simple for now
                    StringTokenizer st = new StringTokenizer(line);
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        if ("#".equals(token)) {
                            break;
                        } else if ("subnet".equals(token)) {
                            subnet = (IP4Address) IPAddress.parseHostAddress(st.nextToken());
                            if (!"netmask".equals(st.nextToken())) {
                                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                                        "invalid dhcp config file: " + this.persistentConfigFileName);
                            }
                            netmask = (IP4Address) IPAddress.parseHostAddress(st.nextToken());
                        } else if ("interface".equals(token)) {
                            ifaceName = st.nextToken();
                            ifaceName = ifaceName.substring(0, ifaceName.indexOf(';'));
                        } else if ("default-lease-time".equals(token)) {
                            String leaseTime = st.nextToken();
                            defaultLeaseTime = Integer.parseInt(leaseTime.substring(0, leaseTime.indexOf(';')));
                        } else if ("max-lease-time".equals(token)) {
                            String leaseTime = st.nextToken();
                            maxLeaseTime = Integer.parseInt(leaseTime.substring(0, leaseTime.indexOf(';')));
                        } else if ("range".equals(token)) {
                            rangeStart = (IP4Address) IPAddress.parseHostAddress(st.nextToken());
                            String rangeEndString = st.nextToken();
                            rangeEndString = rangeEndString.substring(0, rangeEndString.indexOf(';'));
                            rangeEnd = (IP4Address) IPAddress.parseHostAddress(rangeEndString);
                        } else if ("option".equals(token)) {
                            String option = st.nextToken();
                            if ("routers".equals(option)) {
                                String routerString = st.nextToken();
                                routerString = routerString.substring(0, routerString.indexOf(';'));
                                router = (IP4Address) IPAddress.parseHostAddress(routerString);
                            } else if ("domain-name-servers".equals(option)) {
                                String dnsString = st.nextToken();
                                dnsString = dnsString.substring(0, dnsString.indexOf(';'));
                                dnsList.add((IP4Address) IPAddress.parseHostAddress(dnsString));
                            }
                        }
                    }
                }

                // FIXME - prefix still hardcoded
                try {
                    DhcpServerCfg dhcpServerCfg = new DhcpServerCfg(ifaceName, enabled, defaultLeaseTime, maxLeaseTime,
                            passDns);
                    DhcpServerCfgIP4 dhcpServerCfgIP4 = new DhcpServerCfgIP4(subnet, netmask, (short) 24, router,
                            rangeStart, rangeEnd, dnsList);

                    logger.debug(
                            "instantiating DHCP server configuration during init with dhcpServerCfg={} and dhcpServerCfgIP4={}",
                            dhcpServerCfg, dhcpServerCfgIP4);

                    this.dhcpServerConfig4 = new DhcpServerConfigIP4(dhcpServerCfg, dhcpServerCfgIP4);
                } catch (KuraException e) {
                    logger.error("Failed to craete new DhcpServerConfigIP4 object ", e);
                }
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
            }
        } else {
            logger.debug("There is no current DHCP server configuration for {}", this.interfaceName);
        }
    }

    @Override
    public boolean isRunning() throws KuraException {
        try {
            // Check if dhcpd is running
            int pid = LinuxProcessUtil.getPid(formDhcpdCommand());
            return pid > -1;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public boolean enable() throws KuraException {
        logger.debug("enable()");
        try {
            writeConfig();
        } catch (Exception e1) {
            logger.error("Error writing configuration to filesystem", e1);
            return false;
        }

        try {
            // Check if dhcpd is running
            if (isRunning()) {
                // If so, disable it
                logger.error("DHCP server is already running, bringing it down...");
                disable();
            }
            // Start dhcpd
            // FIXME:MC This leads to a process leak
            if (LinuxProcessUtil.startBackground(formDhcpdCommand(), false) == 0) {
                logger.debug("DHCP server started.");
                logger.trace(this.dhcpServerConfig4.toString());
                return true;
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }

        return false;
    }

    public boolean disable() throws KuraException {
        logger.debug("disable()");
        try {
            if (this.dhcpServerConfig4 != null) {
                writeConfig();
            }
        } catch (Exception e1) {
            logger.error("Error writing configuration to filesystem", e1);
            return false;
        }

        try {
            // Check if dhcpd is running
            int pid = LinuxProcessUtil.getPid(formDhcpdCommand());
            if (pid > -1) {
                // If so, kill it.
                if (LinuxProcessUtil.stop(pid)) {
                    removePidFile();
                } else {
                    logger.debug("Failed to stop process...try to kill");
                    if (LinuxProcessUtil.kill(pid)) {
                        removePidFile();
                    } else {
                        throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "error killing process, pid=" + pid);
                    }
                }
            } else {
                logger.debug("tried to kill DHCP server for interface but it is not running");
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }

        return true;
    }

    public boolean isConfigured() {
        return this.dhcpServerConfig4 != null;
    }

    public void setConfig(DhcpServerConfigIP4 dhcpServerConfig4) throws KuraException {
        logger.debug("setConfig()");

        try {
            this.dhcpServerConfig4 = dhcpServerConfig4;
            if (this.dhcpServerConfig4 == null) {
                logger.warn("Set DHCP configuration to null");
            } else {
                if (dhcpServerConfig4.isEnabled()) {
                    enable();
                } else {
                    writeConfig();
                    disable();
                }
            }
        } catch (Exception e) {
            logger.error("Error setting subnet config for {} ", this.interfaceName, e);
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
        }
    }

    public DhcpServerConfig4 getDhcpServerConfig(boolean enabled, boolean passDns) {
        try {
            readConfig(enabled, passDns);
        } catch (Exception e) {
            logger.error("Error reading config", e);
        }
        return this.dhcpServerConfig4;
    }

    public String getConfigFilename() {
        return this.persistentConfigFileName;
    }

    private void writeConfig() throws KuraException {
        logger.trace("writing to {} with: {}", this.persistentConfigFileName, this.dhcpServerConfig4.toString());
        try (FileOutputStream fos = new FileOutputStream(this.persistentConfigFileName);
                PrintWriter pw = new PrintWriter(fos)) {
            pw.write(this.dhcpServerConfig4.toString());
            pw.flush();
            fos.getFD().sync();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                    "error while building up new configuration files for dhcp servers: ", e);
        }
    }

    private boolean removePidFile() {
        boolean ret = true;
        File pidFile = new File(this.persistentPidFilename);
        if (pidFile.exists()) {
            ret = pidFile.delete();
        }
        return ret;
    }

    private String formDhcpdCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("dhcpd -cf ").append(this.persistentConfigFileName).append(" -pf ")
                .append(this.persistentPidFilename);
        return sb.toString();
    }
}
