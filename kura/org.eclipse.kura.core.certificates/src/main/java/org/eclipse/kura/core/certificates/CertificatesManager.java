/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Enumeration;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.certificate.CertificatesService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraTopic;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 *
 */
public final class CertificatesManager implements CertificatesService {

    private static final Logger s_logger = LoggerFactory.getLogger(CertificatesManager.class);

    private static final String DEFAULT_KEYSTORE = System.getProperty("org.osgi.framework.trust.repositories");

    public static final String APP_ID = "org.eclipse.kura.core.certificates.CertificatesManager";

    private CryptoService m_cryptoService;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setCryptoService(CryptoService cryptoService) {
        this.m_cryptoService = cryptoService;
    }

    public void unsetCryptoService(CryptoService cryptoService) {
        this.m_cryptoService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {
        s_logger.info("Bundle " + APP_ID + " has started!");
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.info("Bundle " + APP_ID + " is deactivating!");
    }

    @Override
    public Certificate returnCertificate(String alias) throws KuraException {
        KeyStore ks = null;
        try {
            char[] keystorePassword = this.m_cryptoService.getKeyStorePassword(DEFAULT_KEYSTORE);
            ks = KeyStoreManagement.loadKeyStore(keystorePassword);
            return ks.getCertificate(alias);
        } catch (Exception e) {
            throw KuraException.internalError("Error retrieving the certificate from the keystore");
        }
    }

    @Override
    public void storeCertificate(Certificate arg1, String alias) throws KuraException {
        return;
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
        return listStoredCertificatesAliases();
    }

    @Override
    public Enumeration<String> listCACertificatesAliases() {
        return listStoredCertificatesAliases();
    }

    @Override
    public void removeCertificate(String alias) {
        return;
    }

    @Override
    public boolean verifySignature(KuraTopic kuraTopic, KuraPayload kuraPayload) {
        return true;
    }

    private Enumeration<String> listStoredCertificatesAliases() {
        KeyStore ks = null;
        try {
            char[] keystorePassword = this.m_cryptoService.getKeyStorePassword(DEFAULT_KEYSTORE);
            ks = KeyStoreManagement.loadKeyStore(keystorePassword);
            return ks.aliases();
        } catch (Exception e) {
            return null;
        }
    }
}
