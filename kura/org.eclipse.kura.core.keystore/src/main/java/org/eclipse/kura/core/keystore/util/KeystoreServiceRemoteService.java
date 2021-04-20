/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.keystore.util;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;
import javax.ws.rs.WebApplicationException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.keystore.rest.provider.CsrReadRequest;
import org.eclipse.kura.core.keystore.rest.provider.PrivateKeyWriteRequest;
import org.eclipse.kura.core.keystore.rest.provider.TrustedCertificateWriteRequest;
import org.eclipse.kura.security.keystore.KeystoreInfo;
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

public class KeystoreServiceRemoteService {

    private static final Logger logger = LoggerFactory.getLogger(KeystoreServiceRemoteService.class);

    private static final String INVALID_ENTRY_TYPE = "Invalid entry type";
    public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    public static final String END_CERT = "-----END CERTIFICATE-----";
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    protected Map<String, KeystoreService> keystoreServices = new HashMap<>();
    protected BundleContext bundleContext;
    private ServiceTrackerCustomizer<KeystoreService, KeystoreService> keystoreServiceTrackerCustomizer;
    private ServiceTracker<KeystoreService, KeystoreService> keystoreServiceTracker;
    protected CertificateFactory certFactory;
    private DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;

    public void activate(ComponentContext componentContext) {
        this.bundleContext = componentContext.getBundleContext();
        this.keystoreServiceTrackerCustomizer = new KeystoreServiceTrackerCustomizer();
        initKeystoreServiceTracking();
        try {
            this.certFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            logger.error("Failed to get the certificate factory", e);
        }
    }

    public void deactivate(ComponentContext componentContext) {
        if (this.keystoreServiceTracker != null) {
            this.keystoreServiceTracker.close();
        }
    }

    protected List<KeystoreInfo> listKeystoresInternal() {
        List<KeystoreInfo> keystores = new ArrayList<>();
        this.keystoreServices.entrySet().stream().forEach(entry -> {
            try {
                if (entry.getValue().getKeyStore() != null) {
                    keystores.add(buildKeystoreInfo(entry.getKey(), entry.getValue().getKeyStore()));
                }
            } catch (KuraException | KeyStoreException e) {
                throw new WebApplicationException(e);
            }
        });
        return keystores;
    }

    protected List<EntryInfo> getKeysInternal() {
        List<EntryInfo> keys = new ArrayList<>();
        this.keystoreServices.entrySet().stream().forEach(keystoreService -> {
            if (keystoreService != null) {
                try {
                    keystoreService.getValue().getEntries().entrySet().stream().forEach(entry -> {
                        if (entry.getValue() instanceof PrivateKeyEntry) {
                            keys.add(buildPrivateKeyInfo(keystoreService.getKey(), entry.getKey(),
                                    (PrivateKeyEntry) entry.getValue(), false));
                        } else if (entry.getValue() instanceof TrustedCertificateEntry) {
                            keys.add(buildCertificateInfo(keystoreService.getKey(), entry.getKey(),
                                    (TrustedCertificateEntry) entry.getValue(), false));
                        }
                    });
                } catch (KuraException e) {
                    throw new WebApplicationException(e);
                }
            }
        });
        return keys;
    }

    protected List<EntryInfo> getKeysInternal(final String keystoreServicePid) {
        List<EntryInfo> keys = new ArrayList<>();
        KeystoreService keystoreService = this.keystoreServices.get(keystoreServicePid);
        if (keystoreService != null) {
            try {
                keystoreService.getEntries().entrySet().stream().forEach(entry -> {
                    if (entry.getValue() instanceof PrivateKeyEntry) {
                        keys.add(buildPrivateKeyInfo(keystoreServicePid, entry.getKey(),
                                (PrivateKeyEntry) entry.getValue(), true));
                    } else if (entry.getValue() instanceof TrustedCertificateEntry) {
                        keys.add(buildCertificateInfo(keystoreServicePid, entry.getKey(),
                                (TrustedCertificateEntry) entry.getValue(), true));
                    }
                });
            } catch (KuraException e) {
                throw new WebApplicationException(e);
            }
        } else {
            throw new WebApplicationException(404);
        }
        return keys;
    }

