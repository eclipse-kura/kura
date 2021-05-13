/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
 * KuraProcessExecutionErrorException is raised when a command/process execution fails.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.2
 */
@ProviderType
public class KuraProcessExecutionErrorException extends KuraException {

    private static final long serialVersionUID = 5091705219678526443L;

    public KuraProcessExecutionErrorException(Object argument) {
        super(KuraErrorCode.PROCESS_EXECUTION_ERROR, null, argument);
    }

    public KuraProcessExecutionErrorException(Throwable cause, Object argument) {
        super(KuraErrorCode.PROCESS_EXECUTION_ERROR, cause, argument);
    }
}
