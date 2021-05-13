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
package org.eclipse.kura.security.keystore;

/**
 * Identifies a keystore with its name.
 * Further keystore information can be added (type, size...).
 *
 * @since 2.2
 */
public class KeystoreInfo {

    private final String keystoreServicePid;
    private String type;
    private int size;

    public KeystoreInfo(String keystoreServicePid) {
        this.keystoreServicePid = keystoreServicePid;
    }

    public String getKeystoreServicePid() {
        return this.keystoreServicePid;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }

}
