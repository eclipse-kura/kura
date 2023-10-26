/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.asn1.x500.X500Name;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.testutil.pki.TestCA;
import org.eclipse.kura.core.testutil.pki.TestCA.CertificateCreationOptions;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

@RunWith(Parameterized.class)
public class KeystoreRequestHandlerV2Test extends AbstractRequestHandlerTest {

    @Test
    public void shouldUploadSimplePrivateKeyEntry() {
        givenKeystoreService("bar");
        givenKeyPair("leaf");

        whenKeyPairIsUploaded("bar", "testalias", true);
        thenRequestSucceeds();
        thenKeystoreEntryEqualsCurrentKeyPair("bar", "testalias");
    }

    @Test
    public void shouldUploadPrivateKeyEntryWithMultipleCertificatesInChain() {
        givenKeystoreService("foo");
        givenKeyPair("ca", "leaf");

        whenKeyPairIsUploaded("foo", "testalias", true);
        thenRequestSucceeds();
        thenKeystoreEntryEqualsCurrentKeyPair("foo", "testalias");
    }

    @Test
    public void shouldUpdateSimplePrivateKeyEntry() {
        givenKeystoreService("bar");
        givenKeyPairInKeystore("bar", "testalias", "leaf");
        givenNewLeafCert("otherleaf");

        whenKeyPairIsUploaded("bar", "testalias", false);
        thenRequestSucceeds();
        thenKeystoreEntryEqualsCurrentKeyPair("bar", "testalias");
    }

    @Test
    public void shouldUpdatePrivateKeyEntryWithMultipleCertificatesInChain() {
        givenKeystoreService("foo");
        givenKeyPairInKeystore("foo", "testalias", "ca", "leaf");
        givenNewLeafCert("otherleaf");

        whenKeyPairIsUploaded("foo", "testalias", false);
        thenRequestSucceeds();
        thenKeystoreEntryEqualsCurrentKeyPair("foo", "testalias");
    }

    @Test
    public void shouldRejectEmpryRequestObject() {

        whenRequestIsPerformed(new MethodSpec("POST"), privateKeyRespurceURI, "{}");
        thenResponseCodeIs(400);
    }

    @Test
    public void shouldRejectRequestObjectWithoutPid() {

        whenRequestIsPerformed(new MethodSpec("POST"), privateKeyRespurceURI,
                "{\"alias\":\"foo\",\"privateKey\":\"bar\",\"certificateChain\":[\"foo\"]}");
        thenResponseCodeIs(400);
    }

    @Test
    public void shouldRejectRequestObjectWithoutAlias() {

        whenRequestIsPerformed(new MethodSpec("POST"), privateKeyRespurceURI,
                "{\"keystoreServicePid\":\"foo\",\"privateKey\":\"bar\",\"certificateChain\":[\"foo\"]}");
        thenResponseCodeIs(400);
    }

    @Test
    public void shouldRejectRequestObjectWithoutCertificateChain() {

        whenRequestIsPerformed(new MethodSpec("POST"), privateKeyRespurceURI,
                "{\"keystoreServicePid\":\"foo\",\"alias\":\"bar\",\"privateKey\":\"bar\"}");
        thenResponseCodeIs(400);
    }

