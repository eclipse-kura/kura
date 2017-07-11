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
package org.eclipse.kura.linux.net.wifi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
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

    private static Map<String, Collection<String>> wifiOpt = new HashMap<>();

    private static final String FAILED_TO_EXECUTE_MSG = "Failed to execute {} ";

    private WifiOptions() {
    }

    public static Collection<String> getSupportedOptions(String ifaceName) throws KuraException {
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

    private static boolean isNL80211DriverSupported(String ifaceName) {
        if (!LinuxNetworkUtil.toolExists("iw")) {
            return false;
        }
        boolean ret = false;
        SafeProcess procIw = null;
        try {
            procIw = ProcessUtil.exec(formIwDevInfoCommand(ifaceName));
            if (procIw != null) {
                int status = procIw.waitFor();
                if (status == 0) {
                    ret = true;
                }
            }
        } catch (Exception e) {
            logger.warn(FAILED_TO_EXECUTE_MSG, formIwDevInfoCommand(ifaceName), e);
        } finally {
            if (procIw != null) {
                ProcessUtil.destroy(procIw);
            }
        }

        return ret;
    }

    private static boolean isWextDriverSupported(String ifaceName) {
        if (!LinuxNetworkUtil.toolExists("iwconfig")) {
            return false;
        }
        boolean ret = false;
        SafeProcess proc = null;
        try {
            proc = ProcessUtil.exec(formIwconfigCommand(ifaceName));
            if (proc.waitFor() != 0) {
                logger.warn(FAILED_TO_EXECUTE_MSG, formIwconfigCommand(ifaceName));
                ProcessUtil.destroy(proc);
                return ret;
            }
        } catch (Exception e) {
            logger.warn(FAILED_TO_EXECUTE_MSG, formIwconfigCommand(ifaceName), e);
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
            return false;
        }
        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("IEEE 802.11") || line.contains("Mode:") || line.contains("Access Point:")) {
                    ret = true;
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn(FAILED_TO_EXECUTE_MSG, formIwDevInfoCommand(ifaceName), e);
        } finally {
            ProcessUtil.destroy(proc);
        }
        return ret;
    }

    private static String formIwDevInfoCommand(String ifaceName) {
        StringBuilder sb = new StringBuilder("iw dev ");
        sb.append(ifaceName).append(" info");

        return sb.toString();
    }

    private static String formIwconfigCommand(String ifaceName) {
        StringBuilder sb = new StringBuilder("iwconfig ");
        sb.append(ifaceName);
        return sb.toString();
    }
}
