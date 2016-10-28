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
package org.eclipse.kura.net;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Extends a NetInterfaceAddress which is status as currently running on the system with the
 * interface's configuration in the form of a List of NetConfig objects. The configuration
 * and the status may differ based on environmental conditions and this is why configuration
 * is modeled separately. For example, an interface could be configured as a DHCP client.
 * In this case, the configuration would not include an IP address. However, the 'status' in
 * the NetInterfaceAddress would because the interface does have an IP - just not one that is
 * configured because it is dynamically assigned by the DHCP server.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface NetInterfaceAddressConfig extends NetInterfaceAddress {

    /**
     * Returns a List of NetConfig Objects associated with a given NetInterfaceAddressConfig
     * for a given NetInterface
     *
     * @return the NetConfig Objects associated with the NetInterfaceAddressConfig
     */
    public List<NetConfig> getConfigs();
}
