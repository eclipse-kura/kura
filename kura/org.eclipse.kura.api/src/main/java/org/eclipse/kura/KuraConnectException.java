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
 * KuraConnectException is raised during connect failures.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraConnectException extends KuraException {

    private static final long serialVersionUID = 5894832757268538532L;

    public KuraConnectException(Object argument) {
        super(KuraErrorCode.CONNECTION_FAILED, null, argument);
    }

    public KuraConnectException(Throwable cause, Object argument) {
        super(KuraErrorCode.CONNECTION_FAILED, cause, argument);
    }
}
