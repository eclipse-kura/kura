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
package org.eclipse.kura.net.modem;

import java.util.List;

import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Modem Interface Address Config represents the modem status and the modem configuration
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface ModemInterfaceAddressConfig extends ModemInterfaceAddress, NetInterfaceAddressConfig {

    /**
     * Returns a List of NetConfig Objects associated with a given ModemInterfaceAddressConfig
     * for a given ModemInterface
     *
     * @return the NetConfig Objects associated with the ModemInterfaceAddressConfig
     */
    @Override
    public List<NetConfig> getConfigs();
}