    @Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
                new Object[] { new RestTransport("keystores/v2"), "/entries/privatekey" },
                new Object[] { new MqttTransport("KEYS-V2"), "/keystores/entries/privatekey" });
    }

    public KeystoreRequestHandlerV2Test(final Transport transport, final String privateKeyRespurceURI) {
        super(transport);
        this.privateKeyRespurceURI = privateKeyRespurceURI;
    }

    private final String privateKeyRespurceURI;
    private final Map<String, KeystoreService> createdKeystoreServices = new HashMap<>();

    private KeyPair leafKeyPair;
    private String leafKeyPem;
    private List<String> certificateChainPem = new ArrayList<>();

    private PrivateKey leafKey;
    private List<X509Certificate> certificateChain = new ArrayList<>();

    private TestCA testCA;

    private void givenKeystoreService(final String pid) {
        try {
            final Path dir = Files.createTempDirectory(null);
            final String keystorePath = dir.toFile().getAbsolutePath() + "/" + System.currentTimeMillis() + ".ks";

            final ConfigurationService configurationService = ServiceUtil
                    .trackService(ConfigurationService.class, Optional.empty()).get(30, TimeUnit.SECONDS);

            final KeystoreService keystoreService = ServiceUtil
                    .createFactoryConfiguration(configurationService, KeystoreService.class, pid,
                            "org.eclipse.kura.core.keystore.FilesystemKeystoreServiceImpl",
                            Collections.singletonMap("keystore.path", keystorePath))
                    .get(30, TimeUnit.SECONDS);

            this.createdKeystoreServices.put(pid, keystoreService);

        } catch (final Exception e) {
            fail("failed to create test keystore");
        }
    }

    private void givenKeyPair(final String leafCN) {
        givenKeyPair("foo", leafCN, false);
    }

    private void givenKeyPairInKeystore(final String keystorePid, final String alias, final String leafCN) {
        givenKeyPair(leafCN);

        storeCurrentKeyPair(keystorePid, alias);
    }

    private void givenKeyPair(final String caCN, final String leafCN) {
        givenKeyPair(caCN, leafCN, true);
    }

    private void givenKeyPairInKeystore(final String keystorePid, final String alias, final String caCN,
            final String leafCN) {
        givenKeyPair(caCN, leafCN);

        storeCurrentKeyPair(keystorePid, alias);
    }

    private void givenKeyPair(final String caCN, final String leafCN, final boolean includeCA) {
        try {
            this.testCA = new TestCA(
                    CertificateCreationOptions.builder(new X500Name("cn=" + caCN + ", dc=bar.com")).build());

            this.leafKeyPair = TestCA.generateKeyPair();

            this.leafKey = this.leafKeyPair.getPrivate();
            this.leafKeyPem = privateKeyToPEMString(this.leafKeyPair.getPrivate());

            final X509Certificate leafCert = testCA.createAndSignCertificate(
                    CertificateCreationOptions.builder(new X500Name("cn=" + leafCN + ", dc=bar.com")).build(),
                    this.leafKeyPair);

            this.certificateChain.clear();
            this.certificateChainPem.clear();
            this.certificateChain.add(leafCert);
            this.certificateChainPem.add(certificateToPEMString(leafCert));

            if (includeCA) {
                this.certificateChain.add(testCA.getCertificate());
                this.certificateChainPem.add(certificateToPEMString(testCA.getCertificate()));
            }

        } catch (Exception e) {
            fail("cannot cerate test certificate chain");
        }
    }

    private void givenNewLeafCert(final String leafCN) {
        try {
            final X509Certificate leafCert = this.testCA.createAndSignCertificate(
                    CertificateCreationOptions.builder(new X500Name("cn=" + leafCN + ", dc=bar.com")).build(),
                    this.leafKeyPair);

            this.certificateChain.set(0, leafCert);
            this.certificateChainPem.set(0, certificateToPEMString(leafCert));
        } catch (Exception e) {
            fail("cannot cerate new certificate");
        }
    }

    private void storeCurrentKeyPair(final String keystorePid, final String alias) {
        try {
            this.createdKeystoreServices.get(keystorePid).setEntry(alias,
                    new PrivateKeyEntry(this.leafKey, this.certificateChain.toArray(new X509Certificate[0])));
        } catch (final Exception e) {
            fail("cannot store certificate chain");
        }
    }

    private void whenKeyPairIsUploaded(final String keystorePid, final String alias,
            final boolean includePrivateKey) {
        final JsonObject object = Json.object();
        if (includePrivateKey) {
            object.add("privateKey", this.leafKeyPem);
        }

        final JsonArray chain = Json.array();
        for (final String cert : certificateChainPem) {
            chain.add(cert);
        }

        object.add("certificateChain", chain);
        object.add("keystoreServicePid", keystorePid);
        object.add("alias", alias);

        whenRequestIsPerformed(new MethodSpec("POST"), privateKeyRespurceURI,
                object.toString());
    }

    private void thenKeystoreEntryEqualsCurrentKeyPair(final String pid, final String alias) {
        try {
            final PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) this.createdKeystoreServices.get(pid)
                    .getEntry(alias);

            assertEquals(this.leafKeyPem, privateKeyToPEMString(privateKeyEntry.getPrivateKey()));

            final List<String> entryCertificatesAsPem = new ArrayList<>();

            for (final Certificate cert : privateKeyEntry.getCertificateChain()) {
                entryCertificatesAsPem.add(certificateToPEMString((X509Certificate) cert));
            }

            assertEquals(this.certificateChainPem, entryCertificatesAsPem);
        } catch (KuraException | IOException e) {
            fail("Unable to retrieve keystore entry");
        }
    }

    @After
    public void cleanupKeystoreServices() {
        try {
            final ConfigurationService configurationService = ServiceUtil
                    .trackService(ConfigurationService.class, Optional.empty()).get(30, TimeUnit.SECONDS);

            for (final String pid : createdKeystoreServices.keySet()) {
                ServiceUtil.deleteFactoryConfiguration(configurationService, pid).get(30, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            fail("failed to delete test keystore");
        }
    }

    private String certificateToPEMString(final X509Certificate cert) throws IOException {
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            TestCA.encodeToPEM(cert, out);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String privateKeyToPEMString(final PrivateKey key) throws IOException {
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            TestCA.encodeToPEM(key, out);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
