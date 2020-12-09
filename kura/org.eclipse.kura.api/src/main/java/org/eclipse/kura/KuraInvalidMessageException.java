/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraInvalidMessageException extends KuraRuntimeException {

    private static final long serialVersionUID = -3636897647706575102L;

    public KuraInvalidMessageException(Object argument) {
        super(KuraErrorCode.INVALID_MESSAGE_EXCEPTION, argument);
    }

    public KuraInvalidMessageException(Throwable cause) {
        super(KuraErrorCode.INVALID_MESSAGE_EXCEPTION, cause);
    }

    public KuraInvalidMessageException(Throwable cause, Object argument) {
        super(KuraErrorCode.INVALID_MESSAGE_EXCEPTION, cause, argument);
    }
}
