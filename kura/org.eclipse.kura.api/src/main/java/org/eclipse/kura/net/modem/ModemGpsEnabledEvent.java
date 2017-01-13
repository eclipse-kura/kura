/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class ModemGpsEnabledEvent extends Event {

    /** Topic of the ModemGpsEnabledEvent */
    public static final String MODEM_EVENT_GPS_ENABLED_TOPIC = "org/eclipse/kura/net/modem/gps/ENABLED";

    public static final String Port = "port";
    public static final String BaudRate = "baudRate";
    public static final String DataBits = "bitsPerWord";
    public static final String StopBits = "stopBits";
    public static final String Parity = "parity";

    public ModemGpsEnabledEvent(Map<String, Object> properties) {
        super(MODEM_EVENT_GPS_ENABLED_TOPIC, properties);
    }
}
