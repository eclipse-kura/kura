/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 * 
 *******************************************************************************/
package org.eclipse.kura.internal.wire.query;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.eclipse.kura.wire.store.provider.QueryableWireRecordStoreProvider;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;

public class WireRecordQueryComponent implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final Logger logger = LogManager.getLogger(WireRecordQueryComponent.class);

    private State state = new Unsatisfied();
    private WireSupport wireSupport;
    private WireHelperService wireHelperService;

    public void bindQueryableWireRecordStoreProvider(QueryableWireRecordStoreProvider store) {
        updateState(s -> s.setQueryableWireRecordStoreProvider(store));
    }

    public synchronized void unbindQueryableWireRecordStoreProvider(QueryableWireRecordStoreProvider store) {
        updateState(s -> s.unsetQueryableWireRecordStoreProvider(store));
    }

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = wireHelperService;
    }

    @SuppressWarnings("unchecked")
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.debug("Activating Wire Record Query component...");

        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

        updateState(s -> s.setOptions(new WireRecordQueryComponentOptions(properties)));

        logger.debug("Activating Wire Record Query component...done");
    }

    public void updated(final Map<String, Object> properties) {
        logger.debug("Updating Wire Record Query component... {}", properties);

        updateState(s -> s.setOptions(new WireRecordQueryComponentOptions(properties)));

        logger.debug("Updating Wire Record Query component... Done");
    }
    
    protected void deactivate() {
        logger.debug("Deactivating Wire Record Query Component...");

        logger.debug("Deactivating Wire Record Query Component... Done");
    }

    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    @Override
    public synchronized void onWireReceive(final WireEnvelope wireEnvelope) {

        final Optional<List<WireRecord>> records;

        try {
            records = this.state.getRecords();
        } catch (final Exception e) {
            logger.warn("failed to perform query", e);
            return;
        }

        if (records.isPresent()) {
            this.wireSupport.emit(records.get());
        }
    }

    @Override
    public Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }

    @Override
    public void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

    private synchronized void updateState(final UnaryOperator<State> transitionFunction) {
        this.state = transitionFunction.apply(this.state);
    }

    private interface State {

        public State setOptions(WireRecordQueryComponentOptions options);

        public State setQueryableWireRecordStoreProvider(
                final QueryableWireRecordStoreProvider queryableWireRecordStore);

        public State unsetQueryableWireRecordStoreProvider(
                final QueryableWireRecordStoreProvider queryableWireRecordStore);

        public Optional<List<WireRecord>> getRecords() throws KuraStoreException;
    }

    private static class Unsatisfied implements State {

        private Optional<WireRecordQueryComponentOptions> options = Optional.empty();
        private Optional<QueryableWireRecordStoreProvider> provider = Optional.empty();

        @Override
        public State setOptions(WireRecordQueryComponentOptions options) {
            this.options = Optional.of(options);

            return checkSatisfied();
        }

        @Override
        public State setQueryableWireRecordStoreProvider(QueryableWireRecordStoreProvider queryableWireRecordStore) {
            this.provider = Optional.of(queryableWireRecordStore);

            return checkSatisfied();
        }

        @Override
        public State unsetQueryableWireRecordStoreProvider(QueryableWireRecordStoreProvider queryableWireRecordStore) {
            if (this.provider.equals(Optional.of(queryableWireRecordStore))) {
                this.provider = Optional.empty();
            }

            return checkSatisfied();
        }

        private State checkSatisfied() {
            if (this.options.isPresent() && this.provider.isPresent()) {
                return new Satisfied(this.options.get(), this.provider.get());
            }

            return this;
        }

        @Override
        public Optional<List<WireRecord>> getRecords() throws KuraStoreException {

            throw new KuraStoreException("Component dependencies are not satisfied");
        }

    }

    private static class Satisfied implements State {

        private final WireRecordQueryComponentOptions options;
        private final QueryableWireRecordStoreProvider provider;

        private Optional<CachedRecords> cachedRecords = Optional.empty();

        public Satisfied(final WireRecordQueryComponentOptions options,
                final QueryableWireRecordStoreProvider provider) {
            this.options = options;
            this.provider = provider;
        }

        @Override
        public State setOptions(WireRecordQueryComponentOptions options) {
            if (options.equals(this.options)) {
                return this;
            }

            return new Satisfied(options, this.provider);
        }

        @Override
        public State setQueryableWireRecordStoreProvider(QueryableWireRecordStoreProvider wireRecordStoreProvider) {
            return new Satisfied(this.options, wireRecordStoreProvider);
        }

        @Override
        public State unsetQueryableWireRecordStoreProvider(QueryableWireRecordStoreProvider wireRecordStoreProvider) {
            if (this.provider == wireRecordStoreProvider) {
                return new Unsatisfied().setOptions(this.options);
            } else {
                return this;
            }
        }

        @Override
        public synchronized Optional<List<WireRecord>> getRecords() throws KuraStoreException {

            final List<WireRecord> result;

            if (cachedRecords.isPresent() && cachedRecords.get().isFresh(options)) {
                result = cachedRecords.get().records;
            } else {
                result = this.provider.performQuery(options.getQuery());

                if (options.getCacheExpirationInterval() > 0) {
                    this.cachedRecords = Optional.of(new CachedRecords(result));
                }
            }

            if (!result.isEmpty() || this.options.isEmitOnEmptyResult()) {
                return Optional.of(result);
            } else {
                return Optional.empty();
            }
        }

    }

    private static class CachedRecords {

        private final List<WireRecord> records;
        private final long timestamp;

        public CachedRecords(final List<WireRecord> records) {
            this.records = records;
            this.timestamp = System.nanoTime();
        }

        public boolean isFresh(final WireRecordQueryComponentOptions options) {

            return System.nanoTime() - timestamp < TimeUnit.SECONDS.toNanos(options.getCacheExpirationInterval());
        }
    }
}
