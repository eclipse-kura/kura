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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "org.eclipse.kura.core.token.jwt.JwtServiceImpl", //
        name = "JWT Service", //
        description = "The JwtService issues and verifies JSON Web Tokens. Tokens are signed with RS256, using an RSA"
                + " private key and verified against trusted certificates of a KeystoreService instance.")
public @interface JwtServiceOCD {

    public static final String DEFAULT_KEYSTORE_TARGET_FILTER = "(kura.service.pid=changeme)";
    public static final String DEFAULT_SIGNING_KEY_ALIAS = "jwt-signing-key";
    public static final String DEFAULT_ISSUER = "kura";
    public static final int DEFAULT_MAX_TOKEN_LIFETIME_SEC = 3600;
    public static final int DEFAULT_CLOCK_SKEW_SECONDS = 30;
    public static final boolean DEFAULT_REQUIRE_VALID_CERTIFICATE = true;

    @AttributeDefinition(name = "Keystore Target Filter", //
            required = true, //
            description = "Specifies, as an OSGi target filter, the pid of the KeystoreService that holds the"
                    + " private key entry used to sign issued tokens and the trusted certificates. If the target"
                    + " service cannot be found, or it does not contain the configured signing key alias, token"
                    + " issuing will fail.")
    public String KeystoreService_target() default DEFAULT_KEYSTORE_TARGET_FILTER;

    @AttributeDefinition(name = "Signing Key Alias", //
            required = false, //
            description = "The alias of the RSA private key entry, in the configured key store, used to sign issued"
                    + " tokens. The alias is published as the 'kid' header of issued tokens, so that verifiers can select the"
                    + " matching certificate. If the alias is empty this instance will be able to verify tokens but not to"
                    + " issue them. If the alias cannot be resolved to an RSA private key entry, then token issuing will fail.")
    public String signing_key_alias() default DEFAULT_SIGNING_KEY_ALIAS;

    @AttributeDefinition(name = "Issuer", //
            required = true, //
            description = "The value asserted in the 'iss' claim of issued tokens.")
    public String issuer() default DEFAULT_ISSUER;

    @AttributeDefinition(name = "Trusted Issuers", //
            required = false, //
            description = "Comma-separated list of issuers accepted when verifying tokens, that is, the accepted"
                    + " values of the 'iss' claim. If left empty only the value of the Issuer parameter is accepted.")
    public String trusted_issuers();

    @AttributeDefinition(name = "Verification Key Aliases", //
            required = false, //
            description = "Comma-separated list of trust store aliases whose certificates are accepted when"
                    + " verifying tokens. If left empty, every trusted certificate entry of the configured trust"
                    + " store that holds an RSA key is accepted. Restricting this list is recommended when the trust"
                    + " store is shared with TLS, so that arbitrary TLS certificate authorities cannot be used to"
                    + " mint valid tokens. When restricting, the signing key is automatically added to this whitelist.")
    public String verification_key_aliases();

    @AttributeDefinition(name = "Maximum Token Lifetime (sec)", //
            required = true, //
            min = "0",
            description = "The upper bound, in seconds, on the lifetime of issued tokens. Token issue requests"
                    + " made with the expires at (exp) claim longer than this value will have exp claim set to this value."
                    + " Set to 0 to accept any requested lifetime and to issue tokens without expiration when no lifetime"
                    + " is requested.")
    public int maximum_lifetime_seconds() default DEFAULT_MAX_TOKEN_LIFETIME_SEC;

    @AttributeDefinition(name = "Clock Skew Tolerance (sec)", //
            required = true, //
            min = "0", //
            description = "The tolerance, in seconds, applied to the 'exp', 'nbf' and 'iat' claims during"
                    + " verification, to compensate for clock drift between the issuer and the verifier.")
    public int clock_skew_seconds() default DEFAULT_CLOCK_SKEW_SECONDS;

    @AttributeDefinition(name = "Require Valid Certificate", //
            required = true, //
            description = "If enabled, a trust store certificate is used for verification only while it is within"
                    + " its own validity period, so an expired signing certificate stops validating tokens. If"
                    + " disabled, the certificate validity period is ignored and only the token claims are checked.")
    public boolean require_valid_certificate() default DEFAULT_REQUIRE_VALID_CERTIFICATE;

}
