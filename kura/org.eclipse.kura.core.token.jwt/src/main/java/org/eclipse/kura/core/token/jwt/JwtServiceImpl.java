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
package org.eclipse.kura.core.token.jwt;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.security.keystore.KeystoreChangedEvent;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.eclipse.kura.security.token.jwt.JwtIssueRequest;
import org.eclipse.kura.security.token.jwt.JwtService;
import org.eclipse.kura.security.token.jwt.JwtVerificationProof;
import org.eclipse.kura.security.token.jwt.JwtVerifyRequest;
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
        name = "org.eclipse.kura.core.token.jwt.JwtServiceImpl", //
        configurationPolicy = ConfigurationPolicy.REQUIRE, //
        property = { //
                "kura.service.pid=org.eclipse.kura.core.token.jwt.JwtServiceImpl", //
                EventConstants.EVENT_TOPIC + "=" + KeystoreChangedEvent.EVENT_TOPIC })
@Designate(ocd = JwtServiceOCD.class)
public class JwtServiceImpl implements JwtService, ConfigurableComponent, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(JwtServiceImpl.class);

    private Optional<KeystoreService> keystoreService = Optional.empty();
    private Optional<String> keystoreServicePid = Optional.empty();
    private JwtServiceOptions serviceOptions;
    private volatile Optional<JwtKeyMaterial> jwtKeyMaterial = Optional.empty();
    private volatile JwtVerifier jwtVerifier;
    private volatile JwtIssuer jwtIssuer;

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    public synchronized void setKeystoreService(final KeystoreService keystoreService,
            final Map<String, Object> properties) {
        this.keystoreService = Optional.of(keystoreService);
        this.keystoreServicePid = Optional
                .ofNullable((String) properties.get(ConfigurationService.KURA_SERVICE_PID));

        refreshKeyMaterial();
    }

    public synchronized void unsetKeystoreService(final KeystoreService keystoreService) {
        if (this.keystoreService.equals(Optional.of(keystoreService))) {
            this.keystoreService = Optional.empty();
            this.keystoreServicePid = Optional.empty();

            refreshKeyMaterial();
        }
    }

    @Activate
    public synchronized void activate(final JwtServiceOCD config) {
        logger.info("JwtService - Activating");
        updated(config);
        logger.info("JwtService - Activated");
    }

    @Modified
    public synchronized void updated(final JwtServiceOCD config) {
        logger.info("JwtService - Updating");

        this.serviceOptions = new JwtServiceOptions(config);

        refreshKeyMaterial();

        this.jwtVerifier = new JwtVerifier(this.serviceOptions.getAcceptedIssuers(),
                this.serviceOptions.getClockSkewSeconds(), this.serviceOptions.isRequireValidCertificate());

        this.jwtIssuer = new JwtIssuer(this.serviceOptions.getIssuer(), this.serviceOptions.getMaximumLifetime());

        logger.info("JwtService - Updated");
    }

    @Deactivate
    public synchronized void deactivate() {
        this.jwtKeyMaterial = Optional.empty();
        this.keystoreService = Optional.empty();
        this.keystoreServicePid = Optional.empty();
        logger.info("JwtService - Deactivated");
    }

    @Override
    public synchronized void handleEvent(final Event event) {
        if (!(event instanceof KeystoreChangedEvent keystoreChangedEvent)) {
            return;
        }

        final Optional<String> eventPid = Optional.ofNullable(keystoreChangedEvent.getSenderPid());

        if (eventPid.isPresent() && this.keystoreServicePid.equals(eventPid)) {
            logger.info("JwtService - Keystore changed, reloading JWT key material");
            refreshKeyMaterial();
        }
    }

    @Override
    public Optional<Duration> getMaximumLifetime() {
        return this.serviceOptions.getMaximumLifetime();
    }

    @Override
    public String issue(final JwtIssueRequest request) throws KuraException {
        Objects.requireNonNull(request, "request cannot be null");

        Optional<JwtKeyMaterial> keyMaterial = this.jwtKeyMaterial;
        return this.jwtIssuer.issueToken(request, getOrThrowConfigException(keyMaterial));
    }

    @Override
    public JwtVerificationProof verify(final JwtVerifyRequest request) throws KuraException {
        Objects.requireNonNull(request, "request cannot be null");

        Optional<JwtKeyMaterial> keyMaterial = this.jwtKeyMaterial;
        return this.jwtVerifier.verify(request, getOrThrowConfigException(keyMaterial));
    }

    private void refreshKeyMaterial() {
        Optional<KeystoreService> keystore = this.keystoreService;
        
        if (keystore.isEmpty() || this.serviceOptions == null) {
            this.jwtKeyMaterial = Optional.empty();
            return;
        }

        try {
            this.jwtKeyMaterial = Optional.of(new JwtKeyMaterial(this.serviceOptions.getSigningKeyAlias(),
                    this.serviceOptions.getVerificationKeyAliases(), keystore.get()));
        } catch (final Exception e) {
            this.jwtKeyMaterial = Optional.empty();
            logger.warn("JwtService - Failed to load JWT key material, token issuing and verification are disabled",
                    e);
        }
    }

    private <T> T getOrThrowConfigException(Optional<T> opt) throws KuraException {
        if (opt.isEmpty()) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                    "JwtService misconfigured or incorrect key material");
        }

        return opt.get();
    }

}
