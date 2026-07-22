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
package org.eclipse.kura.core.identity;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.identity.IdentityTokenService;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

@Component(immediate = true, //
        property = "kura.ui.service.hide:Boolean=true", //
        name = "org.eclipse.kura.identity.IdentityJwtTokenServiceImpl")
public class IdentityJwtTokenServiceImpl implements IdentityTokenService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityJwtTokenServiceImpl.class);

    private Algorithm signAlgorithm;
    private String issuer;

    @Reference
    private SystemService systemService;

    @Activate
    public void activate(Map<String, Object> properties) throws NoSuchAlgorithmException {
        byte[] secret = new byte[256];
        SecureRandom.getInstanceStrong().nextBytes(secret);
        this.signAlgorithm = Algorithm.HMAC512(secret);
        this.issuer = "kura_" + this.systemService.getPrimaryMacAddress().replace(":", "").toUpperCase();

        logger.debug("Initialized IdentityJwtTokenServiceImpl with issuer: {}", this.issuer);
    }

    @Override
    public String issueTokenFor(String identityName, Duration ttl) throws KuraException {
        if (identityName == null) {
            throw new IllegalArgumentException("Input identityName cannot be null");
        }
        if (ttl == null) {
            throw new IllegalArgumentException("Input ttl cannot be null");
        }

        try {
            final Instant now = Instant.now();
            final Instant expiresAt = now.plus(ttl);

            final String token = JWT.create().withIssuer(this.issuer) //
                    .withIssuedAt(now) //
                    .withSubject(identityName) //
                    .withExpiresAt(expiresAt) //
                    .sign(this.signAlgorithm);

            logger.info("Issued token with sub: {}, issuer: {}, iat: {}, eat: {}", identityName, this.issuer,
                    now, expiresAt);

            return token;
        } catch (JWTCreationException exception) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR, exception.getMessage());
        }
    }

    @Override
    public Optional<String> verifyToken(String encodedJwt) {
        if (encodedJwt == null) {
            throw new IllegalArgumentException("Input encodedJwt cannot be null");
        }

        try {
            final DecodedJWT jwt = JWT.require(this.signAlgorithm) //
                    .withIssuer(this.issuer) //
                    .build() //
                    .verify(encodedJwt);

            return Optional.ofNullable(jwt.getSubject());
        } catch (JWTVerificationException exception) {
            logger.warn("Failed to verify token", exception);
            return Optional.empty();
        }
    }

}
