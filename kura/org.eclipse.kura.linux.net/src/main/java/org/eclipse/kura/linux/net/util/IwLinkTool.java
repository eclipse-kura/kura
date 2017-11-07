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

public class IwLinkTool implements LinkTool {

    private static final Logger logger = LoggerFactory.getLogger(IwLinkTool.class);

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
    public IwLinkTool(String ifaceName) {
        this.interfaceName = ifaceName;
        this.duplex = "half";
    }

    @Override
    public boolean get() throws KuraException {
        SafeProcess proc = null;
        try {
            proc = ProcessUtil.exec(formIwLinkCommand(this.interfaceName));
            if (proc.waitFor() != 0) {
                logger.warn("The iw returned with exit value {}", proc.exitValue());
                return false;
            }
            return parse(proc);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    private boolean parse(SafeProcess proc) throws KuraException {
        boolean ret = true;
        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            String line = null;
            boolean proceed = true;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Not connected")) {
                    proceed = false;
                }
                if (!parseLine(line)) {
                    ret = false;
                    proceed = false;
                }
                if (!proceed) {
                    break;
                }
            }

        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }
        return ret;
    }

    private boolean parseLine(String line) {
        if (line.contains("signal:")) {
            // e.g.: signal: -55 dBm
            String[] parts = line.split("\\s");
            try {
                int sig = Integer.parseInt(parts[1]);
                if (sig > -100) {     // TODO: adjust this threshold?
                    this.signal = sig;
                    this.linkDetected = true;
                }
            } catch (NumberFormatException e) {
                logger.debug("Could not parse '{}' as int in line: {}", parts[1], line);
                return false;
            }
        } else if (line.contains("tx bitrate:")) {
            // e.g.: tx bitrate: 1.0 MBit/s
            String[] parts = line.split("\\s");
            try {
                double bitrate = Double.parseDouble(parts[2]);
                if ("MBit/s".equals(parts[3])) {
                    bitrate *= 1000000;
                }
                this.speed = (int) Math.round(bitrate);
            } catch (NumberFormatException e) {
                logger.debug("Could not parse '{}' as double in line: {}", parts[2], line);
                return false;
            }
        }
        return true;
    }

    private String formIwLinkCommand(String ifaceName) {
        StringBuilder sb = new StringBuilder("iw ");
        sb.append(ifaceName).append(" link");
        return sb.toString();
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
