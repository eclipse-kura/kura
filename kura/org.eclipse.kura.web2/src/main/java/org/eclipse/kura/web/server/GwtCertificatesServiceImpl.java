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
package org.eclipse.kura.web.server;

import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.certificate.CertificatesService;
import org.eclipse.kura.certificate.KuraCertificateEntry;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtCertificate;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCertificatesService;
import org.osgi.framework.ServiceReference;

public class GwtCertificatesServiceImpl extends OsgiRemoteServiceServlet implements GwtCertificatesService {

    /**
     *
     */
    private static final long serialVersionUID = 7402961266449489433L;
    private static final Decoder BASE64_DECODER = Base64.getDecoder();

    @Override
    public void storeKeyPair(GwtXSRFToken xsrfToken, String keyStorePid, String privateKey, String publicCert,
            String alias) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        try {
            // Remove header if exists
            String key = privateKey.replace("-----BEGIN PRIVATE KEY-----", "").replace("\n", "");
            key = key.replace("-----END PRIVATE KEY-----", "");

            byte[] conversion = base64Decode(key);
            // Parse Base64 - after PKCS8
            PKCS8EncodedKeySpec specPriv = new PKCS8EncodedKeySpec(conversion);

            // Create RSA key
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privKey = kf.generatePrivate(specPriv);

            Certificate[] certs = parsePublicCertificates(publicCert);

            if (privKey == null) {
                throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
            } else {
                PrivateKeyEntry privateKeyEntry = new PrivateKeyEntry(privKey, certs);
                final String filter = format("(%s=%s)", "kura.service.pid", keyStorePid);
                final Collection<ServiceReference<KeystoreService>> keystoreServiceReferences = ServiceLocator
                        .getInstance().getServiceReferences(KeystoreService.class, filter);
                for (ServiceReference<KeystoreService> reference : keystoreServiceReferences) {
                    KeystoreService keystoreService = ServiceLocator.getInstance().getService(reference);
                    keystoreService.setEntry(alias, privateKeyEntry);
                }
            }

        } catch (GeneralSecurityException |

                IOException e) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
        }
    }

    @Override
    public void storeCertificate(GwtXSRFToken xsrfToken, String keyStorePid, String certificate, String alias)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        try {
            X509Certificate[] certs = parsePublicCertificates(certificate);

            if (certs.length == 0) {
                throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
            } else {
                CertificatesService certificateService = ServiceLocator.getInstance()
                        .getService(CertificatesService.class);

                for (X509Certificate cert : certs) {
                    certificateService.addCertificate(new KuraCertificateEntry(keyStorePid, alias, cert));
                }
            }

        } catch (CertificateException | UnsupportedEncodingException | KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
        }
    }

    @Override
    public List<String> listKeystoreServicePids() throws GwtKuraException {
        final List<String> pids = new ArrayList<>();

        ServiceLocator.withAllServiceReferences(null, (r, c) -> {
            final Object pid = r.getProperties().get("kura.service.pid");

            if (pid instanceof String) {
                pids.add((String) pid);
            }
        }, KeystoreService.class);

        return pids;
    }

    private X509Certificate[] parsePublicCertificates(String publicKey)
            throws CertificateException, UnsupportedEncodingException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> publicCertificates = certFactory
                .generateCertificates(new ByteArrayInputStream(publicKey.getBytes("UTF-8")));
        Iterator<? extends Certificate> certIterator = publicCertificates.iterator();

        X509Certificate[] certs = new X509Certificate[publicCertificates.size()];
        int i = 0;

        while (certIterator.hasNext()) {
            X509Certificate cert = (X509Certificate) certIterator.next();
            certs[i] = cert;
            i++;
        }
        return certs;
    }

    private byte[] base64Decode(String key) throws GwtKuraException {
        try {
            return BASE64_DECODER.decode(key);
        } catch (final Exception e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public List<GwtCertificate> listCertificates() throws GwtKuraException {
        CertificatesService certificateService = ServiceLocator.getInstance().getService(CertificatesService.class);

        if (certificateService == null) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }

        try {
            List<KuraCertificateEntry> certs = certificateService.getCertificates();
            List<GwtCertificate> certificates = new ArrayList<>();
            certs.forEach(certInfo -> {
                GwtCertificate gwtCertificate = new GwtCertificate();
                gwtCertificate.setAlias(certInfo.getAlias());
                gwtCertificate.setKeystoreName(certInfo.getKeystoreId());
                certificates.add(gwtCertificate);
            });

            return certificates;
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }

    }

    @Override
    public void removeCertificate(GwtXSRFToken xsrfToken, GwtCertificate certificate) throws GwtKuraException {
        CertificatesService certificateService = ServiceLocator.getInstance().getService(CertificatesService.class);

        if (certificateService == null) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }

        String alias = certificate.getAlias();
        String keystoreName = certificate.getKeystoreName();

        try {
            certificateService.deleteCertificate(keystoreName + ":" + alias);
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }

    }
}