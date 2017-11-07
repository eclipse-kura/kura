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
 * KuraBluetoothIOException is raised when an error is detected during IO operations.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 1.3
 */
@ProviderType
public class KuraBluetoothIOException extends KuraException {

    private static final long serialVersionUID = -2183860317209493405L;

    public KuraBluetoothIOException(Object argument) {
        super(KuraErrorCode.BLE_IO_ERROR, null, argument);
    }

    public KuraBluetoothIOException(Throwable cause, Object argument) {
        super(KuraErrorCode.BLE_IO_ERROR, cause, argument);
    }
}