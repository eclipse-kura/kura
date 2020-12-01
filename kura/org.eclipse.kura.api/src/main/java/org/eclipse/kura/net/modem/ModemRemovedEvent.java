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
package org.eclipse.kura.net.modem;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.event.Event;

/**
 * Emitted when a modem is removed from the gateway
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class ModemRemovedEvent extends Event {

    /** Topic of the ModemRemovedEvent */
    public static final String MODEM_EVENT_REMOVED_TOPIC = "org/eclipse/kura/net/modem/REMOVED";

    public ModemRemovedEvent(Map<String, ?> properties) {
        super(MODEM_EVENT_REMOVED_TOPIC, properties);
    }
}
