/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Eurotech
 *******************************************************************************/

package org.eclipse.kura.core.keystore.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CertificateUtilTest {

    private static final String CERT_FILE_PATH = "target/test-classes/cert";

    private Object pemObject;
    private X509Certificate x509Certificate;
    private List<X509Certificate> x509certificates;
    private Set<TrustAnchor> trustAnchors;

    private String pemString = "";

    private Store<X509CertificateHolder> certHolderStore;

    private KeyPair keyPair;

    private X509Certificate[] certificateChain;

    @BeforeClass
    public static void initSecurity() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Before
    public void cleanUp() {
        this.pemObject = null;
        this.x509Certificate = null;
        this.x509certificates = null;
        this.trustAnchors = null;
        this.pemString = "";
        this.certHolderStore = null;
        this.certificateChain = null;
    }

    @Test
    public void shouldConvertPEMCertToX509Cert() {
        givenCertificateInPEMObject(CERT_FILE_PATH);

        whenIsConcertedToJavaX509();

        thenIsX509Certificate();
    }

    @Test
    public void shouldConvertCertificateToTrustAnchor() {
        givenCertificateInPEMObject(CERT_FILE_PATH);
        givenCertificateListWithSize(2);

        whenIsConvertedToTrustAnchor();

        thenTrusAnchorSetSizeIs(2);
    }

    @Test
    public void shouldConvertPEMStringtoCertHolderStore() {
        givenCertificateInPEMString(CERT_FILE_PATH);

        whenIsConvertedToX509CertificateHolderStore();

        thenStoreContainsCert();
    }

    @Test
    public void shouldConvertX509CertificatetoPemString() {
        givenCertificateInX509Format(CERT_FILE_PATH);

        whenIsConvertedToPemString();

        thenPemStringIsNotEmpty();
    }

    @Test
    public void shouldGenerateCertificate() {

        givenKeyPair("RSA", new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));

        whenCertificateChainIsGeneratedWith("SHA256WITHRSA", "CN=foo", getNotBeforeYesterday(), getNotAfterTomorrow());

        thenCertificainChainIsNotEmpty();
    }

    private void thenCertificainChainIsNotEmpty() {
        assertTrue(this.certificateChain.length > 0);
    }

    private void whenCertificateChainIsGeneratedWith(String signatureAlgorithm, String attributes, Date startDate,
            Date endDate) {
        try {
            this.certificateChain = CertificateUtil.generateCertificateChain(this.keyPair, signatureAlgorithm,
                    attributes, startDate, endDate);
        } catch (OperatorCreationException | CertificateException e) {
            e.printStackTrace();
            fail();
        }
    }

    private Date getNotBeforeYesterday() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);

        return calendar.getTime();
    }

    private Date getNotAfterTomorrow() {
        Calendar calendar = Calendar.getInstance();

        calendar.add(Calendar.DATE, +1);
        return calendar.getTime();
    }

    private void givenKeyPair(String algorithm, AlgorithmParameterSpec spec) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm, "BC");
            keyGen.initialize(spec, new SecureRandom());
            this.keyPair = keyGen.generateKeyPair();
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
            fail();
        }

    }

    private void thenPemStringIsNotEmpty() {
        assertNotEquals("", this.pemString);
    }

    private void whenIsConvertedToPemString() {
        try {
            this.pemString = CertificateUtil.x509CertificateToPem(x509Certificate);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    private void givenCertificateInX509Format(String certFilePath) {
        try (InputStream is = new FileInputStream(certFilePath);
                Reader r = new StringReader(IOUtils.toString(is, StandardCharsets.UTF_8));
                PEMParser pemParser = new PEMParser(r);) {
            this.pemObject = pemParser.readObject();
            this.x509Certificate = CertificateUtil.toJavaX509Certificate(this.pemObject);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

    }

    private void thenStoreContainsCert() {
        assertTrue(this.certHolderStore.getMatches(null).size() > 0);
    }

    private void whenIsConvertedToX509CertificateHolderStore() {
        try {
            this.certHolderStore = CertificateUtil.toX509CertificateHolderStore(this.pemString);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

    }

    private void thenTrusAnchorSetSizeIs(int size) {
        assertEquals(size, trustAnchors.size());
    }

    private void whenIsConvertedToTrustAnchor() {
        try {
            this.trustAnchors = CertificateUtil.toTrustAnchor(this.x509certificates);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private void givenCertificateListWithSize(int size) {
        this.x509certificates = new ArrayList<X509Certificate>();

        for (int i = 0; i < size; ++i) {
            try {
                this.x509certificates.add(CertificateUtil.toJavaX509Certificate(this.pemObject));
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }
    }

    private void thenIsX509Certificate() {
        assertTrue(this.x509Certificate instanceof java.security.cert.X509Certificate);
    }

    private void whenIsConcertedToJavaX509() {
        try {
            this.x509Certificate = CertificateUtil.toJavaX509Certificate(this.pemObject);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private void givenCertificateInPEMString(String filepath) {

        try (InputStream is = new FileInputStream(filepath)) {
            this.pemString = IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    private void givenCertificateInPEMObject(String filepath) {

        try (InputStream is = new FileInputStream(filepath);
                Reader r = new StringReader(IOUtils.toString(is, StandardCharsets.UTF_8));
                PEMParser pemParser = new PEMParser(r);) {
            this.pemObject = pemParser.readObject();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
