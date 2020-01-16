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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WpaSupplicantManager {

    private static final String WPA_SUPPLICANT = "wpa_supplicant";

    private static Logger logger = LoggerFactory.getLogger(WpaSupplicantManager.class);

    private static final File TEMP_CONFIG_FILE = new File("/tmp/wpa_supplicant.conf");

    private String supplicantDriver = null;
    private final CommandExecutorService executorService;
    private final LinuxNetworkUtil linuxNetworkUtil;
    private final WifiOptions wifiOptions;

    public WpaSupplicantManager(CommandExecutorService executorService) {
        this.linuxNetworkUtil = new LinuxNetworkUtil(executorService);
        this.wifiOptions = new WifiOptions(executorService);
        this.executorService = executorService;
    }

    public void start(String interfaceName, String driver) throws KuraException {
        start(interfaceName, driver, new File(getWpaSupplicantConfigFilename(interfaceName)));
    }

    public void startTemp(String interfaceName, String driver) throws KuraException {
        start(interfaceName, driver, TEMP_CONFIG_FILE);
    }

    private synchronized void start(String interfaceName, String driver, File configFile) throws KuraException {
        logger.debug("enable WPA Supplicant");

        try {
            if (isRunning(interfaceName)) {
                stop(interfaceName);
            }

            String drv = getDriver(interfaceName);
            if (drv != null) {
                this.supplicantDriver = drv;
            } else {
                this.supplicantDriver = driver;
            }

            // start wpa_supplicant
            String[] wpaSupplicantCommand = formSupplicantStartCommand(interfaceName, configFile);
            logger.info("starting wpa_supplicant for the {} interface -> {}", interfaceName, wpaSupplicantCommand);
            Command command = new Command(wpaSupplicantCommand);
            command.setTimeout(60);
            CommandStatus status = this.executorService.execute(command);
            int exitValue = (Integer) status.getExitStatus().getExitCode();
            if (exitValue != 0 && exitValue != 255) {
                logger.error("failed to start wpa_supplicant for the {} interface for unknown reason - errorCode={}",
                        interfaceName, exitValue);
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, "failed to start hostapd for unknown reason");
            }
        } catch (Exception e) {
            logger.error("Exception while enabling WPA Supplicant!", e);
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, "Exception while enabling WPA Supplicant!");
        }
    }

    /*
     * This method forms wpa_supplicant start command
     */
    private String[] formSupplicantStartCommand(String ifaceName, File configFile) {
        return new String[] { WPA_SUPPLICANT, "-B", "-D", this.supplicantDriver, "-i", ifaceName, "-c",
                configFile.getAbsolutePath() };
    }

    /**
     * Reports if wpa_supplicant is running
     *
     * @return {@link boolean}
     */
    public boolean isRunning(String ifaceName) {
        return this.executorService.isRunning(new String[] { WPA_SUPPLICANT, "-i", ifaceName });
    }

    public int getPid(String ifaceName) throws KuraException {
        List<Pid> pids = new ArrayList<>(
                this.executorService.getPids(new String[] { WPA_SUPPLICANT, "-i", ifaceName }).values());
        if (!pids.isEmpty()) {
            return (Integer) pids.get(0).getPid();
        } else {
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR,
                    "Failed to get wpa_supplicant pid for interface " + ifaceName);
        }
    }

    public boolean isTempRunning() {
        return this.executorService.isRunning(new String[] { WPA_SUPPLICANT, "-c", TEMP_CONFIG_FILE.toString() });
    }

    /**
     * Stops all instances of wpa_supplicant
     *
     * @throws Exception
     */
    public void stop(String ifaceName) throws KuraException {
        Map<String, Pid> pids = this.executorService.getPids(new String[] { WPA_SUPPLICANT, "-i", ifaceName });
        for (Pid pid : pids.values()) {
            if (!this.executorService.stop(pid, LinuxSignal.SIGKILL)) {
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR,
                        "Failed to stop hostapd for interface " + ifaceName);
            }
        }
        this.linuxNetworkUtil.disableInterface(ifaceName);
    }

    public static String getWpaSupplicantConfigFilename(String ifaceName) {
        StringBuilder sb = new StringBuilder();

        sb.append("/etc/wpa_supplicant-").append(ifaceName).append(".conf");

        return sb.toString();
    }

    public String getDriver(String iface) throws KuraException {
        String driver = null;
        Collection<String> supportedWifiOptions = null;
        try {
            supportedWifiOptions = this.wifiOptions.getSupportedOptions(iface);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }

        if (!supportedWifiOptions.isEmpty()) {
            if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_NL80211)) {
                driver = WifiOptions.WIFI_MANAGED_DRIVER_NL80211;
            } else if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_WEXT)) {
                driver = WifiOptions.WIFI_MANAGED_DRIVER_WEXT;
            }
        }
        return driver;
    }
}
