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
package org.eclipse.kura.core.keystore.rest.provider.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.keystore.rest.provider.CsrReadRequest;
import org.eclipse.kura.core.keystore.rest.provider.CsrResponse;
import org.eclipse.kura.core.keystore.rest.provider.DeleteRequest;
import org.eclipse.kura.core.keystore.rest.provider.KeyPairWriteRequest;
import org.eclipse.kura.core.keystore.rest.provider.KeystoreRestService;
import org.eclipse.kura.core.keystore.rest.provider.TrustedCertificateWriteRequest;
import org.eclipse.kura.core.keystore.util.CertificateInfo;
import org.eclipse.kura.core.keystore.util.EntryInfo;
import org.eclipse.kura.core.keystore.util.EntryType;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.security.keystore.KeystoreInfo;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

public class KeystoreRestServiceTest {

    private final String CERTIFICATE_RSA = "-----BEGIN CERTIFICATE-----\n"
            + "MIIDdzCCAl+gAwIBAgIEQsO0gDANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdV\n"
            + "bmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD\n"
            + "VQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3du\n"
            + "MB4XDTIxMDQxNDA4MDIyOFoXDTIxMDcxMzA4MDIyOFowbDEQMA4GA1UEBhMHVW5r\n"
            + "bm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UE\n"
            + "ChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCC\n"
            + "ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJSWJDxu8UNC4JGOgK31WCvz\n"
            + "NKy2ONH+jTVKnBY7Ckb1hljJY0sKO55aG1HNDfkev2lJTsPIz0nJjNsqBvB1flvf\n"
            + "r6XVCxdN0yxvU5g9SpRxE/iiPX0Qt7463OfzyKW97haJrrhF005RHYNcORMY/Phj\n"
            + "hFDnZhtAwpbQLzq2UuIZ7okJsx0IgRbjH71ZZuvYCqG7Ct/bp1D7w3tT7gTbIKYH\n"
            + "ppQyG9rJDEh9+cr9Hyk8Gz7aAbPT/wMH+/vXDjH2j/M1Tmed0ajuGCJumaTQ4eHs\n"
            + "9xW3B3ugycb6e7Osl/4ESRO5RQL1k2GBONv10OrKDoZ5b66xwSJmC/w3BRWQ1cMC\n"
            + "AwEAAaMhMB8wHQYDVR0OBBYEFPospETb5HNeD/DmS9mwt+v/AYq/MA0GCSqGSIb3\n"
            + "DQEBCwUAA4IBAQBxMe1xQVQKt36A5qVlEZyxI9eb6eQRlYzorOgP2tFaOsvDPpRI\n"
            + "CALhPmxgQl/5QvKFfCXKoxWj1Spg4sF6fJp6jhSjLpmChS9lf5fRaWS20/pxIddM\n"
            + "10diq3r6HxLKSxCYK7Pf5scOeZquvwfo8Kxye01bvCMFf1s1K3ZEZszk5Oo2MnWU\n"
            + "U22YnXfZm1C0h2WMUcou35A7CeVAHPWI0Rvefojv1qYlQScJOkCN5lO6C/1qvRhq\n"
            + "nDQdQN/m1HQbpfh2DD6F33nBjkyLQyMRF8uMnspLrLLj8lecSTJZO4fGJOaIXh3O\n"
            + "44da9A02FAf5nRRQpwP2x/4IZ5RTRBzrqbqD\n" + "-----END CERTIFICATE-----";
    private final String CERTIFICATE_DSA = "-----BEGIN CERTIFICATE-----\n"
            + "MIIDODCCAvSgAwIBAgIERIUjyDANBglghkgBZQMEAwIFADBsMRAwDgYDVQQGEwdV\n"
            + "bmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD\n"
            + "VQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3du\n"
            + "MB4XDTIxMDQxMzA4MTk0M1oXDTIxMDcxMjA4MTk0M1owbDEQMA4GA1UEBhMHVW5r\n"
            + "bm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UE\n"
            + "ChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCC\n"
            + "AbcwggEsBgcqhkjOOAQBMIIBHwKBgQD9f1OBHXUSKVLfSpwu7OTn9hG3UjzvRADD\n"
            + "Hj+AtlEmaUVdQCJR+1k9jVj6v8X1ujD2y5tVbNeBO4AdNG/yZmC3a5lQpaSfn+gE\n"
            + "exAiwk+7qdf+t8Yb+DtX58aophUPBPuD9tPFHsMCNVQTWhaRMvZ1864rYdcq7/Ii\n"
            + "Axmd0UgBxwIVAJdgUI8VIwvMspK5gqLrhAvwWBz1AoGBAPfhoIXWmz3ey7yrXDa4\n"
            + "V7l5lK+7+jrqgvlXTAs9B4JnUVlXjrrUWU/mcQcQgYC0SRZxI+hMKBYTt88JMozI\n"
            + "puE8FnqLVHyNKOCjrh4rs6Z1kW6jfwv6ITVi8ftiegEkO8yk8b6oUZCJqIPf4Vrl\n"
            + "nwaSi2ZegHtVJWQBTDv+z0kqA4GEAAKBgDJAY6FsVu1ibRVd4XIn/VLHJ1GJrFFK\n"
            + "d0I9u76h1KYbO+buEqmRIrnpUMXjErYwad+wc+Fe5/kGhhCKfEts3yNVwNvuLsNU\n"
            + "kVmdTC8vI3BqlyV2F+9Ekar2ogiqtE+BxNPHMEOGIXOJMjSMSWsOHaMOM2c29bXy\n"
            + "IdZr1ENwcPZloyEwHzAdBgNVHQ4EFgQUQa440XL4ulW3fLrOHq7uWiKo/UYwDQYJ\n"
            + "YIZIAWUDBAMCBQADLwAwLAIUbWlpb/M22woaHk/uCyscfbEJqv4CFFPG75R7jtvz\n" + "FllKUJXh+xxmMVfc\n"
            + "-----END CERTIFICATE-----";
    private final String CERTIFICATE_EC = "-----BEGIN CERTIFICATE-----\n"
            + "MIIB7zCCAZOgAwIBAgIEc21cijAMBggqhkjOPQQDAgUAMGwxEDAOBgNVBAYTB1Vu\n"
            + "a25vd24xEDAOBgNVBAgTB1Vua25vd24xEDAOBgNVBAcTB1Vua25vd24xEDAOBgNV\n"
            + "BAoTB1Vua25vd24xEDAOBgNVBAsTB1Vua25vd24xEDAOBgNVBAMTB1Vua25vd24w\n"
            + "HhcNMjEwNDIwMDkwNTA1WhcNMjEwNzE5MDkwNTA1WjBsMRAwDgYDVQQGEwdVbmtu\n"
            + "b3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQK\n"
            + "EwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMFkw\n"
            + "EwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEASCbfb0l60WEkKVAgKFvPslueJE2ppKQ\n"
            + "aa6AfQnGHnhGvKtRMVOMpy96aZPcYWdpX9323DMMPyhbosE/GjK5sqMhMB8wHQYD\n"
            + "VR0OBBYEFJ+MTQRX0X4ihcyt9h9+ODxCh/5LMAwGCCqGSM49BAMCBQADSAAwRQIh\n"
            + "AISr/AGgA2FwJeZFPKB2KEoWPCPsPMpBgA4KrsoJBQmVAiBAkLzQIUWad1cvyEUn\n" + "WNntICChHGdKmvPhWZSQ6n61ew==\n"
            + "-----END CERTIFICATE-----";

