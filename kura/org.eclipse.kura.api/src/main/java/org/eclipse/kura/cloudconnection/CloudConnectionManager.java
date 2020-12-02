/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraDisconnectException;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A {@link CloudEndpoint} can implement CloudConnectionManager to support the management of
 * long-lived/always on connections.
 *
 * This interface provides methods to connect, disconnect and get the connection state of the associated CloudEndpoint.
 * It also provides methods to register {@link CloudConnectionListener}s that will be notified of connection-related
 * events.
 *
 * The implementor must register itself as a CloudConnectionManager OSGi service provider.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
@ProviderType
public interface CloudConnectionManager {

    /**
     * Establishes a connection to the configured cloud platform.
     *
     * @throws org.eclipse.kura.KuraException
     *             if the operation fails
     */
    public void connect() throws KuraConnectException;

    /**
     * Performs a clean disconnection from the cloud platform.
     *
     * @throws org.eclipse.kura.KuraException
     *             if the operation fails
     */
    public void disconnect() throws KuraDisconnectException;

    /**
     * Tests if the connection is alive.
     *
     * @return {@code true} if the framework is connected to the remote server. {@code false} otherwise.
     */
    public boolean isConnected();

    /**
     * The implementation will register the {@link CloudConnectionListener} instance passed as argument. Once a cloud
     * connection related event happens, all the registered {@link CloudConnectionListener}s will be notified.
     *
     * @param cloudConnectionListener
     *            a {@link CloudConnectionListener} instance
     */
    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener);

    /**
     * Unregisters the provided {@link CloudConnectionListener} instance from cloud connection related events
     * notifications.
     *
     * @param cloudConnectionListener
     */
    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener);

}
