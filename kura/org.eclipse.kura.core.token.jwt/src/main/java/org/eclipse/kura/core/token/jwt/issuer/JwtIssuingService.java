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
package org.eclipse.kura.core.token.jwt.issuer;

import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.core.token.jwt.KeystoreTracker;
import org.eclipse.kura.security.keystore.KeystoreChangedEvent;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.eclipse.kura.security.token.TokenIssueRequest;
import org.eclipse.kura.security.token.TokenIssuingService;
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
        name = "org.eclipse.kura.core.token.jwt.issuer.JwtIssuingService", //
        configurationPolicy = ConfigurationPolicy.REQUIRE, //
        property = { //
                "kura.service.pid=org.eclipse.kura.core.token.jwt.issuer.JwtIssuingService", //
                EventConstants.EVENT_TOPIC + "=" + KeystoreChangedEvent.EVENT_TOPIC })
@Designate(ocd = JwtIssuingServiceOCD.class)
public class JwtIssuingService implements TokenIssuingService, ConfigurableComponent, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(JwtIssuingService.class);

    private final KeystoreTracker keystoreTracker = new KeystoreTracker();
    private Optional<JwtIssuingServiceOptions> serviceOptions = Optional.empty();

    private record ServiceState(Optional<Duration> maximumLifetime, Optional<JwtIssuer> issuer) {

        static ServiceState unconfigured() {
            return new ServiceState(Optional.empty(), Optional.empty());
        }

        static ServiceState withoutSigningKey(final Optional<Duration> maximumLifetime) {
            return new ServiceState(maximumLifetime, Optional.empty());
        }
    }

    private final AtomicReference<ServiceState> state = new AtomicReference<>(ServiceState.unconfigured());

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    public synchronized void setKeystoreService(final KeystoreService keystoreService,
            final Map<String, Object> properties) {
        this.keystoreTracker.bind(keystoreService, properties);
        rebuildServiceState();
    }

    public synchronized void unsetKeystoreService(final KeystoreService keystoreService) {
        if (this.keystoreTracker.unbind(keystoreService)) {
            rebuildServiceState();
        }
    }

    @Override
    public synchronized void handleEvent(final Event event) {
        if (this.keystoreTracker.isContentChangedBy(event)) {
            logger.info("Key store changed, reloading JWT key material");
            rebuildServiceState();
        }
    }

    @Activate
    public synchronized void activate(final JwtIssuingServiceOCD config) {
        updated(config);
    }

    @Modified
    public synchronized void updated(final JwtIssuingServiceOCD config) {
        this.serviceOptions = Optional.of(new JwtIssuingServiceOptions(config));
        rebuildServiceState();
    }

    @Deactivate
    public synchronized void deactivate() {
        this.state.set(ServiceState.unconfigured());
    }

    @Override
    public Optional<Duration> getMaximumLifetime() {
        return this.state.get().maximumLifetime;
    }

    @Override
    public String issue(final TokenIssueRequest request) throws KuraException {
        Objects.requireNonNull(request, "request cannot be null");

        final Optional<JwtIssuer> currentIssuer = this.state.get().issuer();

        if (currentIssuer.isEmpty()) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                    "JwtIssuingService misconfigured or not yet ready");
        }

        return currentIssuer.get().issue(request);
    }

    private synchronized void rebuildServiceState() {
        logger.info("Rebuilding JWT issuing service state");

        final Optional<JwtIssuingServiceOptions> options = this.serviceOptions;
        final Optional<KeystoreService> keystore = this.keystoreTracker.get();

        if (options.isEmpty()) {
            this.state.set(ServiceState.unconfigured());
            return;
        }

        if (keystore.isEmpty()) {
            this.state.set(ServiceState.withoutSigningKey(options.get().getMaximumLifetime()));
            return;
        }

        final String alias = options.get().getSigningKeyAlias();
        final Optional<RSAPrivateKey> signingKey = loadRsaPrivateKey(keystore.get(), alias);

        if (signingKey.isEmpty()) {
            logger.warn("Signing key '{}' is not available, token issuing is not available", alias);
            this.state.set(ServiceState.withoutSigningKey(options.get().getMaximumLifetime()));
            return;
        }

        this.state.set(new ServiceState(options.get().getMaximumLifetime(),
                Optional.of(new JwtIssuer(options.get(), signingKey.get()))));

        logger.info("JWT issuing service state rebuilt, using signing key '{}'", alias);
    }

    private static Optional<RSAPrivateKey> loadRsaPrivateKey(final KeystoreService keystoreService,
            final String alias) {
        try {
            final Entry entry = keystoreService.getEntry(alias);

            if (entry == null) {
                return Optional.empty();
            }

            if (!(entry instanceof PrivateKeyEntry privateKeyEntry)) {
                logger.error("Keystore entry '{}' is a {}, a PrivateKeyEntry is required to sign tokens", alias,
                        entry.getClass().getSimpleName());
                return Optional.empty();
            }

            if (!(privateKeyEntry.getPrivateKey() instanceof RSAPrivateKey signingRsaPrivateKey)) {
                logger.error("Signing key '{}' is not a RSAPrivateKey", alias);
                return Optional.empty();
            }

            return Optional.of(signingRsaPrivateKey);
        } catch (Exception e) {
            logger.error("Error while loading signing key '{}'", alias, e);
            return Optional.empty();
        }
    }

}
