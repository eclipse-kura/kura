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
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.dhcp.DhcpServerConfigConverter;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
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
            Optional<DhcpServerConfigConverter> configConverter = DhcpServerManager.getConfigConverter();
            configConverter.ifPresent(converter -> {
                pw.print(converter.convert(dhcpServerConfig));
            });
            pw.flush();
            fos.getFD().sync();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                    "error while building up new configuration files for dhcp servers", e);
        }
    }

}
