/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.session;

public enum Attributes {

    AUTORIZED_USER("org.eclipse.kura.web.user"),
    LAST_ACTIVITY("org.eclipse.kura.web.lastActivity");

    private String value;

    private Attributes(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
