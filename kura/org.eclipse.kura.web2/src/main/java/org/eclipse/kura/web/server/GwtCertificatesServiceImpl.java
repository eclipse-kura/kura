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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.certificate.CertificatesService;
import org.eclipse.kura.certificate.KuraCertificateEntry;
import org.eclipse.kura.core.keystore.util.KeystoreRemoteService;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtKeystoreEntry;
import org.eclipse.kura.web.shared.model.GwtKeystoreEntry.Kind;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCertificatesService;
import org.osgi.framework.ServiceReference;

public class GwtCertificatesServiceImpl extends OsgiRemoteServiceServlet implements GwtCertificatesService {

    /**
     *
     */
    private static final long serialVersionUID = 7402961266449489433L;
    private static final String KURA_SERVICE_PID = "kura.service.pid";

    @Override
    public void storeKeyPair(GwtXSRFToken xsrfToken, String keyStorePid, String privateKey, String publicCert,
            String alias) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        try {
            PrivateKeyEntry entry = KeystoreRemoteService.createPrivateKey(privateKey, publicCert);

            if (entry == null) {
                throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
            } else {
                final String filter = format("(%s=%s)", KURA_SERVICE_PID, keyStorePid);

                final Collection<ServiceReference<KeystoreService>> keystoreServiceReferences = ServiceLocator
                        .getInstance().getServiceReferences(KeystoreService.class, filter);
                for (ServiceReference<KeystoreService> reference : keystoreServiceReferences) {
                    KeystoreService keystoreService = ServiceLocator.getInstance().getService(reference);
                    keystoreService.setEntry(alias, entry);
                    ServiceLocator.getInstance().ungetService(reference);
                }
            }

        } catch (GeneralSecurityException | IOException | KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
        }
    }

    @Override
    public void storeCertificate(GwtXSRFToken xsrfToken, String keyStorePid, String certificate, String alias)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        try {
            X509Certificate[] certs = KeystoreRemoteService.parsePublicCertificates(certificate);

            if (certs.length == 0) {
                throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
            } else {
                CertificatesService certificateService = ServiceLocator.getInstance()
                        .getService(CertificatesService.class);

                for (X509Certificate cert : certs) {
                    certificateService.addCertificate(new KuraCertificateEntry(keyStorePid, alias, cert));
                }
            }

        } catch (CertificateException | KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
        }
    }

    @Override
    public List<String> listKeystoreServicePids() throws GwtKuraException {
        final List<String> pids = new ArrayList<>();

        ServiceLocator.withAllServiceReferences(null, (r, c) -> {
            final Object pid = r.getProperties().get(KURA_SERVICE_PID);

            if (pid instanceof String) {
                pids.add((String) pid);
            }
        }, KeystoreService.class);

        return pids;
    }

    @Override
    public List<GwtKeystoreEntry> listEntries() throws GwtKuraException {

        List<GwtKeystoreEntry> result = new ArrayList<>();

        ServiceLocator.withAllServiceReferences(KeystoreService.class, null, (ref, context) -> {

            final Object kuraServicePid = ref.getProperty(KURA_SERVICE_PID);

            if (!(kuraServicePid instanceof String)) {
                return;
            }

            final KeystoreService service = context.getService(ref);

            if (service == null) {
                return;
            }

            try {
                for (final Map.Entry<String, Entry> e : service.getEntries().entrySet()) {

                    final Kind kind;
                    
                    Date validityStartDate = null;
                    Date validityEndDate = null;

                    if (e.getValue() instanceof PrivateKeyEntry) {
                        kind = Kind.KEY_PAIR;
                        
                        PrivateKeyEntry pke = (PrivateKeyEntry) e.getValue();
                        Certificate[] chain = pke.getCertificateChain();
                        
                        if(chain.length > 0) {
                        	Certificate leaf = chain[chain.length - 1];
                        	
                        	if(leaf instanceof X509Certificate) {
                        		validityStartDate = ((X509Certificate) leaf).getNotBefore();
                        		validityEndDate = ((X509Certificate) leaf).getNotAfter();
                        	}
                        }
                    } else if (e.getValue() instanceof TrustedCertificateEntry) {
                        kind = Kind.TRUSTED_CERT;

                        Certificate cert = ((TrustedCertificateEntry) e.getValue()).getTrustedCertificate();
                        
                        if(cert instanceof X509Certificate) {
                        	validityStartDate = ((X509Certificate) cert).getNotBefore();
                            validityEndDate = ((X509Certificate) cert).getNotAfter();
                        }
                    } else if (e.getValue() instanceof SecretKeyEntry) {
                        kind = Kind.SECRET_KEY;
                    } else {
                        continue;
                    }

                    result.add(new GwtKeystoreEntry(e.getKey(), (String) kuraServicePid, kind, validityStartDate, validityEndDate));
                }
            } finally {
                context.ungetService(ref);
            }
        });

        return result;
    }

    @Override
    public void removeEntry(GwtXSRFToken xsrfToken, GwtKeystoreEntry entry) throws GwtKuraException {

        final Collection<ServiceReference<KeystoreService>> refs = ServiceLocator.getInstance().getServiceReferences(
                KeystoreService.class, "(" + KURA_SERVICE_PID + "=" + entry.getKeystoreName() + ")");

        if (refs.isEmpty()) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
        }

        for (final ServiceReference<KeystoreService> ref : refs) {
            final KeystoreService service = ServiceLocator.getInstance().getService(ref);

            if (service == null) {
                continue;
            }

            try {
                try {
                    service.deleteEntry(entry.getAlias());
                } catch (Exception e) {
                    throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
                }
            } finally {
                ServiceLocator.getInstance().ungetService(ref);
            }
        }
    }
}