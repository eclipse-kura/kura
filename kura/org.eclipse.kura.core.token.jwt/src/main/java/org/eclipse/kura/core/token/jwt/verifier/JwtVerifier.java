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
package org.eclipse.kura.core.token.jwt.verifier;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraAuthenticationFailedException;
import org.eclipse.kura.security.token.VerificationProof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;

final class JwtVerifier {

    private static final Logger logger = LoggerFactory.getLogger(JwtVerifier.class);

    private record TrustAnchor(String alias, X509Certificate certificate) {
    }

    private final List<TrustAnchor> trustAnchors;
    private final Map<String, TrustAnchor> trustAnchorsByAlias;
    private final Set<String> trustedIssuers;
    private final long clockSkewToleranceSec;
    private final boolean requireValidCertificate;

    JwtVerifier(final JwtVerificationServiceOptions options, final Map<String, X509Certificate> certificates) {

        final List<TrustAnchor> anchors = new ArrayList<>(certificates.size());
        final Map<String, TrustAnchor> anchorsByAlias = new LinkedHashMap<>();

        certificates.forEach((alias, certificate) -> {
            final TrustAnchor anchor = new TrustAnchor(alias, certificate);
            anchors.add(anchor);
            anchorsByAlias.put(alias, anchor);
        });

        this.trustAnchors = Collections.unmodifiableList(anchors);
        this.trustAnchorsByAlias = Collections.unmodifiableMap(anchorsByAlias);
        this.trustedIssuers = options.getTrustedIssuers();
        this.clockSkewToleranceSec = options.getClockSkewToleranceSec();
        this.requireValidCertificate = options.isRequireValidCertificate();
    }

    VerificationProof verify(final String encodedToken, final Optional<String> intendedConsumer)
            throws KuraAuthenticationFailedException {

        final DecodedJWT decodedJwt = decode(encodedToken);

        SignatureVerificationException lastSignatureFailure = null;

        for (final TrustAnchor candidate : selectCandidates(decodedJwt.getKeyId())) {

            if (this.requireValidCertificate && !isWithinValidityPeriod(candidate)) {
                continue;
            }

            try {
                return asVerificationProof(verification(candidate, intendedConsumer).build().verify(decodedJwt));
            } catch (final SignatureVerificationException e) {
                // this certificate does not hold the signing key, try the next one
                lastSignatureFailure = e;
            } catch (final JWTVerificationException e) {
                // the signature matched, so the token itself is not acceptable: no other certificate can change that
                throw new KuraAuthenticationFailedException(e, "Failed to verify token");
            }
        }

        if (lastSignatureFailure != null) {
            throw new KuraAuthenticationFailedException(lastSignatureFailure,
                    "No trusted certificate can verify the token signature");
        }

        throw new KuraAuthenticationFailedException("No usable trusted certificate is available to verify the token");
    }

    private static DecodedJWT decode(final String encodedToken) throws KuraAuthenticationFailedException {
        try {
            return JWT.decode(encodedToken);
        } catch (final JWTDecodeException e) {
            throw new KuraAuthenticationFailedException(e, "Failed to decode JWT");
        }
    }

    private List<TrustAnchor> selectCandidates(final String keyId) {
        final TrustAnchor hinted = keyId == null ? null : this.trustAnchorsByAlias.get(keyId);

        if (hinted == null) {
            return this.trustAnchors;
        }

        // the hinted certificate first, the others as a fallback: the 'kid' names an alias in the issuer's key store,
        // which may differ from this trust store's aliases, and the hinted entry may have been superseded by a renewal
        final List<TrustAnchor> ordered = new ArrayList<>(this.trustAnchorsByAlias.size());
        ordered.add(hinted);
        this.trustAnchorsByAlias.forEach((alias, anchor) -> {
            if (!alias.equals(keyId)) {
                ordered.add(anchor);
            }
        });

        return ordered;
    }

    private Verification verification(final TrustAnchor anchor, final Optional<String> intendedConsumer) {

        final Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) anchor.certificate().getPublicKey(), null);

        Verification verification = JWT.require(algorithm) //
                .acceptLeeway(this.clockSkewToleranceSec);

        if (!this.trustedIssuers.isEmpty()) {
            // an empty array would make java-jwt require 'iss' to be one of no values, rejecting every token
            verification = verification.withIssuer(this.trustedIssuers.toArray(String[]::new));
        }

        if (intendedConsumer.isPresent()) {
            verification = verification.withAudience(intendedConsumer.get());
        }

        return verification;
    }

    private static boolean isWithinValidityPeriod(final TrustAnchor anchor) {
        try {
            anchor.certificate().checkValidity();
            return true;
        } catch (final CertificateExpiredException | CertificateNotYetValidException e) {
            logger.debug("Certificate '{}' is outside its validity period, skipping it", anchor.alias(), e);
            return false;
        }
    }

    private static VerificationProof asVerificationProof(final DecodedJWT verifiedJwt)
            throws KuraAuthenticationFailedException {

        final String subject = verifiedJwt.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new KuraAuthenticationFailedException(
                    "Provided token has no 'sub' claim, it is mandatory. Cannot authenticate");
        }

        return new JwtVerificationProof(subject, Instant.now(), verifiedJwt);
    }

    private static final class JwtVerificationProof implements VerificationProof {

        private final String identityName;
        private final Instant verifiedAt;
        private final DecodedJWT decodedJwt;
        private final Map<String, Object> claims;

        public JwtVerificationProof(final String identityName, final Instant verifiedAt, final DecodedJWT decodedJwt) {
            this.identityName = identityName;
            this.verifiedAt = verifiedAt;
            this.decodedJwt = decodedJwt;
            this.claims = Collections.unmodifiableMap(getClaimsMapFrom(decodedJwt));
        }

        @Override
        public Instant getVerifiedAt() {
            return this.verifiedAt;
        }

        @Override
        public Map<String, Object> getClaims() {
            return this.claims;
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
        public String getIdentityName() {
            return this.identityName;
        }

        @Override
        public Optional<String> getTokenID() {
            return Optional.ofNullable(this.decodedJwt.getId());
        }

        private static Map<String, Object> getClaimsMapFrom(final DecodedJWT decodedJwt) {
            final Map<String, Object> result = new LinkedHashMap<>();

            decodedJwt.getClaims().forEach((name, value) -> {
                if (value.isNull()) {
                    result.put(name, null);
                } else {
                    result.put(name, value.as(Object.class));
                }
            });

            return result;
        }

    }

}