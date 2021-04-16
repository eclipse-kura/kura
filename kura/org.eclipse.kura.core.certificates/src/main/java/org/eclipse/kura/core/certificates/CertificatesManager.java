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
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.certificate.CertificatesService;
import org.eclipse.kura.certificate.KuraCertificateEntry;
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
        TrustedCertificateEntry cert;
        try {
            cert = getCertificateEntry(DEFAULT_KEYSTORE_SERVICE_PID + ":" + alias).getCertificateEntry();
        } catch (KuraException e) {
            return null;
        }
        return cert.getTrustedCertificate();
    }

    @Override
    public void storeCertificate(Certificate cert, String alias) throws KuraException {
        if (alias.startsWith(RESOURCE_CERTIFICATE_DM) || alias.startsWith(RESOURCE_CERTIFICATE_BUNDLE)) {
            storeTrustRepoCertificate(cert, alias);
        } else if (alias.startsWith(RESOURCE_CERTIFICATE_LOGIN)) {
            storeLoginCertificate(cert, alias);
        }
    }

    protected void storeLoginCertificate(Certificate cert, String alias) throws KuraException {
        KuraCertificateEntry kuraCertificate = new KuraCertificateEntry(LOGIN_KEYSTORE_SERVICE_PID, alias, cert);
        addCertificate(kuraCertificate);
    }

    protected void storeTrustRepoCertificate(Certificate cert, String alias) throws KuraException {
        KuraCertificateEntry kuraCertificate = new KuraCertificateEntry(DEFAULT_KEYSTORE_SERVICE_PID, alias, cert);
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
    public List<KuraCertificateEntry> getCertificates() throws KuraException {
        List<KuraCertificateEntry> certificates = new ArrayList<>();
        for (Entry<String, KeystoreService> keystoreServiceEntry : this.keystoreServices.entrySet()) {
            String keystoreId = keystoreServiceEntry.getKey();
            try {
                Map<String, java.security.KeyStore.Entry> keystoreEntries = keystoreServiceEntry.getValue()
                        .getEntries();
                keystoreEntries.entrySet().stream().filter(entry -> entry.getValue() instanceof TrustedCertificateEntry)
                        .forEach(entry -> {
                            String alias = entry.getKey();
                            certificates.add(new KuraCertificateEntry(keystoreId, alias,
                                    (TrustedCertificateEntry) entry.getValue()));
                        });
            } catch (GeneralSecurityException | IOException e) {
                throw new KuraIOException(e, "Failed to get certificates from " + keystoreId);
            }
        }
        return certificates;
    }

    @Override
    public KuraCertificateEntry getCertificateEntry(String id) throws KuraException {
        String keystoreId = KuraCertificateEntry.getKeystoreId(id);
        String alias = KuraCertificateEntry.getAlias(id);
        java.security.KeyStore.Entry keystoreEntry = null;
        try {
            keystoreEntry = getKeystore(keystoreId).getEntry(alias);
        } catch (GeneralSecurityException | IOException e) {
            throw new KuraIOException(e, "Failed to get certificates from " + keystoreId);
        }
        if (keystoreEntry instanceof TrustedCertificateEntry) {
            return new KuraCertificateEntry(keystoreId, alias, (TrustedCertificateEntry) keystoreEntry);
        } else {
            throw new KuraIOException("Failed to retrieve certificate " + id);
        }
    }

    @Override
    public void updateCertificate(KuraCertificateEntry certificate) throws KuraException {
        addCertificate(certificate);
    }

    @Override
    public void addCertificate(KuraCertificateEntry certificate) throws KuraException {
        try {
            getKeystore(certificate.getKeystoreId()).setEntry(certificate.getAlias(),
                    certificate.getCertificateEntry());
        } catch (GeneralSecurityException | IOException e) {
            throw new KuraIOException(e, "Failed to add certificate " + certificate.getCertificateId());
        }
    }

    @Override
    public void deleteCertificate(String id) throws KuraException {
        String keystoreId = KuraCertificateEntry.getKeystoreId(id);
        String alias = KuraCertificateEntry.getAlias(id);
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
