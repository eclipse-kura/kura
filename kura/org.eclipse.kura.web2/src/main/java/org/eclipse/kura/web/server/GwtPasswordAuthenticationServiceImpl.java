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

import static java.util.Objects.isNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.web.AuthenticationManager;
import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.service.GwtPasswordAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtPasswordAuthenticationServiceImpl extends OsgiRemoteServiceServlet
        implements GwtPasswordAuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(GwtPasswordAuthenticationServiceImpl.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final AuthenticationManager authenticationManager;
    private final String redirectPath;

    public GwtPasswordAuthenticationServiceImpl(final AuthenticationManager authenticationManager,
            final String redirectPath) {
        this.authenticationManager = authenticationManager;
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
            if (!this.authenticationManager.authenticate(username, password)) {
                logger.warn("UI Login - Failure - Login failed for user: {}, request IP: {}", username, requestIp);
                auditLogger.warn("UI Login - Failure - Login failed for user: {}, request IP: {}", username, requestIp);
                throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
            }

            session.setAttribute(Attributes.AUTORIZED_USER.getValue(), username);
            logger.info("UI Login - Success - Login for user: {}, session id: {}, request IP: {}", username,
                    session.getId(), requestIp);
            auditLogger.info("UI Login - Success - Login for user: {}, session id: {}, request IP: {}", username,
                    session.getId(), requestIp);

            return this.redirectPath;

        } catch (final Exception e) {
            session.invalidate();

            throw new GwtKuraException("unauthorized");
        }
    }

}
