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
 * Defines ethtool utility
 *
 * @author ilya.binshtok
 *
 */
public class EthTool implements LinkTool {

    private static final Logger logger = LoggerFactory.getLogger(EthTool.class);

    private static final String LINK_DETECTED = "Link detected:";
    private static final String LINK_DUPLEX = "Duplex:";
    private static final String LINK_SPEED = "Speed:";

    private String ifaceName = null;
    private boolean linkDetected = false;
    private int speed = 0; // in b/s
    private String duplex = null;

    private final CommandExecutorService executorService;

    /**
     * ethtool constructor
     *
     * @param ifaceName
     *            - interface name as {@link String}
     * @param executorService
     *            - the {@link org.eclipse.kura.executor.CommandExecutorService} used to run the command
     */
    public EthTool(String ifaceName, CommandExecutorService executorService) {
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
        Command command = new Command(new String[] { "ethtool", this.ifaceName });
        command.setTimeout(60);
        command.setOutputStream(new ByteArrayOutputStream());
        CommandStatus status = this.executorService.execute(command);
        parse(new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));

        return status.getExitStatus().isSuccessful();
    }

    private void parse(String commandOutput) {
        String[] lines = commandOutput.split("\n");
        for (String line : lines) {
            int ind = -1;
            if ((ind = line.indexOf(LINK_DETECTED)) >= 0) {
                logger.trace("Link detected from: {}", line);
                line = line.substring(ind + LINK_DETECTED.length()).trim();
                this.linkDetected = line.compareTo("yes") == 0;
            } else if ((ind = line.indexOf(LINK_DUPLEX)) >= 0) {
                this.duplex = line.substring(ind + LINK_DUPLEX.length()).trim();
            } else if ((ind = line.indexOf(LINK_SPEED)) >= 0) {
                line = line.substring(ind + LINK_SPEED.length()).trim();
                setSpeed(line);
            }
        }
    }

    private void setSpeed(String line) {
        if (line.compareTo("10Mb/s") == 0) {
            this.speed = 10000000;
        } else if (line.compareTo("100Mb/s") == 0) {
            this.speed = 100000000;
        } else if (line.compareTo("1000Mb/s") == 0) {
            this.speed = 1000000000;
        }
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
     * @see org.eclipse.util.net.service.ILinkTool#getDuplex()
     */
    @Override
    public String getDuplex() {
        return this.duplex;
    }

    @Override
    public int getSignal() {
        return 0;
    }
}
