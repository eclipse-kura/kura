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
package org.eclipse.kura.position;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class PositionException extends Exception {

    private static final long serialVersionUID = 2611760893640245224L;

    public PositionException() {
        // TODO Auto-generated constructor stub
    }

    public PositionException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public PositionException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public PositionException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

}