    protected List<EntryInfo> getKeysByAliasInternal(final String alias) {
        List<EntryInfo> keys = new ArrayList<>();
        this.keystoreServices.entrySet().stream().filter(entry -> {
            try {
                return entry.getValue().getAliases().contains(alias);
            } catch (KuraException e) {
                throw new WebApplicationException(e);
            }
        }).forEach(entry -> {
            try {
                Entry keystoreEntry = entry.getValue().getEntry(alias);
                if (keystoreEntry instanceof PrivateKeyEntry) {
                    keys.add(buildPrivateKeyInfo(entry.getKey(), alias, (PrivateKeyEntry) keystoreEntry, true));
                } else if (keystoreEntry instanceof TrustedCertificateEntry) {
                    keys.add(
                            buildCertificateInfo(entry.getKey(), alias, (TrustedCertificateEntry) keystoreEntry, true));
                } else {
                    throw new WebApplicationException(404);
                }

            } catch (KuraException e) {
                throw new WebApplicationException(e);
            }
        });

        return keys;
    }

    protected EntryInfo getKeyInternal(final String keystoreServicePid, final String alias) {
        Entry entry;
        KeystoreService keystoreService = this.keystoreServices.get(keystoreServicePid);
        if (keystoreService != null) {
            try {
                entry = keystoreService.getEntry(alias);
                if (entry instanceof PrivateKeyEntry) {
                    return buildPrivateKeyInfo(keystoreServicePid, alias, (PrivateKeyEntry) entry, true);
                } else if (entry instanceof TrustedCertificateEntry) {
                    return buildCertificateInfo(keystoreServicePid, alias, (TrustedCertificateEntry) entry, true);
                } else {
                    throw new WebApplicationException(404);
                }

            } catch (KuraException e) {
                throw new WebApplicationException(e);
            }
        } else {
            throw new WebApplicationException(404);
        }
    }

    protected String getCSRInternal(final EntryInfo request) {
        try {
            if (request.getType() == EntryType.CSR) {
                CsrInfo info = (CsrInfo) request;

                X500Principal principal = new X500Principal(info.getAttributes());
                return this.keystoreServices.get(info.getKeystoreName()).getCSR(info.getAlias(), principal,
                        info.getSignatureAlgorithm());

            } else {
                throw new WebApplicationException(INVALID_ENTRY_TYPE);
            }
        } catch (KuraException e) {
            throw new WebApplicationException(e);
        }
    }

    protected String getCSRInternal(final CsrReadRequest request) {
        try {
            X500Principal principal = new X500Principal(request.getAttributes());
            return this.keystoreServices.get(request.getKeystoreServicePid()).getCSR(request.getAlias(), principal,
                    request.getSignatureAlgorithm());
        } catch (KuraException e) {
            throw new WebApplicationException(e);
        }
    }

    protected String storeKeyEntryInternal(final EntryInfo request) {
        try {
            if (request.getType() == EntryType.TRUSTED_CERTIFICATE) {
                CertificateInfo info = (CertificateInfo) request;
                storeCertificateInternal(info.getKeystoreName(), info.getAlias(), info.getCertificate());
            } else if (request.getType() == EntryType.KEY_PAIR) {
                KeyPairInfo info = (KeyPairInfo) request;
                storeKeyPairInternal(info.getKeystoreName(), info.getAlias(), info.getAlgorithm(), info.getSize(),
                        info.getSignatureAlgorithm(), info.getAttributes());
            } else {
                throw new WebApplicationException(INVALID_ENTRY_TYPE);
            }
        } catch (GeneralSecurityException | KuraException e) {
            throw new WebApplicationException(e);
        }
        return request.getKeystoreName() + ":" + request.getAlias();
    }

