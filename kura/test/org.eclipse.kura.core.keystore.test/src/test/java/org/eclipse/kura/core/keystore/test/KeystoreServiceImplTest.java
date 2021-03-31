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
package org.eclipse.kura.core.keystore.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.core.keystore.KeystoreServiceImpl;
import org.eclipse.kura.crypto.CryptoService;
import org.junit.Before;
import org.junit.Test;

public class KeystoreServiceImplTest {

    private static final String CERT_FILE_PATH = "target/test-classes/cert";
    private static final String KEY_KEYSTORE_PATH = "keystore.path";
    private static final String KEY_KEYSTORE_PASSWORD = "keystore.password";

    private static final String STORE_PATH = "target/key.store";
    private static final String STORE_PASS = "pass";

    private KeyStore store;

    @Before
    public void setupDefaultKeystore()
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        // create a new keystore each time the tests should run

        this.store = KeyStore.getInstance("jks");

        this.store.load(null, null);

        KeyPairGenerator gen = KeyPairGenerator.getInstance("DSA");
        gen.initialize(1024);
        KeyPair pair = gen.generateKeyPair();
        Key key = pair.getPrivate();

        InputStream is = new FileInputStream(CERT_FILE_PATH);
        Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(is);
        is.close();

        Certificate[] chain = { certificate };
        
        store.setKeyEntry("alias", key, STORE_PASS.toCharArray(), chain);
        
        try (OutputStream os = new FileOutputStream(STORE_PATH)) {
            this.store.store(os, STORE_PASS.toCharArray());
        }
    }

    @Test
    public void testGetKeyStore() throws GeneralSecurityException, IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(KEY_KEYSTORE_PATH, STORE_PATH);
        properties.put(KEY_KEYSTORE_PASSWORD, STORE_PASS);

        CryptoService cryptoService = mock(CryptoService.class);

        KeystoreServiceImpl keystoreService = new KeystoreServiceImpl();
        keystoreService.setCryptoService(cryptoService);
        keystoreService.activate(properties);

        KeyStore keystore = keystoreService.getKeyStore();

        assertNotNull(keystore);
        assertEquals(Collections.list(this.store.aliases()), Collections.list(keystore.aliases()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetEntryNullAlias() throws GeneralSecurityException, IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(KEY_KEYSTORE_PATH, STORE_PATH);
        properties.put(KEY_KEYSTORE_PASSWORD, STORE_PASS);

        CryptoService cryptoService = mock(CryptoService.class);

        KeystoreServiceImpl keystoreService = new KeystoreServiceImpl();
        keystoreService.setCryptoService(cryptoService);
        keystoreService.activate(properties);

        keystoreService.getEntry(null);
    }
    
    @Test
    public void testGetEntryEmptyAlias() throws GeneralSecurityException, IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(KEY_KEYSTORE_PATH, STORE_PATH);
        properties.put(KEY_KEYSTORE_PASSWORD, STORE_PASS);

        CryptoService cryptoService = mock(CryptoService.class);

        KeystoreServiceImpl keystoreService = new KeystoreServiceImpl();
        keystoreService.setCryptoService(cryptoService);
        keystoreService.activate(properties);

        Entry entry = keystoreService.getEntry("");
        assertNull(entry);
    }

}
