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

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.event.Event;

/**
 * An event raised when a network interface has been removed from the system.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class NetInterfaceRemovedEvent extends Event {

    /** Topic of the NetworkInterfaceRemovedEvent */
    public static final String NETWORK_EVENT_INTERFACE_REMOVED_TOPIC = "org/eclipse/kura/net/NetworkEvent/interface/REMOVED";

    /** Name of the property to access the network interface name */
    public static final String NETWORK_EVENT_INTERFACE_PROPERTY = "network.interface";

    public NetInterfaceRemovedEvent(Map<String, ?> properties) {
        super(NETWORK_EVENT_INTERFACE_REMOVED_TOPIC, properties);
    }

    /**
     * Returns the name of the removed interface.
     *
     * @return
     */
    public String getInterfaceName() {
        return (String) getProperty(NETWORK_EVENT_INTERFACE_PROPERTY);
    }
}
