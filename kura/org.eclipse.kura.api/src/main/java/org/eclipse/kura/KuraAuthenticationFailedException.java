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
package org.eclipse.kura;

import org.osgi.annotation.versioning.ProviderType;

/**
 * KuraAuthenticationFailedException is raised when an authentication failure occurs.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 3.0
 */
@ProviderType
public class KuraAuthenticationFailedException extends KuraException {

    private static final long serialVersionUID = 7468903237373092296L;

    public KuraAuthenticationFailedException(Throwable cause, String message) {
        super(KuraErrorCode.AUTHENTICATION_FAILED, cause, message);
    }

    public KuraAuthenticationFailedException(String message) {
        super(KuraErrorCode.AUTHENTICATION_FAILED, null, message);
    }

}
