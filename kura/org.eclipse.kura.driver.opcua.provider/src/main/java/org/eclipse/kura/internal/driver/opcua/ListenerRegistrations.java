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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.listener.ChannelEvent;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.internal.driver.opcua.request.ListenParams;
import org.eclipse.kura.internal.driver.opcua.request.ListenRequest;

public class ListenerRegistrations {

    private final Map<ListenParams, List<ListenRequest>> registeredListeners = new HashMap<>();
    private List<Listener> itemListeners = Collections.synchronizedList(new ArrayList<>());

    public synchronized void registerListener(final ListenRequest request) {

        final ListenParams params = request.getParameters();
        List<ListenRequest> listeners = registeredListeners.get(params);
        if (listeners == null) {
            listeners = new ArrayList<>();
            listeners.add(request);
            registeredListeners.put(params, listeners);
            itemListeners.forEach(l -> l.onRegistrationsChanged());
        } else {
            if (listeners.stream()
                    .filter(req -> req.getChannelListener() == request.getChannelListener()
                            && req.getRecord().getChannelName() == request.getRecord().getChannelName())
                    .findAny().isPresent()) {
                return;
            }
            listeners.add(request);
        }
    }

    public synchronized void unregisterListener(final ChannelListener listener) {
        final boolean removed = registeredListeners.entrySet().removeIf(e -> {
            final List<ListenRequest> registeredListeners = e.getValue();
            registeredListeners.removeIf(req -> req.getChannelListener() == listener);
            if (registeredListeners.isEmpty()) {
                return true;
            }
            return false;
        });
        if (removed) {
            itemListeners.forEach(l -> l.onRegistrationsChanged());
        }
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

    public synchronized void dispatchEvent(final ListenParams params, final Consumer<ChannelRecord> filler) {
        final List<ListenRequest> requests = this.registeredListeners.get(params); // TODO dispatch events without
                                                                                   // performing an hash map lookup
        if (requests != null) {
            for (final ListenRequest request : requests) {
                final ChannelRecord record = request.getRecord();
                filler.accept(record);
                request.getChannelListener().onChannelEvent(new ChannelEvent(record));
            }
        }
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
}
