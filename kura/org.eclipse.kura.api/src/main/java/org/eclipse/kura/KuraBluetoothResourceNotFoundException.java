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