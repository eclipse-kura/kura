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
 *******************************************************************************/
package org.eclipse.kura;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraInvalidMetricTypeException extends KuraRuntimeException {

    private static final long serialVersionUID = 3811194468467381264L;

    public KuraInvalidMetricTypeException(Object argument) {
        super(KuraErrorCode.INVALID_METRIC_EXCEPTION, argument);
    }

    public KuraInvalidMetricTypeException(Throwable cause, Object argument) {
        super(KuraErrorCode.INVALID_METRIC_EXCEPTION, cause, argument);
    }
}
