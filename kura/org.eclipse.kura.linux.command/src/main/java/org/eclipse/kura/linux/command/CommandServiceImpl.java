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
package org.eclipse.kura.linux.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.command.CommandService;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandServiceImpl implements CommandService {

    private static final Logger s_logger = LoggerFactory.getLogger(CommandServiceImpl.class);

    private static final String SCRIPT_FILE = System.getProperty("java.io.tmpdir") + File.separator + "runCmd.sh";

    private File m_scriptFile;

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------
    protected void activate() {
        s_logger.debug("Activating...");
    }

    protected void deactivate() {
        s_logger.debug("Deactivating...");
    }

    // ----------------------------------------------------------------
    //
    // Service APIs
    //
    // ----------------------------------------------------------------

    @Override
    public String execute(String cmd) throws KuraException {
        if (cmd == null) {
            s_logger.debug("null command");
            return "null command";
        }

        // Delete script file if it exists
        this.m_scriptFile = new File(SCRIPT_FILE);
        if (this.m_scriptFile.exists()) {
            try {
                this.m_scriptFile.delete();
            } catch (SecurityException se) {
                s_logger.error("File " + this.m_scriptFile + " cannot be deleted");
            }
        }

        // Create script file and set appropriate permissions
        createScript(cmd);
        setPermissions();

        // Run script
        String output = runScript();

        return output;
    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------
    private void createScript(String cmd) throws KuraException {
        try {
            cmd = "#!/bin/sh\n\n" + "cd " + System.getProperty("java.io.tmpdir") + "\n" + cmd;

            FileOutputStream fos = new FileOutputStream(this.m_scriptFile);
            PrintWriter pw = new PrintWriter(fos);
            pw.write(cmd);
            pw.write("\n");
            pw.flush();
            fos.getFD().sync();
            pw.close();
            fos.close();
        } catch (IOException e) {
            throw KuraException.internalError(e);
        }
    }

    private void setPermissions() throws KuraException {
        SafeProcess procChmod = null;
        SafeProcess procDos = null;

        try {
            procChmod = ProcessUtil.exec("chmod 700 " + this.m_scriptFile.toString());
            procChmod.waitFor();

            procDos = ProcessUtil.exec("dos2unix " + this.m_scriptFile.toString());
            procDos.waitFor();
        } catch (Exception e) {
            throw KuraException.internalError(e);
        } finally {
            if (procChmod != null) {
                ProcessUtil.destroy(procChmod);
            }
            if (procDos != null) {
                ProcessUtil.destroy(procDos);
            }
        }

    }

    private String runScript() throws KuraException {
        SafeProcess procUserScript = null;
        BufferedReader ibr = null;
        BufferedReader ebr = null;
        StringBuilder sb = new StringBuilder();
        try {
            procUserScript = ProcessUtil.exec("sh " + this.m_scriptFile.toString());
            procUserScript.waitFor();

            ibr = new BufferedReader(new InputStreamReader(procUserScript.getInputStream()));
            ebr = new BufferedReader(new InputStreamReader(procUserScript.getErrorStream()));

            BufferedReader br = null;
            if (procUserScript.exitValue() == 0) {
                br = ibr;
            } else {
                br = ebr;
            }

            String line = null;
            String newLine = "";
            while ((line = br.readLine()) != null) {
                sb.append(newLine);
                sb.append(line);
                newLine = "\n";
            }
        } catch (Exception e) {
            throw KuraException.internalError(e);
        } finally {
            if (ibr != null) {
                try {
                    ibr.close();
                } catch (IOException e) {
                    s_logger.warn("Cannot close process input stream", e);
                }
            }
            if (ebr != null) {
                try {
                    ebr.close();
                } catch (IOException e) {
                    s_logger.warn("Cannot close process error stream", e);
                }
            }
            if (procUserScript != null) {
                ProcessUtil.destroy(procUserScript);
            }
        }

        return sb.toString();
    }
}
