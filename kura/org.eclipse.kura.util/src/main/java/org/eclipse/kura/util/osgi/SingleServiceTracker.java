/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.util.osgi;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Consumer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Track the highest ranking service instance.
 * 
 * @param <T>
 *            The service type to track
 */
public class SingleServiceTracker<T> {

    private static final Logger logger = LoggerFactory.getLogger(SingleServiceTracker.class);

    private static final class Entry<T> {

        private int ranking;
        private final ServiceReference<T> reference;
        private final T service;

        public Entry(final int ranking, ServiceReference<T> reference, final T service) {
            this.ranking = ranking;
            this.reference = reference;
            this.service = service;
        }

        public boolean isBetterThan(final Entry<T> other) {
            return this.ranking > other.ranking;
        }
    }

    private final ServiceTrackerCustomizer<T, T> customizer = new ServiceTrackerCustomizer<T, T>() {

        @Override
        public T addingService(final ServiceReference<T> reference) {
            return adding(reference);
        }

        @Override
        public void modifiedService(final ServiceReference<T> reference, T service) {
            modified(reference, service);
        }

        @Override
        public void removedService(final ServiceReference<T> reference, T service) {
            removed(reference, service);
        }
    };

    private final BundleContext context;
    private final Consumer<T> consumer;
    private final ServiceTracker<T, T> tracker;

    private LinkedList<Entry<T>> entries = new LinkedList<>();
    private Entry<T> currentEntry;

    public SingleServiceTracker(final BundleContext context, final Class<T> clazz, final Consumer<T> consumer) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(consumer);

        this.context = context;
        this.consumer = consumer;
        this.tracker = new ServiceTracker<>(context, clazz, this.customizer);
    }

    public SingleServiceTracker(final BundleContext context, final Filter filter, final Consumer<T> consumer) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(filter);
        Objects.requireNonNull(consumer);

        this.context = context;
        this.consumer = consumer;
        this.tracker = new ServiceTracker<>(context, filter, this.customizer);
    }

    public void open() {
        this.tracker.open();
    }

    public void close() {
        this.tracker.close();
    }

    private static int getRanking(ServiceReference<?> reference) {
        Object ranking = reference.getProperty(Constants.SERVICE_RANKING);
        if (ranking instanceof Number) {
            return ((Number) ranking).intValue();
        } else {
            return 0;
        }
    }

    private void insertEntry(final Entry<T> entry) {
        if (this.entries.isEmpty()) {

            this.entries.add(entry);

        } else {

            final ListIterator<Entry<T>> i = this.entries.listIterator();
            while (i.hasNext()) {
                if (entry.isBetterThan(i.next())) {
                    i.previous();
                    break;
                }
            }

            i.add(entry);

        }
    }

    private void updateEntry(final ServiceReference<T> reference) {
        final ListIterator<Entry<T>> i = this.entries.listIterator();

        while (i.hasNext()) {
            final Entry<T> entry = i.next();

            if (!entry.reference.equals(reference)) {
                // not the entry we are looking for
                continue;
            }

            final int ranking = getRanking(reference);
            if (entry.ranking != ranking) {
                i.remove();

                // update ranking
                entry.ranking = ranking;

                // re-insert updated entry
                insertEntry(entry);
            }

            // the reference is unique
            break;
        }
    }

    protected T adding(final ServiceReference<T> reference) {

        final int ranking = getRanking(reference);
        final T service = this.context.getService(reference);

        final Entry<T> entry = new Entry<>(ranking, reference, service);

        insertEntry(entry);

        if (this.currentEntry == null || entry.isBetterThan(this.currentEntry)) {
            setBestEntry(entry);
        }

        return service;
    }

    protected void modified(final ServiceReference<T> reference, final T service) {

        updateEntry(reference);

        final Entry<T> bestEntry = this.entries.peekFirst();
        if (this.currentEntry != bestEntry) {
            // we have a different entry now
            setBestEntry(bestEntry);
        }

    }

    protected void removed(final ServiceReference<T> reference, final T service) {

        if (!this.entries.removeIf(entry -> entry.reference.equals(reference))) {
            return;
        }

        if (this.currentEntry == null || this.currentEntry.service != service) {
            return;
        }

        // set next best entry

        if (this.entries.isEmpty()) {
            setBestEntry(null);
        } else {
            // the next best entry already is the first one in the list
            setBestEntry(this.entries.peekFirst());
        }
    }

    private void setBestEntry(final Entry<T> entry) {

        this.currentEntry = entry;
        notifyService(entry != null ? entry.service : null);

    }

    protected void notifyService(final T service) {

        try {
            this.consumer.accept(service);
        } catch (final Exception e) {
            logger.warn("Failed to notify changed service", e);
        }

    }

    public T getService() {

        final Entry<T> entry = this.currentEntry;
        return entry != null ? entry.service : null;

    }

}
