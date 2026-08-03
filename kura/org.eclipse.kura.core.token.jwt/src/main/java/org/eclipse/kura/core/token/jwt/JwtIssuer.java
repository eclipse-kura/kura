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
package org.eclipse.kura.core.token.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.token.jwt.JwtKeyMaterial.SigningKey;
import org.eclipse.kura.security.token.jwt.JwtIssueRequest;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;

class JwtIssuer {

    private final String issuer;
    private final Optional<Duration> maximumLifetime;

    public JwtIssuer(final String issuer, final Optional<Duration> maximumLifetime) {
        this.issuer = issuer;
        this.maximumLifetime = maximumLifetime;
    }

    public String issueToken(final JwtIssueRequest request, final JwtKeyMaterial keyMaterial) throws KuraException {

        Optional<SigningKey> signingKey = keyMaterial.getSigningKey();
        if (signingKey.isEmpty()) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                    "No signing key configured or alias not found in bound keystore, token issuing is disabled");
        }

        final Instant now = Instant.now();

        final Builder builder = JWT.create() //
                .withIssuer(this.issuer) //
                .withIssuedAt(now) //
                .withKeyId(signingKey.get().alias());

        if (!request.getAudience().isEmpty()) {
            builder.withAudience(request.getAudience().toArray(String[]::new));
        }

        final Optional<Instant> reqExpiresAt = request.getExpiresAt();

        if (this.maximumLifetime.isPresent()) {
            final Instant maxLifetime = now.plus(this.maximumLifetime.get());

            if (reqExpiresAt.isEmpty() || reqExpiresAt.get().isAfter(maxLifetime)) {
                builder.withExpiresAt(maxLifetime);
            } else {
                builder.withExpiresAt(reqExpiresAt.get());
            }
        } else {
            reqExpiresAt.ifPresent(builder::withExpiresAt);
        }

        request.getNotBefore().ifPresent(builder::withNotBefore);
        request.getSubject().ifPresent(builder::withSubject);
        request.getJti().ifPresent(builder::withJWTId);

        for (final Entry<String, Object> claim : request.getAdditionalClaims().entrySet()) {
            applyClaim(builder, claim.getKey(), claim.getValue());
        }

        try {
            final Algorithm algorithm = Algorithm.RSA256(null, signingKey.get().rsaPrivateKey());
            return builder.sign(algorithm);
        } catch (final JWTCreationException | IllegalArgumentException e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e, "Failed to sign the token");
        }
    }

    private void applyClaim(final Builder builder, final String name, final Object value) throws KuraException {
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
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST, "Unsupported Map in claim, keys must be String");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

}
