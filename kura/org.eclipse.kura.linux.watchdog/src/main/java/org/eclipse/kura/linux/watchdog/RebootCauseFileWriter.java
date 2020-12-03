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
 *******************************************************************************/
package org.eclipse.kura.linux.watchdog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RebootCauseFileWriter {

    private static final Logger logger = LoggerFactory.getLogger(RebootCauseFileWriter.class);

    private final File rebootCauseFile;

    public RebootCauseFileWriter(String rebootCauseFilePath) {
        this.rebootCauseFile = new File(rebootCauseFilePath).getAbsoluteFile();
    }

    public void writeRebootCause(String cause) {
        final File rebootCauseFileDir = this.rebootCauseFile.getParentFile();

        if (this.rebootCauseFile.exists()) {
            logger.info("Reboot cause file {} already exists, not updating..", this.rebootCauseFile);
            return;
        }
        if (rebootCauseFileDir == null) {
            logger.warn("failed to determine reboot cause file parent directory", rebootCauseFileDir);
            return;
        }
        rebootCauseFileDir.mkdirs();
        if (!rebootCauseFileDir.isDirectory()) {
            logger.warn("failed to create reboot cause file parent directory", rebootCauseFileDir);
            return;
        }

        final long timestamp = System.currentTimeMillis();
        final File tmpFile = new File(this.rebootCauseFile.getPath() + '.' + timestamp);

        try {
            logger.info("Writing reboot cause file...");
            writeFile(tmpFile, timestamp, cause);
            Files.move(tmpFile.toPath(), this.rebootCauseFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
            logger.info("Writing reboot cause file...done");
        } catch (Exception e) {
            logger.warn("failed to write reboot cause file", e);
        } finally {
            tmpFile.delete();
        }
    }

    private void writeFile(File file, long timestamp, String rebootCause) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file); PrintStream writer = new PrintStream(out)) {
            writer.println(timestamp);
            writer.println(rebootCause);
            writer.flush();
            out.getFD().sync();
        }
    }
}
