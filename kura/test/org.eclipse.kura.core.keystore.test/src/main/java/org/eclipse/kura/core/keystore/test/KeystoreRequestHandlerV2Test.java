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
        givenTestCertificateChain("leaf");

        whenCertificateChainIsUploaded("bar", "testalias");
        thenRequestSucceeds();
        thenKeystoreEntryEqualsSubmittedChain("bar", "testalias");
    }

    @Test
    public void shouldUploadPrivateKeyEntryWithMultipleCertificatesInChain() {
        givenKeystoreService("foo");
        givenTestCertificateChain("ca", "leaf");

        whenCertificateChainIsUploaded("foo", "testalias");
        thenRequestSucceeds();
        thenKeystoreEntryEqualsSubmittedChain("foo", "testalias");
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
    public void shouldRejectRequestObjectWithoutPrivateKey() {

        whenRequestIsPerformed(new MethodSpec("POST"), privateKeyRespurceURI,
                "{\"keystoreServicePid\":\"foo\",\"alias\":\"bar\",\"certificateChain\":[\"foo\"]}");
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

    private final Map<String, KeystoreService> createdKeystoreServices = new HashMap<>();

    private List<String> certificateChain = new ArrayList<>();
    private String leafKeyPem;
    private final String privateKeyRespurceURI;

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

    private void givenTestCertificateChain(final String leafCN) {
        try {
            final TestCA testCA = new TestCA(
                    CertificateCreationOptions.builder(new X500Name("cn=foo, dc=bar.com")).build());

            final KeyPair keyPair = TestCA.generateKeyPair();

            this.leafKeyPem = privateKeyToPEMString(keyPair.getPrivate());
            final X509Certificate leafCert = testCA.createAndSignCertificate(
                    CertificateCreationOptions.builder(new X500Name("cn=" + leafCN + ", dc=bar.com")).build(), keyPair);

            certificateChain.add(certificateToPEMString(leafCert));
        } catch (Exception e) {
            fail("cannot cerate test certificate chain");
        }
    }

    private void givenTestCertificateChain(final String caCN, final String leafCN) {
        try {
            final TestCA testCA = new TestCA(
                    CertificateCreationOptions.builder(new X500Name("cn=" + caCN + ", dc=bar.com")).build());

            final KeyPair keyPair = TestCA.generateKeyPair();

            this.leafKeyPem = privateKeyToPEMString(keyPair.getPrivate());
            final X509Certificate leafCert = testCA.createAndSignCertificate(
                    CertificateCreationOptions.builder(new X500Name("cn=" + leafCN + ", dc=bar.com")).build(), keyPair);

            certificateChain.add(certificateToPEMString(leafCert));
            certificateChain.add(certificateToPEMString(testCA.getCertificate()));
        } catch (Exception e) {
            fail("cannot cerate test certificate chain");
        }
    }

    private void whenCertificateChainIsUploaded(final String keystorePid, final String alias) {
        final JsonObject object = Json.object();
        object.add("privateKey", this.leafKeyPem);

        final JsonArray chain = Json.array();
        for (final String cert : certificateChain) {
            chain.add(cert);
        }

        object.add("certificateChain", chain);
        object.add("keystoreServicePid", keystorePid);
        object.add("alias", alias);

        whenRequestIsPerformed(new MethodSpec("POST"), privateKeyRespurceURI,
                object.toString());
    }

    private void thenKeystoreEntryEqualsSubmittedChain(final String pid, final String alias) {
        try {
            final PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) this.createdKeystoreServices.get(pid)
                    .getEntry(alias);

            assertEquals(this.leafKeyPem, privateKeyToPEMString(privateKeyEntry.getPrivateKey()));

            final List<String> entryCertificatesAsPem = new ArrayList<>();

            for (final Certificate cert : privateKeyEntry.getCertificateChain()) {
                entryCertificatesAsPem.add(certificateToPEMString((X509Certificate) cert));
            }

            assertEquals(this.certificateChain, entryCertificatesAsPem);
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
