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

import static org.eclipse.kura.core.keystore.FilesystemKeystoreServiceOptions.KEY_KEYSTORE_PASSWORD;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.crypto.CryptoService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesystemKeystoreServiceImpl extends BaseKeystoreService {

    private static final Logger logger = LoggerFactory.getLogger(FilesystemKeystoreServiceImpl.class);

    private CryptoService cryptoService;
    private ConfigurationService configurationService;

    private FilesystemKeystoreServiceOptions keystoreServiceOptions;

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

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    @Override
    public void activate(ComponentContext context, Map<String, Object> properties) {
        logger.info("Bundle {} is starting!", properties.get(KURA_SERVICE_PID));
        this.componentContext = context;

        this.keystoreServiceOptions = new FilesystemKeystoreServiceOptions(properties, this.cryptoService);
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

        super.activate(context, properties);

        logger.info("Bundle {} has started!", properties.get(KURA_SERVICE_PID));
    }

    @Override
    public void updated(Map<String, Object> properties) {
        logger.info("Bundle {} is updating!", properties.get(KURA_SERVICE_PID));
        FilesystemKeystoreServiceOptions newOptions = new FilesystemKeystoreServiceOptions(properties,
                this.cryptoService);

        if (!this.keystoreServiceOptions.equals(newOptions)) {
            logger.info("Perform update...");

            if (!this.keystoreServiceOptions.getKeystorePath().equals(newOptions.getKeystorePath())) {
                updateKeystorePath(newOptions);
            } else {
                checkAndUpdateKeystorePassword(newOptions);
            }

            this.keystoreServiceOptions = new FilesystemKeystoreServiceOptions(properties, this.cryptoService);

        }

        super.updated(properties);

        logger.info("Bundle {} has updated!", properties.get(KURA_SERVICE_PID));
    }

    @Override
    public void deactivate() {
        logger.info("Bundle {} is deactivating!", this.keystoreServiceOptions.getProperties().get(KURA_SERVICE_PID));

        if (this.selfUpdaterFuture != null && !this.selfUpdaterFuture.isDone()) {

            logger.info("Self updater task running. Stopping it");

            this.selfUpdaterFuture.cancel(true);
        }

        super.deactivate();
    }

    @Override
    protected void saveKeystore(KeystoreInstance ks)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        try (FileOutputStream tsOutStream = new FileOutputStream(this.keystoreServiceOptions.getKeystorePath());) {
            ks.getKeystore().store(tsOutStream, ks.getPassword());
        }
    }

    @Override
    protected KeystoreInstance loadKeystore() throws KuraException {
        return loadKeystore(this.keystoreServiceOptions);
    }

    @Override
    protected String getCrlStorePath() {
        return this.keystoreServiceOptions.getKeystorePath() + ".crl";
    }

    private void checkAndUpdateKeystorePassword(final FilesystemKeystoreServiceOptions options) {
        try {
            final KeystoreInstance ks = loadKeystore(this.keystoreServiceOptions);

            final char[] configPassword = options.getKeystorePassword(cryptoService);

            if (!Arrays.equals(ks.getPassword(), configPassword)) {
                setKeystorePassword(ks, configPassword);
            }

        } catch (final Exception e) {
            logger.warn("failed to load or update keystore password", e);
        }
    }

    private boolean keystoreExists(String keystorePath) {
        return keystorePath != null && new File(keystorePath).isFile();
    }

    private void createKeystore(FilesystemKeystoreServiceOptions options) throws Exception {
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

    private void updateKeystorePath(FilesystemKeystoreServiceOptions newOptions) {
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
            final KeystoreInstance keystore = loadKeystore(this.keystoreServiceOptions);

            char[] newPassword = new BigInteger(160, new SecureRandom()).toString(32).toCharArray();

            setKeystorePassword(keystore, newPassword);

            Map<String, Object> props = new HashMap<>(this.keystoreServiceOptions.getProperties());
            props.put(KEY_KEYSTORE_PASSWORD, new String(this.cryptoService.encryptAes(newPassword)));
            this.keystoreServiceOptions = new FilesystemKeystoreServiceOptions(props, this.cryptoService);

            updatePasswordInConfigService(newPassword);
        } catch (Exception e) {
            logger.warn("Keystore password change failed", e);
        }
    }

    private synchronized void saveKeystore(KeystoreInstance ks, char[] keyStorePassword)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        try (FileOutputStream tsOutStream = new FileOutputStream(((KeystoreInstanceImpl) ks).path)) {
            ks.getKeystore().store(tsOutStream, keyStorePassword);
        }
    }

    private void updatePasswordInConfigService(char[] newPassword) {
        final String pid = this.keystoreServiceOptions.getPid();

        Map<String, Object> props = new HashMap<>();
        props.putAll(this.keystoreServiceOptions.getProperties());
        props.put(FilesystemKeystoreServiceOptions.KEY_KEYSTORE_PATH, this.keystoreServiceOptions.getKeystorePath());
        props.put(FilesystemKeystoreServiceOptions.KEY_KEYSTORE_PASSWORD, new Password(newPassword));
        props.put(FilesystemKeystoreServiceOptions.KEY_RANDOMIZE_PASSWORD, false);

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

    private synchronized void setKeystorePassword(KeystoreInstance ks, char[] password) {
        try {
            updateKeyEntriesPasswords(ks, password);
            saveKeystore(ks, password);

            this.cryptoService.setKeyStorePassword(((KeystoreInstanceImpl) ks).path, password);
        } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | UnrecoverableEntryException
                | IOException e) {
            logger.warn("Failed to change keystore password");
        } catch (KuraException e) {
            logger.warn("Failed to persist keystore password");
        }
    }

    private static void updateKeyEntriesPasswords(KeystoreInstance ks, char[] newPassword)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException {
        Enumeration<String> aliases = ks.getKeystore().aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (ks.getKeystore().isKeyEntry(alias)) {
                PasswordProtection oldPP = new PasswordProtection(ks.getPassword());
                Entry entry = ks.getKeystore().getEntry(alias, oldPP);
                PasswordProtection newPP = new PasswordProtection(newPassword);
                ks.getKeystore().setEntry(alias, entry, newPP);
            }
        }
    }

    private synchronized KeystoreInstance loadKeystore(final FilesystemKeystoreServiceOptions options)
            throws KuraException {
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

        final KeystoreInstance result = new KeystoreLoader(options.getKeystorePath(), passwords).loadKeystore();

        if (!Arrays.equals(passwordInCrypto, result.getPassword())) {
            this.cryptoService.setKeyStorePassword(((KeystoreInstanceImpl) result).path, result.getPassword());
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

        KeystoreInstance loadKeystore() throws KuraException {

            for (final char[] password : this.passwords) {
                try {
                    final KeyStore keyStore = loadKeystore(path, password);

                    return new KeystoreInstanceImpl(keyStore, password, this.path);
                } catch (final Exception e) {
                    logger.debug("failed to load keystore", e);
                }
            }

            throw new KuraException(KuraErrorCode.BAD_REQUEST, "Failed to get the KeyStore");

        }
    }

    private static class KeystoreInstanceImpl implements KeystoreInstance {

        private final KeyStore keystore;
        private final char[] password;
        private final String path;

        public KeystoreInstanceImpl(final KeyStore keystore, final char[] password, final String path) {
            this.keystore = keystore;
            this.password = password;
            this.path = path;
        }

        @Override
        public KeyStore getKeystore() {
            return keystore;
        }

        @Override
        public char[] getPassword() {
            return password;
        }

    }
}