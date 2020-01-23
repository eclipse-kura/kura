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

package org.eclipse.kura.linux.net.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IwScanTool extends ScanTool implements IScanTool {

    private static final Logger logger = LoggerFactory.getLogger(IwScanTool.class);
    private static final Object lock = new Object();
    private String ifaceName;
    private int timeout;
    private final LinuxNetworkUtil linuxNetworkUtil;
    private final CommandExecutorService executorService;

    private InputStream scanOutput;
    private boolean status;
    private String errmsg;

    protected IwScanTool(CommandExecutorService executorService) {
        this.timeout = 20;
        this.linuxNetworkUtil = new LinuxNetworkUtil(executorService);
        this.executorService = executorService;
    }

    protected IwScanTool(String ifaceName, CommandExecutorService executorService) {
        this(executorService);
        this.ifaceName = ifaceName;
        this.errmsg = "";
        this.status = false;
    }

    protected IwScanTool(String ifaceName, int tout, CommandExecutorService executorService) {
        this(ifaceName, executorService);
        this.timeout = tout;
    }

    @Override
    public List<WifiAccessPoint> scan() throws KuraException {

        List<WifiAccessPoint> wifiAccessPoints;
        synchronized (lock) {
            activateInterface();

            String[] cmd = formIwScanCommand(IwScanTool.this.ifaceName);
            if (logger.isInfoEnabled()) {
                logger.info("scan() :: executing: {}", String.join(" ", cmd));
            }
            IwScanTool.this.status = false;
            Command iwScanCommand = new Command(cmd);
            iwScanCommand.setTimeout(IwScanTool.this.timeout);
            iwScanCommand.setOutputStream(new ByteArrayOutputStream());
            iwScanCommand.setErrorStream(new ByteArrayOutputStream());
            CommandStatus iwCommandStatus = this.executorService.execute(iwScanCommand);
            int exitValue = iwCommandStatus.getExitStatus().getExitCode();
            if (logger.isInfoEnabled()) {
                logger.info("scan() :: {} command returns status = {}", String.join(" ", cmd), exitValue);
            }
            // If timedout, the exit value is 124
            if (exitValue == 0 || exitValue == 124) {
                IwScanTool.this.status = true;
                IwScanTool.this.scanOutput = new ByteArrayInputStream(
                        ((ByteArrayOutputStream) iwCommandStatus.getOutputStream()).toByteArray());
            } else {
                if (logger.isErrorEnabled()) {
                    logger.error("scan() :: failed to execute {} error code is {}", String.join(" ", cmd), exitValue);
                    logger.error("scan() :: STDERR: {}", new String(
                            ((ByteArrayOutputStream) iwCommandStatus.getErrorStream()).toByteArray(), Charsets.UTF_8));
                }
            }

            if (!this.status) {
                throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, this.errmsg);
            }

            logger.info("scan() :: the 'iw scan' command executed successfully, parsing output ...");
            try {
                wifiAccessPoints = parse();
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, "error parsing scan results");
            }
        }

        return wifiAccessPoints;
    }

    private void activateInterface() throws KuraException {
        if (!this.linuxNetworkUtil.hasAddress(this.ifaceName)) {
            // activate the interface
            String[] cmdIpLink = { "ip", "link", "set", this.ifaceName, "up" };
            CommandStatus commandStatus = this.executorService.execute(new Command(cmdIpLink));
            if (!commandStatus.getExitStatus().isSuccessful()) {
                throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR,
                        "Failed to activate interface " + this.ifaceName);
            }

            // remove the previous ip address (needed on mgw)
            String[] cmdIpAddr = { "ip", "addr", "flush", "dev", this.ifaceName };
            commandStatus = this.executorService.execute(new Command(cmdIpAddr));
            if (!commandStatus.getExitStatus().isSuccessful()) {
                throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR,
                        "Failed to remove address for interface " + this.ifaceName);
            }
        }
    }

    private List<WifiAccessPoint> parse() throws KuraException {
        List<IWAPParser> apInfos = new ArrayList<>();
        IWAPParser currentAP = null;
        try (InputStreamReader isr = new InputStreamReader(this.scanOutput);
                BufferedReader br = new BufferedReader(isr)) {
            String line = null;
            while ((line = br.readLine()) != null) {

                if (line.startsWith("scan aborted!")) {
                    logger.warn("parse() :: scan operation was aborted");
                    throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, "iw scan operation was aborted");
                }

                if (line.startsWith("BSS")) {
                    currentAP = parseAP(line);
                    // Add it to the list
                    if (currentAP != null) {
                        apInfos.add(currentAP);
                    }

                } else {
                    // Must be an AP property line
                    String propLine = line.trim();
                    if (currentAP != null) {
                        // We're currently parsing an AP
                        currentAP = parseAPProperty(currentAP, propLine);
                    }
                }
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }

        // Generate list of WifiAccessPoint objects
        List<WifiAccessPoint> wifiAccessPoints = new ArrayList<>();
        for (IWAPParser info : apInfos) {
            wifiAccessPoints.add(info.toWifiAccessPoint());
        }
        return wifiAccessPoints;
    }

    private IWAPParser parseAPProperty(IWAPParser currentAP, String propLine) {
        try {
            // Give this line to the AP parser
            currentAP.parsePropLine(propLine);
        } catch (Exception e) {
            currentAP = null;
            logger.error("Failed to parse line: {}; giving up on the current AP", propLine, e);
        }
        return currentAP;
    }

    private IWAPParser parseAP(String line) {
        IWAPParser currentAP = null;
        // new AP - parse out the MAC
        StringTokenizer st = new StringTokenizer(line, " ");
        st.nextToken(); // eat BSS
        String macAddressString = st.nextToken().substring(0, 16);

        if (macAddressString != null) {
            // Set this AP parser as the current one
            currentAP = new IWAPParser(macAddressString);
        }
        return currentAP;
    }

    private String[] formIwScanCommand(String interfaceName) {
        return new String[] { "iw", "dev", interfaceName, "scan" };
    }
}
