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

import static java.util.Objects.isNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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

    public void activate(Map<String, Object> properties) {
        logger.info("Bundle {} has started!", APP_ID);

        this.keystoreServiceOptions = new KeystoreServiceOptions(properties, this.cryptoService);

    }

    public void updated(Map<String, Object> properties) {
        logger.info("Bundle {} is deactivating!", APP_ID);
        this.keystoreServiceOptions = new KeystoreServiceOptions(properties, this.cryptoService);
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
    public Entry getEntry(String alias) throws GeneralSecurityException, IOException {
        if (isNull(alias)) {
            throw new IllegalArgumentException("Key Pair alias cannot be null!");
        }
        KeyStore ks = getKeyStore();

        return ks.getEntry(alias, new PasswordProtection(this.keystoreServiceOptions.getKeystorePassword()));
    }

    @Override
    public Map<String,Entry> getEntries() throws GeneralSecurityException, IOException {
        Map<String, Entry> result = new HashMap<>();

        KeyStore ks = getKeyStore();
        List<String> aliases = Collections.list(ks.aliases());

        for (String alias : aliases) {
            Entry tempEntry = getEntry(alias);
            result.put(alias, tempEntry);
        }
        return result;
    }

    @Override
    public KeyPair createKeyPair(String alias) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCSR(String alias) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<KeyManager> getKeyManagers(String algorithm) throws GeneralSecurityException, IOException {
        if (isNull(algorithm)) {
            throw new IllegalArgumentException("Algorithm cannot be null!");
        }
        KeyStore ks = getKeyStore();
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        kmf.init(ks, this.keystoreServiceOptions.getKeystorePassword());

        return Arrays.asList(kmf.getKeyManagers());
    }

    @Override
    public void deleteEntry(String alias) throws GeneralSecurityException, IOException {
        if (isNull(alias)) {
            throw new IllegalArgumentException("Alias cannot be null!");
        }
        KeyStore ks = getKeyStore();
        ks.deleteEntry(alias);
        saveKeystore(ks);
    }

    private void saveKeystore(KeyStore ks)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        try (FileOutputStream tsOutStream = new FileOutputStream(
                this.keystoreServiceOptions.getKeystorePath().toFile());) {
            ks.store(tsOutStream, this.keystoreServiceOptions.getKeystorePassword());
        }
    }

    @Override
    public void setEntry(String alias, Entry entry) throws GeneralSecurityException, IOException {
        if (isNull(alias) || alias.trim().isEmpty() || isNull(entry)) {
            throw new IllegalArgumentException("Input cannot be null or empty!");
        }
        KeyStore ks = getKeyStore();
        ks.setEntry(alias, entry, new PasswordProtection(this.keystoreServiceOptions.getKeystorePassword()));
        saveKeystore(ks);
    }

    @Override
    public List<String> getAliases() throws GeneralSecurityException, IOException {
        KeyStore ks = getKeyStore();
        return Collections.list(ks.aliases());
    }

}
