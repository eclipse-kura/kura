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
package org.eclipse.kura.linux.net.wifi;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WifiOptions {

    private static final Logger logger = LoggerFactory.getLogger(WifiOptions.class);

    /**
     * Reports the class name representing this interface.
     */
    public static final String SERVICE_NAME = WifiOptions.class.getName();

    public static final String WIFI_MANAGED_DRIVER_WEXT = "wext";
    public static final String WIFI_MANAGED_DRIVER_HOSTAP = "hostap";
    public static final String WIFI_MANAGED_DRIVER_ATMEL = "atmel";
    public static final String WIFI_MANAGED_DRIVER_WIRED = "wired";
    public static final String WIFI_MANAGED_DRIVER_NL80211 = "nl80211";

    private static final String FAILED_TO_EXECUTE_MSG = "Failed to execute {} ";

    private static Map<String, Collection<String>> wifiOpt = new HashMap<>();
    private final CommandExecutorService executorService;

    public WifiOptions(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    public synchronized Collection<String> getSupportedOptions(String ifaceName) {
        Collection<String> options = wifiOpt.get(ifaceName);
        if (options != null) {
            return options;
        }
        options = new HashSet<>();
        if (isNL80211DriverSupported(ifaceName)) {
            options.add(WIFI_MANAGED_DRIVER_NL80211);
        }
        if (isWextDriverSupported(ifaceName)) {
            options.add(WIFI_MANAGED_DRIVER_WEXT);
        }
        wifiOpt.put(ifaceName, options);
        return options;
    }

    private boolean isNL80211DriverSupported(String ifaceName) {
        if (!LinuxNetworkUtil.toolExists("iw")) {
            return false;
        }
        Command command = new Command(formIwDevInfoCommand(ifaceName));
        command.setTimeout(60);
        CommandStatus status = this.executorService.execute(command);
        return status.getExitStatus().isSuccessful();
    }

    private boolean isWextDriverSupported(String ifaceName) {
        if (!LinuxNetworkUtil.toolExists("iwconfig")) {
            return false;
        }
        boolean ret = false;
        String[] cmd = formIwconfigCommand(ifaceName);
        Command command = new Command(cmd);
        command.setTimeout(60);
        command.setOutputStream(new ByteArrayOutputStream());
        CommandStatus status = this.executorService.execute(command);
        if (!status.getExitStatus().isSuccessful()) {
            if (logger.isWarnEnabled()) {
                logger.warn(FAILED_TO_EXECUTE_MSG, String.join(" ", cmd));
            }
            return ret;
        }
        for (String line : new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8)
                .split("\n")) {
            if (line.contains("IEEE 802.11") || line.contains("Mode:") || line.contains("Access Point:")) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    private static String[] formIwDevInfoCommand(String ifaceName) {
        return new String[] { "iw", "dev", ifaceName, "info" };
    }

    private static String[] formIwconfigCommand(String ifaceName) {
        return new String[] { "iwconfig", ifaceName };
    }
}
