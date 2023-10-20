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
import org.eclipse.kura.linux.net.dhcp.DhcpServerConfigConverter;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
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
            Optional<DhcpServerConfigConverter> configConverter = DhcpServerManager.getConfigConverter();
            configConverter.ifPresent(converter -> {
                pw.print(converter.convert(dhcpServerConfig));
            });
            pw.flush();
            fos.getFD().sync();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, WRITE_ERROR_MESSAGE + this.interfaceName, e);
        }
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
