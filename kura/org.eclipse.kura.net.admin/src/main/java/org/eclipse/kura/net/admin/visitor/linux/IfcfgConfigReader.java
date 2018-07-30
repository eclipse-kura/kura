/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Benjamin Cabé - fix for GH issue #299
 *******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux;

import java.util.List;
import java.util.Properties;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.net.NetConfigManager;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class IfcfgConfigReader implements NetworkConfigurationVisitor {

    private static IfcfgConfigReader instance;

    private static NetConfigManager netConfigManager; // TODO: can be null

    public static IfcfgConfigReader getInstance() {
        if (instance == null) {
            instance = new IfcfgConfigReader();
            BundleContext context = FrameworkUtil.getBundle(IfcfgConfigWriter.class).getBundleContext();
            ServiceReference<NetConfigManager> netConfigManagerSR = context.getServiceReference(NetConfigManager.class);
            netConfigManager = context.getService(netConfigManagerSR);
        }
        return instance;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getNetInterfaceConfigs();

        Properties kuraExtendedProps = getKuranetProperties();

        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            netConfigManager.readConfig(netInterfaceConfig, kuraExtendedProps);
        }
    }

    protected Properties getKuranetProperties() {
        return KuranetConfig.getProperties();
    }
}
