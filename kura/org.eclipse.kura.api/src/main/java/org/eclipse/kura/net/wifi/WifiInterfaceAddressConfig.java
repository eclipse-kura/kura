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
package org.eclipse.kura.net.wifi;

import java.util.List;

import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Contains both the wifi interface status as well as all current configurations
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface WifiInterfaceAddressConfig extends WifiInterfaceAddress, NetInterfaceAddressConfig {

    /**
     * Returns a List of NetConfig Objects associated with a given WifiInterfaceAddressConfig
     * for a given WifiInterface
     *
     * @return the NetConfig Objects associated with the WifiInterfaceAddressConfig
     */
    @Override
    public List<NetConfig> getConfigs();
}
