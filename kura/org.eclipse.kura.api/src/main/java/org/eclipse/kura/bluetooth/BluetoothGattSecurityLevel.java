/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.bluetooth;

/**
 * Security levels.
 *
 * @since 1.2
 *
 * @deprecated
 *
 */
@Deprecated
public enum BluetoothGattSecurityLevel {

    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN;

    public static BluetoothGattSecurityLevel getBluetoothGattSecurityLevel(String level) {
        if (LOW.name().equalsIgnoreCase(level)) {
            return LOW;
        } else if (MEDIUM.name().equalsIgnoreCase(level)) {
            return MEDIUM;
        } else if (HIGH.name().equalsIgnoreCase(level)) {
            return HIGH;
        } else {
            return UNKNOWN;
        }

    }

}
