/*******************************************************************************
 * Copyright (c) 2016 Eurotech and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
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
