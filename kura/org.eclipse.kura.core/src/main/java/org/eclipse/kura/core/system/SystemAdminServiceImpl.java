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
package org.eclipse.kura.core.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.system.SystemAdminService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemAdminServiceImpl extends SuperSystemService implements SystemAdminService {

    private static final Logger s_logger = LoggerFactory.getLogger(SystemAdminServiceImpl.class);

    private static final String OS_LINUX = "Linux";
    private static final String OS_MAC_OSX = "Mac OS X";
    private static final String OS_WINDOWS = "windows";
    private static final String UNKNOWN = "UNKNOWN";

    @SuppressWarnings("unused")
    private ComponentContext m_ctx;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {
        //
        // save the bundle context
        this.m_ctx = componentContext;
    }

    protected void deactivate(ComponentContext componentContext) {
        this.m_ctx = null;
    }

    // ----------------------------------------------------------------
    //
    // Service APIs
    //
    // ----------------------------------------------------------------

    @Override
    public String getUptime() {

        String uptimeStr = UNKNOWN;
        long uptime = 0;

        if (getOsName().toLowerCase().startsWith(OS_WINDOWS)) {
            try {
                String[] lastBootUpTime = runSystemCommand("wmic os get LastBootUpTime ").split("\n");
                if (lastBootUpTime[0].toLowerCase().startsWith("lastbootuptime")) {
                    String lastBoot = lastBootUpTime[2];
                    DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                    Date bootDate = df.parse(lastBoot);
                    uptime = System.currentTimeMillis() - bootDate.getTime();
                    uptimeStr = Long.toString(uptime);
                }
            } catch (Exception e) {
                uptimeStr = "0";
                s_logger.error("Could not read uptime", e);
            }
        } else if (OS_LINUX.equals(getOsName())) {
            File file;
            FileReader fr = null;
            BufferedReader br = null;
            try {
                file = new File("/proc/uptime");
                fr = new FileReader(file);
                br = new BufferedReader(fr);

                String line = br.readLine();
                if (line != null) {
                    uptime = (long) (Double.parseDouble(line.substring(0, line.indexOf(" "))) * 1000);
                    uptimeStr = Long.toString(uptime);
                }
            } catch (Exception e) {
                s_logger.error("Could not read uptime", e);
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                    }
                }
                if (fr != null) {
                    try {
                        fr.close();
                    } catch (IOException e) {
                    }
                }
            }
        } else if (OS_MAC_OSX.equals(getOsName())) {
            try {
                String systemUptime = runSystemCommand("uptime");
                if (!systemUptime.isEmpty()) {
                    String[] uptimeParts = systemUptime.split("up\\s+")[1].split("\\s*,\\s*");
                    int days = 0, hours = 0, mins = 0;

                    String uptimePart = uptimeParts[0];

                    // If up less than a day, it will only show the number of mins, hr, or HH:MM
                    if (uptimePart.contains("days")) {
                        days = Integer.parseInt(uptimePart.split("\\s+days")[0]);
                        uptimePart = uptimeParts[1];
                    } else if (uptimePart.contains("day")) {
                        days = Integer.parseInt(uptimePart.split("\\s+day")[0]);
                        uptimePart = uptimeParts[1];
                    }

                    if (uptimePart.contains(":")) {
                        // Showing HH:MM
                        hours = Integer.parseInt(uptimePart.split(":")[0]);
                        mins = Integer.parseInt(uptimePart.split(":")[1]);
                    } else if (uptimePart.contains("hr")) {
                        // Only showing hr
                        hours = Integer.parseInt(uptimePart.split("\\s*hr")[0]);
                    } else if (uptimePart.contains("mins")) {
                        // Only showing mins
                        mins = Integer.parseInt(uptimePart.split("\\s*mins")[0]);
                    } else {
                        s_logger.error("uptime could not be parsed correctly: " + uptimeParts[0]);
                    }

                    uptime = (long) ((days * 24 + hours) * 60 + mins) * 60;
                    uptimeStr = Long.toString(uptime * 1000);
                }
            } catch (Exception e) {
                s_logger.error("Could not parse uptime", e);
            }
        }
        return uptimeStr;
    }

    @Override
    public void reboot() {
        String cmd = "";
        if (OS_LINUX.equals(getOsName()) || OS_MAC_OSX.equals(getOsName())) {
            cmd = "reboot";
        } else if (getOsName().toLowerCase().startsWith(OS_WINDOWS)) {
            cmd = "shutdown -r";
        } else {
            s_logger.error("Unsupported OS for reboot()");
            return;
        }
        SafeProcess proc = null;
        try {
            proc = ProcessUtil.exec(cmd);
            proc.waitFor();
        } catch (Exception e) {
            s_logger.error("failed to issue reboot", e);
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    @Override
    public void sync() {
        String cmd = "";
        if (OS_LINUX.equals(getOsName()) || OS_MAC_OSX.equals(getOsName())) {
            cmd = "sync";
        } else {
            s_logger.error("Unsupported OS for sync()");
            return;
        }
        SafeProcess proc = null;
        try {
            proc = ProcessUtil.exec(cmd);
            int status = proc.waitFor();
            if (status != 0) {
                s_logger.error("sync command failed with exit code of " + status);
            }
        } catch (Exception e) {
            s_logger.error("failed to issue sync command", e);
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    private String getOsName() {
        return System.getProperty("os.name");
    }
}
