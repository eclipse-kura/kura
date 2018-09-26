/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.request;

import org.eclipse.kura.cloudconnection.message.KuraMessage;

/**
 * This class is an enumeration that wraps some of the possible property keys set in the properties of a
 * {@link KuraMessage} used for request/response.
 *
 * @since 2.0
 */
public enum RequestHandlerMessageConstants {

    /**
     * Request arguments. The corresponding value must be a {@code List<String>}.
     */
    ARGS_KEY("args");

    private String value;

    RequestHandlerMessageConstants(final String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
