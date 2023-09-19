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
package org.eclipse.kura.web.shared.model;

public enum Gwt8021xInnerAuth {

    NONE("Kura8021xInnerAuthNone"),
    MSCHAPV2("Kura8021xInnerAuthMschapv2");

    private final String label;

    Gwt8021xInnerAuth(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Gwt8021xInnerAuth fromMetatypeString(String label) {
        for (Gwt8021xInnerAuth innerAuth : Gwt8021xInnerAuth.values()) {
            if (innerAuth.getLabel().equals(label)) {
                return innerAuth;
            }
        }
        return Gwt8021xInnerAuth.NONE;
    }
}
