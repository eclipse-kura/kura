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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "org.eclipse.kura.core.token.jwt.verifier.JwtVerificationService", //
        name = "JWT Verification Service", //
        description = "The JwtVerificationService verifies JSON Web Tokens. Can verify tokens are signed with RS256, using an RSA"
                + " private key and are verified against trusted certificates of a KeystoreService instance.")
public @interface JwtVerificationServiceOCD {

    public static final String DEFAULT_KEYSTORE_TARGET_FILTER = "(kura.service.pid=changeme)";
    public static final int DEFAULT_CLOCK_SKEW_SECONDS = 30;
    public static final boolean DEFAULT_REQUIRE_VALID_CERTIFICATE = true;

    @AttributeDefinition(name = "Truststore Target Filter", //
            required = true, //
            description = "Specifies, as an OSGi target filter, the pid of the KeystoreService that holds the"
                    + " trusted certificates used for verifying tokens.")
    public String KeystoreService_target() default DEFAULT_KEYSTORE_TARGET_FILTER;

    @AttributeDefinition(name = "Trusted Issuers", //
            required = false, //
            description = "Comma-separated list of issuers accepted when verifying tokens, that is, the accepted"
                    + " values of the 'iss' claim. If left empty, the 'iss' claim is not checked.")
    public String trusted_issuers();

    @AttributeDefinition(name = "Verification Key Aliases", //
            required = false, //
            description = "Comma-separated list of trust store aliases whose certificates are accepted when"
                    + " verifying tokens. If left empty, every trusted certificate entry of the configured trust"
                    + " store that holds an RSA key is accepted. Restricting this list is recommended when the trust"
                    + " store is shared with TLS.")
    public String verification_key_aliases();

    @AttributeDefinition(name = "Clock Skew Tolerance (sec)", //
            required = true, //
            min = "0", //
            description = "The tolerance window, in seconds, applied to the 'exp', 'nbf' and 'iat' claims during"
                    + " verification, to compensate for clock drift between the issuer and the verifier.")
    public int clock_skew_seconds() default DEFAULT_CLOCK_SKEW_SECONDS;

    @AttributeDefinition(name = "Require Valid Certificate", //
            required = true, //
            description = "If enabled, a trust store certificate is used for verification only while it is within"
                    + " its own validity period, so an expired signing certificate stops validating tokens. If"
                    + " disabled, the certificate validity period is ignored and only the token claims are checked.")
    public boolean require_valid_certificate() default DEFAULT_REQUIRE_VALID_CERTIFICATE;

}
