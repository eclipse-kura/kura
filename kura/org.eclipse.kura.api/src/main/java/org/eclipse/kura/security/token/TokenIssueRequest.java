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
package org.eclipse.kura.security.token;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Describes the token a caller wants issued.
 * 
 * @since 3.0
 */
public final class TokenIssueRequest {

    private final String identityName;
    private final Instant expiresAt;
    private final Instant notBefore;
    private final Set<String> intendedConsumers;
    private final Map<String, Object> claims;

    private TokenIssueRequest(final Builder builder) {
        this.identityName = builder.identityName;
        this.expiresAt = builder.expiresAt;
        this.notBefore = builder.notBefore;
        this.intendedConsumers = Collections.unmodifiableSet(new LinkedHashSet<>(builder.intendedConsumers));
        this.claims = Collections.unmodifiableMap(new LinkedHashMap<>(builder.claims));
    }

    /**
     * 
     * @return the identity name for this request, cannot be {@code null}, empty or whitespace-only
     */
    public String getIdentityName() {
        return this.identityName;
    }

    /**
     * 
     * @return the requested expiration instant, if present
     */
    public Optional<Instant> getExpiresAt() {
        return Optional.ofNullable(this.expiresAt);
    }

    /**
     * 
     * @return the requested not before instant, if present
     */
    public Optional<Instant> getNotBefore() {
        return Optional.ofNullable(this.notBefore);
    }

    /**
     * 
     * @return an unmodifiable set of the requested consumers, cannot be {@code null}. The returned set can be empty if
     *         no explicit consumers were requested. Set entries cannot be {@code null}, nor empty, nor whitespace-only
     *         {@link String}
     */
    public Set<String> getIntendedConsumers() {
        return this.intendedConsumers;
    }

    /**
     * 
     * @return an unmodifiable map of the requested claims, cannot be {@code null}. The returned map can be empty if
     *         no claims were requested. Map keys cannot be {@code null}, nor empty, nor whitespace-only {@link String}.
     *         Supported claim values depend on the implementation
     */
    public Map<String, Object> getClaims() {
        return this.claims;
    }

    /**
     * Get a builder for constructing a {@link TokenIssueRequest} with the identity name that
     * the request should be issued for.
     * 
     * @param identityName
     *            the name of the identity, must not be {@code null}, empty or whitespace-only
     * @return a {@link Builder} for constructing a {@link TokenIssueRequest}
     * @throws NullPointerException
     *             if {@code identityName} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code identityName} is empty or whitespace-only
     */
    public static Builder builder(final String identityName) {
        return new Builder(ensureNotNullNotBlank(identityName, "identityName"));
    }

    private static String ensureNotNullNotBlank(final String value, final String parameterName) {
        final String result = Objects.requireNonNull(value, parameterName + " cannot be null");
        if (result.isBlank()) {
            throw new IllegalArgumentException(parameterName + " cannot be empty or whitespace-only");
        }
        return result;
    }

    public static final class Builder {

        private final String identityName;
        private Instant expiresAt;
        private Instant notBefore;
        private final Set<String> intendedConsumers = new LinkedHashSet<>();
        private final Map<String, Object> claims = new LinkedHashMap<>();

        private Builder(final String identityName) {
            this.identityName = identityName;
        }

        /**
         * Sets the instant at which the request expires. This property is optional.
         *
         * <p>
         * The value is an absolute point in time; once it has passed, the request is no longer considered valid.
         * Calling this method again overwrites any previously set expiration.
         *
         * @param expiresAt
         *            the expiration instant, must not be {@code null}
         * @return this builder instance, for method chaining
         * @throws NullPointerException
         *             if {@code expiresAt} is {@code null}
         */
        public Builder expiresAt(final Instant expiresAt) {
            this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
            return this;
        }

        /**
         * Sets the instant before which the request must not be accepted. This property is optional.
         *
         * <p>
         * The value is an absolute point in time, inclusive: the request becomes valid at this instant. Calling this
         * method again overwrites any previously set value.
         *
         * @param notBefore
         *            the earliest instant at which the request is valid, must not be {@code null}
         * @return this builder instance, for method chaining
         * @throws NullPointerException
         *             if {@code notBefore} is {@code null}
         * @see #expiresAt(Instant)
         */
        public Builder notBefore(final Instant notBefore) {
            this.notBefore = Objects.requireNonNull(notBefore, "notBefore cannot be null");
            return this;
        }

        /**
         * Adds an intended consumer of the token to this request. This property is optional.
         * 
         * <p>
         * Call this method again to add multiple intended consumers to the request.
         * 
         * <p>
         * Purpose of specifying an intended consumer is that a token consumer (like a token verifier) can reject the
         * presented token if it is not for him.
         * 
         * @param consumer
         *            the consumer name, cannot be {@code null}, empty or whitespace-only
         * @return this builder instance, for method chaining
         * @throws NullPointerException
         *             if {@code consumer} is {@code null}
         * @throws IllegalArgumentException
         *             if {@code consumer} is empty or whitespace-only
         */
        public Builder intendedConsumer(String consumer) {
            this.intendedConsumers.add(ensureNotNullNotBlank(consumer, "consumer"));
            return this;
        }

        /**
         * Sets a claim in the generic form of key-value pair. This property is optional.
         * 
         * <p>
         * Call this method again to add multiple claims to the request. When called on a claim with the same name, its
         * value gets updated.
         * 
         * @param name
         *            the claim name, cannot be {@code null}, empty or whitespace-only
         * @param value
         *            the claim value, supported claim values depend on the implementation
         * @return this builder instance, for method chaining
         * @throws NullPointerException
         *             if {@code name} is {@code null}
         * @throws IllegalArgumentException
         *             if {@code name} is empty or whitespace-only
         */
        public Builder claim(String name, Object value) {
            final String key = ensureNotNullNotBlank(name, "name");
            this.claims.put(key, value);
            return this;
        }

        /**
         * Builds a {@link TokenIssueRequest}.
         * 
         * @return a {@link TokenIssueRequest}
         */
        public TokenIssueRequest build() {
            return new TokenIssueRequest(this);
        }
    }
}
