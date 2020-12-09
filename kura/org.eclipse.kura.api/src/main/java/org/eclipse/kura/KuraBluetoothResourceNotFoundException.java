/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura;

import org.osgi.annotation.versioning.ProviderType;

/**
 * KuraBluetoothResourceNotFoundException is raised when a resource is not found.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 1.3
 */
@ProviderType
public class KuraBluetoothResourceNotFoundException extends KuraException {

    private static final long serialVersionUID = -1142491109524317287L;

    public KuraBluetoothResourceNotFoundException(Object argument) {
        super(KuraErrorCode.BLE_RESOURCE_NOT_FOUND, null, argument);
    }

    public KuraBluetoothResourceNotFoundException(Throwable cause, Object argument) {
        super(KuraErrorCode.BLE_RESOURCE_NOT_FOUND, cause, argument);
    }
}
