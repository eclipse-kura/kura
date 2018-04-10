/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.usb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsbTtyInterface {

    private static final Logger logger = LoggerFactory.getLogger(UsbTtyInterface.class);

    private UsbTtyInterface() {
        // Empty constructor
    }

    public static Integer getInterfaceNumber(String deviceNode) {
        Integer interfaceNumber = null;
        String command = "udevadm info --query=property --name=" + deviceNode;
        logger.debug("Executing: {}", command);
        try {
            return parse(execute(command));
        } catch (IOException e) {
            logger.error("Failed to execute command {}", command, e);
        }
        return interfaceNumber;
    }

    private static Integer parse(List<String> output) {
        Integer interfaceNumber = null;
        for (String line : output) {
            if (line.startsWith("ID_USB_INTERFACE_NUM")) {
                interfaceNumber = Integer.parseInt(line.split("=")[1], 16);
                break;
            }
        }
        return interfaceNumber;
    }

    private static List<String> execute(final String command) throws IOException {
        final DefaultExecutor executor = new DefaultExecutor();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        executor.setStreamHandler(new PumpStreamHandler(out));
        executor.execute(CommandLine.parse(command));

        return IOUtils.readLines(new ByteArrayInputStream(out.toByteArray()), Charset.defaultCharset());
    }
}
