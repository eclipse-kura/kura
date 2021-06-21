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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

import org.bouncycastle.asn1.x500.X500Name;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.keystore.FilesystemKeystoreServiceImpl;
import org.eclipse.kura.core.testutil.http.TestServer;
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

            Thread.sleep(6000);

            assertTrue(new File(fixture.getKeystoreFile().getAbsolutePath() + ".crl").exists());
        }
    }

    @Test
    public void shouldSupportChangingStoreFile() throws Exception {
        final X500Name name = new X500Name("cn=Test CA, dc=bar.com");

        final TestCA ca = new TestCA(CertificateCreationOptions.builder(name).build());

        try (final Fixture fixture = new Fixture()) {
            final CompletableFuture<String> nextDownload = fixture.nextDownloadRelativeURI();
            final CompletableFuture<Event> nextEventAdminEvent = fixture.nextEventAdminEvent();

            fixture.activate();

            final X509CRL crl = ca.generateCRL(CRLCreationOptions.builder().build());
            fixture.setCrl("/foo.crl", crl);

            fixture.keystoreService.setEntry("foo", new TrustedCertificateEntry(ca.getCertificate()));

            final File file = Files.createTempFile(null, null).toFile();

            assertTrue(file.delete());
            assertFalse(file.exists());

            fixture.update(fixture.getOptions().setCrlManagerEnabled(true)
                    .setCrlUrls(Collections.singletonList(fixture.getCrlDownloadURL("/foo.crl")))
                    .setCrlStoreFile(Optional.of(file)));

            assertEquals("/foo.crl", nextDownload.get(1, TimeUnit.MINUTES));
            assertEquals(DEFAULT_KEYSTORE_PID,
                    ((KeystoreChangedEvent) nextEventAdminEvent.get(1, TimeUnit.MINUTES)).getSenderPid());

            Thread.sleep(6000);
            assertTrue(file.exists());
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

    @Test
    public void shouldRejectUpdateIfIssuerNameDiffers() throws Exception {
        final X500Name name = new X500Name("cn=Test CA, dc=bar.com");

        try (final Fixture fixture = new Fixture()) {

            final TestCA ca = new TestCA(CertificateCreationOptions.builder(name)
                    .withCRLDownloadURI(new URI(fixture.getCrlDownloadURL("/foo.crl"))).build());

            CompletableFuture<String> nextDownload = fixture.nextDownloadRelativeURI();
            CompletableFuture<Event> nextEventAdminEvent = fixture.nextEventAdminEvent();

            fixture.setOptions(fixture.getOptions().setCrlManagerEnabled(true).setCrlUpdateInterval(1)
                    .setCrlUpdateIntervalTimeUnit(TimeUnit.SECONDS));
            fixture.activate();

            final X509CRL crl = ca.generateCRL(CRLCreationOptions.builder().build());
            fixture.setCrl("/foo.crl", crl);

            fixture.keystoreService.setEntry("foo", new TrustedCertificateEntry(ca.getCertificate()));

            assertEquals("/foo.crl", nextDownload.get(1, TimeUnit.MINUTES));
            assertEquals(DEFAULT_KEYSTORE_PID,
                    ((KeystoreChangedEvent) nextEventAdminEvent.get(1, TimeUnit.MINUTES)).getSenderPid());

            nextEventAdminEvent = fixture.nextEventAdminEvent();

            final TestCA otherCA = new TestCA(
                    CertificateCreationOptions.builder(new X500Name("cn=Other CA, dc=baz.org")).build());
            fixture.setCrl("/foo.crl", otherCA.generateCRL(CRLCreationOptions.builder().build()));

            nextDownload = fixture.nextDownloadRelativeURI();
            nextDownload.get(1, TimeUnit.MINUTES);

            nextDownload = fixture.nextDownloadRelativeURI();
            nextDownload.get(1, TimeUnit.MINUTES);

            assertEquals(false, nextEventAdminEvent.isDone());

        }
    }

    @Test
    public void shouldRejectUpdateIfNotSignedByTrustedCert() throws Exception {
        final X500Name name = new X500Name("cn=Test CA, dc=bar.com");

        try (final Fixture fixture = new Fixture()) {

            final TestCA ca = new TestCA(CertificateCreationOptions.builder(name)
                    .withCRLDownloadURI(new URI(fixture.getCrlDownloadURL("/foo.crl"))).build());

            CompletableFuture<String> nextDownload = fixture.nextDownloadRelativeURI();
            CompletableFuture<Event> nextEventAdminEvent = fixture.nextEventAdminEvent();

            fixture.setOptions(fixture.getOptions().setCrlManagerEnabled(true).setCrlUpdateInterval(1)
                    .setCrlUpdateIntervalTimeUnit(TimeUnit.SECONDS));
            fixture.activate();

            final X509CRL crl = ca.generateCRL(CRLCreationOptions.builder().build());
            fixture.setCrl("/foo.crl", crl);

            fixture.keystoreService.setEntry("foo", new TrustedCertificateEntry(ca.getCertificate()));

            assertEquals("/foo.crl", nextDownload.get(1, TimeUnit.MINUTES));
            assertEquals(DEFAULT_KEYSTORE_PID,
                    ((KeystoreChangedEvent) nextEventAdminEvent.get(1, TimeUnit.MINUTES)).getSenderPid());

            nextEventAdminEvent = fixture.nextEventAdminEvent();

            final TestCA otherCA = new TestCA(CertificateCreationOptions.builder(name).build());
            fixture.setCrl("/foo.crl", otherCA.generateCRL(CRLCreationOptions.builder().build()));

            nextDownload = fixture.nextDownloadRelativeURI();
            nextDownload.get(1, TimeUnit.MINUTES);

            nextDownload = fixture.nextDownloadRelativeURI();
            nextDownload.get(1, TimeUnit.MINUTES);

            assertEquals(false, nextEventAdminEvent.isDone());

        }
    }

    @Test
    public void shouldAcceptUpdateIfNotSignedByTrustedCertAndVerificationDisabled() throws Exception {
        final X500Name name = new X500Name("cn=Test CA, dc=bar.com");

        try (final Fixture fixture = new Fixture()) {

            final TestCA ca = new TestCA(CertificateCreationOptions.builder(name)
                    .withCRLDownloadURI(new URI(fixture.getCrlDownloadURL("/foo.crl"))).build());

            CompletableFuture<String> nextDownload = fixture.nextDownloadRelativeURI();
            CompletableFuture<Event> nextEventAdminEvent = fixture.nextEventAdminEvent();

            fixture.setOptions(fixture.getOptions().setCrlManagerEnabled(true).setCrlUpdateInterval(1)
                    .setCrlVerificationEnabled(false).setCrlUpdateIntervalTimeUnit(TimeUnit.SECONDS));
            fixture.activate();

            final X509CRL crl = ca.generateCRL(CRLCreationOptions.builder().build());
            fixture.setCrl("/foo.crl", crl);

            fixture.keystoreService.setEntry("foo", new TrustedCertificateEntry(ca.getCertificate()));

            assertEquals("/foo.crl", nextDownload.get(1, TimeUnit.MINUTES));
            assertEquals(DEFAULT_KEYSTORE_PID,
                    ((KeystoreChangedEvent) nextEventAdminEvent.get(1, TimeUnit.MINUTES)).getSenderPid());

            nextEventAdminEvent = fixture.nextEventAdminEvent();

            final TestCA otherCA = new TestCA(CertificateCreationOptions.builder(name).build());
            fixture.setCrl("/foo.crl", otherCA.generateCRL(CRLCreationOptions.builder().build()));

            nextDownload = fixture.nextDownloadRelativeURI();
            nextDownload.get(1, TimeUnit.MINUTES);

            nextDownload = fixture.nextDownloadRelativeURI();
            nextDownload.get(1, TimeUnit.MINUTES);

            assertEquals(true, nextEventAdminEvent.isDone());

        }
    }

    private static class Fixture implements AutoCloseable {

        private final FilesystemKeystoreServiceImpl keystoreService = new FilesystemKeystoreServiceImpl();
        private TestServer server;
        private final EventAdmin eventAdmin = Mockito.mock(EventAdmin.class);
        private final ConfigurationService configurationService = Mockito.mock(ConfigurationService.class);
        private final CryptoService cryptoService = Mockito.mock(CryptoService.class);
        private final ComponentContext componentContext = Mockito.mock(ComponentContext.class);
        private Options options;

        private Optional<Consumer<String>> downloadListener = Optional.empty();
        private Optional<Consumer<Event>> eventAdminListener = Optional.empty();
        private final int currentPort;
        private final File keystoreFile;

        Fixture() throws Exception {
            this(Optional.empty());
        }

        Fixture(final Optional<File> keystoreFile) throws Exception {
            this.currentPort = jettyPort++;

            if (keystoreFile.isPresent()) {
                this.keystoreFile = keystoreFile.get();
            } else {
                this.keystoreFile = Files.createTempFile(null, null).toFile();
            }

            initializeKeystore(this.keystoreFile);

            this.options = new Options(this.keystoreFile);

            Mockito.when(this.cryptoService.encryptAes((char[]) Matchers.any()))
                    .thenAnswer(i -> i.getArgumentAt(0, char[].class));
            Mockito.when(this.cryptoService.decryptAes((char[]) Matchers.any()))
                    .thenAnswer(i -> i.getArgumentAt(0, char[].class));
            Mockito.when(this.cryptoService.getKeyStorePassword(Matchers.any(String.class)))
            .thenReturn(DEFAULT_KEYSTORE_PASSWORD.toCharArray());
            Mockito.doAnswer(i -> {
                this.eventAdminListener.ifPresent(e -> e.accept(i.getArgumentAt(0, Event.class)));
                return (Void) null;
            }).when(this.eventAdmin).postEvent(Matchers.any());

            this.keystoreService.setConfigurationService(this.configurationService);
            this.keystoreService.setEventAdmin(this.eventAdmin);
            this.keystoreService.setCryptoService(this.cryptoService);

            this.server = new TestServer(this.currentPort, Optional.of(s -> {
                this.downloadListener.ifPresent(l -> l.accept(s));
            }));
        }

        public File getKeystoreFile() {
            return this.keystoreFile;
        }

        public String getCrlDownloadURL(final String relativeUrl) {
            return "http://localhost:" + this.currentPort + relativeUrl;
        }

        Options getOptions() {
            return this.options;
        }

        void setCrl(final String relativeURI, final X509CRL crl) throws IOException {
            try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                TestCA.encodeToPEM(crl, out);
                this.server.setResource(relativeURI, out.toByteArray());
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
            this.keystoreService.activate(this.componentContext, this.options.toProperties());
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
            this.server.close();
        }

        private void initializeKeystore(final File file)
                throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
            final KeyStore keystore = KeyStore.getInstance("jks");
            keystore.load(null, null);

            try (final FileOutputStream out = new FileOutputStream(file)) {
                keystore.store(out, DEFAULT_KEYSTORE_PASSWORD.toCharArray());
            }
        }
    }

    private static class Options {

        private final File keystorePath;
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

            result.put("keystore.path", this.keystorePath.toString());
            result.put("keystore.password", this.keystorePassword);
            result.put("crl.management.enabled", this.crlManagerEnabled);
            result.put("crl.update.interval", this.crlUpdateInterval);
            result.put("crl.update.interval.time.unit", this.crlUpdateIntervalTimeUnit.name());
            result.put("crl.check.interval", this.crlCheckInterval);
            result.put("crl.check.interval.time.unit", this.crlCheckIntervalTimeUnit.name());
            result.put("crl.urls", this.crlUrls.toArray(new String[this.crlUrls.size()]));
            this.crlStoreFile.ifPresent(p -> result.put("crl.store.path", p.getAbsolutePath()));
            result.put("verify.crl", this.crlVerificationEnabled);
            result.put(ConfigurationService.KURA_SERVICE_PID, this.ownPid);

            return result;
        }
    }
}
