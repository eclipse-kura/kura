/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates
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
 * KuraBluetoothNotificationException is raised when an error is detected during notification.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 1.3
 */
@ProviderType
public class KuraBluetoothNotificationException extends KuraException {

    private static final long serialVersionUID = -4188172396128459284L;

    public KuraBluetoothNotificationException(Object argument) {
        super(KuraErrorCode.BLE_NOTIFICATION_ERROR, null, argument);
    }

    public KuraBluetoothNotificationException(Throwable cause, Object argument) {
        super(KuraErrorCode.BLE_NOTIFICATION_ERROR, cause, argument);
    }
}
