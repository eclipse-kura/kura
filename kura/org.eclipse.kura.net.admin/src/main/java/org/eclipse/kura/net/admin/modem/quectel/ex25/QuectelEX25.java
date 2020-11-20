/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.quectel.ex25;

import org.eclipse.kura.net.admin.modem.quectel.generic.QuectelGeneric;
import org.eclipse.kura.net.modem.ModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuectelEX25 extends QuectelGeneric {

    private static final Logger logger = LoggerFactory.getLogger(QuectelEX25.class);
    private static final String MODEM_NOT_AVAILABLE = "Modem not available for AT commands: ";

    public QuectelEX25(ModemDevice device, String platform, ConnectionFactory connectionFactory) {
        super(device, platform, connectionFactory);
    }
}
