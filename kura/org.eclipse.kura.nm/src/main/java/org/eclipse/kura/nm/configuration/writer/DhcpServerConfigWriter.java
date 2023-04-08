/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.nm.configuration.writer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.dhcp.DhcpServerTool;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.dhcp.DhcpServerCfg;
import org.eclipse.kura.net.dhcp.DhcpServerCfgIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.nm.NetworkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpServerConfigWriter {

    private static final String DHCP_OPTION_KEY = "dhcp-option=";
    private static final Logger logger = LoggerFactory.getLogger(DhcpServerConfigWriter.class);
    private static final String WRITE_ERROR_MESSAGE = "Failed to write DHCP config file for ";
    private final String interfaceName;
    private final NetworkProperties networkProperties;

    public DhcpServerConfigWriter(String interfaceName, NetworkProperties properties) {
        this.interfaceName = interfaceName;
        this.networkProperties = properties;
    }

    protected String getConfigFilename() {
        return DhcpServerManager.getConfigFilename(this.interfaceName);
    }

    public void writeConfiguration() throws KuraException, UnknownHostException {
        String dhcpConfigFileName = getConfigFilename();
        String tmpDhcpConfigFileName = new StringBuilder(dhcpConfigFileName).append(".tmp").toString();
        logger.debug("Writing DHCP Server configuration for {} in {}", this.interfaceName, dhcpConfigFileName);

        writeConfigFile(tmpDhcpConfigFileName, this.interfaceName, buildDhcpServerConfiguration());
        File tmpDhcpConfigFile = new File(tmpDhcpConfigFileName);
        File dhcpConfigFile = new File(dhcpConfigFileName);
        try {
            if (!FileUtils.contentEquals(tmpDhcpConfigFile, dhcpConfigFile)) {
                if (tmpDhcpConfigFile.renameTo(dhcpConfigFile)) {
                    logger.debug("Successfully wrote DHCP config file for {}", this.interfaceName);
                } else {
                    throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                            WRITE_ERROR_MESSAGE + this.interfaceName);
                }
            } else {
                logger.debug("Not rewriting DHCP config file for {} because it is the same", this.interfaceName);
            }

            Files.deleteIfExists(tmpDhcpConfigFile.toPath());
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, WRITE_ERROR_MESSAGE + this.interfaceName, e);
        }
    }

    private void writeConfigFile(String configFileName, String ifaceName, DhcpServerConfig4 dhcpServerConfig)
            throws KuraException {
        try (FileOutputStream fos = new FileOutputStream(configFileName); PrintWriter pw = new PrintWriter(fos)) {
            logger.debug("writing to {} with: {}", configFileName, dhcpServerConfig);
            DhcpServerTool dhcpServerTool = DhcpServerManager.getTool();
            if (dhcpServerTool == DhcpServerTool.DHCPD) {
                pw.print(dhcpServerConfig.toString());
            } else if (dhcpServerTool == DhcpServerTool.UDHCPD) {
                pw.println("start " + dhcpServerConfig.getRangeStart().getHostAddress());
                pw.println("end " + dhcpServerConfig.getRangeEnd().getHostAddress());
                pw.println("interface " + ifaceName);
                pw.println("pidfile " + DhcpServerManager.getPidFilename(ifaceName));
                pw.println("max_leases "
                        + (ip2int(dhcpServerConfig.getRangeEnd()) - ip2int(dhcpServerConfig.getRangeStart())));
                pw.println("auto_time 0");
                pw.println("decline_time " + dhcpServerConfig.getDefaultLeaseTime());
                pw.println("conflict_time " + dhcpServerConfig.getDefaultLeaseTime());
                pw.println("offer_time " + dhcpServerConfig.getDefaultLeaseTime());
                pw.println("min_lease " + dhcpServerConfig.getDefaultLeaseTime());
                pw.println("opt subnet " + dhcpServerConfig.getSubnetMask().getHostAddress());
                pw.println("opt router " + dhcpServerConfig.getRouterAddress().getHostAddress());
                pw.println("opt lease " + dhcpServerConfig.getDefaultLeaseTime());

                addDNSServersOption(dhcpServerConfig, pw);
            } else if (dhcpServerTool == DhcpServerTool.DNSMASQ) {
                pw.println("interface=" + dhcpServerConfig.getInterfaceName());
                
                StringBuilder dhcpRangeProp = new StringBuilder("dhcp-range=")
                        .append(this.interfaceName)
                        .append(",")
                        .append(dhcpServerConfig.getRangeStart())
                        .append(",")
                        .append(dhcpServerConfig.getRangeEnd())
                        .append(",")
                        .append(dhcpServerConfig.getDefaultLeaseTime()).append("s");
                pw.println(dhcpRangeProp.toString());
                
                pw.println(DHCP_OPTION_KEY + this.interfaceName + ",1,"
                        + NetworkUtil.getNetmaskStringForm(dhcpServerConfig.getPrefix()));
                // router property
                pw.println(
                        DHCP_OPTION_KEY + this.interfaceName + ",3," + dhcpServerConfig.getRouterAddress().toString());

                if (dhcpServerConfig.isPassDns()) {
                    // announce DNS servers on this device
                    pw.println(DHCP_OPTION_KEY + this.interfaceName + ",6,0.0.0.0");
                } else {
                    // leaving the option without value disables it
                    pw.println(DHCP_OPTION_KEY + this.interfaceName + ",6");
                    pw.println("dhcp-ignore-names=" + this.interfaceName);
                }

                // all subnets are local
                pw.println(DHCP_OPTION_KEY + this.interfaceName + ",27,1");
            }
            pw.flush();
            fos.getFD().sync();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, WRITE_ERROR_MESSAGE + this.interfaceName, e);
        }
    }

    private void addDNSServersOption(DhcpServerConfig4 dhcpServerConfig, PrintWriter pw) {
        if (!dhcpServerConfig.getDnsServers().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (IPAddress address : dhcpServerConfig.getDnsServers()) {
                if (address == null) {
                    continue;
                }
                sb.append(address.getHostAddress()).append(" ");
            }
            pw.println("opt dns " + sb.toString().trim());
        }
    }

    private int ip2int(IPAddress ip) {
        int result = 0;
        for (byte b : ip.getAddress()) {
            result = result << 8 | b & 0xFF;
        }
        return result;
    }

    private DhcpServerConfigIP4 buildDhcpServerConfiguration() throws UnknownHostException, KuraException {
        Boolean isEnabled = getDhcpServer4Enabled();
        Integer defaultLeaseTime = getDhcpServer4DefaultLeaseTime();
        Integer maxLeaseTime = getDhcpServer4MaxLeaseTime();
        Boolean passDns = getDhcpServer4PassDns();
        DhcpServerCfg dhcpServerCfg = new DhcpServerCfg(this.interfaceName, isEnabled, defaultLeaseTime, maxLeaseTime,
                passDns);

        IP4Address address = getIP4Address();
        short prefix = getDhcpServer4Prefix();
        IP4Address subnet = getDhcpServer4Subnet(address, prefix);
        IP4Address subnetMask = getIP4SubnetMask(prefix);
        IP4Address rangeStart = getDhcpServer4RangeStart();
        IP4Address rangeEnd = getDhcpServer4RangeEnd();
        List<IP4Address> dnsServers = new ArrayList<>();
        dnsServers.add(address);
        DhcpServerCfgIP4 dhcpServerCfgIP4 = new DhcpServerCfgIP4(subnet, subnetMask, prefix, address, rangeStart,
                rangeEnd, dnsServers);
        return new DhcpServerConfigIP4(dhcpServerCfg, dhcpServerCfgIP4);
    }

    private Boolean getDhcpServer4Enabled() {
        Optional<Boolean> isEnabled = this.networkProperties.getOpt(Boolean.class,
                "net.interface.%s.config.dhcpServer4.enabled", this.interfaceName);
        if (isEnabled.isPresent()) {
            return isEnabled.get();
        } else {
            return false;
        }
    }

    private Integer getDhcpServer4DefaultLeaseTime() {
        Optional<Integer> defaultLeaseTime = this.networkProperties.getOpt(Integer.class,
                "net.interface.%s.config.dhcpServer4.defaultLeaseTime", this.interfaceName);
        if (defaultLeaseTime.isPresent()) {
            return defaultLeaseTime.get();
        } else {
            return -1;
        }
    }

    private Integer getDhcpServer4MaxLeaseTime() {
        Optional<Integer> maxLeaseTime = this.networkProperties.getOpt(Integer.class,
                "net.interface.%s.config.dhcpServer4.maxLeaseTime", this.interfaceName);
        if (maxLeaseTime.isPresent()) {
            return maxLeaseTime.get();
        } else {
            return -1;
        }
    }

    private Boolean getDhcpServer4PassDns() {
        Optional<Boolean> passDns = this.networkProperties.getOpt(Boolean.class,
                "net.interface.%s.config.dhcpServer4.passDns", this.interfaceName);
        if (passDns.isPresent()) {
            return passDns.get();
        } else {
            return false;
        }
    }

    private short getDhcpServer4Prefix() {
        Optional<Short> prefix = this.networkProperties.getOpt(Short.class,
                "net.interface.%s.config.dhcpServer4.prefix", this.interfaceName);
        if (prefix.isPresent()) {
            return prefix.get();
        } else {
            return -1;
        }
    }

    private IP4Address getIP4Address() throws UnknownHostException {
        Optional<String> address = this.networkProperties.getOpt(String.class, "net.interface.%s.config.ip4.address",
                this.interfaceName);
        if (address.isPresent()) {
            return (IP4Address) IPAddress.parseHostAddress(address.get());
        } else {
            throw new UnknownHostException("Address cannot be null or empty.");
        }
    }

    private IP4Address getDhcpServer4Subnet(IP4Address address, short prefix) throws UnknownHostException {
        int prefixInt = prefix;
        int mask = ~((1 << 32 - prefixInt) - 1);
        String subnetMaskString = NetworkUtil.dottedQuad(mask);
        String subnetString = NetworkUtil.calculateNetwork(address.getHostAddress(), subnetMaskString);
        return (IP4Address) IPAddress.parseHostAddress(subnetString);
    }

    private IP4Address getIP4SubnetMask(short prefix) throws UnknownHostException {
        int prefixInt = prefix;
        int mask = ~((1 << 32 - prefixInt) - 1);
        String subnetMaskString = NetworkUtil.dottedQuad(mask);
        return (IP4Address) IPAddress.parseHostAddress(subnetMaskString);
    }

    private IP4Address getDhcpServer4RangeStart() throws UnknownHostException {
        Optional<String> rangeStart = this.networkProperties.getOpt(String.class,
                "net.interface.%s.config.dhcpServer4.rangeStart", this.interfaceName);
        if (rangeStart.isPresent()) {
            return (IP4Address) IPAddress.parseHostAddress(rangeStart.get());
        } else {
            throw new UnknownHostException("Address RangeStart cannot be null or empty.");
        }
    }

    private IP4Address getDhcpServer4RangeEnd() throws UnknownHostException {
        Optional<String> rangeEnd = this.networkProperties.getOpt(String.class,
                "net.interface.%s.config.dhcpServer4.rangeEnd", this.interfaceName);
        if (rangeEnd.isPresent()) {
            return (IP4Address) IPAddress.parseHostAddress(rangeEnd.get());
        } else {
            throw new UnknownHostException("Address RangeEnd cannot be null or empty.");
        }
    }
}
