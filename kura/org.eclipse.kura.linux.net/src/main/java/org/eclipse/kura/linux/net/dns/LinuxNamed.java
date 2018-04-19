/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Add Fedora support
 *******************************************************************************/
package org.eclipse.kura.linux.net.dns;

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
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.dns.DnsServerConfigIP4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxNamed {

    private static final Logger logger = LoggerFactory.getLogger(LinuxNamed.class);

    private static final String OS_VERSION = System.getProperty("kura.os.version");
    private static final String TARGET_NAME = System.getProperty("target.device");

    private static LinuxNamed linuxNamed = null;
    private static String persistentConfigFileName = null;
    private static String rfc1912ZonesFilename = null;
    private static String procString = null;

    private DnsServerConfigIP4 dnsServerConfigIP4;

    private LinuxNamed() throws KuraException {
        if (OS_VERSION
                .equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion())
                || OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName())
                || OS_VERSION.equals(KuraConstants.BeagleBone.getImageName())) {
            persistentConfigFileName = "/etc/bind/named.conf";
            procString = "/usr/sbin/named";
            if (TARGET_NAME.equals(KuraConstants.ReliaGATE_15_10.getTargetName())) {
                rfc1912ZonesFilename = "/etc/bind/named.rfc1912.zones";
            } else {
                rfc1912ZonesFilename = "/etc/named.rfc1912.zones";
            }
        } else if (OS_VERSION.equals(KuraConstants.ReliaGATE_50_21_Ubuntu.getImageName() + "_"
                + KuraConstants.ReliaGATE_50_21_Ubuntu.getImageVersion())) {
            persistentConfigFileName = "/etc/bind/named.conf";
            procString = "/usr/sbin/named";
            rfc1912ZonesFilename = "/etc/bind/named.rfc1912.zones";
        } else if (OS_VERSION.equals(KuraConstants.Fedora_Pi.getImageName()) || OS_VERSION.equals(
                KuraConstants.Reliagate_20_26.getImageName() + "_" + KuraConstants.Reliagate_20_26.getImageVersion())) {
            persistentConfigFileName = "/etc/named.conf";
            procString = "named -u named -t";
            rfc1912ZonesFilename = "/etc/named.rfc1912.zones";
        } else {
            persistentConfigFileName = "/etc/named.conf";
            procString = "named -u named -t";
            rfc1912ZonesFilename = "/etc/named.rfc1912.zones";
        }

        // initialize the configuration
        init();

        if (this.dnsServerConfigIP4 == null) {
            Set<IP4Address> forwarders = new HashSet<>();
            HashSet<NetworkPair<IP4Address>> allowedNetworks = new HashSet<>();
            this.dnsServerConfigIP4 = new DnsServerConfigIP4(forwarders, allowedNetworks);
        }
    }

    public static synchronized LinuxNamed getInstance() throws KuraException {
        if (linuxNamed == null) {
            linuxNamed = new LinuxNamed();
        }

        return linuxNamed;
    }

    private void init() throws KuraException {
        File configFile = new File(persistentConfigFileName);
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
                                allowedNetworks.add(new NetworkPair<IP4Address>(
                                        (IP4Address) IPAddress.parseHostAddress(splitNetwork[0]),
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
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
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
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
        return forwardingConfig;
    }

    public boolean isEnabled() throws KuraException {
        try {
            // Check if named is running
            int pid = LinuxProcessUtil.getPid(procString);
            return pid > -1;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public boolean enable() throws KuraException {
        // write config happened during 'set config' step
        try {
            // Check if named is running
            int pid = LinuxProcessUtil.getPid(procString);
            if (pid > -1) {
                // If so, disable it
                logger.error("DNS server is already running, bringing it down...");
                disable();
            }
            // Start named
            int result;
            if (OS_VERSION.equals(
                    KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion())
                    || OS_VERSION.equals(KuraConstants.ReliaGATE_10_05.getImageName() + "_"
                            + KuraConstants.ReliaGATE_10_05.getImageVersion())
                    || OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_"
                            + KuraConstants.Intel_Edison.getImageVersion() + "_"
                            + KuraConstants.Intel_Edison.getTargetName())) {
                result = LinuxProcessUtil.start("/etc/init.d/bind start");
            } else if (OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName())
                    || OS_VERSION.equals(KuraConstants.BeagleBone.getImageName())
                    || OS_VERSION.equals(KuraConstants.ReliaGATE_50_21_Ubuntu.getImageName() + "_"
                            + KuraConstants.ReliaGATE_50_21_Ubuntu.getImageVersion())) {
                result = LinuxProcessUtil.start("/etc/init.d/bind9 start");
            } else if (OS_VERSION.equals(KuraConstants.Fedora_Pi.getImageName())
                    || OS_VERSION.equals(KuraConstants.Reliagate_20_26.getImageName() + "_"
                            + KuraConstants.Reliagate_20_26.getImageVersion())) {
                result = LinuxProcessUtil.start("/bin/systemctl start named");
            } else {
                logger.info("Linux named enable fallback");
                result = LinuxProcessUtil.start("/etc/init.d/named start");
            }
            if (result == 0) {
                logger.debug("DNS server started.");
                logger.trace(this.dnsServerConfigIP4.toString());
                return true;
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }

        return false;
    }

    public boolean disable() throws KuraException {
        try {
            int result;
            // If so, stop it.
            if (OS_VERSION.equals(
                    KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion())
                    || OS_VERSION.equals(KuraConstants.ReliaGATE_10_05.getImageName() + "_"
                            + KuraConstants.ReliaGATE_10_05.getImageVersion())
                    || OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_"
                            + KuraConstants.Intel_Edison.getImageVersion() + "_"
                            + KuraConstants.Intel_Edison.getTargetName())) {
                result = LinuxProcessUtil.start("/etc/init.d/bind stop");
            } else if (OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName())
                    || OS_VERSION.equals(KuraConstants.BeagleBone.getImageName())
                    || OS_VERSION.equals(KuraConstants.ReliaGATE_50_21_Ubuntu.getImageName() + "_"
                            + KuraConstants.ReliaGATE_50_21_Ubuntu.getImageVersion())) {
                result = LinuxProcessUtil.start("/etc/init.d/bind9 stop");
            } else if (OS_VERSION.equals(KuraConstants.Fedora_Pi.getImageName())
                    || OS_VERSION.equals(KuraConstants.Reliagate_20_26.getImageName() + "_"
                            + KuraConstants.Reliagate_20_26.getImageVersion())) {
                result = LinuxProcessUtil.start("/bin/systemctl stop named");
            } else {
                result = LinuxProcessUtil.start("/etc/init.d/named stop");
            }

            if (result == 0) {
                logger.debug("DNS server stopped.");
                logger.trace(this.dnsServerConfigIP4.toString());
                return true;
            } else {
                logger.debug("tried to kill DNS server for interface but it is not running");
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }

        return true;
    }

    public boolean restart() throws KuraException {
        try {
            if (LinuxProcessUtil.start("/etc/init.d/named restart") == 0) {
                logger.debug("DNS server restarted.");
            } else {
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, "error restarting");
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }

        return true;
    }

    public boolean isConfigured() {
        if ((this.dnsServerConfigIP4 == null) || (this.dnsServerConfigIP4.getForwarders() == null)
                || (this.dnsServerConfigIP4.getAllowedNetworks() == null)) {
            return false;
        }
        return (this.dnsServerConfigIP4.getForwarders().isEmpty()
                || this.dnsServerConfigIP4.getAllowedNetworks().isEmpty()) ? false : true;
    }

    public void setConfig(DnsServerConfigIP4 dnsServerConfigIP4) throws KuraException {
        try {
            this.dnsServerConfigIP4 = dnsServerConfigIP4;
            if (this.dnsServerConfigIP4 == null) {
                logger.warn("Set DNS server configuration to null");
            }
            writeConfig();
        } catch (Exception e) {
            logger.error("Error setting DNS server config", e);
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public DnsServerConfigIP4 getDnsServerConfig() {
        return this.dnsServerConfigIP4;
    }

    public String getConfigFilename() {
        return persistentConfigFileName;
    }

    private void writeConfig() throws KuraException {
        try (FileOutputStream fos = new FileOutputStream(persistentConfigFileName);
                PrintWriter pw = new PrintWriter(fos);) {
            // build up the file
            if (isConfigured()) {
                logger.debug("writing custom named.conf to {} with: {}", persistentConfigFileName,
                        this.dnsServerConfigIP4.toString());
                pw.print(getForwardingNamedFile());
            } else {
                logger.debug("writing default named.conf to {} with: {}", persistentConfigFileName,
                        this.dnsServerConfigIP4.toString());
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
        for (IP4Address forwarder : forwarders) {
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
                .append(rfc1912ZonesFilename) //
                .append("\";\n");

        return sb.toString();
    }

    private static final String getDefaultNamedFile() {
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
                .append(rfc1912ZonesFilename) //
                .append("\";\n"); //
        return sb.toString();
    }
}
