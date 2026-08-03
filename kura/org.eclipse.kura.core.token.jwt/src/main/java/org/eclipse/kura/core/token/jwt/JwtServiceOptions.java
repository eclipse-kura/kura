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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

class JwtServiceOptions {

    private final Optional<String> signingKeyAlias;
    private final String issuer;
    private final Set<String> trustedIssuers;
    private final Set<String> verificationKeyAliases;
    private final Optional<Duration> maximumLifetime;
    private final long clockSkewSeconds;
    private final boolean requireValidCertificate;

    JwtServiceOptions(final JwtServiceOCD config) {
        String alias = config.signing_key_alias();
        if (alias == null || alias.isBlank()) {
            this.signingKeyAlias = Optional.empty();
        } else {
            this.signingKeyAlias = Optional.of(alias);
        }

        this.issuer = config.issuer();

        Set<String> issuers = new LinkedHashSet<>();
        issuers.add(this.issuer);
        issuers.addAll(csvToStringSet(config.trusted_issuers()));

        this.trustedIssuers = Collections.unmodifiableSet(issuers);
        this.verificationKeyAliases = csvToStringSet(config.verification_key_aliases());
        this.maximumLifetime = toMaximumLifetime(config.maximum_lifetime_seconds());
        this.clockSkewSeconds = config.clock_skew_seconds();
        this.requireValidCertificate = config.require_valid_certificate();
    }

    Optional<String> getSigningKeyAlias() {
        return this.signingKeyAlias;
    }

    String getIssuer() {
        return this.issuer;
    }

    Set<String> getAcceptedIssuers() {
        return this.trustedIssuers;
    }

    /**
     * 
     * @return the trust store aliases allowed for verification, empty if every trusted certificate entry is allowed
     */
    Set<String> getVerificationKeyAliases() {
        return this.verificationKeyAliases;
    }

    /**
     * 
     * @return if empty, accept any requested lifetime
     */
    Optional<Duration> getMaximumLifetime() {
        return this.maximumLifetime;
    }

    /**
     * 
     * @return always >= 0
     */
    long getClockSkewSeconds() {
        return this.clockSkewSeconds;
    }

    boolean isRequireValidCertificate() {
        return this.requireValidCertificate;
    }

    private static Optional<Duration> toMaximumLifetime(int configMaxLifetime) {
        if (configMaxLifetime > 0) {
            return Optional.of(Duration.ofSeconds(configMaxLifetime));
        }

        return Optional.empty();
    }

    private static Set<String> csvToStringSet(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptySet();
        }

        final Set<String> result = new LinkedHashSet<>();
        for (String value : csv.split(",")) {
            if (!value.isBlank()) {
                result.add(value.trim());
            }
        }

        return Collections.unmodifiableSet(result);
    }

}
