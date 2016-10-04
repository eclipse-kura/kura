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
package org.eclipse.kura.net.admin.modem.sierra.usb598;

import org.eclipse.kura.net.admin.modem.ModemPppConfigGenerator;
import org.eclipse.kura.net.admin.modem.PppPeer;
import org.eclipse.kura.net.admin.visitor.linux.util.ModemXchangeScript;
import org.eclipse.kura.net.modem.ModemConfig;

public class SierraUsb598ConfigGenerator implements ModemPppConfigGenerator {

    @Override
    public PppPeer getPppPeer(String deviceId, ModemConfig modemConfig, String logFile, String connectScript,
            String disconnectScript) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ModemXchangeScript getConnectScript(ModemConfig modemConfig) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ModemXchangeScript getDisconnectScript(ModemConfig modemConfig) {
        // TODO Auto-generated method stub
        return null;
    }

}
