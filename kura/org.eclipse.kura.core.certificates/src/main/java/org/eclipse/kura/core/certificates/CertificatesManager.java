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

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.certificate.CertificatesService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraApplicationTopic;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 *
 */
public class CertificatesManager implements CertificatesService {

    private static final Logger s_logger = LoggerFactory.getLogger(CertificatesManager.class);

    private static final String DEFAULT_KEYSTORE = System.getProperty("org.osgi.framework.trust.repositories");

    public static final String APP_ID = "org.eclipse.kura.core.certificates.CertificatesManager";

    private CryptoService cryptoService;

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
        return listStoredCertificatesAliases();
    }

    @Override
    public Enumeration<String> listCACertificatesAliases() {
        return listStoredCertificatesAliases();
    }

    @Override
    public void removeCertificate(String alias) throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
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

    private Enumeration<String> listStoredCertificatesAliases() {
        try {
            char[] keystorePassword = this.cryptoService.getKeyStorePassword(DEFAULT_KEYSTORE);
            return getAliasesFromKeyStore(keystorePassword);
        } catch (Exception e) {
            return null;
        }
    }
}
