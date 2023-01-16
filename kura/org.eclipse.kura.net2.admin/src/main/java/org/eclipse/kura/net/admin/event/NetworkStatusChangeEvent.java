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
 *******************************************************************************/
package org.eclipse.kura.net.admin.event;

import java.util.Map;

import org.eclipse.kura.net.admin.monitor.InterfaceState;
import org.osgi.service.event.Event;

public class NetworkStatusChangeEvent extends Event {

    /** Topic of the NetworkStatusChangeEvent */
    public static final String NETWORK_EVENT_STATUS_CHANGE_TOPIC = "org/eclipse/kura/net/admin/event/NETWORK_EVENT_STATUS_CHANGE_TOPIC";

    InterfaceState interfaceState;

    public NetworkStatusChangeEvent(String interfaceName, InterfaceState ifaceState, Map<String, ?> properties) {
        super(NETWORK_EVENT_STATUS_CHANGE_TOPIC, properties);

        this.interfaceState = ifaceState;
    }

    /**
     * Returns interface state
     *
     * @return
     */
    public InterfaceState getInterfaceState() {
        return this.interfaceState;
    }
}
