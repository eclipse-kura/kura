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

import java.time.Duration;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service that issues authentication tokens.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.0
 */
@ProviderType
public interface TokenIssuingService {

    /**
     * The configured upper bound on the lifetime of issued tokens.
     * 
     * @return the maximum lifetime, empty if any lifetime is accepted
     */
    public Optional<Duration> getMaximumLifetime();

    /**
     * Issues a new token for the identity and validity window described by the given request.
     *
     * <p>
     * Each invocation issues a distinct token: this method is not idempotent, and calling it twice with equal requests
     * yields two independently valid tokens.
     *
     * <p>
     * The returned value is a credential and should be treated as a secret. It must not be logged, and it is the
     * caller's responsibility to transport it over a secured channel.
     *
     * @param request
     *            the parameters of the token to be issued, must not be
     *            {@code null}
     * @return the encoded token, never {@code null} nor empty
     * @throws KuraException
     *             if the token cannot be issued
     * @throws NullPointerException
     *             if {@code request} is {@code null}
     */
    public String issue(final TokenIssueRequest request) throws KuraException;

}