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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.keystore.KeystoreServiceOptions;
import org.junit.Test;

public class KeystoreServiceOptionsTest {

    private static final String CHANGEIT_PASSWORD = "changeit";

    @Test(expected = IllegalArgumentException.class)
    public void testNullPropertiesCrypto() {
        new KeystoreServiceOptions(null);
    }

    @Test
    public void testConstructorMissingProps() throws KuraException {
        Map<String, Object> properties = new HashMap<>();

        KeystoreServiceOptions keystoreServiceOptions = new KeystoreServiceOptions(properties);

        assertEquals("/tmp", keystoreServiceOptions.getKeystorePath());
        assertArrayEquals(CHANGEIT_PASSWORD.toCharArray(), keystoreServiceOptions.getKeystorePassword());
    }

    @Test
    public void testConstructor() throws KuraException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("keystore.path", "/abc");
        properties.put("keystore.password", "testPassword");

        KeystoreServiceOptions keystoreServiceOptions = new KeystoreServiceOptions(properties);

        assertEquals("/abc", keystoreServiceOptions.getKeystorePath());
        assertArrayEquals("testPassword".toCharArray(), keystoreServiceOptions.getKeystorePassword());
    }

    @Test
    public void testCompareSame() throws KuraException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("keystore.path", "/abc");
        properties.put("keystore.password", "testPassword");

        KeystoreServiceOptions keystoreServiceOptions1 = new KeystoreServiceOptions(properties);

        KeystoreServiceOptions keystoreServiceOptions2 = new KeystoreServiceOptions(properties);

        assertEquals(keystoreServiceOptions1, keystoreServiceOptions2);
    }

    @Test
    public void testCompareDifferent() throws KuraException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("keystore.path", "/abc");
        properties.put("keystore.password", "testPassword");

        KeystoreServiceOptions keystoreServiceOptions1 = new KeystoreServiceOptions(properties);

        Map<String, Object> properties2 = new HashMap<>();
        properties2.put("keystore.path", "/abc1");
        properties2.put("keystore.password", "testPassword1");
        KeystoreServiceOptions keystoreServiceOptions2 = new KeystoreServiceOptions(properties2);

        assertNotEquals(keystoreServiceOptions1, keystoreServiceOptions2);

    }

}
