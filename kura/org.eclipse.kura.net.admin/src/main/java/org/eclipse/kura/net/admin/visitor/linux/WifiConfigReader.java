/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WifiConfigReader implements NetworkConfigurationVisitor{

    private static final Logger s_logger = LoggerFactory.getLogger(WifiConfigReader.class);
            
    private static WifiConfigReader s_instance;
    
    private List<NetworkConfigurationVisitor> m_visitors;
    
    private WifiConfigReader() {
        m_visitors = new ArrayList<NetworkConfigurationVisitor>();
        m_visitors.add(WpaSupplicantConfigReader.getInstance());
        m_visitors.add(HostapdConfigReader.getInstance());
    }
    
    public static WifiConfigReader getInstance() {
        if(s_instance == null) {
            s_instance = new WifiConfigReader();
        }
        
        return s_instance;
    }
    
    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config.getNetInterfaceConfigs();
        
        for(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if(netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
                getConfig((WifiInterfaceConfigImpl)netInterfaceConfig);
            }
        }
        
        // Get wpa_supplicant and hostapd configs
        for(NetworkConfigurationVisitor visitor : m_visitors) {
            visitor.visit(config);
        }
    }

    // Get common wifi config
    private void getConfig(WifiInterfaceConfigImpl wifiInterfaceConfig) throws KuraException {
        String interfaceName = wifiInterfaceConfig.getName();
        s_logger.debug("Getting wifi config for " + interfaceName);
        
        List<WifiInterfaceAddressConfig> wifiInterfaceAddressConfigs = wifiInterfaceConfig.getNetInterfaceAddresses();
        
        if(wifiInterfaceAddressConfigs == null || wifiInterfaceAddressConfigs.size() == 0) { 
            wifiInterfaceAddressConfigs = new ArrayList<WifiInterfaceAddressConfig>();
            wifiInterfaceAddressConfigs.add(new WifiInterfaceAddressConfigImpl());
            wifiInterfaceConfig.setNetInterfaceAddresses(wifiInterfaceAddressConfigs);
        }
        
        for(WifiInterfaceAddressConfig wifiInterfaceAddressConfig : wifiInterfaceAddressConfigs) {
            if(wifiInterfaceAddressConfig instanceof WifiInterfaceAddressConfigImpl) {
                StringBuilder wifiModeKey = new StringBuilder("net.interface.").append(interfaceName).append(".config.wifi.mode");
                
                WifiMode wifiMode = WifiMode.UNKNOWN;
                String wifiModeString = KuranetConfig.getProperty(wifiModeKey.toString());
                if(wifiModeString != null) {
                    wifiMode = WifiMode.valueOf(wifiModeString);
                }
                
                s_logger.debug("Got wifiMode: " + wifiMode);
                ((WifiInterfaceAddressConfigImpl) wifiInterfaceAddressConfig).setMode(wifiMode);
            }
        }
    }
}
