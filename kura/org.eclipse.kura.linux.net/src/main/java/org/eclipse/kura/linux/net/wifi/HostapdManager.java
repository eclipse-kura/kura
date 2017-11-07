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

import java.io.File;
import java.util.Collection;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes Hostapd Manager
 * 
 * @author ibinshtok
 *
 */
public class HostapdManager {

    private static Logger logger = LoggerFactory.getLogger(HostapdManager.class);

    private static final String OS_VERSION = System.getProperty("kura.os.version");
    private static final String HOSTAPD_EXEC = "hostapd";

    private static boolean isIntelEdison = false;

    static {
        StringBuilder sb = new StringBuilder(KuraConstants.Intel_Edison.getImageName());
        sb.append('_').append(KuraConstants.Intel_Edison.getImageVersion()).append('_')
                .append(KuraConstants.Intel_Edison.getTargetName());
        if (OS_VERSION.equals(sb.toString())) {
            isIntelEdison = true;
        }
    }

    private HostapdManager() {
    }

    public static void start(String ifaceName) throws KuraException {
        File configFile = new File(getHostapdConfigFileName(ifaceName));
        if (!configFile.exists()) {
            throw KuraException.internalError("Config file does not exist: " + configFile.getAbsolutePath());
        }
        try {
            if (HostapdManager.isRunning(ifaceName)) {
                stop(ifaceName);
            }

            // start hostapd
            String launchHostapdCommand = generateStartCommand(ifaceName);
            logger.info("starting hostapd for the {} interface --> {}", ifaceName, launchHostapdCommand);
            int stat = LinuxProcessUtil.start(launchHostapdCommand);
            if (stat != 0) {
                logger.error("failed to start hostapd for the {} interface for unknown reason - errorCode={}",
                        ifaceName, stat);
                throw KuraException.internalError("failed to start hostapd for unknown reason");
            }
            Thread.sleep(1000);
        } catch (Exception e) {
            throw KuraException.internalError(e);
        }
    }

    public static void stop(String ifaceName) throws KuraException {
        try {
            if (!isIntelEdison) {
                LinuxProcessUtil.stopAndKill(getPid(ifaceName));
            } else {
                LinuxProcessUtil.start("systemctl stop hostapd");
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            throw KuraException.internalError(e);
        }
    }

    public static boolean isRunning(String ifaceName) throws KuraException {
        try {
            boolean ret = false;
            if (getPid(ifaceName) > 0) {
                ret = true;
            }
            logger.trace("isRunning() :: --> {}", ret);
            return ret;
        } catch (Exception e) {
            throw KuraException.internalError(e);
        }
    }

    public static int getPid(String ifaceName) throws KuraException {
        try {
            String[] tokens = { getHostapdConfigFileName(ifaceName) };
            int pid = LinuxProcessUtil.getPid("hostapd", tokens);
            logger.trace("getPid() :: pid={}", pid);
            return pid;
        } catch (Exception e) {
            throw KuraException.internalError(e);
        }
    }

    private static String generateStartCommand(String ifaceName) {
        StringBuilder cmd = new StringBuilder();
        if (isIntelEdison) {
            cmd.append("systemctl start hostapd");
        } else {
            File configFile = new File(getHostapdConfigFileName(ifaceName));
            cmd.append(HOSTAPD_EXEC).append(" -B ").append(configFile.getAbsolutePath());
        }
        return cmd.toString();
    }

    public static String getHostapdConfigFileName(String ifaceName) {
        StringBuilder sb = new StringBuilder();
        if (isIntelEdison) {
            sb.append("/etc/hostapd/hostapd.conf");
        } else {
            sb.append("/etc/hostapd-").append(ifaceName).append(".conf");
        }
        return sb.toString();
    }

    public static String getDriver(String iface) throws KuraException {
        String driver;
        Collection<String> supportedWifiOptions = null;
        try {
            supportedWifiOptions = WifiOptions.getSupportedOptions(iface);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }

        if (!supportedWifiOptions.isEmpty()) {
            if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_NL80211)) {
                driver = WifiOptions.WIFI_MANAGED_DRIVER_NL80211;
            } else {
                driver = "hostap";
            }
        } else {
            // make a guess
            driver = WifiOptions.WIFI_MANAGED_DRIVER_NL80211;
        }
        return driver;
    }
}
