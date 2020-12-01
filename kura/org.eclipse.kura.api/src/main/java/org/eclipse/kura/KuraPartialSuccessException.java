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

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * KuraPartialSuccessException is used capture the response
 * of bulk operations which allow for the failures of some
 * of their steps.
 * KuraPartialSuccessException.getCauses() will return the
 * exceptions collected during operations for those steps
 * that failed.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraPartialSuccessException extends KuraException {

    private static final long serialVersionUID = -350563041335590477L;

    private final List<Throwable> causes;

    public KuraPartialSuccessException(String message, List<Throwable> causes) {
        super(KuraErrorCode.PARTIAL_SUCCESS, (Throwable) null, message);
        this.causes = causes;
    }

    /**
     * Returns the list of failures collected during the execution of the bulk operation.
     *
     * @return causes
     */
    public List<Throwable> getCauses() {
        return this.causes;
    }
}
