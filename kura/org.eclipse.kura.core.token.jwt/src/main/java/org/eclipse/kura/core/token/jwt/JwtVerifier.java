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

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraAuthenticationFailedException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.security.token.jwt.JwtVerificationProof;
import org.eclipse.kura.security.token.jwt.JwtVerifyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;

class JwtVerifier {

    private static final Logger logger = LoggerFactory.getLogger(JwtVerifier.class);

    private final Set<String> trustedIssuers;
    private final long clockSkewSeconds;
    private final boolean requireValidCertificate;

    public JwtVerifier(final Set<String> trustedIssuers, final long clockSkewSeconds,
            final boolean requireValidCertificate) {
        this.trustedIssuers = trustedIssuers;
        this.clockSkewSeconds = clockSkewSeconds;
        this.requireValidCertificate = requireValidCertificate;
    }

    public JwtVerificationProof verify(final JwtVerifyRequest request, final JwtKeyMaterial keyMaterial)
            throws KuraException {

        final DecodedJWT decodedJwt;
        try {
            decodedJwt = JWT.decode(request.getToken());
        } catch (final JWTDecodeException e) {
            throw new KuraAuthenticationFailedException(e, "Failed to decode JWT");
        }

        final Map<String, X509Certificate> candidates = selectCandidates(decodedJwt, keyMaterial);

        if (candidates.isEmpty()) {
            throw new KuraAuthenticationFailedException("No trusted certificate matches the token key id");
        }

        for (final Entry<String, X509Certificate> candidate : candidates.entrySet()) {
            try {
                final DecodedJWT verifiedJwt = verifyWith(decodedJwt, candidate.getKey(), candidate.getValue(),
                        request.getAcceptedAudience());
                return new VerificationProof(Instant.now(), verifiedJwt);
            } catch (final CertificateExpiredException | CertificateNotYetValidException certExp) {
                logger.debug("Certificate {} is outside its validity period", candidate.getKey(), certExp);
            } catch (final JWTVerificationException ve) {
                logger.debug("Token verification failed", ve);
            }
        }

        throw new KuraAuthenticationFailedException("Failed to verify JWT");
    }

    private Map<String, X509Certificate> selectCandidates(final DecodedJWT decodedJwt,
            final JwtKeyMaterial keyMaterial) {

        final Optional<String> keyId = Optional.ofNullable(decodedJwt.getKeyId());

        if (!keyId.isPresent()) {
            return keyMaterial.getVerificationCertificates();
        }

        Optional<X509Certificate> certificate = keyMaterial.getVerificationCertificate(keyId.get());
        if (certificate.isPresent()) {
            return Collections.singletonMap(keyId.get(), certificate.get());
        }

        return Collections.emptyMap();
    }

    private DecodedJWT verifyWith(final DecodedJWT decodedJwt, final String alias, final X509Certificate certificate,
            final Set<String> acceptedAudience) throws CertificateExpiredException, CertificateNotYetValidException {

        if (this.requireValidCertificate) {
            certificate.checkValidity();
        }

        final Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) certificate.getPublicKey(), null);

        Verification verification = JWT.require(algorithm) //
                .withIssuer(this.trustedIssuers.toArray(String[]::new)) //
                .acceptLeeway(this.clockSkewSeconds);

        if (!acceptedAudience.isEmpty()) {
            verification = verification.withAnyOfAudience(acceptedAudience.toArray(String[]::new));
        }

        return verification.build().verify(decodedJwt);
    }

    private static final class VerificationProof implements JwtVerificationProof {

        private final Instant verifiedAt;
        private final DecodedJWT decodedJwt;

        public VerificationProof(final Instant verifiedAt, final DecodedJWT decodedJwt) {
            this.verifiedAt = verifiedAt;
            this.decodedJwt = decodedJwt;
        }

        @Override
        public Instant getVerifiedAt() {
            return this.verifiedAt;
        }

        @Override
        public Map<String, Object> getClaims() {
            Map<String, Object> result = new LinkedHashMap<>();
            this.decodedJwt.getClaims().forEach((name, value) -> {
                if (value.isNull()) {
                    result.put(name, null);
                } else {
                    result.put(name, value.as(Object.class));
                }
            });

            return Collections.unmodifiableMap(result);
        }

        @Override
        public Optional<String> getIssuer() {
            return Optional.ofNullable(this.decodedJwt.getIssuer());
        }

        @Override
        public Optional<String> getSubject() {
            return Optional.ofNullable(this.decodedJwt.getSubject());
        }

        @Override
        public Optional<Set<String>> getAudience() {
            if (this.decodedJwt.getAudience() == null) {
                return Optional.empty();
            }

            return Optional.of(this.decodedJwt.getAudience().stream().collect(Collectors.toUnmodifiableSet()));
        }

        @Override
        public Optional<Instant> getExpiresAt() {
            return Optional.ofNullable(this.decodedJwt.getExpiresAtAsInstant());
        }

        @Override
        public Optional<Instant> getNotBefore() {
            return Optional.ofNullable(this.decodedJwt.getNotBeforeAsInstant());
        }

        @Override
        public Optional<Instant> getIssuedAt() {
            return Optional.ofNullable(this.decodedJwt.getIssuedAtAsInstant());
        }

        @Override
        public Optional<String> getJti() {
            return Optional.ofNullable(this.decodedJwt.getId());
        }

    }

}
