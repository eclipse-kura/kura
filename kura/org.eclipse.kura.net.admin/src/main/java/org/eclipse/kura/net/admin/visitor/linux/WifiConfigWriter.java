/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.net.admin.visitor.linux;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationWriter;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WifiConfigWriter implements NetworkConfigurationWriter{

    private static final Logger s_logger = LoggerFactory.getLogger(WifiConfigWriter.class);
            
    private static WifiConfigWriter s_instance;
    
    private List<NetworkConfigurationWriter> m_visitors;
    
    private WifiConfigWriter() {
        m_visitors = new ArrayList<NetworkConfigurationWriter>();
        m_visitors.add(WpaSupplicantConfigWriter.getInstance());
        m_visitors.add(HostapdConfigWriter.getInstance());
    }
    
    public static WifiConfigWriter getInstance() {
        if(s_instance == null) {
            s_instance = new WifiConfigWriter();
        }
        
        return s_instance;
    }
    
    @Override
    public void write(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config.getModifiedNetInterfaceConfigs();
        
        for(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if(netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
                writeConfig((WifiInterfaceConfigImpl)netInterfaceConfig);
            }
        }
        
        // Write wpa_supplicant and hostapd configs
        for(NetworkConfigurationWriter visitor : m_visitors) {
            visitor.write(config);
        }
    }

    // Write common wifi config
    private void writeConfig(WifiInterfaceConfigImpl wifiInterfaceConfig) throws KuraException {
        String interfaceName = wifiInterfaceConfig.getName();
        s_logger.debug("Writing wifi config for " + interfaceName);
        
        List<WifiInterfaceAddressConfig> wifiInterfaceAddressConfigs = wifiInterfaceConfig.getNetInterfaceAddresses();
        
        if(wifiInterfaceAddressConfigs != null) { 
            for(WifiInterfaceAddressConfig wifiInterfaceAddressConfig : wifiInterfaceAddressConfigs) {
                // Store the selected wifi mode
                WifiMode wifiMode = wifiInterfaceAddressConfig.getMode();

                s_logger.debug("Store wifiMode: " + wifiMode);
                StringBuilder key = new StringBuilder("net.interface." + interfaceName + ".config.wifi.mode");
                try {
                    KuranetConfig.setProperty(key.toString(), wifiMode.toString());
                } catch (Exception e) {
                    s_logger.error("Failed to save kuranet config", e);
                    throw KuraException.internalError(e);
                }
            }
        }
    }
}
