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

import java.io.Closeable;
import java.io.File;
import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertStore;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.kura.core.keystore.util.CRLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CRLManager implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(CRLManager.class);

    private final CRLStore store;
    private final ScheduledExecutorService updateExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService downloadExecutor = Executors.newCachedThreadPool();

    private final List<DistributionPointState> referencedDistributionPoints = new ArrayList<>();
    private final long forceUpdateIntervalNanos;
    private final long periodicReckeckIntervalMs;
    private final CRLVerifier verifier;

    private Optional<ScheduledFuture<?>> updateTask = Optional.empty();
    private Optional<Listener> listener;

    public CRLManager(final File storeFile, final long storeDelayMs, final long periodicRecheckIntervalMs,
            final long forceUpdateIntervalMs, final CRLVerifier verifier) {
        this.store = new CRLStore(storeFile, storeDelayMs);
        this.periodicReckeckIntervalMs = periodicRecheckIntervalMs;
        this.forceUpdateIntervalNanos = Duration.ofMillis(forceUpdateIntervalMs).toNanos();
        this.verifier = verifier;
        requestUpdate();
    }

    public void setListener(final Optional<Listener> listener) {
        this.listener = listener;
    }

    public synchronized boolean addDistributionPoint(final Set<URI> uris) {
        logger.info("referencing distribution points: {}", uris);

        final Optional<DistributionPointState> existing = this.referencedDistributionPoints.stream()
                .filter(p -> p.distributionPoints.equals(uris)).findAny();

        if (existing.isPresent()) {
            existing.get().ref();
            return false;
        } else {
            this.referencedDistributionPoints.add(new DistributionPointState(uris));
            requestUpdate();
            return true;
        }
    }

    public synchronized boolean removeDistributionPoint(final Set<URI> uris) {
        logger.info("unreferencing distribution points: {}", uris);

        if (this.referencedDistributionPoints.removeIf(p -> p.distributionPoints.equals(uris) && p.unref() <= 0)) {
            requestUpdate();
            return true;
        } else {
            return false;
        }
    }

    public boolean addTrustedCertificate(final X509Certificate entry) {

        final Set<URI> distributionPoints;

        try {
            distributionPoints = CRLUtil.getCrlURIs(entry);
        } catch (final Exception e) {
            logger.warn("failed to get distribution points for {}", entry.getSubjectX500Principal(), e);
            return false;
        }

        if (distributionPoints.isEmpty()) {
            logger.info("certificate {} has no CRL distribution points", entry.getSubjectX500Principal());
            return false;
        }

        return addDistributionPoint(distributionPoints);
    }

    public boolean removeTrustedCertificate(final X509Certificate entry) {

        try {
            final Set<URI> distributionPoints = CRLUtil.getCrlURIs(entry);

            return removeDistributionPoint(distributionPoints);

        } catch (final Exception e) {
            logger.warn("failed to get distribution points for {}", entry.getSubjectX500Principal(), e);
            return false;
        }
    }

    public synchronized List<X509CRL> getCrls() {
        return store.getCRLs().stream().map(StoredCRL::getCrl).collect(Collectors.toList());
    }

    public CertStore getCertStore() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        return store.getCertStore();
    }

    public CRLStore getCRLStore() {
        return store;
    }

    @Override
    public void close() {
        listener = Optional.empty();
        updateExecutor.shutdown();
        downloadExecutor.shutdown();
    }

    private void requestUpdate() {
        if (this.updateTask.isPresent()) {
            this.updateTask.get().cancel(false);
        }

        this.updateTask = Optional.of(this.updateExecutor.scheduleWithFixedDelay(this::update, 5000,
                this.periodicReckeckIntervalMs, TimeUnit.MILLISECONDS));
    }

    private synchronized void update() {

        boolean changed = false;
        final long now = System.nanoTime();

        for (final DistributionPointState state : referencedDistributionPoints) {

            final Optional<StoredCRL> storedCrl = store.getCRLs().stream()
                    .filter(c -> c.getDistributionPoints().equals(state.distributionPoints)).findAny();

            if (!storedCrl.isPresent() || storedCrl.get().isExpired() || !state.lastDownloadInstantNanos.isPresent()
                    || now - state.lastDownloadInstantNanos.getAsLong() > this.forceUpdateIntervalNanos) {
                final CompletableFuture<X509CRL> future = CRLUtil.fetchCRL(state.distributionPoints, downloadExecutor);

                try {
                    final X509CRL crl = future.get(1, TimeUnit.MINUTES);

                    changed |= validateAndStoreCRL(now, state, storedCrl, crl);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("failed to download CRL", e);
                    future.cancel(true);
                } catch (final Exception e) {
                    logger.warn("failed to download CRL", e);
                    future.cancel(true);
                }
            }
        }

        changed |= this.store.removeCRLs(c -> this.referencedDistributionPoints.stream()
                .noneMatch(p -> p.distributionPoints.equals(c.getDistributionPoints())));

        if (changed) {
            listener.ifPresent(Listener::onCRLCacheChanged);
        }
    }

    private boolean validateAndStoreCRL(final long now, final DistributionPointState state,
            final Optional<StoredCRL> storedCrl, final X509CRL newCrl) {

        if (storedCrl.isPresent()) {
            final X509CRL stored = storedCrl.get().getCrl();

            if (stored.equals(newCrl)) {
                logger.info("current CRL is up to date");
                state.lastDownloadInstantNanos = OptionalLong.of(now);
                return false;
            }

            if (!stored.getIssuerX500Principal().equals(newCrl.getIssuerX500Principal())) {
                logger.warn("CRL issuer differs, not updating CRL");
                return false;
            }
        }

        if (verifier.verifyCRL(newCrl)) {
            store.storeCRL(new StoredCRL(state.distributionPoints, newCrl));
            state.lastDownloadInstantNanos = OptionalLong.of(now);
            return true;
        } else {
            logger.warn("CRL verification failed");
            return false;
        }
    }

    private class DistributionPointState {

        private int refCnt = 1;
        private OptionalLong lastDownloadInstantNanos = OptionalLong.empty();
        private final Set<URI> distributionPoints;

        public DistributionPointState(Set<URI> distributionPoints) {
            this.distributionPoints = distributionPoints;
        }

        int ref() {
            this.refCnt++;
            return this.refCnt;
        }

        int unref() {
            this.refCnt--;
            return this.refCnt;
        }
    }

    public interface CRLVerifier {

        public boolean verifyCRL(final X509CRL crl);
    }

    public interface Listener {

        public void onCRLCacheChanged();
    }
}
