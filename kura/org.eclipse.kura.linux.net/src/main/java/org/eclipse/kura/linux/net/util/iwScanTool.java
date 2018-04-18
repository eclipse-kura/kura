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

package org.eclipse.kura.linux.net.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class iwScanTool extends ScanTool implements IScanTool {

    private static final Logger logger = LoggerFactory.getLogger(iwScanTool.class);
    private static final String SCAN_THREAD_NAME = "iwScanThread";
    private static final String FAILED_EXECUTE_SCAN_CMD_MSG = "failed to execute scan command ";
    private static final Object s_lock = new Object();
    private String ifaceName;
    private int timeout;

    // FIXME:MC Is this process always closed?
    private SafeProcess process;
    private boolean status;
    private String errmsg;

    protected iwScanTool() {
        this.timeout = 20;
    }

    protected iwScanTool(String ifaceName) {
        this();
        this.ifaceName = ifaceName;
        this.errmsg = "";
        this.status = false;
    }

    protected iwScanTool(String ifaceName, int tout) {
        this(ifaceName);
        this.timeout = tout;
    }

    @Override
    public List<WifiAccessPoint> scan() throws KuraException {

        List<WifiAccessPoint> wifiAccessPoints = new ArrayList<>();
        synchronized (s_lock) {
            StringBuilder sb = new StringBuilder();

            SafeProcess prIpLink = null;
            SafeProcess prIpAddr = null;
            try {
                if (!LinuxNetworkUtil.hasAddress(this.ifaceName)) {
                    // activate the interface
                    sb.append("ip link set ").append(this.ifaceName).append(" up");
                    prIpLink = ProcessUtil.exec(sb.toString());
                    prIpLink.waitFor();

                    // remove the previous ip address (needed on mgw)
                    sb = new StringBuilder();
                    sb.append("ip addr flush dev ").append(this.ifaceName);
                    prIpAddr = ProcessUtil.exec(sb.toString());
                    prIpAddr.waitFor();
                }
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
            } finally {
                if (prIpLink != null) {
                    ProcessUtil.destroy(prIpLink);
                }
                if (prIpAddr != null) {
                    ProcessUtil.destroy(prIpAddr);
                }
            }

            long timerStart = System.currentTimeMillis();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> task = executor.submit(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName(SCAN_THREAD_NAME);
                    int stat = -1;
                    iwScanTool.this.process = null;
                    StringBuilder sb = new StringBuilder();
                    sb.append("iw dev ").append(iwScanTool.this.ifaceName).append(" scan");
                    logger.info("scan() :: executing: {}", sb.toString());
                    iwScanTool.this.status = false;
                    try {
                        iwScanTool.this.process = ProcessUtil.exec(sb.toString());
                        stat = iwScanTool.this.process.waitFor();
                        logger.info("scan() :: {} command returns status={}", sb.toString(), stat);
                        if (stat == 0) {
                            iwScanTool.this.status = true;
                        } else {
                            logger.error("scan() :: failed to execute {} error code is {}", sb.toString(), stat);
                            logger.error("scan() :: STDERR: {}",
                                    LinuxProcessUtil.getInputStreamAsString(iwScanTool.this.process.getErrorStream()));
                        }
                    } catch (Exception e) {
                        iwScanTool.this.errmsg = "exception executing scan command";
                        logger.error(FAILED_EXECUTE_SCAN_CMD_MSG, e);
                    }
                }
            });

            while (!task.isDone()) {
                if (System.currentTimeMillis() > timerStart + this.timeout * 1000) {
                    logger.warn("scan() :: scan timeout");
                    sb = new StringBuilder();
                    sb.append("iw dev ").append(this.ifaceName).append(" scan");
                    try {
                        int pid = LinuxProcessUtil.getPid(sb.toString());
                        if (pid >= 0) {
                            logger.warn("scan() :: scan timeout :: killing pid {}", pid);
                            LinuxProcessUtil.kill(pid);
                        }
                    } catch (Exception e) {
                        logger.error(FAILED_EXECUTE_SCAN_CMD_MSG, e);
                    }
                    task.cancel(true);
                    task = null;
                    this.errmsg = "timeout executing scan command";
                    break;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            if (!this.status || this.process == null) {
                throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, this.errmsg);
            }

            logger.info("scan() :: the 'iw scan' command executed successfully, parsing output ...");
            try {
                wifiAccessPoints = parse();
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, "error parsing scan results");
            } finally {
                logger.info("scan() :: destroing scan proccess ...");
                if (this.process != null) {
                    ProcessUtil.destroy(this.process);
                }
                this.process = null;

                logger.info("scan() :: Terminating {} ...", SCAN_THREAD_NAME);
                executor.shutdownNow();
                try {
                    executor.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted", e);
                }
                logger.info("scan() :: 'iw scan' thread terminated? - {}", executor.isTerminated());
                executor = null;
            }
        }

        return wifiAccessPoints;
    }

    private List<WifiAccessPoint> parse() throws KuraException {
        List<IWAPParser> apInfos = new ArrayList<>();
        IWAPParser currentAP = null;
        try (InputStreamReader isr = new InputStreamReader(this.process.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            String line = null;
            while ((line = br.readLine()) != null) {

                if (line.startsWith("scan aborted!")) {
                    br.close();
                    logger.warn("parse() :: scan operation was aborted");
                    throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, "iw scan operation was aborted");
                }

                if (line.startsWith("BSS")) {
                    // new AP - parse out the MAC
                    StringTokenizer st = new StringTokenizer(line, " ");
                    st.nextToken(); // eat BSS
                    String macAddressString = st.nextToken().substring(0, 16);

                    if (macAddressString != null) {
                        // Set this AP parser as the current one
                        currentAP = new IWAPParser(macAddressString);
                    }

                    // Add it to the list
                    apInfos.add(currentAP);

                } else {
                    // Must be an AP property line
                    String propLine = line.trim();

                    if (currentAP != null) {
                        // We're currently parsing an AP
                        try {
                            // Give this line to the AP parser
                            currentAP.parsePropLine(propLine);
                        } catch (Exception e) {
                            currentAP = null;
                            logger.error("Failed to parse line: {}; giving up on the current AP", propLine, e);
                        }
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
}
