/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WpaSupplicantManager {

    private static Logger s_logger = LoggerFactory.getLogger(WpaSupplicantManager.class);

    private static final String OS_VERSION = System.getProperty("kura.os.version");
    private static final File TEMP_CONFIG_FILE = new File("/tmp/wpa_supplicant.conf");

    private static String m_driver = null;

    private static boolean s_isIntelEdison = false;

    static {
        StringBuilder sb = new StringBuilder(KuraConstants.Intel_Edison.getImageName());
        sb.append('_').append(KuraConstants.Intel_Edison.getImageVersion()).append('_')
                .append(KuraConstants.Intel_Edison.getTargetName());
        if (OS_VERSION.equals(sb.toString())) {
            s_isIntelEdison = true;
        }
    }

    public static void start(String interfaceName, final WifiMode mode, String driver) throws KuraException {
        start(interfaceName, mode, driver, new File(getWpaSupplicantConfigFilename(interfaceName)));
    }

    public static void startTemp(String interfaceName, final WifiMode mode, String driver) throws KuraException {
        start(interfaceName, mode, driver, TEMP_CONFIG_FILE);
    }

    private static synchronized void start(String interfaceName, final WifiMode mode, String driver, File configFile)
            throws KuraException {
        s_logger.debug("enable WPA Supplicant");

        try {
            if (WpaSupplicantManager.isRunning(interfaceName)) {
                stop(interfaceName);
            }

            String drv = WpaSupplicant.getDriver(interfaceName);
            if (drv != null) {
                if (s_isIntelEdison) {
                    m_driver = driver;
                } else {
                    m_driver = drv;
                }
            } else {
                m_driver = driver;
            }

            // start wpa_supplicant
            String wpaSupplicantCommand = formSupplicantStartCommand(interfaceName, configFile);
            s_logger.debug("starting wpa_supplicant -> {}", wpaSupplicantCommand);
            LinuxProcessUtil.start(wpaSupplicantCommand);
        } catch (Exception e) {
            s_logger.error("Exception while enabling WPA Supplicant!", e);
            throw KuraException.internalError(e);
        }
    }

    /*
     * This method forms wpa_supplicant start command
     */
    private static String formSupplicantStartCommand(String ifaceName, File configFile) {
        StringBuilder sb = new StringBuilder();
        if (s_isIntelEdison) {
            sb.append("systemctl start wpa_supplicant");
        } else {
            sb.append("wpa_supplicant -B -D ");
            sb.append(m_driver);
            sb.append(" -i ");
            sb.append(ifaceName);
            sb.append(" -c ");
            sb.append(configFile);
        }

        return sb.toString();
    }

    /*
     * This method forms wpa_supplicant start command
     */
    private static String formSupplicantStopCommand(String ifaceName) throws KuraException {

        StringBuilder sb = new StringBuilder();
        if (s_isIntelEdison) {
            sb.append("systemctl stop wpa_supplicant");
        } else {
            // sb.append("killall wpa_supplicant");
            int pid;
            try {
                pid = getPid(ifaceName);
                if (pid > 0) {
                    sb.append("kill -9 ").append(pid);
                }
            } catch (KuraException e) {
                throw KuraException.internalError(e);
            }
        }

        return sb.toString();
    }

    /**
     * Reports if wpa_supplicant is running
     *
     * @return {@link boolean}
     */
    public static boolean isRunning(String ifaceName) throws KuraException {
        try {
            boolean ret = false;
            if (getPid(ifaceName) > 0) {
                ret = true;
            }
            s_logger.trace("isRunning() :: --> {}", ret);
            return ret;
        } catch (Exception e) {
            throw KuraException.internalError(e);
        }
    }

    public static int getPid(String ifaceName) throws KuraException {
        try {
            String[] tokens = { "-i " + ifaceName };
            int pid = LinuxProcessUtil.getPid("wpa_supplicant", tokens);
            s_logger.trace("getPid() :: pid={}", pid);
            return pid;
        } catch (Exception e) {
            throw KuraException.internalError(e);
        }
    }

    public static boolean isTempRunning() throws KuraException {
        try {
            String[] tokens = { "-c " + TEMP_CONFIG_FILE };
            int pid = LinuxProcessUtil.getPid("wpa_supplicant", tokens);
            return pid > -1;
        } catch (Exception e) {
            throw KuraException.internalError(e);
        }
    }

    /**
     * Stops all instances of wpa_supplicant
     *
     * @throws Exception
     */
    public static void stop(String ifaceName) throws KuraException {
        try {
            // kill wpa_supplicant
            s_logger.debug("stopping wpa_supplicant");
            String cmd = formSupplicantStopCommand(ifaceName);
            if (cmd != null && !cmd.isEmpty()) {
                LinuxProcessUtil.start(cmd);
                if (ifaceName != null) {
                    LinuxNetworkUtil.disableInterface(ifaceName);
                }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            throw KuraException.internalError(e);
        }
    }

    public static String getWpaSupplicantConfigFilename(String ifaceName) {
        StringBuilder sb = new StringBuilder();
        if (s_isIntelEdison) {
            sb.append("/etc/wpa_supplicant/wpa_supplicant.conf");
        } else {
            sb.append("/etc/wpa_supplicant-").append(ifaceName).append(".conf");
        }
        return sb.toString();
    }
}
