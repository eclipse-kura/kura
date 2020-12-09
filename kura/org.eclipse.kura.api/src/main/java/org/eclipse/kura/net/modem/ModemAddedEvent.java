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
 * Emitted when a modem is inserted into the gateway
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class ModemAddedEvent extends Event {

    /** Topic of the ModemAddedEvent */
    public static final String MODEM_EVENT_ADDED_TOPIC = "org/eclipse/kura/net/modem/ADDED";

    ModemDevice modemDevice;

    public ModemAddedEvent(ModemDevice modemDevice) {
        super(MODEM_EVENT_ADDED_TOPIC, (Map<String, ?>) null);

        this.modemDevice = modemDevice;
    }

    public ModemDevice getModemDevice() {
        return this.modemDevice;
    }
}
