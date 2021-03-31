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
package org.eclipse.kura.core.keystore;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 *
 */
public class KeystoreServiceImpl implements KeystoreService, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(KeystoreServiceImpl.class);

    public static final String APP_ID = "KeystoreService";

    private CryptoService cryptoService;
    private KeystoreServiceOptions keystoreServiceOptions;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(Map<String, Object> properties) {
        logger.info("Bundle {} has started!", APP_ID);

        this.keystoreServiceOptions = new KeystoreServiceOptions(properties, cryptoService);

    }

    public void updated(Map<String, Object> properties) {
        logger.info("Bundle {} is deactivating!", APP_ID);
        this.keystoreServiceOptions = new KeystoreServiceOptions(properties, cryptoService);
    }

    protected void deactivate() {
        logger.info("Bundle {} is deactivating!", APP_ID);
    }

    @Override
    public KeyStore getKeyStore() throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        try (InputStream tsReadStream = new FileInputStream(this.keystoreServiceOptions.getKeystorePath().toFile());) {
            ks.load(tsReadStream, this.keystoreServiceOptions.getKeystorePassword());
        }

        return ks;
    }

    @Override
    public KeyPair getKeyPair(String id) throws GeneralSecurityException, IOException {
        KeyStore ks = getKeyStore();
        final Key key = ks.getKey(id, this.keystoreServiceOptions.getKeystorePassword());

        final Certificate cert = ks.getCertificate(id);
        final PublicKey publicKey = cert.getPublicKey();

        return new KeyPair(publicKey, (PrivateKey) key);
    }

    @Override
    public List<KeyPair> getKeyPairs() throws GeneralSecurityException, IOException {
        List<KeyPair> result = new ArrayList<>();

        KeyStore ks = getKeyStore();
        List<String> aliases = Collections.list(ks.aliases());

        for (String alias : aliases) {
            KeyPair tempKeyPair = getKeyPair(alias);
            result.add(tempKeyPair);
        }
        return result;
    }

    @Override
    public KeyPair createKeyPair(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCSR(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<KeyManager> getKeyManagers(String algorithm) throws GeneralSecurityException, IOException {
        KeyStore ks = getKeyStore();
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        kmf.init(ks, this.keystoreServiceOptions.getKeystorePassword());

        return Arrays.asList(kmf.getKeyManagers());
    }

    @Override
    public void deleteEntry(String id) throws GeneralSecurityException, IOException {
        KeyStore ks = getKeyStore();
        ks.deleteEntry(id);
        saveKeystore(ks);
    }

    private void saveKeystore(KeyStore ks)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        try (FileOutputStream tsOutStream = new FileOutputStream(
                this.keystoreServiceOptions.getKeystorePath().toFile());) {
            ks.store(tsOutStream, this.keystoreServiceOptions.getKeystorePassword());
        }
    }
}
