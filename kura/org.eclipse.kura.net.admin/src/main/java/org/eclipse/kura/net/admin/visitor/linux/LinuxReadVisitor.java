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
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;


public class LinuxReadVisitor implements NetworkConfigurationVisitor{

    private static LinuxReadVisitor s_instance;
    
    private List<NetworkConfigurationVisitor> m_visitors;
    
    private LinuxReadVisitor() {
        m_visitors = new ArrayList<NetworkConfigurationVisitor>();
        m_visitors.add(IfcfgConfigReader.getInstance());
        m_visitors.add(WifiConfigReader.getInstance());
        m_visitors.add(PppConfigReader.getInstance());
        m_visitors.add(DhcpConfigReader.getInstance());
        m_visitors.add(FirewallAutoNatConfigReader.getInstance());
    }
    
    public static LinuxReadVisitor getInstance() {
        if(s_instance == null) {
            s_instance = new LinuxReadVisitor();
        }
        
        return s_instance;
    }
    
    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        for(NetworkConfigurationVisitor visitor : m_visitors) {
            visitor.visit(config);
        }
    }

}
