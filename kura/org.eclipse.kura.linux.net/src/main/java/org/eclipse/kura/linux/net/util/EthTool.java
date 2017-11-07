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

/**
 * Defines ethtool utility
 *
 * @author ilya.binshtok
 *
 */
public class EthTool implements LinkTool {

    private static final Logger logger = LoggerFactory.getLogger(LinuxNetworkUtil.class);

    private static final String LINK_DETECTED = "Link detected:";
    private static final String LINK_DUPLEX = "Duplex:";
    private static final String LINK_SPEED = "Speed:";

    private String ifaceName = null;
    private boolean linkDetected = false;
    private int speed = 0; // in b/s
    private String duplex = null;

    /**
     * ethtool constructor
     *
     * @param ifaceName
     *            - interface name as {@link String}
     */
    public EthTool(String ifaceName) {
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
            proc = ProcessUtil.exec("ethtool " + this.ifaceName);
            result = proc.waitFor() == 0 ? true : false;
            parse(proc);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }

        return result;
    }

    private void parse(SafeProcess proc) throws KuraException {
        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            String line = null;
            int ind = -1;
            while ((line = br.readLine()) != null) {
                if ((ind = line.indexOf(LINK_DETECTED)) >= 0) {
                    logger.trace("Link detected from: {}", line);
                    line = line.substring(ind + LINK_DETECTED.length()).trim();
                    this.linkDetected = line.compareTo("yes") == 0 ? true : false;
                } else if ((ind = line.indexOf(LINK_DUPLEX)) >= 0) {
                    this.duplex = line.substring(ind + LINK_DUPLEX.length()).trim();
                } else if ((ind = line.indexOf(LINK_SPEED)) >= 0) {
                    line = line.substring(ind + LINK_SPEED.length()).trim();
                    setSpeed(line);
                }
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
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
