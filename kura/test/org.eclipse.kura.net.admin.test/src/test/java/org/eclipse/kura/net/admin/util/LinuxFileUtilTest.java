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
package org.eclipse.kura.net.admin.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;


public class LinuxFileUtilTest {

    @Test
    public void testNoDeletionNew() throws Exception {
        if (isWin()) {
            return;
        }

        String sourceFile = "/tmp/srcfile";
        File f = new File(sourceFile);
        f.createNewFile();

        String targetFile = "/tmp/targetfile";
        File tgt = new File(targetFile);

        assertFalse(tgt.exists());

        LinuxFileUtil.createSymbolicLink(sourceFile, targetFile, false);

        assertTrue(tgt.exists());

        f.delete();
        tgt.delete();
    }

    private boolean isWin() {
        return System.getProperty("os.name").contains("indows");
    }

    @Test
    public void testNoDeletionOld() throws Exception {
        String sourceFile = "/tmp/srcfile";
        File f = new File(sourceFile);
        Files.write(Paths.get(sourceFile), "test".getBytes());

        String targetFile = "/tmp/targetfile";
        File tgt = new File(targetFile);
        Files.write(Paths.get(targetFile), "target".getBytes());

        LinuxFileUtil.createSymbolicLink(sourceFile, targetFile, false);

        assertTrue(tgt.exists());
        List<String> lines = Files.readAllLines(Paths.get(targetFile));

        f.delete();
        tgt.delete();

        assertEquals(1, lines.size());
        assertEquals("target", lines.get(0));
    }

    @Test
    public void testDeleteOld() throws Exception {
        if (isWin()) {
            return;
        }

        String sourceFile = "/tmp/srcfile";
        File f = new File(sourceFile);
        Files.write(Paths.get(sourceFile), "test".getBytes());

        String targetFile = "/tmp/targetfile";
        File tgt = new File(targetFile);
        Files.write(Paths.get(targetFile), "target".getBytes());

        LinuxFileUtil.createSymbolicLink(sourceFile, targetFile);

        assertTrue(tgt.exists());

        List<String> lines = Files.readAllLines(Paths.get(targetFile));

        f.delete();
        tgt.delete();

        assertEquals(1, lines.size());
        assertEquals("test", lines.get(0));
    }

}
