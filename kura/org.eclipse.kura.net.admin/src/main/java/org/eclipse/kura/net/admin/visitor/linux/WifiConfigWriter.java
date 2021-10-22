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

import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;

public class WifiConfigWriter implements NetworkConfigurationVisitor {

    private CommandExecutorService executorService;
    private WpaSupplicantConfigWriter wpaSupplicantConfigWriter;
    private HostapdConfigWriter hostapdConfigWriter;

    public WifiConfigWriter() {
        this.wpaSupplicantConfigWriter = new WpaSupplicantConfigWriter();
        this.hostapdConfigWriter = new HostapdConfigWriter();
    }

    @Override
    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getModifiedNetInterfaceConfigs();

        Optional<WifiMode> wifiMode = getWifiMode(netInterfaceConfigs);
        if (wifiMode.isPresent()) {
            switch (wifiMode.get()) {
            case MASTER:
                this.hostapdConfigWriter.setExecutorService(this.executorService);
                this.hostapdConfigWriter.visit(config);
                break;
            case ADHOC:
            case INFRA:
                this.wpaSupplicantConfigWriter.setExecutorService(this.executorService);
                this.wpaSupplicantConfigWriter.visit(config);
                break;
            case UNKNOWN:
            default:
            }
        }

        // After every visit, unset the executorService. This must be set before every call.
        this.executorService = null;
    }

    private Optional<WifiMode> getWifiMode(
            List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs) throws KuraException {
        Optional<WifiMode> wifiMode = Optional.empty();
        Optional<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> wifiConfig = netInterfaceConfigs.stream()
                .filter(WifiInterfaceConfigImpl.class::isInstance).findFirst();
        if (wifiConfig.isPresent()) {
            WifiInterfaceAddressConfig wifiInterfaceAddressConfig = (WifiInterfaceAddressConfig) ((WifiInterfaceConfigImpl) wifiConfig
                    .get()).getNetInterfaceAddressConfig();
            wifiMode = Optional.of(wifiInterfaceAddressConfig.getMode());
        }
        return wifiMode;
    }
}
