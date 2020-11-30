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
package org.eclipse.kura.web.server;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.UserManager;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtConsoleUserOptions;
import org.eclipse.kura.web.shared.model.GwtUserConfig;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtSessionServiceImpl extends OsgiRemoteServiceServlet implements GwtSessionService {

    private static final Logger logger = LoggerFactory.getLogger(GwtSessionServiceImpl.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private final UserManager userManager;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public GwtSessionServiceImpl(final UserManager userManager) {
        this.userManager = userManager;
    }

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

            logger.info("UI Logout - Success - Logout succeeded for user: {}, session {}", username, id);
            auditLogger.info("UI Logout - Success - Logout succeeded for user: {}, session {}", username, id);

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

    @Override
    public GwtUserConfig getUserConfig(final GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        if (session != null) {
            final Object username = session.getAttribute(Attributes.AUTORIZED_USER.getValue());
            return userManager.getUserConfig((String) username)
                    .orElseThrow(() -> new GwtKuraException(GwtKuraErrorCode.UNAUTHENTICATED));
        } else {
            throw new GwtKuraException(GwtKuraErrorCode.UNAUTHENTICATED);
        }
    }
}
