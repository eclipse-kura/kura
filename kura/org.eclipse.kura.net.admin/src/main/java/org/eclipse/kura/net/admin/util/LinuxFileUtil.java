/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxFileUtil {

    private static final Logger s_logger = LoggerFactory.getLogger(LinuxFileUtil.class);

    /*
     * This method creates symbolic link, deleting existing link by default
     */
    public static void createSymbolicLink(String sourceFile, String targetFile) throws Exception {
        createSymbolicLink(sourceFile, targetFile, true);
    }

    /*
     * This method creates symbolic link
     */
    public static void createSymbolicLink(String sourceFile, String targetFile, boolean deleteOldLink)
            throws Exception {

        s_logger.debug("Creating symbolic link from " + targetFile + " to " + sourceFile);

        File f = new File(targetFile);
        if (f.exists() && deleteOldLink) {
            s_logger.debug("Deleting existing link");
            f.delete();
        }

        if (!f.exists()) {
            Files.createSymbolicLink(Paths.get(targetFile), Paths.get(sourceFile));
        }
    }
}
