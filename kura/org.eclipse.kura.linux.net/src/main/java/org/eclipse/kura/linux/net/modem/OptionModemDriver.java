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
 *******************************************************************************/
package org.eclipse.kura.linux.net.modem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.kura.executor.CommandExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionModemDriver extends UsbModemDriver {

    private static final Logger logger = LoggerFactory.getLogger(OptionModemDriver.class);

    private static final String USB_BUS_DRIVERS_PATH = "/sys/bus/usb-serial/drivers/option1/new_id";

    public OptionModemDriver(String vendor, String product) {
        super("option", vendor, product);
    }

    @Override
    public int install(CommandExecutorService executorService) {
        int status = super.install(executorService);
        if (status == 0) {
            logger.info("submiting {}:{} information to option driver ...", getVendor(), getProduct());
            File newIdFile = new File(USB_BUS_DRIVERS_PATH);
            if (newIdFile.exists()) {
                try {
                    writeToFile(newIdFile);
                } catch (IOException e) {
                    logger.error("Failed to write options on file " + USB_BUS_DRIVERS_PATH, e);
                }
            }
        }
        return status;
    }

    private void writeToFile(File newIdFile) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(getVendor());
        sb.append(' ');
        sb.append(getProduct());

        try (FileOutputStream fos = new FileOutputStream(newIdFile); PrintWriter pw = new PrintWriter(fos)) {
            pw.write(sb.toString());
            pw.flush();
        }
    }
}
