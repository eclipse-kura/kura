/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WifiConfigReader implements NetworkConfigurationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(WifiConfigReader.class);

    private static WifiConfigReader instance;

    private final List<NetworkConfigurationVisitor> visitors;

    private WifiConfigReader() {
        this.visitors = new ArrayList<>();
        this.visitors.add(WpaSupplicantConfigReader.getInstance());
        this.visitors.add(HostapdConfigReader.getInstance());
    }

    public static WifiConfigReader getInstance() {
        if (instance == null) {
            instance = new WifiConfigReader();
        }

        return instance;
    }

    @Override
    public void setExecutorService(CommandExecutorService executorService) {
        // Not needed
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getNetInterfaceConfigs();

        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if (netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
                getConfig((WifiInterfaceConfigImpl) netInterfaceConfig);
            }
        }

        // Get wpa_supplicant and hostapd configs
        for (NetworkConfigurationVisitor visitor : this.visitors) {
            visitor.visit(config);
        }
    }

    // Get common wifi config
    private void getConfig(WifiInterfaceConfigImpl wifiInterfaceConfig) throws KuraException {
        String interfaceName = wifiInterfaceConfig.getName();
        logger.debug("Getting wifi config for {}", interfaceName);

        List<WifiInterfaceAddressConfig> wifiInterfaceAddressConfigs = wifiInterfaceConfig.getNetInterfaceAddresses();

        if (wifiInterfaceAddressConfigs == null || wifiInterfaceAddressConfigs.isEmpty()) {
            wifiInterfaceAddressConfigs = new ArrayList<>();
            wifiInterfaceAddressConfigs.add(new WifiInterfaceAddressConfigImpl());
            wifiInterfaceConfig.setNetInterfaceAddresses(wifiInterfaceAddressConfigs);
        }

        WifiInterfaceAddressConfig wifiInterfaceAddressConfig = wifiInterfaceAddressConfigs.get(0);
        if (wifiInterfaceAddressConfig instanceof WifiInterfaceAddressConfigImpl) {
            StringBuilder wifiModeKey = new StringBuilder("net.interface.").append(interfaceName)
                    .append(".config.wifi.mode");

            WifiMode wifiMode = WifiMode.UNKNOWN;
            // String wifiModeString = KuranetConfig.getProperty(wifiModeKey.toString());
            // if (wifiModeString != null) {
            // wifiMode = WifiMode.valueOf(wifiModeString);
            // }

            logger.debug("Got wifiMode: {}", wifiMode);
            ((WifiInterfaceAddressConfigImpl) wifiInterfaceAddressConfig).setMode(wifiMode);
        }
    }
}
