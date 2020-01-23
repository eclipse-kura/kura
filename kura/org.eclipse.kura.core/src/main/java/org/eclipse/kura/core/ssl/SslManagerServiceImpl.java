/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.ssl.SslServiceListener;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslManagerServiceImpl implements SslManagerService, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(SslManagerServiceImpl.class);

    private SslServiceListeners sslServiceListeners;

    private ComponentContext ctx;
    private Map<String, Object> properties;
    private SslManagerServiceOptions options;

    private CryptoService cryptoService;
    private ConfigurationService configurationService;

    private ScheduledExecutorService selfUpdaterExecutor;
    private ScheduledFuture<?> selfUpdaterFuture;

    private Map<ConnectionSslOptions, SSLContext> sslContexts;

    private SystemService systemService;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void unsetCryptoService(CryptoService cryptoService) {
        this.cryptoService = null;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void unsetConfigurationService(ConfigurationService configurationService) {
        this.configurationService = null;
    }

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        this.systemService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("activate...");

        //
        // save the bundle context and the properties
        this.ctx = componentContext;
        this.properties = properties;
        this.options = new SslManagerServiceOptions(properties);
        this.sslContexts = new ConcurrentHashMap<>();

        this.selfUpdaterExecutor = Executors.newSingleThreadScheduledExecutor();

        ServiceTracker<SslServiceListener, SslServiceListener> listenersTracker = new ServiceTracker<>(
                componentContext.getBundleContext(), SslServiceListener.class, null);

        // Deferred open of tracker to prevent
        // java.lang.Exception: Recursive invocation of
        // ServiceFactory.getService
        // on ProSyst
        this.sslServiceListeners = new SslServiceListeners(listenersTracker);

        accessKeystore();
    }

    public void updated(Map<String, Object> properties) {
        logger.info("updated...");

        this.properties = properties;
        this.options = new SslManagerServiceOptions(properties);
        this.sslContexts = new ConcurrentHashMap<>();

        accessKeystore();

        // Notify listeners that service has been updated
        this.sslServiceListeners.onConfigurationUpdated();
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("deactivate...");
        this.sslServiceListeners.close();
        if (this.selfUpdaterFuture != null && !this.selfUpdaterFuture.isDone()) {

            logger.info("Self updater task running. Stopping it");

            this.selfUpdaterFuture.cancel(true);
        }
    }

    // ----------------------------------------------------------------
    //
    // Service APIs
    //
    // ----------------------------------------------------------------

    @Override
    public SSLContext getSSLContext() throws GeneralSecurityException, IOException {
        return getSSLContext("");
    }

    @Override
    public SSLContext getSSLContext(String keyAlias) throws GeneralSecurityException, IOException {
        String protocol = this.options.getSslProtocol();
        String ciphers = this.options.getSslCiphers();
        String trustStore = this.options.getSslKeyStore();
        char[] keyStorePassword = getKeyStorePassword();
        boolean hostnameVerifcation = this.options.isSslHostnameVerification();

        return getSSLContext(protocol, ciphers, trustStore, trustStore, keyStorePassword, keyAlias,
                hostnameVerifcation);
    }

    @Override
    public SSLContext getSSLContext(String protocol, String ciphers, String trustStore, String keyStore,
            char[] keyStorePassword, String keyAlias) throws GeneralSecurityException, IOException {
        return getSSLContext(protocol, ciphers, trustStore, keyStore, keyStorePassword, keyAlias,
                this.options.isSslHostnameVerification());
    }

    @Override
    public SSLContext getSSLContext(String protocol, String ciphers, String trustStore, String keyStore,
            char[] keyStorePassword, String keyAlias, boolean hostnameVerification)
            throws GeneralSecurityException, IOException {
        ConnectionSslOptions connSslOpts = new ConnectionSslOptions(this.options);
        connSslOpts.setProtocol(protocol);
        connSslOpts.setCiphers(ciphers);
        connSslOpts.setTrustStore(trustStore);
        connSslOpts.setKeyStore(keyStore);
        if (keyStorePassword == null) {
            connSslOpts.setKeyStorePassword(getKeyStorePassword());
        } else {
            connSslOpts.setKeyStorePassword(keyStorePassword);
        }
        connSslOpts.setAlias(keyAlias);
        connSslOpts.setHostnameVerification(hostnameVerification);

        return getSSLContextInternal(connSslOpts);
    }

    @Override
    public SSLSocketFactory getSSLSocketFactory() throws GeneralSecurityException, IOException {
        return getSSLContext().getSocketFactory();
    }

    @Override
    public SSLSocketFactory getSSLSocketFactory(String keyAlias) throws GeneralSecurityException, IOException {
        return getSSLContext(keyAlias).getSocketFactory();
    }

    @Override
    public SSLSocketFactory getSSLSocketFactory(String protocol, String ciphers, String trustStore, String keyStore,
            char[] keyStorePassword, String keyAlias) throws GeneralSecurityException, IOException {
        return getSSLContext(protocol, ciphers, trustStore, keyStore, keyStorePassword, keyAlias).getSocketFactory();
    }

    @Override
    public SSLSocketFactory getSSLSocketFactory(String protocol, String ciphers, String trustStore, String keyStore,
            char[] keyStorePassword, String keyAlias, boolean hostnameVerification)
            throws GeneralSecurityException, IOException {
        return getSSLContext(protocol, ciphers, trustStore, keyStore, keyStorePassword, keyAlias, hostnameVerification)
                .getSocketFactory();
    }

    @Override
    public X509Certificate[] getTrustCertificates() throws GeneralSecurityException, IOException {
        X509Certificate[] cacerts = null;
        String trustStore = this.options.getSslKeyStore();
        TrustManager[] tms = getTrustManagers(trustStore, this.options.getSslKeystorePassword().toCharArray());
        for (TrustManager tm : tms) {
            if (tm instanceof X509TrustManager) {
                X509TrustManager x509tm = (X509TrustManager) tm;
                cacerts = x509tm.getAcceptedIssuers();
                break;
            }
        }
        return cacerts;
    }

    @Override
    public void installTrustCertificate(String alias, X509Certificate x509crt)
            throws GeneralSecurityException, IOException {

        String keyStore = this.options.getSslKeyStore();
        char[] keyStorePassword = getKeyStorePassword();

        KeyStore ks = loadKeystore(keyStore, keyStorePassword);

        ks.setCertificateEntry(alias, x509crt);

        saveKeystore(keyStore, keyStorePassword, ks);
    }

    @Override
    public void deleteTrustCertificate(String alias) throws GeneralSecurityException, IOException {
        String keyStore = this.options.getSslKeyStore();
        char[] keyStorePassword = getKeyStorePassword();

        KeyStore ks = loadKeystore(keyStore, keyStorePassword);

        ks.deleteEntry(alias);

        saveKeystore(keyStore, keyStorePassword, ks);
    }

    @Override
    public void installPrivateKey(String alias, PrivateKey privateKey, char[] password, Certificate[] publicCerts)
            throws GeneralSecurityException, IOException {
        // Note that password parameter is unused

        String keyStore = this.options.getSslKeyStore();
        char[] keyStorePassword = getKeyStorePassword();

        KeyStore ks = loadKeystore(keyStore, keyStorePassword);

        ks.setKeyEntry(alias, privateKey, keyStorePassword, publicCerts);

        saveKeystore(keyStore, keyStorePassword, ks);
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

    private void accessKeystore() {
        String keystorePath = this.options.getSslKeyStore();
        File fKeyStore = new File(keystorePath);
        if (!fKeyStore.exists()) {
            return;
        }

        if (isFirstBoot()) {
            changeDefaultKeystorePassword();
        } else {
            char[] oldPassword = getOldKeystorePassword(keystorePath);

            char[] newPassword = null;
            try {
                newPassword = this.cryptoService.decryptAes(this.options.getSslKeystorePassword().toCharArray());
            } catch (KuraException e) {
                logger.warn("Failed to decrypt keystore password");
            }
            updateKeystorePassword(oldPassword, newPassword);
        }
    }

    private char[] getOldKeystorePassword(String keystorePath) {
        char[] password = this.cryptoService.getKeyStorePassword(keystorePath);
        if (password != null && isKeyStoreAccessible(this.options.getSslKeyStore(), password)) {
            return password;
        }

        try {
            password = this.cryptoService.decryptAes(this.options.getSslKeystorePassword().toCharArray());
        } catch (KuraException e) {
            password = new char[0];
        }

        return password;
    }

    private void updateKeystorePassword(char[] oldPassword, char[] newPassword) {
        try {
            changeKeyStorePassword(this.options.getSslKeyStore(), oldPassword, newPassword);

            this.cryptoService.setKeyStorePassword(this.options.getSslKeyStore(), newPassword);
        } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | UnrecoverableEntryException
                | IOException e) {
            logger.warn("Failed to change keystore password");
        } catch (KuraException e) {
            logger.warn("Failed to persist keystore password");
        }
    }

    private void changeDefaultKeystorePassword() {

        char[] oldPassword = this.systemService.getJavaKeyStorePassword();

        if (isDefaultFromCrypto()) {
            oldPassword = this.cryptoService.getKeyStorePassword(this.options.getSslKeyStore());
        }

        char[] newPassword = new BigInteger(160, new SecureRandom()).toString(32).toCharArray();

        try {
            changeKeyStorePassword(this.options.getSslKeyStore(), oldPassword, newPassword);

            this.cryptoService.setKeyStorePassword(this.options.getSslKeyStore(), newPassword);

            updatePasswordInConfigService(newPassword);
        } catch (Exception e) {
            logger.warn("Keystore password change failed", e);
        }
    }

    private void updatePasswordInConfigService(char[] newPassword) {
        // update our configuration with the newly generated password
        final String pid = (String) this.properties.get("service.pid");

        Map<String, Object> props = new HashMap<>(this.properties);
        props.put(SslManagerServiceOptions.PROP_TRUST_PASSWORD, new Password(newPassword));

        this.selfUpdaterFuture = this.selfUpdaterExecutor.scheduleAtFixedRate(() -> {
            try {
                if (SslManagerServiceImpl.this.ctx.getServiceReference() != null
                        && SslManagerServiceImpl.this.configurationService.getComponentConfiguration(pid) != null) {
                    SslManagerServiceImpl.this.configurationService.updateConfiguration(pid, props);
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
        if (Arrays.equals(SslManagerServiceOptions.PROP_DEFAULT_TRUST_PASSWORD.toCharArray(), snapshotPassword)) {
            result = true;
        }

        return result;
    }

    private char[] getUnencryptedSslKeystorePassword() {
        char[] snapshotPassword = this.options.getSslKeystorePassword().toCharArray();
        try {
            snapshotPassword = this.cryptoService.decryptAes(snapshotPassword);
        } catch (KuraException e) {
            // Nothing to do
        }
        return snapshotPassword;
    }

    private boolean isDefaultFromCrypto() {
        char[] cryptoPassword = this.cryptoService.getKeyStorePassword(this.options.getSslKeyStore());

        return isKeyStoreAccessible(this.options.getSslKeyStore(), cryptoPassword);
    }

    private boolean isDefaultFromUser() {
        return isKeyStoreAccessible(this.options.getSslKeyStore(), this.systemService.getJavaKeyStorePassword());
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    private SSLContext getSSLContextInternal(ConnectionSslOptions options)
            throws GeneralSecurityException, IOException {
        // Only create a new SSLSocketFactory instance if the configuration has
        // changed or
        // for a new alias.
        // This allows for SSL Context Resumption and abbreviated SSL handshake
        // in case of reconnects to the same host.
        SSLContext context = this.sslContexts.get(options);
        if (context == null) {
            logger.info("Creating a new SSLSocketFactory instance");

            TrustManager[] tms = getTrustManagers(options.getTrustStore(), options.getKeyStorePassword());

            KeyManager[] kms = getKeyManagers(options.getKeyStore(), options.getKeyStorePassword(), options.getAlias());

            context = createSSLContext(options.getProtocol(), options.getCiphers(), kms, tms,
                    options.getHostnameVerification());
            this.sslContexts.put(options, context);
        }

        return context;
    }

    private static SSLContext createSSLContext(String protocol, String ciphers, KeyManager[] kms, TrustManager[] tms,
            boolean hostnameVerification) throws NoSuchAlgorithmException, KeyManagementException {
        // inits the SSL context
        SSLContext sslCtx;
        if (protocol == null || protocol.isEmpty()) {
            sslCtx = SSLContext.getDefault();
        } else {
            sslCtx = SSLContext.getInstance(protocol);
            sslCtx.init(kms, tms, null);
        }

        // get the SSLSocketFactory
        final SSLSocketFactory sslSocketFactory = sslCtx.getSocketFactory();
        final SSLSocketFactoryWrapper socketFactoryWrapper = new SSLSocketFactoryWrapper(sslSocketFactory, ciphers,
                hostnameVerification);

        // wrap it
        return new SSLContext(new SSLContextSPIWrapper(sslCtx, socketFactoryWrapper), sslCtx.getProvider(),
                sslCtx.getProtocol()) {
        };
    }

    private static TrustManager[] getTrustManagers(String trustStore, char[] keyStorePassword)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        TrustManager[] result = new TrustManager[0];
        TrustManagerFactory tmf = null;
        if (trustStore != null) {

            // Load the configured the Trust Store
            File fTrustStore = new File(trustStore);
            if (fTrustStore.exists()) {

                KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
                InputStream tsReadStream = new FileInputStream(trustStore);
                ts.load(tsReadStream, keyStorePassword);
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ts);
                result = tmf.getTrustManagers();
                tsReadStream.close();
            }
        }
        return result;
    }

    private KeyManager[] getKeyManagers(String keyStore, char[] keyStorePassword, String keyAlias)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
            UnrecoverableEntryException {
        KeyStore ks = getKeyStore(keyStore, keyStorePassword, keyAlias);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyStorePassword);

        return kmf.getKeyManagers();
    }

    private KeyStore getKeyStore(String keyStore, char[] keyStorePassword, String keyAlias) throws KeyStoreException,
            IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException {

        // Load the configured the Key Store
        File fKeyStore = new File(keyStore);
        if (!fKeyStore.exists() || !isKeyStoreAccessible(keyStore, keyStorePassword)) {
            logger.warn("The referenced keystore does not exist or is not accessible");
            throw new KeyStoreException("The referenced keystore does not exist or is not accessible");
        }

        try (InputStream ksReadStream = new FileInputStream(keyStore);) {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(ksReadStream, keyStorePassword);

            // if we have an alias, then build KeyStore with such key
            if (ks.containsAlias(keyAlias) && ks.isKeyEntry(keyAlias)) {
                PasswordProtection pp = new PasswordProtection(keyStorePassword);
                Entry entry = ks.getEntry(keyAlias, pp);
                ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(null, null);
                ks.setEntry(keyAlias, entry, pp);
            }

            return ks;
        }
    }

    private char[] getKeyStorePassword() {
        return this.cryptoService.getKeyStorePassword(this.options.getSslKeyStore());
    }

    private boolean isKeyStoreAccessible(String location, char[] password) {
        try {
            loadKeystore(location, password);
            return true;
        } catch (Exception e) {
            return false;
        }
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

}
