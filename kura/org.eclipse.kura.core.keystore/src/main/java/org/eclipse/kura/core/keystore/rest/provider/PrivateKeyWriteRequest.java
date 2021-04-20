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
package org.eclipse.kura.core.keystore.rest.provider;

import org.eclipse.kura.rest.utils.Validable;

public class PrivateKeyWriteRequest implements Validable {

    private String keystoreServicePid;
    private String alias;
    private String privateKey;
    private String[] certificateChain;
    private String algorithm;
    private String signatureAlgorithm;
    private String attributes;
    private int size;

    public String getKeystoreServicePid() {
        return this.keystoreServicePid;
    }

    public String getAlias() {
        return this.alias;
    }

    public String getPrivateKey() {
        return this.privateKey;
    }

    public String[] getCertificateChain() {
        return this.certificateChain;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    public String getSignatureAlgorithm() {
        return this.signatureAlgorithm;
    }

    public int getSize() {
        return this.size;
    }

    public String getAttributes() {
        return this.attributes;
    }

    @Override
    public String toString() {
        return "WriteRequest [keystoreServicePid=" + this.keystoreServicePid + ", alias=" + this.alias + "]";
    }

    @Override
    public boolean isValid() {
        if (this.keystoreServicePid == null || this.alias == null) {
            return false;
        }
        return !(this.algorithm == null || this.size == 0 || this.signatureAlgorithm == null
                || this.attributes == null);
    }

}
