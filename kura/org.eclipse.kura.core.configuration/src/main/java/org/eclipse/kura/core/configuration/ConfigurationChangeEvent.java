/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.configuration;

import java.util.Map;

import org.osgi.service.event.Event;

public class ConfigurationChangeEvent extends Event {

    public static final String CONF_CHANGE_EVENT_TOPIC = "org/eclipse/kura/core/configuration/event/CONF_CHANGE_EVENT_TOPIC";
    public static final String CONF_CHANGE_EVENT_SESSION_PROP = "session";
    public static final String CONF_CHANGE_EVENT_PID_PROP = "pid";

    public ConfigurationChangeEvent(Map<String, ?> properties) {
        super(CONF_CHANGE_EVENT_TOPIC, properties);
    }

}
