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
package org.eclipse.kura.web.server.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkinServlet extends HttpServlet {

    private static final long serialVersionUID = -556598856721497972L;

    private static Logger logger = LoggerFactory.getLogger(SkinServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String resourceName = request.getPathInfo();

        File fResourceFile = null;
        try {
            fResourceFile = checkFile(resourceName);
        } catch (GwtKuraException | IOException e) {
            logger.warn("Failed to load skin resource, {}", e.getMessage());
        }

        if (fResourceFile == null) {
            try {
                response.sendError(404);
            } catch (final IOException e) {
                logger.warn("Failed to send response", e);
            }
        } else if (resourceName.endsWith(".css")) {
            response.setContentType("text/css");
            streamText(fResourceFile, response);
        } else if (resourceName.endsWith(".js")) {
            response.setContentType("text/javascript");
            streamText(fResourceFile, response);
        } else if (resourceName.endsWith(".jpg") || resourceName.endsWith(".png")) {
            response.setContentType("image/png");
            streamBinary(fResourceFile, response);
        } else if (resourceName.endsWith(".ico")) {
            response.setContentType("image/x-icon");
            streamBinary(fResourceFile, response);
        }

    }

    private void streamText(File fResourceFile, HttpServletResponse response) {
        try (FileReader fr = new FileReader(fResourceFile); PrintWriter w = response.getWriter();) {
            char[] buffer = new char[1024];
            int iRead = fr.read(buffer);
            while (iRead != -1) {
                w.write(buffer, 0, iRead);
                iRead = fr.read(buffer);
            }
        } catch (IOException e) {
            logger.error("Error loading skin resource", e);
        }
    }

    private void streamBinary(File fResourceFile, HttpServletResponse response) {
        try (FileInputStream in = new FileInputStream(fResourceFile); OutputStream o = response.getOutputStream();) {
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = in.read(buf)) >= 0) {
                o.write(buf, 0, len);
            }

        } catch (IOException e) {
            logger.error("Error loading skin resource", e);
        }
    }

    private File checkFile(String resourceName) throws GwtKuraException, IOException {
        SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);

        String styleDirectory = systemService.getKuraStyleDirectory();

        if (styleDirectory == null) {
            throw new GwtKuraException(null, null, "Style resource directory is not configured");
        }

        try (Stream<Path> kuraStyleDirStream = Files.list(Paths.get(styleDirectory));) {
            Optional<Path> fResourcePath = kuraStyleDirStream.filter(filePath -> filePath.toFile().isFile())
                    .filter(filePath -> filePath.toFile().getAbsolutePath().endsWith(resourceName)).findFirst();

            if (!fResourcePath.isPresent()) {
                throw new IOException("Resource File " + resourceName + " does not exist");
            }

            return fResourcePath.get().toFile();
        }
    }
}
