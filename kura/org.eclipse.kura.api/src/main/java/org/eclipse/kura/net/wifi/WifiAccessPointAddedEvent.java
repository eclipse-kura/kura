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

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.event.Event;

/**
 * Emitted when a new access point is found by the device.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class WifiAccessPointAddedEvent extends Event {

    /** Topic of the WifiAccessPointAddedEvent */
    public static final String NETWORK_EVENT_ACCESSPOINT_ADDED_TOPIC = "org/eclipse/kura/net/NetworkEvent/AccessPoint/ADDED";

    /** Name of the property to access the network interface name */
    public static final String NETWORK_EVENT_INTERFACE_PROPERTY = "network.interface";

    /** Name of the property to access the access point */
    public static final String NETWORK_EVENT_ACCESS_POINT_PROPERTY = "network.access.point";

    public WifiAccessPointAddedEvent(Map<String, ?> properties) {
        super(NETWORK_EVENT_ACCESSPOINT_ADDED_TOPIC, properties);
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
     * Returns the name of the added access point.
     *
     * @return
     */
    public WifiAccessPoint getAccessPoint() {
        return (WifiAccessPoint) getProperty(NETWORK_EVENT_ACCESS_POINT_PROPERTY);
    }
}
