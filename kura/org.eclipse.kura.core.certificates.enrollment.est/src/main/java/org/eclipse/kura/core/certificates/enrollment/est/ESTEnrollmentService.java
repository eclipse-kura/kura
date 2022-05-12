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
import java.net.MalformedURLException;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
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

    private static final String EST_CACERTS_ALIAS = "est-cacerts";
    private static final String EST_CLIENT_ALIAS = "est-client-certs";
    private static final String EST_PRIVATE_KEY_CERT = "est-private-key-certs";

    private static final String CLIENT_AUTH_ALIAS = "client-auth-certs";

    private static final boolean DO_NOT_RE_ENROLL = false;

    private CryptoService cryptoService;
    private KeystoreService keystoreService;

    private Optional<String> keystoreServicePid;

    private ESTService estService;

    private ESTEnrollmentServiceOptions estOptions;
    private boolean needBootstrap;

    private boolean enrolled;

    private PKCS10CertificationRequestBuilder csrBuilder;

    private ESTAuth auth;

    private ExecutorService enrollmentService = Executors.newSingleThreadExecutor();
    private ScheduledExecutorService delayer = Executors.newSingleThreadScheduledExecutor();

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
            this.estOptions = new ESTEnrollmentServiceOptions(properties);
            checkCerts();
            if (Boolean.TRUE.equals(this.estOptions.isEnabled())) {
                initESTService();
                if (!isEnrolled()) {
                    generateKeyPair();
                    createCSR();
                    createAuthorization();
                    performEnrollment(DO_NOT_RE_ENROLL);
                }

            }
        } catch (MalformedURLException e) {
            logger.error("Invalid EST Server URL", e);
        } catch (Exception e) {
            logger.error("Error configuring EST client", e);
        }
    }

    private void initESTService() throws Exception {

        Object truesteCACerts = null;

        if (needBootstrap()) {
            logger.info("Bootstrapping EST connection...");
            truesteCACerts = readPemCertificates(this.estOptions.getServer().getBootstrapCertificate());
        } else {
            // the ca certificate provided by the configuration is only needed for the bootstrapping
            truesteCACerts = getStoredCACerts();
        }

        buildESTService(truesteCACerts, getRevocationList());

        if (needBootstrap()) {

            logger.info("Downloading CA certificates...");
            CACertsResponse cacertsResponse = this.estService.getCACerts();
            if (cacertsResponse.isTrusted() && cacertsResponse.hasCertificates()) {
                TrustedCertificateEntry cacertsTrustedEntry = new TrustedCertificateEntry(
                        toJavaX509Certificate(cacertsResponse.getCertificateStore()));
                this.keystoreService.setEntry(EST_CACERTS_ALIAS, cacertsTrustedEntry);
                logger.info("CA certificates added in {} with alias {}", this.keystoreServicePid.get(),
                        EST_CACERTS_ALIAS);
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
            buildESTService(getStoredCACerts(), getRevocationList());

            logger.info("Bootstrap endend.");
        }
    }

    private void generateKeyPair() throws KuraException {

        AlgorithmParameterSpec algorithmParameter = null;

        switch (this.estOptions.getClient().getKeyPairAlgorithm()) {
        case "ECDSA":
            algorithmParameter = new ECGenParameterSpec(this.estOptions.getClient().getKeyPairAlgorithmParameter());
        case "RSA":
            algorithmParameter = new RSAKeyGenParameterSpec(
                    Integer.parseInt(this.estOptions.getClient().getKeyPairAlgorithmParameter()),
                    RSAKeyGenParameterSpec.F4);
        }

        this.keystoreService.createKeyPair(EST_PRIVATE_KEY_CERT, this.estOptions.getClient().getKeyPairAlgorithm(),
                algorithmParameter, this.estOptions.getClient().getSignerAlgorithm(),
                this.estOptions.getClient().getSubjectDN());

    }

    private void createCSR() throws KuraException, OperatorCreationException {

        X500Principal x509Principal = new X500Principal(this.estOptions.getClient().getSubjectDN());
        this.csrBuilder = this.keystoreService.getCSRAsPKCS10Builder(EST_PRIVATE_KEY_CERT, x509Principal);

    }

    private void createAuthorization() throws OperatorCreationException, KuraException {

        if (this.estOptions.getClient().getBasicAuthentication().isEnabled()) {
            String username = this.estOptions.getClient().getBasicAuthentication().getUsername();
            char[] passw = this.cryptoService
                    .decryptAes(this.estOptions.getClient().getBasicAuthentication().getPassword().getPassword());
            if (this.estOptions.getClient().getBasicAuthentication().isDigestEnabled()) {
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
            revocationList = this.keystoreService.getCRLs().toArray(revocationList);
        }
        return revocationList;
    }

    private Object getStoredCACerts() throws KuraException {
        Object truesteCACerts;
        Entry storedCaCerts = this.keystoreService.getEntry(EST_CACERTS_ALIAS);
        if (storedCaCerts != null && !(storedCaCerts instanceof TrustedCertificateEntry)) {
            throw new KuraException(KuraErrorCode.INVALID_CERTIFICATE_EXCEPTION);
        }

        truesteCACerts = storedCaCerts;
        return truesteCACerts;
    }

    private PrivateKeyEntry getESTKeyPair() throws KuraException {
        Entry storedKeyPair = this.keystoreService.getEntry(EST_PRIVATE_KEY_CERT);

        if (storedKeyPair != null && !(storedKeyPair instanceof PrivateKeyEntry)) {
            throw new KuraException(KuraErrorCode.INVALID_CERTIFICATE_EXCEPTION);
        }

        return (PrivateKeyEntry) storedKeyPair;
    }

    private PrivateKeyEntry getClientAuthKeyPair() throws KuraException {
        Entry storedKeyPair = this.keystoreService.getEntry(CLIENT_AUTH_ALIAS);

        if (storedKeyPair != null && !(storedKeyPair instanceof PrivateKeyEntry)) {
            throw new KuraException(KuraErrorCode.INVALID_CERTIFICATE_EXCEPTION);
        }

        return (PrivateKeyEntry) storedKeyPair;
    }

    private void buildESTService(Object truesteCACerts, CRL[] revocationList) throws Exception {
        JsseESTServiceBuilder estServiceBuilder = new JsseESTServiceBuilder(
                this.estOptions.getServer().getUrl().toString(),
                JcaJceUtils.getCertPathTrustManager(toTrustAnchor(truesteCACerts), revocationList));

        estServiceBuilder.withProvider("BCJSSE");

        if (Boolean.TRUE.equals(this.estOptions.getClient().isProofOfPossessionEnabled())) {
            estServiceBuilder.withChannelBindingProvider(new BCChannelBindingProvider());
        }

        if (this.estOptions.getClient().isTlsAuthenticationEnabled()) {
            KeyStore clientKeystore = KeyStore.getInstance("JKS");
            clientKeystore.load(null);

            clientKeystore.setEntry(CLIENT_AUTH_ALIAS, getClientAuthKeyPair(),
                    new PasswordProtection(DEFAULT_KEYSTORE_PASSW));

            KeyManager[] keyManager = JcaJceUtils.createKeyManagerFactory(KeyManagerFactory.getDefaultAlgorithm(), null,
                    clientKeystore, DEFAULT_KEYSTORE_PASSW).getKeyManagers();

            estServiceBuilder.withKeyManagers(keyManager);
        }

        this.estService = estServiceBuilder.withTimeout(60000).build();
    }

    private void performEnrollment(boolean reEnroll) {

        CompletableFuture.supplyAsync(this.enrollmentTask(reEnroll), this.enrollmentService).acceptEither(
                this.timeoutAfter(ENROLLMENT_PROCESS_TIMEOUT, TimeUnit.MILLISECONDS), this.processEnrollmentResponse());

    }

    private Consumer<? super EnrollmentResponse> processEnrollmentResponse() {
        return (EnrollmentResponse response) -> {
            if (response != null) {
                Store<X509CertificateHolder> certStore = response.getStore();
                try {
                    this.keystoreService.getKeyStore().setCertificateEntry(EST_CLIENT_ALIAS,
                            toJavaX509Certificate(certStore));
                } catch (Exception e) {
                    logger.error("Unable to store client cert with alias {} in keystore {}", EST_CLIENT_ALIAS,
                            this.keystoreServicePid.get());
                }
            }
        };
    }

    private Supplier<EnrollmentResponse> enrollmentTask(boolean reEnroll) {

        return () -> {
            logger.info("Starting system enrollment with: {}", this.estOptions.getServer().getUrl());
            ContentSigner signer;
            EnrollmentResponse enrollmentResponse = null;
            try {
                signer = new JcaContentSignerBuilder(this.estOptions.getClient().getSignerAlgorithm())
                        .build(getESTKeyPair().getPrivateKey());
                do {
                    if (this.estOptions.getClient().isProofOfPossessionEnabled()) {
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

                } while (enrollmentResponse.isCompleted());

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
    }

    @Override
    public void enroll() {
        // TODO Auto-generated method stub

    }

    @Override
    public void renew() {
        // TODO Auto-generated method stub

    }

    @Override
    public Certificate getCACertificate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void forceCACertificateRollover() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isEnrolled() {
        return this.enrolled;
    }

    @Override
    public Certificate getClientCertificate() {
        // TODO Auto-generated method stub
        return null;
    }

    private void checkCerts() throws KuraException {
        if (this.keystoreService != null && this.keystoreService.getEntry(EST_CACERTS_ALIAS) != null) {
            this.needBootstrap = false;
        }

        if (this.keystoreService != null && this.keystoreService.getEntry(EST_CLIENT_ALIAS) != null) {
            this.enrolled = true;
        }

        this.enrolled = false;
        this.needBootstrap = true;

    }

    private boolean needBootstrap() {
        return this.needBootstrap;
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

    private static Set<TrustAnchor> toTrustAnchor(Object... oo) throws Exception {
        CertificateFactory fac = CertificateFactory.getInstance("X509");
        Set<TrustAnchor> out = new HashSet<>();
        for (Object o : oo) {
            if (o instanceof X509CertificateHolder) {
                out.add(new TrustAnchor((java.security.cert.X509Certificate) fac
                        .generateCertificate(new ByteArrayInputStream(((X509CertificateHolder) o).getEncoded())),
                        null));
            } else if (o instanceof X509Certificate) {
                out.add(new TrustAnchor(
                        (java.security.cert.X509Certificate) fac
                                .generateCertificate(new ByteArrayInputStream(((X509Certificate) o).getEncoded())),
                        null));
            } else if (o instanceof java.security.cert.X509Certificate) {
                out.add(new TrustAnchor((java.security.cert.X509Certificate) o, null));
            } else if (o instanceof TrustAnchor) {
                out.add((TrustAnchor) o);
            } else {
                throw new IllegalArgumentException(
                        "Could not convert " + o.getClass().getName() + " to X509Certificate");
            }
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
        CompletableFuture<T> result = new CompletableFuture<T>();
        delayer.schedule(() -> result.completeExceptionally(new TimeoutException()), timeout, unit);
        return result;
    }

}
