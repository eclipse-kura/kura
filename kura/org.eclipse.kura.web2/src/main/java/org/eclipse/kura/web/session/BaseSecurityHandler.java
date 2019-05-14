/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.session;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BaseSecurityHandler implements SecurityHandler {

    private final String appRoot;

    public BaseSecurityHandler(final String appRoot) {
        this.appRoot = appRoot;
    }

    @Override
    public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        response.setHeader("X-FRAME-OPTIONS", "SAMEORIGIN");
        response.setHeader("X-XSS-protection", "1; mode=block");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "no-cache,no-store,must-revalidate");
        response.setHeader("Pragma", "no-cache");

        // If a trailing "/" is used when accesssing the app, redirect
        if (request.getRequestURI().equals(this.appRoot + "/")) {
            response.sendRedirect(this.appRoot);
            return false;
        }

        // If using root context, redirect
        if (request.getRequestURI().equals("/")) {
            response.sendRedirect(this.appRoot);
            return false;
        }

        return true;
    }

}
