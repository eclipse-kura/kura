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
package org.eclipse.kura.linux.bluetooth.le.beacon;

import org.eclipse.kura.bluetooth.BluetoothBeaconCommandListener;
import org.eclipse.kura.linux.bluetooth.util.BluetoothProcessListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothConfigurationProcessListener implements BluetoothProcessListener {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothConfigurationProcessListener.class);

    private BluetoothBeaconCommandListener listener = null;

    public BluetoothConfigurationProcessListener(BluetoothBeaconCommandListener listener) {
        this.listener = listener;
    }

    // --------------------------------------------------------------------
    //
    // BluetoothProcessListener API
    //
    // --------------------------------------------------------------------
    @Override
    public void processInputStream(String string) {

        // Check if the command succedeed and return the last line
        logger.debug("Command response : {}", string);
        String[] lines = string.split("\n");
        if (lines[0].toLowerCase().contains("usage")) {
            logger.info("Command failed. Error in command syntax.");
            this.listener.onCommandFailed(null);
        } else {
            String lastLine = lines[lines.length - 1];

            // The last line of hcitool cmd return contains:
            // the numbers of packets sent (1 byte)
            // the opcode (2 bytes)
            // the exit code (1 byte)
            // the returned data if any
            String exitCode = lastLine.substring(11, 13);

            if (exitCode.equals("00")) {
                logger.info("Command " + lines[0].substring(15, 35) + " Succeeded.");
                this.listener.onCommandResults(lastLine);
            } else {
                logger.info("Command " + lines[0].substring(15, 35) + " failed. Error " + exitCode);
                this.listener.onCommandFailed(exitCode);
            }
        }

    }

    @Override
    public void processInputStream(int ch) {
    }

    @Override
    public void processErrorStream(String string) {
    }

}
