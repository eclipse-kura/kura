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

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class iwconfigLinkTool implements LinkTool {

    private static final Logger logger = LoggerFactory.getLogger(iwconfigLinkTool.class);

    private static final String MODE = "Mode:";
    private static final String SIGNAL_LEVEL = "Signal level=";
    private static final String BIT_RATE = "Bit Rate=";

    private String interfaceName = null;
    private boolean linkDetected = false;
    private int speed = 0; // in b/s
    private String duplex = null;
    private int signal = 0;

    /**
     * constructor
     *
     * @param ifaceName
     *            - interface name as {@link String}
     */
    public iwconfigLinkTool(String ifaceName) {
        this.interfaceName = ifaceName;
        this.duplex = "half";
    }

    @Override
    public boolean get() throws KuraException {
        SafeProcess proc = null;
        try {
            proc = ProcessUtil.exec("iwconfig " + this.interfaceName);
            if (proc.waitFor() != 0) {
                logger.warn("The iwconfig returned with exit value {}", proc.exitValue());
                return false;
            }
            parse(proc);
            return true;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    private void parse(SafeProcess proc) throws KuraException {
        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            boolean associated = false;
            String line = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.contains(MODE)) {
                    int modeInd = line.indexOf(MODE);
                    if (modeInd >= 0) {
                        String mode = line.substring(modeInd + MODE.length());
                        mode = mode.substring(0, mode.indexOf(' '));
                        if ("Managed".equals(mode)) {
                            int apInd = line.indexOf("Access Point:");
                            if (apInd > 0) {
                                line = line.substring(apInd + "Access Point:".length()).trim();
                                if (line.startsWith("Not-Associated")) {
                                    break;
                                }
                                associated = true;
                            }
                        } else {
                            break;
                        }
                    }
                } else if (line.contains(BIT_RATE)) {
                    int bitRateInd = line.indexOf(BIT_RATE);
                    line = line.substring(bitRateInd + BIT_RATE.length());
                    line = line.substring(0, line.indexOf(' '));
                    double bitrate = Double.parseDouble(line) * 1000000;
                    this.speed = (int) Math.round(bitrate);
                } else if (line.contains(SIGNAL_LEVEL)) {
                    int sigLevelInd = line.indexOf(SIGNAL_LEVEL);
                    line = line.substring(sigLevelInd + SIGNAL_LEVEL.length());
                    line = line.substring(0, line.indexOf(' '));
                    int sig = 0;
                    if (line.contains("/")) {
                        // Could also be of format 39/100
                        final String[] parts = line.split("/");
                        sig = (int) Float.parseFloat(parts[0]);
                        if (sig <= 0) {
                            sig = -100;
                        } else if (sig >= 100) {
                            sig = -50;
                        } else {
                            sig = sig / 2 - 100;
                        }
                    } else {
                        sig = Integer.parseInt(line);
                    }

                    if (associated && sig > -100) { // TODO: adjust this threshold?
                        logger.debug("get() :: !! Link Detected !!");
                        this.signal = sig;
                        this.linkDetected = true;
                    }
                }
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }
    }

    @Override
    public String getIfaceName() {
        return this.interfaceName;
    }

    @Override
    public boolean isLinkDetected() {
        return this.linkDetected;
    }

    @Override
    public int getSpeed() {
        return this.speed;
    }

    @Override
    public String getDuplex() {
        return this.duplex;
    }

    @Override
    public int getSignal() {
        return this.signal;
    }
}
