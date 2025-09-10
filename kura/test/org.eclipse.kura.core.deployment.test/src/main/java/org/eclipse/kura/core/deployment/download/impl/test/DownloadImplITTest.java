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
 *******************************************************************************/
package org.eclipse.kura.core.deployment.download.impl.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bouncycastle.asn1.x500.X500Name;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.deployment.download.impl.DownloadImpl;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.ssl.SslManagerService;

import org.eclipse.kura.core.testutil.pki.TestCA;
import org.eclipse.kura.core.testutil.pki.TestCA.CertificateCreationOptions;
import org.eclipse.kura.core.testutil.pki.TestCA.TestCAException;
import org.eclipse.kura.core.testutil.service.ServiceUtil;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class DownloadImplITTest {

    private static final String TEST_FILENAME = "test";
    private static final String DP_EXTENSION = "dp";
    private static final String DP_VERSION = "0.0.0";

    private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";

    private static final CountDownLatch dependencies = new CountDownLatch(5);

    private static final String SERVER_KEYSTORE_PID = "serverKeystore";
    private static final String CLIENT_KEYSTORE_PID = "clientKeystore";
    private static final String HTTP_SERVER_MANAGER_PID = "org.eclipse.kura.http.server.manager.HttpService";
    private static final String REST_SERVICE_PID = "org.eclipse.kura.internal.rest.provider.RestService";
    private static final String SSL_MANAGER_SERVICE_PID = "org.eclipse.kura.ssl.SslManagerService";

    private static final int HTTP_PORT = 8080;
    private static final int HTTPS_PORT = 9993;

    private static SslManagerService sslManagerService;
    private static ConfigurationService configurationService;
    @SuppressWarnings("unused")
    private static ConfigurableComponent httpServerManager;
    private static CryptoService cryptoService;
    @SuppressWarnings("unused")
    private static ConfigurableComponent restService;

    private static TestKeystore serverKeystore;

    private static TestKeystore clientKeystore;

    private DownloadImpl downloadImpl;

    private DeploymentPackageDownloadOptions deploymentPackageDownloadOptions;

    private Exception occurredException;

    private String tempDir = System.getProperty("java.io.tmpdir");

    public void setSslManagerService(SslManagerService sslManagerService) {
        DownloadImplITTest.sslManagerService = sslManagerService;
        dependencies.countDown();
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        DownloadImplITTest.configurationService = configurationService;
        dependencies.countDown();
    }

    public void setHttpServerManager(ConfigurableComponent httpServerManager) {
        DownloadImplITTest.httpServerManager = httpServerManager;
        dependencies.countDown();
    }

    public void setCryptoService(CryptoService cryptoService) {
        DownloadImplITTest.cryptoService = cryptoService;
        dependencies.countDown();
    }

    public void setRestService(ConfigurableComponent restService) {
        DownloadImplITTest.restService = restService;
        dependencies.countDown();
    }

    @BeforeClass
    public static void setupEnvironment() throws InterruptedException, TestCAException, IOException {
        awaitDependencies();
        setupCAAndClientKeystore();
        setupSslManagerService();
        setupHttpServerManager();
        setupRestService();
    }

    @Before
    public void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get(this.tempDir, TEST_FILENAME));
    }

    @Test
    public void shouldDownloadFileFromHttpsUrlWithoutHostnameVerificationEnabled() {
        givenDeploymentPackageDownloadOptions("https://localhost:9993/services/test/download", TEST_FILENAME,
                DP_VERSION);
        givenDownloadImpl();
        givenHostnameVerificationInSslManagerService(false);

        whenDownload();

        thenNoExceptionOccured();
        thenDownloadedFileIs(TEST_FILENAME + "_" + DP_VERSION + "." + DP_EXTENSION);
    }

    @Test
    public void shouldNotDownloadFileFromHttpsUrlWithHostnameVerificationEnabled() {
        givenDeploymentPackageDownloadOptions("https://localhost:9993/services/test/download", TEST_FILENAME,
                DP_VERSION);
        givenDownloadImpl();
        givenHostnameVerificationInSslManagerService(true);

        whenDownload();

        thenNoExceptionOccured();
        thenFileIsEmpty(TEST_FILENAME + "_" + DP_VERSION + "." + DP_EXTENSION);
    }

    private void givenDeploymentPackageDownloadOptions(String deployUri, String dpName, String dpVersion) {
        this.deploymentPackageDownloadOptions = new DeploymentPackageDownloadOptions(deployUri, dpName, dpVersion);
        this.deploymentPackageDownloadOptions.setJobId(123l);
        this.deploymentPackageDownloadOptions.setDownloadProtocol("HTTP");
        this.deploymentPackageDownloadOptions.setDownloadDirectory(tempDir);
    }

    private void givenDownloadImpl() {
        this.downloadImpl = new DownloadImpl(this.deploymentPackageDownloadOptions,
                mock(CloudDeploymentHandlerV2.class));
        this.downloadImpl.setSslManager(sslManagerService);
    }

    private void givenHostnameVerificationInSslManagerService(Boolean hostnameVerification) {
        Map<String, Object> props = new HashMap<>();
        props.put("ssl.hostname.verification", hostnameVerification);
        try {
            updateComponentConfiguration(configurationService, SSL_MANAGER_SERVICE_PID, props).get(30,
                    TimeUnit.SECONDS);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private void whenDownload() {
        try {
            this.downloadImpl.downloadDeploymentPackageInternal();
        } catch (KuraException e) {
            occurredException = e;
        }
    }

    private void thenNoExceptionOccured() {
        assertNull(this.occurredException);
    }

    private void thenFileIsEmpty(String filename) {
        Path tempFile = Paths.get(this.tempDir, filename);
        try {
            assertTrue(Files.readAllBytes(tempFile).length == 0);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private void thenDownloadedFileIs(String filename) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(TEST_FILENAME)) {
            int numberOfBytes;
            byte[] data = new byte[4096];

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            while ((numberOfBytes = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, numberOfBytes);
            }
            buffer.flush();
            byte[] expectedContent = buffer.toByteArray();
            byte[] downloadedContent = Files.readAllBytes(Paths.get(this.tempDir, filename));
            assertArrayEquals(expectedContent, downloadedContent);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private static void setupCAAndClientKeystore() throws TestCAException, IOException {

        final TestCA serverCA = new TestCA(
                CertificateCreationOptions.builder(new X500Name("cn=Server CA, dn=foo.org")).build());

        final KeyPair serverKeyPair = TestCA.generateKeyPair();

        X509Certificate serverCertificate = serverCA.createAndSignCertificate(
                CertificateCreationOptions.builder(new X500Name("cn=Server Cert, dn=foo.org")).build(), serverKeyPair);

        File serverKeystoreFile = TestCA.writeKeystore(new KeyStore.PrivateKeyEntry(serverKeyPair.getPrivate(),
                new Certificate[] { serverCertificate, serverCA.getCertificate() }));

        File clientKeystoreFile = TestCA.writeKeystore(new KeyStore.TrustedCertificateEntry(serverCertificate));

        try {
            serverKeystore = new TestKeystore(DownloadImplITTest.configurationService, DownloadImplITTest.cryptoService,
                    SERVER_KEYSTORE_PID, serverKeystoreFile.getAbsolutePath(), DEFAULT_KEYSTORE_PASSWORD);
            clientKeystore = new TestKeystore(DownloadImplITTest.configurationService, DownloadImplITTest.cryptoService,
                    CLIENT_KEYSTORE_PID, clientKeystoreFile.getAbsolutePath(), DEFAULT_KEYSTORE_PASSWORD);
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    private static void setupSslManagerService() {
        Map<String, Object> props = new HashMap<>();
        props.put("KeystoreService.target", clientKeystore.getTargetFilter());
        props.put("TruststoreKeystoreService.target", clientKeystore.getTargetFilter());
        try {
            updateComponentConfiguration(configurationService, SSL_MANAGER_SERVICE_PID, props).get(30,
                    TimeUnit.SECONDS);
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    private static void setupHttpServerManager() {

        Map<String, Object> props = new HashMap<>();
        props.put("https.ports", new Integer[] { HTTPS_PORT });
        props.put("KeystoreService.target", serverKeystore.getTargetFilter());

        try {
            updateComponentConfiguration(DownloadImplITTest.configurationService, HTTP_SERVER_MANAGER_PID, props) //
                    .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    private static void setupRestService() {
        Map<String, Object> props = new HashMap<>();
        props.put("allowed.ports", new Integer[] { HTTP_PORT, HTTPS_PORT });
        try {
            updateComponentConfiguration(DownloadImplITTest.configurationService, REST_SERVICE_PID, props) //
                    .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    private static void awaitDependencies() throws InterruptedException {
        if (!dependencies.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("dependencies not resolved in 30 seconds");
        }
    }

    private static class TestKeystore implements AutoCloseable {

        private static final String FILESYSTEM_KEYSTORE_SERVICE_PID = "org.eclipse.kura.core.keystore.FilesystemKeystoreServiceImpl";

        private final ConfigurationService configSvc;
        private final String pid;

        public TestKeystore(final ConfigurationService configurationService, final CryptoService cryptoService,
                final String pid, final String keystorePath, String keystorePassword)
                throws InterruptedException, ExecutionException, TimeoutException, KuraException {
            this.configSvc = configurationService;
            this.pid = pid;

            Map<String, Object> props = new HashMap<>();

            props.put("keystore.path", keystorePath);
            props.put("keystore.password", new String(cryptoService.encryptAes(keystorePassword.toCharArray())));

            ServiceUtil.createFactoryConfiguration(configurationService, ConfigurableComponent.class, pid,
                    FILESYSTEM_KEYSTORE_SERVICE_PID, props).get(30, TimeUnit.SECONDS);
        }

        String getTargetFilter() {
            return "(kura.service.pid=" + pid + ")";
        }

        @Override
        public void close() throws Exception {
            ServiceUtil.deleteFactoryConfiguration(configSvc, pid).get(30, TimeUnit.SECONDS);
        }

    }

    static CompletableFuture<Void> updateComponentConfiguration(final ConfigurationService configurationService,
            final String pid, final Map<String, Object> properties) throws InvalidSyntaxException {

        final CompletableFuture<Void> result = new CompletableFuture<>();
        final BundleContext context = FrameworkUtil.getBundle(DownloadImpl.class).getBundleContext();

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        final ServiceTracker<?, ?> tracker = new ServiceTracker<>(context,
                FrameworkUtil.createFilter("(kura.service.pid=" + pid + ")"),
                new ServiceTrackerCustomizer<Object, Object>() {

                    Optional<ScheduledFuture<?>> task = Optional.empty();

                    @Override
                    public Object addingService(ServiceReference<Object> reference) {

                        task = Optional.of(executor.schedule(() -> {
                            try {
                                configurationService.updateConfiguration(pid, properties);
                            } catch (KuraException e) {
                                throw new RuntimeException(e);
                            }
                        }, 5, TimeUnit.SECONDS));

                        return context.getService(reference);
                    }

                    @Override
                    public void modifiedService(ServiceReference<Object> reference, Object service) {
                        result.complete(null);
                    }

                    @Override
                    public void removedService(ServiceReference<Object> reference, Object service) {
                        context.ungetService(reference);
                        final Optional<ScheduledFuture<?>> currentTask = task;
                        if (currentTask.isPresent()) {
                            currentTask.get().cancel(false);
                            task = Optional.empty();
                        }
                    }
                });

        tracker.open();

        return result.whenComplete((ok, ex) -> {
            tracker.close();
            executor.shutdown();
        });
    }
}
