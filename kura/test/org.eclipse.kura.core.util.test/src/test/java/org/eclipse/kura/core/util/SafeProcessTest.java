/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SafeProcessTest {
	private static final String TEMP_SCRIPT_FILE_PATH = "/tmp/kura_test_script_SafeProcessTest";

	@Test
	public void testExec() throws Exception {
		int[] values = new int[]{0, 1, 2};
		String[] commandArray = {"/bin/sh", TEMP_SCRIPT_FILE_PATH};
		File file = new File(TEMP_SCRIPT_FILE_PATH);
		file.deleteOnExit();

		for (int expectedErrorCode : values) {
			String stdout;
			String stderr;
			int errorCode = -1;

			try {
				try (PrintWriter out = new PrintWriter(file)) {
					out.println("echo stdout");
					out.println("echo stderr 1>&2");
					out.println("exit " + expectedErrorCode);
				}

				SafeProcess process = new SafeProcess();
				process.exec(commandArray);

				errorCode = process.exitValue();
				stdout = IOUtils.toString(process.getInputStream(),
						StandardCharsets.UTF_8);
				stderr = IOUtils.toString(process.getErrorStream(),
						StandardCharsets.UTF_8);
			} catch (Exception e) {
				throw e;
			} finally {
				file.delete();
			}

			assertEquals(expectedErrorCode, errorCode);
			assertEquals("stdout\n", stdout);
			assertEquals("stderr\n", stderr);
		}
	}

	@Test
	public void testDestroy() throws Exception {
		// First execute the process
		String[] commandArray = {"/bin/sh", TEMP_SCRIPT_FILE_PATH};
		File file = new File(TEMP_SCRIPT_FILE_PATH);
		file.deleteOnExit();

		SafeProcess process = new SafeProcess();

		try {
			try (PrintWriter out = new PrintWriter(file)) {
				out.println("echo stdout");
				out.println("echo stderr 1>&2");
				out.println("exit 0");
			}

			process.exec(commandArray);
		} catch (Exception e) {
			throw e;
		} finally {
			file.delete();
		}

		String stdout = IOUtils.toString(process.getInputStream(),
				StandardCharsets.UTF_8);
		String stderr = IOUtils.toString(process.getErrorStream(),
				StandardCharsets.UTF_8);

		assertEquals("stdout\n", stdout);
		assertEquals("stderr\n", stderr);

		// Then destroy it 
		process.destroy();

		assertNull(TestUtil.getFieldValue(process, "m_inBytes"));
		assertNull(TestUtil.getFieldValue(process, "m_errBytes"));
		assertNull(TestUtil.getFieldValue(process, "m_process"));
	}
}
