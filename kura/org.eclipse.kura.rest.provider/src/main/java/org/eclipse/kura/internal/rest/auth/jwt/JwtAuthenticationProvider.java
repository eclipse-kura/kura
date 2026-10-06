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
package org.eclipse.kura.internal.rest.auth.jwt;

import java.security.Principal;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.internal.rest.auth.RestIdentityHelper;
import org.eclipse.kura.rest.auth.AuthenticationProvider;
import org.eclipse.kura.security.token.TokenVerificationService;
import org.eclipse.kura.security.token.TokenVerifyRequest;
import org.eclipse.kura.security.token.VerificationProof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;

@Priority(400)
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private final RestIdentityHelper identityHelper;
    private AtomicReference<TokenVerificationService> tokenVerifier = new AtomicReference<>();

    public void setTokenVerificationService(final TokenVerificationService verifier) {
        this.tokenVerifier.set(verifier);
    }

    // a replacement service can be set before the old one is unset
    public void unsetTokenVerificationService(final TokenVerificationService verifier) {
        this.tokenVerifier.compareAndSet(verifier, null);
    }

    public JwtAuthenticationProvider(final RestIdentityHelper identityHelper) {
        this.identityHelper = identityHelper;
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
    public Optional<Principal> authenticate(final HttpServletRequest request,
            final ContainerRequestContext requestContext) {
        final AuditContext auditContext = AuditContext.currentOrInternal();
        final Optional<String> tokenInHeader = getTokenFromAuthorizationHeader(
                requestContext.getHeaderString("Authorization"));

        if (tokenInHeader.isEmpty()) {
            return Optional.empty();
        }

        final TokenVerificationService verifier = this.tokenVerifier.get();

        if (verifier == null) {
            return Optional.empty();
        }

        final Optional<VerificationProof> proof = verifyAccessToken(tokenInHeader.get(), verifier);
        if (proof.isEmpty()) {
            return notAuthenticated(auditContext);
        }

        final String identityName = proof.get().getIdentityName();

        try {
            if (!this.identityHelper.identityExists(identityName)) {
                return notAuthenticated(auditContext);
            }

            if (this.identityHelper.isPasswordChangeRequired(identityName)) {
                return notAuthenticated(auditContext, "Password change required");
            }

            auditLogger.info("{} Rest - Success - Authentication succeeded via JWT authentication provider",
                    auditContext);
            return Optional.of(new JwtPrincipal(identityName));
        } catch (KuraException e) {
            return notAuthenticated(auditContext);
        }
    }

    private static Optional<String> getTokenFromAuthorizationHeader(final String authHeader) {
        if (authHeader == null) {
            return Optional.empty();
        }

        final StringTokenizer headerTokens = new StringTokenizer(authHeader);

        if (headerTokens.countTokens() != 2) {
            return Optional.empty();
        }

        final String authScheme = headerTokens.nextToken();
        if (!"Bearer".equalsIgnoreCase(authScheme)) {
            return Optional.empty();
        }

        return Optional.of(headerTokens.nextToken());
    }

    private static Optional<VerificationProof> verifyAccessToken(final String token,
            final TokenVerificationService verifier) {
        try {
            final TokenVerifyRequest verifyRequest = TokenVerifyRequest.builder(token)
                    .intendedConsumer(JwtRestServiceConstants.INTENDED_ACCESS_CONSUMER).build();
            return Optional.of(verifier.verify(verifyRequest));
        } catch (KuraException e) {
            return Optional.empty();
        }
    }

    private Optional<Principal> notAuthenticated(final AuditContext auditContext) {
        return notAuthenticated(auditContext, "JWT authentication failed");
    }

    private Optional<Principal> notAuthenticated(final AuditContext auditContext, final String reason) {
        auditLogger.warn(JwtRestServiceConstants.AUDIT_FAILURE_FORMAT_STRING, auditContext, reason);
        return Optional.empty();
    }

}
