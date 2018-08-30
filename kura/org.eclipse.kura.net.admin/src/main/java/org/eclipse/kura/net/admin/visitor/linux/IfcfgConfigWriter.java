/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.internal.linux.net.NetInterfaceConfigSerializationService;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfcfgConfigWriter implements NetworkConfigurationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(IfcfgConfigWriter.class);

    private static IfcfgConfigWriter instance;

    private static NetInterfaceConfigSerializationService netConfigManager; // TODO: can be null

    public static synchronized IfcfgConfigWriter getInstance() {
        if (instance == null) {
            instance = new IfcfgConfigWriter();
            BundleContext context = FrameworkUtil.getBundle(IfcfgConfigWriter.class).getBundleContext();
            ServiceReference<NetInterfaceConfigSerializationService> netConfigManagerSR = context.getServiceReference(NetInterfaceConfigSerializationService.class);
            netConfigManager = context.getService(netConfigManagerSR);
        }

        return instance;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getModifiedNetInterfaceConfigs();

        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if (netInterfaceConfig != null) {
                if (((AbstractNetInterface<?>) netInterfaceConfig).isInterfaceManaged()) {
                    writeConfig(netInterfaceConfig);
                }
                writeKuraExtendedConfig(netInterfaceConfig);
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

    protected Properties getKuranetProperties() {
        return KuranetConfig.getProperties();
    }

    public static void writeKuraExtendedConfig(
            NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {

        NetInterfaceStatus netInterfaceStatus = ((AbstractNetInterface<?>) netInterfaceConfig).getInterfaceStatus();
        logger.debug("Setting NetInterfaceStatus to {} for {}", netInterfaceStatus, netInterfaceConfig.getName());

        // set it all
        Properties kuraExtendedProps = getInstance().getKuranetProperties();

        // write it
        if (!kuraExtendedProps.isEmpty()) {
            StringBuilder sb = new StringBuilder().append("net.interface.").append(netInterfaceConfig.getName())
                    .append(".config.ip4.status");
            kuraExtendedProps.put(sb.toString(), netInterfaceStatus.toString());
            try {
                KuranetConfig.storeProperties(kuraExtendedProps);
            } catch (IOException e) {
                logger.error("Failed to store properties in the kuranet.conf file.", e);
                throw KuraException.internalError(e.getMessage());
            }
        }
    }

    public static void removeKuraExtendedConfig(String interfaceName) throws KuraException {
        try {
            StringBuilder sb = new StringBuilder().append("net.interface.").append(interfaceName)
                    .append(".config.ip4.status");
            KuranetConfig.deleteProperty(sb.toString());
        } catch (IOException e) {
            logger.error("Failed to remove net.interface..config.ip4.status property from the kuranet.conf file.", e);
            throw KuraException.internalError(e.getMessage());
        }
    }

}
