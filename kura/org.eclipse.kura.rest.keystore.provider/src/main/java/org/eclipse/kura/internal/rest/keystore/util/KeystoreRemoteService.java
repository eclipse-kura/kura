/*******************************************************************************
 * Copyright (c) 2021, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.keystore.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.security.auth.x500.X500Principal;
import javax.ws.rs.WebApplicationException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.keystore.util.CertificateInfo;
import org.eclipse.kura.core.keystore.util.CsrInfo;
import org.eclipse.kura.core.keystore.util.EntryInfo;
import org.eclipse.kura.core.keystore.util.KeyPairInfo;
import org.eclipse.kura.core.keystore.util.KeystoreUtils;
import org.eclipse.kura.core.keystore.util.PrivateKeyInfo;
import org.eclipse.kura.internal.rest.keystore.request.CsrReadRequest;
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

public class KeystoreRemoteService {

    private static final Logger logger = LoggerFactory.getLogger(KeystoreRemoteService.class);

    public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    public static final String END_CERT = "-----END CERTIFICATE-----";
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    protected Map<String, KeystoreService> keystoreServices = new HashMap<>();
    protected BundleContext bundleContext;
    private ServiceTrackerCustomizer<KeystoreService, KeystoreService> keystoreServiceTrackerCustomizer;
    private ServiceTracker<KeystoreService, KeystoreService> keystoreServiceTracker;

    public void activate(ComponentContext componentContext) {
        this.bundleContext = componentContext.getBundleContext();
        this.keystoreServiceTrackerCustomizer = new KeystoreServiceTrackerCustomizer();
        initKeystoreServiceTracking();
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

    protected List<EntryInfo> getKeysByPidInternal(final String keystoreServicePid) {
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

    protected String getCSRInternal(final CsrInfo info) {
        try {
            X500Principal principal = new X500Principal(info.getAttributes());
            return this.keystoreServices.get(info.getKeystoreServicePid()).getCSR(info.getAlias(), principal,
                    info.getSignatureAlgorithm());
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

    protected void storeTrustedCertificateEntryInternal(final CertificateInfo writeRequest) {
        try {
            this.keystoreServices.get(writeRequest.getKeystoreServicePid()).setEntry(writeRequest.getAlias(),
                    KeystoreUtils.createCertificateEntry(writeRequest.getCertificate()));
        } catch (GeneralSecurityException | KuraException e) {
            throw new WebApplicationException(e);
        }
    }

    protected void storeKeyPairEntryInternal(final KeyPairInfo writeRequest) {
        try {
            this.keystoreServices.get(writeRequest.getKeystoreServicePid()).createKeyPair(writeRequest.getAlias(),
                    writeRequest.getAlgorithm(), writeRequest.getSize(), writeRequest.getSignatureAlgorithm(),
                    writeRequest.getAttributes());
        } catch (KuraException e) {
            throw new WebApplicationException(e);
        }
    }

    protected void storePrivateKeyEntryInternal(final PrivateKeyInfo writeRequest)
            throws KuraException, IOException, GeneralSecurityException {

        final KeystoreService targetKeystore = Optional
                .ofNullable(this.keystoreServices.get(writeRequest.getKeystoreServicePid()))
                .orElseThrow(() -> new KuraException(KuraErrorCode.NOT_FOUND, "KeystoreService not found"));

        if (writeRequest.getPrivateKey() == null) {
            updatePrivateKeyEntryCertificateChain(targetKeystore, writeRequest);
        } else {
            createPrivateKeyEntry(targetKeystore, writeRequest);
        }
    }

    private void updatePrivateKeyEntryCertificateChain(final KeystoreService targetKeystore,
            final PrivateKeyInfo writeRequest) throws KuraException, CertificateException {
        final Entry targetEntry = Optional.ofNullable(targetKeystore.getEntry(writeRequest.getAlias()))
                .orElseThrow(() -> new KuraException(KuraErrorCode.NOT_FOUND, "Entry not found"));

        if (!(targetEntry instanceof PrivateKeyEntry)) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "Target entry is not a PrivateKeyEntry");
        }

        final PrivateKeyEntry existingPrivateKeyEntry = (PrivateKeyEntry) targetEntry;

        final Certificate[] certificateChain = KeystoreUtils.parsePublicCertificates(
                Arrays.stream(writeRequest.getCertificateChain()).collect(Collectors.joining("\n")));

        final PrivateKeyEntry result = new PrivateKeyEntry(existingPrivateKeyEntry.getPrivateKey(), certificateChain);

        targetKeystore.setEntry(writeRequest.getAlias(), result);
    }

    private void createPrivateKeyEntry(final KeystoreService targetKeystore, final PrivateKeyInfo writeRequest)
            throws IOException, GeneralSecurityException, KuraException {
        final PrivateKeyEntry privateKeyEntry = KeystoreUtils.createPrivateKey(writeRequest.getPrivateKey(),
                Arrays.stream(writeRequest.getCertificateChain()).collect(Collectors.joining("\n")));

        targetKeystore.setEntry(writeRequest.getAlias(), privateKeyEntry);
    }

    protected void deleteKeyEntryInternal(String keystoreServicePid, String alias) {
        try {
            this.keystoreServices.get(keystoreServicePid).deleteEntry(alias);
        } catch (KuraException e) {
            throw new WebApplicationException(e);
        }
    }

    private KeystoreInfo buildKeystoreInfo(String keystoreServicePid, KeyStore keystore) throws KeyStoreException {
        KeystoreInfo keystoreInfo = new KeystoreInfo(keystoreServicePid);
        keystoreInfo.setType(keystore.getType());
        keystoreInfo.setSize(keystore.size());
        return keystoreInfo;
    }

    private CertificateInfo buildCertificateInfo(String keystoreServicePid, String alias,
            TrustedCertificateEntry certificate, boolean withCertificate) {
        CertificateInfo certificateInfo = new CertificateInfo(keystoreServicePid, alias);
        if (certificate != null && certificate.getTrustedCertificate() instanceof X509Certificate) {
            X509Certificate x509Certificate = (X509Certificate) certificate.getTrustedCertificate();
            certificateInfo.setSubjectDN(x509Certificate.getSubjectDN().getName());
            certificateInfo.setIssuer(x509Certificate.getIssuerX500Principal().getName());
            certificateInfo.setStartDate(x509Certificate.getNotBefore().getTime());
            certificateInfo.setExpirationDate(x509Certificate.getNotAfter().getTime());
            certificateInfo.setAlgorithm(x509Certificate.getSigAlgName());
            certificateInfo.setSize(getSize(x509Certificate.getPublicKey()));
            try {
                certificateInfo.setSubjectAN(x509Certificate.getSubjectAlternativeNames());
            } catch (CertificateParsingException e) {
                logger.error("Cannot parse certificate subject alternative names", e);
            }
            if (withCertificate) {
                final Base64.Encoder encoder = Base64.getMimeEncoder(64,
                        LINE_SEPARATOR.getBytes(StandardCharsets.UTF_8));
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
        PrivateKeyInfo privateKeyInfo = new PrivateKeyInfo(keystoreServicePid, alias);
        if (privateKey != null) {
            privateKeyInfo.setAlgorithm(privateKey.getPrivateKey().getAlgorithm());
            privateKeyInfo.setSize(getSize(privateKey.getCertificate().getPublicKey()));
            if (withCertificate) {
                final Base64.Encoder encoder = Base64.getMimeEncoder(64,
                        LINE_SEPARATOR.getBytes(StandardCharsets.UTF_8));
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
            KeystoreRemoteService.this.keystoreServices.put(kuraServicePid,
                    KeystoreRemoteService.this.bundleContext.getService(reference));
            return KeystoreRemoteService.this.keystoreServices.get(kuraServicePid);
        }

        @Override
        public void modifiedService(final ServiceReference<KeystoreService> reference, final KeystoreService service) {
            String kuraServicePid = (String) reference.getProperty(KURA_SERVICE_PID);
            KeystoreRemoteService.this.keystoreServices.put(kuraServicePid,
                    KeystoreRemoteService.this.bundleContext.getService(reference));
        }

        @Override
        public void removedService(final ServiceReference<KeystoreService> reference, final KeystoreService service) {
            String kuraServicePid = (String) reference.getProperty(KURA_SERVICE_PID);
            KeystoreRemoteService.this.keystoreServices.remove(kuraServicePid);
        }
    }
}