    @Test
    public void listKeystoresTest() throws KuraException, IOException, GeneralSecurityException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        when(ksMock.getKeyStore()).thenReturn(ks);

        KeystoreRestService krs = new KeystoreRestService() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put("MyKeystore", ksMock);
            }
        };
        krs.activate(null);

        List<KeystoreInfo> keystores = krs.listKeystores();

        List<String> types = Arrays.asList("jks", "pkcs12");
        assertEquals(1, keystores.size());
        assertTrue(types.contains(keystores.get(0).getType()));
        assertEquals(0, keystores.get(0).getSize());
    }

    @Test
    public void listKeysTest() throws KuraException, IOException, GeneralSecurityException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        ByteArrayInputStream is = new ByteArrayInputStream(this.CERTIFICATE_RSA.getBytes());
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        ks.setCertificateEntry("alias", cert);
        Map<String, Entry> certs = new HashMap<>();
        certs.put("alias", new KeyStore.TrustedCertificateEntry(cert));
        when(ksMock.getKeyStore()).thenReturn(ks);
        when(ksMock.getEntries()).thenReturn(certs);

        KeystoreRestService krs = new KeystoreRestService() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put("MyKeystore", ksMock);
            }
        };
        krs.activate(null);

        List<EntryInfo> keys = krs.getEntries(null, null);

        assertEquals(1, keys.size());
        assertTrue(keys.get(0) instanceof CertificateInfo);
        CertificateInfo result = (CertificateInfo) keys.get(0);
        assertEquals("alias", result.getAlias());
        assertEquals("MyKeystore", result.getKeystoreServicePid());
        assertEquals(EntryType.TRUSTED_CERTIFICATE, result.getType());
        assertEquals(2048, result.getSize());
        assertEquals("SHA256withRSA", result.getAlgorithm());
    }

    @Test
    public void listKeysTestWithId() throws KuraException, IOException, GeneralSecurityException, NoSuchFieldException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        ByteArrayInputStream is = new ByteArrayInputStream(this.CERTIFICATE_DSA.getBytes());
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        ks.setCertificateEntry("alias", cert);
        Map<String, Entry> certs = new HashMap<>();
        certs.put("alias", new KeyStore.TrustedCertificateEntry(cert));
        when(ksMock.getKeyStore()).thenReturn(ks);
        when(ksMock.getEntries()).thenReturn(certs);

        KeystoreRestService krs = new KeystoreRestService() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put("MyKeystore", ksMock);
            }
        };
        krs.activate(null);

        List<EntryInfo> keys = krs.getEntries("MyKeystore", null);

        assertEquals(1, keys.size());
        assertTrue(keys.get(0) instanceof CertificateInfo);
        CertificateInfo result = (CertificateInfo) keys.get(0);
        assertEquals("alias", result.getAlias());
        assertEquals("MyKeystore", result.getKeystoreServicePid());
        assertEquals(EntryType.TRUSTED_CERTIFICATE, result.getType());
        assertEquals(1024, result.getSize());
        assertEquals("SHA256withDSA", result.getAlgorithm());
        assertEquals(this.CERTIFICATE_DSA, result.getCertificate());
    }

    @Test
    public void listKeyTest() throws KuraException, IOException, GeneralSecurityException, NoSuchFieldException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        ByteArrayInputStream is = new ByteArrayInputStream(this.CERTIFICATE_EC.getBytes());
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        ks.setCertificateEntry("alias", cert);
        when(ksMock.getKeyStore()).thenReturn(ks);
        when(ksMock.getEntry("alias")).thenReturn(new KeyStore.TrustedCertificateEntry(cert));

        KeystoreRestService krs = new KeystoreRestService() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put("MyKeystore", ksMock);
            }
        };
        krs.activate(null);

        EntryInfo key = krs.getEntry("MyKeystore", "alias");

        assertTrue(key instanceof CertificateInfo);
        CertificateInfo result = (CertificateInfo) key;
        assertEquals("alias", result.getAlias());
        assertEquals("MyKeystore", result.getKeystoreServicePid());
        assertEquals(EntryType.TRUSTED_CERTIFICATE, result.getType());
        assertEquals(256, result.getSize());
        assertEquals("SHA256withECDSA", result.getAlgorithm());
        assertEquals(this.CERTIFICATE_EC, result.getCertificate());
    }

    @Test
    public void storeKeyEntryCertTest()
            throws KuraException, IOException, GeneralSecurityException, NoSuchFieldException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        when(ksMock.getKeyStore()).thenReturn(ks);

        KeystoreRestService krs = new KeystoreRestService() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put("MyKeystore", ksMock);
            }
        };
        krs.activate(null);

        TrustedCertificateWriteRequest writeRequest = new TrustedCertificateWriteRequest("MyKeystore", "MyAlias");
        TestUtil.setFieldValue(writeRequest, "certificate", this.CERTIFICATE_RSA);
        krs.storeTrustedCertificateEntry(writeRequest);

        verify(ksMock, times(1)).setEntry(eq("MyAlias"), any(TrustedCertificateEntry.class));

    }

    @Test
    public void deleteKeyEntryTest() throws KuraException, IOException, GeneralSecurityException, NoSuchFieldException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        when(ksMock.getKeyStore()).thenReturn(ks);

        KeystoreRestService krs = new KeystoreRestService() {

            @Override
            public void activate(ComponentContext componentContext) {

                this.keystoreServices.put("MyKeystore", ksMock);
            }
        };
        krs.activate(null);

        DeleteRequest deleteRequest = new DeleteRequest();
        TestUtil.setFieldValue(deleteRequest, "keystoreServicePid", "MyKeystore");
        TestUtil.setFieldValue(deleteRequest, "alias", "MyAlias");

        krs.deleteKeyEntry(deleteRequest);
        verify(ksMock).deleteEntry("MyAlias");
    }

    @Test
    public void createKeyPairTest() throws KuraException, IOException, GeneralSecurityException, NoSuchFieldException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        when(ksMock.getKeyStore()).thenReturn(ks);

        KeystoreRestService krs = new KeystoreRestService() {

            @Override
            public void activate(ComponentContext componentContext) {

                this.keystoreServices.put("MyKeystore", ksMock);
            }
        };
        krs.activate(null);

        KeyPairWriteRequest writeRequest = new KeyPairWriteRequest("MyKeystore", "MyAlias");
        TestUtil.setFieldValue(writeRequest, "algorithm", "RSA");
        TestUtil.setFieldValue(writeRequest, "signatureAlgorithm", "SHA256WithRSA");
        TestUtil.setFieldValue(writeRequest, "size", 1024);
        TestUtil.setFieldValue(writeRequest, "attributes", "CN=Kura, OU=IoT, O=Eclipse, C=US");

        krs.storeKeypairEntry(writeRequest);

        verify(ksMock, times(1)).createKeyPair("MyAlias", "RSA", 1024, "SHA256WithRSA",
                "CN=Kura, OU=IoT, O=Eclipse, C=US");
    }

    @Test
    public void getCSRTest() throws KuraException, IOException, GeneralSecurityException, NoSuchFieldException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        when(ksMock.getKeyStore()).thenReturn(ks);
        when(ksMock.getCSR(eq("MyAlias"), anyObject(), eq("SHA256WithRSA")))
                .thenReturn("-----BEGIN CERTIFICATE REQUEST-----");

        KeystoreRestService krs = new KeystoreRestService() {

            @Override
            public void activate(ComponentContext componentContext) {

                this.keystoreServices.put("MyKeystore", ksMock);
            }
        };
        krs.activate(null);

        KeyPairWriteRequest writeRequest = new KeyPairWriteRequest("MyKeystore", "MyAlias");
        TestUtil.setFieldValue(writeRequest, "algorithm", "RSA");
        TestUtil.setFieldValue(writeRequest, "signatureAlgorithm", "SHA256WithRSA");
        TestUtil.setFieldValue(writeRequest, "size", 1024);
        TestUtil.setFieldValue(writeRequest, "attributes", "CN=Kura, OU=IoT, O=Eclipse, C=US");

        krs.storeKeypairEntry(writeRequest);

        CsrReadRequest readRequest = new CsrReadRequest();
        TestUtil.setFieldValue(readRequest, "keystoreServicePid", "MyKeystore");
        TestUtil.setFieldValue(readRequest, "alias", "MyAlias");
        TestUtil.setFieldValue(readRequest, "signatureAlgorithm", "SHA256WithRSA");
        TestUtil.setFieldValue(readRequest, "attributes", "CN=Kura, OU=IoT, O=Eclipse, C=US");

        CsrResponse csr = krs.getCSR(readRequest);
        assertNotNull(csr);
        assertTrue(csr.getSigningRequest().startsWith("-----BEGIN CERTIFICATE REQUEST-----"));
    }
}
