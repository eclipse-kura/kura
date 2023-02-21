/**
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 */
package org.eclipse.kura.internal.driver.opcua.auth;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.milo.opcua.stack.core.security.TrustListManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class TrustListManagerImpl implements TrustListManager {

    private static final Logger logger = LoggerFactory.getLogger(TrustListManagerImpl.class);

    private final Set<X509Certificate> issuerCertificates = new HashSet<>();
    private final Set<X509CRL> issuerCrls = new HashSet<>();
    private final Set<X509CRL> trustedCrls = new HashSet<>();
    private final Set<X509Certificate> rejectedCertificates = new HashSet<>();
    private final Set<X509Certificate> trustedCertificates = new HashSet<>();

    public TrustListManagerImpl(final KeyStore keyStore) throws KeyStoreException {

        final Set<X509Certificate> trustedCerts = new HashSet<>();
        final Set<X509Certificate> issuerCerts = new HashSet<>();

        final Enumeration<String> aliases = keyStore.aliases();

        while (aliases.hasMoreElements()) {
            final String alias = aliases.nextElement();

            if (!keyStore.isCertificateEntry(alias)) {
                continue;
            }

            final X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

            if (isSelfSigned(cert) && cert.getBasicConstraints() != -1) {
                issuerCerts.add(cert);
            } else {
                trustedCerts.add(cert);
            }
        }

        logger.debug("available issuer certificates: {}", issuerCerts.size());
        logger.debug("available trusted certificates: {}", trustedCerts.size());

        this.trustedCertificates.addAll(trustedCerts);
        this.issuerCertificates.addAll(issuerCerts);

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

    @Override
    public synchronized ImmutableList<X509Certificate> getIssuerCertificates() {
        return ImmutableList.copyOf(issuerCertificates);
    }

    @Override
    public synchronized ImmutableList<X509CRL> getIssuerCrls() {
        return ImmutableList.copyOf(issuerCrls);
    }

    @Override
    public synchronized ImmutableList<X509Certificate> getRejectedCertificates() {
        return ImmutableList.copyOf(rejectedCertificates);
    }

    @Override
    public synchronized ImmutableList<X509Certificate> getTrustedCertificates() {
        return ImmutableList.copyOf(trustedCertificates);
    }

    @Override
    public synchronized void addIssuerCertificate(X509Certificate certificate) {
        this.issuerCertificates.add(certificate);
    }

    @Override
    public synchronized void addRejectedCertificate(X509Certificate certificate) {
        this.rejectedCertificates.add(certificate);
    }

    @Override
    public synchronized void addTrustedCertificate(X509Certificate certificate) {
        this.trustedCertificates.add(certificate);
    }

    @Override
    public synchronized ImmutableList<X509CRL> getTrustedCrls() {
        return ImmutableList.copyOf(trustedCrls);
    }

    @Override
    public synchronized boolean removeIssuerCertificate(ByteString arg0) {
        return false;
    }

    @Override
    public synchronized boolean removeRejectedCertificate(ByteString arg0) {
        return false;
    }

    @Override
    public synchronized boolean removeTrustedCertificate(ByteString arg0) {
        return false;
    }

    @Override
    public synchronized void setIssuerCertificates(List<X509Certificate> certificates) {
        this.issuerCertificates.clear();
        this.issuerCertificates.addAll(certificates);
    }

    @Override
    public synchronized void setIssuerCrls(List<X509CRL> crls) {
        this.issuerCrls.clear();
        this.issuerCrls.addAll(crls);
    }

    @Override
    public synchronized void setTrustedCertificates(List<X509Certificate> certificates) {
        this.trustedCertificates.clear();
        this.trustedCertificates.addAll(certificates);
    }

    @Override
    public synchronized void setTrustedCrls(List<X509CRL> crls) {
        this.trustedCrls.clear();
        this.trustedCrls.addAll(crls);
    }

}
