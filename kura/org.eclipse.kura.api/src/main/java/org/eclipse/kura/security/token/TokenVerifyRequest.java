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

import java.util.Objects;
import java.util.Optional;

/**
 * Describes what the caller wants to verify on the token.
 * 
 * @since 3.0
 */
public final class TokenVerifyRequest {

    private final String token;
    private final String intendedConsumer;

    private TokenVerifyRequest(final Builder builder) {
        this.token = builder.token;
        this.intendedConsumer = builder.intendedConsumer;
    }

    /**
     * 
     * @return the encoded token associated with this request, cannot be {@code null}, or empty, or whitespace-only
     */
    public String getToken() {
        return this.token;
    }

    /**
     * 
     * @return the intended consumer for the presented token, so that token verifier can reject the presented token if
     *         it is not for him, cannot be {@code null}
     */
    public Optional<String> getIntendedConsumer() {
        return Optional.ofNullable(this.intendedConsumer);
    }

    /**
     * Get a builder for constructing a {@link TokenVerifyRequest} with the token that needs to be verified.
     * 
     * @param token
     *            the encoded token to be verified, must not be {@code null}, nor empty, nor whitespace-only
     * @return a {@link Builder} for constructing a {@link TokenVerifyRequest}
     * @throws NullPointerException
     *             if the provided token is {@code null}
     * @throws IllegalArgumentException
     *             if {@code token} is empty or whitespace-only
     */
    public static Builder builder(final String token) {
        return new Builder(ensureNotNullNotBlank(token, "token"));
    }

    private static String ensureNotNullNotBlank(final String value, final String parameterName) {
        final String result = Objects.requireNonNull(value, parameterName + " cannot be null");
        if (result.isBlank()) {
            throw new IllegalArgumentException(parameterName + " cannot be empty or whitespace-only");
        }
        return result;
    }

    public static final class Builder {

        private final String token;
        private String intendedConsumer;

        private Builder(final String token) {
            this.token = token;
        }

        /**
         * Sets the identifier of the component that is presenting the token for use. This property is optional.
         *
         * <p>
         * When set, verification succeeds only if the token was issued for use by the specified consumer. This prevents
         * a token obtained by one component from being replayed against a different, possibly more privileged, one.
         * </p>
         *
         * <p>
         * If this property is left unset, no consumer constraint is enforced and a token issued for any consumer is
         * accepted.
         * </p>
         *
         * @param intendedConsumer
         *            the identifier of the component presenting the token, must not be {@code null}, empty or
         *            whitespace-only
         * @return this builder instance, for method chaining
         * @throws NullPointerException
         *             if the provided intendedConsumer is {@code null}
         * @throws IllegalArgumentException
         *             if {@code intendedConsumer} is empty or whitespace-only
         */
        public Builder intendedConsumer(final String intendedConsumer) {
            this.intendedConsumer = ensureNotNullNotBlank(intendedConsumer, "intendedConsumer");
            return this;
        }

        /**
         * Builds a {@link TokenVerifyRequest}.
         * 
         * @return a {@link TokenVerifyRequest}
         */
        public TokenVerifyRequest build() {
            return new TokenVerifyRequest(this);
        }

    }
}