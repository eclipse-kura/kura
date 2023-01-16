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

import org.osgi.service.event.Event;

public class NetworkConfigurationChangeEvent extends Event {

    /** Topic of the NetworkConfigurationChangeEvent */
    public static final String NETWORK_EVENT_CONFIG_CHANGE_TOPIC = "org/eclipse/kura/net/admin/event/NETWORK_EVENT_CONFIG_CHANGE_TOPIC";

    public NetworkConfigurationChangeEvent(Map<String, ?> properties) {
        super(NETWORK_EVENT_CONFIG_CHANGE_TOPIC, properties);
    }
}
