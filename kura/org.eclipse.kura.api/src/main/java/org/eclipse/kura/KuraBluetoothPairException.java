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
 * KuraBluetoothPairException is raised when an error is detected during the device pairing.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 1.3
 */
@ProviderType
public class KuraBluetoothPairException extends KuraException {

    private static final long serialVersionUID = 4156356604467216236L;

    public KuraBluetoothPairException(Object argument) {
        super(KuraErrorCode.BLE_PAIR_ERROR, null, argument);
    }

    public KuraBluetoothPairException(Throwable cause, Object argument) {
        super(KuraErrorCode.BLE_PAIR_ERROR, cause, argument);
    }
}
