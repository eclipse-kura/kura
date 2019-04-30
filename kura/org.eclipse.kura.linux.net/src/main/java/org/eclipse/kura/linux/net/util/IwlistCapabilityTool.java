/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.linux.net.util;

import static org.eclipse.kura.linux.net.util.LinuxNetworkUtil.formFailedCommandMessage;
import static org.eclipse.kura.linux.net.util.LinuxNetworkUtil.formInterruptedCommandMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.EnumSet;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
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
    public static Set<Capability> probeCapabilities(String ifaceName) throws KuraException {
        Set<Capability> capabilities = EnumSet.noneOf(Capability.class);

        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return capabilities;
        }

        SafeProcess proc = null;
        String cmd = "iwlist " + ifaceName + " auth";
        try {
            // start the process
            proc = ProcessUtil.exec(cmd);
            if (proc.waitFor() != 0) {
                logger.warn("error executing command --- iwlist --- exit value = {}", proc.exitValue());
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, cmd, proc.exitValue());
            }

            // get the output
            getWifiCapabilitiesParse(cmd, capabilities, proc);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formInterruptedCommandMessage(cmd));
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
        return capabilities;
    }

    private static void getWifiCapabilitiesParse(String cmd, Set<Capability> capabilities, SafeProcess proc)
            throws KuraException {

        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            String line = null;
            while ((line = br.readLine()) != null) {
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
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        }
    }
}
