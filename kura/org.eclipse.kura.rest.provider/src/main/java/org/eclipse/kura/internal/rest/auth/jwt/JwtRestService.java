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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.kura.KuraAuthenticationFailedException;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.audit.AuditConstants;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.internal.rest.auth.RestIdentityHelper;
import org.eclipse.kura.internal.rest.auth.dto.TokenPairDTO;
import org.eclipse.kura.internal.rest.provider.RestServiceOptions;
import org.eclipse.kura.security.token.TokenIssueRequest;
import org.eclipse.kura.security.token.TokenIssuingService;
import org.eclipse.kura.security.token.TokenVerificationService;
import org.eclipse.kura.security.token.TokenVerifyRequest;
import org.eclipse.kura.security.token.VerificationProof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;

@Path(JwtRestServiceConstants.SERVICE_PATH)
public class JwtRestService {

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    static final int MAX_REFRESH_TOKEN_BYTES = 8 * 1024;

    private final RestIdentityHelper identityHelper;
    private final AtomicReference<TokenIssuingService> tokenIssuer = new AtomicReference<>();
    private final AtomicReference<TokenVerificationService> tokenVerifier = new AtomicReference<>();
    private final AtomicReference<RestServiceOptions> options = new AtomicReference<>();

    private final ConcurrentMap<String, Instant> usedRefreshTokens = new ConcurrentHashMap<>();

    public JwtRestService(final RestIdentityHelper identityHelper) {
        this.identityHelper = identityHelper;
    }

    public void setOptions(final RestServiceOptions options) {
        this.options.set(options);
    }

    public void setTokenIssuingService(final TokenIssuingService issuingService) {
        this.tokenIssuer.set(issuingService);
    }

    // a replacement service can be set before the old one is unset
    public void unsetTokenIssuingService(final TokenIssuingService issuingService) {
        this.tokenIssuer.compareAndSet(issuingService, null);
    }

    public void setTokenVerificationService(final TokenVerificationService verificationService) {
        this.tokenVerifier.set(verificationService);
    }

    // a replacement service can be set before the old one is unset
    public void unsetTokenVerificationService(final TokenVerificationService verificationService) {
        this.tokenVerifier.compareAndSet(verificationService, null);
    }

