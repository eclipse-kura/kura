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
 ******************************************************************************/
package org.eclipse.kura.security.token.jwt;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Describes the JSON Web Token (JWT) a caller wants issued.
 * 
 * <p>
 * A request says <em>what should be asserted</em>, never <em>how it should be protected</em>. Issuer identity, signing
 * key alias, issuing time (iat), clock source, algorithm, key rotation and maximum lifetime belong to the configuration
 * of the {@link JwtService} instance.
 * </p>
 * 
 * @since 3.0
 */
public final class JwtIssueRequest {
 
    /**
     * Registered claims: "iss", "sub", "aud", "exp", "nbf", "iat", "jti"
     */
    public static final Set<String> REGISTERED_CLAIMS = Set.of("iss", "sub", "aud", "exp", "nbf", "iat", "jti");

    private final String jti;
    private final String subject;
    private final Set<String> audience;
    private final Instant expiresAt;
    private final Instant notBefore;
    private final Map<String, Object> additionalClaims;

    private JwtIssueRequest(final Builder builder) {
        this.jti = builder.jti;
        this.subject = builder.subject;
        this.audience = Collections.unmodifiableSet(new LinkedHashSet<>(builder.audience));
        this.expiresAt = builder.expiresAt;
        this.notBefore = builder.notBefore;
        this.additionalClaims = Collections.unmodifiableMap(new LinkedHashMap<>(builder.additionalClaims));
    }

    /**
     * 
     * @return this request's 'jti' claim, if present
     */
    public Optional<String> getJti() {
        return Optional.ofNullable(this.jti);
    }

    /**
     * 
     * @return this request's 'sub' claim, if present
     */
    public Optional<String> getSubject() {
        return Optional.ofNullable(this.subject);
    }

    /**
     * 
     * @return this request's 'aud' claim, empty if not present
     */
    public Set<String> getAudience() {
        return this.audience;
    }

    /**
     * 
     * @return this request's 'exp' claim, if present
     */
    public Optional<Instant> getExpiresAt() {
        return Optional.ofNullable(this.expiresAt);
    }

    /**
     * 
     * @return this request's 'nbf' claim, if present
     */
    public Optional<Instant> getNotBefore() {
        return Optional.ofNullable(this.notBefore);
    }

    /**
     * 
     * @return this request's additional claims. Returns an empty map if no additional claims except the
     *         standard ones are present
     */
    public Map<String, Object> getAdditionalClaims() {
        return this.additionalClaims;
    }
 
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String jti;
        private String subject;
        private final Set<String> audience = new LinkedHashSet<>();;
        private Instant expiresAt;
        private Instant notBefore;
        private final Map<String, Object> additionalClaims = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * 
         * @param jti
         *            build the request with the 'jti' claim, cannot be {@code null}
         */
        public Builder jti(final String jti) {
            this.jti = Objects.requireNonNull(jti, "jti cannot be null");
            return this;
        }

        /**
         * 
         * @param sub
         *            build the request with the 'sub' claim, cannot be {@code null}
         */
        public Builder subject(final String sub) {
            this.subject = Objects.requireNonNull(sub, "subject cannot be null");
            return this;
        }

        /**
         * 
         * @param aud
         *            build the request with the 'aud' claim, cannot be {@code null} and cannot contain {@code null}
         *            values
         */
        public Builder audience(final Set<String> aud) {
            for (final String entry : Objects.requireNonNull(aud, "aud cannot be null")) {
                this.audience.add(Objects.requireNonNull(entry, "aud cannot contain null values"));
            }

            return this;
        }

        /**
         * 
         * @param exp
         *            build the request with the 'exp' claim, cannot be {@code null}
         */
        public Builder expiresAt(final Instant exp) {
            this.expiresAt = Objects.requireNonNull(exp, "exp cannot be null");
            return this;
        }

        /**
         * 
         * @param nbf
         *            build the request with the 'nbf' claim, cannot be {@code null}
         */
        public Builder notBefore(final Instant nbf) {
            this.notBefore = Objects.requireNonNull(nbf, "nbf cannot be null");
            return this;
        }

        /**
         * Adds a custom claim.
         * 
         * @param name
         *            the name of the claim, cannot be {@code null}
         * @param value
         *            value of the claim
         */
        public Builder claim(final String name, final Object value) {
            Objects.requireNonNull(name, "claim name cannot be null");

            if (REGISTERED_CLAIMS.contains(name)) {
                throw new IllegalArgumentException("claim '" + name + "' is registered and managed by the issuer");
            }

            this.additionalClaims.put(name, value);
            return this;
        }

        public JwtIssueRequest build() {
            return new JwtIssueRequest(this);
        }
    }
}