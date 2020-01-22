/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
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

    ModemDevice m_modemDevice;

    public ModemAddedEvent(ModemDevice modemDevice) {
        super(MODEM_EVENT_ADDED_TOPIC, (Map<String, ?>) null);

        this.m_modemDevice = modemDevice;
    }

    public ModemDevice getModemDevice() {
        return this.m_modemDevice;
    }
}
