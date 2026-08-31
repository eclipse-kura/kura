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
package org.eclipse.kura.identity;

import java.time.Duration;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Issues and verifies tokens used to authenticate identities within Kura.
 * <p>
 * The specific token format (e.g. JWT, opaque reference token, etc.) and the mechanism used to establish trust in a
 * token (e.g. cryptographic signature, server-side lookup, etc.) are left to the implementation. Implementations are
 * expected to embed or otherwise associate the identity name with the issued token, so that it can be recovered on
 * verification.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.0.0
 */
@ProviderType
public interface IdentityTokenService {

    /**
     * Issues a token for the given identity, valid for the given time-to-live.
     *
     * @param identityName
     *            the name of the identity to issue the token for; must not be {@code null}
     * @param ttl
     *            the duration for which the token should remain valid, starting from now; must not be {@code null}
     * @return the issued token
     * @throws IllegalArgumentException
     *             if {@code identityName} or {@code ttl} is {@code null}
     * @throws KuraException
     *             with {@link KuraErrorCode#ENCODE_ERROR} if the token could not be created
     */
    public String issueTokenFor(String identityName, Duration ttl) throws KuraException;

    /**
     * Verifies the given token.
     * <p>
     * The exact checks performed depend on the implementation, but in general this covers both validation (checking
     * that the token is well-formed) and verification (confirming the token is authentic, unexpired, and otherwise
     * trustworthy according to the implementation's own criteria).
     *
     * @param token
     *            the raw token; must not be {@code null}
     * @return an empty {@link Optional} if verification fails for any reason, or if the identity
     *         name cannot be recovered from the token; otherwise an {@link Optional} containing
     *         the identity name the token was issued for
     * @throws IllegalArgumentException
     *             if {@code token} is {@code null}
     */
    public Optional<String> verifyToken(String token);

}
