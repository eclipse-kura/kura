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
package org.eclipse.kura.core.keystore;

import static java.util.Objects.isNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.security.keystore.KeystoreChangedEvent;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesystemKeystoreServiceImpl implements KeystoreService, ConfigurableComponent {

    private static final String PEM_CERTIFICATE_REQUEST_TYPE = "CERTIFICATE REQUEST";
    private static final String KURA_HTTPS_KEY_STORE_PASSWORD_KEY = "kura.https.keyStorePassword";

    private static final Logger logger = LoggerFactory.getLogger(FilesystemKeystoreServiceImpl.class);

    private ComponentContext componentContext;

    private CryptoService cryptoService;
    private SystemService systemService;
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

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
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

    public synchronized void activate(ComponentContext context, Map<String, Object> properties) {
        logger.info("Bundle {} is starting!", this.getClass().getSimpleName());
        this.componentContext = context;

        this.ownPid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
        this.keystoreServiceOptions = new KeystoreServiceOptions(properties, this.cryptoService);
        this.selfUpdaterExecutor = Executors.newSingleThreadScheduledExecutor();

        if (keystoreExists(this.keystoreServiceOptions.getKeystorePath())
                && this.keystoreServiceOptions.needsRandomPassword()) {
            changeDefaultKeystorePassword();
        }

        this.crlManagerOptions = new CRLManagerOptions(properties);

        updateCRLManager(this.crlManagerOptions);

        logger.info("Bundle {} has started!", this.getClass().getSimpleName());
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Bundle {} is updating!", this.getClass().getSimpleName());
        KeystoreServiceOptions newOptions = new KeystoreServiceOptions(properties, this.cryptoService);

        if (!this.keystoreServiceOptions.equals(newOptions)) {
            logger.info("Perform update...");
            this.keystoreServiceOptions = new KeystoreServiceOptions(properties, this.cryptoService);
            if (keystoreExists(this.keystoreServiceOptions.getKeystorePath())) {
                accessKeystore();
            }
        }

        final CRLManagerOptions newCRLManagerOptions = new CRLManagerOptions(properties);

        if (!this.crlManagerOptions.equals(newCRLManagerOptions)) {
            this.crlManagerOptions = newCRLManagerOptions;

            updateCRLManager(newCRLManagerOptions);
        }

        logger.info("Bundle {} has updated!", this.getClass().getSimpleName());
    }

    protected void deactivate() {
        logger.info("Bundle {} is deactivating!", this.getClass().getSimpleName());

        if (this.selfUpdaterFuture != null && !this.selfUpdaterFuture.isDone()) {

            logger.info("Self updater task running. Stopping it");

            this.selfUpdaterFuture.cancel(true);
        }

        shutdownCRLManager();
    }

    private boolean keystoreExists(String keystorePath) {
        boolean result = false;
        File fKeyStore = new File(keystorePath);
        if (fKeyStore.exists()) {
            result = true;
        }
        return result;
    }

    private void accessKeystore() {
        String keystorePath = this.keystoreServiceOptions.getKeystorePath();
        if (!keystoreExists(keystorePath)) {
            return;
        }

        char[] oldPassword = getOldKeystorePassword(keystorePath);
        char[] newPassword = this.keystoreServiceOptions.getKeystorePassword(this.cryptoService);

        if (!Arrays.equals(oldPassword, newPassword)) {
            updateKeystorePassword(oldPassword, newPassword);
        }
    }

    private void changeDefaultKeystorePassword() {

        char[] oldPassword = this.systemService.getProperties().getProperty(KURA_HTTPS_KEY_STORE_PASSWORD_KEY)
                .toCharArray();

        if (isDefaultFromCrypto()) {
            oldPassword = this.cryptoService.getKeyStorePassword(this.keystoreServiceOptions.getKeystorePath());
        }

        char[] newPassword = new BigInteger(160, new SecureRandom()).toString(32).toCharArray();

        try {
            changeKeyStorePassword(this.keystoreServiceOptions.getKeystorePath(), oldPassword, newPassword);

            this.cryptoService.setKeyStorePassword(this.keystoreServiceOptions.getKeystorePath(), newPassword);

            updatePasswordInConfigService(newPassword);
        } catch (Exception e) {
            logger.warn("Keystore password change failed", e);
        }
    }

    private void changeKeyStorePassword(String location, char[] oldPassword, char[] newPassword) throws IOException,
            NoSuchAlgorithmException, CertificateException, KeyStoreException, UnrecoverableEntryException {

        KeyStore keystore = loadKeystore(location, oldPassword);

        updateKeyEntriesPasswords(keystore, oldPassword, newPassword);
        saveKeystore(location, newPassword, keystore);
    }

    private void saveKeystore(String keyStoreFileName, char[] keyStorePassword, KeyStore ks)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        try (FileOutputStream tsOutStream = new FileOutputStream(keyStoreFileName);) {
            ks.store(tsOutStream, keyStorePassword);
        }
    }

    private void updatePasswordInConfigService(char[] newPassword) {
        // update our configuration with the newly generated password
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

    private boolean isDefaultFromCrypto() {
        char[] cryptoPassword = this.cryptoService.getKeyStorePassword(this.keystoreServiceOptions.getKeystorePath());

        if (cryptoPassword == null) {
            return false;
        }
        return isKeyStoreAccessible(this.keystoreServiceOptions.getKeystorePath(), cryptoPassword);
    }

    private boolean isKeyStoreAccessible(String location, char[] password) {
        try {
            loadKeystore(location, password);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private KeyStore loadKeystore(String keyStore, char[] keyStorePassword)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        try (InputStream tsReadStream = new FileInputStream(keyStore);) {
            ks.load(tsReadStream, keyStorePassword);
        }

        return ks;
    }

    private void updateKeystorePassword(char[] oldPassword, char[] newPassword) {
        try {
            changeKeyStorePassword(oldPassword, newPassword);

            this.cryptoService.setKeyStorePassword(this.keystoreServiceOptions.getKeystorePath(), newPassword);
        } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | UnrecoverableEntryException
                | IOException e) {
            logger.warn("Failed to change keystore password");
        } catch (KuraException e) {
            logger.warn("Failed to persist keystore password");
        } catch (GeneralSecurityException e) {
            logger.warn("Failed to load keystore");
        }
    }

    private void changeKeyStorePassword(char[] oldPassword, char[] newPassword)
            throws IOException, GeneralSecurityException, KuraException {

        KeyStore keystore = getKeyStore();

        updateKeyEntriesPasswords(keystore, oldPassword, newPassword);
        saveKeystore(keystore, newPassword);
    }

    private char[] getOldKeystorePassword(String keystorePath) {
        char[] password = this.cryptoService.getKeyStorePassword(keystorePath);
        if (password != null && isKeyStoreAccessible()) {
            return password;
        }

        return this.keystoreServiceOptions.getKeystorePassword(this.cryptoService);
    }

    private boolean isKeyStoreAccessible() {
        try {
            getKeyStore();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void updateKeyEntriesPasswords(KeyStore keystore, char[] oldPassword, char[] newPassword)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException {
        Enumeration<String> aliases = keystore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keystore.isKeyEntry(alias)) {
                PasswordProtection oldPP = new PasswordProtection(oldPassword);
                Entry entry = keystore.getEntry(alias, oldPP);
                PasswordProtection newPP = new PasswordProtection(newPassword);
                keystore.setEntry(alias, entry, newPP);
            }
        }
    }

    @Override
    public KeyStore getKeyStore() throws KuraException {
        KeyStore ks = null;
        try (InputStream tsReadStream = new FileInputStream(this.keystoreServiceOptions.getKeystorePath());) {
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] keystorePassword = this.keystoreServiceOptions.getKeystorePassword(this.cryptoService);

            ks.load(tsReadStream, keystorePassword);
        } catch (GeneralSecurityException | IOException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, e, "Failed to get the KeyStore");
        }

        return ks;
    }

    @Override
    public Entry getEntry(String alias) throws KuraException {
        if (isNull(alias)) {
            throw new IllegalArgumentException("Key Pair alias cannot be null!");
        }
        KeyStore ks = getKeyStore();

        try {
            if (ks.entryInstanceOf(alias, PrivateKeyEntry.class) || ks.entryInstanceOf(alias, SecretKeyEntry.class)) {
                return ks.getEntry(alias,
                        new PasswordProtection(this.keystoreServiceOptions.getKeystorePassword(this.cryptoService)));
            } else {
                return ks.getEntry(alias, null);
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
        if (isNull(principal) || isNull(keypair) || isNull(signerAlg) || signerAlg.trim().isEmpty()) {
            throw new IllegalArgumentException("Input parameters cannot be null!");
        }
        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(principal,
                keypair.getPublic());
        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(signerAlg);
        ContentSigner signer = null;
        try {
            signer = csBuilder.build(keypair.getPrivate());
        } catch (OperatorCreationException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, e, "Failed to get CSR");
        }
        PKCS10CertificationRequest csr = p10Builder.build(signer);
        try (StringWriter str = new StringWriter(); JcaPEMWriter pemWriter = new JcaPEMWriter(str);) {
            PemObject pemCSR = new PemObject(PEM_CERTIFICATE_REQUEST_TYPE, csr.getEncoded());

            pemWriter.writeObject(pemCSR);
            pemWriter.flush();
            return str.toString();
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR, e, "Failed to get CSR");
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
    public List<KeyManager> getKeyManagers(String algorithm) throws KuraException {
        if (isNull(algorithm)) {
            throw new IllegalArgumentException("Algorithm cannot be null!");
        }
        KeyStore ks = getKeyStore();
        try {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, this.keystoreServiceOptions.getKeystorePassword(this.cryptoService));

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
        KeyStore ks = getKeyStore();
        try {
            ks.deleteEntry(alias);
            saveKeystore(ks);
            postChangedEvent();
        } catch (GeneralSecurityException | IOException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, e, "Failed to delete entry " + alias);
        }
    }

    private void saveKeystore(KeyStore ks)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        saveKeystore(ks, this.keystoreServiceOptions.getKeystorePassword(this.cryptoService));
    }

    private void saveKeystore(KeyStore ks, char[] password)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        try (FileOutputStream tsOutStream = new FileOutputStream(this.keystoreServiceOptions.getKeystorePath());) {
            ks.store(tsOutStream, password);
        }
    }

    @Override
    public void setEntry(String alias, Entry entry) throws KuraException {
        if (isNull(alias) || alias.trim().isEmpty() || isNull(entry)) {
            throw new IllegalArgumentException("Input cannot be null or empty!");
        }
        KeyStore ks = getKeyStore();

        final ProtectionParameter protectionParameter;

        if (entry instanceof TrustedCertificateEntry) {
            protectionParameter = null;
        } else {
            protectionParameter = new PasswordProtection(
                    this.keystoreServiceOptions.getKeystorePassword(this.cryptoService));
        }
        try {
            ks.setEntry(alias, entry, protectionParameter);
            saveKeystore(ks);
            postChangedEvent();
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
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
            keyGen.initialize(keySize, secureRandom);
            keyPair = keyGen.generateKeyPair();
            setEntry(alias, new PrivateKeyEntry(keyPair.getPrivate(),
                    generateCertificateChain(keyPair, signatureAlgorithm, attributes)));
        } catch (GeneralSecurityException | OperatorCreationException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
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
                for (final Entry e : this.getEntries().values()) {
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
        this.eventAdmin.postEvent(new KeystoreChangedEvent(ownPid));
    }
}
