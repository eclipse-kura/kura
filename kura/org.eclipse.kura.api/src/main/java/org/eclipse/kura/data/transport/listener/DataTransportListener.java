/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.data.transport.listener;

import org.eclipse.kura.data.DataTransportToken;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Listener interface to be implemented by applications that needs to be notified of events in the
 * {@link org.eclipse.kura.data.DataTransportService}.
 * All registered listeners are called synchronously by the {@link org.eclipse.kura.data.DataTransportService} at the
 * occurrence of the event.
 * It expected that implementers of this interface do NOT perform long running tasks in the implementation of this
 * interface.
 *
 * @since 1.0.8
 */
@ConsumerType
public interface DataTransportListener {

    /**
     * Notifies the listener of the establishment of the new connection with the remote server.
     *
     * @param newSession
     *            true if the connection is to the same broker with the same client ID.
     */
    public void onConnectionEstablished(boolean newSession);

    /**
     * Notifies the listener that the connection to the remote server is about to be terminated.
     */
    public void onDisconnecting();

    /**
     * Notifies the listener that the connection to the remote server has been terminated.
     */
    public void onDisconnected();

    /**
     * Notifies the {@link org.eclipse.kura.data.DataTransportService} has received a configuration update.
     */
    public void onConfigurationUpdating(boolean wasConnected);

    /**
     * Notifies the {@link org.eclipse.kura.data.DataTransportService} has received a configuration update and it has
     * applied the new
     * configuration
     */
    public void onConfigurationUpdated(boolean wasConnected);

    /**
     * Notifies the listener that the connection to the remote server has been lost.
     */
    public void onConnectionLost(Throwable cause);

    /**
     * Notifies the listener that a new message has been received from the remote server.
     */
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained);

    /**
     * Notifies the listener that a message has been confirmed by the remote server.
     */
    public void onMessageConfirmed(DataTransportToken token);
}
