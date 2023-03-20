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
package org.eclipse.kura.internal.wire.store;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.connection.listener.ConnectionListener;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.eclipse.kura.wire.store.provider.WireRecordStore;
import org.eclipse.kura.wire.store.provider.WireRecordStoreProvider;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;

public class WireRecordStoreComponent implements WireEmitter, WireReceiver, ConfigurableComponent, ConnectionListener {

    private static final Logger logger = LogManager.getLogger(WireRecordStoreComponent.class);

    private State state = new Unsatisfied();
    private WireHelperService wireHelperService;
    private WireSupport wireSupport;

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = wireHelperService;
    }

    public synchronized void bindWireRecordStoreProvider(final WireRecordStoreProvider wireRecordStoreProvider) {
        wireRecordStoreProvider.addListener(this);
        updateState(s -> s.setWireRecordStoreProvider(wireRecordStoreProvider));
    }

    public synchronized void unbindWireRecordStoreProvider(final WireRecordStoreProvider wireRecordStoreProvider) {
        if (state.getWireRecordStoreProvider().equals(Optional.of(wireRecordStoreProvider))) {
            wireRecordStoreProvider.removeListener(this);
            updateState(s -> s.unsetWireRecordStoreProvider(wireRecordStoreProvider));
        }
    }

    @SuppressWarnings("unchecked")
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.debug("Activating Wire Record Store Component...");

        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

        updateState(s -> s.setOptions(new WireRecordStoreComponentOptions(properties)));

        logger.debug("Activating Wire Record Store Component... Done");
    }

    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug("Updating Wire Record Store Component...");

        updateState(s -> s.setOptions(new WireRecordStoreComponentOptions(properties)));

        logger.debug("Updating Wire Record Store Component... Done");
    }

    protected void deactivate() {
        logger.debug("Deactivating Wire Record Store Component...");

        this.state.shutdown();

        logger.debug("Deactivating Wire Record Store Component... Done");
    }

    @Override
    public synchronized void onWireReceive(final WireEnvelope wireEvelope) {

        final List<WireRecord> records = wireEvelope.getRecords();

        try {
            this.state.store(records);
        } catch (KuraException e) {
            logger.warn("Failed to store Wire Records", e);
        }

        this.wireSupport.emit(records);
    }

    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
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

        public State setOptions(WireRecordStoreComponentOptions options);

        public State setWireRecordStoreProvider(final WireRecordStoreProvider wireRecordStoreProvider);

        public State unsetWireRecordStoreProvider(final WireRecordStoreProvider wireRecordStoreProvider);

        public Optional<WireRecordStoreProvider> getWireRecordStoreProvider();

        public void store(final List<WireRecord> records) throws KuraStoreException;

        public void shutdown();

        public State onWireRecordStoreDisconnected();
    }

    private static class Unsatisfied implements State {

        private Optional<WireRecordStoreComponentOptions> options = Optional.empty();
        private Optional<WireRecordStoreProvider> provider = Optional.empty();

        @Override
        public State setOptions(WireRecordStoreComponentOptions options) {
            this.options = Optional.of(options);

            return checkSatisfied();
        }

        @Override
        public State setWireRecordStoreProvider(WireRecordStoreProvider wireRecordStoreProvider) {
            this.provider = Optional.of(wireRecordStoreProvider);

            return checkSatisfied();
        }

        @Override
        public State unsetWireRecordStoreProvider(WireRecordStoreProvider wireRecordStoreProvider) {
            this.provider = Optional.empty();

            return checkSatisfied();
        }

        private State checkSatisfied() {
            if (this.options.isPresent() && this.provider.isPresent()) {
                return new Satisfied(this.options.get(), this.provider.get());
            }

            return this;
        }

        @Override
        public void store(final List<WireRecord> records) throws KuraStoreException {

            throw new KuraStoreException("Component dependencies are not satisfied");
        }

        @Override
        public void shutdown() {
            // nothing to shutdown
        }

        @Override
        public State onWireRecordStoreDisconnected() {
            return this;
        }

        @Override
        public Optional<WireRecordStoreProvider> getWireRecordStoreProvider() {
            return this.provider;
        }

    }

    private static class Satisfied implements State {

        private final WireRecordStoreComponentOptions options;
        private final WireRecordStoreProvider provider;
        private Optional<WireRecordStore> store = Optional.empty();

        public Satisfied(final WireRecordStoreComponentOptions options, final WireRecordStoreProvider provider) {
            this.options = options;
            this.provider = provider;
        }

        @Override
        public State setOptions(WireRecordStoreComponentOptions options) {
            if (options.equals(this.options)) {
                return this;
            }

            shutdown();
            return new Satisfied(options, this.provider);
        }

        @Override
        public State setWireRecordStoreProvider(WireRecordStoreProvider wireRecordStoreProvider) {
            shutdown();
            return new Satisfied(this.options, wireRecordStoreProvider);
        }

        @Override
        public State unsetWireRecordStoreProvider(WireRecordStoreProvider wireRecordStoreProvider) {
            return new Unsatisfied().setOptions(this.options);
        }

        @Override
        public void store(final List<WireRecord> records) throws KuraStoreException {
            try {

                storeInternal(records);

            } catch (final Exception e) {
                logger.warn("failed to store records, attempting to reopen store...");
                shutdown();
                storeInternal(records);
            }
        }

        private void storeInternal(final List<WireRecord> records) throws KuraStoreException {
            final WireRecordStore currentStore = getWireRecordStore();

            if (currentStore.getSize() >= this.options.getMaximumStoreSize()) {
                final int recordsToKeep = Math.min(this.options.getCleanupRecordsKeep(),
                        this.options.getMaximumStoreSize());

                currentStore.truncate(Math.max(0, recordsToKeep - 1));
            }

            currentStore.insertRecords(records);
        }

        @Override
        public void shutdown() {
            if (this.store.isPresent()) {
                this.store.get().close();
                this.store = Optional.empty();
            }
        }

        private WireRecordStore getWireRecordStore() throws KuraStoreException {
            if (this.store.isPresent()) {
                return this.store.get();
            }

            this.store = Optional.of(this.provider.openWireRecordStore(this.options.getStoreName()));

            return getWireRecordStore();
        }

        @Override
        public State onWireRecordStoreDisconnected() {
            this.shutdown();
            return this;
        }

        @Override
        public Optional<WireRecordStoreProvider> getWireRecordStoreProvider() {
            return Optional.of(this.provider);
        }
    }

    @Override
    public void disconnected() {
        updateState(State::onWireRecordStoreDisconnected);
    }

    @Override
    public void connected() {
        // TODO Auto-generated method stub

    }
}
