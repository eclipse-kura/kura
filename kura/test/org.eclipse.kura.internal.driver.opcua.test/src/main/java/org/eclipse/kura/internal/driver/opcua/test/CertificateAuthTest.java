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
 ******************************************************************************/
package org.eclipse.kura.internal.driver.opcua.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.driver.Driver;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfigBuilder;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.CertificateManager;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.DefaultTrustListManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.security.TrustListManager;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateBuilder;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;
import org.eclipse.milo.opcua.stack.server.security.DefaultServerCertificateValidator;
import org.eclipse.milo.opcua.stack.server.security.ServerCertificateValidator;
import org.junit.After;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertificateAuthTest {

    @Test
    public void shoudlSupportBasic256Sha256() throws Exception {
        givenServerSecurityMode(MessageSecurityMode.SignAndEncrypt);
        givenServerSecurityPolicy(SecurityPolicy.Basic256Sha256);
        givenDriverKeyStore();
        givenServerKeyStore();
        givenServerTrustingClientCert();
        givenDriverTrustingServerCert();

        givenOpcUaDriver();
        givenServer();

        whenDriverIsConnected();

        thenNoExceptionIsTrown();
    }

    @Test
    public void connectionShouldFailIfServerCertDoesNotHaveCorrectApplicationUri() throws Exception {
        givenServerSecurityMode(MessageSecurityMode.SignAndEncrypt);
        givenServerSecurityPolicy(SecurityPolicy.Basic256Sha256);
        givenDriverKeyStore();
        givenServerKeyStore("urn:kura:opcua:wronguri");
        givenServerTrustingClientCert();
        givenDriverTrustingServerCert();

        givenOpcUaDriver();
        givenServer();

        whenDriverIsConnected();

        thenExceptionIsTrown();
    }

    @Test
    public void connectionShouldSucceedWithWrongServerApplicationUriAndClientCeckDisabled() throws Exception {
        givenServerSecurityMode(MessageSecurityMode.SignAndEncrypt);
        givenServerSecurityPolicy(SecurityPolicy.Basic256Sha256);
        givenDriverKeyStore();
        givenServerKeyStore("urn:kura:opcua:wronguri");
        givenDriverServerAuthenticationEnabled(false);
        givenServerTrustingClientCert();
        givenDriverTrustingServerCert();

        givenOpcUaDriver();
        givenServer();

        whenDriverIsConnected();

        thenNoExceptionIsTrown();
    }

    @Test
    public void connectionShouldFailIfDriverDoesNotTrustServerCert() throws Exception {
        givenServerSecurityMode(MessageSecurityMode.SignAndEncrypt);
        givenServerSecurityPolicy(SecurityPolicy.Basic256Sha256);
        givenDriverKeyStore();
        givenServerKeyStore();
        givenServerTrustingClientCert();

        givenOpcUaDriver();
        givenServer();

        whenDriverIsConnected();

        thenExceptionIsTrown();
    }

    private static final String TEST_CLIENT_KEYSTORE_PASSWORD = "changeit";
    private static final String TEST_CLIENT_KEYSTORE_ALIAS = "client-cert";

    private static final Logger logger = LoggerFactory.getLogger(CertificateAuthTest.class);
    private static final AtomicInteger NEXT_PORT = new AtomicInteger(1400);

    private final Path testRoot;
    private final Path serverTrustManagerRoot;
    private final Path clientKeystorePath;
    private final TrustListManager serverTrustListManager;
    private final String driverPid;
    private Map<String, Object> driverConfiguration;

    private OpcUaServer server;
    private int currentPort;
    private String serverApplicationUri = "urn:kura:opcua:testserver";
    private String clientApplicationUri = "urn:kura:opcua:testclient";
    private SecurityPolicy serverSecurityPolicy;
    private MessageSecurityMode serverSecurityMode;
    private KeyPair serverKeyPair;
    private X509Certificate serverCertificate;
    private X509Certificate clientCertificate;
    private Driver driver;
    private Optional<Throwable> exception;

    public CertificateAuthTest()
            throws IOException, KuraException, InterruptedException, ExecutionException, TimeoutException {
        this.currentPort = NEXT_PORT.incrementAndGet();
        this.driverPid = "testDriver" + this.currentPort;
        this.testRoot = Files.createTempDirectory(null);
        this.serverTrustManagerRoot = new File(testRoot.toFile(), "server").toPath();
        this.clientKeystorePath = new File(testRoot.toFile(), "client.ks").toPath();
        this.serverTrustListManager = new DefaultTrustListManager(serverTrustManagerRoot.toFile());
        this.driverConfiguration = initialDriverConfiguration(trackService(CryptoService.class));
    }

    @After
    public void cleanup() throws KuraException, InterruptedException, ExecutionException, TimeoutException {
        server.shutdown();
        trackService(ConfigurationService.class).deleteFactoryConfiguration(driverPid, false);
    }

    private void givenServer() throws UaException, IOException {
        final ServerCertificateValidator certificateValidator = new DefaultServerCertificateValidator(
                serverTrustListManager);
        final CertificateManager certificateManager = new DefaultCertificateManager(serverKeyPair, serverCertificate);

        final EndpointConfiguration securedEndpoint = EndpointConfiguration.newBuilder()
                .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
                .setBindAddress("localhost")
                .setHostname("localhost")
                .setPath("/opcsvr")
                .setBindPort(currentPort)
                .addTokenPolicy(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS)
                .setSecurityPolicy(serverSecurityPolicy)
                .setSecurityMode(serverSecurityMode)
                .setCertificate(() -> this.serverCertificate)
                .build();

        final EndpointConfiguration discoveryEndpoint = EndpointConfiguration.newBuilder()
                .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
                .setBindAddress("localhost")
                .setHostname("localhost")
                .setPath("/opcsvr")
                .setBindPort(currentPort)
                .addTokenPolicy(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS)
                .setSecurityPolicy(SecurityPolicy.None)
                .setSecurityMode(MessageSecurityMode.None)
                .build();

        logger.info("starting test server with endpoint {}", securedEndpoint.getEndpointUrl());

        OpcUaServerConfig config = new OpcUaServerConfigBuilder().setApplicationUri(serverApplicationUri)
                .setApplicationName(LocalizedText.english("opcsvr")).setCertificateValidator(certificateValidator)
                .setCertificateManager(certificateManager)
                .setEndpoints(Arrays.asList(securedEndpoint, discoveryEndpoint).stream().collect(Collectors.toSet()))
                .build();

        server = new OpcUaServer(config);
        final TestNamespace testNamespace = new TestNamespace(server);
        server.getAddressSpaceManager().register(testNamespace);
        server.startup();
    }

    private void givenServerSecurityPolicy(final SecurityPolicy securityPolicy) {
        this.serverSecurityPolicy = securityPolicy;
    }

    private void givenServerSecurityMode(final MessageSecurityMode securityMode) {
        this.serverSecurityMode = securityMode;
    }

    private void givenDriverSecurityPolicy(final int securityPolicy) {
        driverConfiguration.put("security.policy", securityPolicy);
    }

    private void givenDriverApplicationUri(final String applicationUri) {
        driverConfiguration.put("application.uri", applicationUri);
    }

    private void givenOpcUaDriver()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        final ConfigurationService configurationService = trackService(ConfigurationService.class);

        driver = ServiceUtil
                .createFactoryConfiguration(configurationService, Driver.class, driverPid,
                        "org.eclipse.kura.driver.opcua", Collections.emptyMap())
                .get(30, TimeUnit.SECONDS);

        ServiceUtil.updateComponentConfiguration(configurationService, driverPid, driverConfiguration).get(30,
                TimeUnit.SECONDS);
    }

    private void givenServerKeyStore() throws Exception {
        givenServerKeyStore(this.serverApplicationUri);
    }

    private void givenServerKeyStore(final String applicationUri) throws Exception {
        serverKeyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);

        final SelfSignedCertificateBuilder builder = new SelfSignedCertificateBuilder(serverKeyPair,
                testSelfSignedCertificateGenerator())
                        .setCommonName("Test server " + currentPort)
                        .setOrganization("Eclipse")
                        .setOrganizationalUnit("dev")
                        .setApplicationUri(applicationUri);

        serverCertificate = builder.build();
    }

    private void givenDriverKeyStore() throws Exception {
        final KeyPair clientKeyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);

        final SelfSignedCertificateBuilder builder = new SelfSignedCertificateBuilder(clientKeyPair,
                testSelfSignedCertificateGenerator())
                        .setCommonName("Test client " + currentPort)
                        .setOrganization("Eclipse")
                        .setOrganizationalUnit("dev")
                        .setApplicationUri(clientApplicationUri);

        clientCertificate = builder.build();

        final KeyStore store = KeyStore.getInstance("JKS");
        store.load(null);

        store.setEntry(TEST_CLIENT_KEYSTORE_ALIAS,
                new KeyStore.PrivateKeyEntry(clientKeyPair.getPrivate(), new Certificate[] { clientCertificate }),
                new KeyStore.PasswordProtection(TEST_CLIENT_KEYSTORE_PASSWORD.toCharArray()));

        try (final FileOutputStream out = new FileOutputStream(clientKeystorePath.toFile())) {
            store.store(out, TEST_CLIENT_KEYSTORE_PASSWORD.toCharArray());
        }
    }

    private void givenDriverTrustingServerCert() throws KeyStoreException, FileNotFoundException, IOException,
            NoSuchAlgorithmException, CertificateException {

        final KeyStore store = KeyStore.getInstance("JKS");

        try (final FileInputStream in = new FileInputStream(clientKeystorePath.toFile())) {
            store.load(in, TEST_CLIENT_KEYSTORE_PASSWORD.toCharArray());
        }

        store.setEntry("server-cert", new KeyStore.TrustedCertificateEntry(serverCertificate), null);

        try (final FileOutputStream out = new FileOutputStream(clientKeystorePath.toFile())) {
            store.store(out, TEST_CLIENT_KEYSTORE_PASSWORD.toCharArray());
        }
    }

    private void givenServerTrustingClientCert() {
        this.serverTrustListManager.addTrustedCertificate(clientCertificate);
    }

    private void givenDriverServerAuthenticationEnabled(final boolean enabled) {
        driverConfiguration.put("authenticate.server", enabled);
    }

    private void whenDriverIsConnected() {
        try {
            this.driver.connect();
            this.exception = Optional.empty();
        } catch (final Exception e) {
            this.exception = Optional.of(e);
        }
    }

    private void thenNoExceptionIsTrown() {
        if (exception.isPresent()) {
            logger.warn("test failed due to exception", this.exception.get());
        }

        assertEquals(Optional.empty(), this.exception);
    }

    private void thenExceptionIsTrown() {
        assertTrue(exception.isPresent());
    }

    private Map<String, Object> initialDriverConfiguration(final CryptoService cryptoService) throws KuraException {

        final Map<String, Object> driverConfiguration = new HashMap<>();

        driverConfiguration.put("endpoint.ip", "localhost");
        driverConfiguration.put("endpoint.port", currentPort);
        driverConfiguration.put("server.name", "opcsvr");

        driverConfiguration.put("request.timeout", 1500);
        driverConfiguration.put("session.timeout", 2000);

        driverConfiguration.put("keystore.password",
                new String(cryptoService.encryptAes(TEST_CLIENT_KEYSTORE_PASSWORD.toCharArray())));
        driverConfiguration.put("certificate.location", clientKeystorePath.toFile().getAbsolutePath());
        driverConfiguration.put("keystore.type", "JKS");
        driverConfiguration.put("keystore.client.alias", TEST_CLIENT_KEYSTORE_ALIAS);

        driverConfiguration.put("security.policy", 3);
        driverConfiguration.put("application.uri", clientApplicationUri);

        return driverConfiguration;
    }

    private SelfSignedCertificateGenerator testSelfSignedCertificateGenerator() {
        return new SelfSignedCertificateGenerator() {
            protected void addBasicConstraints(
                    org.bouncycastle.cert.X509v3CertificateBuilder certificateBuilder,
                    org.bouncycastle.asn1.x509.BasicConstraints basicConstraints)
                    throws org.bouncycastle.cert.CertIOException {
            }
        };
    }

    private <T> T trackService(final Class<T> classz)
            throws InterruptedException, ExecutionException, TimeoutException {
        return ServiceUtil
                .trackService(classz, Optional.empty()).get(30, TimeUnit.SECONDS);
    }
}
