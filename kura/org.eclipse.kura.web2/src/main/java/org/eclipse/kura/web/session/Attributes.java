/*******************************************************************************
 * Copyright (c) 2019, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.session;

public enum Attributes {

    AUTORIZED_USER("org.eclipse.kura.web.user"),
    LAST_ACTIVITY("org.eclipse.kura.web.lastActivity"),
    CREDENTIALS_HASH("org.eclipse.kura.web.credentialsHash"),
    AUDIT_CONTEXT("org.eclipse.kura.audit.context"),
    LOCKED("org.eclipse.kura.locked");

    private String value;

    private Attributes(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
