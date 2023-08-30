/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.admin.ipv6.event;

import java.util.Map;

import org.osgi.service.event.Event;

public class FirewallConfigurationIPv6ChangeEvent extends Event {

    /** Topic of the FirewallConfigurationChangeEvent */
    public static final String FIREWALL_EVENT_CONFIG_CHANGE_TOPIC = "org/eclipse/kura/net/admin/ipv6/event/FIREWALL_IPV6_EVENT_CONFIG_CHANGE_TOPIC";

    public FirewallConfigurationIPv6ChangeEvent(Map<String, ?> properties) {
        super(FIREWALL_EVENT_CONFIG_CHANGE_TOPIC, properties);
    }
}
