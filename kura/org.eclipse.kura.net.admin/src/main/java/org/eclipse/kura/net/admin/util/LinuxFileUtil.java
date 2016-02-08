/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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

import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxFileUtil {
    
    private static final Logger s_logger = LoggerFactory.getLogger(LinuxFileUtil.class);
    
    /*
     * This method creates symbolic link, deleting existing link by default
     */
    public static void createSymbolicLink(String sourceFile, String targetFile)
            throws Exception {
        createSymbolicLink(sourceFile, targetFile, true);
    }
    
    /*
     * This method creates symbolic link
     */
    public static void createSymbolicLink(String sourceFile, String targetFile, boolean deleteOldLink)
            throws Exception {

        s_logger.debug("Creating symbolic link from " + targetFile + " to " + sourceFile);
        
        File f = new File(targetFile);
        if (f.exists()) {
            if (deleteOldLink) {
                s_logger.debug("Deleting existing link");
                f.delete();
            }
        }
    
        if (!f.exists()) {
            StringBuffer buf = new StringBuffer("ln -s");
            buf.append(' ');
            buf.append(sourceFile);
            buf.append(' ');
            buf.append(targetFile);

            int ret = LinuxProcessUtil.start(buf.toString());
            if (ret != 0) {
                throw new Exception("error executing command - "
                        + buf.toString());
            }
        }
    }
}
