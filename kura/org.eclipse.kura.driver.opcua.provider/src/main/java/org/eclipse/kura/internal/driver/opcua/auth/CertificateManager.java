/**
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *  
 */

package org.eclipse.kura.internal.driver.opcua.auth;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.application.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.util.CertificateValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CertificateManager implements CertificateValidator {

    private static final Logger logger = LoggerFactory.getLogger(CertificateManager.class);

    private final boolean enabled;

    private Set<X509Certificate> trustedCertificates = Collections.emptySet();
    private Set<X509Certificate> issuerCertificates = Collections.emptySet();

    private KeyPair clientKeyPair;
    private X509Certificate clientCertificate;

    public CertificateManager(final boolean enabled) {
        this.enabled = enabled;
    }

    public abstract void load() throws Exception;

    public KeyPair getClientKeyPair() {
        return clientKeyPair;
    }

    public X509Certificate getClientCertificate() {
        return clientCertificate;
    }

    @Override
    public void validate(X509Certificate certificate) throws UaException {
        if (!enabled) {
            logger.debug("skipping certificate validation");
            return;
        }

        logger.debug("validating certificate: {}", certificate);

        CertificateValidationUtil.validateCertificateValidity(certificate);

        logger.debug("certificate validation successful");
    }

    @Override
    public void verifyTrustChain(List<X509Certificate> certificateChain) throws UaException {
        if (!enabled) {
            logger.debug("skipping certificate chain verification");
            return;
        }

        logger.debug("verifiyng certificate chain: {}", certificateChain);

        CertificateValidationUtil.verifyTrustChain(certificateChain, trustedCertificates, issuerCertificates);

        logger.debug("certificate chain verification successful");
    }

    protected void load(final X509Certificate[] certificates, final KeyPair clientKeyPair,
            final X509Certificate clientCertificate) {
        final Set<X509Certificate> trustedCerts = new HashSet<>();
        final Set<X509Certificate> issuerCerts = new HashSet<>();

        for (final X509Certificate cert : certificates) {
            if (isSelfSigned(cert)) {
                issuerCerts.add(cert);
            } else {
                trustedCerts.add(cert);
            }
        }

        logger.debug("client certificate loaded: {}", clientCertificate != null);
        logger.debug("client key pair loaded: {}", clientKeyPair != null);
        logger.debug("available issuer certificates: {}", issuerCerts.size());
        logger.debug("available trusted certificates: {}", trustedCerts.size());

        this.trustedCertificates = trustedCerts;
        this.issuerCertificates = issuerCerts;

        this.clientKeyPair = clientKeyPair;
        this.clientCertificate = clientCertificate;
    }

    private static boolean isSelfSigned(final X509Certificate cert) {
        final PublicKey key = cert.getPublicKey();
        try {
            cert.verify(key);
            return true;
        } catch (final Exception e) {
            return false;
        }
    }
}
