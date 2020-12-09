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
 * KuraStoreException is raised when a failure occurred during a persistence operation.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraStoreException extends KuraException {

    private static final long serialVersionUID = -3405089623687223551L;

    public KuraStoreException(Object argument) {
        super(KuraErrorCode.STORE_ERROR, null, argument);
    }

    public KuraStoreException(Throwable cause, Object argument) {
        super(KuraErrorCode.STORE_ERROR, cause, argument);
    }
}
