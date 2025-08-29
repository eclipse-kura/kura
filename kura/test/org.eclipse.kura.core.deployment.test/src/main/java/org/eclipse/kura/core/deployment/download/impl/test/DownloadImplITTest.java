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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
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
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.deployment.download.impl.DownloadImpl;
import org.eclipse.kura.core.testutil.pki.TestCA;
import org.eclipse.kura.core.testutil.pki.TestCA.CertificateCreationOptions;
import org.eclipse.kura.core.testutil.pki.TestCA.TestCAException;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.ssl.SslManagerService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class DownloadImplITTest {

    private static final Logger logger = LoggerFactory.getLogger(DownloadImplITTest.class);

    private static final CountDownLatch dependencies = new CountDownLatch(4);

    private static final String TEST_KEYSTORE_PID = "testKeystore";
    private static final String HTTP_SERVICE_PID = "org.eclipse.kura.http.server.manager.HttpService";

    private static final int HTTPS_PORT = 9993;

    private static SslManagerService sslManagerService;
    private static ConfigurationService configurationService;
    private static ConfigurableComponent httpService;
    private static CryptoService cryptoService;

    private static File serverKeystore;
    private static File clientKeystore;
    private static TestCA clientCA;
    private static X509Certificate clientCertificate;

    private DownloadImpl downloadImpl;

    private DeploymentPackageDownloadOptions deploymentPackageDownloadOptions;

    @Reference
    public void setSslManagerService(SslManagerService sslManagerService) {
        DownloadImplITTest.sslManagerService = sslManagerService;
        dependencies.countDown();
    }

    @Reference
    public void setConfigurationService(ConfigurationService configurationService) {
        DownloadImplITTest.configurationService = configurationService;
        dependencies.countDown();
    }

    @Reference(target = "(kura.service.pid=" + HTTP_SERVICE_PID + ")")
    public void setHttpService(ConfigurableComponent httpService) {
        DownloadImplITTest.httpService = httpService;
        dependencies.countDown();
    }

    @Reference
    public void setCryptoService(CryptoService cryptoService) {
        DownloadImplITTest.cryptoService = cryptoService;
        dependencies.countDown();
    }

    @BeforeClass
    public static void setupEnvironment() throws InterruptedException, TestCAException, IOException {
        awaitDependencies();
        setUpCA();
        setupHttpService();
    }

    @Test
    public void ciao() {
        assertNotNull(sslManagerService);
    }

    @Test
    public void ciao2() {
        assertNotNull(configurationService);
    }

    private void givenDownloadImpl() {
        this.downloadImpl = new DownloadImpl(this.deploymentPackageDownloadOptions,
                null /* callback not needed, we are only testing download" */);
    }

    private static void setUpCA() throws TestCAException, IOException {

        final TestCA serverCA = new TestCA(
                CertificateCreationOptions.builder(new X500Name("cn=Server CA, dn=foo.org")).build());

        final KeyPair serverKeyPair = TestCA.generateKeyPair();

        final X509Certificate serverCertificate = serverCA.createAndSignCertificate(
                CertificateCreationOptions.builder(new X500Name("cn=Server Cert, dn=foo.org")).build(), serverKeyPair);

        clientCA = new TestCA(CertificateCreationOptions.builder(new X500Name("cn=Client CA, dn=bar.org")).build());

        serverKeystore = TestCA.writeKeystore(
                new KeyStore.PrivateKeyEntry(serverKeyPair.getPrivate(),
                        new Certificate[] { serverCertificate, serverCA.getCertificate() }),
                new KeyStore.TrustedCertificateEntry(clientCA.getCertificate()));

        final KeyPair clientKeyPair = TestCA.generateKeyPair();

        clientCertificate = clientCA.createAndSignCertificate(
                CertificateCreationOptions.builder(new X500Name("cn=admin, dn=bar.org")).build(), clientKeyPair);

        clientKeystore = TestCA.writeKeystore(new KeyStore.PrivateKeyEntry(clientKeyPair.getPrivate(),
                new Certificate[] { clientCertificate, clientCA.getCertificate() }));
    }

    private static void setupHttpService() {
        try {
            try (final TestKeystore testKeystore = new TestKeystore(DownloadImplITTest.configurationService,
                    DownloadImplITTest.cryptoService, TEST_KEYSTORE_PID,
                    HttpsKeystoreServiceOptions.defaultConfiguration())) {

                Map<String, Object> props = new HashMap<>();
                props.put("https.ports", new Integer[] { HTTPS_PORT });
                props.put("KeystoreService.target", testKeystore.getTargetFilter());
                updateComponentConfiguration(DownloadImplITTest.configurationService, HTTP_SERVICE_PID, props) //
                        .get(30, TimeUnit.SECONDS);
            }

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private static void awaitDependencies() throws InterruptedException, TestCAException, IOException {
        if (!dependencies.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("dependencies not resolved in 30 seconds");
        }
    }

    private static class TestKeystore implements AutoCloseable {

        private final ConfigurationService configSvc;
        private final String pid;

        public TestKeystore(final ConfigurationService configSvc, final CryptoService cryptoSvc, final String pid,
                final HttpsKeystoreServiceOptions options)
                throws InterruptedException, ExecutionException, TimeoutException, KuraException {
            this.configSvc = configSvc;
            this.pid = pid;

            ServiceUtil
                    .createFactoryConfiguration(configSvc, ConfigurableComponent.class, pid,
                            "org.eclipse.kura.core.keystore.FilesystemKeystoreServiceImpl",
                            options.withKeystorePath(serverKeystore.getAbsolutePath())
                                    .withKeystorePassword("changeit", cryptoSvc).toProperties())
                    .get(30, TimeUnit.SECONDS);
        }

        String getTargetFilter() {
            return "(kura.service.pid=" + pid + ")";
        }

        @Override
        public void close() throws Exception {
            ServiceUtil.deleteFactoryConfiguration(configSvc, pid).get(30, TimeUnit.SECONDS);
        }

    }

    private static class HttpsKeystoreServiceOptions {

        private String keystorePath = "";
        private String keystorePassword = "";
        private boolean crlManagerEnabled = false;
        private Optional<String[]> crlUrls = Optional.empty();

        private HttpsKeystoreServiceOptions() {
        }

        static HttpsKeystoreServiceOptions defaultConfiguration() {
            return new HttpsKeystoreServiceOptions();
        }

        HttpsKeystoreServiceOptions withKeystorePath(final String keystorePath) {
            this.keystorePath = keystorePath;
            return this;
        }

        HttpsKeystoreServiceOptions withKeystorePassword(final String keystorePassword,
                final CryptoService cryptoService) throws KuraException {
            this.keystorePassword = new String(cryptoService.encryptAes(keystorePassword.toCharArray()));
            return this;
        }

        Map<String, Object> toProperties() {
            final Map<String, Object> result = new HashMap<>();

            result.put("keystore.path", this.keystorePath);
            result.put("keystore.password", this.keystorePassword);
            result.put("crl.check.interval", 1L);
            result.put("crl.check.interval.time.unit", TimeUnit.SECONDS.name());
            result.put("crl.update.interval", 1L);
            result.put("crl.update.interval.time.unit", TimeUnit.SECONDS.name());
            result.put("crl.management.enabled", crlManagerEnabled);
            this.crlUrls.ifPresent(u -> result.put("crl.urls", u));

            return result;
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
