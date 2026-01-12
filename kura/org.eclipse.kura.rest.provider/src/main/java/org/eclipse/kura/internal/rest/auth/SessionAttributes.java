/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.auth;

public enum SessionAttributes {

    AUTORIZED_USER("org.eclipse.kura.user"),
    LAST_ACTIVITY("org.eclipse.kura.lastActivity"),
    CREDENTIALS_HASH("org.eclipse.kura.credentialsHash"),
    AUDIT_CONTEXT("org.eclipse.kura.audit.context"),
    LOCKED("org.eclipse.kura.locked"),
    XSRF_TOKEN("org.eclipse.kura.xsrf.token");

    private String value;

    private SessionAttributes(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
