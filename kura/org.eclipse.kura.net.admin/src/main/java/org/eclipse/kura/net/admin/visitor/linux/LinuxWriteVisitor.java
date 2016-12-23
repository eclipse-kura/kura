/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;

public class LinuxWriteVisitor implements NetworkConfigurationVisitor {

    private static LinuxWriteVisitor s_instance;

    private final List<NetworkConfigurationVisitor> m_visitors;

    private LinuxWriteVisitor() {
        this.m_visitors = new ArrayList<NetworkConfigurationVisitor>();
        this.m_visitors.add(IfcfgConfigWriter.getInstance());
        this.m_visitors.add(WifiConfigWriter.getInstance());
        this.m_visitors.add(PppConfigWriter.getInstance());
        this.m_visitors.add(DhcpConfigWriter.getInstance());
        this.m_visitors.add(FirewallAutoNatConfigWriter.getInstance());
    }

    public static LinuxWriteVisitor getInstance() {
        if (s_instance == null) {
            s_instance = new LinuxWriteVisitor();
        }

        return s_instance;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        for (NetworkConfigurationVisitor visitor : this.m_visitors) {
            visitor.visit(config);
        }
    }
}
