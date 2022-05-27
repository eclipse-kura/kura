/*******************************************************************************
 * Copyright (c) 2011, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.util.zip;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

public class UnZip {

    private static final int BUFFER = 1024;
    private static int tooBig = 0x6400000; // Max size of unzipped data, 100MB
    private static int tooMany = 1024;     // Max number of files

    private UnZip() {
        // Do nothing...
    }

    public static void unZipBytes(byte[] bytes, String outputFolder) throws IOException {
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes));
        unZipZipInputStream(zis, outputFolder);
    }

    public static void unZipFile(String filename, String outputFolder) throws IOException {
        File file = new File(filename);
        ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
        unZipZipInputStream(zis, outputFolder);
    }

    private static void unZipZipInputStream(ZipInputStream zis, String outFolder) throws IOException {
        String outputFolder = outFolder;
        if (outputFolder == null) {
            outputFolder = System.getProperty("user.dir");
        }

        File folder = new File(outputFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        int entries = 0;
        long total = 0;
        try {
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                byte[] buffer = new byte[BUFFER];

                String expectedFilePath = new StringBuilder(folder.getPath()).append(File.separator)
                        .append(ze.getName()).toString();
                File newFile = getFile(expectedFilePath, folder);

                if (ze.isDirectory()) {
                    newFile.mkdirs();
                    ze = zis.getNextEntry();
                    continue;
                }

                if (newFile.getParent() != null) {
                    File parent = new File(newFile.getParent());
                    parent.mkdirs();
                }

                try (FileOutputStream fos = new FileOutputStream(newFile)) {

                    int len = zis.read(buffer);
                    while (total + BUFFER <= tooBig && len > 0) {
                        fos.write(buffer, 0, len);
                        total += len;
                        len = zis.read(buffer);
                    }
                    fos.flush();
                }

                entries++;
                if (entries > tooMany) {
                    throw new IllegalStateException("Too many files to unzip.");
                }
                if (total + BUFFER > tooBig) {
                    throw new IllegalStateException("File being unzipped is too big.");
                }

                ze = zis.getNextEntry();
            }

            zis.closeEntry();
        } finally {
            if (zis != null) {
                zis.close();
            }
        }
    }

    private static File getFile(String expectedFilePath, File folder) throws IOException {
        String fileName;
        try {
            fileName = validateFileName(expectedFilePath, folder.getPath());
        } catch (KuraException e) {
            throw new IOException("File is outside extraction target directory.");
        }
        return new File(fileName);
    }

    private static String validateFileName(String zipFileName, String intendedDir) throws IOException, KuraException {
        File zipFile = new File(zipFileName);
        String filePath = zipFile.getCanonicalPath();

        File iD = new File(intendedDir);
        String canonicalID = iD.getCanonicalPath();

        if (filePath.startsWith(canonicalID)) {
            return filePath;
        } else {
            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
        }
    }

    public static boolean isZipCompressed(String filePath) throws IOException {
        byte b1 = 0;
        byte b2 = 0;

        try (InputStream is = new FileInputStream(filePath)) {
            b1 = (byte) is.read();
            b2 = (byte) is.read();
        } catch (IOException e) {
            throw new IOException(e);
        }

        return b1 == 0x50 && b2 == 0x4B;
    }

    public static boolean isZipCompressed(byte[] bytes) {
        if (bytes.length > 2) {
            return bytes[0] == 0x50 && bytes[1] == 0x4B;
        } else {
            return false;
        }
    }

}
