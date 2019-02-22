/**
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *  
 */

package org.eclipse.kura.internal.driver.opcua.auth;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class ExternalKeystoreCertificateManager extends CertificateManager {

    private final String keyStorePath;
    private final String keyStoreType;
    private final char[] keyStorePassword;
    private final String clientAlias;

    private volatile boolean loaded;

    public ExternalKeystoreCertificateManager(final String keyStorePath, final String keyStoreType,
            final String keyStorePassword, final String clientAlias, final boolean authenticateServer) {
        super(authenticateServer);
        this.keyStorePath = keyStorePath;
        this.keyStoreType = keyStoreType;
        this.keyStorePassword = keyStorePassword.toCharArray();
        this.clientAlias = clientAlias;
    }

    @Override
    public synchronized void load() throws Exception {

        if (loaded) {
            return;
        }

        final KeyStore keyStore = KeyStore.getInstance(this.keyStoreType);
        keyStore.load(Files.newInputStream(Paths.get(this.keyStorePath)), this.keyStorePassword);

        X509Certificate clientCertificate = null;
        KeyPair clientKeyPair = null;

        final Key clientPrivateKey = keyStore.getKey(this.clientAlias, this.keyStorePassword);

        if (clientPrivateKey instanceof PrivateKey) {
            clientCertificate = (X509Certificate) keyStore.getCertificate(this.clientAlias);
            final PublicKey clientPublicKey = clientCertificate.getPublicKey();
            clientKeyPair = new KeyPair(clientPublicKey, (PrivateKey) clientPrivateKey);
        }

        super.load(getTrustedCertificates(keyStore), clientKeyPair, clientCertificate);

        loaded = true;
    }

    private static X509Certificate[] getTrustedCertificates(final KeyStore keyStore)
            throws KeyStoreException, NoSuchAlgorithmException {

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        for (TrustManager tm : trustManagers) {
            if (tm instanceof X509TrustManager) {
                X509TrustManager x509tm = (X509TrustManager) tm;
                return x509tm.getAcceptedIssuers();
            }
        }

        return new X509Certificate[0];
    }

}
