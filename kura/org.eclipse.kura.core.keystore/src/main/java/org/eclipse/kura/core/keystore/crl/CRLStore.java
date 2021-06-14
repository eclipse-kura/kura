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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.eclipse.kura.core.keystore.util.MappingCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;

public class CRLStore implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(CRLStore.class);

    private final File storeFile;
    private final Map<Set<URI>, StoredCRL> crls = new HashMap<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final long storeDelayMs;
    private Optional<ScheduledFuture<?>> storeTask = Optional.empty();

    public CRLStore(final File storeFile, final long storeDelayMs) {
        this.storeFile = storeFile;
        this.storeDelayMs = storeDelayMs;

        if (storeFile.exists()) {
            load();
        } else {
            final File parent = storeFile.getParentFile();

            if (!parent.isDirectory() && !parent.mkdirs()) {
                logger.warn("failed to create directory: {}", parent);
            }
        }
    }

    public synchronized void storeCRL(final StoredCRL crl) {
        this.crls.put(crl.getDistributionPoints(), crl);
        requestStore();
    }

    public synchronized boolean removeCRLs(final Predicate<StoredCRL> predicate) {

        boolean changed = false;

        final Iterator<StoredCRL> iter = this.crls.values().iterator();

        while (iter.hasNext()) {
            final StoredCRL next = iter.next();

            if (predicate.test(next)) {
                logger.info("removing CRL: {}", next.getCrl().getIssuerX500Principal());
                changed = true;
                iter.remove();
            }
        }

        if (changed) {
            requestStore();
        }

        return changed;
    }

    public Optional<StoredCRL> getCrl(final Set<URI> distributionPoints) {
        return Optional.ofNullable(this.crls.get(distributionPoints));
    }

    public synchronized List<StoredCRL> getCRLs() {
        return new ArrayList<>(crls.values());
    }

    public CertStore getCertStore() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        return CertStore.getInstance("Collection",
                new CollectionCertStoreParameters(new MappingCollection<>(crls.values(), StoredCRL::getCrl)));
    }

    private void requestStore() {
        final Optional<ScheduledFuture<?>> currentTask = this.storeTask;

        if (currentTask.isPresent()) {
            currentTask.get().cancel(false);
        }

        this.storeTask = Optional.of(this.executor.schedule(this::store, storeDelayMs, TimeUnit.MILLISECONDS));
    }

    private synchronized void load() {
        try {
            try (final Reader in = new InputStreamReader(new FileInputStream(this.storeFile))) {

                final JsonArray array = Json.parse(in).asArray();

                for (final JsonValue value : array) {
                    final StoredCRL crl = StoredCRL.fromJson(value.asObject());

                    logger.info("loaded CRL for {}", crl.getCrl().getIssuerX500Principal());
                    this.crls.put(crl.getDistributionPoints(), crl);
                }
            }
        } catch (final Exception e) {
            logger.warn("failed to load CRLs", e);
        }
    }

    private void store() {
        try {
            logger.info("storing CRLs...");

            final List<StoredCRL> currentCrls = getCRLs();

            final JsonArray array = new JsonArray();

            for (final StoredCRL crl : currentCrls) {
                array.add(crl.toJson());
            }

            final File tmpFile = new File(this.storeFile.getParent(), this.storeFile.getName() + "_");

            try (final Writer out = new FileWriter(tmpFile)) {
                array.writeTo(out);
            }

            Files.move(tmpFile.toPath(), this.storeFile.toPath(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

            logger.info("storing CRLs...done");
        } catch (final Exception e) {
            logger.warn("failed to store CRLs", e);
        }
    }

    @Override
    public void close() throws IOException {
        executor.shutdown();
    }

}
