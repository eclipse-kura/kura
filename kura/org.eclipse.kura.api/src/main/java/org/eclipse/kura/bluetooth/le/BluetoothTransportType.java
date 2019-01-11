/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.bluetooth.le;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Defines the type of transport for Bluetooth devices.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
@ProviderType
public enum BluetoothTransportType {

    AUTO,
    BREDR,
    LE

}
