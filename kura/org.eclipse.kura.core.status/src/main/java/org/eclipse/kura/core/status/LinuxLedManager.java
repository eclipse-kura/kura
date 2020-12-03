/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.status;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxLedManager implements LedManager {

    private static final Logger logger = LoggerFactory.getLogger(LinuxLedManager.class);

    private final String brightnessPath;

    public LinuxLedManager(String ledPath) {
        this.brightnessPath = ledPath + "/brightness";
    }

    @Override
    public void writeLed(boolean enabled) throws KuraException {
        try (FileWriter ledFileWriter = new FileWriter(this.brightnessPath);
                BufferedWriter ledBufferedWriter = new BufferedWriter(ledFileWriter)) {
            if (enabled) {
                ledBufferedWriter.write("1");
            } else {
                ledBufferedWriter.write("0");
            }
            ledBufferedWriter.flush();
        } catch (IOException e) {
            logger.error("Error accessing the specified LED!");
            throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE);
        }
    }
}
