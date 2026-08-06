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

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service that verifies authentication tokens.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.0
 */
@ProviderType
public interface TokenVerificationService {

    /**
     * Verifies a token.
     *
     * @param request
     *            the verification request, carrying the encoded token and the constraints to be enforced, must not be
     *            {@code null}
     * @return the proof of verification describing the verified identity and the properties of the token, never
     *         {@code null}
     * @throws KuraException
     *             if verification fails for any reason. If authentication fails, it is a
     *             {@link org.eclipse.kura.KuraAuthenticationFailedException}
     */
    public VerificationProof verify(final TokenVerifyRequest request) throws KuraException;

}