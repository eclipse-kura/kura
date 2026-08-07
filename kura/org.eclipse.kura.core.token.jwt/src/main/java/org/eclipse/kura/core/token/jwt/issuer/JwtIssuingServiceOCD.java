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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "org.eclipse.kura.core.token.jwt.issuer.JwtIssuingService", //
        name = "JWT Issuing Service", //
        description = "The JwtIssuingService issues JSON Web Tokens. Tokens are signed with RS256, using an RSA"
                + " private key and verified against trusted certificates of a KeystoreService instance.")
public @interface JwtIssuingServiceOCD {

    public static final String DEFAULT_KEYSTORE_TARGET_FILTER = "(kura.service.pid=changeme)";
    public static final String DEFAULT_SIGNING_KEY_ALIAS = "jwt-signing-key";
    public static final String DEFAULT_ISSUER = "kura";
    public static final int DEFAULT_MAX_TOKEN_LIFETIME_SEC = 3600;

    @AttributeDefinition(name = "Keystore Target Filter", //
            required = true, //
            description = "Specifies, as an OSGi target filter, the pid of the KeystoreService that holds the"
                    + " private key entry used to sign issued tokens.")
    public String KeystoreService_target() default DEFAULT_KEYSTORE_TARGET_FILTER;

    @AttributeDefinition(name = "Signing Key Alias", //
            required = true, //
            description = "The alias of the RSA private key entry, in the configured key store, used to sign issued"
                    + " tokens. The alias is published as the 'kid' header of issued tokens, so that verifiers can select the"
                    + " matching certificate. If the alias cannot be resolved to an RSA private key entry, then token"
                    + " issuing will fail.")
    public String signing_key_alias() default DEFAULT_SIGNING_KEY_ALIAS;

    @AttributeDefinition(name = "Issuer", //
            required = true, //
            description = "The issuer set as the 'iss' claim of issued tokens.")
    public String issuer() default DEFAULT_ISSUER;

    @AttributeDefinition(name = "Maximum Token Lifetime (sec)", //
            required = true, //
            min = "0", description = "The upper bound, in seconds, on the lifetime of issued tokens. Token issue requests"
                    + " made with the expires at (exp) claim longer than this value will have exp claim set to this value."
                    + " Set to 0 to accept any requested lifetime and to issue tokens without expiration when no lifetime"
                    + " is requested.")
    public int maximum_lifetime_seconds() default DEFAULT_MAX_TOKEN_LIFETIME_SEC;


}
