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
 *******************************************************************************/
package org.eclipse.kura.core.keystore.util;

public enum EntryType {

    TRUSTED_CERTIFICATE("TrustedCertificate"),
    PRIVATE_KEY("PrivateKey"),
    KEY_PAIR("KeyPair"),
    CSR("CSR");

    String type;

    private EntryType(String type) {
        this.type = type;
    }

    public static EntryType valueOfType(String type) {
        for (EntryType e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
