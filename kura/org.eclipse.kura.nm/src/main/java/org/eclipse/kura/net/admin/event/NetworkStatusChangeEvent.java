/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
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
import java.util.Objects;

import org.eclipse.kura.net.admin.monitor.InterfaceState;
import org.osgi.service.event.Event;

public class NetworkStatusChangeEvent extends Event {

    /** Topic of the NetworkStatusChangeEvent */
    public static final String NETWORK_EVENT_STATUS_CHANGE_TOPIC = "org/eclipse/kura/net/admin/event/NETWORK_EVENT_STATUS_CHANGE_TOPIC";

    private final InterfaceState interfaceState;

    public NetworkStatusChangeEvent(InterfaceState ifaceState, Map<String, ?> properties) {
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(this.interfaceState);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj) || (getClass() != obj.getClass())) {
            return false;
        }
        NetworkStatusChangeEvent other = (NetworkStatusChangeEvent) obj;
        return Objects.equals(this.interfaceState, other.interfaceState);
    }

}
