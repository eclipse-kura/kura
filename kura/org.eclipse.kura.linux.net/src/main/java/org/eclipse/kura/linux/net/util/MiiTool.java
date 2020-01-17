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

/**
 * Defines mii-tool utility
 *
 * @author ilya.binshtok
 *
 */
public class MiiTool implements LinkTool {

    private static final Logger logger = LoggerFactory.getLogger(MiiTool.class);
    private static final String DUPLEX = null;
    private String ifaceName = null;
    private boolean linkDetected = false;
    private int speed = 0; // in b/s

    private CommandExecutorService executorService;

    /**
     * MiiTool constructor
     *
     * @param ifaceName
     *            - interface name as {@link String}
     * @param executorService
     *            - the {@link org.eclipse.kura.executor.CommandExecutorService} used to run the command
     */
    public MiiTool(String ifaceName, CommandExecutorService executorService) {
        this.ifaceName = ifaceName;
        this.executorService = executorService;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.kura.util.net.service.ILinkTool#get()
     */
    @Override
    public boolean get() throws KuraException {
        Command command = new Command(new String[] { "mii-tool", this.ifaceName });
        command.setTimeout(60);
        command.setOutputStream(new ByteArrayOutputStream());
        CommandStatus status = this.executorService.execute(command);
        parse(new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));

        return status.getExitStatus().isSuccessful();
    }

    private void parse(String commandOutput) {
        boolean isLinkDetected = false;
        int spd = 0;
        String[] lines = commandOutput.split("\n");
        for (String line : lines) {
            if (line.startsWith(this.ifaceName) && (line.indexOf("link ok") > -1)) {
                isLinkDetected = true;
                spd = parseLine(line);
            }
        }
        this.speed = spd;
        this.linkDetected = isLinkDetected;
    }

    private int parseLine(String line) {
        int spd = 0;
        try {
            String tmp;
            if (line.indexOf("negotiated") > -1) {
                tmp = line.substring(line.indexOf("negotiated") + 11);
            } else {
                tmp = line.substring(line.indexOf(',') + 2);
            }
            String speedTxt = tmp.substring(0, tmp.indexOf(','));
            if ((speedTxt.compareTo("10baseT-HD") == 0) || (speedTxt.compareTo("10baseT-FD") == 0)) {
                spd = 10000000;
            } else if ((speedTxt.compareTo("100baseTx-HD") == 0) || (speedTxt.compareTo("100baseTx-FD") == 0)) {
                spd = 100000000;
            } else {
                spd = -2;
            }
        } catch (Exception e) {
            logger.warn("Exception while parsing string...", e);
        }
        return spd;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.kura.util.net.service.ILinkTool#getIfaceName()
     */
    @Override
    public String getIfaceName() {
        return this.ifaceName;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.kura.util.net.service.ILinkTool#isLinkDetected()
     */
    @Override
    public boolean isLinkDetected() {
        return this.linkDetected;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.kura.util.net.service.ILinkTool#getSpeed()
     */
    @Override
    public int getSpeed() {
        return this.speed;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.kura.util.net.service.ILinkTool#getDuplex()
     */
    @Override
    public String getDuplex() {
        return DUPLEX;
    }

    @Override
    public int getSignal() {
        return 0;
    }
}
