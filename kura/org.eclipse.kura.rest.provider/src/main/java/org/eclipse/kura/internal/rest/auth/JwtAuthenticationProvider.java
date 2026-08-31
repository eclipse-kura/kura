/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.auth;

import static java.util.Objects.isNull;

import java.security.Principal;
import java.util.Optional;
import java.util.StringTokenizer;

import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.identity.IdentityTokenService;
import org.eclipse.kura.rest.auth.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;

@Priority(400)
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private final RestIdentityHelper identityHelper;
    private final IdentityTokenService tokenService;

    public JwtAuthenticationProvider(final RestIdentityHelper identityHelper, final IdentityTokenService tokenService) {
        this.identityHelper = identityHelper;
        this.tokenService = tokenService;
    }

    @Override
    public void onEnabled() {
        // nothing to do
    }

    @Override
    public void onDisabled() {
        // nothing to do
    }

    @Override
    public Optional<Principal> authenticate(HttpServletRequest request, ContainerRequestContext requestContext) {
        final AuditContext auditContext = AuditContext.currentOrInternal();

        // step 1: check if token is present in authorization header

        final String authHeader = requestContext.getHeaderString("Authorization");
        if (isNull(authHeader)) {
            return Optional.empty();
        }

        final StringTokenizer tokens = new StringTokenizer(authHeader);
        final String authScheme = tokens.nextToken();
        if (!"Bearer".equals(authScheme)) {
            return Optional.empty();
        }

        // step 2: validate token and authenticate

        try {
            final String token = tokens.nextToken();
            Optional<String> subject = this.tokenService.verifyToken(token);
            if (subject.isEmpty()) {
                return notAuthenticated(auditContext);
            }

            if (this.identityHelper.identityExists(subject.get())) {
                auditLogger.info("{} Rest - Success - JWT authentication succeeded", auditContext);
                return Optional.of(() -> subject.get());
            } else {
                return notAuthenticated(auditContext);
            }
        } catch (Exception e) {
            return notAuthenticated(auditContext);
        }
    }

    private Optional<Principal> notAuthenticated(AuditContext auditContext) {
        auditLogger.info("{} Rest - Failure - JWT authentication failed", auditContext);
        return Optional.empty();
    }

}
