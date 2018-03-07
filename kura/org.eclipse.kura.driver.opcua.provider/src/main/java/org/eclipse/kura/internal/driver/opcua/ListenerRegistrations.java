/**
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.listener.ChannelEvent;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.internal.driver.opcua.request.ListenParams;
import org.eclipse.kura.internal.driver.opcua.request.ListenRequest;

public class ListenerRegistrations {

    private final Map<ListenParams, Collection<ListenRequest>> registeredListeners = new HashMap<>();
    private List<Listener> itemListeners = new CopyOnWriteArrayList<>();
    private long state;

    public synchronized void registerListener(final ListenRequest request) {

        final ListenParams params = request.getParameters();
        Collection<ListenRequest> listeners = registeredListeners.get(params);
        if (listeners == null) {
            listeners = new CopyOnWriteArrayList<>();
            listeners.add(request);
            registeredListeners.put(params, listeners);
            notifyChanged();
        } else {
            if (listeners.stream().anyMatch(req -> req.getChannelListener() == request.getChannelListener()
                    && req.getRecord().getChannelName() == request.getRecord().getChannelName())) {
                return;
            }
            listeners.add(request);
        }
    }

    public synchronized void unregisterListener(final ChannelListener listener) {
        final boolean removed = registeredListeners.entrySet().removeIf(e -> {
            final Collection<ListenRequest> listeners = e.getValue();
            listeners.removeIf(req -> req.getChannelListener() == listener);
            return listeners.isEmpty();
        });
        if (removed) {
            notifyChanged();
        }
    }

    private void notifyChanged() {
        state = Math.max(0, state + 1);
        itemListeners.forEach(Listener::onRegistrationsChanged);
    }

    public synchronized void computeDifferences(final Set<ListenParams> other, final Consumer<ListenParams> toBeCreated,
            final Consumer<ListenParams> toBeDeleted) {
        final Set<ListenParams> currentItems = registeredListeners.keySet();
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
    }

    public final class Dispatcher {

        private final ListenParams params;
        private Optional<Collection<ListenRequest>> channelListeners = Optional.empty();
        private long currentState = -1;

        private Dispatcher(ListenParams params) {
            this.params = params;
            sync();
        }

        private void sync() {
            synchronized (ListenerRegistrations.this) {
                if (state != currentState) {
                    currentState = state;
                    channelListeners = Optional.ofNullable(registeredListeners.get(params));
                }
            }
        }

        public ListenParams getParams() {
            return params;
        }

        public void dispatch(final Consumer<ChannelRecord> filler) {
            sync();
            if (channelListeners.isPresent()) {
                final Collection<ListenRequest> requests = channelListeners.get();
                for (final ListenRequest request : requests) {
                    final ChannelRecord record = request.getRecord();
                    filler.accept(record);
                    request.getChannelListener().onChannelEvent(new ChannelEvent(record));
                }
            }
        }
    }
}
