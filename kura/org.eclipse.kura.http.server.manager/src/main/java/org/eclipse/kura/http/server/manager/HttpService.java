/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.http.server.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.eclipse.equinox.http.jetty.JettyConstants;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpService implements ConfigurableComponent {

    private static final String KURA_JETTY_PID = "kura.default";
    private static final String KURA_HTTPS_KEY_STORE_PASSWORD_KEY = "kura.https.keyStorePassword";

    private static final Logger logger = LoggerFactory.getLogger(HttpService.class);

    private ComponentContext componentContext;
    private HttpServiceOptions options;

    private CryptoService cryptoService;
    private SystemService systemService;
    private ConfigurationService configurationService;

    private Map<String, Object> properties;

    private ScheduledExecutorService selfUpdaterExecutor;
    private ScheduledFuture<?> selfUpdaterFuture;

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void unsetCryptoService(CryptoService cryptoService) {
        this.cryptoService = null;
    }

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        this.systemService = null;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void unsetConfigurationService(ConfigurationService configurationService) {
        this.configurationService = null;
    }

    public void activate(ComponentContext context, Map<String, Object> properties) {
        ServiceReference<?> osgiHttpService = context.getBundleContext()
                .getServiceReference("org.osgi.service.http.HttpService");
        if (osgiHttpService != null) {
            logger.warn("Default http server is running. Use default http server");
            return;
        }
        logger.info("Activating {}", this.getClass().getSimpleName());
        this.componentContext = context;

        this.properties = properties;

        this.options = new HttpServiceOptions(properties, this.systemService.getKuraHome());
        this.selfUpdaterExecutor = Executors.newSingleThreadScheduledExecutor();

        if (keystoreExists(this.options.getHttpsKeystorePath())) {
            if (isFirstBoot()) {
                changeDefaultKeystorePassword();
            } else {
                activateHttpService();
            }
        } else
            activateOnlyHttpService();

        logger.info("Activating... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updating {}", this.getClass().getSimpleName());

        this.properties = properties;

        HttpServiceOptions updatedOptions = new HttpServiceOptions(properties, this.systemService.getKuraHome());

        if (!this.options.equals(updatedOptions)) {
            logger.debug("Updating, new props");
            this.options = updatedOptions;

            if (keystoreExists(this.options.getHttpsKeystorePath())) {

                deactivateHttpService();

                accessKeystore();

                activateHttpService();
            } else {
                deactivateHttpService();
                activateOnlyHttpService();
            }
        }

        logger.info("Updating... Done.");
    }

    public void deactivate() {
        logger.info("Deactivating {}", this.getClass().getSimpleName());

        deactivateHttpService();

        if (this.selfUpdaterFuture != null && !this.selfUpdaterFuture.isDone()) {

            logger.info("Self updater task running. Stopping it");

            this.selfUpdaterFuture.cancel(true);
        }
    }

    private Dictionary<String, Object> getJettyConfig() {

        final Hashtable<String, Object> config = new Hashtable<>();

        config.put(JettyConstants.HTTPS_PORT, this.options.getHttpsPort());
        config.put(JettyConstants.HTTPS_ENABLED, this.options.isHttpsEnabled());
        config.put(JettyConstants.HTTPS_HOST, "0.0.0.0");
        config.put(JettyConstants.SSL_KEYSTORE, this.options.getHttpsKeystorePath());

        char[] decryptedPassword;
        try {
            decryptedPassword = this.cryptoService.decryptAes(this.options.getHttpsKeystorePassword());
        } catch (KuraException e) {
            logger.warn("Unable to decrypt property password");
            decryptedPassword = this.options.getHttpsKeystorePassword();
        }

        config.put(JettyConstants.SSL_PASSWORD, new String(decryptedPassword));
        config.put(JettyConstants.HTTP_PORT, this.options.getHttpPort());
        config.put(JettyConstants.HTTP_ENABLED, this.options.isHttpEnabled());
        config.put(JettyConstants.HTTP_HOST, "0.0.0.0");

        final String customizerClass = System
                .getProperty(JettyConstants.PROPERTY_PREFIX + JettyConstants.CUSTOMIZER_CLASS);

        if (customizerClass instanceof String) {
            config.put(JettyConstants.CUSTOMIZER_CLASS, customizerClass);
        }

        return config;
    }

    private Dictionary<String, Object> getJettyHttpConfig() {

        final Hashtable<String, Object> config = new Hashtable<>();

        config.put(JettyConstants.HTTP_PORT, this.options.getHttpPort());
        config.put(JettyConstants.HTTP_ENABLED, this.options.isHttpEnabled());

        config.put(JettyConstants.HTTPS_ENABLED, false);

        final String customizerClass = System
                .getProperty(JettyConstants.PROPERTY_PREFIX + JettyConstants.CUSTOMIZER_CLASS);

        if (customizerClass instanceof String) {
            config.put(JettyConstants.CUSTOMIZER_CLASS, customizerClass);
        }

        return config;
    }

    private void activateOnlyHttpService() {
        logger.warn("no keystore https disabled.");
        try {
            logger.info("starting Jetty http only instance...");
            JettyConfigurator.startServer(KURA_JETTY_PID, getJettyHttpConfig());
            logger.info("starting Jetty http onlny instance...done");
        } catch (final Exception e) {
            logger.error("Could not start Jetty http only Web server", e);
        }
    }

    private void activateHttpService() {
        try {
            logger.info("starting Jetty instance...");
            JettyConfigurator.startServer(KURA_JETTY_PID, getJettyConfig());
            logger.info("starting Jetty instance...done");
        } catch (final Exception e) {
            logger.error("Could not start Jetty Web server", e);
        }
    }

    private void deactivateHttpService() {
        try {
            logger.info("stopping Jetty instance...");
            JettyConfigurator.stopServer(KURA_JETTY_PID);
            logger.info("stopping Jetty instance...done");
        } catch (final Exception e) {
            logger.error("Could not stop Jetty Web server", e);
        }
    }

    private void accessKeystore() {
        String keystorePath = this.options.getHttpsKeystorePath();
        if (!keystoreExists(keystorePath)) {
            return;
        }

        char[] oldPassword = getOldKeystorePassword(keystorePath);

        char[] newPassword = null;
        try {
            newPassword = this.cryptoService.decryptAes(this.options.getHttpsKeystorePassword());
        } catch (KuraException e) {
            logger.warn("Failed to decrypt keystore password");
        }
        updateKeystorePassword(oldPassword, newPassword);
    }

    private boolean keystoreExists(String keystorePath) {
        boolean result = false;
        File fKeyStore = new File(keystorePath);
        if (fKeyStore.exists()) {
            result = true;
        }
        return result;
    }

    private char[] getOldKeystorePassword(String keystorePath) {
        char[] password = this.cryptoService.getKeyStorePassword(keystorePath);
        if (password != null && isKeyStoreAccessible(this.options.getHttpsKeystorePath(), password)) {
            return password;
        }

        try {
            password = this.cryptoService.decryptAes(this.options.getHttpsKeystorePassword());
        } catch (KuraException e) {
            password = new char[0];
        }

        return password;
    }

    private void updateKeystorePassword(char[] oldPassword, char[] newPassword) {
        try {
            changeKeyStorePassword(this.options.getHttpsKeystorePath(), oldPassword, newPassword);

            this.cryptoService.setKeyStorePassword(this.options.getHttpsKeystorePath(), newPassword);
        } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | UnrecoverableEntryException
                | IOException e) {
            logger.warn("Failed to change keystore password", e);
        } catch (KuraException e) {
            logger.warn("Failed to persist keystore password", e);
        }
    }

    private void changeDefaultKeystorePassword() {

        char[] oldPassword = this.systemService.getProperties().getProperty(KURA_HTTPS_KEY_STORE_PASSWORD_KEY)
                .toCharArray();

        if (isDefaultFromCrypto()) {
            oldPassword = this.cryptoService.getKeyStorePassword(this.options.getHttpsKeystorePath());
        }

        char[] newPassword = new BigInteger(160, new SecureRandom()).toString(32).toCharArray();

        try {
            changeKeyStorePassword(this.options.getHttpsKeystorePath(), oldPassword, newPassword);

            this.cryptoService.setKeyStorePassword(this.options.getHttpsKeystorePath(), newPassword);

            updatePasswordInConfigService(newPassword);
        } catch (Exception e) {
            logger.warn("Keystore password change failed", e);
        }
    }

    private void updatePasswordInConfigService(char[] newPassword) {
        // update our configuration with the newly generated password
        final String pid = (String) this.properties.get("service.pid");

        Map<String, Object> props = new HashMap<>();
        props.putAll(this.properties);
        props.put(HttpServiceOptions.PROP_HTTPS_KEYSTORE_PATH, this.options.getHttpsKeystorePath());
        props.put(HttpServiceOptions.PROP_HTTPS_KEYSTORE_PASSWORD, new Password(newPassword));

        this.selfUpdaterFuture = this.selfUpdaterExecutor.scheduleAtFixedRate(() -> {
            try {
                if (HttpService.this.componentContext.getServiceReference() != null
                        && HttpService.this.configurationService.getComponentConfiguration(pid) != null) {
                    HttpService.this.configurationService.updateConfiguration(pid, props);
                    throw new RuntimeException("Updated. The task will be terminated.");
                } else {
                    logger.info("No service or configuration available yet.");
                }
            } catch (KuraException e) {
                logger.warn("Cannot get/update configuration for pid: {}", pid, e);
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    private boolean isFirstBoot() {
        boolean result = false;
        if (isSnapshotPasswordDefault() && (isDefaultFromUser() || isDefaultFromCrypto())) {
            result = true;
        }
        return result;
    }

    private boolean isSnapshotPasswordDefault() {
        boolean result = false;

        char[] snapshotPassword = getUnencryptedSslKeystorePassword();
        if (Arrays.equals(HttpServiceOptions.DEFAULT_HTTPS_KEYSTORE_PASSWORD.toCharArray(), snapshotPassword)) {
            result = true;
        }

        return result;
    }

    private char[] getUnencryptedSslKeystorePassword() {
        char[] snapshotPassword = this.options.getHttpsKeystorePassword();
        try {
            snapshotPassword = this.cryptoService.decryptAes(snapshotPassword);
        } catch (KuraException e) {
            // Nothing to do
        }
        return snapshotPassword;
    }

    private boolean isDefaultFromCrypto() {
        char[] cryptoPassword = this.cryptoService.getKeyStorePassword(this.options.getHttpsKeystorePath());

        if (cryptoPassword == null) {
            return false;
        }
        return isKeyStoreAccessible(this.options.getHttpsKeystorePath(), cryptoPassword);
    }

    private boolean isDefaultFromUser() {
        return isKeyStoreAccessible(this.options.getHttpsKeystorePath(),
                (char[]) this.systemService.getProperties().get(KURA_HTTPS_KEY_STORE_PASSWORD_KEY));
    }

    private void changeKeyStorePassword(String location, char[] oldPassword, char[] newPassword) throws IOException,
            NoSuchAlgorithmException, CertificateException, KeyStoreException, UnrecoverableEntryException {

        KeyStore keystore = loadKeystore(location, oldPassword);

        updateKeyEntiesPasswords(keystore, oldPassword, newPassword);
        saveKeystore(location, newPassword, keystore);
    }

    private static void updateKeyEntiesPasswords(KeyStore keystore, char[] oldPassword, char[] newPassword)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException {
        Enumeration<String> aliases = keystore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keystore.isKeyEntry(alias)) { // TODO: not sure why this check
                PasswordProtection oldPP = new PasswordProtection(oldPassword);
                Entry entry = keystore.getEntry(alias, oldPP);
                PasswordProtection newPP = new PasswordProtection(newPassword);
                keystore.setEntry(alias, entry, newPP);
            }
        }
    }

    private void saveKeystore(String keyStoreFileName, char[] keyStorePassword, KeyStore ks)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        try (FileOutputStream tsOutStream = new FileOutputStream(keyStoreFileName);) {
            ks.store(tsOutStream, keyStorePassword);
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

    private boolean isKeyStoreAccessible(String location, char[] password) {
        try {
            loadKeystore(location, password);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
