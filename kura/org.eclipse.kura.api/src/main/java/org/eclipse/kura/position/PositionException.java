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
