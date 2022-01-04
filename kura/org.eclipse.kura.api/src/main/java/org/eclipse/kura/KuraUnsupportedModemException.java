/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
 * KuraUnsupportedModemException is raised when a modem of unknown type is attached.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.3
 */
@ProviderType
public class KuraUnsupportedModemException extends KuraException {

    private static final long serialVersionUID = -8851093400895948523L;

    public KuraUnsupportedModemException(Object argument) {
        super(KuraErrorCode.UNSUPPORTED_MODEM, null, argument);
    }

    public KuraUnsupportedModemException(Throwable cause, Object argument) {
        super(KuraErrorCode.UNSUPPORTED_MODEM, cause, argument);
    }
}
