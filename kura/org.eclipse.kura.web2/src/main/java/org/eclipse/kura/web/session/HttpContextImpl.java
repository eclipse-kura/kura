/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.service.http.HttpContext;

public class HttpContextImpl implements HttpContext {

    private final SecurityHandler securityHandler;
    private final HttpContext delegate;

    public HttpContextImpl(SecurityHandler securityHandler, HttpContext delegate) {
        this.securityHandler = securityHandler;
        this.delegate = delegate;
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (handleSecurityInternal(request, response)) {
            return true;
        }

        final HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return false;
    }

    private boolean handleSecurityInternal(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            return this.securityHandler.handleSecurity(request, response);
        } catch (final Exception e) {
            return false;
        }
    }

    @Override
    public URL getResource(String name) {
        return this.delegate.getResource(name);
    }

    @Override
    public String getMimeType(String name) {
        return this.delegate.getMimeType(name);
    }

}
