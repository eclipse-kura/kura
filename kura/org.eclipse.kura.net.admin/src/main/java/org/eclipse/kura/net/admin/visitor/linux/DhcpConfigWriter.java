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
package org.eclipse.kura.net.admin.visitor.linux;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.dhcp.DhcpServerTool;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpConfigWriter implements NetworkConfigurationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(DhcpConfigWriter.class);

    public DhcpConfigWriter() {
        // Do nothing...
    }

    @Override
    public void setExecutorService(CommandExecutorService executorService) {
        // Not needed
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getModifiedNetInterfaceConfigs();

        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if (netInterfaceConfig.getType() == NetInterfaceType.ETHERNET
                    || netInterfaceConfig.getType() == NetInterfaceType.WIFI) {
                writeConfig(netInterfaceConfig);
            }
        }
    }

    protected String getConfigFilename(String interfaceName) {
        return DhcpServerManager.getConfigFilename(interfaceName);
    }

    private void writeConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {
        String interfaceName = netInterfaceConfig.getName();

        String dhcpConfigFileName = getConfigFilename(interfaceName);
        String tmpDhcpConfigFileName = new StringBuilder(dhcpConfigFileName).append(".tmp").toString();

        logger.debug("Writing DHCP config for {}", interfaceName);
        NetInterfaceAddressConfig netInterfaceAddressConfig = ((AbstractNetInterface<?>) netInterfaceConfig)
                .getNetInterfaceAddressConfig();
        if (((AbstractNetInterface<?>) netInterfaceConfig).isInterfaceEnabled()
                && ((AbstractNetInterface<?>) netInterfaceConfig)
                        .getInterfaceStatus() != NetInterfaceStatus.netIPv4StatusL2Only) {
            writeNetInterfaceConfig(interfaceName, dhcpConfigFileName, tmpDhcpConfigFileName,
                    netInterfaceAddressConfig);
        }

    }

    private void writeNetInterfaceConfig(String interfaceName, String dhcpConfigFileName, String tmpDhcpConfigFileName,
            NetInterfaceAddressConfig netInterfaceAddressConfig) throws KuraException {
        List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();

        if (netConfigs != null) {
            for (NetConfig netConfig : netConfigs) {
                if (netConfig instanceof DhcpServerConfig4) {
                    DhcpServerConfig4 dhcpServerConfig = (DhcpServerConfig4) netConfig;
                    writeConfigFile(tmpDhcpConfigFileName, interfaceName, dhcpServerConfig);
                    // move the file if we made it this far and they are different
                    File tmpDhcpConfigFile = new File(tmpDhcpConfigFileName);
                    File dhcpConfigFile = new File(dhcpConfigFileName);
                    try {
                        if (!FileUtils.contentEquals(tmpDhcpConfigFile, dhcpConfigFile)) {
                            if (tmpDhcpConfigFile.renameTo(dhcpConfigFile)) {
                                logger.trace("Successfully wrote DHCP config file");
                            } else {
                                logger.error("Failed to write DHCP config file for {}", interfaceName);
                                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                                        "error while building up new configuration files for dhcp server: "
                                                + interfaceName);
                            }
                        } else {
                            logger.info("Not rewriting DHCP config file for {} because it is the same", interfaceName);
                        }

                        Files.deleteIfExists(tmpDhcpConfigFile.toPath());
                    } catch (IOException e) {
                        throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                                "error while building up new configuration files for dhcp servers", e);
                    }
                }
            }
        }
    }

    private void writeConfigFile(String configFileName, String ifaceName, DhcpServerConfig4 dhcpServerConfig)
            throws KuraException {
        try (FileOutputStream fos = new FileOutputStream(configFileName); PrintWriter pw = new PrintWriter(fos)) {
            logger.trace("writing to {} with: {}", configFileName, dhcpServerConfig.toString());
            DhcpServerTool dhcpServerTool = DhcpServerManager.getTool();
            if (dhcpServerTool == DhcpServerTool.DHCPD) {
                pw.print(dhcpServerConfig.toString());
            } else if (dhcpServerTool == DhcpServerTool.UDHCPD) {
                pw.println("start " + dhcpServerConfig.getRangeStart().getHostAddress());
                pw.println("end " + dhcpServerConfig.getRangeEnd().getHostAddress());
                pw.println("interface " + ifaceName);
                pw.println("pidfile " + DhcpServerManager.getPidFilename(ifaceName));
                pw.println("lease_file " + DhcpServerManager.getLeaseFilename(ifaceName));
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
            }
            pw.flush();
            fos.getFD().sync();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                    "error while building up new configuration files for dhcp servers", e);
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

}
