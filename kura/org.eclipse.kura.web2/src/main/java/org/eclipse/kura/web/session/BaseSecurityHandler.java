/*******************************************************************************
 * Copyright (c) 2019, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.session;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.kura.web.Console;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseSecurityHandler implements SecurityHandler {

    private static final Logger logger = LoggerFactory.getLogger(BaseSecurityHandler.class);

    @Override
    public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        if (!Console.getConsoleOptions().isPortAllowed(request.getLocalPort())) {
            response.sendError(404);
            return false;
        }

        response.setHeader("X-FRAME-OPTIONS", "SAMEORIGIN");
        response.setHeader("X-XSS-protection", "1; mode=block");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "no-cache,no-store,must-revalidate");
        response.setHeader("Pragma", "no-cache");

        fixTrailingSlashes(request, response);

        return true;
    }

    private static void fixTrailingSlashes(final HttpServletRequest request, final HttpServletResponse response) {

        final String path = request.getRequestURI();

        if (!path.endsWith("/") || "/".contentEquals(path) || path.isEmpty()) {
            return;
        }

        int end = path.length() - 1;
        for (; end > 1 && path.charAt(end) == '/'; end--) {
            ;
        }

        try {
            response.sendRedirect(path.substring(0, end + 1));
        } catch (IOException e) {
            logger.warn("unexpected exception", e);
        }
    }
}
