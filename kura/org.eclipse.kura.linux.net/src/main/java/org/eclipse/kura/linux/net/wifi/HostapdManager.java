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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.Pid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes Hostapd Manager
 *
 */
public class HostapdManager {

    private static Logger logger = LoggerFactory.getLogger(HostapdManager.class);

    private static final String HOSTAPD = "hostapd";
    private final CommandExecutorService executorService;
    private final WifiOptions wifiOptions;

    public HostapdManager(CommandExecutorService executorService) {
        this.executorService = executorService;
        this.wifiOptions = new WifiOptions(executorService);
    }

    public void start(String ifaceName) throws KuraException {
        File configFile = new File(getHostapdConfigFileName(ifaceName));
        if (!configFile.exists()) {
            throw KuraException.internalError("Config file does not exist: " + configFile.getAbsolutePath());
        }
        try {
            if (isRunning(ifaceName)) {
                stop(ifaceName);
            }

            // start hostapd
            String[] launchHostapdCommand = formHostapdStartCommand(ifaceName);
            logger.info("starting hostapd for the {} interface --> {}", ifaceName, launchHostapdCommand);
            CommandStatus status = this.executorService.execute(new Command(launchHostapdCommand));
            int exitValue = status.getExitStatus();
            if (exitValue != 0) {
                logger.error("failed to start hostapd for the {} interface for unknown reason - errorCode={}",
                        ifaceName, exitValue);
                throw KuraException.internalError("failed to start hostapd for unknown reason");
            }
            Thread.sleep(1000);
        } catch (Exception e) {
            throw KuraException.internalError(e);
        }
    }

    public void stop(String ifaceName) throws KuraException {
        if (!this.executorService.kill(formHostapdStartCommand(ifaceName), LinuxSignal.SIGKILL)) {
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR,
                    "Failed to stop hostapd for interface " + ifaceName);
        }
    }

    public boolean isRunning(String ifaceName) {
        return this.executorService.isRunning(formHostapdStartCommand(ifaceName));
    }

    public int getPid(String ifaceName) throws KuraException {
        String[] command = formHostapdStartCommand(ifaceName);
        // Filter the pid whose command exactly matches the connectCommand
        List<Pid> pids = executorService.getPids(command).entrySet().stream()
                .filter(entry -> entry.getKey().equals(String.join(" ", command))).map(Map.Entry::getValue)
                .collect(Collectors.toList());
        if (!pids.isEmpty()) {
            return pids.get(0).getPid();
        } else {
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR,
                    "Failed to get hostapd pid for interface " + ifaceName);
        }
    }

    private static String[] formHostapdStartCommand(String ifaceName) {
        File configFile = new File(getHostapdConfigFileName(ifaceName));
        return new String[] { HOSTAPD, "-B", configFile.getAbsolutePath() };
    }

    public static String getHostapdConfigFileName(String ifaceName) {
        StringBuilder sb = new StringBuilder();

        sb.append("/etc/hostapd-").append(ifaceName).append(".conf");

        return sb.toString();
    }

    public String getDriver(String iface) throws KuraException {
        String driver;
        Collection<String> supportedWifiOptions = null;
        try {
            supportedWifiOptions = this.wifiOptions.getSupportedOptions(iface);
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
