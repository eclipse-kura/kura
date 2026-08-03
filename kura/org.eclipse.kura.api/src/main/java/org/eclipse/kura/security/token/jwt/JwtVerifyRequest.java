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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Describes what the caller wants to verify on the encoded JSON Web Token (JWT).
 * 
 * @since 3.0
 */
public final class JwtVerifyRequest {

    private final Set<String> acceptedAudience;
    private final String token;

    private JwtVerifyRequest(final Builder builder) {
        this.acceptedAudience = Collections.unmodifiableSet(new LinkedHashSet<>(builder.acceptedAudience));
        this.token = builder.token;
    }

    /**
     * 
     * @return the audience values the caller identifies with, empty if the {@code aud} claim must not be checked
     */
    public Set<String> getAcceptedAudience() {
        return this.acceptedAudience;
    }

    /**
     * 
     * @return the encoded JWT token associated with this request
     */
    public String getToken() {
        return this.token;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final Set<String> acceptedAudience = new LinkedHashSet<>();
        private boolean acceptAnyAudience = false;
        private String token;

        private Builder() {
        }

        /**
         * The token is accepted only if its 'aud' claim contains at least one of these values. A token carrying
         * no 'aud' claim is rejected.
         * 
         * @param audience
         *            cannot be {@code null} and cannot contain {@code null} values.
         */
        public Builder audience(final Set<String> audience) {
            for (final String entry : Objects.requireNonNull(audience, "audience cannot be null")) {
                this.acceptedAudience.add(Objects.requireNonNull(entry, "audience cannot contain null values"));
            }
            return this;
        }

        /**
         * Disables the 'aud' claim check, accepting tokens issued for any audience. Use only for tokens that are not
         * audience scoped.
         */
        public Builder acceptAnyAudience() {
            this.acceptAnyAudience = true;
            return this;
        }

        /**
         * @param token
         *            the token associated with this request.
         */
        public Builder token(final String token) {
            Objects.requireNonNull(token, "token cannot be null");
            if (token.isBlank()) {
                throw new IllegalArgumentException("token cannot be empty or contain only whitespaces");
            }
            this.token = token;
            return this;
        }

        public JwtVerifyRequest build() {
            Objects.requireNonNull(this.token, "setting the token is mandatory");

            if (this.acceptedAudience.isEmpty() && !this.acceptAnyAudience) {
                throw new IllegalStateException("an expected audience is required, or acceptAnyAudience()");
            }
            if (!this.acceptedAudience.isEmpty() && this.acceptAnyAudience) {
                throw new IllegalStateException("acceptAnyAudience() cannot be combined with an expected audience");
            }

            return new JwtVerifyRequest(this);
        }

    }
}
