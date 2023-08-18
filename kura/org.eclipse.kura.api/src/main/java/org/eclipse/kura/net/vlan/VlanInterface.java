/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Areti
 ******************************************************************************/
package org.eclipse.kura.net.vlan;

import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Network interface for Vlans.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface VlanInterface<T extends NetInterfaceAddress> extends NetInterface<T> {

    /**
     * Indicates Vlan configuration flag.
     *
     * @return
     */
    public int getFlags();
    
    /**
     * Indicates the underlying physical interface to the Vlan.
     *
     * @return
     */
    public String getParentInterface();
    
    /**
     * Indicates the configured Vlan tag.
     *
     * @return
     */
    public int getVlanId();
     
}
