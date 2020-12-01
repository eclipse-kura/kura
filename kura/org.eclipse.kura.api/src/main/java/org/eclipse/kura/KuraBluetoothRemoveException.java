/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
