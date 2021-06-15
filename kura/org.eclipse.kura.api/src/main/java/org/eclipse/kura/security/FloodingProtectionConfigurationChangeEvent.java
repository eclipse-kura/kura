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
 * Eurotech
 ******************************************************************************/
package org.eclipse.kura.security;

import java.util.Map;

import org.osgi.service.event.Event;

/**
 * @since 3.0
 */
public class FloodingProtectionConfigurationChangeEvent extends Event {

    public static final String FP_EVENT_CONFIG_CHANGE_TOPIC = "org/eclipse/kura/floodingprotection/event/FP_EVENT_CONFIG_CHANGE_TOPIC";

    public FloodingProtectionConfigurationChangeEvent(Map<String, ?> properties) {
        super(FP_EVENT_CONFIG_CHANGE_TOPIC, properties);
    }

}
