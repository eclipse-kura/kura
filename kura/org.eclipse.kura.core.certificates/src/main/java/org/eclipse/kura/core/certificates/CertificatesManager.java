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
package org.eclipse.kura.core.certificates;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.certificate.CertificateInfo;
import org.eclipse.kura.certificate.CertificateType;
import org.eclipse.kura.certificate.CertificatesService;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.message.KuraApplicationTopic;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 *
 */
public class CertificatesManager implements CertificatesService {

    private static final Logger logger = LoggerFactory.getLogger(CertificatesManager.class);

    private static final String DEFAULT_KEYSTORE = System.getProperty("org.osgi.framework.trust.repositories");

    public static final String APP_ID = "org.eclipse.kura.core.certificates.CertificatesManager";

    private static final String RESOURCE_CERTIFICATE_DM = "dm";

    private static final String HTTP_SERVICE_PID = "org.eclipse.kura.http.server.manager.HttpService";
    private static final String SSL_SERVICE_PID = "org.eclipse.kura.ssl.SslManagerService";

    private CryptoService cryptoService;
    private ConfigurationService configurationService;

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

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {
        logger.info("Bundle {} has started!", APP_ID);
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("Bundle {} is deactivating!", APP_ID);
    }

    @Override
    public Certificate returnCertificate(String alias) throws KuraException {
        try {
            char[] keystorePassword = this.cryptoService.getKeyStorePassword(DEFAULT_KEYSTORE);
            return getCertificateFromKeyStore(keystorePassword, alias);
        } catch (Exception e) {
            throw KuraException.internalError("Error retrieving the certificate from the keystore");
        }
    }

    @Override
    public void storeCertificate(Certificate arg1, String alias) throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public Enumeration<String> listBundleCertificatesAliases() {
        return listStoredCertificatesAliases();
    }

    @Override
    public Enumeration<String> listDMCertificatesAliases() {
        return listStoredCertificatesAliases();
    }

    @Override
    public Enumeration<String> listSSLCertificatesAliases() {
        try {
            String path = getSslKeystorePath();
            char[] keystorePassword = this.cryptoService.getKeyStorePassword(path);
            return getAliasesFromKeyStore(path, keystorePassword);
        } catch (Exception e) {
            return Collections.emptyEnumeration();
        }
    }

    protected String getSslKeystorePath() throws KuraException {
        ComponentConfiguration cc = this.configurationService.getComponentConfiguration(SSL_SERVICE_PID);
        return (String) cc.getConfigurationProperties().get("ssl.default.trustStore");
    }

    @Override
    public Enumeration<String> listCACertificatesAliases() {
        return listStoredCertificatesAliases();
    }

    @Override
    public void removeCertificate(String alias) throws KuraException {
        removeTrustRepoCertificate(alias);
        removeSslCertificate(alias);
        removeLoginCertificate(alias);
    }

    private void removeTrustRepoCertificate(String alias) {
        try {
            String path = DEFAULT_KEYSTORE;
            removeCertificate(alias, path);
        } catch (Exception e) {
            logger.info("Impossible to remove the certificate with alias: {} from trust keystore", alias);
        }
    }

    private void removeSslCertificate(String alias) {
        try {
            String path = getSslKeystorePath();
            removeCertificate(alias, path);
        } catch (Exception e) {
            logger.info("Impossible to remove the certificate with alias: {} from ssl keystore", alias);
        }
    }

    private void removeLoginCertificate(String alias) {
        try {
            String path = getLoginKeystorePath();
            removeCertificate(alias, path);
        } catch (Exception e) {
            logger.info("Impossible to remove the certificate with alias: {} from login keystore", alias);
        }
    }

    private void removeCertificate(String alias, String path)
            throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException {
        char[] keystorePassword = this.cryptoService.getKeyStorePassword(path);
        KeyStore ks = KeyStoreManagement.loadKeyStore(path, keystorePassword);
        ks.deleteEntry(alias);
        KeyStoreManagement.saveKeyStore(path, ks, keystorePassword);
    }

    @Override
    public boolean verifySignature(KuraApplicationTopic kuraTopic, KuraPayload kuraPayload) {
        return true;
    }

    protected Certificate getCertificateFromKeyStore(char[] keyStorePassword, String alias)
            throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
        KeyStore ks = KeyStoreManagement.loadKeyStore(keyStorePassword);
        return ks.getCertificate(alias);
    }

    protected Enumeration<String> getAliasesFromKeyStore(char[] keyStorePassword)
            throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
        KeyStore ks = KeyStoreManagement.loadKeyStore(keyStorePassword);
        return ks.aliases();
    }

    protected Enumeration<String> getAliasesFromKeyStore(final String path, char[] keyStorePassword)
            throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
        KeyStore ks = KeyStoreManagement.loadKeyStore(path, keyStorePassword);
        return ks.aliases();
    }

    private Enumeration<String> listStoredCertificatesAliases() {
        try {
            char[] keystorePassword = this.cryptoService.getKeyStorePassword(DEFAULT_KEYSTORE);
            return getAliasesFromKeyStore(keystorePassword);
        } catch (Exception e) {
            return Collections.emptyEnumeration();
        }
    }

    private Enumeration<String> listLoginAliases() throws KuraException {
        String path = getLoginKeystorePath();
        KeyStore ks = null;
        try {
            char[] keystorePassword = this.cryptoService.getKeyStorePassword(path);
            ks = KeyStoreManagement.loadKeyStore(path, keystorePassword);
            return ks.aliases();
        } catch (Exception e) {
            return Collections.emptyEnumeration();
        }
    }

    private String getLoginKeystorePath() throws KuraException {
        ComponentConfiguration cc = this.configurationService.getComponentConfiguration(HTTP_SERVICE_PID);
        return (String) cc.getConfigurationProperties().get("https.keystore.path");
    }

    @Override
    public Set<CertificateInfo> listStoredCertificates() throws KuraException {
        List<String> trustRepoCertAliases = Collections.list(listStoredCertificatesAliases());
        List<String> loginCertAliases = Collections.list(listLoginAliases());
        List<String> sslCertAliases = Collections.list(listSSLCertificatesAliases());

        Set<CertificateInfo> certsInfo = new HashSet<>();

        trustRepoCertAliases.forEach(trustRepoCertAlias -> {
            CertificateType type;
            if (trustRepoCertAlias.startsWith(RESOURCE_CERTIFICATE_DM)) {
                type = CertificateType.DM;
            } else {
                type = CertificateType.BUNDLE;
            }
            CertificateInfo certificateInfo = new CertificateInfo(trustRepoCertAlias, type);
            certsInfo.add(certificateInfo);
        });

        loginCertAliases.forEach(loginCert -> certsInfo.add(new CertificateInfo(loginCert, CertificateType.LOGIN)));
        sslCertAliases.forEach(sslCertAlias -> certsInfo.add(new CertificateInfo(sslCertAlias, CertificateType.SSL)));
        return certsInfo;
    }

    @Override
    public void installPrivateKey(String alias, PrivateKey privateKey, char[] password, Certificate[] publicCerts)
            throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
    }
}
