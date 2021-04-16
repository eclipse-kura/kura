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
 ******************************************************************************/
package org.eclipse.kura.certificate;

import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 *
 * The KuraPrivateKeyEntry represents a {@java.security.KeyStore.PrivateKeyEntry} stored in a keystore
 * along with the its certificate chain.
 * The private key entry is identified by an id made with the id of the keystore and the alias.
 *
 * @since 2.2
 */
public class KuraPrivateKeyEntry {

    private final String privateKeyId;
    private final String keystoreId;
    private final String alias;
    private final PrivateKeyEntry privateKeyEntry;

    public KuraPrivateKeyEntry(String keystoreId, String alias, PrivateKeyEntry privateKeyEntry) {
        super();
        this.keystoreId = keystoreId;
        this.alias = alias;
        this.privateKeyEntry = privateKeyEntry;
        this.privateKeyId = keystoreId + ":" + alias;
    }

    public KuraPrivateKeyEntry(String keystoreId, String alias, PrivateKey privateKey, Certificate[] certificateChain) {
        super();
        this.keystoreId = keystoreId;
        this.alias = alias;
        this.privateKeyEntry = new PrivateKeyEntry(privateKey, certificateChain);
        this.privateKeyId = keystoreId + ":" + alias;
    }

    public String getPrivateKeyId() {
        return this.privateKeyId;
    }

    public String getKeystoreId() {
        return this.keystoreId;
    }

    public String getAlias() {
        return this.alias;
    }

    public PrivateKeyEntry getPrivateKey() {
        return this.privateKeyEntry;
    }

}
