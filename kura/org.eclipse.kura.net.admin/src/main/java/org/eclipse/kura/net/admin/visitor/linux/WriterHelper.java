/*******************************************************************************
 * Copyright (c) 2018, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux;

import static java.nio.file.Files.setPosixFilePermissions;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static java.util.EnumSet.of;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.kura.KuraException;

public final class WriterHelper {

    private WriterHelper() {
    }

    /*
     * This method copies supplied String to a file
     */
    public static void copyFile(String data, final Path destination) throws KuraException {

        data = data.replaceAll("\r\n", "\n");

        try {
            try (Writer writer = Files.newBufferedWriter(destination)) {
                writer.write(data);
            }
            setPermissions(destination);
        } catch (IOException e) {
            throw KuraException.internalError(e);
        }
    }

    /*
     * This method sets permissions to hostapd configuration file
     */
    private static void setPermissions(Path fileName) throws IOException {
        setPosixFilePermissions(fileName, of(OWNER_READ, OWNER_WRITE));
    }

}
