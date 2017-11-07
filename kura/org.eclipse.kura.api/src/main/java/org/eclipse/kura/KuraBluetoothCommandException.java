/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura;

import org.osgi.annotation.versioning.ProviderType;

/**
 * KuraBluetoothCommandException is raised when a command returns an error.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 1.3
 */
@ProviderType
public class KuraBluetoothCommandException extends KuraException {

    private static final long serialVersionUID = -5848254103027432830L;

    public KuraBluetoothCommandException(Object argument) {
        super(KuraErrorCode.BLE_COMMAND_ERROR, null, argument);
    }

    public KuraBluetoothCommandException(Throwable cause, Object argument) {
        super(KuraErrorCode.BLE_COMMAND_ERROR, cause, argument);
    }
}