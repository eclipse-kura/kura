/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.x500.X500Principal;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.system.SystemService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

public class SslManagerServiceImplTest {

    private static final String CERT_FILE_PATH = "target/test-classes/cert";
    private static final String KEY_STORE_PATH = "target/key.store";
    private static final char[] KEY_STORE_PASS = "pass".toCharArray();

    @Before
    public void setup() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        // create a new keystore each time the tests should run

        KeyStore store = KeyStore.getInstance("jks");

        store.load(null, null);

        try (OutputStream os = new FileOutputStream(KEY_STORE_PATH)) {
            store.store(os, KEY_STORE_PASS);
        }
    }

    @Test
    public void testActivate() throws KuraException, InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, InterruptedException {

        // activation and deactivation

        SslManagerServiceImpl svc = new SslManagerServiceImpl();

        CryptoService csMock = mock(CryptoService.class);
        svc.setCryptoService(csMock);

        char[] enc = "pass".toCharArray();
        char[] dec = "changeit".toCharArray();
        when(csMock.decryptAes(enc)).thenReturn(dec);

        char[] origPass = "pass".toCharArray();
        when(csMock.getKeyStorePassword(KEY_STORE_PATH)).thenReturn(origPass);

        when(csMock.isFrameworkSecure()).thenReturn(true);

        SystemService ssMock = mock(SystemService.class);
        svc.setSystemService(ssMock);

        when(ssMock.getJavaKeyStorePassword()).thenReturn(KEY_STORE_PASS);

        ComponentContext ccMock = mock(ComponentContext.class);

        BundleContext bcMock = mock(BundleContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        final Object lock = new Object();

        doAnswer(invocation -> {
            synchronized (lock) {
                lock.notifyAll();
            }

            throw new NullPointerException("test"); // break the scheduler loop
        }).when(ccMock).getServiceReference(); // called during changeDefaultKeystorePassword()

        Map<String, Object> properties = new HashMap<>();
        properties.put("ssl.default.protocol", "TLSv1");
        properties.put("ssl.default.trustStore", KEY_STORE_PATH);
        properties.put("ssl.hostname.verification", "true");
        properties.put("ssl.keystore.password", "pass");

        svc.activate(ccMock, properties);

        synchronized (lock) {
            lock.wait(20000);
        }

        verify(ccMock, times(1)).getServiceReference();

        svc.deactivate(ccMock);
    }

    @Test
    public void testUpdatePassFailures() throws KuraException, NoSuchFieldException {
        // test password failures during update

        SslManagerServiceImpl svc = new SslManagerServiceImpl();

        CryptoService csMock = mock(CryptoService.class);
        svc.setCryptoService(csMock);

        char[] enc = "pass".toCharArray();
        when(csMock.decryptAes(enc)).thenThrow(new KuraException(KuraErrorCode.INVALID_PARAMETER, "test"));

        when(csMock.getKeyStorePassword(KEY_STORE_PATH)).thenReturn(null);

        AtomicBoolean visited = new AtomicBoolean(false);
        SslServiceListeners listener = new SslServiceListeners(null) {

            @Override
            public void onConfigurationUpdated() {
                visited.set(true);
            }
        };

        TestUtil.setFieldValue(svc, "sslServiceListeners", listener);

        Map<String, Object> properties = new HashMap<>();
        properties.put("ssl.default.protocol", "TLSv1");
        properties.put("ssl.default.trustStore", KEY_STORE_PATH);
        properties.put("ssl.hostname.verification", "true");
        properties.put("ssl.keystore.password", "pass");

        svc.updated(properties);

        assertTrue(visited.get());
    }

    @Test
    public void testUpdate() throws KuraException, NoSuchFieldException {
        // test successful update

        SslManagerServiceImpl svc = new SslManagerServiceImpl();

        CryptoService csMock = mock(CryptoService.class);
        svc.setCryptoService(csMock);

        char[] enc = "pass".toCharArray();
        char[] dec = "changeit".toCharArray();
        when(csMock.decryptAes(enc)).thenReturn(dec);

        char[] origPass = "pass".toCharArray();
        when(csMock.getKeyStorePassword(KEY_STORE_PATH)).thenReturn(origPass);

        AtomicBoolean visited = new AtomicBoolean(false);
        SslServiceListeners listener = new SslServiceListeners(null) {

            @Override
            public void onConfigurationUpdated() {
                visited.set(true);
            }
        };

        TestUtil.setFieldValue(svc, "sslServiceListeners", listener);

        Map<String, Object> properties = new HashMap<>();
        properties.put("ssl.default.protocol", "TLSv1");
        properties.put("ssl.default.trustStore", KEY_STORE_PATH);
        properties.put("ssl.hostname.verification", "true");
        properties.put("ssl.keystore.password", "pass");

        svc.updated(properties);

        assertTrue(visited.get());

        verify(csMock, times(1)).setKeyStorePassword(KEY_STORE_PATH, dec);
        ;
    }

    @Test
    public void testUpdateNoPassOK() throws KuraException, NoSuchFieldException {
        // test update with passwords not matching the keystore

        SslManagerServiceImpl svc = new SslManagerServiceImpl();

        CryptoService csMock = mock(CryptoService.class);
        svc.setCryptoService(csMock);

        char[] enc = "pass".toCharArray();
        char[] dec = "changeit".toCharArray();
        when(csMock.decryptAes(enc)).thenReturn(dec);

        char[] enc2 = "passs".toCharArray();
        char[] dec2 = "changeitt".toCharArray();
        when(csMock.decryptAes(enc2)).thenReturn(dec2);

        char[] origPass = "passs".toCharArray();
        when(csMock.getKeyStorePassword(KEY_STORE_PATH)).thenReturn(origPass);

        AtomicBoolean visited = new AtomicBoolean(false);
        SslServiceListeners listener = new SslServiceListeners(null) {

            @Override
            public void onConfigurationUpdated() {
                visited.set(true);
            }
        };

        TestUtil.setFieldValue(svc, "sslServiceListeners", listener);

        Map<String, Object> properties = new HashMap<>();
        properties.put("ssl.default.protocol", "TLSv1");
        properties.put("ssl.default.trustStore", KEY_STORE_PATH);
        properties.put("ssl.hostname.verification", "true");
        properties.put("ssl.keystore.password", "pass");

        svc.updated(properties);

        assertTrue(visited.get());

        verify(csMock, times(0)).setKeyStorePassword(anyString(), (char[]) anyObject());
    }

    @Test
    public void testUpdateSamePass() throws KuraException, NoSuchFieldException {
        // test update with same old and new passwords

        SslManagerServiceImpl svc = new SslManagerServiceImpl();

        CryptoService csMock = mock(CryptoService.class);
        svc.setCryptoService(csMock);

        char[] enc = "pass".toCharArray();
        char[] dec = "pass".toCharArray();
        when(csMock.decryptAes(enc)).thenReturn(dec);

        char[] origPass = "pass".toCharArray();
        when(csMock.getKeyStorePassword(KEY_STORE_PATH)).thenReturn(origPass);

        AtomicBoolean visited = new AtomicBoolean(false);
        SslServiceListeners listener = new SslServiceListeners(null) {

            @Override
            public void onConfigurationUpdated() {
                visited.set(true);
            }
        };

        TestUtil.setFieldValue(svc, "sslServiceListeners", listener);

        Map<String, Object> properties = new HashMap<>();
        properties.put("ssl.default.protocol", "TLSv1");
        properties.put("ssl.default.trustStore", KEY_STORE_PATH);
        properties.put("ssl.hostname.verification", "true");
        properties.put("ssl.keystore.password", "pass");

        svc.updated(properties);

        assertTrue(visited.get());

        verify(csMock, times(0)).setKeyStorePassword(anyString(), (char[]) anyObject());
    }

    @Test
    public void testUpdateKeyEntiesPasswords() throws Throwable {
        // test keystore password update

        SslManagerServiceImpl svc = new SslManagerServiceImpl();

        // install a new private key and check it's really there

        KeyPairGenerator gen = KeyPairGenerator.getInstance("DSA");
        gen.initialize(1024);
        KeyPair pair = gen.generateKeyPair();
        Key key = pair.getPrivate();

        InputStream is = new FileInputStream(CERT_FILE_PATH);
        Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(is);
        is.close();

        Certificate[] chain = { certificate };

        KeyStore store = KeyStore.getInstance("jks");

        is = new FileInputStream(KEY_STORE_PATH);
        store.load(is, KEY_STORE_PASS);
        is.close();

        String alias = "kuraTestAlias";
        store.setKeyEntry(alias, key, KEY_STORE_PASS, chain);

        assertTrue(store.isKeyEntry(alias));
        store.getKey(alias, KEY_STORE_PASS);

        // update KS password
        char[] newPass = "new password".toCharArray();

        TestUtil.invokePrivate(svc, "updateKeyEntiesPasswords", store, KEY_STORE_PASS, newPass);

        assertTrue(store.isKeyEntry(alias)); // key is still in there

        try {
            store.getKey(alias, KEY_STORE_PASS);
            fail("Old password shouldn't work anymore.");
        } catch (UnrecoverableKeyException e) {
            // expected
        }

        store.getKey(alias, newPass);
    }

    @Test
    public void testGetSSLSocketFactory()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        // test preparation of an SslSocketFactory

        SslManagerServiceImpl svc = new SslManagerServiceImpl();

        CryptoService csMock = mock(CryptoService.class);
        svc.setCryptoService(csMock);

        char[] enc = "pass".toCharArray();
        char[] dec = "pass".toCharArray();
        when(csMock.decryptAes(enc)).thenReturn(dec);

        char[] origPass = "pass".toCharArray();
        when(csMock.getKeyStorePassword(KEY_STORE_PATH)).thenReturn(origPass);

        SslServiceListeners listener = new SslServiceListeners(null) {

            @Override
            public void onConfigurationUpdated() {
                //OK
            }
        };

        TestUtil.setFieldValue(svc, "sslServiceListeners", listener);

        Map<ConnectionSslOptions, SSLSocketFactory> sslFactories = new ConcurrentHashMap<>();
        TestUtil.setFieldValue(svc, "sslSocketFactories", sslFactories);

        Map<String, Object> properties = new HashMap<>();
        properties.put("ssl.default.protocol", "TLSv1");
        properties.put("ssl.default.trustStore", KEY_STORE_PATH);
        properties.put("ssl.hostname.verification", "true");
        properties.put("ssl.keystore.password", "pass");

        svc.updated(properties);

        SSLSocketFactory factory = svc.getSSLSocketFactory();

        assertNotNull(factory);
        assertEquals(1, sslFactories.size());
        assertEquals(factory, sslFactories.values().iterator().next());
    }

    @Test
    public void testPrivateKey() throws Throwable {
        // test key installation

        SslManagerServiceImpl svc = new SslManagerServiceImpl();

        CryptoService csMock = mock(CryptoService.class);
        svc.setCryptoService(csMock);

        char[] enc = "pass".toCharArray();
        char[] dec = "pass".toCharArray();
        when(csMock.decryptAes(enc)).thenReturn(dec);

        char[] origPass = "pass".toCharArray();
        when(csMock.getKeyStorePassword(KEY_STORE_PATH)).thenReturn(origPass);

        SslServiceListeners listener = new SslServiceListeners(null) {

            @Override
            public void onConfigurationUpdated() {
                // OK
            }
        };

        TestUtil.setFieldValue(svc, "sslServiceListeners", listener);

        Map<ConnectionSslOptions, SSLSocketFactory> sslFactories = new ConcurrentHashMap<>();
        TestUtil.setFieldValue(svc, "sslSocketFactories", sslFactories);

        Map<String, Object> properties = new HashMap<>();
        properties.put("ssl.default.protocol", "TLSv1");
        properties.put("ssl.default.trustStore", KEY_STORE_PATH);
        properties.put("ssl.hostname.verification", "true");
        properties.put("ssl.keystore.password", "pass");

        svc.updated(properties);

        // install a new private key and check it's really there

        KeyPairGenerator gen = KeyPairGenerator.getInstance("DSA");
        gen.initialize(1024);
        KeyPair pair = gen.generateKeyPair();
        Key key = pair.getPrivate();

        InputStream is = new FileInputStream(CERT_FILE_PATH);
        Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(is);
        is.close();

        Certificate[] chain = { certificate };

        String alias = "kuraTestAlias";
        svc.installPrivateKey(alias, (PrivateKey) key, KEY_STORE_PASS, chain);

        KeyStore store = KeyStore.getInstance("jks");

        is = new FileInputStream(KEY_STORE_PATH);
        store.load(is, KEY_STORE_PASS);
        is.close();

        assertTrue(store.isKeyEntry(alias));

        // install another private key and check that getKeyStore only returns one, if alias is specified

        pair = gen.generateKeyPair();
        key = pair.getPrivate();

        svc.installPrivateKey("secondKey", (PrivateKey) key, KEY_STORE_PASS, chain);

        KeyStore ks = (KeyStore) TestUtil.invokePrivate(svc, "getKeyStore", KEY_STORE_PATH, KEY_STORE_PASS, alias);

        assertNotNull(ks);
        assertTrue(ks.containsAlias(alias));

        Enumeration<String> aliases = ks.aliases(); // all lowercase aliases???
        assertNotNull(aliases);

        List<String> aliasList = new ArrayList<>(); // only for size
        while (aliases.hasMoreElements()) {
            aliasList.add(aliases.nextElement());
        }
        assertEquals(1, aliasList.size());
    }

    @Test
    public void testCertificates()
            throws NoSuchFieldException, GeneralSecurityException, IOException, KuraException {
        // test working with certificates

        SslManagerServiceImpl svc = new SslManagerServiceImpl();

        CryptoService csMock = mock(CryptoService.class);
        svc.setCryptoService(csMock);

        char[] enc = "pass".toCharArray();
        char[] dec = "pass".toCharArray();
        when(csMock.decryptAes(enc)).thenReturn(dec);

        char[] origPass = "pass".toCharArray();
        when(csMock.getKeyStorePassword(KEY_STORE_PATH)).thenReturn(origPass);

        SslServiceListeners listener = new SslServiceListeners(null) {

            @Override
            public void onConfigurationUpdated() {
                // OK
            }
        };

        TestUtil.setFieldValue(svc, "sslServiceListeners", listener);

        Map<ConnectionSslOptions, SSLSocketFactory> sslFactories = new ConcurrentHashMap<>();
        TestUtil.setFieldValue(svc, "sslSocketFactories", sslFactories);

        Map<String, Object> properties = new HashMap<>();
        properties.put("ssl.default.protocol", "TLSv1");
        properties.put("ssl.default.trustStore", KEY_STORE_PATH);
        properties.put("ssl.hostname.verification", "true");
        properties.put("ssl.keystore.password", "pass");

        svc.updated(properties);

        KeyStore store = KeyStore.getInstance("jks");

        InputStream is = new FileInputStream(KEY_STORE_PATH);
        store.load(is, KEY_STORE_PASS);
        is.close();

        // add a certificate
        X509Certificate[] certificates = svc.getTrustCertificates();

        assertEquals(0, certificates.length);

        is = new FileInputStream(CERT_FILE_PATH);
        Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(is);
        is.close();

        String alias = "kura";
        svc.installTrustCertificate(alias, (X509Certificate) certificate);

        // does service return the new cert?
        X509Certificate[] tcs = svc.getTrustCertificates();
        assertEquals(1, tcs.length);
        X509Certificate cert = tcs[0];
        assertEquals(BigInteger.valueOf(0x4afb9c19), cert.getSerialNumber());

        // is it really in the keystore as well
        is = new FileInputStream(KEY_STORE_PATH);
        store.load(is, KEY_STORE_PASS);
        is.close();

        cert = (X509Certificate) store.getCertificate(alias);

        assertNotNull(cert);
        assertEquals(BigInteger.valueOf(0x4afb9c19), cert.getSerialNumber());
        X500Principal issuer = cert.getIssuerX500Principal();
        String rfcNames = issuer.getName();
        assertEquals("CN=kura", rfcNames);

        // delete the certificate
        svc.deleteTrustCertificate(alias);

        // does service stil return the deleted cert?
        tcs = svc.getTrustCertificates();
        assertEquals(0, tcs.length);

        // is it really not in the keystore enymore
        is = new FileInputStream(KEY_STORE_PATH);
        store.load(is, KEY_STORE_PASS);
        is.close();

        assertFalse(store.isCertificateEntry(alias));
    }

}
