/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.linux.net.modem;

import java.util.Map;

import org.osgi.service.event.Event;

public class SerialModemAddedEvent extends Event {

    public static final String SERIAL_MODEM_EVENT_ADDED_TOPIC = "org/eclipse/kura/net/modem/serial-modem/ADDED";

    private final SupportedSerialModemInfo supportedSerialModemInfo;

    public SerialModemAddedEvent(SupportedSerialModemInfo supportedSerialModemInfo) {
        super(SERIAL_MODEM_EVENT_ADDED_TOPIC, (Map<String, ?>) null);
        this.supportedSerialModemInfo = supportedSerialModemInfo;
    }

    public SupportedSerialModemInfo getSupportedSerialModemInfo() {
        return this.supportedSerialModemInfo;
    }
}
