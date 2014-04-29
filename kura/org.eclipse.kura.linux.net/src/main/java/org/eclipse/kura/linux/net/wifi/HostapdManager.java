/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.linux.net.wifi;

import java.io.File;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostapdManager {
    
    private static Logger s_logger = LoggerFactory.getLogger(HostapdManager.class);
    
    private static final File CONFIG_FILE = new File("/etc/hostapd.conf");
    private static final String HOSTAPD_EXEC = "hostapd";

    public static void start() throws KuraException {
        Process proc = null;
        
        if(!CONFIG_FILE.exists()) {
            throw KuraException.internalError("Config file does not exist: " + CONFIG_FILE.getAbsolutePath());
        }
        
        try {
            if(HostapdManager.isRunning()) {
                stop();
            }
            
            //start hostapd
            String launchHostapdCommand = generateCommand();
            s_logger.debug("starting hostapd --> " + launchHostapdCommand);
            proc = ProcessUtil.exec(launchHostapdCommand);
            if(proc.waitFor() != 0) {
                s_logger.error("failed to start hostapd for unknown reason");
                throw KuraException.internalError("failed to start hostapd for unknown reason");
            }
            Thread.sleep(1000);
        } catch(Exception e) {
            throw KuraException.internalError(e);
        }
        finally {
            ProcessUtil.destroy(proc);
        }
    }
    
    public static void stop() throws KuraException {
        Process proc = null;
        try {
            //kill hostapd
            s_logger.debug("stopping hostapd");
            proc = ProcessUtil.exec("killall hostapd");
            proc.waitFor();
            Thread.sleep(1000);
        } catch(Exception e) {
            throw KuraException.internalError(e);
        }
        finally {
            ProcessUtil.destroy(proc);
        }
    }

    public static boolean isRunning() throws KuraException {
        try {
            // Check if hostapd is running
            int pid = LinuxProcessUtil.getPid(generateCommand());
            return (pid > -1);
        } catch (Exception e) {
            throw KuraException.internalError(e);
        }
    }
    
    private static String generateCommand() {
        StringBuilder cmd = new StringBuilder(HOSTAPD_EXEC);
        cmd.append(" -B ").append(CONFIG_FILE.getAbsolutePath());
        
        return cmd.toString();
    }
}