    protected String storeTrustedCertificateEntryInternal(final TrustedCertificateWriteRequest writeRequest) {
        try {
            storeCertificateInternal(writeRequest.getKeystoreServicePid(), writeRequest.getAlias(),
                    writeRequest.getCertificate());
        } catch (GeneralSecurityException | KuraException e) {
            throw new WebApplicationException(e);
        }
        return writeRequest.getKeystoreServicePid() + ":" + writeRequest.getAlias();
    }

    protected String storeKeyPairEntryInternal(final PrivateKeyWriteRequest writeRequest) {
        try {
            storeKeyPairInternal(writeRequest.getKeystoreServicePid(), writeRequest.getAlias(),
                    writeRequest.getAlgorithm(), writeRequest.getSize(), writeRequest.getSignatureAlgorithm(),
                    writeRequest.getAttributes());
        } catch (KuraException e) {
            throw new WebApplicationException(e);
        }
        return writeRequest.getKeystoreServicePid() + ":" + writeRequest.getAlias();
    }

    protected void deleteKeyEntryInternal(String keystoreServicePid, String alias) {
        try {
            this.keystoreServices.get(keystoreServicePid).deleteEntry(alias);
        } catch (KuraException e) {
            throw new WebApplicationException(e);
        }
    }

    private void storeCertificateInternal(String keystoreServicePid, String alias, String certificate)
            throws KuraException, CertificateException {
        ByteArrayInputStream is = new ByteArrayInputStream(certificate.getBytes());
        X509Certificate cert = (X509Certificate) this.certFactory.generateCertificate(is);
        this.keystoreServices.get(keystoreServicePid).setEntry(alias, new TrustedCertificateEntry(cert));
    }

    private void storeKeyPairInternal(String keystoreServicePid, String alias, String algorithm, int size,
            String signatureAlgorithm, String attributes) throws KuraException {
        this.keystoreServices.get(keystoreServicePid).createKeyPair(alias, algorithm, size, signatureAlgorithm,
                attributes);
    }

    private KeystoreInfo buildKeystoreInfo(String keystoreServicePid, KeyStore keystore) throws KeyStoreException {
        KeystoreInfo keystoreInfo = new KeystoreInfo(keystoreServicePid);
        keystoreInfo.setType(keystore.getType());
        keystoreInfo.setSize(keystore.size());
        return keystoreInfo;
    }

