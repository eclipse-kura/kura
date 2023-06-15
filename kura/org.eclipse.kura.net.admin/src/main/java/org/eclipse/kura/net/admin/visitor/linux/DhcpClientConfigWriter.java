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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private static final String DEFAULT_RESOLV_CONF_HOOK_SCRIPT = "#!/bin/sh\n" + "\n" + "interfaces=\"\"\n" + "\n"
            + "for item in $interfaces; do \n" + "    if [ \"$item\" = \"$interface\" ]; then\n"
            + "        make_resolv_conf(){\n" + "            logger \"Don't set DNS address for $interface\"\n"
            + "            :\n" + "        }\n" + "    fi\n" + "done\n";

    private static final String DEFAULT_ROUTE_HOOK_SCRIPT = "#!/bin/sh\n" + "\ninterfaces=\"\"\n" + "\n"
            + "for iface in $interfaces; do\n" + "    if [ \"$iface\" = \"$interface\" ]; then\n"
            + "        case $reason in\n" + "        BOUND|RENEW|REBIND|REBOOT)\n" + "            unset new_routers\n"
            + "            ;;\n" + "        esac\n" + "    fi\n" + "done\n";

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

        List<String> lanInterfaceNames = new ArrayList<>();
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getModifiedNetInterfaceConfigs();
        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            boolean isLan = ((AbstractNetInterface<?>) netInterfaceConfig)
                    .getInterfaceStatus() == NetInterfaceStatus.netIPv4StatusEnabledLAN;
            if (isLan && (netInterfaceConfig.getType() == NetInterfaceType.ETHERNET
                    || netInterfaceConfig.getType() == NetInterfaceType.WIFI)) {
                lanInterfaceNames.add(netInterfaceConfig.getName());
            }
        }

        lanInterfaceNames.sort(null);
        writeDhcpConfClientConfig(lanInterfaceNames, DhcpClientManager.getResolvConfHookScriptFileName(),
                DEFAULT_RESOLV_CONF_HOOK_SCRIPT);
        writeDhcpConfClientConfig(lanInterfaceNames, DhcpClientManager.getRouteHookScriptFileName(),
                DEFAULT_ROUTE_HOOK_SCRIPT);
    }

    private void writeDhcpConfClientConfig(List<String> interfaceNames, String hookScriptFileName,
            String scriptContent) {
        if (hookScriptFileName == null || hookScriptFileName.isEmpty()) {
            logger.debug("Hook script file name not defined. Do nothing.");
            return;
        }

        try {
            writeDhcpClientHookScript(interfaceNames, hookScriptFileName, scriptContent);
        } catch (KuraIOException e) {
            logger.error("Failed to write dhclient hook script", e);
        }
    }

    private void writeDhcpClientHookScript(List<String> interfaceNames, String hookScriptFileName, String scriptContent)
            throws KuraIOException {
        StringBuilder interfacesLine = new StringBuilder("interfaces=\"");
        interfacesLine.append(interfaceNames.stream().collect(Collectors.joining(" ")));
        interfacesLine.append("\"\n");
        String hookScriptContent = scriptContent.replace("interfaces=\"\"\n", interfacesLine);

        Path hookScriptFilePath = Paths.get(hookScriptFileName);
        try {
            if (Files.exists(hookScriptFilePath) && Files.isReadable(hookScriptFilePath)) {
                String currentHookScriptContent = new String(Files.readAllBytes(hookScriptFilePath),
                        StandardCharsets.UTF_8);
                if (!hookScriptContent.equals(currentHookScriptContent)) {
                    writeFile(hookScriptFilePath, hookScriptContent);
                }
            } else {
                writeFile(hookScriptFilePath, hookScriptContent);
            }
        } catch (IOException e) {
            throw new KuraIOException(e, "Failed to update dhclient hook script");
        }
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

}
