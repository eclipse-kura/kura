/*******************************************************************************
 * Copyright (c) 2019, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  3 Port d.o.o.
 *******************************************************************************/
package org.eclipse.kura.linux.net.dns;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.net.util.SubnetUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.dns.DnsServerConfig;
import org.eclipse.kura.net.dns.DnsServerConfigIP4;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LinuxDnsServer {

    private static final Logger logger = LoggerFactory.getLogger(LinuxDnsServer.class);
    public static final String NAMED = "named";
    public static final String SYSTEMCTL_COMMAND = "/bin/systemctl";

    private DnsServerConfigIP4 dnsServerConfigIP4;
    private CommandExecutorService executorService;

    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    public void unsetExecutorService(CommandExecutorService executorService) {
        this.executorService = null;
    }

    protected CommandExecutorService getCommandExecutorService() {
        return this.executorService;
    }

    protected void activate() {
        logger.info("Activating LinuxDnsServer...");
        try {
            init();
        } catch (KuraException e) {
            logger.info("Error activating LinuxDnsServer...");
            if (this.dnsServerConfigIP4 == null) {
                Set<IP4Address> forwarders = new HashSet<>();
                HashSet<NetworkPair<IP4Address>> allowedNetworks = new HashSet<>();
                this.dnsServerConfigIP4 = new DnsServerConfigIP4(forwarders, allowedNetworks);
            }
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("Deactivating LinuxDnsServer...");
    }

    private void init() throws KuraException {
        File configFile = new File(getDnsConfigFileName());
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
                parseDnsConfigFile(forwarders, allowedNetworks, line);
            }

            // set the configuration and return
            this.dnsServerConfigIP4 = new DnsServerConfigIP4(forwarders, allowedNetworks);
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
        }
    }

    private void parseDnsConfigFile(Set<IP4Address> forwarders, Set<NetworkPair<IP4Address>> allowedNetworks,
            String line) throws UnknownHostException {
        StringTokenizer st = new StringTokenizer(line);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if ("forwarders".equals(token)) {
                getForwarders(forwarders, st);
            } else if ("allow-query".equals(token)) {
                getAllowedNetworks(allowedNetworks, st);
            }
        }
    }

    private void getAllowedNetworks(Set<NetworkPair<IP4Address>> allowedNetworks, StringTokenizer st)
            throws UnknownHostException {
        // get the networks 'allow-query {192.168.2.0/24;192.168.3.0/24};'
        StringTokenizer st2 = new StringTokenizer(st.nextToken(), "{} ;");
        while (st2.hasMoreTokens()) {
            String allowedNetwork = st2.nextToken();
            if (allowedNetwork != null && !"".equals(allowedNetwork.trim())) {
                String[] splitNetwork = allowedNetwork.split("/");
                allowedNetworks.add(new NetworkPair<>((IP4Address) IPAddress.parseHostAddress(splitNetwork[0]),
                        Short.parseShort(splitNetwork[1])));
            }
        }
    }

    private void getForwarders(Set<IP4Address> forwarders, StringTokenizer st) throws UnknownHostException {
        // get the forwarders 'forwarders {192.168.1.1;192.168.2.1;};'
        StringTokenizer st2 = new StringTokenizer(st.nextToken(), "{} ;");
        while (st2.hasMoreTokens()) {
            String forwarder = st2.nextToken();
            if (forwarder != null && !"".equals(forwarder.trim())) {
                logger.debug("found forwarder: {}", forwarder);
                forwarders.add((IP4Address) IPAddress.parseHostAddress(forwarder));
            }
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
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
        }
        return forwardingConfig;
    }

    public boolean isRunning() {
        // Check if named is running
        return this.executorService.isRunning(new String[] { getDnsServiceName() });
    }

    public void start() throws KuraException {
        // write config happened during 'set config' step
        if (isRunning()) {
            // If so, disable it
            logger.error("DNS server is already running, bringing it down...");
            stop();
        }
        // Start named
        CommandStatus status = this.executorService.execute(new Command(getDnsStartCommand()));
        if (status.getExitStatus().isSuccessful()) {
            logger.debug("DNS server started.");
            logger.trace("{}", this.dnsServerConfigIP4);
        } else {
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, "Failed to start named");
        }
    }

    public void stop() throws KuraException {
        // Stop named
        CommandStatus status = this.executorService.execute(new Command(getDnsStopCommand()));
        if (status.getExitStatus().isSuccessful()) {
            logger.debug("DNS server stopped.");
            logger.trace("{}", this.dnsServerConfigIP4);
        } else {
            logger.debug("tried to kill DNS server for interface but it is not running");
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, "Failed to stop named");
        }
    }

    public void restart() throws KuraException {
        // Restart named
        CommandStatus status = this.executorService.execute(new Command(getDnsRestartCommand()));
        if (status.getExitStatus().isSuccessful()) {
            logger.debug("DNS server restarted.");
        } else {
            logger.debug("tried to kill DNS server for interface but it is not running");
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, "Failed to restart named");
        }
    }

    public boolean isConfigured() {
        if (this.dnsServerConfigIP4 == null || this.dnsServerConfigIP4.getForwarders() == null
                || this.dnsServerConfigIP4.getAllowedNetworks() == null) {
            return false;
        }
        return !(this.dnsServerConfigIP4.getForwarders().isEmpty()
                || this.dnsServerConfigIP4.getAllowedNetworks().isEmpty());
    }

    public void setConfig(DnsServerConfig dnsServerConfig) {
        if (this.dnsServerConfigIP4 == null) {
            logger.warn("Set DNS server configuration to null");
        }
        if (dnsServerConfig instanceof DnsServerConfigIP4) {
            this.dnsServerConfigIP4 = (DnsServerConfigIP4) dnsServerConfig;
        }
        try {
            writeConfig();
        } catch (IOException e) {
            logger.error("Error persisting DNS server config.", e);
        }
    }

    public DnsServerConfig getConfig() {
        return this.dnsServerConfigIP4;
    }

    private void writeConfig() throws IOException {

        final File tempFile = new File(getDnsConfigFileName() + ".tmp");
        final File persistentConfigFile = new File(getDnsConfigFileName());

        try (FileOutputStream fos = new FileOutputStream(tempFile); PrintWriter pw = new PrintWriter(fos);) {
            // build up the file
            if (isConfigured()) {
                logger.debug("writing custom named.conf to {} with: {}", persistentConfigFile.getAbsolutePath(),
                        this.dnsServerConfigIP4);
                pw.print(getForwardingNamedFile());
            } else {
                logger.debug("writing default named.conf to {} with: {}", persistentConfigFile.getAbsolutePath(),
                        this.dnsServerConfigIP4);
                pw.print(getDefaultNamedFile());
            }
            pw.flush();
            fos.getFD().sync();
        }

        if (!tempFile.setReadable(true, false)) {
            logger.warn("failed to set permissions to {}", tempFile.getAbsolutePath());
        }

        Files.move(tempFile.toPath(), persistentConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
            SubnetUtils ip = new SubnetUtils(pair.getIpAddress().getHostAddress() + "/" + pair.getPrefix());
            sb.append(ip.getInfo().getNetworkAddress()) //
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
                .append(getDnsRfcZonesFileName()) //
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
                .append("\tdirectory    \"/var/named\";\n") //
                .append("\tdump-file    \"/var/named/data/cache_dump.db\";\n") //
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
                .append(getDnsRfcZonesFileName()) //
                .append("\";\n"); //
        return sb.toString();
    }

    public String getDnsConfigFileName() {
        return "/etc/bind/named.conf";
    }

    public String getDnsRfcZonesFileName() {
        return "/etc/named.rfc1912.zones";
    }

    public String getDnsServiceName() {
        return "/usr/sbin/named";
    }

    public String[] getDnsStartCommand() {
        return new String[] { SYSTEMCTL_COMMAND, "start", NAMED };
    }

    public String[] getDnsRestartCommand() {
        return new String[] { SYSTEMCTL_COMMAND, "restart", NAMED };
    }

    public String[] getDnsStopCommand() {
        return new String[] { SYSTEMCTL_COMMAND, "stop", NAMED };
    }
}
