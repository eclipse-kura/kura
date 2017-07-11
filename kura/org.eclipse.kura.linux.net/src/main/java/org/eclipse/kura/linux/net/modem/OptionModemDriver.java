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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionModemDriver extends UsbModemDriver {

    private static final Logger logger = LoggerFactory.getLogger(OptionModemDriver.class);

    private static final String USB_BUS_DRIVERS_PATH = "/sys/bus/usb-serial/drivers/option1/new_id";

    public OptionModemDriver(String vendor, String product) {
        super("option", vendor, product);
    }

    @Override
    public int install() throws Exception {
        int status = super.install();
        if (status == 0) {
            logger.info("submiting {}:{} information to option driver ...", getVendor(), getProduct());
            File newIdFile = new File(USB_BUS_DRIVERS_PATH);
            if (newIdFile.exists()) {
                writeToFile(newIdFile);
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
