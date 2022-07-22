/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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

import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;

public abstract class AbstractLinuxConfigWriter implements NetworkConfigurationVisitor {

    private Optional<WifiMode> getWifiMode(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {

        Optional<WifiMode> wifiMode = Optional.empty();

        Optional<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> wifiConfig = netInterfaceConfig instanceof WifiInterfaceConfigImpl
                ? Optional.of(netInterfaceConfig)
                : Optional.empty();

        if (wifiConfig.isPresent()) {
            WifiInterfaceAddressConfig wifiInterfaceAddressConfig = (WifiInterfaceAddressConfig) ((WifiInterfaceConfigImpl) wifiConfig
                    .get()).getNetInterfaceAddressConfig();
            wifiMode = Optional.ofNullable(wifiInterfaceAddressConfig.getMode());
        }

        return wifiMode;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {

        if (getExecutorService() == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "The CommandExecutorService cannot be null");
        }

        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getModifiedNetInterfaceConfigs();

        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {

            if (netInterfaceConfig.getType() != NetInterfaceType.WIFI
                    || netInterfaceConfig.getName().startsWith("mon.")) {
                continue;
            }

            Optional<WifiMode> wifiMode = getWifiMode(netInterfaceConfig);

            if (wifiMode.isPresent() && acceptMode(wifiMode.get())) {

                writeConfig(netInterfaceConfig);
            }
        }
    }

    protected abstract CommandExecutorService getExecutorService();

    protected abstract void writeConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException;

    protected abstract boolean acceptMode(WifiMode mode);
}
