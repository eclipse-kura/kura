/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
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

public class FirewallConfigurationChangeEvent extends Event {

    /** Topic of the FirewallConfigurationChangeEvent */
    public static final String FIREWALL_EVENT_CONFIG_CHANGE_TOPIC = "org/eclipse/kura/net/admin/event/FIREWALL_EVENT_CONFIG_CHANGE_TOPIC";

    public FirewallConfigurationChangeEvent(Map<String, ?> properties) {
        super(FIREWALL_EVENT_CONFIG_CHANGE_TOPIC, properties);
    }
}
