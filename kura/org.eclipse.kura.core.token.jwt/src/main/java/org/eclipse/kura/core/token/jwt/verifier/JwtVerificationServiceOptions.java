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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

class JwtVerificationServiceOptions {

    private final Set<String> trustedIssuers;
    private final Set<String> verificationKeyAliases;
    private final long clockSkewToleranceSec;
    private final boolean requireValidCertificate;

    public JwtVerificationServiceOptions(JwtVerificationServiceOCD config) {
        this.trustedIssuers = csvToStringSet(config.trusted_issuers());
        this.verificationKeyAliases = csvToStringSet(config.verification_key_aliases());
        this.clockSkewToleranceSec = config.clock_skew_seconds();
        this.requireValidCertificate = config.require_valid_certificate();
    }

    Set<String> getTrustedIssuers() {
        return this.trustedIssuers;
    }

    Set<String> getVerificationKeyAliases() {
        return this.verificationKeyAliases;
    }

    long getClockSkewToleranceSec() {
        return this.clockSkewToleranceSec;
    }

    boolean isRequireValidCertificate() {
        return this.requireValidCertificate;
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
