/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
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
 * KuraConnectException is raised during disconnection failures.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
@ProviderType
public class KuraDisconnectException extends KuraException{

    private static final long serialVersionUID = 52917095245324570L;

    public KuraDisconnectException(Object argument) {
        super(KuraErrorCode.DISCONNECTION_FAILED, null, argument);
    }
    
    public KuraDisconnectException(Throwable cause, Object argument) {
        super(KuraErrorCode.DISCONNECTION_FAILED, cause, argument);
    }
}
