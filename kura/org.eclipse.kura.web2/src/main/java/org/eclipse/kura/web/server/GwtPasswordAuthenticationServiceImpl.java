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

import javax.servlet.http.HttpSession;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.audit.AuditConstants;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.UserManager;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtPasswordAuthenticationResult;
import org.eclipse.kura.web.shared.service.GwtPasswordAuthenticationService;

public class GwtPasswordAuthenticationServiceImpl extends OsgiRemoteServiceServlet
        implements GwtPasswordAuthenticationService {

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
    public GwtPasswordAuthenticationResult authenticate(final String username, final String password)
            throws GwtKuraException {

        final HttpSession session = Console.instance().createNewSession(getThreadLocalRequest());

        final AuditContext context = AuditContext.currentOrInternal();
        context.getProperties().put(AuditConstants.KEY_IDENTITY.getValue(), username);

        try {
            if (!Console.getConsoleOptions().isAuthenticationMethodEnabled("Password")) {
                throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
            }

            this.userManager.authenticateWithPassword(username, password);

            context.getProperties().put("session.id", GwtServerUtil.getSessionIdHash(session));

            Console.instance().setAuthenticated(session, username, context.copy());

            final boolean needsPasswordChange = this.userManager.isPasswordChangeRequired(username);

            if (needsPasswordChange) {
                session.setAttribute(Attributes.LOCKED.getValue(), true);
            }

            return new GwtPasswordAuthenticationResult(needsPasswordChange, this.redirectPath);

        } catch (final Exception e) {
            session.invalidate();

            throw new GwtKuraException("unauthorized");
        }
    }

}
