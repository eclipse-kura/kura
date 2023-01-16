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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.executor.CommandExecutorService;

public class LinuxWriteVisitor implements NetworkConfigurationVisitor {

    private static LinuxWriteVisitor instance;
    private CommandExecutorService executorService;

    private final List<NetworkConfigurationVisitor> visitors;

    private LinuxWriteVisitor() {
        this.visitors = new ArrayList<>();
        this.visitors.add(new IfcfgConfigWriter());
        this.visitors.add(new WifiConfigWriter());
        this.visitors.add(new PppConfigWriter());
        this.visitors.add(new DhcpConfigWriter());
        this.visitors.add(new FirewallAutoNatConfigWriter());
    }

    public static LinuxWriteVisitor getInstance() {
        if (instance == null) {
            instance = new LinuxWriteVisitor();
        }

        return instance;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        for (NetworkConfigurationVisitor visitor : this.visitors) {
            visitor.setExecutorService(this.executorService);
            visitor.visit(config);
        }

        // After every visit, unset the executorService. This must be set before every call.
        this.executorService = null;
    }

    @Override
    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }
}
