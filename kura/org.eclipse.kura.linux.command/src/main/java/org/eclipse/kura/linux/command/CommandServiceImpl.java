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
 *     Jens Reimann <jreimann@redhat.com> Fix concurrency issue with scripts
 *     		Clean up script processing to not rely on command line tools
 *******************************************************************************/
package org.eclipse.kura.linux.command;

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.command.CommandService;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandServiceImpl implements CommandService {

	private static final Logger s_logger = LoggerFactory.getLogger(CommandServiceImpl.class);
	private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));

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

	public String execute(final String cmd) throws KuraException {
		if (cmd == null) {
			s_logger.debug("null command");
			return "null command";
		}

		// Delete script file if it exists
		try {
			final Path scriptFile = Files.createTempFile("script-", ".sh");
			try {
				createScript(scriptFile, cmd);
				return runScript(scriptFile);
			} finally {
				Files.deleteIfExists(scriptFile);
			}
		} catch (Exception e) {
			throw KuraException.internalError(e, "Failed to execute command");
		}
	}

	// ----------------------------------------------------------------
	//
	// Private Methods
	//
	// ----------------------------------------------------------------
	private void createScript(final Path scriptFile, final String cmd) throws IOException {
		try (final OutputStream fos = Files.newOutputStream(scriptFile); final PrintWriter pw = new PrintWriter(fos);) {
			pw.println("#!/bin/sh");
			pw.println();
			pw.println(cmd.replace("\r\n", "\n"));
		}
		Files.setPosixFilePermissions(scriptFile, EnumSet.of(OWNER_EXECUTE, OWNER_READ, OWNER_WRITE));
	}

	private String runScript(final Path scriptFile) throws KuraException {
		SafeProcess procUserScript = null;
		final StringBuilder sb = new StringBuilder();
		try {
			procUserScript = ProcessUtil.exec(TEMP_DIR, scriptFile.toAbsolutePath().toString());
			procUserScript.waitFor();

			try (BufferedReader ibr = new BufferedReader(new InputStreamReader(procUserScript.getInputStream()));
					BufferedReader ebr = new BufferedReader(new InputStreamReader(procUserScript.getErrorStream()));) {

				final BufferedReader br;
				if (procUserScript.exitValue() == 0) {
					br = ibr;
				} else {
					br = ebr;
				}

				String line;
				String newLine = "";
				while ((line = br.readLine()) != null) {
					sb.append(newLine);
					sb.append(line);
					newLine = "\n";
				}
			}
		} catch (Exception e) {
			throw KuraException.internalError(e);
		} finally {
			if (procUserScript != null)
				ProcessUtil.destroy(procUserScript);
		}

		return sb.toString();
	}
}
