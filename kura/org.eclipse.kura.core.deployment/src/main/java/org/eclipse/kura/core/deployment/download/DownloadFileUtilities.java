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

package org.eclipse.kura.core.deployment.download;

import java.io.File;
import java.io.IOException;

import org.eclipse.kura.core.deployment.install.DeploymentPackageInstallOptions;
import org.eclipse.kura.core.deployment.util.FileUtilities;

public class DownloadFileUtilities extends FileUtilities {

    // File Management
    public static File getDpDownloadFile(DeploymentPackageInstallOptions options) throws IOException {
        String downloadDirectory = options.getDownloadDirectory();
        String packageFilename;
        if (!options.getSystemUpdate()) {
            String dpName = FileUtilities.getFileName(options.getDpName(), options.getDpVersion(), ".dp", "_");
            packageFilename = new StringBuilder().append(downloadDirectory).append(File.separator).append(dpName)
                    .toString();
        } else {
            String shName = FileUtilities.getFileName(options.getDpName(), options.getDpVersion(), ".sh", "-");
            packageFilename = new StringBuilder().append(downloadDirectory).append(File.separator).append(shName)
                    .toString();
        }

        File localFolder = new File(downloadDirectory);
        String fileName = validateFileName(packageFilename, localFolder.getPath());
        return new File(fileName);
    }

    public static boolean deleteDownloadedFile(DeploymentPackageInstallOptions options) throws IOException {
        File file = getDpDownloadFile(options);

        if (file != null && file.exists() && file.isFile()) {
            return file.delete();
        }

        return false;
    }

    private static String validateFileName(String destFileName, String intendedDir) throws IOException {
        File destFile = new File(destFileName);
        String filePath = destFile.getCanonicalPath();

        File iD = new File(intendedDir);
        String canonicalID = iD.getCanonicalPath();

        if (filePath.startsWith(canonicalID)) {
            return filePath;
        } else {
            throw new IOException();
        }
    }
}
