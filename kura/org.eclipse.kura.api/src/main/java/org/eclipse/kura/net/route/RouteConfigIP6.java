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
package org.eclipse.kura.net.route;

import org.eclipse.kura.net.IP6Address;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Implementation of IPv6 route configurations
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class RouteConfigIP6 extends RouteConfigIP<IP6Address> implements RouteConfig6 {

    public RouteConfigIP6(IP6Address destination, IP6Address gateway, IP6Address netmask, String interfaceName,
            int metric) {
        super(destination, gateway, netmask, interfaceName, metric);
    }

    @Override
    public boolean equals(RouteConfig r) {
        // TODO Auto-generated method stub
        return false;
    }
}
