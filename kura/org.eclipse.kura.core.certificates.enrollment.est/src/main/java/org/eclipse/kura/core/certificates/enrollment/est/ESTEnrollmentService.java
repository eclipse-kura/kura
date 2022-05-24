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

package org.eclipse.kura.core.certificates.enrollment.est;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.est.CACertsResponse;
import org.bouncycastle.est.ESTAuth;
import org.bouncycastle.est.ESTService;
import org.bouncycastle.est.EnrollmentResponse;
import org.bouncycastle.est.HttpAuth;
import org.bouncycastle.est.jcajce.ChannelBindingProvider;
import org.bouncycastle.est.jcajce.JcaHttpAuthBuilder;
import org.bouncycastle.est.jcajce.JcaJceUtils;
import org.bouncycastle.est.jcajce.JsseESTServiceBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.BCSSLConnection;
import org.bouncycastle.jsse.BCSSLSocket;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.certificate.enrollment.EnrollmentService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESTEnrollmentService implements EnrollmentService, ConfigurableComponent {

    private static final int ENROLLMENT_PROCESS_TIMEOUT = 60000; // milliseconds

    private static final char[] DEFAULT_KEYSTORE_PASSW = "estpassw".toCharArray();

    private static final Logger logger = LoggerFactory.getLogger(ESTEnrollmentService.class);

    private static final String EST_CACERTS_ALIAS_PREFIX = "est-cacerts_";
    private static final String EST_CLIENT_CERT_ALIAS = "est-client-certs";
    private static final String EST_PRIVATE_KEY_CERT = "est-private-key-certs";

    private static final String CLIENT_AUTH_ALIAS = "client-auth-certs";

    private static final boolean DO_NOT_RE_ENROLL = false;

    private CryptoService cryptoService;
    private KeystoreService keystoreService;

    private Optional<String> keystoreServicePid;

    private ESTService estService;

    private ESTEnrollmentServiceOptions estOptions;
    private boolean hasCaCerts;

    private boolean enrolled;

    private PKCS10CertificationRequestBuilder csrBuilder;

    private ESTAuth auth;

    private ExecutorService enrollmentService = Executors.newSingleThreadExecutor();
    private ScheduledExecutorService delayer = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService caExpirationChecker = Executors.newSingleThreadScheduledExecutor();

    private JcaX509CRLConverter jcaX509CRLConverter;

    public ESTEnrollmentService() {

        Security.addProvider(new BouncyCastleProvider());
        Security.addProvider(new BouncyCastleJsseProvider());

        this.jcaX509CRLConverter = new JcaX509CRLConverter().setProvider("BC");

    }

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void unsetCryptoService(CryptoService cryptoService) {
        if (this.cryptoService != null) {
            this.cryptoService = null;
        }
    }

    public void setKeystoreService(KeystoreService keystoreService, final Map<String, Object> properties) {
        this.keystoreService = keystoreService;
        this.keystoreServicePid = Optional.of((String) properties.get(ConfigurationService.KURA_SERVICE_PID));
    }

    public void unsetKeystoreService(KeystoreService keystoreService) {
        if (this.keystoreService == keystoreService) {

            this.keystoreService = null;
            this.keystoreServicePid = Optional.empty();
        }
    }

    protected void activate(Map<String, Object> properties) {
        logger.info("activating...");
        try {
            this.estOptions = new ESTEnrollmentServiceOptions(this.cryptoService, properties);
            if (Boolean.TRUE.equals(this.estOptions.isEnabled())) {
                checkCerts();
                execServiceChain(isEnrolled());
            }
        } catch (MalformedURLException e) {
            logger.error("Invalid EST Server URL", e);
        } catch (Exception e) {
            logger.error("Error configuring EST client", e);
        }
    }

    private void execServiceChain(boolean isEnrolled) throws Exception {

        initESTService();
        if (!isEnrolled) {
            generateKeyPair();
            createCSR();
            createAuthorization();
            performEnrollment(DO_NOT_RE_ENROLL);

            startCAChecker();

        }
    }

    private void startCAChecker() {

        /*
         * this.caExpirationChecker.scheduleAtFixedRate(() -> {
         * 
         * }, 0, 1, TimeUnit.DAYS);
         */
    }

    private void initESTService() throws Exception {

        Object[] truesteCACerts = null;
        CRL[] revocationList = null;

        if (needBootstrap()) {
            // the ca root certificate provided by the configuration is only needed for the bootstrapping
            wipeAllCaCerts();
            logger.info("Bootstrapping EST connection...");
            truesteCACerts = readPemCertificates(this.estOptions.getServer().getBootstrapCertificate());
        } else {
            truesteCACerts = getCertificatesEntryAsX509Certificate(this::getStoredCACerts);
            revocationList = getRevocationList();
        }

        buildESTService(truesteCACerts, revocationList);

        if (needBootstrap()) {

            logger.info("Downloading CA certificates...");
            CACertsResponse cacertsResponse = this.estService.getCACerts();
            if (cacertsResponse.isTrusted() && cacertsResponse.hasCertificates()) {
                addCaCertsChainToKeystore(cacertsResponse.getCertificateStore());
            } else {
                throw new KuraException(KuraErrorCode.INVALID_CERTIFICATE_EXCEPTION);
            }

            if (cacertsResponse.hasCRLs()) {
                for (X509CRLHolder crlHolder : cacertsResponse.getCrlStore().getMatches(null)) {
                    X509CRL x509CRL = jcaX509CRLConverter.getCRL(crlHolder);
                    this.keystoreService.addCRL(x509CRL);
                }
            }

            // rebuild client with the explicit Trust Anchor
            buildESTService(getCertificatesEntryAsX509Certificate(this::getStoredCACerts), getRevocationList());

            logger.info("Bootstrap ended.");
        }
    }

    private void wipeAllCaCerts() {
        getStoredEntriesByAliasPrefix(EST_CACERTS_ALIAS_PREFIX).map(Map.Entry::getKey).forEach(alias -> {
            try {
                this.keystoreService.deleteEntry(alias);
            } catch (KuraException e) {
                logger.error("Cannot delete entry with alias {} from keystore service with pid: {}", alias,
                        this.keystoreServicePid, e);
            }
        });
    }

    private void addCaCertsChainToKeystore(Store<X509CertificateHolder> certificateStore) {

        int i = 0;

        CertStore certStore;
        try {
            certStore = toCertStore(certificateStore);
            for (Certificate cert : certStore.getCertificates(null)) {
                String alias = EST_CACERTS_ALIAS_PREFIX + i++;
                try {
                    this.keystoreService.setEntry(alias, new TrustedCertificateEntry(cert));
                    logger.info("CA certificates added in {} with alias {}", this.keystoreServicePid.get(), alias);
                } catch (Exception e) {
                    logger.error("Unable to add certificate with alias {} in {}", alias, this.keystoreServicePid.get());
                }
            }

        } catch (Exception e) {
            logger.error("Unable to retrieve certificates from the store", e);
        }
    }

    private void generateKeyPair() throws KuraException {

        AlgorithmParameterSpec algorithmParameter = null;

        switch (this.estOptions.getClient().getKeyPairAlgorithm()) {
        case "EC":
            algorithmParameter = new ECGenParameterSpec(this.estOptions.getClient().getKeyPairAlgorithmParameter());
            break;
        case "RSA":
            algorithmParameter = new RSAKeyGenParameterSpec(
                    Integer.parseInt(this.estOptions.getClient().getKeyPairAlgorithmParameter()),
                    RSAKeyGenParameterSpec.F4);
            break;
        default:
            throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, "client.keypair.algorithm",
                    this.estOptions.getClient().getKeyPairAlgorithm());
        }

        this.keystoreService.createKeyPair(EST_PRIVATE_KEY_CERT, this.estOptions.getClient().getKeyPairAlgorithm(),
                algorithmParameter, this.estOptions.getClient().getSignerAlgorithm(),
                this.estOptions.getClient().getSubjectDN());

    }

    private void createCSR() throws KuraException {

        X500Principal x509Principal = new X500Principal(this.estOptions.getClient().getSubjectDN());
        this.csrBuilder = this.keystoreService.getCSRAsPKCS10Builder(EST_PRIVATE_KEY_CERT, x509Principal);

    }

    private void createAuthorization() throws OperatorCreationException, KuraException {

        if (Boolean.TRUE.equals(this.estOptions.getClient().getBasicAuthentication().isEnabled())) {
            String username = this.estOptions.getClient().getBasicAuthentication().getUsername();

            char[] passw = this.estOptions.getClient().getBasicAuthentication().getPassword();
            if (Boolean.TRUE.equals(this.estOptions.getClient().getBasicAuthentication().isDigestEnabled())) {
                DigestCalculatorProvider digestBuilder = new JcaDigestCalculatorProviderBuilder().setProvider("BC")
                        .build();

                this.auth = new HttpAuth(username, passw, new SecureRandom(), digestBuilder);
            } else {
                this.auth = new JcaHttpAuthBuilder(username, passw).setNonceGenerator(new SecureRandom())
                        .setProvider("BC").build();

            }
        }
    }

    private CRL[] getRevocationList() throws KuraException {
        CRL[] revocationList = null;

        if (this.keystoreService != null) {
            revocationList = this.keystoreService.getCRLs().toArray(new CRL[0]);

            // we need to pass null to disable revocation check
            if (revocationList.length < 1) {
                revocationList = null;
            }
        }
        return revocationList;
    }

    private TrustedCertificateEntry[] getStoredCACerts() {

        return getStoredCertByAliasPrefix(EST_CACERTS_ALIAS_PREFIX, TrustedCertificateEntry.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T[] getStoredCertByAliasPrefix(String aliasPrefix, Class<T> clazz) {
        T[] storedCaCerts = (T[]) Array.newInstance(clazz, 0);
        try {
            storedCaCerts = getStoredEntriesByAliasPrefix(aliasPrefix).map(Map.Entry::getValue)
                    .collect(Collectors.toList()).toArray((T[]) Array.newInstance(clazz, 0));

            if (!Arrays.stream(storedCaCerts).allMatch(clazz::isInstance)) {
                throw new KuraException(KuraErrorCode.INVALID_CERTIFICATE_EXCEPTION);
            }

        } catch (Exception e) {
            logger.error("Unable to get certificate from keystore with pid {}", this.keystoreServicePid);
        }

        return storedCaCerts;
    }

    private Stream<Map.Entry<String, Entry>> getStoredEntriesByAliasPrefix(String aliasPrefix) {

        Stream<Map.Entry<String, Entry>> storedEntriesStream = Stream.empty();

        if (this.keystoreService != null) {
            try {
                storedEntriesStream = this.keystoreService.getEntries().entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith(aliasPrefix));
            } catch (KuraException e) {
                logger.error("Unable to get certificate from keystore with pid {}", this.keystoreServicePid);
            }
        }

        return storedEntriesStream;

    }

    @SuppressWarnings("unchecked")
    private <T> T getESTEntity(String alias, Class<T> clazz) throws KuraException {
        Entry storedEntity = this.keystoreService.getEntry(alias);

        if (storedEntity != null && !(clazz.isInstance(storedEntity))) {
            throw new KuraException(KuraErrorCode.INVALID_CERTIFICATE_EXCEPTION);
        }

        return (T) storedEntity;
    }

    private TrustedCertificateEntry getESTClientCert() throws KuraException {
        return getESTEntity(EST_CLIENT_CERT_ALIAS, TrustedCertificateEntry.class);
    }

    private PrivateKeyEntry getESTKeyPair() throws KuraException {
        return getESTEntity(EST_PRIVATE_KEY_CERT, PrivateKeyEntry.class);
    }

    private PrivateKeyEntry getClientAuthKeyPair() throws KuraException {
        return getESTEntity(CLIENT_AUTH_ALIAS, PrivateKeyEntry.class);
    }

    private void buildESTService(Object[] truesteCACerts, CRL[] revocationList) throws Exception {
        JsseESTServiceBuilder estServiceBuilder = new JsseESTServiceBuilder(this.estOptions.getServer().getHost(),
                JcaJceUtils.getCertPathTrustManager(toTrustAnchor(truesteCACerts), revocationList));

        estServiceBuilder.withProvider("BCJSSE");

        if (Boolean.TRUE.equals(this.estOptions.getClient().isProofOfPossessionEnabled())) {
            estServiceBuilder.withChannelBindingProvider(new BCChannelBindingProvider());
        }

        if (Boolean.TRUE.equals(this.estOptions.getClient().isTlsAuthenticationEnabled())) {
            KeyStore clientKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            clientKeystore.load(null);

            clientKeystore.setEntry(CLIENT_AUTH_ALIAS, getClientAuthKeyPair(),
                    new PasswordProtection(DEFAULT_KEYSTORE_PASSW));

            KeyManager[] keyManager = JcaJceUtils.createKeyManagerFactory(KeyManagerFactory.getDefaultAlgorithm(), null,
                    clientKeystore, DEFAULT_KEYSTORE_PASSW).getKeyManagers();

            estServiceBuilder.withKeyManagers(keyManager);

        }

        this.estService = estServiceBuilder.withTimeout(60000).build();
    }

    private synchronized void performEnrollment(boolean reEnroll) {

        CompletableFuture.supplyAsync(this.enrollmentTask(reEnroll), this.enrollmentService)
                .acceptEither(this.timeoutAfter(ENROLLMENT_PROCESS_TIMEOUT, TimeUnit.MILLISECONDS),
                        this.processEnrollmentResponse())
                .whenComplete((voidValue, ex) -> {
                    if (ex != null) {
                        logger.error("Error in the enrollment process", ex);
                    } else {
                        logger.info("The enrollment process has finished");
                    }
                });

    }

    private Consumer<? super EnrollmentResponse> processEnrollmentResponse() {
        return (EnrollmentResponse response) -> {
            if (response != null) {
                try {
                    CertStore certStore = toCertStore(response.getStore());
                    // we expect only 1 certificate in the store
                    Certificate cert = certStore.getCertificates(null).iterator().next();
                    this.keystoreService.setEntry(EST_CLIENT_CERT_ALIAS, new TrustedCertificateEntry(cert));
                    logger.info("Client certificate with alias {} stored in keyservice {}", EST_CLIENT_CERT_ALIAS,
                            this.keystoreServicePid.get());

                } catch (KuraException e) {
                    logger.error("Unable to store client cert with alias {} in keystore {}", EST_CLIENT_CERT_ALIAS,
                            this.keystoreServicePid.get());

                } catch (Exception e) {
                    logger.error("Unable to retrieve certificates from the store", e);
                }
            }
        };
    }

    private CertStore toCertStore(Store<?> store) throws Exception {
        List<?> certificatesX509 = store.getMatches(null).stream().map(t -> {
            try {
                return toJavaX509Certificate(t);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());

        try {
            return CertStore.getInstance("Collection", new CollectionCertStoreParameters(certificatesX509));
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            throw new Exception(e);
        }
    }

    private Supplier<EnrollmentResponse> enrollmentTask(boolean reEnroll) {

        return () -> {
            logger.info("Starting system enrollment with: {}", this.estOptions.getServer().getHost());
            ContentSigner signer;
            EnrollmentResponse enrollmentResponse = null;
            try {
                signer = new JcaContentSignerBuilder(this.estOptions.getClient().getSignerAlgorithm())
                        .build(getESTKeyPair().getPrivateKey());
                do {
                    if (Boolean.TRUE.equals(this.estOptions.getClient().isProofOfPossessionEnabled())) {
                        enrollmentResponse = this.estService.simpleEnrollPoP(reEnroll, this.csrBuilder, signer,
                                this.auth);
                    } else {
                        enrollmentResponse = this.estService.simpleEnroll(reEnroll, this.csrBuilder.build(signer),
                                this.auth);
                    }

                    if (!enrollmentResponse.isCompleted()) {
                        long t = enrollmentResponse.getNotBefore() - System.currentTimeMillis();
                        if (t < 0) {
                            continue;
                        }
                        t += 1000;
                        Thread.sleep(t);
                        continue;
                    }

                } while (!enrollmentResponse.isCompleted());

            } catch (OperatorCreationException | KuraException e) {
                logger.error("Unable to create the content signer", e);
            } catch (IOException e) {
                logger.error("Unable to perform the enrollment operation", e);
            } catch (InterruptedException e) {
                logger.error("Enrollment task interrupted", e);
                Thread.currentThread().interrupt();
            }
            return enrollmentResponse;
        };
    }

    protected void deactivate() {
        logger.info("deactivating...");
    }

    protected void updated(Map<String, Object> properties) {
        logger.info("updating...");

        ESTEnrollmentServiceOptions newOptions;
        try {
            newOptions = new ESTEnrollmentServiceOptions(this.cryptoService, properties);
            if (this.estOptions.equals(newOptions)) {
                logger.info("Properties are not changed. Nothing to update");
                return;
            }

            this.estOptions = newOptions;
            if (Boolean.TRUE.equals(this.estOptions.isEnabled())) {
                checkCerts();
                execServiceChain(isEnrolled());
            }
        } catch (MalformedURLException e) {
            logger.error("Invalid EST Server URL", e);
        } catch (Exception e) {
            logger.error("Error configuring EST client", e);
        }

        logger.info("update complete");
    }

    @Override
    public synchronized void enroll() throws KuraException {
        try {
            execServiceChain(false);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INVALID_CERTIFICATE_EXCEPTION, e);
        }
    }

    @Override
    public void renew() {
        // 2.3. Client Certificate Reissuance
        //
        // An EST client can renew/rekey its existing client certificate by
        // submitting a re-enrollment request to an EST server.
        //
        // When the current EST client certificate can be used for TLS client
        // authentication (Section 3.3.2), the client presents this certificate
        // to the EST server for client authentication. When the to be reissued
        // EST client certificate cannot be used for TLS client authentication,
        // any of the authentication methods used for initial enrollment can be
        // used.
        //
        // For example, if the client has an alternative certificate issued by
        // the EST CA that can be used for TLS client authentication, then it
        // can be used.

        // Generally, the client will use an existing certificate for renew or
        // rekey operations. If the certificate to be renewed or rekeyed is
        // appropriate for the negotiated cipher suite, then the client MUST use
        // it for the TLS handshake, otherwise the client SHOULD use an
        // alternate certificate that is suitable for the cipher suite and
        // contains the same subject identity information. When requesting an
        // enroll operation, the client MAY use a client certificate issued by a
        // third party to authenticate itself.

        // 4.2.2. Simple Re-enrollment of Clients
        //
        // EST clients renew/rekey certificates with an HTTPS POST using the
        // operation path value of "/simplereenroll".
        //
        // A certificate request employs the same format as the "simpleenroll"
        // request, using the same HTTP content-type. The request Subject field
        // and SubjectAltName extension MUST be identical to the corresponding
        // fields in the certificate being renewed/rekeyed. The
        // ChangeSubjectName attribute, as defined in [RFC6402], MAY be included
        // in the CSR to request that these fields be changed in the new
        // certificate.
        //
        // If the Subject Public Key Info in the certification request is the
        // same as the current client certificate, then the EST server renews
        // the client certificate. If the public key information in the
        // certification request is different than the current client
        // certificate, then the EST server rekeys the client certificate.

    }

    @Override
    public void rekey() throws KuraException {
        // TODO Auto-generated method stub

    }

    private Certificate[] getCertificatesEntryAsX509Certificate(Supplier<TrustedCertificateEntry[]> entrySupplier) {
        TrustedCertificateEntry[] storedCaCerts = entrySupplier.get();
        Certificate[] certificates = new Certificate[0];
        if (storedCaCerts != null && storedCaCerts.length > 0) {
            certificates = Arrays.stream(storedCaCerts).map(TrustedCertificateEntry::getTrustedCertificate)
                    .toArray(Certificate[]::new);
        }
        return certificates;
    }

    @Override
    public CertStore getCACertificate() throws KuraException {
        try {
            return CertStore.getInstance("Collection", new CollectionCertStoreParameters(
                    Arrays.asList(getCertificatesEntryAsX509Certificate(this::getStoredCACerts))));
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INVALID_CERTIFICATE_EXCEPTION, e);
        }
    }

    @Override
    public void forceCACertificateRollover() {
        // TODO
    }

    @Override
    public boolean isEnrolled() {
        if (this.enrolled) {
            logger.info("System is enrolled");
        }
        return this.enrolled;
    }

    @Override
    public Certificate getClientCertificate() throws KuraException {
        try {
            TrustedCertificateEntry estClientCert = getESTClientCert();
            return estClientCert != null ? estClientCert.getTrustedCertificate() : null;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INVALID_CERTIFICATE_EXCEPTION, e);
        }

    }

    private synchronized void checkCerts() throws KuraException {
        this.enrolled = false;
        this.hasCaCerts = false;

        Object[] storedCACerts = this.getStoredCACerts();
        if (this.keystoreService != null && storedCACerts != null && storedCACerts.length > 0) {
            this.hasCaCerts = true;
        }

        if (this.keystoreService != null && this.getESTClientCert() != null) {
            this.enrolled = true;
        }

    }

    private boolean needBootstrap() {
        return !this.hasCaCerts || isCaCertsExpired();
    }

    private boolean isCaCertsExpired() {
        boolean isExpired = false;
        for (TrustedCertificateEntry entry : getStoredCACerts()) {
            X509Certificate x509cert = null;
            try {
                x509cert = toJavaX509Certificate(entry.getTrustedCertificate());
            } catch (Exception e) {
                logger.error("Unable to check certificate validity.");
            }
            try {
                if (x509cert != null) {
                    x509cert.checkValidity();
                }
            } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                logger.info("Ca certificate with serial number {} is not valid anymore", x509cert.getSerialNumber());
                isExpired = true;
                break;
            }
        }

        return isExpired;
    }

    private static Object[] readPemCertificates(String pemString) throws Exception {
        List<Object> certs = new ArrayList<>();
        try (Reader r = new StringReader(pemString); PemReader reader = new PemReader(r);) {

            PemObject o;

            while ((o = reader.readPemObject()) != null) {
                certs.add(new X509CertificateHolder(o.getContent()));
            }
        }
        return certs.toArray(new Object[certs.size()]);
    }

    class BCChannelBindingProvider implements ChannelBindingProvider {

        @Override
        public boolean canAccessChannelBinding(Socket sock) {
            return sock instanceof BCSSLSocket;
        }

        @Override
        public byte[] getChannelBinding(Socket sock, String binding) {
            BCSSLConnection bcon = ((BCSSLSocket) sock).getConnection();
            if (bcon != null) {
                return bcon.getChannelBinding(binding);
            }
            return null;
        }
    }

    private static Set<TrustAnchor> toTrustAnchor(Object[] oo) throws Exception {

        Set<TrustAnchor> out = new HashSet<>();
        for (Object o : oo) {
            out.add(new TrustAnchor(toJavaX509Certificate(o), null));
        }

        return out;
    }

    public static java.security.cert.X509Certificate toJavaX509Certificate(Object o) throws Exception {
        CertificateFactory fac = CertificateFactory.getInstance("X509");
        if (o instanceof X509CertificateHolder) {
            return (java.security.cert.X509Certificate) fac
                    .generateCertificate(new ByteArrayInputStream(((X509CertificateHolder) o).getEncoded()));
        } else if (o instanceof X509Certificate) {
            return (java.security.cert.X509Certificate) fac
                    .generateCertificate(new ByteArrayInputStream(((X509Certificate) o).getEncoded()));
        } else if (o instanceof java.security.cert.X509Certificate) {
            return (java.security.cert.X509Certificate) o;
        }
        throw new IllegalArgumentException(
                "Object not X509CertificateHolder, javax..X509Certificate or java...X509Certificate");
    }

    private static String x509CertificateToPem(final Certificate cert) throws IOException {
        final StringWriter writer = new StringWriter();
        final JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        pemWriter.writeObject(cert);
        pemWriter.flush();
        pemWriter.close();
        return writer.toString();
    }

    public <T> CompletableFuture<T> timeoutAfter(long timeout, TimeUnit unit) {
        CompletableFuture<T> result = new CompletableFuture<>();
        delayer.schedule(() -> result.completeExceptionally(new TimeoutException()), timeout, unit);
        return result;
    }

}
