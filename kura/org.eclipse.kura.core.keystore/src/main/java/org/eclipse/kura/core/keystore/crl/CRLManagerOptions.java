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
package org.eclipse.kura.core.keystore.crl;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.util.configuration.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CRLManagerOptions {

    private static final Logger logger = LoggerFactory.getLogger(CRLManagerOptions.class);

    private static final Property<Boolean> CRL_MANAGEMENT_ENABLED = new Property<>("crl.management.enabled", false);
    private static final Property<Long> CRL_UPDATE_INTERVAL = new Property<>("crl.update.interval", 1L);
    private static final Property<String> CRL_UPDATE_INTERVAL_TIME_UNIT = new Property<>(
            "crl.update.interval.time.unit", TimeUnit.DAYS.name());
    private static final Property<Long> CRL_CHECK_INTERVAL = new Property<>("crl.check.interval", 5L);
    private static final Property<String> CRL_CHECK_INTERVAL_TIME_UNIT = new Property<>("crl.check.interval.time.unit",
            TimeUnit.MINUTES.name());
    private static final Property<String[]> CRL_URLS = new Property<>("crl.urls", new String[0]);
    private static final Property<String> CRL_STORE_PATH = new Property<>("crl.store.path", String.class);
    private static final Property<Boolean> CRL_VERIFICATION_ENABLED = new Property<>("verify.crl", true);

    private final boolean crlManagementEnabled;
    private final long crlUpdateIntervalMs;
    private final long crlCheckIntervalMs;
    private final Set<URI> crlURIs;
    private final Optional<File> crlStore;
    private final boolean verifyCRL;

    public CRLManagerOptions(final Map<String, Object> properties) {
        this.crlManagementEnabled = CRL_MANAGEMENT_ENABLED.get(properties);
        this.crlUpdateIntervalMs = extractInterval(properties, CRL_UPDATE_INTERVAL, CRL_UPDATE_INTERVAL_TIME_UNIT);
        this.crlCheckIntervalMs = extractInterval(properties, CRL_CHECK_INTERVAL, CRL_CHECK_INTERVAL_TIME_UNIT);
        this.crlURIs = extractCrlURIs(properties);
        this.crlStore = CRL_STORE_PATH.getOptional(properties).filter(s -> !s.trim().isEmpty()).map(File::new);
        this.verifyCRL = CRL_VERIFICATION_ENABLED.get(properties);
    }

    public boolean isCrlManagementEnabled() {
        return this.crlManagementEnabled;
    }

    public long getCrlUpdateIntervalMs() {
        return this.crlUpdateIntervalMs;
    }

    public long getCrlCheckIntervalMs() {
        return crlCheckIntervalMs;
    }

    public Set<URI> getCrlURIs() {
        return this.crlURIs;
    }

    public Optional<File> getStoreFile() {
        return this.crlStore;
    }

    public boolean isCRLVerificationEnabled() {
        return verifyCRL;
    }

    private static long extractInterval(final Map<String, Object> properties, final Property<Long> valueProperty,
            final Property<String> timeUnitProperty) {
        final long updateInterval = valueProperty.get(properties);
        final TimeUnit timeUnit = TimeUnit.valueOf(timeUnitProperty.get(properties));

        return timeUnit.toMillis(updateInterval);
    }

    private static Set<URI> extractCrlURIs(final Map<String, Object> properties) {
        final String[] values = CRL_URLS.get(properties);

        final Set<URI> result = new HashSet<>();

        for (final String value : values) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }

            try {
                result.add(new URI(value));
            } catch (final Exception e) {
                logger.warn("failed to parse URL: {}", value, e);
            }
        }

        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(crlCheckIntervalMs, crlManagementEnabled, crlStore, crlURIs, crlUpdateIntervalMs);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CRLManagerOptions)) {
            return false;
        }
        CRLManagerOptions other = (CRLManagerOptions) obj;
        return crlCheckIntervalMs == other.crlCheckIntervalMs && crlManagementEnabled == other.crlManagementEnabled
                && Objects.equals(crlStore, other.crlStore) && Objects.equals(crlURIs, other.crlURIs)
                && crlUpdateIntervalMs == other.crlUpdateIntervalMs;
    }

}
