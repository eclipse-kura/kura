/*******************************************************************************
 * Copyright (c) 2018 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.linux.net.dns;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.dns.DnsServer;
import org.eclipse.kura.net.dns.DnsServerConfig;
import org.eclipse.kura.net.dns.DnsServerConfigIP4;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxNamed implements DnsServer {

    private static final Logger logger = LoggerFactory.getLogger(LinuxNamed.class);

    private static final String PERSISTENT_CONFIG_FILE_NAME = "/etc/bind/named.conf";
    private static final String RFC_1912_ZONES_FILENAME = "/etc/named.rfc1912.zones";
    private static final String PROC_STRING = "/usr/sbin/named";

    private DnsServerConfigIP4 dnsServerConfigIP4;

    protected void activate() {
        logger.info("Activating LinuxNamed...");
        try {
            init();
        } catch (KuraException e) {
            logger.info("Error activating LinuxNamed...");
            if (this.dnsServerConfigIP4 == null) {
                Set<IP4Address> forwarders = new HashSet<>();
                HashSet<NetworkPair<IP4Address>> allowedNetworks = new HashSet<>();
                this.dnsServerConfigIP4 = new DnsServerConfigIP4(forwarders, allowedNetworks);
            }
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("Deactivating LinuxNamed...");
    }

    private void init() throws KuraException {
        File configFile = new File(LinuxNamed.PERSISTENT_CONFIG_FILE_NAME);
        if (!configFile.exists() || !isForwardOnlyConfiguration(configFile)) {
            logger.debug("There is no current DNS server configuration that allows forwarding");
            return;
        }

        logger.debug("initing DNS Server configuration");

        Set<IP4Address> forwarders = new HashSet<>();
        Set<NetworkPair<IP4Address>> allowedNetworks = new HashSet<>();

        try (FileReader fr = new FileReader(configFile); BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if ("forwarders".equals(token)) {
                        // get the forwarders 'forwarders {192.168.1.1;192.168.2.1;};'
                        StringTokenizer st2 = new StringTokenizer(st.nextToken(), "{} ;");
                        while (st2.hasMoreTokens()) {
                            String forwarder = st2.nextToken();
                            if (forwarder != null && !"".equals(forwarder.trim())) {
                                logger.debug("found forwarder: {}", forwarder);
                                forwarders.add((IP4Address) IPAddress.parseHostAddress(forwarder));
                            }
                        }
                    } else if ("allow-query".equals(token)) {
                        // get the networks 'allow-query {192.168.2.0/24;192.168.3.0/24};'
                        StringTokenizer st2 = new StringTokenizer(st.nextToken(), "{} ;");
                        while (st2.hasMoreTokens()) {
                            String allowedNetwork = st2.nextToken();
                            if (allowedNetwork != null && !"".equals(allowedNetwork.trim())) {
                                String[] splitNetwork = allowedNetwork.split("/");
                                allowedNetworks
                                        .add(new NetworkPair<>((IP4Address) IPAddress.parseHostAddress(splitNetwork[0]),
                                                Short.parseShort(splitNetwork[1])));
                            }
                        }
                    }
                }
            }

            // set the configuration and return
            this.dnsServerConfigIP4 = new DnsServerConfigIP4(forwarders, allowedNetworks);
        } catch (Exception e) {
            logger.error("init() :: failed to read the {} configuration file ", configFile.getName(), e);
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
        }
    }

    private boolean isForwardOnlyConfiguration(File configFile) throws KuraException {
        boolean forwardingConfig = false;
        try (FileReader fr = new FileReader(configFile); BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                if ("forward only;".equals(line.trim())) {
                    forwardingConfig = true;
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("isForwardOnlyConfiguration() :: failed to read the {} configuration file ",
                    configFile.getName(), e);
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
        }
        return forwardingConfig;
    }

    @Override
    public boolean isEnabled() {
        try {
            // Check if named is running
            int pid = LinuxProcessUtil.getPid(LinuxNamed.PROC_STRING);
            return pid > -1;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void enable() throws KuraException {
        // write config happened during 'set config' step
        try {
            // Check if named is running
            int pid = LinuxProcessUtil.getPid(LinuxNamed.PROC_STRING);
            if (pid > -1) {
                // If so, disable it
                logger.error("DNS server is already running, bringing it down...");
                disable();
            }
            // Start named
            int result = LinuxProcessUtil.start("/etc/init.d/bind9 start");

            if (result == 0) {
                logger.debug("DNS server started.");
                logger.trace("{}", this.dnsServerConfigIP4);
            } else {
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR);
            }

        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }
    }

    @Override
    public void disable() throws KuraException {
        try {
            int result = LinuxProcessUtil.start("/etc/init.d/bind9 stop");

            if (result == 0) {
                logger.debug("DNS server stopped.");
                logger.trace("{}", this.dnsServerConfigIP4);
            } else {
                logger.debug("tried to kill DNS server for interface but it is not running");
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR);
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }
    }

    @Override
    public void restart() throws KuraException {
        try {
            if (LinuxProcessUtil.start("/etc/init.d/named restart") == 0) {
                logger.debug("DNS server restarted.");
            } else {
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, "error restarting");
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }
    }

    @Override
    public boolean isConfigured() {
        if (this.dnsServerConfigIP4 == null || this.dnsServerConfigIP4.getForwarders() == null
                || this.dnsServerConfigIP4.getAllowedNetworks() == null) {
            return false;
        }
        return this.dnsServerConfigIP4.getForwarders().isEmpty()
                || this.dnsServerConfigIP4.getAllowedNetworks().isEmpty() ? false : true;
    }

    @Override
    public void setConfig(DnsServerConfig dnsServerConfig) throws KuraException {
        if (this.dnsServerConfigIP4 == null) {
            logger.warn("Set DNS server configuration to null");
        }
        if (dnsServerConfig instanceof DnsServerConfigIP4) {
            this.dnsServerConfigIP4 = (DnsServerConfigIP4) dnsServerConfig;
        }
        try {
            writeConfig();
        } catch (Exception e) {
            logger.error("Error setting DNS server config", e);
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public DnsServerConfig getDnsServerConfig() {
        return this.dnsServerConfigIP4;
    }

    @Override
    public String getConfigFilename() {
        return LinuxNamed.PERSISTENT_CONFIG_FILE_NAME;
    }

    private void writeConfig() throws KuraException {
        try (FileOutputStream fos = new FileOutputStream(LinuxNamed.PERSISTENT_CONFIG_FILE_NAME);
                PrintWriter pw = new PrintWriter(fos);) {
            // build up the file
            if (isConfigured()) {
                logger.debug("writing custom named.conf to {} with: {}", LinuxNamed.PERSISTENT_CONFIG_FILE_NAME,
                        this.dnsServerConfigIP4);
                pw.print(getForwardingNamedFile());
            } else {
                logger.debug("writing default named.conf to {} with: {}", LinuxNamed.PERSISTENT_CONFIG_FILE_NAME,
                        this.dnsServerConfigIP4);
                pw.print(getDefaultNamedFile());
            }
            pw.flush();
            fos.getFD().sync();
        } catch (Exception e) {
            logger.error("Failed to write new configuration files for dns servers", e);
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                    "error while building up new configuration files for dns servers: " + e.getMessage());
        }
    }

    private String getForwardingNamedFile() {
        StringBuilder sb = new StringBuilder().append("// Forwarding and Caching Name Server Configuration\n")
                .append("options {\n") //
                .append("\tdirectory \"/var/named\";\n") //
                .append("\tversion \"not currently available\";\n") //
                .append("\tforwarders {");

        Set<IP4Address> forwarders = this.dnsServerConfigIP4.getForwarders();
        for (IPAddress forwarder : forwarders) {
            sb.append(forwarder.getHostAddress()).append(";");
        }
        sb.append("};\n");

        sb.append("\tforward only;\n") //
                .append("\tallow-transfer{\"none\";};\n") //
                .append("\tallow-query {");

        Set<NetworkPair<IP4Address>> allowedNetworks = this.dnsServerConfigIP4.getAllowedNetworks();
        for (NetworkPair<IP4Address> pair : allowedNetworks) {
            sb.append(pair.getIpAddress().getHostAddress()) //
                    .append("/") //
                    .append(pair.getPrefix()) //
                    .append(";");
        }
        sb.append("};\n");
        sb.append("\tmax-cache-ttl 30;\n");
        sb.append("\tmax-ncache-ttl 30;\n");
        sb.append("};\n") //
                .append("zone \".\" IN {\n") //
                .append("\ttype hint;\n") //
                .append("\tfile \"named.ca\";\n") //
                .append("};\n") //
                .append("include \"") //
                .append(LinuxNamed.RFC_1912_ZONES_FILENAME) //
                .append("\";\n");

        return sb.toString();
    }

    private String getDefaultNamedFile() {
        StringBuilder sb = new StringBuilder().append("//\n") //
                .append("// named.conf\n") //
                .append("//\n") //
                .append("// Provided by Red Hat bind package to configure the ISC BIND named(8) DNS\n") //
                .append("// server as a caching only nameserver (as a localhost DNS resolver only).\n") //
                .append("//\n") //
                .append("// See /usr/share/doc/bind*/sample/ for example named configuration files.\n") //
                .append("//\n") //
                .append("\n") //
                .append("options {\n") //
                .append("\tlisten-on port 53 { 127.0.0.1; };\n") //
                .append("\tlisten-on-v6 port 53 { ::1; };\n") //
                .append("\tdirectory	\"/var/named\";\n") //
                .append("\tdump-file	\"/var/named/data/cache_dump.db\";\n") //
                .append("\tstatistics-file \"/var/named/data/named_stats.txt\";\n") //
                .append("\tmemstatistics-file \"/var/named/data/named_mem_stats.txt\";\n") //
                .append("\tallow-query     { localhost; };\n") //
                .append("\trecursion yes;\n") //
                .append("\n") //
                .append("\tmax-cache-ttl 30;\n") //
                .append("\tmax-ncache-ttl 30;\n") //
                .append("\tdnssec-enable yes;\n") //
                .append("\tdnssec-validation yes;\n") //
                .append("\tdnssec-lookaside auto;\n") //
                .append("\n") //
                .append("\t/* Path to ISC DLV key */\n") //
                .append("\nbindkeys-file \"/etc/named.iscdlv.key\";\n") //
                .append("};\n") //
                .append("\n") //
                .append("zone \".\" IN {\n") //
                .append("\ttype hint;\n") //
                .append("\tfile \"named.ca\";\n") //
                .append("};\n") //
                .append("\n") //
                .append("include \"") //
                .append(LinuxNamed.RFC_1912_ZONES_FILENAME) //
                .append("\";\n"); //
        return sb.toString();
    }
}
