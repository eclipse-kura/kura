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

import java.time.Duration;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service that issues and verifies JSON Web Tokens (JWT).
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.0
 */
@ProviderType
public interface JwtService {

    /**
     * The configured upper bound on the lifetime of issued tokens.
     * 
     * @return the maximum lifetime, empty if any lifetime is accepted
     */
    public Optional<Duration> getMaximumLifetime();

    /**
     * Issues a signed token.
     * 
     * @param request
     *            the claims to assert, must not be {@code null}
     * @return the issued, signed token. Never {@code null}
     * @throws KuraException
     *             if the signature fails for any reason
     */
    public String issue(final JwtIssueRequest request) throws KuraException;

    /**
     * Verifies a token.
     * 
     * @param request
     *            the request containing the encoded token, along with some other verification requirements. Must not be
     *            {@code null}
     * @return the proof of verification
     * @throws KuraException
     *             if verification fails for any reason. If authentication fails, it is a
     *             {@link org.eclipse.kura.KuraAuthenticationFailedException}
     */
    public JwtVerificationProof verify(final JwtVerifyRequest request) throws KuraException;

}
