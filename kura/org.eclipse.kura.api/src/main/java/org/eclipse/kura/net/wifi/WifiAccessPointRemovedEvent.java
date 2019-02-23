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
package org.eclipse.kura.net.wifi;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.event.Event;

/**
 * Emitted when an access point disappears from view of the device.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class WifiAccessPointRemovedEvent extends Event {

    /** Topic of the WifiAccessPointAddedEvent */
    public static final String NETWORK_EVENT_ACCESSPOINT_REMOVED_TOPIC = "org/eclipse/kura/net/NetworkEvent/AccessPoint/REMOVED";

    /** Name of the property to access the network interface name */
    public static final String NETWORK_EVENT_INTERFACE_PROPERTY = "network.interface";

    /** Name of the property to access the access point */
    public static final String NETWORK_EVENT_ACCESS_POINT_PROPERTY = "network.access.point";

    public WifiAccessPointRemovedEvent(Map<String, ?> properties) {
        super(NETWORK_EVENT_ACCESSPOINT_REMOVED_TOPIC, properties);
    }

    /**
     * Returns the network interface name.
     *
     * @return
     */
    public String getInterfaceName() {
        return (String) getProperty(NETWORK_EVENT_INTERFACE_PROPERTY);
    }

    /**
     * Returns the name of the removed access point.
     *
     * @return
     */
    public WifiAccessPoint getAccessPoint() {
        return (WifiAccessPoint) getProperty(NETWORK_EVENT_ACCESS_POINT_PROPERTY);
    }
}
