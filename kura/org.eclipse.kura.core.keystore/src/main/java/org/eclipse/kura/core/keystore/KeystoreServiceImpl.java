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
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.security.auth.x500.X500Principal;

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
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 *
 */
public class KeystoreServiceImpl implements KeystoreService, ConfigurableComponent {

    private static final String PEM_CERTIFICATE_REQUEST_TYPE = "CERTIFICATE REQUEST";
    private static final String KURA_HTTPS_KEY_STORE_PASSWORD_KEY = "kura.https.keyStorePassword";

    private static final Logger logger = LoggerFactory.getLogger(KeystoreServiceImpl.class);

    private ComponentContext componentContext;

    private CryptoService cryptoService;
    private SystemService systemService;
    private ConfigurationService configurationService;

    private KeystoreServiceOptions keystoreServiceOptions;

    private ScheduledExecutorService selfUpdaterExecutor;
    private ScheduledFuture<?> selfUpdaterFuture;

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

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    public synchronized void activate(ComponentContext context, Map<String, Object> properties) {
        logger.info("Bundle {} is starting!", this.getClass().getSimpleName());
        this.componentContext = context;

        this.keystoreServiceOptions = new KeystoreServiceOptions(properties);
        this.selfUpdaterExecutor = Executors.newSingleThreadScheduledExecutor();

        if (keystoreExists(this.keystoreServiceOptions.getKeystorePath())
                && this.keystoreServiceOptions.needsRandomPassword()) {
            changeDefaultKeystorePassword();
        }

        logger.info("Bundle {} has started!", this.getClass().getSimpleName());
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Bundle {} is updating!", this.getClass().getSimpleName());
        KeystoreServiceOptions newOptions = new KeystoreServiceOptions(properties);

        if (!this.keystoreServiceOptions.equals(newOptions)) {
            logger.info("Perform update...");
            this.keystoreServiceOptions = new KeystoreServiceOptions(properties);
            if (keystoreExists(this.keystoreServiceOptions.getKeystorePath())) {
                accessKeystore();
            }
        }
        logger.info("Bundle {} has updated!", this.getClass().getSimpleName());
    }

