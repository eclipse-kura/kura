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
package org.eclipse.kura.web.session;

import java.io.IOException;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.audit.AuditContext.Scope;
import org.eclipse.kura.web.Console;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionAutorizationSecurityHandler implements SecurityHandler {

    private static final Logger logger = LoggerFactory.getLogger(SessionAutorizationSecurityHandler.class);

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {

        final AuditContext auditContext = Console.instance().initAuditContext(request);

        try (final Scope scope = AuditContext.openScope(auditContext)) {
            final HttpSession session = request.getSession(false);

            if (session == null) {
                auditLogger.warn("{} UI Session - Failure - User session does not exist", auditContext);
                return false;
            }

            final Object authorized = session.getAttribute(Attributes.AUTORIZED_USER.getValue());

            if (!(authorized instanceof String)) {
                auditLogger.warn("{} UI Session - Failure - User is not authenticated", auditContext);
                return false;
            }

            final String userName = (String) authorized;

            boolean isPasswordSame;

            try {
                isPasswordSame = Objects.equals(session.getAttribute(Attributes.CREDENTIALS_HASH.getValue()),
                        Console.instance().getUserManager().getCredentialsHash(userName));
            } catch (KuraException e) {
                logger.warn("failed to compute credential hash", e);
                isPasswordSame = false;
            }

            if (!isPasswordSame) {
                auditLogger.warn("{} UI Session - Failure - Ending session due to identity password change",
                        auditContext);
            }

            return isPasswordSame;
        }
    }

}
