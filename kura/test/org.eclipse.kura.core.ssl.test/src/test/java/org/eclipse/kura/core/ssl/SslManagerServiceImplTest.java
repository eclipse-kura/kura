/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.x500.X500Principal;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.security.keystore.KeystoreChangedEvent;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.eclipse.kura.ssl.SslServiceListener;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

public class SslManagerServiceImplTest {

    private static final String CERT_FILE_PATH = "target/test-classes/cert";
    private static final String STORE_PATH = "target/key.store";
    private static final String STORE_PASS = "pass";

    private KeyStore store;

    @Before
    public void setupDefaultKeystore()
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        // create a new keystore each time the tests should run

        this.store = KeyStore.getInstance("jks");

        this.store.load(null, null);

        // KeyPairGenerator gen = KeyPairGenerator.getInstance("DSA");
        // gen.initialize(1024);
        // KeyPair pair = gen.generateKeyPair();
        // Key key = pair.getPrivate();
        //
        // InputStream is = new FileInputStream(CERT_FILE_PATH);
        // Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(is);
        // is.close();
        //
        // Certificate[] chain = { certificate };
        //
        // this.store.setKeyEntry(DEFAULT_KEY_ALIAS, key, STORE_PASS.toCharArray(), chain);

        try (OutputStream os = new FileOutputStream(STORE_PATH)) {
            this.store.store(os, STORE_PASS.toCharArray());
        }
    }

    // @Test(expected = KeyStoreException.class)
    // public void testActivateNoKeystore()
    // throws InterruptedException, NoSuchFieldException, GeneralSecurityException, IOException {
    // SslManagerServiceImpl svc = new SslManagerServiceImpl();
    //
    // ComponentContext ccMock = mock(ComponentContext.class);
    //
    // BundleContext bcMock = mock(BundleContext.class);
    // when(ccMock.getBundleContext()).thenReturn(bcMock);
    //
    // Map<String, Object> properties = new HashMap<>();
    // properties.put("ssl.default.protocol", "TLSv1");
    // properties.put("ssl.default.trustStore", "target/key1.store");
    // properties.put("ssl.hostname.verification", "true");
    // properties.put("ssl.keystore.password", "changeit");
    //
    // svc.activate(ccMock, properties);
    //
    // verify(cs, times(0)).getKeyStorePassword("target/key1.store");
    //
    // svc.getSSLSocketFactory();
    // }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetSSLSocketFactory()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        // test preparation of an SslSocketFactory
        setupDefaultKeystore();

        SslManagerServiceImpl svc = new SslManagerServiceImpl();

        SslServiceListeners listener = new SslServiceListeners(null) {

            @Override
            public void onConfigurationUpdated() {
                // OK
            }
        };

        ComponentContext ccMock = mock(ComponentContext.class);

        BundleContext bcMock = mock(BundleContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("ssl.default.protocol", "TLSv1");
        properties.put("ssl.hostname.verification", "true");

        KeystoreService keystoreService = mock(KeystoreService.class);
        when(keystoreService.getKeyStore()).thenReturn(store);

        svc.setKeystoreService(keystoreService, Collections.singletonMap("kura.service.pid", "foo"));
        svc.activate(ccMock, properties);

        TestUtil.setFieldValue(svc, "sslServiceListeners", listener);

        SSLSocketFactory factory = svc.getSSLSocketFactory();

        Map<ConnectionSslOptions, SSLContext> sslContexts = (Map<ConnectionSslOptions, SSLContext>) TestUtil
                .getFieldValue(svc, "sslContexts");

        assertNotNull(factory);
        assertEquals(1, sslContexts.size());
        assertEquals(factory, sslContexts.values().iterator().next().getSocketFactory());

        svc.updated(properties);

        Map<ConnectionSslOptions, SSLContext> updatedSslContexts = (Map<ConnectionSslOptions, SSLContext>) TestUtil
                .getFieldValue(svc, "sslContexts");
        assertNotNull(updatedSslContexts);
        assertEquals(0, updatedSslContexts.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSSLSocketFactoryCacheCleanedAfterSet()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        // test preparation of an SslSocketFactory
        setupDefaultKeystore();

        SslManagerServiceImpl svc = new SslManagerServiceImpl();

        SslServiceListener listener = () -> {
        };

        ComponentContext ccMock = mock(ComponentContext.class);

        BundleContext bcMock = mock(BundleContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("ssl.default.protocol", "TLSv1");
        properties.put("ssl.hostname.verification", "true");

        KeystoreService keystoreService = mock(KeystoreService.class);
        when(keystoreService.getKeyStore()).thenReturn(store);

        svc.setKeystoreService(keystoreService, Collections.singletonMap("kura.service.pid", "foo"));
        svc.activate(ccMock, properties);

        TestUtil.setFieldValue(svc, "sslServiceListeners", listener);

        SSLSocketFactory factory = svc.getSSLSocketFactory();

        Map<ConnectionSslOptions, SSLContext> sslContexts = (Map<ConnectionSslOptions, SSLContext>) TestUtil
                .getFieldValue(svc, "sslContexts");

        assertNotNull(factory);
        assertEquals(1, sslContexts.size());
        assertEquals(factory, sslContexts.values().iterator().next().getSocketFactory());

        svc.setKeystoreService(keystoreService, Collections.singletonMap("kura.service.pid", "foo"));

        Map<ConnectionSslOptions, SSLContext> updatedSslContexts = (Map<ConnectionSslOptions, SSLContext>) TestUtil
                .getFieldValue(svc, "sslContexts");

        assertEquals(0, updatedSslContexts.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSSLSocketFactoryCacheCleanedAfterUnset()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        // test preparation of an SslSocketFactory
        setupDefaultKeystore();

        SslManagerServiceImpl svc = new SslManagerServiceImpl();

        SslServiceListener listener = () -> {
        };

        ComponentContext ccMock = mock(ComponentContext.class);

        BundleContext bcMock = mock(BundleContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("ssl.default.protocol", "TLSv1");
        properties.put("ssl.hostname.verification", "true");

        KeystoreService keystoreService = mock(KeystoreService.class);
        when(keystoreService.getKeyStore()).thenReturn(store);

        svc.setKeystoreService(keystoreService, Collections.singletonMap("kura.service.pid", "foo"));
        svc.activate(ccMock, properties);

        TestUtil.setFieldValue(svc, "sslServiceListeners", listener);

        SSLSocketFactory factory = svc.getSSLSocketFactory();

        Map<ConnectionSslOptions, SSLContext> sslContexts = (Map<ConnectionSslOptions, SSLContext>) TestUtil
                .getFieldValue(svc, "sslContexts");

        assertNotNull(factory);
        assertEquals(1, sslContexts.size());
        assertEquals(factory, sslContexts.values().iterator().next().getSocketFactory());

        svc.unsetKeystoreService(keystoreService);

        Map<ConnectionSslOptions, SSLContext> updatedSslContexts = (Map<ConnectionSslOptions, SSLContext>) TestUtil
                .getFieldValue(svc, "sslContexts");

        assertEquals(0, updatedSslContexts.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSSLSocketFactoryCacheCleanedAfterEvent()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        // test preparation of an SslSocketFactory
        setupDefaultKeystore();

        SslManagerServiceImpl svc = new SslManagerServiceImpl();

        SslServiceListener listener = () -> {
        };

        ComponentContext ccMock = mock(ComponentContext.class);

        BundleContext bcMock = mock(BundleContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("ssl.default.protocol", "TLSv1");
        properties.put("ssl.hostname.verification", "true");

        KeystoreService keystoreService = mock(KeystoreService.class);
        when(keystoreService.getKeyStore()).thenReturn(store);

        svc.setKeystoreService(keystoreService, Collections.singletonMap("kura.service.pid", "foo"));
        svc.activate(ccMock, properties);

        TestUtil.setFieldValue(svc, "sslServiceListeners", listener);

        SSLSocketFactory factory = svc.getSSLSocketFactory();

        Map<ConnectionSslOptions, SSLContext> sslContexts = (Map<ConnectionSslOptions, SSLContext>) TestUtil
                .getFieldValue(svc, "sslContexts");

        assertNotNull(factory);
        assertEquals(1, sslContexts.size());
        assertEquals(factory, sslContexts.values().iterator().next().getSocketFactory());

        svc.handleEvent(new KeystoreChangedEvent("foo"));

        Map<ConnectionSslOptions, SSLContext> updatedSslContexts = (Map<ConnectionSslOptions, SSLContext>) TestUtil
                .getFieldValue(svc, "sslContexts");

        assertEquals(0, updatedSslContexts.size());
    }

    @Test
    @Ignore
    public void testPrivateKey() throws Throwable {
        // test key installation
        setupDefaultKeystore();

        SslManagerServiceImpl svc = new SslManagerServiceImpl();

        SslServiceListeners listener = new SslServiceListeners(null) {

            @Override
            public void onConfigurationUpdated() {
                // OK
            }
        };

        TestUtil.setFieldValue(svc, "sslServiceListeners", listener);

        Map<ConnectionSslOptions, SSLContext> sslContexts = new ConcurrentHashMap<>();
        TestUtil.setFieldValue(svc, "sslContexts", sslContexts);

        Map<String, Object> properties = new HashMap<>();
        properties.put("ssl.default.protocol", "TLSv1");
        properties.put("ssl.hostname.verification", "true");

        KeystoreService keystoreService = mock(KeystoreService.class);
        when(keystoreService.getKeyStore()).thenReturn(store);

        svc.setKeystoreService(keystoreService, Collections.singletonMap("kura.service.pid", "foo"));

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
        svc.installPrivateKey(alias, (PrivateKey) key, STORE_PASS.toCharArray(), chain);

        KeyStore store = KeyStore.getInstance("jks");

        is = new FileInputStream(STORE_PATH);
        store.load(is, STORE_PASS.toCharArray());
        is.close();

        assertTrue(store.isKeyEntry(alias));

        // install another private key and check that getKeyStore only returns one, if alias is specified

        pair = gen.generateKeyPair();
        key = pair.getPrivate();

        svc.installPrivateKey("secondKey", (PrivateKey) key, STORE_PASS.toCharArray(), chain);

        KeyStore ks = (KeyStore) TestUtil.invokePrivate(svc, "getKeyStore", STORE_PATH, STORE_PASS.toCharArray(),
                alias);

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
    @Ignore
    public void testCertificates() throws NoSuchFieldException, GeneralSecurityException, IOException, KuraException {
        // test working with certificates
        setupDefaultKeystore();

        String alias = "kura";

        SslManagerServiceImpl svc = new SslManagerServiceImpl();

        KeystoreService keystoreService = mock(KeystoreService.class);
        when(keystoreService.getKeyStore()).thenReturn(store);

        svc.setKeystoreService(keystoreService, Collections.singletonMap("kura.service.pid", "foo"));

        SslServiceListeners listener = new SslServiceListeners(null) {

            @Override
            public void onConfigurationUpdated() {
                // OK
            }
        };

        TestUtil.setFieldValue(svc, "sslServiceListeners", listener);

        Map<ConnectionSslOptions, SSLContext> sslContexts = new ConcurrentHashMap<>();
        TestUtil.setFieldValue(svc, "sslContexts", sslContexts);

        Map<String, Object> properties = new HashMap<>();
        properties.put("ssl.default.protocol", "TLSv1");
        properties.put("ssl.hostname.verification", "true");

        svc.updated(properties);

        KeyStore store = KeyStore.getInstance("jks");

        InputStream is = new FileInputStream(STORE_PATH);
        store.load(is, STORE_PASS.toCharArray());
        is.close();

        // add a certificate
        X509Certificate[] certificates = svc.getTrustCertificates();

        assertEquals(0, certificates.length);

        is = new FileInputStream(CERT_FILE_PATH);
        Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(is);
        is.close();

        svc.installTrustCertificate(alias, (X509Certificate) certificate);

        // does service return the new cert?
        X509Certificate[] tcs = svc.getTrustCertificates();
        assertEquals(1, tcs.length);
        X509Certificate cert = tcs[0];
        assertEquals(BigInteger.valueOf(0x4afb9c19), cert.getSerialNumber());

        // is it really in the keystore as well
        is = new FileInputStream(STORE_PATH);
        store.load(is, STORE_PASS.toCharArray());
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
        is = new FileInputStream(STORE_PATH);
        store.load(is, STORE_PASS.toCharArray());
        is.close();

        assertFalse(store.isCertificateEntry(alias));
    }
}
