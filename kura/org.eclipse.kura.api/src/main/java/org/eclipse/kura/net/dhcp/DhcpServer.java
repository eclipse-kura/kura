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
 ******************************************************************************/
package org.eclipse.kura.net.dhcp;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a DHCP server.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@FunctionalInterface
@ProviderType
public interface DhcpServer {

    /**
     * Returns whether or not the DhcpServer is actively running or not
     *
     * @return a boolean denoting whether or not the DhcpServer is running or not
     * @throws KuraException
     */
    public boolean isRunning() throws KuraException;
}
