/**
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal
 */
package org.eclipse.kura.internal.driver.opcua;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * The Class {@link KeyStoreLoader} loads the provided keystore
 */
public final class KeyStoreLoader {

    private final String certificate;

    private final String clientAlias;

    private X509Certificate clientCertificate;

    private KeyPair clientKeyPair;

    private final String keystoreType;

    private final char[] password;

    @SuppressWarnings("unused")
    private final String serverAlias;

    /**
     * Instantiates a new key store loader.
     *
     * @param keystoreType
     *            the keystore type
     * @param clientAlias
     *            the client alias
     * @param serverAlias
     *            the server alias
     * @param password
     *            the password
     * @param certificate
     *            the certificate
     */
    public KeyStoreLoader(final String keystoreType, final String clientAlias, final String serverAlias,
            final String password, final String certificate) {
        this.keystoreType = keystoreType;
        this.clientAlias = clientAlias;
        this.serverAlias = serverAlias;
        this.password = password.toCharArray();
        this.certificate = certificate;
    }

    /**
     * Gets the client certificate.
     *
     * @return the client certificate
     */
    X509Certificate getClientCertificate() {
        return this.clientCertificate;
    }

    /**
     * Gets the client key pair.
     *
     * @return the client key pair
     */
    KeyPair getClientKeyPair() {
        return this.clientKeyPair;
    }

    /**
     * Loads the certificate.
     *
     * @return the keystore instance
     * @throws Exception
     *             if the load is unsuccessful
     */
    KeyStoreLoader load() throws Exception {
        final KeyStore keyStore = KeyStore.getInstance(this.keystoreType);
        keyStore.load(Files.newInputStream(Paths.get(this.certificate)), this.password);
        final Key clientPrivateKey = keyStore.getKey(this.clientAlias, this.password);

        if (clientPrivateKey instanceof PrivateKey) {
            this.clientCertificate = (X509Certificate) keyStore.getCertificate(this.clientAlias);
            final PublicKey clientPublicKey = this.clientCertificate.getPublicKey();
            this.clientKeyPair = new KeyPair(clientPublicKey, (PrivateKey) clientPrivateKey);
        }

        return this;
    }
}