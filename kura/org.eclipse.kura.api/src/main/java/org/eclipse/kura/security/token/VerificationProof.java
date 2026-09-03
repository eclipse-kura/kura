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
package org.eclipse.kura.security.token;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

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
public interface VerificationProof {

    /**
     * Returns the identity name bound to the token.
     *
     * @return the identity name the token is bound to, cannot be {@code null}, empty or whitespace-only
     */
    public String getIdentityName();

    /**
     * Returns the instant at which verification ran.
     *
     * @return the instant at which verification ran, as seen by the verifier clock, cannot be {@code null}
     */
    public Instant getVerifiedAt();

    /**
     * Returns the instant after which the verified token is no longer valid.
     *
     * <p>
     * The expiration was checked as part of verification and had not been reached at {@link #getVerifiedAt()}. It is
     * reported here so that callers can decide when to obtain a fresh token, and to bound how long this instance may be
     * retained.
     * </p>
     *
     * <p>
     * An empty value means the token carries no intrinsic expiration. Callers should treat such tokens with additional
     * care and must not retain this instance indefinitely on the strength of a missing expiration.
     *
     * @return the instant at which the verified token expires, or an empty {@link Optional} if the token does not
     *         expire on its own, cannot be {@code null}
     */
    public Optional<Instant> getExpiresAt();

    /**
     * Returns the instant before which the verified token was not valid.
     *
     * <p>
     * This constraint was checked as part of verification and had already been satisfied at {@link #getVerifiedAt()}.
     * The value is reported for informational and auditing purposes.
     *
     * @return the instant from which the verified token became valid, or an empty {@link Optional} if the token
     *         declares no such constraint, cannot be {@code null}
     */
    public Optional<Instant> getNotBefore();

    /**
     * Returns the instant at which the verified token was issued.
     *
     * @return the instant at which the verified token was issued, or an empty {@link Optional} if the token does not
     *         record it, cannot be {@code null}
     */
    public Optional<Instant> getIssuedAt();

    /**
     * Returns the identifier of the verified token.
     *
     * <p>
     * It is intended for correlating issuance.
     *
     * @return the identifier of the verified token, or an empty {@link Optional} if the implementation does not assign
     *         one, cannot be {@code null}
     */
    public Optional<String> getTokenID();

    /**
     * Returns the claims of the verified token.
     * 
     * <p>
     * Depending on the implementation, the returned map may or may not contain the claims that can be accessed by the
     * other methods of this interface, like {@link VerificationProof#getTokenID()} or
     * {@link VerificationProof#getExpiresAt()}.
     * 
     * @return a map of the verified claims, cannot be {@code null}, can be empty. Map keys cannot be {@code null}, nor
     *         empty, nor whitespace-only {@link String}. Supported claim values depend on the implementation
     */
    public Map<String, Object> getClaims();

}