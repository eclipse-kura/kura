/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WifiConfigWriter implements NetworkConfigurationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(WifiConfigWriter.class);

    private static WifiConfigWriter wifiConfigWriterInstance;
    private final List<NetworkConfigurationVisitor> visitors;
    private CommandExecutorService executorService;

    private WifiConfigWriter() {
        this.visitors = new ArrayList<>();
        this.visitors.add(WpaSupplicantConfigWriter.getInstance());
        this.visitors.add(HostapdConfigWriter.getInstance());
    }

    public static WifiConfigWriter getInstance() {
        if (wifiConfigWriterInstance == null) {
            wifiConfigWriterInstance = new WifiConfigWriter();
        }

        return wifiConfigWriterInstance;
    }

    @Override
    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getModifiedNetInterfaceConfigs();

        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if (netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
                writeConfig((WifiInterfaceConfigImpl) netInterfaceConfig);
            }
        }

        // Write wpa_supplicant and hostapd configs
        for (NetworkConfigurationVisitor visitor : this.visitors) {
            visitor.setExecutorService(this.executorService);
            visitor.visit(config);
        }

        // After every visit, unset the executorService. This must be set before every call.
        this.executorService = null;
    }

    // Write common wifi config
    private void writeConfig(WifiInterfaceConfigImpl wifiInterfaceConfig) throws KuraException {
        String interfaceName = wifiInterfaceConfig.getName();
        logger.debug("Writing wifi config for {}", interfaceName);

        WifiInterfaceAddressConfig wifiInterfaceAddressConfig = (WifiInterfaceAddressConfig) (wifiInterfaceConfig)
                .getNetInterfaceAddressConfig();

        // Store the selected wifi mode
        WifiMode wifiMode = wifiInterfaceAddressConfig.getMode();
        logger.debug("Store wifiMode: {}", wifiMode);
        StringBuilder key = new StringBuilder("net.interface." + interfaceName + ".config.wifi.mode");
        try {
            KuranetConfig.setProperty(key.toString(), wifiMode.toString());
        } catch (Exception e) {
            logger.error("Failed to save kuranet config", e);
            throw KuraException.internalError(e);
        }
    }
}
