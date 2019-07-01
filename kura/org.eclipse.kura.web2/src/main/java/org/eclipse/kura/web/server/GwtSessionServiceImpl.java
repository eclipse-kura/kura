/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.server;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtConsoleUserOptions;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtSessionServiceImpl extends OsgiRemoteServiceServlet implements GwtSessionService {

    private static final Logger logger = LoggerFactory.getLogger(GwtSessionServiceImpl.class);

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void logout(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpServletResponse response = getThreadLocalResponse();

        final HttpSession session = request.getSession(false);

        if (session != null) {
            final Object username = session.getAttribute(Attributes.AUTORIZED_USER.getValue());
            final String id = session.getId();
            session.invalidate();

            logger.warn("UI Logout - Success - Logout succeeded for user: {}, session {}", username, id);

            Cookie[] cookies = request.getCookies();
            for (Cookie cookie : cookies) {
                cookie.setMaxAge(0);
                cookie.setValue(null);
                cookie.setPath("/");
                response.addCookie(cookie);
            }
        }
    }

    @Override
    public GwtConsoleUserOptions getUserOptions(final GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        return Console.getConsoleOptions().getUserOptions();
    }
}
