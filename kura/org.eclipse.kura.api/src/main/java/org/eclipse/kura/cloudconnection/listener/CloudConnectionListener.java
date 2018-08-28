/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.listener;

import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * The CloudConnectionListener interface is implemented by applications that want to be notified on connection
 * status changes.
 * To be notified, the implementor needs to register itself to a specific {@link CloudConnectionManager}.
 *
 * @since 2.0
 */
@ConsumerType
public interface CloudConnectionListener {

    /**
     * Notifies a clean disconnection from the cloud platform.
     */
    public void onDisconnected();

    /**
     * Called when the client has lost its connection to the cloud platform. This is only a notification, the callback
     * handler
     * should not attempt to handle the reconnect.
     *
     * If the bundle using the client relies on subscriptions beyond the default ones,
     * it is responsibility of the application to implement the {@link CloudConnectionListener#onConnectionEstablished}
     * callback method to restore the subscriptions it needs after a connection loss.
     */
    public void onConnectionLost();

    /**
     * Called when the cloud stack has successfully connected to the cloud platform.
     *
     * If the bundle using the client relies on subscriptions beyond the default ones,
     * it is responsibility of the application to implement the {@link CloudConnectionListener#onConnectionEstablished}
     * callback method to restore the subscriptions it needs after a connection loss.
     */
    public void onConnectionEstablished();

}
