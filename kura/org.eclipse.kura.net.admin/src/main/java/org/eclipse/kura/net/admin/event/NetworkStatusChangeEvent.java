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
package org.eclipse.kura.net.admin.event;

import java.util.Map;

import org.eclipse.kura.net.admin.monitor.InterfaceState;
import org.osgi.service.event.Event;

public class NetworkStatusChangeEvent extends Event {

    /** Topic of the NetworkStatusChangeEvent */
    public static final String NETWORK_EVENT_STATUS_CHANGE_TOPIC = "org/eclipse/kura/net/admin/event/NETWORK_EVENT_STATUS_CHANGE_TOPIC";

    InterfaceState m_interfaceState;

    public NetworkStatusChangeEvent(String interfaceName, InterfaceState ifaceState, Map<String, ?> properties) {
        super(NETWORK_EVENT_STATUS_CHANGE_TOPIC, properties);

        this.m_interfaceState = ifaceState;
    }

    /**
     * Returns interface state
     *
     * @return
     */
    public InterfaceState getInterfaceState() {
        return this.m_interfaceState;
    }
}
