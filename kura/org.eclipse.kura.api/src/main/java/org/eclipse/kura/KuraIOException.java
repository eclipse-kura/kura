/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
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
 * KuraIOException is raised when an IO failure is detected.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 3.0
 */
@ProviderType
public class KuraIOException extends KuraException {

    private static final long serialVersionUID = 4440807883112997377L;

    public KuraIOException(Object argument) {
        super(KuraErrorCode.IO_ERROR, null, argument);
    }

    public KuraIOException(Throwable cause, Object argument) {
        super(KuraErrorCode.IO_ERROR, cause, argument);
    }
}