    private CertificateInfo buildCertificateInfo(String keystoreServicePid, String alias,
            TrustedCertificateEntry certificate, boolean withCertificate) {
        CertificateInfo certificateInfo = new CertificateInfo(alias, keystoreServicePid);
        if (certificate != null && certificate.getTrustedCertificate() instanceof X509Certificate) {
            X509Certificate x509Certificate = (X509Certificate) certificate.getTrustedCertificate();
            certificateInfo.setSubjectDN(x509Certificate.getSubjectDN().getName());
            certificateInfo.setIssuer(x509Certificate.getIssuerX500Principal().getName());
            ZonedDateTime startDate = Instant.ofEpochMilli(x509Certificate.getNotBefore().getTime())
                    .atZone(ZoneOffset.UTC);
            certificateInfo.setStartDate(formatter.format(startDate));
            ZonedDateTime expirationDate = Instant.ofEpochMilli(x509Certificate.getNotAfter().getTime())
                    .atZone(ZoneOffset.UTC);
            certificateInfo.setExpirationDate(formatter.format(expirationDate));
            certificateInfo.setAlgorithm(x509Certificate.getSigAlgName());
            certificateInfo.setSize(getSize(x509Certificate.getPublicKey()));
            try {
                certificateInfo.setSubjectAN(x509Certificate.getSubjectAlternativeNames());
            } catch (CertificateParsingException e) {
                logger.error("Cannot parse certificate subject alternative names", e);
            }
            if (withCertificate) {
                final Base64.Encoder encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());
                StringBuilder pemCertificate = new StringBuilder();
                pemCertificate.append(BEGIN_CERT);
                pemCertificate.append(LINE_SEPARATOR);
                try {
                    pemCertificate.append(encoder.encodeToString(x509Certificate.getEncoded()));
                } catch (CertificateEncodingException e) {
                    logger.error("Cannot encode certificate", e);
                }
                pemCertificate.append(LINE_SEPARATOR);
                pemCertificate.append(END_CERT);
                certificateInfo.setCertificate(pemCertificate.toString());
            }
        }
        return certificateInfo;
    }

    private PrivateKeyInfo buildPrivateKeyInfo(String keystoreServicePid, String alias, PrivateKeyEntry privateKey,
            boolean withCertificate) {
        PrivateKeyInfo privateKeyInfo = new PrivateKeyInfo(alias, keystoreServicePid);
        if (privateKey != null) {
            privateKeyInfo.setAlgorithm(privateKey.getPrivateKey().getAlgorithm());
            privateKeyInfo.setSize(getSize(privateKey.getCertificate().getPublicKey()));
            if (withCertificate) {
                final Base64.Encoder encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());
                String[] certificateChain = new String[privateKey.getCertificateChain().length];
                for (int i = 0; i < certificateChain.length; i++) {
                    StringBuilder pemCertificate = new StringBuilder();
                    pemCertificate.append(BEGIN_CERT);
                    pemCertificate.append(LINE_SEPARATOR);
                    try {
                        pemCertificate.append(encoder.encodeToString(privateKey.getCertificateChain()[i].getEncoded()));
                    } catch (CertificateEncodingException e) {
                        logger.error("Cannot encode certificate", e);
                    }
                    pemCertificate.append(LINE_SEPARATOR);
                    pemCertificate.append(END_CERT);
                    certificateChain[i] = pemCertificate.toString();
                }
                privateKeyInfo.setCertificateChain(certificateChain);
            }

        }
        return privateKeyInfo;
    }

    private int getSize(Key key) {
        int size = 0;
        if (key instanceof RSAPublicKey) {
            size = ((RSAPublicKey) key).getModulus().bitLength();
        } else if (key instanceof ECPublicKey) {
            ECParameterSpec spec = ((ECPublicKey) key).getParams();
            if (spec != null) {
                size = spec.getOrder().bitLength();
            }
        } else if (key instanceof DSAPublicKey) {
            DSAPublicKey dsaCertificate = (DSAPublicKey) key;
            if (dsaCertificate.getParams() != null) {
                size = dsaCertificate.getParams().getP().bitLength();
            } else {
                size = dsaCertificate.getY().bitLength();
            }
        }
        return size;
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

    private final class KeystoreServiceTrackerCustomizer
            implements ServiceTrackerCustomizer<KeystoreService, KeystoreService> {

        private static final String KURA_SERVICE_PID = "kura.service.pid";

        @Override
        public KeystoreService addingService(final ServiceReference<KeystoreService> reference) {
            String kuraServicePid = (String) reference.getProperty(KURA_SERVICE_PID);
            KeystoreServiceRemoteService.this.keystoreServices.put(kuraServicePid,
                    KeystoreServiceRemoteService.this.bundleContext.getService(reference));
            return KeystoreServiceRemoteService.this.keystoreServices.get(kuraServicePid);
        }

        @Override
        public void modifiedService(final ServiceReference<KeystoreService> reference, final KeystoreService service) {
            String kuraServicePid = (String) reference.getProperty(KURA_SERVICE_PID);
            KeystoreServiceRemoteService.this.keystoreServices.put(kuraServicePid,
                    KeystoreServiceRemoteService.this.bundleContext.getService(reference));
        }

        @Override
        public void removedService(final ServiceReference<KeystoreService> reference, final KeystoreService service) {
            String kuraServicePid = (String) reference.getProperty(KURA_SERVICE_PID);
            KeystoreServiceRemoteService.this.keystoreServices.remove(kuraServicePid);
        }
    }
}
