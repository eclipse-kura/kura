/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.gpio;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraGPIODeviceException extends KuraException {

    private static final long serialVersionUID = -1750311704822256084L;

    public KuraGPIODeviceException(Object argument) {
        super(KuraErrorCode.GPIO_EXCEPTION, null, argument);
    }

    public KuraGPIODeviceException(Throwable cause, Object argument) {
        super(KuraErrorCode.GPIO_EXCEPTION, cause, argument);
    }
}
