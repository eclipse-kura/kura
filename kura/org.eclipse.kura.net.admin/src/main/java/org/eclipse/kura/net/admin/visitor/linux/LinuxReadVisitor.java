/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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

public class LinuxReadVisitor implements NetworkConfigurationVisitor {

    private static LinuxReadVisitor instance;
    private CommandExecutorService executorService;

    private final List<NetworkConfigurationVisitor> visitors;

    private LinuxReadVisitor() {
        this.visitors = new ArrayList<>();
        this.visitors.add(IfcfgConfigReader.getInstance());
        this.visitors.add(WifiConfigReader.getInstance());
        this.visitors.add(PppConfigReader.getInstance());
        this.visitors.add(DhcpConfigReader.getInstance());
        this.visitors.add(FirewallAutoNatConfigReader.getInstance());
    }

    public static LinuxReadVisitor getInstance() {
        if (instance == null) {
            instance = new LinuxReadVisitor();
        }

        return instance;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        for (NetworkConfigurationVisitor visitor : this.visitors) {
            visitor.setExecutorService(this.executorService);
            visitor.visit(config);
        }
    }

    @Override
    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

}
