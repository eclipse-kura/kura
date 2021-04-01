/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.certificates;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.certificate.CertificateInfo;
import org.eclipse.kura.certificate.CertificatesService;
import org.eclipse.kura.certificate.KuraCertificate;
import org.eclipse.kura.certificate.KuraPrivateKey;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.message.KuraApplicationTopic;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 *
 */
public class CertificatesManager implements CertificatesService {

    private static final Logger logger = LoggerFactory.getLogger(CertificatesManager.class);

    public static final String APP_ID = "org.eclipse.kura.core.certificates.CertificatesManager";

    private static final String RESOURCE_CERTIFICATE_DM = "dm";
    private static final String RESOURCE_CERTIFICATE_LOGIN = "login";
    private static final String RESOURCE_CERTIFICATE_BUNDLE = "bundle";

    private static final String LOGIN_KEYSTORE_SERVICE_PID = "HttpsKeystore";
    private static final String SSL_KEYSTORE_SERVICE_PID = "org.eclipse.kura.ssl.SslManagerService";
    private static final String DEFAULT_KEYSTORE_SERVICE_PID = "org.eclipse.kura.crypto.CryptoService";

    private CryptoService cryptoService;
    private ConfigurationService configurationService;
    private Map<String, KeystoreService> keystoreServices = new HashMap<>();
    private BundleContext bundleContext;
    private ServiceTrackerCustomizer<KeystoreService, KeystoreService> keystoreServiceTrackerCustomizer;
    private ServiceTracker<KeystoreService, KeystoreService> keystoreServiceTracker;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void unsetCryptoService(CryptoService cryptoService) {
        if (this.cryptoService == cryptoService) {
            this.cryptoService = null;
        }
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void unsetConfigurationService(ConfigurationService configurationService) {
        if (this.configurationService == configurationService) {
            this.configurationService = null;
        }
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {
        this.bundleContext = componentContext.getBundleContext();
        this.keystoreServiceTrackerCustomizer = new KeystoreServiceTrackerCustomizer();
        initKeystoreServiceTracking();
        logger.info("Bundle {} has started!", APP_ID);
    }

    protected void deactivate(ComponentContext componentContext) {
        if (this.keystoreServiceTracker != null) {
            this.keystoreServiceTracker.close();
        }
        logger.info("Bundle {} is deactivating!", APP_ID);
    }

    @Override
    public Certificate returnCertificate(String alias) throws KuraException {
        Optional<Certificate> cert = getCertificate(DEFAULT_KEYSTORE_SERVICE_PID + ":" + alias).getCertificate();
        if (cert.isPresent()) {
            return cert.get();
        }
        return null;
    }

    @Override
    public void storeCertificate(Certificate cert, String alias) throws KuraException {
        if (alias.startsWith(RESOURCE_CERTIFICATE_DM) || alias.startsWith(RESOURCE_CERTIFICATE_BUNDLE)) {
            storeTrustRepoCertificate(cert, alias);
        } else if (alias.startsWith(RESOURCE_CERTIFICATE_LOGIN)) {
            storeLoginCertificate(cert, alias);
        }
    }

    private void storeLoginCertificate(Certificate cert, String alias) throws KuraException {
        KuraCertificate kuraCertificate = new KuraCertificate(LOGIN_KEYSTORE_SERVICE_PID, alias, cert);
        addCertificate(kuraCertificate);
    }

    private void storeTrustRepoCertificate(Certificate cert, String alias) throws KuraException {
        KuraCertificate kuraCertificate = new KuraCertificate(DEFAULT_KEYSTORE_SERVICE_PID, alias, cert);
        addCertificate(kuraCertificate);
    }

    @Override
    public Enumeration<String> listBundleCertificatesAliases() {
        return listCertificatesAliases(DEFAULT_KEYSTORE_SERVICE_PID);
    }

    @Override
    public Enumeration<String> listDMCertificatesAliases() {
        return listCertificatesAliases(DEFAULT_KEYSTORE_SERVICE_PID);
    }

    @Override
    public Enumeration<String> listSSLCertificatesAliases() {
        return listCertificatesAliases(SSL_KEYSTORE_SERVICE_PID);
    }

    @Override
    public Enumeration<String> listCACertificatesAliases() {
        return listCertificatesAliases(DEFAULT_KEYSTORE_SERVICE_PID);
    }

    @Override
    public void removeCertificate(String alias) throws KuraException {
        for (Entry<String, KeystoreService> keystoreServiceEntry : this.keystoreServices.entrySet()) {
            try {
                keystoreServiceEntry.getValue().deleteEntry(alias);
            } catch (GeneralSecurityException | IOException e) {
                throw new KuraIOException(e, "Failed to remove entry " + alias);
            }
        }
    }

    @Override
    public boolean verifySignature(KuraApplicationTopic kuraTopic, KuraPayload kuraPayload) {
        return true;
    }

    private Enumeration<String> listCertificatesAliases(String keystoreId) {
        try {
            return Collections.enumeration(getKeystore(keystoreId).getAliases());
        } catch (IllegalArgumentException | GeneralSecurityException | IOException | KuraException e) {
            return Collections.emptyEnumeration();
        }
    }

    @Override
    public Set<CertificateInfo> listStoredCertificates() throws KuraException {
        Set<CertificateInfo> certsInfo = new HashSet<>();
        for (Entry<String, KeystoreService> keystoreServiceEntry : this.keystoreServices.entrySet()) {
            try {
                keystoreServiceEntry.getValue().getAliases().stream()
                        .forEach(alias -> certsInfo.add(new CertificateInfo(alias, keystoreServiceEntry.getKey())));
            } catch (GeneralSecurityException | IOException e) {
                throw new KuraIOException(e, "Failed to get certificates info from " + keystoreServiceEntry.getKey());
            }
        }
        return certsInfo;
    }

    @Override
    public void addPrivateKey(KuraPrivateKey privateKey) throws KuraException {
        KeystoreService service = getKeystore(privateKey.getKeystoreId());
        if (privateKey.getPrivateKey().isPresent() && privateKey.getCertificateChain().isPresent()) {
            PrivateKeyEntry entry = new PrivateKeyEntry(privateKey.getPrivateKey().get(),
                    privateKey.getCertificateChain().get());
            try {
                service.setEntry(privateKey.getAlias(), entry);
            } catch (GeneralSecurityException | IOException e) {
                throw new KuraIOException(e, "Error adding a key pair to the keystore " + privateKey.getKeystoreId());
            }
        }
    }

    @Override
    public List<KuraCertificate> getCertificates() throws KuraException {
        List<KuraCertificate> certificates = new ArrayList<>();
        for (Entry<String, KeystoreService> keystoreServiceEntry : this.keystoreServices.entrySet()) {
            String keystoreId = keystoreServiceEntry.getKey();
            try {
                Map<String, java.security.KeyStore.Entry> keystoreEntries = keystoreServiceEntry.getValue()
                        .getEntries();
                keystoreEntries.entrySet().stream().forEach(entry -> {
                    String alias = entry.getKey();
                    certificates.add(new KuraCertificate(keystoreId, alias, getCertificateFromEntry(entry.getValue())));
                });
            } catch (GeneralSecurityException | IOException e) {
                throw new KuraIOException(e, "Failed to get certificates from " + keystoreId);
            }
        }
        return certificates;
    }

    @Override
    public KuraCertificate getCertificate(String id) throws KuraException {
        String keystoreId = KuraCertificate.getKeystoreId(id);
        String alias = KuraCertificate.getAlias(id);
        java.security.KeyStore.Entry keystoreEntry = null;
        try {
            keystoreEntry = getKeystore(keystoreId).getEntry(alias);
        } catch (GeneralSecurityException | IOException e) {
            throw new KuraIOException(e, "Failed to get certificates from " + keystoreId);
        }
        Certificate cert = getCertificateFromEntry(keystoreEntry);
        return new KuraCertificate(keystoreId, alias, cert);
    }

    @Override
    public void updateCertificate(KuraCertificate certificate) throws KuraException {
        addCertificate(certificate);
    }

    @Override
    public void addCertificate(KuraCertificate certificate) throws KuraException {
        if (certificate.getCertificate().isPresent()) {
            try {
                getKeystore(certificate.getKeystoreId()).setEntry(certificate.getAlias(),
                        new TrustedCertificateEntry(certificate.getCertificate().get()));
            } catch (GeneralSecurityException | IOException e) {
                throw new KuraIOException(e, "Failed to add certificate " + certificate.getCertificateId());
            }
        }
    }

    @Override
    public void deleteCertificate(String id) throws KuraException {
        String keystoreId = KuraCertificate.getKeystoreId(id);
        String alias = KuraCertificate.getAlias(id);
        try {
            getKeystore(keystoreId).deleteEntry(alias);
        } catch (GeneralSecurityException | IOException e) {
            throw new KuraIOException(e, "Failed to delete certificate " + id);
        }
    }

    private void initKeystoreServiceTracking() {
        String filterString = String.format("(&(%s=%s))", Constants.OBJECTCLASS, KeystoreService.class.getName());
        Filter filter = null;
        try {
            filter = this.bundleContext.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            logger.error("Filter setup exception ", e);
        }
        this.keystoreServiceTracker = new ServiceTracker<>(this.bundleContext, filter,
                this.keystoreServiceTrackerCustomizer);
        this.keystoreServiceTracker.open();
    }

    private Certificate getCertificateFromEntry(java.security.KeyStore.Entry keystoreEntry) {
        Certificate cert = null;
        if (keystoreEntry != null) {
            if (keystoreEntry instanceof PrivateKeyEntry) {
                cert = ((PrivateKeyEntry) keystoreEntry).getCertificate();
            } else if (keystoreEntry instanceof TrustedCertificateEntry) {
                cert = ((TrustedCertificateEntry) keystoreEntry).getTrustedCertificate();
            }
        }
        return cert;
    }

    private KeystoreService getKeystore(String keystoreId) throws KuraException {
        KeystoreService service = this.keystoreServices.get(keystoreId);
        if (service == null) {
            throw new KuraException(KuraErrorCode.SERVICE_UNAVAILABLE, "KeystoreService " + keystoreId + " not found");
        }
        return service;
    }

    private final class KeystoreServiceTrackerCustomizer
            implements ServiceTrackerCustomizer<KeystoreService, KeystoreService> {

        private static final String KURA_SERVICE_PID = "kura.service.pid";

        @Override
        public KeystoreService addingService(final ServiceReference<KeystoreService> reference) {
            String kuraServicePid = (String) reference.getProperty(KURA_SERVICE_PID);
            CertificatesManager.this.keystoreServices.put(kuraServicePid,
                    CertificatesManager.this.bundleContext.getService(reference));
            return CertificatesManager.this.keystoreServices.get(kuraServicePid);
        }

        @Override
        public void modifiedService(final ServiceReference<KeystoreService> reference, final KeystoreService service) {
            String kuraServicePid = (String) reference.getProperty(KURA_SERVICE_PID);
            CertificatesManager.this.keystoreServices.put(kuraServicePid,
                    CertificatesManager.this.bundleContext.getService(reference));
        }

        @Override
        public void removedService(final ServiceReference<KeystoreService> reference, final KeystoreService service) {
            String kuraServicePid = (String) reference.getProperty(KURA_SERVICE_PID);
            CertificatesManager.this.keystoreServices.remove(kuraServicePid);
        }
    }
}
