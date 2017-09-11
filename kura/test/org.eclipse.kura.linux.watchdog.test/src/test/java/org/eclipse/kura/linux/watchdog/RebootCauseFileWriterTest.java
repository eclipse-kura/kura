/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.linux.watchdog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

public class RebootCauseFileWriterTest {

    @Test
    public void testWriteRebootCauseExistingFile() throws IOException {
        String path = "target/existingFile";
        RebootCauseFileWriter svc = new RebootCauseFileWriter(path);

        File f = new File(path);
        f.createNewFile();

        svc.writeRebootCause("test");

        assertEquals(0, f.length());

        f.delete();
    }

    @Test
    public void testWriteRebootCauseNonExistingFile() throws IOException {
        String path = "target/nonExistingFile";
        RebootCauseFileWriter svc = new RebootCauseFileWriter(path);

        File f = new File(path);

        svc.writeRebootCause("test");

        assertTrue(f.length() >= 18);

        FileReader reader = new FileReader(f);
        char[] cbuf = new char[100];
        int read = reader.read(cbuf);
        reader.close();

        f.delete();

        String s = new String(cbuf, 0, read);
        assertTrue(s.contains("test"));
    }

}
