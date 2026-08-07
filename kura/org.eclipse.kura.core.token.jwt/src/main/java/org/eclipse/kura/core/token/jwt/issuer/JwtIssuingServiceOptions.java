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
package org.eclipse.kura.core.token.jwt.issuer;

import java.time.Duration;
import java.util.Optional;

class JwtIssuingServiceOptions {

    private final String signingKeyAlias;
    private final String issuer;
    private final Optional<Duration> maximumLifetime;

    JwtIssuingServiceOptions(JwtIssuingServiceOCD config) {
        this.signingKeyAlias = config.signing_key_alias();
        this.issuer = config.issuer();

        if (config.maximum_lifetime_seconds() > 0) {
            this.maximumLifetime = Optional.of(Duration.ofSeconds(config.maximum_lifetime_seconds()));
        } else {
            this.maximumLifetime = Optional.empty();
        }
    }

    String getSigningKeyAlias() {
        return this.signingKeyAlias;
    }

    String getIssuer() {
        return this.issuer;
    }

    Optional<Duration> getMaximumLifetime() {
        return this.maximumLifetime;
    }

}
