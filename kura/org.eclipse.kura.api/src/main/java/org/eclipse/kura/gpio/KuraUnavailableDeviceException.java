/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.gpio;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraUnavailableDeviceException extends KuraException {

    private static final long serialVersionUID = -5115093706356681148L;

    public KuraUnavailableDeviceException(Object argument) {
        super(KuraErrorCode.UNAVAILABLE_DEVICE, null, argument);
    }

    public KuraUnavailableDeviceException(Throwable cause, Object argument) {
        super(KuraErrorCode.UNAVAILABLE_DEVICE, cause, argument);
    }
}
