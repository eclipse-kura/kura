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

import static java.util.Objects.isNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.UserManager;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.service.GwtPasswordAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtPasswordAuthenticationServiceImpl extends OsgiRemoteServiceServlet
        implements GwtPasswordAuthenticationService {

    private static final String UI_LOGIN_FAILURE_MESSAGE = "UI Login - Failure - Login failed for user: {}, request IP: {}";

    private static final Logger logger = LoggerFactory.getLogger(GwtPasswordAuthenticationServiceImpl.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final UserManager userManager;
    private final String redirectPath;

    public GwtPasswordAuthenticationServiceImpl(final UserManager userManager, final String redirectPath) {
        this.userManager = userManager;
        this.redirectPath = redirectPath;
    }

    @Override
    public String authenticate(final String username, final String password) throws GwtKuraException {

        final HttpSession session = Console.instance().createSession(getThreadLocalRequest());
        final HttpServletRequest request = getThreadLocalRequest();

        String requestIp = request.getHeader("X-FORWARDED-FOR");
        if (isNull(requestIp)) {
            requestIp = request.getRemoteAddr();
        }

        try {
            if (!Console.getConsoleOptions().isAuthenticationMethodEnabled("Password")) {
                throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
            }

            this.userManager.authenticateWithPassword(username, password);
            Console.instance().setAuthenticated(session, username);

            session.setAttribute(Attributes.AUTORIZED_USER.getValue(), username);
            logger.info("UI Login - Success - Login for user: {}, session id: {}, request IP: {}", username,
                    session.getId(), requestIp);
            auditLogger.info("UI Login - Success - Login for user: {}, session id: {}, request IP: {}", username,
                    session.getId(), requestIp);

            return this.redirectPath;

        } catch (final Exception e) {
            session.invalidate();
            logger.warn(UI_LOGIN_FAILURE_MESSAGE, username, requestIp);
            auditLogger.warn(UI_LOGIN_FAILURE_MESSAGE, username, requestIp);

            throw new GwtKuraException("unauthorized");
        }
    }

}
