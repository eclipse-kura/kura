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
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.keystore;

import static java.util.Objects.isNull;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.keystore.crl.CRLManager;
import org.eclipse.kura.core.keystore.crl.CRLManager.CRLVerifier;
import org.eclipse.kura.core.keystore.crl.CRLManagerOptions;
import org.eclipse.kura.core.keystore.crl.StoredCRL;
import org.eclipse.kura.security.keystore.KeystoreChangedEvent;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseKeystoreService implements KeystoreService, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(BaseKeystoreService.class);

    protected static final String NULL_INPUT_PARAMS_MESSAGE = "Input parameters cannot be null!";
    protected static final String KURA_SERVICE_PID = "kura.service.pid";
    protected static final String PEM_CERTIFICATE_REQUEST_TYPE = "CERTIFICATE REQUEST";

    protected EventAdmin eventAdmin;

    protected Optional<CRLManager> crlManager = Optional.empty();

    protected String ownPid;

    protected ComponentContext componentContext;

    private CRLManagerOptions crlManagerOptions;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    protected abstract KeystoreInstance loadKeystore() throws KuraException;

    protected abstract void saveKeystore(KeystoreInstance keystore)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException;

    protected abstract String getCrlStorePath();

    @Override
    public KeyStore getKeyStore() throws KuraException {

        return loadKeystore().getKeystore();
    }

    public void activate(ComponentContext context, Map<String, Object> properties) {
        this.componentContext = context;

        this.ownPid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
        this.crlManagerOptions = new CRLManagerOptions(properties);

        updateCRLManager(this.crlManagerOptions);
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Bundle {} is updating!", properties.get(KURA_SERVICE_PID));

        final CRLManagerOptions newCRLManagerOptions = new CRLManagerOptions(properties);

        if (!this.crlManagerOptions.equals(newCRLManagerOptions)) {
            this.crlManagerOptions = newCRLManagerOptions;

            updateCRLManager(newCRLManagerOptions);
        }
    }

    public void deactivate() {
        shutdownCRLManager();
    }

    @Override
    public Entry getEntry(String alias) throws KuraException {
        if (isNull(alias)) {
            throw new IllegalArgumentException("Key Pair alias cannot be null!");
        }
        KeystoreInstance ks = loadKeystore();

        try {
            if (ks.getKeystore().entryInstanceOf(alias, PrivateKeyEntry.class)
                    || ks.getKeystore().entryInstanceOf(alias, SecretKeyEntry.class)) {
                return ks.getKeystore().getEntry(alias, new PasswordProtection(ks.getPassword()));
            } else {
                return ks.getKeystore().getEntry(alias, null);
            }
        } catch (GeneralSecurityException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, e, "Failed to get the entry " + alias);
        }
    }

    @Override
    public void setEntry(String alias, Entry entry) throws KuraException {
        if (isNull(alias) || alias.trim().isEmpty() || isNull(entry)) {
            throw new IllegalArgumentException("Input cannot be null or empty!");
        }
        KeystoreInstance ks = loadKeystore();

        final ProtectionParameter protectionParameter;

        if (entry instanceof TrustedCertificateEntry) {
            protectionParameter = null;
        } else {
            protectionParameter = new PasswordProtection(ks.getPassword());
        }
        try {
            ks.getKeystore().setEntry(alias, entry, protectionParameter);
            saveKeystore(ks);
            if (!tryAddToCrlManagement(entry)) {
                postChangedEvent();
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, e, "Failed to set the entry " + alias);
        }

    }

    @Override
    public Map<String, Entry> getEntries() throws KuraException {
        Map<String, Entry> result = new HashMap<>();

        KeyStore ks = getKeyStore();
        try {
            List<String> aliases = Collections.list(ks.aliases());

            for (String alias : aliases) {
                Entry tempEntry = getEntry(alias);
                result.put(alias, tempEntry);
            }
            return result;
        } catch (GeneralSecurityException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, e, "Failed to get the entries");
        }
    }

    @Override
    public void deleteEntry(String alias) throws KuraException {
        if (isNull(alias)) {
            throw new IllegalArgumentException("Alias cannot be null!");
        }
        final Optional<Entry> currentEntry = Optional.ofNullable(getEntry(alias));

        if (!currentEntry.isPresent()) {
            return;
        }

        KeystoreInstance ks = loadKeystore();
        try {
            ks.getKeystore().deleteEntry(alias);
            saveKeystore(ks);
            boolean crlStoreChanged = false;
            crlStoreChanged = tryRemoveFromCrlManagement(currentEntry.get());
            if (!crlStoreChanged) {
                postChangedEvent();
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, e, "Failed to delete entry " + alias);
        }
    }

    @Override
    public List<KeyManager> getKeyManagers(String algorithm) throws KuraException {
        if (isNull(algorithm)) {
            throw new IllegalArgumentException("Algorithm cannot be null!");
        }
        KeystoreInstance ks = loadKeystore();
        try {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks.getKeystore(), ks.getPassword());

            return Arrays.asList(kmf.getKeyManagers());
        } catch (GeneralSecurityException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, e,
                    "Failed to get the key managers for algorithm " + algorithm);
        }
    }

    @Override
    public void createKeyPair(String alias, String algorithm, int keySize, String signatureAlgorithm, String attributes)
            throws KuraException {
        createKeyPair(alias, algorithm, keySize, signatureAlgorithm, attributes, new SecureRandom());
    }

    @Override
    public void createKeyPair(String alias, String algorithm, int keySize, String signatureAlgorithm, String attributes,
            SecureRandom secureRandom) throws KuraException {
        if (isNull(algorithm) || algorithm.trim().isEmpty() || isNull(secureRandom) || isNull(alias)
                || isNull(attributes) || attributes.trim().isEmpty() || isNull(signatureAlgorithm)
                || signatureAlgorithm.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameters cannot be null or empty!");
        }
        KeyPair keyPair;
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm, "BC");
            keyGen.initialize(keySize, secureRandom);
            keyPair = keyGen.generateKeyPair();
            setEntry(alias, new PrivateKeyEntry(keyPair.getPrivate(),
                    generateCertificateChain(keyPair, signatureAlgorithm, attributes)));
        } catch (GeneralSecurityException | OperatorCreationException e) {
            logger.error("Error occured. Exception: {}.", e.getClass());
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
    }

    @Override
    public void createKeyPair(String alias, String algorithm, AlgorithmParameterSpec algorithmParameter,
            String signatureAlgorithm, String attributes, SecureRandom secureRandom) throws KuraException {

        if (isNull(algorithm) || algorithm.trim().isEmpty() || isNull(secureRandom) || isNull(alias)
                || isNull(attributes) || attributes.trim().isEmpty() || isNull(signatureAlgorithm)
                || signatureAlgorithm.trim().isEmpty() || isNull(algorithmParameter)) {
            throw new IllegalArgumentException("Parameters cannot be null or empty!");
        }
        KeyPair keyPair;
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm, "BC");
            keyGen.initialize(algorithmParameter, secureRandom);
            keyPair = keyGen.generateKeyPair();
            setEntry(alias, new PrivateKeyEntry(keyPair.getPrivate(),
                    generateCertificateChain(keyPair, signatureAlgorithm, attributes)));
        } catch (GeneralSecurityException | OperatorCreationException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

    }

    @Override
    public void createKeyPair(String alias, String algorithm, AlgorithmParameterSpec algorithmParameter,
            String signatureAlgorithm, String attributes) throws KuraException {
        createKeyPair(alias, algorithm, algorithmParameter, signatureAlgorithm, attributes, new SecureRandom());

    }

    @Override
    public String getCSR(KeyPair keypair, X500Principal principal, String signerAlg) throws KuraException {

        if (isNull(principal) || isNull(keypair) || isNull(signerAlg) || signerAlg.trim().isEmpty()) {
            throw new IllegalArgumentException(NULL_INPUT_PARAMS_MESSAGE);
        }

        try (StringWriter str = new StringWriter(); JcaPEMWriter pemWriter = new JcaPEMWriter(str);) {
            ContentSigner signer = new JcaContentSignerBuilder(signerAlg).build(keypair.getPrivate());
            PKCS10CertificationRequest csr = getCSRAsPKCS10Builder(keypair, principal).build(signer);

            PemObject pemCSR = new PemObject(PEM_CERTIFICATE_REQUEST_TYPE, csr.getEncoded());

            pemWriter.writeObject(pemCSR);
            pemWriter.flush();
            return str.toString();
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR, e, "Failed to get CSR");
        } catch (OperatorCreationException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, e, "Failed to get CSR");
        }
    }

    @Override
    public String getCSR(String alias, X500Principal principal, String signerAlg) throws KuraException {
        if (isNull(principal) || isNull(alias) || alias.trim().isEmpty() || isNull(signerAlg)
                || signerAlg.trim().isEmpty()) {
            throw new IllegalArgumentException(NULL_INPUT_PARAMS_MESSAGE);
        }

        Entry entry = getEntry(alias);
        if (entry == null) {
            throw new KuraException(KuraErrorCode.NOT_FOUND);
        }
        if (!(entry instanceof PrivateKeyEntry)) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
        PrivateKey privateKey = ((PrivateKeyEntry) entry).getPrivateKey();
        PublicKey publicKey = ((PrivateKeyEntry) entry).getCertificate().getPublicKey();
        KeyPair keyPair = new KeyPair(publicKey, privateKey);
        return getCSR(keyPair, principal, signerAlg);
    }

    protected PKCS10CertificationRequestBuilder getCSRAsPKCS10Builder(KeyPair keyPair, X500Principal principal) {
        if (isNull(principal) || isNull(keyPair)) {
            throw new IllegalArgumentException(NULL_INPUT_PARAMS_MESSAGE);
        }
        return new JcaPKCS10CertificationRequestBuilder(principal, keyPair.getPublic());

    }

    @Override
    public List<String> getAliases() throws KuraException {
        KeyStore ks = getKeyStore();
        try {
            return Collections.list(ks.aliases());
        } catch (GeneralSecurityException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, e, "Failed to get aliases");
        }
    }

    @Override
    public Collection<CRL> getCRLs() {

        final Optional<CRLManager> currentCRLManager = this.crlManager;

        if (!currentCRLManager.isPresent()) {
            return Collections.emptyList();
        } else {
            return new ArrayList<>(currentCRLManager.get().getCrls());
        }

    }

    @Override
    public CertStore getCRLStore() throws KuraException {
        final Optional<CRLManager> currentCRLManager = this.crlManager;

        try {
            if (!currentCRLManager.isPresent()) {
                return CertStore.getInstance("Collection", new CollectionCertStoreParameters());
            } else {
                return currentCRLManager.get().getCertStore();
            }
        } catch (final Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
        }
    }

    @Override
    public void addCRL(X509CRL crl) throws KuraException {
        this.crlManager.ifPresent(manager -> {
            StoredCRL storedCRL = new StoredCRL(Collections.emptySet(), crl);
            manager.getCRLStore().storeCRL(storedCRL);
        });
    }

    protected void postChangedEvent() {
        this.eventAdmin.postEvent(new KeystoreChangedEvent(ownPid));
    }

    protected X509Certificate[] generateCertificateChain(KeyPair keyPair, String signatureAlgorithm, String attributes)
            throws OperatorCreationException, CertificateException {
        Provider bcProvider = new BouncyCastleProvider();
        Security.addProvider(bcProvider);
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        X500Name dnName = new X500Name(attributes);
        // Use the timestamp as serial number
        BigInteger certSerialNumber = new BigInteger(Long.toString(now));
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.YEAR, 1);
        Date endDate = calendar.getTime();

        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(dnName, certSerialNumber, startDate,
                endDate, dnName, subjectPublicKeyInfo);
        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).setProvider(bcProvider)
                .build(keyPair.getPrivate());
        X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

        return new X509Certificate[] { new JcaX509CertificateConverter().getCertificate(certificateHolder) };
    }

    protected Optional<X509Certificate> extractCertificate(final Entry entry) {
        if (!(entry instanceof TrustedCertificateEntry)) {
            return Optional.empty();
        }

        final TrustedCertificateEntry trustedCertificateEntry = (TrustedCertificateEntry) entry;
        final Certificate certificate = trustedCertificateEntry.getTrustedCertificate();

        if (!(certificate instanceof X509Certificate)) {
            return Optional.empty();
        } else {
            return Optional.of((X509Certificate) certificate);
        }

    }

    protected boolean tryAddToCrlManagement(final Entry entry) {
        final Optional<X509Certificate> certificate = extractCertificate(entry);
        final Optional<CRLManager> currentCrlManager = this.crlManager;

        if (certificate.isPresent() && currentCrlManager.isPresent()) {
            return currentCrlManager.get().addTrustedCertificate(certificate.get());
        } else {
            return false;
        }
    }

    protected boolean tryRemoveFromCrlManagement(final Entry entry) {
        final Optional<X509Certificate> certificate = extractCertificate(entry);
        final Optional<CRLManager> currentCrlManager = this.crlManager;

        if (certificate.isPresent() && currentCrlManager.isPresent()) {
            return currentCrlManager.get().removeTrustedCertificate(certificate.get());
        } else {
            return false;
        }
    }

    protected void updateCRLManager(final CRLManagerOptions newCRLManagerOptions) {
        shutdownCRLManager();

        if (this.crlManagerOptions.isCrlManagementEnabled()) {

            final CRLManager currentCRLManager = new CRLManager(
                    this.crlManagerOptions.getStoreFile().orElseGet(() -> new File(getCrlStorePath())), 5000,
                    newCRLManagerOptions.getCrlCheckIntervalMs(), newCRLManagerOptions.getCrlUpdateIntervalMs(),
                    getCRLVerifier(newCRLManagerOptions));

            currentCRLManager.setListener(Optional.of(this::postChangedEvent));

            for (final URI uri : newCRLManagerOptions.getCrlURIs()) {
                currentCRLManager.addDistributionPoint(Collections.singleton(uri));
            }

            try {
                for (final Entry e : getEntries().values()) {
                    if (!(e instanceof TrustedCertificateEntry)) {
                        continue;
                    }

                    final TrustedCertificateEntry certEntry = (TrustedCertificateEntry) e;

                    final Certificate cert = certEntry.getTrustedCertificate();

                    if (cert instanceof X509Certificate) {
                        currentCRLManager.addTrustedCertificate((X509Certificate) cert);
                    }
                }

            } catch (final Exception e) {
                logger.warn("failed to add current trusted certificates to CRL manager", e);
            }

            this.crlManager = Optional.of(currentCRLManager);
        }
    }

    protected CRLVerifier getCRLVerifier(final CRLManagerOptions options) {
        if (!options.isCRLVerificationEnabled()) {
            return crl -> true;
        }

        return crl -> {
            try {
                for (final Entry e : getEntries().values()) {
                    if (!(e instanceof TrustedCertificateEntry)) {
                        continue;
                    }

                    final TrustedCertificateEntry trustedCertEntry = (TrustedCertificateEntry) e;

                    if (verifyCRL(crl, trustedCertEntry)) {
                        return true;
                    }
                }
                return false;
            } catch (final Exception e) {
                logger.warn("Exception verifying CRL", e);
                return false;
            }
        };
    }

    protected void shutdownCRLManager() {
        if (this.crlManager.isPresent()) {
            this.crlManager.get().close();
            this.crlManager = Optional.empty();
        }
    }

    protected boolean verifyCRL(X509CRL crl, final TrustedCertificateEntry trustedCertEntry) {
        try {
            crl.verify(trustedCertEntry.getTrustedCertificate().getPublicKey());
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

}