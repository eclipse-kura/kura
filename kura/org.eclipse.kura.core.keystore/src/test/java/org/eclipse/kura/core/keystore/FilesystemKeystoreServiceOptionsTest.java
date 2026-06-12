/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.crypto.CryptoService;
import org.junit.Test;

public class FilesystemKeystoreServiceOptionsTest {

    private static final String CHANGEIT_PASSWORD = "changeit";

    @Test(expected = IllegalArgumentException.class)
    public void testNullPropertiesCrypto() {
        new FilesystemKeystoreServiceOptions(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullCrypto() {
        Map<String, Object> properties = new HashMap<>();
        new FilesystemKeystoreServiceOptions(properties, null);
    }

    @Test
    public void testConstructorMissingProps() throws KuraException {
        Map<String, Object> properties = new HashMap<>();

        CryptoService cryptoService = mock(CryptoService.class);
        when(cryptoService.encryptAes(CHANGEIT_PASSWORD.toCharArray())).thenReturn("encrypted".toCharArray());
        when(cryptoService.decryptAes("encrypted".toCharArray())).thenReturn(CHANGEIT_PASSWORD.toCharArray());
        when(cryptoService.getKeyStorePassword(any(String.class))).thenReturn(CHANGEIT_PASSWORD.toCharArray());

        FilesystemKeystoreServiceOptions FilesystemKeystoreServiceOptions = new FilesystemKeystoreServiceOptions(
                properties, cryptoService);

        assertEquals("/tmp/keystore.ks", FilesystemKeystoreServiceOptions.getKeystorePath());
        assertArrayEquals(CHANGEIT_PASSWORD.toCharArray(),
                FilesystemKeystoreServiceOptions.getKeystorePassword(cryptoService));
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
        when(cryptoService.getKeyStorePassword(any(String.class))).thenReturn("testPassword".toCharArray());

        FilesystemKeystoreServiceOptions FilesystemKeystoreServiceOptions = new FilesystemKeystoreServiceOptions(
                properties, cryptoService);

        assertEquals("/abc", FilesystemKeystoreServiceOptions.getKeystorePath());
        assertArrayEquals("testPassword".toCharArray(),
                FilesystemKeystoreServiceOptions.getKeystorePassword(cryptoService));
    }

    @Test
    public void testCompareSame() throws KuraException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("keystore.path", "/abc");
        properties.put("keystore.password", "testPassword");

        CryptoService cryptoService = mock(CryptoService.class);
        when(cryptoService.encryptAes("testPassword".toCharArray())).thenReturn("encrypted".toCharArray());
        when(cryptoService.decryptAes("encrypted".toCharArray())).thenReturn("testPassword".toCharArray());

        FilesystemKeystoreServiceOptions FilesystemKeystoreServiceOptions1 = new FilesystemKeystoreServiceOptions(
                properties, cryptoService);

        FilesystemKeystoreServiceOptions FilesystemKeystoreServiceOptions2 = new FilesystemKeystoreServiceOptions(
                properties, cryptoService);

        assertEquals(FilesystemKeystoreServiceOptions1, FilesystemKeystoreServiceOptions2);
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

        FilesystemKeystoreServiceOptions FilesystemKeystoreServiceOptions1 = new FilesystemKeystoreServiceOptions(
                properties, cryptoService);

        Map<String, Object> properties2 = new HashMap<>();
        properties2.put("keystore.path", "/abc1");
        properties2.put("keystore.password", "testPassword1");
        FilesystemKeystoreServiceOptions FilesystemKeystoreServiceOptions2 = new FilesystemKeystoreServiceOptions(
                properties2, cryptoService);

        assertNotEquals(FilesystemKeystoreServiceOptions1, FilesystemKeystoreServiceOptions2);

    }

}
