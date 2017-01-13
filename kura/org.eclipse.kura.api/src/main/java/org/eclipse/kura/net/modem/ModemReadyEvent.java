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
public class ModemReadyEvent extends Event {

    /** Topic of the ModemRemovedEvent */
    public static final String MODEM_EVENT_READY_TOPIC = "org/eclipse/kura/net/modem/READY";

    public static final String IMEI = "IMEI";
    public static final String IMSI = "IMSI";
    public static final String ICCID = "ICCID";
    public static final String RSSI = "RSSI";

    public ModemReadyEvent(Map<String, String> properties) {
        super(MODEM_EVENT_READY_TOPIC, properties);
    }
}
