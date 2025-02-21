/*******************************************************************************
 * Copyright (c) 2019, 2025 Eurotech and/or its affiliates and others
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
import java.util.Objects;

import org.osgi.framework.FrameworkUtil;
import org.osgi.service.servlet.context.ServletContextHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class HttpServletContextHelper extends ServletContextHelper {

    private SecurityHandler securityHandler;

    public HttpServletContextHelper(SecurityHandler securityHandler) {
        super(FrameworkUtil.getBundle(HttpServletContextHelper.class));

        Objects.requireNonNull(securityHandler);
        this.securityHandler = securityHandler;
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

}
