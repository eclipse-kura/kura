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
package org.eclipse.kura.core.net;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;

public interface NetworkConfigurationVisitor {

    /**
     * This method visits the provided network configuration.
     * Since it typically performs operations on the filesystem or runs commands on the system,
     * it needs a {@link CommandExecutorService}. It has to be set before the visit using the
     * {@link NetworkConfigurationVisitor#setExecutorService}
     * method.
     *
     * @param config
     *            the {@link NetworkConfiguration} used by the visitor
     * @throws KuraException
     */
    public void visit(NetworkConfiguration config) throws KuraException;

    /**
     * Sets the {@link CommandExecutorService} for the visitor. It has to be set before every call of the
     * {@link NetworkConfigurationVisitor#visit} method.
     *
     * @param executorService
     *            the {@link CommandExecutorService} used to perform operations on the system
     */
    public void setExecutorService(CommandExecutorService executorService);
}
