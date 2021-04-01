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

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Optional;

/**
 *
 * The KuraPrivateKey represents a {@java.security.PrivateKey} stored in a keystore
 * along with the its certificate chain.
 * The private key is identified by an id made with the id of the keystore and the alias.
 *
 * @since 2.2
 */
public class KuraPrivateKey {

    private final String privateKeyId;
    private final String keystoreId;
    private final String alias;
    private final Optional<PrivateKey> privateKey;
    private final Optional<Certificate[]> certificateChain;

    public KuraPrivateKey(String keystoreId, String alias, PrivateKey privateKey, Certificate[] certificateChain) {
        super();
        this.keystoreId = keystoreId;
        this.alias = alias;
        this.privateKey = Optional.of(privateKey);
        this.certificateChain = Optional.of(certificateChain);
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

    public Optional<PrivateKey> getPrivateKey() {
        return this.privateKey;
    }

    public Optional<Certificate[]> getCertificateChain() {
        return this.certificateChain;
    }

}
