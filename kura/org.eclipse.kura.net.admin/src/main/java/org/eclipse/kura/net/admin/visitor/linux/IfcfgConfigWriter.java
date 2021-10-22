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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.internal.linux.net.NetInterfaceConfigSerializationService;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfcfgConfigWriter implements NetworkConfigurationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(IfcfgConfigWriter.class);

    private NetInterfaceConfigSerializationService netConfigManager; // can be null

    public IfcfgConfigWriter() {
        BundleContext context = FrameworkUtil.getBundle(IfcfgConfigWriter.class).getBundleContext();
        ServiceReference<NetInterfaceConfigSerializationService> netConfigManagerSR = context
                .getServiceReference(NetInterfaceConfigSerializationService.class);
        this.netConfigManager = context.getService(netConfigManagerSR);
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
            if (netInterfaceConfig != null && ((AbstractNetInterface<?>) netInterfaceConfig).isInterfaceManaged()) {
                writeConfig(netInterfaceConfig);
            }
        }
    }

    private void writeConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {

        NetInterfaceType type = netInterfaceConfig.getType();
        if (type != NetInterfaceType.ETHERNET && type != NetInterfaceType.WIFI && type != NetInterfaceType.LOOPBACK) {
            logger.info("writeConfig() :: Cannot write configuration file for this type of interface - {}", type);
            return;
        }
        netConfigManager.write(netInterfaceConfig);

    }

}
