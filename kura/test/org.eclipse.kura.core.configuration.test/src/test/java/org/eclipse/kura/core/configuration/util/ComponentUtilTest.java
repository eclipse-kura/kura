/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.configuration.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.crypto.CryptoService;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

public class ComponentUtilTest {

    @Test
    public void testEncryptConfigsNull() throws NoSuchMethodException { // move
        // test with null parameter
        try {
            ComponentUtil.encryptConfigs(null, null);
        } catch (Throwable e) {
            fail("Parameters not checked.");
        }

    }

    @Test
    public void testEncryptConfigsNoConfigs() { // move
        // empty list
        boolean exceptionCaught = false;

        try {
            ComponentUtil.encryptConfigs(new ArrayList<>(), null);
        } catch (Throwable t) {
            exceptionCaught = true;
        }
        assertFalse(exceptionCaught);
    }

    @Test
    public void testEncryptConfigsEncryptionException() throws Throwable { // move
        // test failed encryption of a password: add a password and run; fail

        CryptoService cryptoServiceMock = mock(CryptoService.class);

        // first decryption must fail
        when(cryptoServiceMock.decryptAes("pass".toCharArray()))
                .thenThrow(new KuraException(KuraErrorCode.DECODER_ERROR, "password"));
        // then also encryption can fail
        when(cryptoServiceMock.encryptAes("pass".toCharArray()))
                .thenThrow(new KuraException(KuraErrorCode.ENCODE_ERROR, "password"));

        List<ComponentConfiguration> configs = new ArrayList<>();

        ComponentConfigurationImpl cfg = new ComponentConfigurationImpl();
        Map<String, Object> props = new HashMap<>();
        props.put("key1", new Password("pass"));
        cfg.setProperties(props);

        configs.add(cfg);

        ComponentUtil.encryptConfigs(configs, cryptoServiceMock);

        verify(cryptoServiceMock, times(1)).decryptAes("pass".toCharArray());
        verify(cryptoServiceMock, times(1)).encryptAes("pass".toCharArray());

        assertEquals("property was deleted", 0, props.size());
    }

    @Test
    public void testEncryptConfigs() throws Throwable { // move
        // test encrypting a password: add a password and run

        CryptoService cryptoServiceMock = mock(CryptoService.class);

        // first decryption must fail
        when(cryptoServiceMock.decryptAes("pass".toCharArray()))
                .thenThrow(new KuraException(KuraErrorCode.DECODER_ERROR, "configuration"));
        // so that encryption is attempted at all
        when(cryptoServiceMock.encryptAes("pass".toCharArray())).thenReturn("encrypted".toCharArray());

        List<ComponentConfiguration> configs = new ArrayList<>();

        ComponentConfigurationImpl cfg = new ComponentConfigurationImpl();
        Map<String, Object> props = new HashMap<>();
        props.put("key1", new Password("pass"));
        cfg.setProperties(props);

        configs.add(cfg);

        ComponentUtil.encryptConfigs(configs, cryptoServiceMock);

        verify(cryptoServiceMock, times(1)).decryptAes("pass".toCharArray());
        verify(cryptoServiceMock, times(1)).encryptAes("pass".toCharArray());

        assertEquals("property was updated", 1, props.size());
        assertTrue("key still exists", props.containsKey("key1"));
        assertArrayEquals("key is encrypted", "encrypted".toCharArray(), ((Password) props.get("key1")).getPassword());
    }

    @Test
    public void testEncryptConfigsPreencryptedPassword() throws Throwable {
        // test encrypting a password when the password is already encrypted

        CryptoService cryptoServiceMock = mock(CryptoService.class);

        // decryption succeeds this time
        when(cryptoServiceMock.decryptAes("pass".toCharArray())).thenReturn("pass".toCharArray());

        List<ComponentConfiguration> configs = new ArrayList<>();

        ComponentConfigurationImpl cfg = new ComponentConfigurationImpl();
        Map<String, Object> props = new HashMap<>();
        props.put("key1", new Password("pass"));
        cfg.setProperties(props);

        configs.add(cfg);

        ComponentUtil.encryptConfigs(configs, cryptoServiceMock);

        verify(cryptoServiceMock, times(1)).decryptAes("pass".toCharArray());
        verify(cryptoServiceMock, times(0)).encryptAes((char[]) ArgumentMatchers.any());

        assertEquals("property remains", 1, props.size());
        assertTrue("key still exists", props.containsKey("key1"));
        assertArrayEquals("key is already encrypted", "pass".toCharArray(),
                ((Password) props.get("key1")).getPassword());
    }

}
