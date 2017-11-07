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
package org.eclipse.kura.linux.net.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
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

    /**
     * MiiTool constructor
     *
     * @param ifaceName
     *            - interface name as {@link String}
     */
    public MiiTool(String ifaceName) {
        this.ifaceName = ifaceName;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.kura.util.net.service.ILinkTool#get()
     */
    @Override
    public boolean get() throws KuraException {
        SafeProcess proc = null;
        boolean result = false;
        try {
            // start the process
            proc = ProcessUtil.exec("mii-tool " + this.ifaceName);
            result = proc.waitFor() == 0 ? true : false;
            parse(proc);
            return result;
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
            String line = null;
            boolean isLinkDetected = false;
            int spd = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(this.ifaceName) && (line.indexOf("link ok") > -1)) {
                    isLinkDetected = true;
                    spd = parseLine(line);
                }
            }
            this.speed = spd;
            this.linkDetected = isLinkDetected;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }
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
