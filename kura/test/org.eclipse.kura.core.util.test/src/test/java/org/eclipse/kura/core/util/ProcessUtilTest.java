/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProcessUtilTest {

    private static String TEMP_SCRIPT_FILE_PATH = "/tmp/kura_test_script_ProcessUtilTest";

    private static boolean win = System.getProperty("os.name").contains("indows");

    @BeforeClass
    public static void setup() {
        if (win) {
            TEMP_SCRIPT_FILE_PATH += ".bat";

            File f = new File("/tmp");
            if (!f.exists()) {
                f.mkdirs();
                f.deleteOnExit();
            }
        }
    }

    @Test
    public void testExecString() throws Exception {
        int[] values = new int[] { 0, 1, 2 };
        String command = "/bin/sh " + TEMP_SCRIPT_FILE_PATH;
        if (win) {
            command = TEMP_SCRIPT_FILE_PATH;
        }
        File file = new File(TEMP_SCRIPT_FILE_PATH);
        file.deleteOnExit();

        for (int value : values) {
            String stdout;
            String stderr;
            int errorCode = -1;

            try {
                try (PrintWriter out = new PrintWriter(file)) {
                    if (win) {
                        out.println("@echo off");
                    }
                    out.println("echo stdout");
                    out.println("echo stderr 1>&2");
                    out.println("exit " + value);
                }

                SafeProcess process = ProcessUtil.exec(command);

                errorCode = process.exitValue();
                stdout = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
                stderr = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw e;
            } finally {
                file.delete();
            }

            assertEquals(value, errorCode);
            assertEquals("stdout", stdout.trim());
            assertEquals("stderr", stderr.trim());
        }
    }

    @Test
    public void testExecStringArray() throws Exception {
        int[] values = new int[] { 0, 1, 2 };
        String[] commandArray = { "/bin/sh", TEMP_SCRIPT_FILE_PATH };
        if (win) {
            commandArray = new String[] { TEMP_SCRIPT_FILE_PATH };
        }
        File file = new File(TEMP_SCRIPT_FILE_PATH);
        file.deleteOnExit();

        for (int value : values) {
            String stdout;
            String stderr;
            int errorCode = -1;

            try {
                try (PrintWriter out = new PrintWriter(file)) {
                    if (win) {
                        out.println("@echo off");
                    }
                    out.println("echo stdout");
                    out.println("echo stderr 1>&2");
                    out.println("exit " + value);
                }

                SafeProcess process = ProcessUtil.exec(commandArray);

                errorCode = process.exitValue();
                stdout = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
                stderr = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw e;
            } finally {
                file.delete();
            }

            assertEquals(value, errorCode);
            assertEquals("stdout", stdout.trim());
            assertEquals("stderr", stderr.trim());
        }
    }

    @Test
    public void testDestroy() throws Exception {
        // First execute the process
        String[] commandArray = { "/bin/sh", TEMP_SCRIPT_FILE_PATH };
        if (win) {
            commandArray = new String[] { TEMP_SCRIPT_FILE_PATH };
        }
        File file = new File(TEMP_SCRIPT_FILE_PATH);
        file.deleteOnExit();

        SafeProcess process;

        try {
            try (PrintWriter out = new PrintWriter(file)) {
                if (win) {
                    out.println("@echo off");
                }
                out.println("echo stdout");
                out.println("echo stderr 1>&2");
                out.println("exit 0");
            }

            process = ProcessUtil.exec(commandArray);
        } catch (Exception e) {
            throw e;
        } finally {
            file.delete();
        }

        String stdout = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        String stderr = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);

        assertEquals("stdout", stdout.trim());
        assertEquals("stderr", stderr.trim());

        // Then destroy it
        ProcessUtil.destroy(process);

        assertNull(TestUtil.getFieldValue(process, "inBytes"));
        assertNull(TestUtil.getFieldValue(process, "errBytes"));
        assertNull(TestUtil.getFieldValue(process, "process"));
    }
}