    /**
     * Issue a new token pair. Can be called after a successful authentication with a different AuthenticationProvider
     * that is not a {@link JwtAuthenticationProvider}
     * 
     * @param requestContext
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path(JwtRestServiceConstants.ISSUE_PATH)
    public TokenPairDTO issue(@Context final ContainerRequestContext requestContext) {
        checkServiceEnabled();
        final TokenIssuingService issuer = requireService(this.tokenIssuer);
        final String identityName = requireCorrectPrincipal(requestContext);
        final AuditContext auditContext = AuditContext.currentOrInternal();

        try {
            checkIdentityCanAuthenticate(identityName, auditContext);

            final TokenPairDTO result = issueTokenPair(identityName, issuer);
            auditLogger.info("{} Rest - Success - JWT token pair issued", auditContext);
            return result;
        } catch (final KuraException e) {
            throw toWebApplicationException(e);
        }
    }

    /**
     * Accepts any refresh token the verification service trusts, issued by this service or by an external one.
     * 
     * @param body
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    @Path(JwtRestServiceConstants.REFRESH_PATH)
    public TokenPairDTO refresh(final InputStream body) {
        checkServiceEnabled();
        final TokenIssuingService issuer = requireService(this.tokenIssuer);
        final TokenVerificationService verifier = requireService(this.tokenVerifier);
        final String refreshToken = readRefreshToken(body);
        final AuditContext auditContext = AuditContext.currentOrInternal();

        try {
            final VerificationProof proof = verifier.verify(TokenVerifyRequest.builder(refreshToken)
                    .intendedConsumer(JwtRestServiceConstants.INTENDED_REFRESH_CONSUMER).build());
            final String identityName = proof.getIdentityName();
            auditContext.getProperties().put(AuditConstants.KEY_IDENTITY.getValue(), identityName);
            checkIdentityCanAuthenticate(identityName, auditContext);

            final String id = createIdForReplayDetection(proof);
            final TokenPairDTO result = issueTokenPair(identityName, issuer);

            updateUsedRefreshTokens(id, proof.getExpiresAt());

            auditLogger.info("{} Rest - Success - JWT token pair refreshed", auditContext);
            return result;
        } catch (final KuraAuthenticationFailedException e) {
            auditLogger.warn(JwtRestServiceConstants.AUDIT_FAILURE_FORMAT_STRING, auditContext,
                    "JWT token refresh failed, " + e.getMessage());
            throw new WebApplicationException(e, Response.Status.UNAUTHORIZED);
        } catch (final KuraException e) {
            throw toWebApplicationException(e);
        }
    }

    private void checkServiceEnabled() {
        final RestServiceOptions currentOptions = this.options.get();
        if (currentOptions == null || !currentOptions.isJwtAuthEnabled()) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
    }

    private static String requireCorrectPrincipal(final ContainerRequestContext requestContext) {
        final SecurityContext securityContext = requestContext.getSecurityContext();
        final Principal principal = securityContext == null ? null : securityContext.getUserPrincipal();

        if (principal == null || principal instanceof JwtPrincipal) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }

        return principal.getName();
    }

    private static <T> T requireService(AtomicReference<T> reference) {
        final T service = reference.get();
        if (service == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        return service;
    }

    private void checkIdentityCanAuthenticate(final String identityName, final AuditContext auditContext)
            throws KuraException {
        if (!this.identityHelper.identityExists(identityName)) {
            auditLogger.warn(JwtRestServiceConstants.AUDIT_FAILURE_FORMAT_STRING, auditContext,
                    "Identity does not exists");
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }

        if (this.identityHelper.isPasswordChangeRequired(identityName)) {
            auditLogger.warn(JwtRestServiceConstants.AUDIT_FAILURE_FORMAT_STRING, auditContext,
                    "Password change required");
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }

    // the endpoint is public: read a bounded prefix instead of buffering whatever the caller sends
    private static String readRefreshToken(final InputStream body) {
        final byte[] bytes;

        try {
            bytes = body.readNBytes(MAX_REFRESH_TOKEN_BYTES + 1);
        } catch (final IOException e) {
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        if (bytes.length > MAX_REFRESH_TOKEN_BYTES) {
            throw new WebApplicationException(Response.Status.REQUEST_ENTITY_TOO_LARGE);
        }

        final String refreshToken = new String(bytes, StandardCharsets.UTF_8).trim();

        if (refreshToken.isEmpty()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        return refreshToken;
    }

    private TokenPairDTO issueTokenPair(final String identityName, final TokenIssuingService issuer)
            throws KuraException {
        final RestServiceOptions currentOptions = this.options.get();
        final Instant now = Instant.now();
        final Duration accessTokenLifetime = cappedLifetime(currentOptions.getJwtAccessTokenDuration(), issuer);
        final Duration refreshTokenLifetime = cappedLifetime(currentOptions.getJwtRefreshTokenDuration(), issuer);

        final String accessToken = issuer.issue(
                tokenRequest(identityName, now, accessTokenLifetime, JwtRestServiceConstants.INTENDED_ACCESS_CONSUMER)
                        .build());
        final String refreshToken = issuer.issue(
                tokenRequest(identityName, now, refreshTokenLifetime, JwtRestServiceConstants.INTENDED_REFRESH_CONSUMER)
                        .build());

        return new TokenPairDTO(JwtRestServiceConstants.TOKEN_TYPE, accessToken, accessTokenLifetime.toSeconds(),
                refreshToken, refreshTokenLifetime.toSeconds());
    }

    private static Duration cappedLifetime(final Duration configured, final TokenIssuingService issuer) {
        return issuer.getMaximumLifetime().filter(maximum -> maximum.compareTo(configured) < 0).orElse(configured);
    }

    private static TokenIssueRequest.Builder tokenRequest(final String identityName, final Instant now,
                    final Duration duration, final String intendedConsumer) {
        return TokenIssueRequest.builder(identityName) //
                .intendedConsumer(intendedConsumer) //
                .notBefore(now) //
                .expiresAt(now.plus(duration));
    }

    private String createIdForReplayDetection(final VerificationProof proof) throws KuraAuthenticationFailedException {
        // jti and iss are always set in the {@link org.eclipse.kura.core.token.jwt.issuer.JwtIssuingService}
        final String jti = proof.getTokenID()
                .orElseThrow(() -> new KuraAuthenticationFailedException("Refresh token has no jti"));
        final Object iss = proof.getClaims().get("iss");
        if (!(iss instanceof String issuer)) {
            throw new KuraAuthenticationFailedException("Refresh token has no iss");
        }

        final String id = issuer + "#" + jti;

        if (this.usedRefreshTokens.containsKey(id)) {
            throw new KuraAuthenticationFailedException("Refresh token already used");
        }

        return id;
    }

    private void updateUsedRefreshTokens(final String id, final Optional<Instant> expiresAt)
            throws KuraAuthenticationFailedException {
        final Duration retention = this.options.get().getJwtUsedRefreshTokenRetention();
        final Instant trackedUntil = expiresAt.map(expiration -> expiration.plus(retention)).orElse(Instant.MAX);

        final Instant now = Instant.now();
        this.usedRefreshTokens.values().removeIf(until -> until.isBefore(now));

        if (this.usedRefreshTokens.putIfAbsent(id, trackedUntil) != null) {
            throw new KuraAuthenticationFailedException("Refresh token already used");
        }
    }

    private static WebApplicationException toWebApplicationException(final KuraException e) {
        if (e.getCode() == KuraErrorCode.SERVICE_UNAVAILABLE) {
            return new WebApplicationException(e, Response.Status.SERVICE_UNAVAILABLE);
        }

        return new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
}
