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

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The outcome of a successful verification.
 *
 * <p>
 * Holding an instance of this type <em>is</em> the proof that verification succeeded.
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.0
 */
@ProviderType
public interface JwtVerificationProof {

    /**
     * 
     * @return the instant at which verification ran, as seen by the verifier clock, cannot be {@code null}
     */
    public Instant getVerifiedAt();

    /**
     * 
     * @return The verified claims, registered and custom, keyed by claim name. Cannot be {@code null}
     */
    public Map<String, Object> getClaims();
    
    /**
     * 
     * @return the verified token 'iss' claim, if present
     */
    public Optional<String> getIssuer();
    
    /**
     * 
     * @return the verified token 'sub' claim, if present
     */
    public Optional<String> getSubject();
    
    /**
     * 
     * @return the verified token 'aud' claim, if present. If present, then values of the returned Set are not
     *         {@code null}
     */
    public Optional<Set<String>> getAudience();
    
    /**
     * 
     * @return the verified token 'exp' claim, if present
     */
    public Optional<Instant> getExpiresAt();

    /**
     * 
     * @return the verified token 'nbf' claim, if present
     */
    public Optional<Instant> getNotBefore();

    /**
     * 
     * @return the verified token 'iat' claim, if present
     */
    public Optional<Instant> getIssuedAt();

    /**
     * 
     * @return the verified token 'jti' claim, if present
     */
    public Optional<String> getJti();

}