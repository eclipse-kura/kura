/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.linux.net.util;

import java.io.ByteArrayOutputStream;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.net.wifi.WifiInterface.Capability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IwlistCapabilityTool {

    private static final Logger logger = LoggerFactory.getLogger(IwlistCapabilityTool.class);

    private IwlistCapabilityTool() {
    }

    /*
     * Returns an empty capabilities set if the interface is not found or on error
     */
    public static Set<Capability> probeCapabilities(String ifaceName, CommandExecutorService executorService)
            throws KuraException {
        Set<Capability> capabilities = EnumSet.noneOf(Capability.class);

        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return capabilities;
        }

        String[] cmd = { "iwlist", ifaceName, "auth" };
        Command command = new Command(cmd);
        command.setTimeout(60);
        command.setOutputStream(new ByteArrayOutputStream());
        CommandStatus status = executorService.execute(command);
        int exitValue = status.getExitStatus().getExitCode();
        if (!status.getExitStatus().isSuccessful()) {
            logger.warn("error executing command --- iwlist --- exit value = {}", exitValue);
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, String.join(" ", cmd), exitValue);
        }

        // get the output
        getWifiCapabilitiesParse(capabilities,
                new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));
        return capabilities;
    }

    private static void getWifiCapabilitiesParse(Set<Capability> capabilities, String commandOutput) {
        for (String line : commandOutput.split("\n")) {
            // Remove all whitespace
            String cleanLine = line.replaceAll("\\s", "");

            if ("WPA".equals(cleanLine)) {
                capabilities.add(Capability.WPA);
            } else if ("WPA2".equals(cleanLine)) {
                capabilities.add(Capability.RSN);
            } else if ("CIPHER-TKIP".equals(cleanLine)) {
                capabilities.add(Capability.CIPHER_TKIP);
            } else if ("CIPHER-CCMP".equals(cleanLine)) {
                capabilities.add(Capability.CIPHER_CCMP);
                // TODO: WEP options don't always seem to be displayed?
            } else if ("WEP-104".equals(cleanLine)) {
                capabilities.add(Capability.CIPHER_WEP104);
            } else if ("WEP-40".equals(cleanLine)) {
                capabilities.add(Capability.CIPHER_WEP40);
            }
        }
    }
}
