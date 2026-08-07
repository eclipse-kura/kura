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
package org.eclipse.kura.core.token.jwt.issuer;

import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.security.token.TokenIssueRequest;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;

final class JwtIssuer {

    private static final Set<String> RESERVED_CLAIMS = Set.of("iss", "sub", "aud", "exp", "nbf", "iat", "jti");

    private final Algorithm algorithm;
    private final String issuer;
    private final String signingKeyAlias;
    private final Optional<Duration> maximumLifetime;

    JwtIssuer(final JwtIssuingServiceOptions options, final RSAPrivateKey signingKey) {
        this.algorithm = Algorithm.RSA256(null, signingKey);
        this.issuer = options.getIssuer();
        this.signingKeyAlias = options.getSigningKeyAlias();
        this.maximumLifetime = options.getMaximumLifetime();
    }

    String issue(final TokenIssueRequest request) throws KuraException {

        final Instant now = Instant.now();
        final Builder builder = JWT.create();

        applyClaims(builder, request.getClaims());

        builder.withIssuer(this.issuer) //
                .withIssuedAt(now) //
                .withKeyId(this.signingKeyAlias) //
                .withSubject(request.getIdentityName()) //
                .withJWTId(UUID.randomUUID().toString());

        if (!request.getIntendedConsumers().isEmpty()) {
            builder.withAudience(request.getIntendedConsumers().toArray(String[]::new));
        }

        expiresAt(request.getExpiresAt(), now).ifPresent(builder::withExpiresAt);
        request.getNotBefore().ifPresent(builder::withNotBefore);

        try {
            return builder.sign(this.algorithm);
        } catch (final JWTCreationException | IllegalArgumentException e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e, "Failed to sign the token");
        }
    }

    private Optional<Instant> expiresAt(final Optional<Instant> requestedExpiresAt, final Instant now) {

        final Optional<Instant> latestAllowed = this.maximumLifetime.map(now::plus);

        if (latestAllowed.isEmpty()) {
            return requestedExpiresAt;
        }

        if (requestedExpiresAt.isEmpty() || requestedExpiresAt.get().isAfter(latestAllowed.get())) {
            return latestAllowed;
        }

        return requestedExpiresAt;
    }

    private static void applyClaims(final Builder builder, final Map<String, Object> claims) throws KuraException {

        for (final Entry<String, Object> claim : claims.entrySet()) {
            final String name = claim.getKey();

            if (RESERVED_CLAIMS.contains(name)) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST,
                        "Claim '" + name + "' is reserved and cannot be set through the request");
            }

            try {
                applyClaim(builder, name, claim.getValue());
            } catch (final IllegalArgumentException e) {
                // thrown by the builder when the value cannot be applied
                throw new KuraException(KuraErrorCode.BAD_REQUEST, e, "Unsupported claim element");
            }
        }
    }

    private static void applyClaim(final Builder builder, final String name, final Object value) throws KuraException {
        switch (value) {
        case null -> builder.withNullClaim(name);
        case String s -> builder.withClaim(name, s);
        case Boolean b -> builder.withClaim(name, b);
        case Integer i -> builder.withClaim(name, i);
        case Long l -> builder.withClaim(name, l);
        case Double d -> builder.withClaim(name, d);
        case Instant i -> builder.withClaim(name, i);
        case List<?> list -> builder.withClaim(name, list);
        case Map<?, ?> map -> builder.withClaim(name, asStringKeyedMap(map));
        default -> throw new KuraException(KuraErrorCode.BAD_REQUEST,
                "Unsupported value type " + value.getClass().getName() + " for claim '" + name + "'");
        }
    }

    private static Map<String, Object> asStringKeyedMap(final Map<?, ?> map) throws KuraException {

        final Map<String, Object> result = new LinkedHashMap<>();

        for (final Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST, "Unsupported Map in claim, keys must be String");
            }
            result.put(key, entry.getValue());
        }

        return result;
    }

}