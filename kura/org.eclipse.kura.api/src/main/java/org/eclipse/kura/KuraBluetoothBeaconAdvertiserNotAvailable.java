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
 * KuraBluetoothBeaconAdvertiserNotAvailable is raised when the advertiser is not available.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 1.3
 */
@ProviderType
public class KuraBluetoothBeaconAdvertiserNotAvailable extends KuraException {

    private static final long serialVersionUID = -1243607248475874911L;

    public KuraBluetoothBeaconAdvertiserNotAvailable(Object argument) {
        super(KuraErrorCode.BLE_RESOURCE_NOT_FOUND, null, argument);
    }

    public KuraBluetoothBeaconAdvertiserNotAvailable(Throwable cause, Object argument) {
        super(KuraErrorCode.BLE_RESOURCE_NOT_FOUND, cause, argument);
    }
}
