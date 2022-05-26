/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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
import static org.eclipse.kura.core.keystore.KeystoreServiceOptions.KEY_KEYSTORE_PASSWORD;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.security.UnrecoverableEntryException;
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.keystore.crl.CRLManager;
import org.eclipse.kura.core.keystore.crl.CRLManager.CRLVerifier;
import org.eclipse.kura.core.keystore.crl.CRLManagerOptions;
import org.eclipse.kura.core.keystore.crl.StoredCRL;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.security.keystore.KeystoreChangedEvent;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesystemKeystoreServiceImpl implements KeystoreService, ConfigurableComponent {

    private static final String KURA_SERVICE_PID = "kura.service.pid";
    private static final String PEM_CERTIFICATE_REQUEST_TYPE = "CERTIFICATE REQUEST";

    private static final Logger logger = LoggerFactory.getLogger(FilesystemKeystoreServiceImpl.class);

    private ComponentContext componentContext;

    private CryptoService cryptoService;
    private ConfigurationService configurationService;
    private EventAdmin eventAdmin;

    private KeystoreServiceOptions keystoreServiceOptions;
    private CRLManagerOptions crlManagerOptions;

    private Optional<CRLManager> crlManager = Optional.empty();

    private ScheduledExecutorService selfUpdaterExecutor;
    private ScheduledFuture<?> selfUpdaterFuture;

    private String ownPid;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    public void activate(ComponentContext context, Map<String, Object> properties) {
        logger.info("Bundle {} is starting!", properties.get(KURA_SERVICE_PID));
        this.componentContext = context;

        this.ownPid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
        this.keystoreServiceOptions = new KeystoreServiceOptions(properties, this.cryptoService);
        this.selfUpdaterExecutor = Executors.newSingleThreadScheduledExecutor();

        if (!keystoreExists(this.keystoreServiceOptions.getKeystorePath())) {
            try {
                createKeystore(this.keystoreServiceOptions);
            } catch (Exception e) {
                logger.error("Keystore file creation failed", e);
            }
        }

        if (this.keystoreServiceOptions.needsRandomPassword()) {
            setRandomPassword();
        }

        this.crlManagerOptions = new CRLManagerOptions(properties);

        updateCRLManager(this.crlManagerOptions);

        logger.info("Bundle {} has started!", properties.get(KURA_SERVICE_PID));
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Bundle {} is updating!", properties.get(KURA_SERVICE_PID));
        KeystoreServiceOptions newOptions = new KeystoreServiceOptions(properties, this.cryptoService);

        if (!this.keystoreServiceOptions.equals(newOptions)) {
            logger.info("Perform update...");

            if (!this.keystoreServiceOptions.getKeystorePath().equals(newOptions.getKeystorePath())) {
                updateKeystorePath(newOptions);
            } else {
                checkAndUpdateKeystorePassword(newOptions);
            }

            this.keystoreServiceOptions = new KeystoreServiceOptions(properties, this.cryptoService);

        }

        final CRLManagerOptions newCRLManagerOptions = new CRLManagerOptions(properties);

        if (!this.crlManagerOptions.equals(newCRLManagerOptions)) {
            this.crlManagerOptions = newCRLManagerOptions;

            updateCRLManager(newCRLManagerOptions);
        }

        logger.info("Bundle {} has updated!", properties.get(KURA_SERVICE_PID));
    }

    private void checkAndUpdateKeystorePassword(final KeystoreServiceOptions options) {
        try {
            final LoadedKeystore ks = loadKeystore(this.keystoreServiceOptions);

            final char[] configPassword = options.getKeystorePassword(cryptoService);

            if (!Arrays.equals(ks.password, configPassword)) {
                setKeystorePassword(ks, configPassword);
            }

        } catch (final Exception e) {
            logger.warn("failed to load or update keystore password", e);
        }
    }

    public void deactivate() {
        logger.info("Bundle {} is deactivating!", this.keystoreServiceOptions.getProperties().get(KURA_SERVICE_PID));

        if (this.selfUpdaterFuture != null && !this.selfUpdaterFuture.isDone()) {

            logger.info("Self updater task running. Stopping it");

            this.selfUpdaterFuture.cancel(true);
        }

        shutdownCRLManager();
    }

    private boolean keystoreExists(String keystorePath) {
        return keystorePath != null && new File(keystorePath).isFile();
    }

    private void createKeystore(KeystoreServiceOptions options) throws Exception {
        String keystorePath = options.getKeystorePath();
        char[] passwordChar = options.getKeystorePassword(this.cryptoService);
        if (keystorePath == null) {
            return;
        }
        File fKeyStore = new File(keystorePath);
        if (!fKeyStore.createNewFile()) {
            logger.error("Keystore file already exists at location {}", keystorePath);
            throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, "keystore.path", keystorePath,
                    "file already exists");
        }

        // Immediately save the keystore with the default password to allow to be loaded with the default password.
        try (OutputStream os = new FileOutputStream(fKeyStore)) {
            KeyStore newKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            newKeystore.load(null, passwordChar);
            newKeystore.store(os, passwordChar);
            os.flush();
        } catch (Exception e) {
            logger.error("Unable to load and store the keystore", e);
            throw e;
        }

        setKeystorePassword(this.loadKeystore(options), passwordChar);
    }

    private void updateKeystorePath(KeystoreServiceOptions newOptions) {
        if (!keystoreExists(newOptions.getKeystorePath())) {
            try {
                createKeystore(newOptions);
            } catch (Exception e) {
                logger.error("Keystore file creation failed", e);
            }
        }

        try {
            loadKeystore(newOptions);
        } catch (final Exception e) {
            logger.warn("Keystore {} not accessible!", newOptions.getKeystorePath());
        }
    }

    private void setRandomPassword() {

        try {
            final LoadedKeystore keystore = loadKeystore(this.keystoreServiceOptions);

            char[] newPassword = new BigInteger(160, new SecureRandom()).toString(32).toCharArray();

            setKeystorePassword(keystore, newPassword);

            Map<String, Object> props = new HashMap<>(this.keystoreServiceOptions.getProperties());
            props.put(KEY_KEYSTORE_PASSWORD, new String(this.cryptoService.encryptAes(newPassword)));
            this.keystoreServiceOptions = new KeystoreServiceOptions(props, this.cryptoService);

            updatePasswordInConfigService(newPassword);
        } catch (Exception e) {
            logger.warn("Keystore password change failed", e);
        }
    }

    private synchronized void saveKeystore(LoadedKeystore ks, char[] keyStorePassword)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        try (FileOutputStream tsOutStream = new FileOutputStream(ks.path)) {
            ks.keystore.store(tsOutStream, keyStorePassword);
        }
    }

    private void updatePasswordInConfigService(char[] newPassword) {
        final String pid = this.keystoreServiceOptions.getPid();

        Map<String, Object> props = new HashMap<>();
        props.putAll(this.keystoreServiceOptions.getProperties());
        props.put(KeystoreServiceOptions.KEY_KEYSTORE_PATH, this.keystoreServiceOptions.getKeystorePath());
        props.put(KeystoreServiceOptions.KEY_KEYSTORE_PASSWORD, new Password(newPassword));
        props.put(KeystoreServiceOptions.KEY_RANDOMIZE_PASSWORD, false);

        this.selfUpdaterFuture = this.selfUpdaterExecutor.scheduleAtFixedRate(() -> {
            try {
                if (this.componentContext.getServiceReference() != null
                        && this.configurationService.getComponentConfiguration(pid) != null
                        && this.configurationService.getComponentConfiguration(pid).getDefinition() != null) {
                    this.configurationService.updateConfiguration(pid, props);
                    throw new KuraRuntimeException(KuraErrorCode.CONFIGURATION_SNAPSHOT_TAKING,
                            "Updated. The task will be terminated.");
                } else {
                    logger.info("No service or configuration available yet.");
                }
            } catch (KuraException e) {
                logger.warn("Cannot get/update configuration for pid: {}", pid, e);
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    private synchronized void setKeystorePassword(LoadedKeystore ks, char[] password) {
        try {
            updateKeyEntriesPasswords(ks, password);
            saveKeystore(ks, password);

            this.cryptoService.setKeyStorePassword(ks.path, password);
        } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | UnrecoverableEntryException
                | IOException e) {
            logger.warn("Failed to change keystore password");
        } catch (KuraException e) {
            logger.warn("Failed to persist keystore password");
        }
    }

    private static void updateKeyEntriesPasswords(LoadedKeystore ks, char[] newPassword)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException {
        Enumeration<String> aliases = ks.keystore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (ks.keystore.isKeyEntry(alias)) {
                PasswordProtection oldPP = new PasswordProtection(ks.password);
                Entry entry = ks.keystore.getEntry(alias, oldPP);
                PasswordProtection newPP = new PasswordProtection(newPassword);
                ks.keystore.setEntry(alias, entry, newPP);
            }
        }
    }

    @Override
    public synchronized KeyStore getKeyStore() throws KuraException {
        return loadKeystore(this.keystoreServiceOptions).keystore;
    }

    @Override
    public Entry getEntry(String alias) throws KuraException {
        if (isNull(alias)) {
            throw new IllegalArgumentException("Key Pair alias cannot be null!");
        }
        LoadedKeystore ks = loadKeystore(this.keystoreServiceOptions);

        try {
            if (ks.keystore.entryInstanceOf(alias, PrivateKeyEntry.class)
                    || ks.keystore.entryInstanceOf(alias, SecretKeyEntry.class)) {
                return ks.keystore.getEntry(alias, new PasswordProtection(ks.password));
            } else {
                return ks.keystore.getEntry(alias, null);
            }
        } catch (GeneralSecurityException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, e, "Failed to get the entry " + alias);
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
    public String getCSR(KeyPair keypair, X500Principal principal, String signerAlg) throws KuraException {

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
            throw new IllegalArgumentException("Input parameters cannot be null!");
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

    @Override
    public PKCS10CertificationRequestBuilder getCSRAsPKCS10Builder(KeyPair keyPair, X500Principal principal)
            throws KuraException {
        if (isNull(principal) || isNull(keyPair)) {
            throw new IllegalArgumentException("Input parameters cannot be null!");
        }
        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(principal,
                keyPair.getPublic());

        return p10Builder;
    }

    @Override
    public PKCS10CertificationRequestBuilder getCSRAsPKCS10Builder(String alias, X500Principal principal)
            throws KuraException {
        if (isNull(principal) || isNull(alias) || alias.trim().isEmpty()) {
            throw new IllegalArgumentException("Input parameters cannot be null!");
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
        return getCSRAsPKCS10Builder(keyPair, principal);
    }

    @Override
    public List<KeyManager> getKeyManagers(String algorithm) throws KuraException {
        if (isNull(algorithm)) {
            throw new IllegalArgumentException("Algorithm cannot be null!");
        }
        LoadedKeystore ks = loadKeystore(this.keystoreServiceOptions);
        try {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks.keystore, ks.password);

            return Arrays.asList(kmf.getKeyManagers());
        } catch (GeneralSecurityException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, e,
                    "Failed to get the key managers for algorithm " + algorithm);
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

        LoadedKeystore ks = loadKeystore(this.keystoreServiceOptions);
        try {
            ks.keystore.deleteEntry(alias);
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

    private void saveKeystore(LoadedKeystore ks)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        try (FileOutputStream tsOutStream = new FileOutputStream(this.keystoreServiceOptions.getKeystorePath());) {
            ks.keystore.store(tsOutStream, ks.password);
        }
    }

    @Override
    public void setEntry(String alias, Entry entry) throws KuraException {
        if (isNull(alias) || alias.trim().isEmpty() || isNull(entry)) {
            throw new IllegalArgumentException("Input cannot be null or empty!");
        }
        LoadedKeystore ks = loadKeystore(keystoreServiceOptions);

        final ProtectionParameter protectionParameter;

        if (entry instanceof TrustedCertificateEntry) {
            protectionParameter = null;
        } else {
            protectionParameter = new PasswordProtection(ks.password);
        }
        try {
            ks.keystore.setEntry(alias, entry, protectionParameter);
            saveKeystore(ks);
            if (!tryAddToCrlManagement(entry)) {
                postChangedEvent();
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, e, "Failed to set the entry " + alias);
        }
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

    public X509Certificate[] generateCertificateChain(KeyPair keyPair, String signatureAlgorithm, String attributes)
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

    private void updateCRLManager(final CRLManagerOptions newCRLManagerOptions) {
        shutdownCRLManager();

        if (this.crlManagerOptions.isCrlManagementEnabled()) {

            final CRLManager currentCRLManager = new CRLManager(
                    this.crlManagerOptions.getStoreFile()
                            .orElseGet(() -> new File(this.keystoreServiceOptions.getKeystorePath() + ".crl")),
                    5000, newCRLManagerOptions.getCrlCheckIntervalMs(), newCRLManagerOptions.getCrlUpdateIntervalMs(),
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

    private CRLVerifier getCRLVerifier(final CRLManagerOptions options) {
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

    private Optional<X509Certificate> extractCertificate(final Entry entry) {
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

    private boolean tryAddToCrlManagement(final Entry entry) {
        final Optional<X509Certificate> certificate = extractCertificate(entry);
        final Optional<CRLManager> currentCrlManager = this.crlManager;

        if (certificate.isPresent() && currentCrlManager.isPresent()) {
            return currentCrlManager.get().addTrustedCertificate(certificate.get());
        } else {
            return false;
        }
    }

    private boolean tryRemoveFromCrlManagement(final Entry entry) {
        final Optional<X509Certificate> certificate = extractCertificate(entry);
        final Optional<CRLManager> currentCrlManager = this.crlManager;

        if (certificate.isPresent() && currentCrlManager.isPresent()) {
            return currentCrlManager.get().removeTrustedCertificate(certificate.get());
        } else {
            return false;
        }
    }

    private boolean verifyCRL(X509CRL crl, final TrustedCertificateEntry trustedCertEntry) {
        try {
            crl.verify(trustedCertEntry.getTrustedCertificate().getPublicKey());
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    private void shutdownCRLManager() {
        if (this.crlManager.isPresent()) {
            this.crlManager.get().close();
            this.crlManager = Optional.empty();
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

    private void postChangedEvent() {
        this.eventAdmin.postEvent(new KeystoreChangedEvent(this.ownPid));
    }

    private synchronized LoadedKeystore loadKeystore(final KeystoreServiceOptions options) throws KuraException {
        final List<char[]> passwords = new ArrayList<>(2);

        passwords.add(options.getKeystorePassword(cryptoService));

        char[] passwordInCrypto = null;

        try {
            passwordInCrypto = this.cryptoService.getKeyStorePassword(options.getKeystorePath());
            if (passwordInCrypto != null) {
                passwords.add(passwordInCrypto);
            }
        } catch (final Exception e) {
            logger.debug("failed to retrieve password", e);
        }

        final LoadedKeystore result = new KeystoreLoader(options.getKeystorePath(), passwords).loadKeystore();

        if (!Arrays.equals(passwordInCrypto, result.password)) {
            this.cryptoService.setKeyStorePassword(result.path, result.password);
        }

        return result;
    }

    private static class KeystoreLoader {

        private final String path;
        private final List<char[]> passwords;

        KeystoreLoader(final String path, final List<char[]> passwords) {
            this.path = path;
            this.passwords = passwords;
        }

        private KeyStore loadKeystore(final String path, final char[] password)
                throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
            KeyStore ks = null;
            try (InputStream tsReadStream = new FileInputStream(path)) {
                ks = KeyStore.getInstance(KeyStore.getDefaultType());
                char[] keystorePassword = password;

                ks.load(tsReadStream, keystorePassword);
            }

            return ks;
        }

        LoadedKeystore loadKeystore() throws KuraException {

            for (final char[] password : this.passwords) {
                try {
                    final KeyStore keyStore = loadKeystore(path, password);

                    return new LoadedKeystore(keyStore, password, this.path);
                } catch (final Exception e) {
                    logger.debug("failed to load keystore", e);
                }
            }

            throw new KuraException(KuraErrorCode.BAD_REQUEST, "Failed to get the KeyStore");

        }
    }

    private static class LoadedKeystore {

        private final KeyStore keystore;
        private final char[] password;
        private final String path;

        public LoadedKeystore(final KeyStore keystore, final char[] password, final String path) {
            this.keystore = keystore;
            this.password = password;
            this.path = path;
        }

    }

    @Override
    public void addCRL(X509CRL crl) throws KuraException {
        this.crlManager.ifPresent(manager -> {
            StoredCRL storedCRL = new StoredCRL(Collections.emptySet(), crl);
            manager.getCRLStore().storeCRL(storedCRL);
        });
    }

}