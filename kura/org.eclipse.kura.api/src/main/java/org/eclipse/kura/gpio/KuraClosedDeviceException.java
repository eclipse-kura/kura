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
public class KuraClosedDeviceException extends KuraException {

    /**
     *
     */
    private static final long serialVersionUID = -1750311704822256084L;

    public KuraClosedDeviceException(Object argument) {
        super(KuraErrorCode.CLOSED_DEVICE, null, argument);
    }

    public KuraClosedDeviceException(Throwable cause, Object argument) {
        super(KuraErrorCode.CLOSED_DEVICE, cause, argument);
    }
}
