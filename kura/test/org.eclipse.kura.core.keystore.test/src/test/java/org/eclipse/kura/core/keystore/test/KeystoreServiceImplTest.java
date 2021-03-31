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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.core.keystore.KeystoreServiceImpl;
import org.eclipse.kura.crypto.CryptoService;
import org.junit.Before;
import org.junit.Test;


public class KeystoreServiceImplTest {
    
    private static final String KEY_KEYSTORE_PATH = "keystore.path";
    private static final String KEY_KEYSTORE_PASSWORD = "keystore.password";

    private static final String STORE_PATH = "target/key.store";
    private static final String STORE_PASS = "pass";
    
    private KeyStore store;

    @Before
    public void setupDefaultKeystore()
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        // create a new keystore each time the tests should run

        store = KeyStore.getInstance("jks");

        store.load(null, null);

        try (OutputStream os = new FileOutputStream(STORE_PATH)) {
            store.store(os, STORE_PASS.toCharArray());
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
        assertEquals(store.aliases(), keystore.aliases());
    }

}
