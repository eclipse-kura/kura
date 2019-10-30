/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.linux.command;

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.command.CommandService;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;

public class CommandServiceImpl implements CommandService {

    private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    private CommandExecutorService executorService;

    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    public void unsetExecutorService(CommandExecutorService executorService) {
        this.executorService = null;
    }

    @Override
    public String execute(final String cmd) throws KuraException {

        if (cmd == null || cmd.isEmpty()) {
            return "<empty command>";
        }

        try {
            final Path scriptFile = Files.createTempFile("script-", ".sh");
            try {
                createScript(scriptFile, cmd);
                return runScript(scriptFile);
            } finally {
                Files.deleteIfExists(scriptFile);
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, cmd);
        }
    }

    private void createScript(final Path scriptFile, final String cmd) throws IOException {
        try (final OutputStream fos = Files.newOutputStream(scriptFile); final PrintWriter pw = new PrintWriter(fos);) {
            pw.println("#!/bin/sh");
            pw.println();
            pw.println(cmd.replace("\r\n", "\n"));
        }
        Files.setPosixFilePermissions(scriptFile, EnumSet.of(OWNER_EXECUTE, OWNER_READ, OWNER_WRITE));
    }

    private String runScript(final Path scriptFile) {
        Command command = new Command(new String[] { scriptFile.toString() });
        command.setDirectory(TEMP_DIR.getAbsolutePath());
        command.setOutputStream(new ByteArrayOutputStream());
        command.setErrorStream(new ByteArrayOutputStream());
        CommandStatus status = this.executorService.execute(command);
        if ((Integer) status.getExitStatus().getExitValue() == 0) {
            return new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8);
        } else {
            return new String(((ByteArrayOutputStream) status.getErrorStream()).toByteArray(), Charsets.UTF_8);
        }

    }
}
