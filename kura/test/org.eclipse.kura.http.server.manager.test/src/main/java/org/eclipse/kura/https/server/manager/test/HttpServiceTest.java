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
package org.eclipse.kura.https.server.manager.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class HttpServiceTest {

    private static final String HTTP_SERVER_MANAGER_PID = "org.eclipse.kura.http.server.manager.HttpService";

    private static CompletableFuture<ConfigurationService> configurationService = new CompletableFuture<>();
    private static CompletableFuture<CryptoService> cryptoService = new CompletableFuture<>();

    public void setConfigurationService(final ConfigurationService configurationService) {
        HttpServiceTest.configurationService.complete(configurationService);
    }

    public void setCryptoService(final CryptoService cryptoService) {
        HttpServiceTest.cryptoService.complete(cryptoService);
    }

    @Test
    public void shouldNotOpenAnyPortWithDefaultConfig() throws KuraException, InvalidSyntaxException,
            InterruptedException, ExecutionException, TimeoutException, MalformedURLException {
        final ConfigurationService configService = configurationService.get(5, TimeUnit.MINUTES);

        updateComponentConfiguration(configService, HTTP_SERVER_MANAGER_PID,
                HttpServiceOptions.defaultConfiguration().toProperties()).get(30, TimeUnit.SECONDS);

        assertAlwaysTrue(() -> getHttpStatusCode("http://localhost:80/") instanceof Failure);
        assertAlwaysTrue(() -> getHttpStatusCode("http://localhost:8080/") instanceof Failure);
        assertAlwaysTrue(() -> getHttpStatusCode("https://localhost:4442/") instanceof Failure);
        assertAlwaysTrue(() -> getHttpStatusCode("https://localhost:4443/") instanceof Failure);

    }

    @Test
    public void shouldOpenHttpPorts() throws KuraException, InvalidSyntaxException, InterruptedException,
            ExecutionException, TimeoutException, MalformedURLException {
        final ConfigurationService configService = configurationService.get(5, TimeUnit.MINUTES);

        updateComponentConfiguration(configService, HTTP_SERVER_MANAGER_PID,
                HttpServiceOptions.defaultConfiguration().withHttpPorts(8080).toProperties()).get(30, TimeUnit.SECONDS);

        assertTrueAtLeastOnce(() -> getHttpStatusCode("http://localhost:8080/").equals(new StatusCode(404)));

        assertAlwaysTrue(() -> getHttpStatusCode("http://localhost:80/") instanceof Failure);
        assertAlwaysTrue(() -> getHttpStatusCode("https://localhost:4442/") instanceof Failure);
        assertAlwaysTrue(() -> getHttpStatusCode("https://localhost:4443/") instanceof Failure);

    }

    @Test
    public void shouldOpenMultipleHttpPorts() throws KuraException, InvalidSyntaxException, InterruptedException,
            ExecutionException, TimeoutException, MalformedURLException {
        final ConfigurationService configService = configurationService.get(5, TimeUnit.MINUTES);

        updateComponentConfiguration(configService, HTTP_SERVER_MANAGER_PID,
                HttpServiceOptions.defaultConfiguration().withHttpPorts(8080, 8081, 8082).toProperties()).get(30,
                        TimeUnit.SECONDS);

        assertTrueAtLeastOnce(() -> getHttpStatusCode("http://localhost:8080/").equals(new StatusCode(404)));
        assertTrueAtLeastOnce(() -> getHttpStatusCode("http://localhost:8081/").equals(new StatusCode(404)));
        assertTrueAtLeastOnce(() -> getHttpStatusCode("http://localhost:8082/").equals(new StatusCode(404)));

    }

    @Test
    public void shouldNotSupportHttpsWithoutKeystore() throws KuraException, InvalidSyntaxException,
            InterruptedException, ExecutionException, TimeoutException, MalformedURLException {
        final ConfigurationService configService = configurationService.get(5, TimeUnit.MINUTES);

        updateComponentConfiguration(configService, HTTP_SERVER_MANAGER_PID, HttpServiceOptions.defaultConfiguration()
                .withHttpsPorts(4442).withHttpsClientAuthPorts(4443).toProperties()).get(30, TimeUnit.SECONDS);

        assertAlwaysTrue(() -> getHttpStatusCode("https://localhost:443/") instanceof Failure);
        assertAlwaysTrue(() -> getHttpStatusCode("https://localhost:4443/") instanceof Failure);

    }

    @Test
    public void shouldSupportHttps() throws KuraException, InvalidSyntaxException, InterruptedException,
            ExecutionException, TimeoutException, IOException {

        final File httpsKeystore = deployResource("/httpskeystore.ks");

        final ConfigurationService configSvc = configurationService.get(5, TimeUnit.MINUTES);
        final CryptoService cryptoSvc = cryptoService.get(5, TimeUnit.MINUTES);

        updateComponentConfiguration(configSvc, HTTP_SERVER_MANAGER_PID,
                HttpServiceOptions.defaultConfiguration().withHttpsPorts(4442)
                        .withKeystorePath(httpsKeystore.getAbsolutePath()).withKeystorePassword("changeit", cryptoSvc)
                        .toProperties()).get(30, TimeUnit.SECONDS);

        assertTrueAtLeastOnce(() -> getHttpStatusCode("https://localhost:4442/", Optional.empty(),
                Optional.of(buildClientTrustManagers())).equals(new StatusCode(404)));
        assertAlwaysTrue(() -> getHttpStatusCode("http://localhost:8080/") instanceof Failure);
        assertAlwaysTrue(() -> getHttpStatusCode("https://localhost:4443/") instanceof Failure);
        assertAlwaysTrue(() -> getHttpStatusCode("http://localhost:80/") instanceof Failure);

    }

    @Test
    public void shouldSupportHttpsClientAuth() throws KuraException, InvalidSyntaxException, InterruptedException,
            ExecutionException, TimeoutException, IOException, UnrecoverableKeyException, KeyManagementException,
            KeyStoreException, NoSuchAlgorithmException, CertificateException {

        final File httpsKeystore = deployResource("/httpskeystore.ks");

        final ConfigurationService configSvc = configurationService.get(5, TimeUnit.MINUTES);
        final CryptoService cryptoSvc = cryptoService.get(5, TimeUnit.MINUTES);

        updateComponentConfiguration(configSvc, HTTP_SERVER_MANAGER_PID,
                HttpServiceOptions.defaultConfiguration().withHttpsClientAuthPorts(4443)
                        .withKeystorePath(httpsKeystore.getAbsolutePath()).withKeystorePassword("changeit", cryptoSvc)
                        .toProperties()).get(30, TimeUnit.SECONDS);

        final KeyManager[] keyManagers = buildClientKeyManagers();

        assertTrueAtLeastOnce(() -> getHttpStatusCode("https://localhost:4443/", Optional.of(keyManagers),
                Optional.of(buildClientTrustManagers())).equals(new StatusCode(404)));
        assertAlwaysTrue(() -> getHttpStatusCode("http://localhost:8080/") instanceof Failure);
        assertAlwaysTrue(() -> getHttpStatusCode("https://localhost:4442/") instanceof Failure);
        assertAlwaysTrue(() -> getHttpStatusCode("http://localhost:80/") instanceof Failure);

    }

    @Test
    public void shouldRejectClientConnectionWithNoCert() throws KuraException, InvalidSyntaxException,
            InterruptedException, ExecutionException, TimeoutException, IOException, UnrecoverableKeyException,
            KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException {

        final File httpsKeystore = deployResource("/httpskeystore.ks");

        final ConfigurationService configSvc = configurationService.get(5, TimeUnit.MINUTES);
        final CryptoService cryptoSvc = cryptoService.get(5, TimeUnit.MINUTES);

        updateComponentConfiguration(configSvc, HTTP_SERVER_MANAGER_PID,
                HttpServiceOptions.defaultConfiguration().withHttpsClientAuthPorts(4443)
                        .withKeystorePath(httpsKeystore.getAbsolutePath()).withKeystorePassword("changeit", cryptoSvc)
                        .toProperties()).get(30, TimeUnit.SECONDS);

        assertTrueAtLeastOnce(() -> getHttpStatusCode("https://localhost:4443/", Optional.empty(),
                Optional.of(buildClientTrustManagers())) instanceof Failure);

    }

    @Test
    public void shouldSupportAllAuthMethods() throws KuraException, InvalidSyntaxException, InterruptedException,
            ExecutionException, TimeoutException, IOException, UnrecoverableKeyException, KeyManagementException,
            KeyStoreException, NoSuchAlgorithmException, CertificateException {

        final File httpsKeystore = deployResource("/httpskeystore.ks");

        final ConfigurationService configSvc = configurationService.get(5, TimeUnit.MINUTES);
        final CryptoService cryptoSvc = cryptoService.get(5, TimeUnit.MINUTES);

        updateComponentConfiguration(configSvc, HTTP_SERVER_MANAGER_PID,
                HttpServiceOptions.defaultConfiguration().withHttpPorts(8080).withHttpsPorts(4442)
                        .withHttpsClientAuthPorts(4443).withKeystorePath(httpsKeystore.getAbsolutePath())
                        .withKeystorePassword("changeit", cryptoSvc).toProperties()).get(30, TimeUnit.SECONDS);

        final KeyManager[] keyManagers = buildClientKeyManagers();

        assertTrueAtLeastOnce(() -> getHttpStatusCode("https://localhost:4443/", Optional.of(keyManagers),
                Optional.of(buildClientTrustManagers())).equals(new StatusCode(404)));
        assertTrueAtLeastOnce(() -> getHttpStatusCode("http://localhost:8080/").equals(new StatusCode(404)));
        assertTrueAtLeastOnce(() -> getHttpStatusCode("https://localhost:4442/", Optional.empty(),
                Optional.of(buildClientTrustManagers())).equals(new StatusCode(404)));

    }

    private static class HttpServiceOptions {

        private Integer[] httpPorts = new Integer[] {};
        private Integer[] httpsPorts = new Integer[] {};
        private Integer[] httpsClientAuthPorts = new Integer[] {};
        private Optional<String> keystorePath = Optional.empty();
        private Optional<String> keystorePassword = Optional.empty();

        private HttpServiceOptions() {
        }

        static HttpServiceOptions defaultConfiguration() {
            return new HttpServiceOptions();
        }

        HttpServiceOptions withHttpPorts(final Integer... httpPorts) {
            this.httpPorts = httpPorts;
            return this;
        }

        HttpServiceOptions withHttpsPorts(final Integer... httpsPorts) {
            this.httpsPorts = httpsPorts;
            return this;
        }

        HttpServiceOptions withHttpsClientAuthPorts(final Integer... httpsClientAuthPorts) {
            this.httpsClientAuthPorts = httpsClientAuthPorts;
            return this;
        }

        HttpServiceOptions withKeystorePath(final String keystorePath) {
            this.keystorePath = Optional.of(keystorePath);
            return this;
        }

        HttpServiceOptions withKeystorePassword(final String keystorePassword, final CryptoService cryptoService)
                throws KuraException {
            this.keystorePassword = Optional.of(new String(cryptoService.encryptAes(keystorePassword.toCharArray())));
            return this;
        }

        Map<String, Object> toProperties() {
            final Map<String, Object> result = new HashMap<>();

            result.put("http.ports", httpPorts);
            result.put("https.ports", httpsPorts);
            result.put("https.client.auth.ports", httpsClientAuthPorts);
            result.put("https.keystore.path", this.keystorePath.orElse(""));
            this.keystorePassword.ifPresent(p -> result.put("https.keystore.password", p));

            return result;
        }

    }

    private interface ConnectionResult {
    }

    private static class StatusCode implements ConnectionResult {

        private final int value;

        StatusCode(final int value) {
            super();
            this.value = value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            StatusCode other = (StatusCode) obj;
            return value == other.value;
        }

        @Override
        public String toString() {
            return "StatusCode [value=" + value + "]";
        }

    }

    private static class Failure implements ConnectionResult {

        private final Throwable cause;

        Failure(Throwable cause) {
            this.cause = cause;
        }

        @Override
        public String toString() {
            return "Failure [cause=" + cause + "]";
        }

    }

    private static ConnectionResult getHttpStatusCode(final String url) {
        return getHttpStatusCode(url, Optional.empty(), Optional.empty());
    }

    private static ConnectionResult getHttpStatusCode(final String url, final Optional<KeyManager[]> keyManagers,
            final Optional<TrustManager[]> trustManagers) {
        try {
            final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

            if (connection instanceof HttpsURLConnection) {
                final HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

                final SSLContext context = SSLContext.getInstance("TLS");

                context.init(keyManagers.orElse(null), trustManagers.orElse(null), null);

                HttpsURLConnection.setDefaultHostnameVerifier((h, s) -> true);
                HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

                httpsConnection.setHostnameVerifier((h, s) -> true);
            }

            return new StatusCode(connection.getResponseCode());
        } catch (final Exception e) {
            return new Failure(e);
        }
    }

    private static void assertAlwaysTrue(final Supplier<Boolean> check) throws InterruptedException {
        for (int i = 0; i < 3; i++) {

            if (!check.get()) {
                fail("should have been true");
            }

            Thread.sleep(1000);
        }
    }

    private static void assertTrueAtLeastOnce(final Supplier<Boolean> check) throws InterruptedException {
        for (int i = 0; i < 3; i++) {

            if (check.get()) {
                return;
            }

            Thread.sleep(1000);
        }

        fail("should have been true at least once");
    }

    static CompletableFuture<Void> updateComponentConfiguration(final ConfigurationService configurationService,
            final String pid, final Map<String, Object> properties) throws KuraException, InvalidSyntaxException {

        final CompletableFuture<Void> result = new CompletableFuture<Void>();
        final BundleContext context = FrameworkUtil.getBundle(WireTestUtil.class).getBundleContext();

        final ServiceTracker<?, ?> tracker = new ServiceTracker<Object, Object>(context,
                FrameworkUtil.createFilter("(kura.service.pid=" + pid + ")"),
                new ServiceTrackerCustomizer<Object, Object>() {

                    @Override
                    public Object addingService(ServiceReference<Object> reference) {
                        return context.getService(reference);
                    }

                    @Override
                    public void modifiedService(ServiceReference<Object> reference, Object service) {
                        result.complete(null);
                    }

                    @Override
                    public void removedService(ServiceReference<Object> reference, Object service) {
                        context.ungetService(reference);
                    }
                });

        tracker.open();

        configurationService.updateConfiguration(pid, properties);

        return result.whenComplete((ok, ex) -> tracker.close());
    }

    private static File deployResource(final String resourcePath) throws IOException {

        final File target = Files.createTempFile(null, null).toFile();

        final byte[] buf = new byte[4096];

        try (final FileOutputStream out = new FileOutputStream(target);
                final InputStream in = HttpServiceTest.class.getResourceAsStream(resourcePath)) {
            int rd;
            while ((rd = in.read(buf)) > 0) {
                out.write(buf, 0, rd);
            }
        }

        return target;
    }

    private static KeyManager[] buildClientKeyManagers() throws KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException, UnrecoverableKeyException, KeyManagementException {
        final KeyStore keystore = KeyStore.getInstance("JKS");
        try (final FileInputStream in = new FileInputStream(deployResource("/clientkeystore.ks"))) {
            keystore.load(in, "changeit".toCharArray());
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keystore, "foobar".toCharArray());

        return keyManagerFactory.getKeyManagers();
    }

    private static TrustManager[] buildClientTrustManagers() {
        final TrustManager trustManager = new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                // accept
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                // accept
            }
        };

        return new TrustManager[] { trustManager };
    }

}
