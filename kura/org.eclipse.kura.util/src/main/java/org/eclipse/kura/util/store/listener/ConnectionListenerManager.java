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
 *******************************************************************************/

package org.eclipse.kura.util.store.listener;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.kura.store.listener.ConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionListenerManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionListenerManager.class);

    private final ExecutorService dispatchThread = Executors.newSingleThreadExecutor();
    private Set<ConnectionListener> listeners = new CopyOnWriteArraySet<>();

    public void dispatchConnected() {
        this.dispatchThread.execute(() -> dispatch(ConnectionListener::connected));
    }

    public void dispatchDisconnected() {
        this.dispatchThread.execute(() -> dispatch(ConnectionListener::disconnected));
    }

    private void dispatch(final Consumer<ConnectionListener> consumer) {
        for (final ConnectionListener listener : listeners) {
            try {
                consumer.accept(listener);
            } catch (final Exception e) {
                logger.warn("Unexpected exception dispatching event", e);
            }
        }
    }

    public void addAll(Set<ConnectionListener> listeners) {
        this.listeners.addAll(listeners);

    }

    public void add(ConnectionListener listener) {
        this.listeners.add(listener);

    }

    public void remove(ConnectionListener listener) {
        this.listeners.remove(listener);
    }

    public void shutdown() {
        this.dispatchThread.shutdown();
        try {
            this.dispatchThread.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for executor shutdown", e);
            Thread.currentThread().interrupt();
        }
    }

}
