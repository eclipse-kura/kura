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

import org.eclipse.kura.store.listener.ConnectionListener;

public class ConnectionListenerManager {

    private Set<ConnectionListener> listeners = new CopyOnWriteArraySet<>();

    public void dispatchConnected() {
        this.listeners.forEach(ConnectionListener::connected);

    }

    public void dispatchDisconnected() {
        this.listeners.forEach(ConnectionListener::disconnected);

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

}
