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
package org.eclipse.kura.bluetooth.le;

import org.osgi.annotation.versioning.ProviderType;

/**
 * BluetoothLeGattCharacteristicProperties contains the GATT characteristic property values
 *
 * @since 1.3
 */
@ProviderType
public enum BluetoothLeGattCharacteristicProperties {

    BROADCAST(0x01),
    READ(0x02),
    WRITE_WITHOUT_RESPONSE(0x04),
    WRITE(0x08),
    NOTIFY(0x10),
    INDICATE(0x20),
    AUTHENTICATE_SIGNED_WRITES(0x40),
    EXTENDED_PROPERTIES(0x80),
    UNKNOWN(0x00);

    private final int code;

    private BluetoothLeGattCharacteristicProperties(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

}
