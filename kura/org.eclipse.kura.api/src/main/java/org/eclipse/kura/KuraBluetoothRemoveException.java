/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
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
 * KuraBluetoothRemoveException is raised when an error is detected during removing a device from the system.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
@ProviderType
public class KuraBluetoothRemoveException extends KuraException {

    private static final long serialVersionUID = 6080252526631911747L;

    public KuraBluetoothRemoveException(Object argument) {
        super(KuraErrorCode.BLE_REMOVE_ERROR, null, argument);
    }

    public KuraBluetoothRemoveException(Throwable cause, Object argument) {
        super(KuraErrorCode.BLE_REMOVE_ERROR, cause, argument);
    }
}
