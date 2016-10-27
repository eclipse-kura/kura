/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
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