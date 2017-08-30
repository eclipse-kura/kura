/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.net.admin.modem.telit.le910v2;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910;
import org.eclipse.kura.net.modem.ModemDevice;
import org.osgi.service.io.ConnectionFactory;

public class TelitLe910v2 extends TelitHe910 implements HspaCellularModem {

    public TelitLe910v2(ModemDevice device, String platform, ConnectionFactory connectionFactory) {
        super(device, platform, connectionFactory);
    }

    @Override
    public void enableGps() throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void disableGps() throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public boolean isGpsEnabled() {
        return false;
    }

    @Override
    public boolean isGpsSupported() throws KuraException {
        // GPS devices attached to the modem are not yet supported
        return false;
    }
}
