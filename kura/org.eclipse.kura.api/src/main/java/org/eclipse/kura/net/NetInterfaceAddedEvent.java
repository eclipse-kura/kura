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
package org.eclipse.kura.net;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.event.Event;

/**
 * An event raised when a new network interface has been added to the system.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class NetInterfaceAddedEvent extends Event {

    /** Topic of the NetworkInterfaceAddedEvent */
    public static final String NETWORK_EVENT_INTERFACE_ADDED_TOPIC = "org/eclipse/kura/net/NetworkEvent/interface/ADDED";

    /** Name of the property to access the network interface name */
    public static final String NETWORK_EVENT_INTERFACE_PROPERTY = "network.interface";

    public NetInterfaceAddedEvent(Map<String, ?> properties) {
        super(NETWORK_EVENT_INTERFACE_ADDED_TOPIC, properties);
    }

    /**
     * Returns the name of the added interface.
     *
     * @return
     */
    public String getInterfaceName() {
        return (String) getProperty(NETWORK_EVENT_INTERFACE_PROPERTY);
    }
}
