/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.admin.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxFileUtil {

    private static final Logger logger = LoggerFactory.getLogger(LinuxFileUtil.class);
    
    private LinuxFileUtil() {
        
    }

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

        logger.debug("Creating symbolic link from {} to {}", targetFile, sourceFile);

        File f = new File(targetFile);
        if (f.exists() && deleteOldLink) {
            logger.debug("Deleting existing link");
            f.delete();
        }

        if (!f.exists()) {
            Files.createSymbolicLink(Paths.get(targetFile), Paths.get(sourceFile));
        }
    }
}
