/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.token.jwt.verifier;

import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.security.keystore.KeystoreChangedEvent;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.eclipse.kura.security.token.TokenVerificationService;
import org.eclipse.kura.security.token.TokenVerifyRequest;
import org.eclipse.kura.security.token.VerificationProof;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, //
        name = "org.eclipse.kura.core.token.jwt.verifier.JwtVerificationService", //
        configurationPolicy = ConfigurationPolicy.REQUIRE, //
        property = { //
                "kura.service.pid=org.eclipse.kura.core.token.jwt.verifier.JwtVerificationService", //
                EventConstants.EVENT_TOPIC + "=" + KeystoreChangedEvent.EVENT_TOPIC })
@Designate(ocd = JwtVerificationServiceOCD.class)
public class JwtVerificationService implements TokenVerificationService, ConfigurableComponent, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(JwtVerificationService.class);

    private Optional<KeystoreService> keystoreService = Optional.empty();
    private Optional<String> keystoreServicePid = Optional.empty();
    private Optional<JwtVerificationServiceOptions> serviceOptions = Optional.empty();

    private volatile Optional<JwtVerifier> verifier = Optional.empty();

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    public synchronized void setKeystoreService(final KeystoreService keystoreService,
            final Map<String, Object> properties) {
        this.keystoreService = Optional.of(keystoreService);
        this.keystoreServicePid = Optional.ofNullable((String) properties.get(ConfigurationService.KURA_SERVICE_PID));
        rebuildVerifier();
    }

    public synchronized void unsetKeystoreService(final KeystoreService keystoreService) {
        if (this.keystoreService.equals(Optional.of(keystoreService))) {
            this.keystoreService = Optional.empty();
            this.keystoreServicePid = Optional.empty();
            rebuildVerifier();
        }
    }

    @Override
    public synchronized void handleEvent(final Event event) {
        if (!(event instanceof KeystoreChangedEvent keystoreChangedEvent)) {
            return;
        }

        final Optional<String> eventPid = Optional.ofNullable(keystoreChangedEvent.getSenderPid());

        if (eventPid.isPresent() && this.keystoreServicePid.equals(eventPid)) {
            logger.info("Keystore changed, reloading JWT key material");
            rebuildVerifier();
        }
    }

    @Activate
    public synchronized void activate(final JwtVerificationServiceOCD config) {
        updated(config);
    }

    @Modified
    public synchronized void updated(final JwtVerificationServiceOCD config) {
        this.serviceOptions = Optional.of(new JwtVerificationServiceOptions(config));
        rebuildVerifier();
    }

    @Deactivate
    public synchronized void deactivate() {
        this.verifier = Optional.empty();
    }

    @Override
    public VerificationProof verify(final TokenVerifyRequest request) throws KuraException {
        Objects.requireNonNull(request, "Request cannot be null");

        final Optional<JwtVerifier> currentVerifier = this.verifier;

        if (currentVerifier.isEmpty()) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                    "JwtVerificationService misconfigured or not yet ready");
        }

        return currentVerifier.get().verify(request.getToken(), request.getIntendedConsumer());
    }

    private synchronized void rebuildVerifier() {
        logger.info("Rebuilding JWT verification service state");

        final Optional<JwtVerificationServiceOptions> options = this.serviceOptions;
        final Optional<KeystoreService> keystore = this.keystoreService;

        if (options.isEmpty() || keystore.isEmpty()) {
            this.verifier = Optional.empty();
            return;
        }

        final Map<String, X509Certificate> certificates = loadVerificationCertificates(keystore.get(),
                options.get().getVerificationKeyAliases());

        if (certificates.isEmpty()) {
            logger.warn("No usable certificate found in the configured key store, token verification is not available");
            this.verifier = Optional.empty();
            return;
        }

        this.verifier = Optional.of(new JwtVerifier(options.get(), certificates));

        logger.info("JWT verification service state rebuilt, {} trusted certificate(s) loaded", certificates.size());
    }

    private static Map<String, X509Certificate> loadVerificationCertificates(final KeystoreService keystoreService,
            final Set<String> aliases) {

        final Map<String, X509Certificate> result = new LinkedHashMap<>();

        try {
            for (final Map.Entry<String, Entry> storeEntry : keystoreService.getEntries().entrySet()) {
                final String alias = storeEntry.getKey();

                if (!aliases.isEmpty() && !aliases.contains(alias)) {
                    continue;
                }

                asX509Certificate(storeEntry.getValue()) //
                        .filter(JwtVerificationService::isX509CertificateUsable) //
                        .ifPresent(certificate -> result.put(alias, certificate));
            }
        } catch (Exception e) {
            logger.error("Error retrieving certificates for aliases: {}", aliases, e);
        }

        return result;
    }

    private static boolean isX509CertificateUsable(final X509Certificate certificate) {
        return certificate.getPublicKey() instanceof RSAPublicKey;
    }

    private static Optional<X509Certificate> asX509Certificate(final Entry entry) {
        if (entry instanceof TrustedCertificateEntry trustedCertificateEntry) {
            return asX509Certificate(trustedCertificateEntry.getTrustedCertificate());
        }

        if (entry instanceof PrivateKeyEntry privateKeyEntry) {
            return asX509Certificate(privateKeyEntry.getCertificate());
        }

        return Optional.empty();
    }

    private static Optional<X509Certificate> asX509Certificate(final Certificate certificate) {
        return certificate instanceof X509Certificate x509Certificate ? Optional.of(x509Certificate) : Optional.empty();
    }

}
