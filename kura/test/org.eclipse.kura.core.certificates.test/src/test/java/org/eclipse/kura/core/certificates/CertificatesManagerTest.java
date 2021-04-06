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
 *******************************************************************************/
package org.eclipse.kura.core.certificates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.certificate.CertificateInfo;
import org.eclipse.kura.certificate.KuraCertificate;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.junit.BeforeClass;
import org.junit.Test;

public class CertificatesManagerTest {

    private static String defaultKeystore;
    private static String httpsKeystore;
    private static String sslKeystore;
    private static CertificatesManager certificatesManager;

    @BeforeClass
    public static void setup() throws NoSuchFieldException {
        certificatesManager = new CertificatesManager();
        defaultKeystore = (String) TestUtil.getFieldValue(certificatesManager, "DEFAULT_KEYSTORE_SERVICE_PID");
        httpsKeystore = (String) TestUtil.getFieldValue(certificatesManager, "LOGIN_KEYSTORE_SERVICE_PID");
        sslKeystore = (String) TestUtil.getFieldValue(certificatesManager, "SSL_KEYSTORE_SERVICE_PID");
    }

    @Test
    public void returnCertificateTest()
            throws NoSuchFieldException, KuraException, GeneralSecurityException, IOException {
        X509Certificate certMock = mock(X509Certificate.class);
        KeystoreService ksMock = mock(KeystoreService.class);
        when(ksMock.getEntry("certTest")).thenReturn(new TrustedCertificateEntry(certMock));
        Map<String, KeystoreService> keystoreServices = new HashMap<>();
        keystoreServices.put("keystoreTest", ksMock);
        TestUtil.setFieldValue(certificatesManager, "keystoreServices", keystoreServices);

        String id = "keystoreTest:certTest";
        KuraCertificate kuraCertificate = certificatesManager.getCertificate(id);
        assertEquals("keystoreTest", kuraCertificate.getKeystoreId());
        assertEquals("certTest", kuraCertificate.getAlias());
        assertTrue(kuraCertificate.getCertificate().isPresent());
    }

    @Test
    public void storeCertificateInDefaultKeystoreTest()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        String alias = "dm_certTest";
        X509Certificate certMock = mock(X509Certificate.class);
        KeystoreService ksMock = mock(KeystoreService.class);
        when(ksMock.getEntry(alias)).thenReturn(new TrustedCertificateEntry(certMock));
        Map<String, KeystoreService> keystoreServices = new HashMap<>();
        keystoreServices.put(defaultKeystore, ksMock);
        TestUtil.setFieldValue(certificatesManager, "keystoreServices", keystoreServices);

