/**
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.eclipse.kura.internal.driver.opcua;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.listener.ChannelEvent;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.internal.driver.opcua.request.ListenParams;
import org.eclipse.kura.internal.driver.opcua.request.ListenRequest;
import org.eclipse.kura.internal.driver.opcua.request.SingleNodeListenParams;

public class ListenerRegistrationRegistry {

    private final Map<ListenParams, Collection<ListenRequest>> registeredListeners = new HashMap<>();
    private final List<Listener> itemListeners = new CopyOnWriteArrayList<>();
    private long state;

    public synchronized void registerListeners(final Collection<ListenRequest> requests) {
        boolean subscriptionsChanged = false;
        for (final ListenRequest request : requests) {
            subscriptionsChanged |= registerListenerInternal(request);
        }
        if (subscriptionsChanged) {
            notifyChanged();
        }
    }

    public synchronized void unregisterListeners(final Collection<ChannelListener> listeners) {
        boolean subscriptionsChanged = false;
        for (final ChannelListener listener : listeners) {
            subscriptionsChanged |= unregisterListenerInternal(listener);
        }
        if (subscriptionsChanged) {
            notifyChanged();
        }
    }

    public synchronized void registerListener(final ListenRequest request) {
        if (registerListenerInternal(request)) {
            notifyChanged();
        }
    }

    public synchronized void unregisterListener(final ChannelListener listener) {
        if (unregisterListenerInternal(listener)) {
            notifyChanged();
        }
    }

    public Collection<ListenRequest> getRequests(final SingleNodeListenParams params) {
        return this.registeredListeners.get(params);
    }

    public Map<ListenParams, Collection<ListenRequest>> getRegisteredListeners() {
        return this.registeredListeners;
    }

    private boolean registerListenerInternal(final ListenRequest request) {
        final ListenParams params = request.getParameters();
        Collection<ListenRequest> listeners = this.registeredListeners.get(params);
        if (listeners == null) {
            listeners = new CopyOnWriteArrayList<>();
            listeners.add(request);
            this.registeredListeners.put(params, listeners);
            this.itemListeners.forEach(l -> l.onListenerRegistered(request));
            return true;
        } else {
            if (listeners.stream().anyMatch(req -> req.getChannelListener() == request.getChannelListener()
                    && Objects.equals(req.getRecord().getChannelName(), request.getRecord().getChannelName()))) {
                return false;
            }
            listeners.add(request);
            this.itemListeners.forEach(l -> l.onListenerRegistered(request));
            return false;
        }
    }

    private boolean unregisterListenerInternal(final ChannelListener listener) {
        final boolean removed = this.registeredListeners.entrySet().removeIf(e -> {
            final Collection<ListenRequest> listeners = e.getValue();
            listeners.removeIf(req -> req.getChannelListener() == listener);
            return listeners.isEmpty();
        });
        this.itemListeners.forEach(l -> l.onListenerUnregistered(listener));
        return removed;
    }

    private void notifyChanged() {
        this.state = Math.max(0, this.state + 1);
        this.itemListeners.forEach(Listener::onRegistrationsChanged);
    }

    public synchronized void computeDifferences(final Set<ListenParams> other, final Consumer<ListenParams> toBeCreated,
            final Consumer<ListenParams> toBeDeleted) {
        final Set<ListenParams> currentItems = this.registeredListeners.keySet();
        for (final ListenParams params : currentItems) {
            if (!other.contains(params)) {
                toBeCreated.accept(params);
            }
        }
        for (final ListenParams params : other) {
            if (!currentItems.contains(params)) {
                toBeDeleted.accept(params);
            }
        }
    }

    public Dispatcher getDispatcher(final ListenParams params) {
        return new Dispatcher(params);
    }

    public Set<ListenParams> getItems() {
        return Collections.unmodifiableSet(this.registeredListeners.keySet());
    }

    public synchronized boolean isEmpty() {
        return this.registeredListeners.isEmpty();
    }

    public synchronized void addRegistrationItemListener(final Listener listener) {
        this.itemListeners.add(listener);
    }

    public synchronized void removeRegistrationItemListener(final Listener listener) {
        this.itemListeners.remove(listener);
    }

    public interface Listener {

        public void onRegistrationsChanged();

        public void onListenerRegistered(final ListenRequest request);

        public void onListenerUnregistered(final ChannelListener listener);
    }

    public final class Dispatcher {

        private final ListenParams params;
        private Optional<Collection<ListenRequest>> channelListeners = Optional.empty();
        private long currentState = -1;

        private Dispatcher(final ListenParams params) {
            this.params = params;
            sync();
        }

        private void sync() {
            synchronized (ListenerRegistrationRegistry.this) {
                if (ListenerRegistrationRegistry.this.state != this.currentState) {
                    this.currentState = ListenerRegistrationRegistry.this.state;
                    this.channelListeners = Optional
                            .ofNullable(ListenerRegistrationRegistry.this.registeredListeners.get(this.params));
                }
            }
        }

        public ListenParams getParams() {
            return this.params;
        }

        public void dispatch(final Consumer<ChannelRecord> filler) {
            sync();
            if (this.channelListeners.isPresent()) {
                final Collection<ListenRequest> requests = this.channelListeners.get();
                for (final ListenRequest request : requests) {
                    final ChannelRecord record = request.getRecord();
                    filler.accept(record);
                    request.getChannelListener().onChannelEvent(new ChannelEvent(record));
                }
            }
        }
    }
}
