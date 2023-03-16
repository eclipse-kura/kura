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
 ******************************************************************************/
package org.eclipse.kura.wire.store.provider;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.connection.listener.ConnectionListener;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a service that allows to create {@link WireRecordStore} instances.
 * 
 * @since 2.5
 * @noextend This class is not intended to be extended by clients.
 */
@ProviderType
public interface WireRecordStoreProvider {

    /**
     * Opens or creates a {@link WireRecordStore} instance with the given name. Invoking
     * this method could allocate the resources required to support the returned {@link WireRecordStore} instance (for
     * example tables in a RDBMS).*
     * 
     * @param name
     *            the store name
     * @return the result {@link WireRecordStore}.
     * @throws KuraStoreException
     */
    public WireRecordStore openWireRecordStore(String name) throws KuraStoreException;

    /**
     * Adds a {@link ConnectionListener}. A typical behavior of a client of this listener is to close the currently
     * open
     * {@link WireRecordStore} instances when a {@link ConnectionListener#disconnected()} event is received.
     *
     * @param listener
     *            to add
     *
     * @since 2.5.0
     */
    public void addListener(ConnectionListener listener);

    /**
     * Removes a {@link ConnectionListener}
     *
     * @param listener
     *            to remove
     *
     * @since 2.5.0
     */
    public void removeListener(ConnectionListener listener);
}
