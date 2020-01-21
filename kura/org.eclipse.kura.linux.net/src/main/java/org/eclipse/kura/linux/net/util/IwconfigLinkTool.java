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

import java.io.ByteArrayOutputStream;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IwconfigLinkTool extends LinkToolImpl implements LinkTool {

    private static final Logger logger = LoggerFactory.getLogger(IwconfigLinkTool.class);

    private static final String MODE = "Mode:";
    private static final String SIGNAL_LEVEL = "Signal level=";
    private static final String BIT_RATE = "Bit Rate=";

    private final CommandExecutorService executorService;

    /**
     * constructor
     *
     * @param ifaceName
     *            - interface name as {@link String}
     * @param executorService
     *            - the {@link org.eclipse.kura.executor.CommandExecutorService} used to run the command
     */
    public IwconfigLinkTool(String ifaceName, CommandExecutorService executorService) {
        setIfaceName(ifaceName);
        setDuplex("half");
        this.executorService = executorService;
    }

    @Override
    public boolean get() throws KuraException {
        Command command = new Command(new String[] { "iwconfig", getIfaceName() });
        command.setTimeout(60);
        command.setOutputStream(new ByteArrayOutputStream());
        CommandStatus status = this.executorService.execute(command);
        if (!status.getExitStatus().isSuccessful()) {
            logger.warn("The iwconfig returned with exit value {}", status.getExitStatus().getExitCode());
            return false;
        }
        parse(new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));
        return true;
    }

    private void parse(String commandOutput) {
        boolean associated = false;
        for (String line : commandOutput.split("\n")) {
            line = line.trim();
            if (line.contains(MODE)) {
                associated = parseMode(line);
            } else if (line.contains(BIT_RATE)) {
                parseBitrate(line);
            } else if (line.contains(SIGNAL_LEVEL)) {
                int sig = parseSignalLevel(line);
                if (associated && sig > -100) { // TODO: adjust this threshold?
                    logger.debug("get() :: !! Link Detected !!");
                    setSignal(sig);
                    setLinkDetected(true);
                }
            }

            if (!associated) {
                break;
            }
        }
    }

    private boolean parseMode(String line) {
        boolean associated = false;
        int modeInd = line.indexOf(MODE);
        if (modeInd >= 0) {
            String mode = line.substring(modeInd + MODE.length());
            mode = mode.substring(0, mode.indexOf(' '));
            if ("Managed".equals(mode)) {
                int apInd = line.indexOf("Access Point:");
                if (apInd > 0) {
                    line = line.substring(apInd + "Access Point:".length()).trim();
                    if (!line.startsWith("Not-Associated")) {
                        associated = true;
                    }
                }
            }
        }
        return associated;
    }

    private void parseBitrate(String line) {
        int bitRateInd = line.indexOf(BIT_RATE);
        line = line.substring(bitRateInd + BIT_RATE.length());
        line = line.substring(0, line.indexOf(' '));
        double bitrate = Double.parseDouble(line) * 1000000;
        setSpeed((int) Math.round(bitrate));
    }

    private int parseSignalLevel(String line) {
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
        return sig;
    }
}
