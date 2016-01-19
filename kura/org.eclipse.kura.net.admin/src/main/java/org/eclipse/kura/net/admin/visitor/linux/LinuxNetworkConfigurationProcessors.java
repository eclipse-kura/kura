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

import org.eclipse.kura.core.net.NetworkConfigurationReader;
import org.eclipse.kura.core.net.NetworkConfigurationWriter;


public class LinuxNetworkConfigurationProcessors {

    public static List<NetworkConfigurationReader> getReaders() {
    	List<NetworkConfigurationReader> readers = new ArrayList<NetworkConfigurationReader>();
    	readers.add(IfcfgConfigReader.getInstance());
    	readers.add(WifiConfigReader.getInstance());
    	readers.add(PppConfigReader.getInstance());
    	readers.add(DhcpConfigReader.getInstance());
    	readers.add(FirewallAutoNatConfigReader.getInstance());
        
        return readers;
    }
    
    public static List<NetworkConfigurationWriter> getWriters() {
    	List<NetworkConfigurationWriter> writers = new ArrayList<NetworkConfigurationWriter>();
    	writers.add(IfcfgConfigWriter.getInstance());
    	writers.add(WifiConfigWriter.getInstance());
    	writers.add(PppConfigWriter.getInstance());
    	writers.add(DhcpConfigWriter.getInstance());
    	writers.add(FirewallAutoNatConfigWriter.getInstance());
    	
        return writers;
    }
}
