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
 * Event raised when the state of the network has changed.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class NetworkStateChangedEvent extends Event {

    /** Topic of the NetworkStateChangedEvent */
    public static final String NETWORK_EVENT_STATE_CHANGED_TOPIC = "org/eclipse/kura/net/NetworkEvent/STATE_CHANGED";

    /** Name of the property to access the new network state */
    public static final String NETWORK_EVENT_NEW_STATE_PROPERTY = "network.state";

    public NetworkStateChangedEvent(Map<String, ?> properties) {
        super(NETWORK_EVENT_STATE_CHANGED_TOPIC, properties);
    }

    /**
     * Returns the new network state.
     *
     * @return
     */
    public NetworkState getState() {
        return (NetworkState) getProperty(NETWORK_EVENT_NEW_STATE_PROPERTY);
    }
}
