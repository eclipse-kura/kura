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
package org.eclipse.kura.nm;

public enum Kura8021xEAP {

    KURA_8021X_EAP_TLS("Kura8021xEapTls"),
    KURA_8021X_EAP_PEAP("Kura8021xEapPeap"),
    KURA_8021X_EAP_TTLS("Kura8021xEapTtls");

    private final String value;

    private Kura8021xEAP(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static Kura8021xEAP fromString(String name) {
        for (Kura8021xEAP eap : Kura8021xEAP.values()) {
            if (eap.getValue().equals(name)) {
                return eap;
            }
        }

        throw new IllegalArgumentException("Invalid EAP type in snapshot: " + name);
    }
}
