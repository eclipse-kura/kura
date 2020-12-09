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
package org.eclipse.kura.configuration;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.event.Event;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraNetConfigReadyEvent extends Event {

    /** Topic of the KuraConfigurationReadyEvent */
    public static final String KURA_NET_CONFIG_EVENT_READY_TOPIC = "org/eclipse/kura/configuration/NetConfigEvent/READY";

    public KuraNetConfigReadyEvent(Map<String, ?> properties) {
        super(KURA_NET_CONFIG_EVENT_READY_TOPIC, properties);
    }
}
