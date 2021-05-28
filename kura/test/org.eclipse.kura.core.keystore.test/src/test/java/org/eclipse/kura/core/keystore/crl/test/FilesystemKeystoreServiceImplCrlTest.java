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
package org.eclipse.kura.core.keystore.crl.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.asn1.x500.X500Name;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.keystore.FilesystemKeystoreServiceImpl;
import org.eclipse.kura.core.testutil.pki.TestCA;
import org.eclipse.kura.core.testutil.pki.TestCA.CRLCreationOptions;
import org.eclipse.kura.core.testutil.pki.TestCA.CertificateCreationOptions;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.security.keystore.KeystoreChangedEvent;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesystemKeystoreServiceImplCrlTest {

    private static final Logger logger = LoggerFactory.getLogger(FilesystemKeystoreServiceImplCrlTest.class);

    private static final String DEFAULT_KEYSTORE_PASSWORD = "foo";
    private static final String DEFAULT_KEYSTORE_PID = "foo";

    private static int jettyPort = 8085;

    @Test
    public void shouldDownloadCRLFromConfig() throws Exception {
        final X500Name name = new X500Name("cn=Test CA, dc=bar.com");

        final TestCA ca = new TestCA(CertificateCreationOptions.builder(name).build());

        try (final Fixture fixture = new Fixture()) {
            final CompletableFuture<String> nextDownload = fixture.nextDownloadRelativeURI();
            final CompletableFuture<Event> nextEventAdminEvent = fixture.nextEventAdminEvent();

            fixture.activate();

            final X509CRL crl = ca.generateCRL(CRLCreationOptions.builder().build());
            fixture.setCrl("/foo.crl", crl);

            fixture.keystoreService.setEntry("foo", new TrustedCertificateEntry(ca.getCertificate()));

            fixture.update(fixture.getOptions().setCrlManagerEnabled(true)
                    .setCrlUrls(Collections.singletonList(fixture.getCrlDownloadURL("/foo.crl"))));

            assertEquals("/foo.crl", nextDownload.get(1, TimeUnit.MINUTES));
            assertEquals(DEFAULT_KEYSTORE_PID,
                    ((KeystoreChangedEvent) nextEventAdminEvent.get(1, TimeUnit.MINUTES)).getSenderPid());
        }
    }

    @Test
    public void shouldDownloadCrlFromCertificateDistributionPoint() throws Exception {
        final X500Name name = new X500Name("cn=Test CA, dc=bar.com");

        try (final Fixture fixture = new Fixture()) {

            final TestCA ca = new TestCA(CertificateCreationOptions.builder(name)
                    .withCRLDownloadURI(new URI(fixture.getCrlDownloadURL("/foo.crl"))).build());

            final CompletableFuture<String> nextDownload = fixture.nextDownloadRelativeURI();
            final CompletableFuture<Event> nextEventAdminEvent = fixture.nextEventAdminEvent();

            fixture.activate();

            final X509CRL crl = ca.generateCRL(CRLCreationOptions.builder().build());
            fixture.setCrl("/foo.crl", crl);

            fixture.keystoreService.setEntry("foo", new TrustedCertificateEntry(ca.getCertificate()));

            fixture.update(fixture.getOptions().setCrlManagerEnabled(true));

            assertEquals("/foo.crl", nextDownload.get(1, TimeUnit.MINUTES));
            assertEquals(DEFAULT_KEYSTORE_PID,
                    ((KeystoreChangedEvent) nextEventAdminEvent.get(1, TimeUnit.MINUTES)).getSenderPid());
        }
    }

    @Test
    public void shouldMergeDistributionPoints() throws Exception {
        final X500Name name = new X500Name("cn=Test CA, dc=bar.com");

        try (final Fixture fixture = new Fixture()) {

            final TestCA ca = new TestCA(CertificateCreationOptions.builder(name)
                    .withCRLDownloadURI(new URI(fixture.getCrlDownloadURL("/foo.crl"))).build());

            final List<String> downloads = fixture.collectDownloadRelativeURIs();
            final List<Event> eventAdminEvents = fixture.collectEventAdminEvents();

            fixture.activate();

            final X509CRL crl = ca.generateCRL(CRLCreationOptions.builder().build());
            fixture.setCrl("/foo.crl", crl);

            fixture.keystoreService.setEntry("foo", new TrustedCertificateEntry(ca.getCertificate()));

            fixture.update(fixture.getOptions().setCrlManagerEnabled(true)
                    .setCrlUrls(Collections.singletonList(fixture.getCrlDownloadURL("/foo.crl"))));

            Thread.sleep(10000);

            assertEquals(1, downloads.size());
            assertEquals(2, eventAdminEvents.size());
        }
    }

    @Test
    public void shouldSupportLoad() throws Exception {
        final X500Name name = new X500Name("cn=Test CA, dc=bar.com");

        File keystoreFile;

        try (final Fixture fixture = new Fixture()) {

            keystoreFile = fixture.getKeystoreFile();

            final TestCA ca = new TestCA(CertificateCreationOptions.builder(name).build());

            final CompletableFuture<String> nextDownload = fixture.nextDownloadRelativeURI();

            fixture.activate();

            final X509CRL crl = ca.generateCRL(CRLCreationOptions.builder().build());
            fixture.setCrl("/foo.crl", crl);

            fixture.keystoreService.setEntry("foo", new TrustedCertificateEntry(ca.getCertificate()));

            fixture.update(fixture.getOptions().setCrlManagerEnabled(true)
                    .setCrlUrls(Collections.singletonList(fixture.getCrlDownloadURL("/foo.crl"))));

            assertEquals("/foo.crl", nextDownload.get(1, TimeUnit.MINUTES));

            Thread.sleep(10000);
        }

        try (final Fixture fixture = new Fixture(Optional.of(keystoreFile))) {

            fixture.setOptions(fixture.getOptions().setCrlManagerEnabled(true)
                    .setCrlUrls(Collections.singletonList(fixture.getCrlDownloadURL("/foo.crl"))));

            fixture.activate();

            assertEquals(1, fixture.keystoreService.getCRLs().size());
        }
    }

    @Test
    public void shouldDownloadCRLAgainOnNextUpdate() throws Exception {
        final X500Name name = new X500Name("cn=Test CA, dc=bar.com");

        final TestCA ca = new TestCA(CertificateCreationOptions.builder(name).build());

        try (final Fixture fixture = new Fixture()) {
            CompletableFuture<String> nextDownload = fixture.nextDownloadRelativeURI();

            fixture.activate();

            final X509CRL crl = ca.generateCRL(
                    CRLCreationOptions.builder().withEndDate(Date.from(Instant.now().plusMillis(5000))).build());
            fixture.setCrl("/foo.crl", crl);

            fixture.keystoreService.setEntry("foo", new TrustedCertificateEntry(ca.getCertificate()));

            fixture.update(fixture.getOptions().setCrlManagerEnabled(true)
                    .setCrlUrls(Collections.singletonList(fixture.getCrlDownloadURL("/foo.crl"))));

            assertEquals("/foo.crl", nextDownload.get(1, TimeUnit.MINUTES));
            nextDownload = fixture.nextDownloadRelativeURI();

            final X509CRL newCrl = ca.generateCRL(CRLCreationOptions.builder().build());
            fixture.setCrl("/foo.crl", newCrl);
            assertEquals("/foo.crl", nextDownload.get(1, TimeUnit.MINUTES));
        }
    }

    @Test
    public void shouldSupportCertificateRemoval() throws Exception {
        final X500Name name = new X500Name("cn=Test CA, dc=bar.com");

        try (final Fixture fixture = new Fixture()) {

            final TestCA ca = new TestCA(CertificateCreationOptions.builder(name)
                    .withCRLDownloadURI(new URI(fixture.getCrlDownloadURL("/toBeRemoved.crl"))).build());

            final CompletableFuture<String> nextDownload = fixture.nextDownloadRelativeURI();
            CompletableFuture<Event> nextEventAdminEvent = fixture.nextEventAdminEvent();

            fixture.setOptions(fixture.getOptions().setCrlManagerEnabled(true));
            fixture.activate();

            final X509CRL crl = ca.generateCRL(CRLCreationOptions.builder().build());
            fixture.setCrl("/toBeRemoved.crl", crl);

            fixture.keystoreService.setEntry("toBeRemoved", new TrustedCertificateEntry(ca.getCertificate()));

            assertEquals("/toBeRemoved.crl", nextDownload.get(1, TimeUnit.MINUTES));
            assertEquals(DEFAULT_KEYSTORE_PID,
                    ((KeystoreChangedEvent) nextEventAdminEvent.get(1, TimeUnit.MINUTES)).getSenderPid());
            nextEventAdminEvent = fixture.nextEventAdminEvent();

            assertEquals(1, fixture.keystoreService.getCRLs().size());

            fixture.keystoreService.deleteEntry("toBeRemoved");
            assertEquals(DEFAULT_KEYSTORE_PID,
                    ((KeystoreChangedEvent) nextEventAdminEvent.get(1, TimeUnit.MINUTES)).getSenderPid());

            assertEquals(0, fixture.keystoreService.getCRLs().size());
        }
    }

    private static class Fixture implements AutoCloseable {

        private final FilesystemKeystoreServiceImpl keystoreService = new FilesystemKeystoreServiceImpl();
        private Server server;
        private final EventAdmin eventAdmin = Mockito.mock(EventAdmin.class);
        private final ConfigurationService configurationService = Mockito.mock(ConfigurationService.class);
        private final CryptoService cryptoService = Mockito.mock(CryptoService.class);
        private final ComponentContext componentContext = Mockito.mock(ComponentContext.class);
        private Options options;

        private final Map<String, byte[]> distributionPoints = new HashMap<>();
        private Optional<Consumer<String>> downloadListener = Optional.empty();
        private Optional<Consumer<Event>> eventAdminListener = Optional.empty();
        private final int currentPort;
        private final File keystoreFile;

        Fixture() throws Exception {
            this(Optional.empty());
        }

        Fixture(final Optional<File> keystoreFile) throws Exception {
            currentPort = jettyPort++;

            if (keystoreFile.isPresent()) {
                this.keystoreFile = keystoreFile.get();
            } else {
                this.keystoreFile = Files.createTempFile(null, null).toFile();
            }

            initializeKeystore(this.keystoreFile);

            this.options = new Options(this.keystoreFile);

            Mockito.when(cryptoService.encryptAes((char[]) Matchers.any()))
                    .thenAnswer(i -> i.getArgumentAt(0, char[].class));
            Mockito.when(cryptoService.decryptAes((char[]) Matchers.any()))
                    .thenAnswer(i -> i.getArgumentAt(0, char[].class));
            Mockito.doAnswer(i -> {
                eventAdminListener.ifPresent(e -> e.accept(i.getArgumentAt(0, Event.class)));
                return (Void) null;
            }).when(eventAdmin).postEvent(Mockito.any());

            keystoreService.setConfigurationService(configurationService);
            keystoreService.setEventAdmin(eventAdmin);
            keystoreService.setCryptoService(cryptoService);

            this.server = new Server();

            final ServerConnector connector = new ServerConnector(server);
            connector.setPort(currentPort);
            this.server.setConnectors(new ServerConnector[] { connector });

            final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            this.server.setHandler(context);

            context.addServlet(new ServletHolder(new DownloadServlet()), "/*");
            this.server.start();
        }

        public File getKeystoreFile() {
            return keystoreFile;
        }

        public String getCrlDownloadURL(final String relativeUrl) {
            return "http://localhost:" + currentPort + relativeUrl;
        }

        Options getOptions() {
            return options;
        }

        void setCrl(final String relativeURI, final X509CRL crl) throws IOException {
            try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                TestCA.encodeToPEM(crl, out);
                this.distributionPoints.put(relativeURI, out.toByteArray());
            }
        }

        void setOptions(final Options options) {
            this.options = options;
        }

        void update(final Options options) {
            this.options = options;
            this.keystoreService.updated(options.toProperties());
        }

        void activate() {
            keystoreService.activate(componentContext, options.toProperties());
        }

        public void setDownloadListener(final Consumer<String> listener) {
            this.downloadListener = Optional.of(listener);
        }

        public CompletableFuture<String> nextDownloadRelativeURI() {
            final CompletableFuture<String> result = new CompletableFuture<>();
            setDownloadListener(result::complete);
            return result.whenComplete((ok, ex) -> this.downloadListener = Optional.empty());
        }

        public List<String> collectDownloadRelativeURIs() {
            final List<String> result = new ArrayList<>();
            setDownloadListener(result::add);
            return result;
        }

        public void setEventAdminListener(final Consumer<Event> eventConsumer) {
            this.eventAdminListener = Optional.of(eventConsumer);
        }

        public CompletableFuture<Event> nextEventAdminEvent() {
            final CompletableFuture<Event> result = new CompletableFuture<>();
            setEventAdminListener(result::complete);
            return result.whenComplete((ok, ex) -> this.eventAdminListener = Optional.empty());
        }

        public List<Event> collectEventAdminEvents() {
            final List<Event> result = new ArrayList<>();
            setEventAdminListener(result::add);
            return result;
        }

        @Override
        public void close() throws Exception {
            this.keystoreService.deactivate();
            this.server.stop();
        }

        private void initializeKeystore(final File file)
                throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
            final KeyStore keystore = KeyStore.getInstance("jks");
            keystore.load(null, null);

            try (final FileOutputStream out = new FileOutputStream(file)) {
                keystore.store(out, DEFAULT_KEYSTORE_PASSWORD.toCharArray());
            }
        }

        private class DownloadServlet extends HttpServlet {

            @Override
            protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
                    throws ServletException, IOException {

                logger.info("called");

                final String uri = req.getRequestURI();

                logger.info("got request {}", uri);

                downloadListener.ifPresent(l -> l.accept(uri));

                final Optional<byte[]> data = Optional.ofNullable(distributionPoints.get(uri));

                try {
                    if (data.isPresent()) {
                        resp.setStatus(200);
                        resp.setContentType("application/pkix-crl");
                        final OutputStream out = resp.getOutputStream();
                        out.write(data.get());
                        out.flush();
                    } else {
                        resp.sendError(404);
                    }
                } catch (final IOException e) {
                    // do nothing
                }
            }

        }

    }

    private static class Options {

        private File keystorePath;
        private String keystorePassword = DEFAULT_KEYSTORE_PASSWORD;
        private boolean crlManagerEnabled = false;
        private long crlUpdateInterval = 1;
        private TimeUnit crlUpdateIntervalTimeUnit = TimeUnit.DAYS;
        private long crlCheckInterval = 1;
        private TimeUnit crlCheckIntervalTimeUnit = TimeUnit.SECONDS;
        private Optional<File> crlStoreFile = Optional.empty();
        private List<String> crlUrls = Collections.emptyList();
        private boolean crlVerificationEnabled = true;
        private String ownPid = "foo";

        Options(final File keystorePath) {
            this.keystorePath = keystorePath;
        }

        Options setKeystorePassword(String keyStorePassword) {
            this.keystorePassword = keyStorePassword;
            return this;
        }

        Options setCrlManagerEnabled(boolean crlManagerEnabled) {
            this.crlManagerEnabled = crlManagerEnabled;
            return this;
        }

        Options setCrlUpdateInterval(long crlUpdateInterval) {
            this.crlUpdateInterval = crlUpdateInterval;
            return this;
        }

        Options setCrlUpdateIntervalTimeUnit(TimeUnit crlUpdateIntervalTimeUnit) {
            this.crlUpdateIntervalTimeUnit = crlUpdateIntervalTimeUnit;
            return this;
        }

        Options setCrlCheckInterval(long crlCheckInterval) {
            this.crlCheckInterval = crlCheckInterval;
            return this;
        }

        Options setCrlCheckIntervalTimeUnit(TimeUnit crlCheckIntervalTimeUnit) {
            this.crlCheckIntervalTimeUnit = crlCheckIntervalTimeUnit;
            return this;
        }

        Options setCrlStoreFile(Optional<File> crlStoreFile) {
            this.crlStoreFile = crlStoreFile;
            return this;
        }

        Options setCrlUrls(List<String> crlUrls) {
            this.crlUrls = crlUrls;
            return this;
        }

        Options setCrlVerificationEnabled(boolean crlVerificationEnabled) {
            this.crlVerificationEnabled = crlVerificationEnabled;
            return this;
        }

        Options setOwnPid(final String ownPid) {
            this.ownPid = ownPid;
            return this;
        }

        Map<String, Object> toProperties() {
            final Map<String, Object> result = new HashMap<>();

            result.put("keystore.path", keystorePath.toString());
            result.put("keystore.password", keystorePassword);
            result.put("crl.management.enabled", crlManagerEnabled);
            result.put("crl.update.interval", crlUpdateInterval);
            result.put("crl.update.interval.time.unit", crlUpdateIntervalTimeUnit.name());
            result.put("crl.check.interval", crlCheckInterval);
            result.put("crl.check.interval.time.unit", crlCheckIntervalTimeUnit.name());
            result.put("crl.urls", crlUrls.toArray(new String[crlUrls.size()]));
            crlStoreFile.ifPresent(p -> result.put("crl.store.path", p.getAbsolutePath()));
            result.put("verify.crl", crlVerificationEnabled);
            result.put(ConfigurationService.KURA_SERVICE_PID, ownPid);

            return result;
        }
    }
}
