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

public enum Gwt8021xEap {

    TLS("Kura8021xEapTls"),
    TTLS("Kura8021xEapTtls"),
    PEAP("Kura8021xEapPeap");

    private final String label;

    Gwt8021xEap(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Gwt8021xEap fromMetatypeString(String label) {
        for (Gwt8021xEap eap : Gwt8021xEap.values()) {
            if (eap.getLabel().equals(label)) {
                return eap;
            }
        }
        return Gwt8021xEap.TTLS;
    }

}
