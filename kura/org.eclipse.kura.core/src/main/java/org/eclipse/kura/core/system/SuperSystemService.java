/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Superclass of System*Service.
 */
public class SuperSystemService {

    private static final Logger logger = LoggerFactory.getLogger(SuperSystemService.class);

    protected String runSystemCommand(String command) {
        return runSystemCommand(command.split("\\s+"));
    }

    protected static String runSystemCommand(String[] commands) {
        StringBuffer response = new StringBuffer();
        SafeProcess proc = null;
        BufferedReader br = null;
        try {
            proc = ProcessUtil.exec(commands);
            proc.waitFor();
            br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = null;
            String newLine = "";
            while ((line = br.readLine()) != null) {
                response.append(newLine);
                response.append(line);
                newLine = "\n";
            }
        } catch (Exception e) {
            StringBuilder command = new StringBuilder();
            String delim = "";
            for (String command2 : commands) {
                command.append(delim);
                command.append(command2);
                delim = " ";
            }
            logger.error("failed to run commands " + command.toString(), e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    logger.error("I/O Exception while closing BufferedReader!");
                }
            }
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
        return response.toString();
    }

}