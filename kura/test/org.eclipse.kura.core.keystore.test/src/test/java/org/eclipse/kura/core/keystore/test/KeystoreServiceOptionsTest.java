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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.keystore.KeystoreServiceOptions;
import org.eclipse.kura.crypto.CryptoService;
import org.junit.Test;

public class KeystoreServiceOptionsTest {

    private static final String CHANGEIT_PASSWORD = "changeit";

    @Test(expected = IllegalArgumentException.class)
    public void testNullPropertiesCrypto() {
        new KeystoreServiceOptions(null, null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNullCrypto() {
        Map<String, Object> properties = new HashMap<>();
        new KeystoreServiceOptions(properties, null);
    }

    @Test
    public void testConstructorMissingProps() throws KuraException {
        Map<String, Object> properties = new HashMap<>();
        
        CryptoService cryptoService = mock(CryptoService.class);
        when(cryptoService.encryptAes(CHANGEIT_PASSWORD.toCharArray())).thenReturn("encrypted".toCharArray());
        when(cryptoService.decryptAes("encrypted".toCharArray())).thenReturn(CHANGEIT_PASSWORD.toCharArray());

        KeystoreServiceOptions keystoreServiceOptions = new KeystoreServiceOptions(properties, cryptoService);

        assertEquals("/tmp", keystoreServiceOptions.getKeystorePath());
        assertArrayEquals(CHANGEIT_PASSWORD.toCharArray(), keystoreServiceOptions.getKeystorePassword(cryptoService));
    }

    @Test
    public void testConstructor() throws KuraException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("keystore.path", "/abc");
        properties.put("keystore.password", "testPassword");
        
        CryptoService cryptoService = mock(CryptoService.class);
        when(cryptoService.encryptAes("testPassword".toCharArray())).thenReturn("encrypted".toCharArray());
        when(cryptoService.decryptAes("encrypted".toCharArray())).thenReturn("testPassword".toCharArray());
        when(cryptoService.decryptAes("testPassword".toCharArray())).thenReturn("testPassword".toCharArray());

        KeystoreServiceOptions keystoreServiceOptions = new KeystoreServiceOptions(properties, cryptoService);

        assertEquals("/abc", keystoreServiceOptions.getKeystorePath());
        assertArrayEquals("testPassword".toCharArray(), keystoreServiceOptions.getKeystorePassword(cryptoService));
    }

    @Test
    public void testCompareSame() throws KuraException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("keystore.path", "/abc");
        properties.put("keystore.password", "testPassword");
        
        CryptoService cryptoService = mock(CryptoService.class);
        when(cryptoService.encryptAes("testPassword".toCharArray())).thenReturn("encrypted".toCharArray());
        when(cryptoService.decryptAes("encrypted".toCharArray())).thenReturn("testPassword".toCharArray());

        KeystoreServiceOptions keystoreServiceOptions1 = new KeystoreServiceOptions(properties, cryptoService);

        KeystoreServiceOptions keystoreServiceOptions2 = new KeystoreServiceOptions(properties, cryptoService);

        assertEquals(keystoreServiceOptions1, keystoreServiceOptions2);
    }

    @Test
    public void testCompareDifferent() throws KuraException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("keystore.path", "/abc");
        properties.put("keystore.password", "testPassword");
        
        CryptoService cryptoService = mock(CryptoService.class);
        when(cryptoService.encryptAes("testPassword".toCharArray())).thenReturn("encrypted".toCharArray());
        when(cryptoService.encryptAes("testPassword1".toCharArray())).thenReturn("encrypted1".toCharArray());
        when(cryptoService.decryptAes("encrypted".toCharArray())).thenReturn("testPassword".toCharArray());
        when(cryptoService.decryptAes("encrypted1".toCharArray())).thenReturn("testPassword1".toCharArray());

        KeystoreServiceOptions keystoreServiceOptions1 = new KeystoreServiceOptions(properties, cryptoService);

        Map<String, Object> properties2 = new HashMap<>();
        properties2.put("keystore.path", "/abc1");
        properties2.put("keystore.password", "testPassword1");
        KeystoreServiceOptions keystoreServiceOptions2 = new KeystoreServiceOptions(properties2, cryptoService);

        assertNotEquals(keystoreServiceOptions1, keystoreServiceOptions2);

    }

}
