/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.request;

/**
 * This class is an enumeration that wraps some of the possible property keys set in the properties of a
 * {@link org.eclipse.kura.cloudconnection.message.KuraMessage} used for request/response.
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
