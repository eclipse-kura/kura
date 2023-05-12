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
package org.eclipse.kura.net.admin.visitor.linux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.dhcp.DhcpClientManager;
import org.eclipse.kura.linux.net.dhcp.DhcpClientTool;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpClientConfigWriter implements NetworkConfigurationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(DhcpClientConfigWriter.class);
    private static final String STANDARD_HOOK_SCRIPT = "#!/bin/bash\n"
            + "\n"
            + "interfaces=()\n"
            + "\n"
            + "if [[ \"${interfaces[*]}\" =~ ${interface} ]]; then\n"
            + "    make_resolv_conf(){\n"
            + "        logger \"Don't set DNS address for $interface\"\n"
            + "        :\n"
            + "    }\n"
            + "fi";

    public DhcpClientConfigWriter() {
        // Do nothing...
    }

    @Override
    public void setExecutorService(CommandExecutorService executorService) {
        // Not needed
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        if (DhcpClientManager.getTool() != DhcpClientTool.DHCLIENT) {
            logger.debug("Hook scripts are supported only by dhclient. Do nothing.");
            return;
        }
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getModifiedNetInterfaceConfigs();

        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if (netInterfaceConfig.getType() == NetInterfaceType.ETHERNET
                    || netInterfaceConfig.getType() == NetInterfaceType.WIFI) {
                writeConfig(netInterfaceConfig);
            }
        }
    }

    private void writeConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) {
        String interfaceName = netInterfaceConfig.getName();
        String hookScriptFileName = DhcpClientManager.getHookScriptFileName();
        if (hookScriptFileName == null || hookScriptFileName.isEmpty()) {
            logger.debug("Hook scripts cannot be empty. Do nothing.");
            return;
        }
        boolean isLan = ((AbstractNetInterface<?>) netInterfaceConfig)
                .getInterfaceStatus() == NetInterfaceStatus.netIPv4StatusEnabledLAN;

        try {
            writeHookScript(interfaceName, hookScriptFileName, isLan);
        } catch (KuraIOException e) {
            logger.error("Failed to write dhcp client hook script for interface {}", interfaceName, e);
        }
    }

    private void writeHookScript(String interfaceName, String hookScriptFileName, boolean addToList)
            throws KuraIOException {
        Path hookScriptFilePath = Paths.get(hookScriptFileName);
        createHookScriptFile(hookScriptFilePath);

        try {
            String fileContent = new String(Files.readAllBytes(hookScriptFilePath), StandardCharsets.UTF_8);
            if (addToList) {
                if (!fileContent.contains(interfaceName)) {
                    Pattern pattern = Pattern.compile("interfaces=\\(.*\\)");
                    Matcher matcher = pattern.matcher(fileContent);
                    if (matcher.find()) {
                        String line = matcher.group();
                        String newLine = line.substring(0, line.length() - 1) + " " + interfaceName + ")";
                        fileContent = fileContent.replace(line, newLine);
                    }
                }
            } else {
                fileContent = fileContent.replace(" " + interfaceName, "");
            }
            Files.write(hookScriptFilePath, fileContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new KuraIOException(e, "Failed to update dhcp client hook script");
        }

    }

    private void createHookScriptFile(Path hookScriptPath) throws KuraIOException {
        if (!Files.exists(hookScriptPath)) {
            try {
                Files.write(hookScriptPath, STANDARD_HOOK_SCRIPT.getBytes());
            } catch (IOException e) {
                throw new KuraIOException(e, "Failed to write standard dhcp client hook script");
            }
        }
        File file = hookScriptPath.toFile();
        if (!file.setReadable(true, false)) {
            logger.debug("Failed to set read permissions to {}", hookScriptPath);
        }
        if (!file.setWritable(true, false)) {
            logger.debug("Failed to set write permissions to {}", hookScriptPath);
        }
    }
}