        KuraCertificate kuraCertificate = new KuraCertificate(defaultKeystore, alias, certMock);
        certificatesManager.storeCertificate(certMock, alias);
        kuraCertificate = certificatesManager.getCertificate(defaultKeystore + ":" + alias);
        assertEquals(defaultKeystore, kuraCertificate.getKeystoreId());
        assertEquals(alias, kuraCertificate.getAlias());
        verify(ksMock).setEntry(eq(alias), anyObject());
    }

    @Test
    public void storeCertificateInHttpsKeystoreTest()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        String alias = "login_certTest";
        X509Certificate certMock = mock(X509Certificate.class);
        KeystoreService ksMock = mock(KeystoreService.class);
        when(ksMock.getEntry(alias)).thenReturn(new TrustedCertificateEntry(certMock));
        Map<String, KeystoreService> keystoreServices = new HashMap<>();
        keystoreServices.put(httpsKeystore, ksMock);
        TestUtil.setFieldValue(certificatesManager, "keystoreServices", keystoreServices);

        certificatesManager.storeCertificate(certMock, alias);
        KuraCertificate kuraCertificate = certificatesManager.getCertificate(httpsKeystore + ":" + alias);
        assertEquals(httpsKeystore, kuraCertificate.getKeystoreId());
        assertEquals(alias, kuraCertificate.getAlias());
        verify(ksMock).setEntry(eq(alias), anyObject());
    }

    @Test
    public void listBundleCertificatesAliasesTest()
            throws NoSuchFieldException, GeneralSecurityException, IOException, KuraException {
        List<String> aliases = new ArrayList<>();
        aliases.add("bundle_certTest1");
        aliases.add("bundle_certTest2");
        X509Certificate certMock = mock(X509Certificate.class);
        KeystoreService ksMock = mock(KeystoreService.class);
        when(ksMock.getAliases()).thenReturn(aliases);
        Map<String, KeystoreService> keystoreServices = new HashMap<>();
        keystoreServices.put(defaultKeystore, ksMock);
        TestUtil.setFieldValue(certificatesManager, "keystoreServices", keystoreServices);

        aliases.stream().forEach(alias -> {
            try {
                certificatesManager.storeCertificate(certMock, alias);
            } catch (KuraException e) {
            }
        });

        List<String> aliasList = Collections.list(certificatesManager.listBundleCertificatesAliases());
        assertEquals(aliases.size(), aliasList.size());
        aliasList.stream().forEach(alias -> assertTrue(alias.startsWith("bundle_")));
    }

    @Test
    public void listDMCertificatesAliasesTest()
            throws NoSuchFieldException, GeneralSecurityException, IOException, KuraException {
        List<String> aliases = new ArrayList<>();
        aliases.add("dm_certTest1");
        aliases.add("dm_certTest2");
        X509Certificate certMock = mock(X509Certificate.class);
        KeystoreService ksMock = mock(KeystoreService.class);
        when(ksMock.getAliases()).thenReturn(aliases);
        Map<String, KeystoreService> keystoreServices = new HashMap<>();
        keystoreServices.put(defaultKeystore, ksMock);
        TestUtil.setFieldValue(certificatesManager, "keystoreServices", keystoreServices);

        aliases.stream().forEach(alias -> {
            try {
                certificatesManager.storeCertificate(certMock, alias);
            } catch (KuraException e) {
            }
        });

        List<String> aliasList = Collections.list(certificatesManager.listDMCertificatesAliases());
        assertEquals(aliases.size(), aliasList.size());
        aliasList.stream().forEach(alias -> assertTrue(alias.startsWith("dm_")));
    }

    @Test
    public void listStoredCertificatesAliasesTest()
            throws NoSuchFieldException, GeneralSecurityException, IOException, KuraException {
        List<String> aliases = new ArrayList<>();
        aliases.add("dm_certTest1");
        aliases.add("dm_certTest2");
        X509Certificate certMock = mock(X509Certificate.class);
        KeystoreService ksMock = mock(KeystoreService.class);
        when(ksMock.getAliases()).thenReturn(aliases);
        Map<String, KeystoreService> keystoreServices = new HashMap<>();
        keystoreServices.put(defaultKeystore, ksMock);
        TestUtil.setFieldValue(certificatesManager, "keystoreServices", keystoreServices);

        aliases.stream().forEach(alias -> {
            try {
                certificatesManager.storeCertificate(certMock, alias);
            } catch (KuraException e) {
            }
        });

        Set<CertificateInfo> certificates = certificatesManager.listStoredCertificates();
        assertEquals(aliases.size(), certificates.size());
        certificates.stream().forEach(cert -> assertTrue(cert.getAlias().startsWith("dm_")));
    }

    @Test
    public void listSSLCertificatesAliasesTest()
            throws NoSuchFieldException, GeneralSecurityException, IOException, KuraException {
        List<String> aliases = new ArrayList<>();
        aliases.add("certTest1");
        aliases.add("certTest2");
        X509Certificate certMock = mock(X509Certificate.class);
        KeystoreService ksMock = mock(KeystoreService.class);
        when(ksMock.getAliases()).thenReturn(aliases);
        Map<String, KeystoreService> keystoreServices = new HashMap<>();
        keystoreServices.put(sslKeystore, ksMock);
        TestUtil.setFieldValue(certificatesManager, "keystoreServices", keystoreServices);

        aliases.stream().forEach(alias -> {
            try {
                certificatesManager.storeCertificate(certMock, alias);
            } catch (KuraException e) {
            }
        });

        List<String> aliasList = Collections.list(certificatesManager.listSSLCertificatesAliases());
        assertEquals(aliases.size(), aliasList.size());
        aliasList.stream().forEach(alias -> assertTrue(alias.startsWith("certTest")));
    }

    @Test
    public void removeCertificateTest()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        Map<String, KeystoreService> keystoreServices = new HashMap<>();
        keystoreServices.put("keystoreTest", ksMock);
        TestUtil.setFieldValue(certificatesManager, "keystoreServices", keystoreServices);

        String id = "keystoreTest:certTest";
        certificatesManager.removeCertificate("certTest");
        verify(ksMock).deleteEntry("certTest");
    }

    @Test
    public void verifySignatureTest() {
        // Not implemented, always returns true
        CertificatesManager manager = new CertificatesManager();
        assertTrue(manager.verifySignature(null, null));
    }

    @Test
    public void getCertificatesTest()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        X509Certificate certMock = mock(X509Certificate.class);
        KeystoreService ksMock = mock(KeystoreService.class);
        Map<String, java.security.KeyStore.Entry> entries = new HashMap<>();
        entries.put("certTest1", new TrustedCertificateEntry(certMock));
        entries.put("certTest2", new TrustedCertificateEntry(certMock));
        when(ksMock.getEntries()).thenReturn(entries);
        Map<String, KeystoreService> keystoreServices = new HashMap<>();
        keystoreServices.put("keystoreTest", ksMock);
        TestUtil.setFieldValue(certificatesManager, "keystoreServices", keystoreServices);

        List<KuraCertificate> kuraCertificates = certificatesManager.getCertificates();
        assertEquals(2, kuraCertificates.size());
        assertEquals("keystoreTest", kuraCertificates.get(0).getKeystoreId());
        assertEquals("certTest2", kuraCertificates.get(0).getAlias());
    }

    @Test
    public void getCertificateTest() throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        X509Certificate certMock = mock(X509Certificate.class);
        KeystoreService ksMock = mock(KeystoreService.class);
        when(ksMock.getEntry("certTest")).thenReturn(new TrustedCertificateEntry(certMock));
        Map<String, KeystoreService> keystoreServices = new HashMap<>();
        keystoreServices.put("keystoreTest", ksMock);
        TestUtil.setFieldValue(certificatesManager, "keystoreServices", keystoreServices);

        String id = "keystoreTest:certTest";
        KuraCertificate kuraCertificate = certificatesManager.getCertificate(id);
        assertEquals("keystoreTest", kuraCertificate.getKeystoreId());
        assertEquals("certTest", kuraCertificate.getAlias());
    }

    @Test
    public void addCertificateTest() throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        X509Certificate certMock = mock(X509Certificate.class);
        KeystoreService ksMock = mock(KeystoreService.class);
        when(ksMock.getEntry("certTest")).thenReturn(new TrustedCertificateEntry(certMock));
        Map<String, KeystoreService> keystoreServices = new HashMap<>();
        keystoreServices.put("keystoreTest", ksMock);
        TestUtil.setFieldValue(certificatesManager, "keystoreServices", keystoreServices);

        String id = "keystoreTest:certTest";
        KuraCertificate kuraCertificate = new KuraCertificate("keystoreTest", "certTest", certMock);
        certificatesManager.addCertificate(kuraCertificate);
        kuraCertificate = certificatesManager.getCertificate(id);
        assertEquals("keystoreTest", kuraCertificate.getKeystoreId());
        assertEquals("certTest", kuraCertificate.getAlias());
        verify(ksMock).setEntry(eq("certTest"), anyObject());
    }

    @Test
    public void updateCertificateTest()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        X509Certificate certMock = mock(X509Certificate.class);
        KeystoreService ksMock = mock(KeystoreService.class);
        when(ksMock.getEntry("certTest")).thenReturn(new TrustedCertificateEntry(certMock));
        Map<String, KeystoreService> keystoreServices = new HashMap<>();
        keystoreServices.put("keystoreTest", ksMock);
        TestUtil.setFieldValue(certificatesManager, "keystoreServices", keystoreServices);

        String id = "keystoreTest:certTest";
        KuraCertificate kuraCertificate = new KuraCertificate("keystoreTest", "certTest", certMock);
        certificatesManager.updateCertificate(kuraCertificate);
        kuraCertificate = certificatesManager.getCertificate(id);
        assertEquals("keystoreTest", kuraCertificate.getKeystoreId());
        assertEquals("certTest", kuraCertificate.getAlias());
        verify(ksMock).setEntry(eq("certTest"), anyObject());
    }

    @Test
    public void deleteCertificateTest()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        Map<String, KeystoreService> keystoreServices = new HashMap<>();
        keystoreServices.put("keystoreTest", ksMock);
        TestUtil.setFieldValue(certificatesManager, "keystoreServices", keystoreServices);

        String id = "keystoreTest:certTest";
        certificatesManager.deleteCertificate(id);
        verify(ksMock).deleteEntry("certTest");
    }

    String getDefaultKeyStore(CertificatesManager manager) throws NoSuchFieldException {
        return (String) TestUtil.getFieldValue(manager, "DEFAULT_KEYSTORE");
    }

    CryptoService createMockCryptoService(String keyStorePath, String keyStorePassword) throws NoSuchFieldException {
        CryptoService mockService = mock(CryptoService.class);
        when(mockService.getKeyStorePassword(keyStorePath)).thenReturn(keyStorePassword.toCharArray());

        return mockService;
    }

}
