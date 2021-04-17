/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.core.keystore.util;

public class EntryInfo {

    private final String id;
    private final String keystoreName;
    private final String alias;
    private EntryType type;

    public EntryInfo(String alias, String keystoreName) {
        this.alias = alias;
        this.keystoreName = keystoreName;
        this.id = keystoreName + ":" + alias;
    }

    public String getId() {
        return this.id;
    }

    public String getKeystoreName() {
        return this.keystoreName;
    }

    public String getAlias() {
        return this.alias;
    }

    public EntryType getType() {
        return this.type;
    }

    public void setType(EntryType type) {
        this.type = type;
    }
}
