/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.wires;

/**
 * The ViewData used by {@link ValidationInputCell}.
 */
public final class ValidationData {

    private boolean invalid;
    private String value;

    public String getValue() {
        return this.value;
    }

    public boolean isInvalid() {
        return this.invalid;
    }

    public void setInvalid(final boolean invalid) {
        this.invalid = invalid;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}