/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.system;

import java.io.ByteArrayOutputStream;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Superclass of SystemService.
 */
@SuppressWarnings("checkstyle:hideUtilityClassConstructor")
public class SuperSystemService {

    private static final Logger logger = LoggerFactory.getLogger(SuperSystemService.class);

    protected static String runSystemCommand(String[] commandLine, boolean runInShell,
            CommandExecutorService executorService) {
        String response = "";
        Command command = new Command(commandLine);
        command.setTimeout(60);
        command.setOutputStream(new ByteArrayOutputStream());
        command.setExecuteInAShell(runInShell);
        CommandStatus status = executorService.execute(command);
        if (status.getExitStatus().isSuccessful()) {
            response = new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8);
        } else {
            if (logger.isErrorEnabled()) {
                logger.error("failed to run commands {}", String.join(" ", commandLine));
            }
        }
        return response;
    }

    protected static String runSystemCommand(String commandLine, boolean runInShell,
            CommandExecutorService executorService) {
        return runSystemCommand(commandLine.split("\\s+"), runInShell, executorService);
    }

}