    protected void deactivate() {
        logger.info("Bundle {} is deactivating!", this.getClass().getSimpleName());

        if (this.selfUpdaterFuture != null && !this.selfUpdaterFuture.isDone()) {

            logger.info("Self updater task running. Stopping it");

            this.selfUpdaterFuture.cancel(true);
        }
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
        char[] newPassword = null;

        try {
            newPassword = this.cryptoService.decryptAes(this.keystoreServiceOptions.getKeystorePassword());
        } catch (KuraException e) {
            logger.warn("Failed to decrypt keystore password");
        }

        if (newPassword != null && !Arrays.equals(oldPassword, newPassword)) {
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

        updateKeyEntiesPasswords(keystore, oldPassword, newPassword);
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
                        && this.configurationService.getComponentConfiguration(pid) != null) {
                    this.configurationService.updateConfiguration(pid, props);
                    throw new KuraRuntimeException(KuraErrorCode.CONFIGURATION_SNAPSHOT_TAKING, "Updated. The task will be terminated.");
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
            throws IOException, GeneralSecurityException {

        KeyStore keystore = getKeyStore();

        updateKeyEntiesPasswords(keystore, oldPassword, newPassword);
        saveKeystore(keystore, newPassword);
    }

    private char[] getOldKeystorePassword(String keystorePath) {
        char[] password = this.cryptoService.getKeyStorePassword(keystorePath);
        if (password != null && isKeyStoreAccessible()) {
            return password;
        }

        try {
            password = this.cryptoService.decryptAes(this.keystoreServiceOptions.getKeystorePassword());
        } catch (KuraException e) {
            password = new char[0];
        }

        return password;
    }

    private boolean isKeyStoreAccessible() {
        try {
            getKeyStore();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void updateKeyEntiesPasswords(KeyStore keystore, char[] oldPassword, char[] newPassword)
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
    public KeyStore getKeyStore() throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        char[] keystorePassword = this.keystoreServiceOptions.getKeystorePassword();
        try {
            keystorePassword = this.cryptoService.decryptAes(this.keystoreServiceOptions.getKeystorePassword());
        } catch (KuraException e) {
            logger.warn("Failed to decrypt keystore password");
        }

        try (InputStream tsReadStream = new FileInputStream(this.keystoreServiceOptions.getKeystorePath());) {
            ks.load(tsReadStream, keystorePassword);
        }

        return ks;
    }

    @Override
    public Entry getEntry(String alias) throws GeneralSecurityException, IOException {
        if (isNull(alias)) {
            throw new IllegalArgumentException("Key Pair alias cannot be null!");
        }
        KeyStore ks = getKeyStore();

        return ks.getEntry(alias, new PasswordProtection(this.keystoreServiceOptions.getKeystorePassword()));
    }

    @Override
    public Map<String, Entry> getEntries() throws GeneralSecurityException, IOException {
        Map<String, Entry> result = new HashMap<>();

        KeyStore ks = getKeyStore();
        List<String> aliases = Collections.list(ks.aliases());

        for (String alias : aliases) {
            Entry tempEntry = getEntry(alias);
            result.put(alias, tempEntry);
        }
        return result;
    }

    @Override
    public String getCSR(X500Principal principal, KeyPair keypair, String signerAlg) throws KuraException {
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
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
        PKCS10CertificationRequest csr = p10Builder.build(signer);
        try (StringWriter str = new StringWriter(); JcaPEMWriter pemWriter = new JcaPEMWriter(str);) {
            PemObject pemCSR = new PemObject(PEM_CERTIFICATE_REQUEST_TYPE, csr.getEncoded());

            pemWriter.writeObject(pemCSR);
            pemWriter.flush();
            return str.toString();
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR);
        }
    }

    @Override
    public List<KeyManager> getKeyManagers(String algorithm) throws GeneralSecurityException, IOException {
        if (isNull(algorithm)) {
            throw new IllegalArgumentException("Algorithm cannot be null!");
        }
        KeyStore ks = getKeyStore();
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        kmf.init(ks, this.keystoreServiceOptions.getKeystorePassword());

        return Arrays.asList(kmf.getKeyManagers());
    }

    @Override
    public void deleteEntry(String alias) throws GeneralSecurityException, IOException {
        if (isNull(alias)) {
            throw new IllegalArgumentException("Alias cannot be null!");
        }
        KeyStore ks = getKeyStore();
        ks.deleteEntry(alias);
        saveKeystore(ks);
    }

    private void saveKeystore(KeyStore ks)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        saveKeystore(ks, this.keystoreServiceOptions.getKeystorePassword());
    }

    private void saveKeystore(KeyStore ks, char[] password)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        try (FileOutputStream tsOutStream = new FileOutputStream(this.keystoreServiceOptions.getKeystorePath());) {
            ks.store(tsOutStream, password);
        }
    }

    @Override
    public void setEntry(String alias, Entry entry) throws GeneralSecurityException, IOException {
        if (isNull(alias) || alias.trim().isEmpty() || isNull(entry)) {
            throw new IllegalArgumentException("Input cannot be null or empty!");
        }
        KeyStore ks = getKeyStore();
        ks.setEntry(alias, entry, new PasswordProtection(this.keystoreServiceOptions.getKeystorePassword()));
        saveKeystore(ks);
    }

    @Override
    public List<String> getAliases() throws GeneralSecurityException, IOException {
        KeyStore ks = getKeyStore();
        return Collections.list(ks.aliases());
    }

    @Override
    public KeyPair createKeyPair(String algorithm, int keySize) throws KuraException {
        return createKeyPair(algorithm, keySize, new SecureRandom());
    }

    @Override
    public KeyPair createKeyPair(String algorithm, int keySize, SecureRandom secureRandom) throws KuraException {
        if (isNull(algorithm) || algorithm.trim().isEmpty() || isNull(secureRandom)) {
            throw new IllegalArgumentException("Algorithm or random cannot be null or empty!");
        }
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
            keyGen.initialize(keySize, secureRandom);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
    }

}
