/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server.servlet;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogServlet extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 3969980124054250070L;

    private static Logger logger = LoggerFactory.getLogger(LogServlet.class);

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        String logDir = "/var/log";
        String oldLogsDir = "/var/old_logs";

        List<String> paths = new ArrayList<>();
        paths.add(logDir);
        paths.add(oldLogsDir);

        List<File> fileList = new ArrayList<>();
        paths.stream().forEach(path -> {
            try (Stream<Path> kuraLogDirStream = Files.list(Paths.get(path));) {
                fileList.addAll(kuraLogDirStream.filter(filePath -> filePath.toFile().isFile()).map(Path::toFile)
                        .collect(Collectors.toList()));
            } catch (IOException e) {
                logger.warn("Unable to fetch log files");
            }
        });

        try {
            byte[] zip = zipFiles(fileList);
            ServletOutputStream sos = httpServletResponse.getOutputStream();
            httpServletResponse.setContentType("application/zip");
            httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"Kura_Logs.zip\"");

            sos.write(zip);
            sos.flush();
        } catch (IOException e) {
            logger.warn("Unable to create zip file containing log resources");
        }

    }

    private byte[] zipFiles(List<File> files) throws IOException {
        byte[] bytes = new byte[2048];

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos);) {
            for (File file : files) {
                zipFile(bytes, zos, file);
            }
            zos.flush();
            baos.flush();
            return baos.toByteArray();
        }

    }

    private void zipFile(byte[] bytes, ZipOutputStream zos, File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file.getCanonicalPath());
                BufferedInputStream bis = new BufferedInputStream(fis);) {

            zos.putNextEntry(new ZipEntry(file.getName()));

            int bytesRead;
            while ((bytesRead = bis.read(bytes)) != -1) {
                zos.write(bytes, 0, bytesRead);
            }
            zos.closeEntry();
        }
    }

}
