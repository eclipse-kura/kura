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

public class PrivateKeyInfo extends EntryInfo {

    private String algorithm;
    private int size;
    private String privateKey;
    private String[] certificateChain;

    public PrivateKeyInfo(String alias, String keystoreName) {
        super(alias, keystoreName);
        setType(EntryType.PRIVATE_KEY);
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getPrivateKey() {
        return this.privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String[] getCertificateChain() {
        return this.certificateChain;
    }

    public void setCertificateChain(String[] certificateChain) {
        this.certificateChain = certificateChain;
    }
}
