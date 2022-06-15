/*******************************************************************************
 * Copyright (c) 2011, 2022 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.executor.CommandExecutorService;

public class WifiConfigWriter implements NetworkConfigurationVisitor {

    private CommandExecutorService executorService;
    private WpaSupplicantConfigWriter wpaSupplicantConfigWriter;
    private HostapdConfigWriter hostapdConfigWriter;

    public WifiConfigWriter() {
        this.wpaSupplicantConfigWriter = new WpaSupplicantConfigWriter();
        this.hostapdConfigWriter = new HostapdConfigWriter();
    }

    @Override
    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {

        this.hostapdConfigWriter.setExecutorService(this.executorService);
        this.hostapdConfigWriter.visit(config);

        this.wpaSupplicantConfigWriter.setExecutorService(this.executorService);
        this.wpaSupplicantConfigWriter.visit(config);

        // After every visit, unset the executorService. This must be set before every call.
        this.executorService = null;
    }

}
