/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura;

import org.osgi.annotation.versioning.ProviderType;

/**
 * KuraIOException is raised when an IO failure is detected.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.2
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
