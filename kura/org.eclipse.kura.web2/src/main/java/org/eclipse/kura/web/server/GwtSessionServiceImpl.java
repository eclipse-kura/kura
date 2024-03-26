/*******************************************************************************
 * Copyright (c) 2019, 2024 Eurotech and/or its affiliates and others
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

import java.util.Objects;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.audit.AuditContext;
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
            session.invalidate();

            auditLogger.info("{} UI Session - Success - Logout succeeded for user: {}",
                    AuditContext.currentOrInternal(), username);

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

        if (session == null) {
            throw new GwtKuraException(GwtKuraErrorCode.UNAUTHENTICATED);
        }

        final Object username = session.getAttribute(Attributes.AUTORIZED_USER.getValue());

        if (!(username instanceof String)) {
            throw new GwtKuraException(GwtKuraErrorCode.UNAUTHENTICATED);
        }

        try {
            return userManager.getUserConfig((String) username).orElse(null);
        } catch (KuraException e) {
            auditLogger.warn("{} UI Session - Failure - Failed to get configuration for user {}",
                    AuditContext.currentOrInternal(), username);
            return null;
        }
    }

    @Override
    public void updatePassword(GwtXSRFToken xsrfToken, String oldPassword, String newPassword) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        String username = getSessionUsername(session);

        try {
            Optional<GwtUserConfig> userConfig = this.userManager.getUserConfig(username);

            if (!userConfig.isPresent() || !userConfig.get().isPasswordAuthEnabled()) {
                throw new GwtKuraException(GwtKuraErrorCode.OPERATION_NOT_SUPPORTED);
            }
        } catch (final KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.OPERATION_NOT_SUPPORTED);
        }

        try {
            this.userManager.authenticateWithPassword(username, oldPassword);
        } catch (KuraException e) {
            auditLogger.warn("{} UI Session - Failure - Wrong password for user {}", AuditContext.currentOrInternal(),
                    username);
            throw new GwtKuraException(GwtKuraErrorCode.INVALID_USERNAME_PASSWORD);
        }

        if (Objects.equals(oldPassword, newPassword)) {
            throw new GwtKuraException(GwtKuraErrorCode.PASSWORD_CHANGE_SAME_PASSWORD);
        }

        try {
            this.userManager.setUserPassword(username, newPassword);

            auditLogger.info("{} UI Session - Success - Password updated for user {}", AuditContext.currentOrInternal(),
                    username);
        } catch (final KuraException e) {
            auditLogger.warn("{} UI Session - Failure - Failed to update password for user {}",
                    AuditContext.currentOrInternal(), username);
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }

        setAuthenticated(session, username);
    }

    private String getSessionUsername(HttpSession session) throws GwtKuraException {
        if (session == null) {
            throw new GwtKuraException(GwtKuraErrorCode.UNAUTHENTICATED);
        }

        final Object username = session.getAttribute(Attributes.AUTORIZED_USER.getValue());

        if (!(username instanceof String)) {
            throw new GwtKuraException(GwtKuraErrorCode.UNAUTHENTICATED);
        }

        return (String) username;
    }

    private void setAuthenticated(HttpSession session, String username) throws GwtKuraException {
        Console.instance().setAuthenticated(session, username,
                AuditContext.current().orElseThrow(() -> new GwtKuraException("Audit context is not available")));
        session.removeAttribute(Attributes.LOCKED.getValue());
    }
}
