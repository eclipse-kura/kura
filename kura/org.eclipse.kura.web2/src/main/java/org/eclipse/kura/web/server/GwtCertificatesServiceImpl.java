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
package org.eclipse.kura.web.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.certificate.CertificateInfo;
import org.eclipse.kura.certificate.CertificatesService;
import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtCertificate;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCertificatesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtCertificatesServiceImpl extends OsgiRemoteServiceServlet implements GwtCertificatesService {

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");
    /**
     *
     */
    private static final long serialVersionUID = 7402961266449489433L;
    private static final Decoder BASE64_DECODER = Base64.getDecoder();

    @Override
    public Integer storeSSLPublicPrivateKeys(GwtXSRFToken xsrfToken, String privateKey, String publicKey,
            String password, String alias) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

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

            Certificate[] certs = parsePublicCertificates(publicKey);

            if (privKey == null) {
                auditLogger.warn("UI Certificate - Failure - Failed to store SSL key for user: {}, session {}",
                        session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
                throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
            } else {
                char[] privateKeyPassword = new char[0];
                if (password != null) {
                    privateKeyPassword = password.toCharArray();
                }
                SslManagerService sslService = ServiceLocator.getInstance().getService(SslManagerService.class);
                sslService.installPrivateKey(alias, privKey, privateKeyPassword, certs);
            }

            auditLogger.info("UI Certificate - Success - Successfully stored SSL key for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            return 1;
        } catch (GeneralSecurityException | IOException e) {
            auditLogger.warn("UI Certificate - Failure - Failed to store SSL key for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
        }
    }

    @Override
    public Integer storeSSLPublicChain(GwtXSRFToken xsrfToken, String publicKeys, String alias)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        try {
            X509Certificate[] certs = parsePublicCertificates(publicKeys);

            if (certs.length == 0) {
                throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
            } else {
                SslManagerService sslService = ServiceLocator.getInstance().getService(SslManagerService.class);

                boolean leafAssigned = false;
                for (X509Certificate cert : certs) {
                    if (!leafAssigned && (cert.getBasicConstraints() == -1
                            || cert.getKeyUsage() != null && !cert.getKeyUsage()[5])) { // certificate is leaf
                        sslService.installTrustCertificate("ssl-" + alias, cert);
                        leafAssigned = true;
                    } else { // Certificate is CA.
                        // http://stackoverflow.com/questions/12092457/how-to-check-if-x509certificate-is-ca-certificate
                        String certificateAlias = "ca-" + cert.getSerialNumber().toString();
                        sslService.installTrustCertificate(certificateAlias, cert);
                    }
                }
            }
            auditLogger.info("UI Certificate - Success - Successfully stored public chain for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            return certs.length;
        } catch (GeneralSecurityException | IOException e) {
            auditLogger.warn("UI Certificate - Failure - Failed to store public chain for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
        }
    }

    @Override
    public Integer storeApplicationPublicChain(GwtXSRFToken xsrfToken, String publicKeys, String alias)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        try {
            X509Certificate[] certs = parsePublicCertificates(publicKeys);

            if (certs.length == 0) {
                throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
            } else {
                CertificatesService certificateService = ServiceLocator.getInstance()
                        .getService(CertificatesService.class);

                boolean leafAssigned = false;
                for (X509Certificate cert : certs) {
                    if (!leafAssigned && (cert.getBasicConstraints() == -1
                            || cert.getKeyUsage() != null && !cert.getKeyUsage()[5])) { // certificate is leaf
                        certificateService.storeCertificate(cert, "bundle-" + alias);
                        leafAssigned = true;
                    } else { // Certificate is CA.
                        // http://stackoverflow.com/questions/12092457/how-to-check-if-x509certificate-is-ca-certificate
                        String certificateAlias = "bundle-" + cert.getSerialNumber().toString();
                        certificateService.storeCertificate(cert, certificateAlias);
                    }
                }
            }

            auditLogger.info(
                    "UI Certificate - Success - Successfully stored application public chain for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());

            return certs.length;
        } catch (CertificateException | UnsupportedEncodingException | KuraException e) {
            auditLogger.warn(
                    "UI Certificate - Failure - Failed to store application public chain for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
        }
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
    public Integer storeLoginPublicChain(GwtXSRFToken xsrfToken, String publicCert) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        try {
            X509Certificate[] certs = parsePublicCertificates(publicCert);

            if (certs.length == 0) {
                throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
            } else {
                CertificatesService certificateService = ServiceLocator.getInstance()
                        .getService(CertificatesService.class);

                for (X509Certificate cert : certs) {
                    String certificateAlias = "login-" + cert.getSerialNumber().toString();
                    certificateService.storeCertificate(cert, certificateAlias);
                }
            }

            auditLogger.info(
                    "UI Certificate - Success - Successfully stored login public chain for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());

            return certs.length;
        } catch (CertificateException | UnsupportedEncodingException | KuraException e) {
            auditLogger.warn("UI Certificate - Failure - Failed to store login public chain for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
        }
    }

    @Override
    public List<GwtCertificate> listCertificates() throws GwtKuraException {
        CertificatesService certificateService = ServiceLocator.getInstance().getService(CertificatesService.class);

        if (certificateService == null) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        try {
            Set<CertificateInfo> certInfos = certificateService.listStoredCertificates();
            List<GwtCertificate> certificates = new ArrayList<>();
            certInfos.forEach(certInfo -> {
                GwtCertificate gwtCertificate = new GwtCertificate();
                gwtCertificate.setAlias(certInfo.getAlias());
                gwtCertificate.setType(certInfo.getType().name());
                certificates.add(gwtCertificate);
            });

            auditLogger.info(
                    "UI Certificate - Success - Successfully listed stored certificates for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            return certificates;
        } catch (KuraException e) {
            auditLogger.warn("UI Certificate - Failure - Failed to list certificates for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }

    }

    @Override
    public void removeCertificate(GwtXSRFToken xsrfToken, GwtCertificate certificate) throws GwtKuraException {
        CertificatesService certificateService = ServiceLocator.getInstance().getService(CertificatesService.class);

        if (certificateService == null) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        String alias = certificate.getAlias();

        try {
            certificateService.removeCertificate(alias);
            auditLogger.info(
                    "UI Certificate - Success - Successfully removed certificate for user: {}, session {}, alias: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), alias);
        } catch (KuraException e) {
            auditLogger.warn(
                    "UI Certificate - Failure - Failed to remove certificate for user: {}, session {}, alias: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), alias);
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }

    }

    @Override
    public Integer storeLoginPublicPrivateKeys(GwtXSRFToken xsrfToken, String privateKey, String publicKey,
            String password, String alias) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

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

            Certificate[] certs = parsePublicCertificates(publicKey);

            if (privKey == null) {
                auditLogger.warn("UI Certificate - Failure - Failed to store UI HTTPS key for user: {}, session {}",
                        session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
                throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
            } else {
                char[] privateKeyPassword = new char[0];
                if (password != null) {
                    privateKeyPassword = password.toCharArray();
                }
                CertificatesService certificateService = ServiceLocator.getInstance()
                        .getService(CertificatesService.class);
                certificateService.installPrivateKey("login-" + alias, privKey, privateKeyPassword, certs);
            }

            auditLogger.info("UI Certificate - Success - Successfully stored UI HTTPS key for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            return 1;
        } catch (GeneralSecurityException | IOException | KuraException e) {
            auditLogger.warn("UI Certificate - Failure - Failed to store UI HTTPS key for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
        }
    }
}