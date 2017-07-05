/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
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
