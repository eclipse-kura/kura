/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.subscriber.listener;

import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * The {@link CloudSubscriberListener} interface has to be implemented by applications that needs to be notified of
 * events in the subscriber.
 * Notification methods are invoked whenever a message is received in the associated {@link CloudSubscriber}.
 *
 * @since 2.0
 */
@ConsumerType
@FunctionalInterface
public interface CloudSubscriberListener {

    /**
     * Called by the {@link CloudSubscriber} when a message is received from the remote cloud platform.
     * The received message will be parsed and passed as a {@link KuraMessage} to the listener.
     *
     * @param message
     *            The {@link KuraMessage} that wraps the received message.
     */
    public void onMessageArrived(KuraMessage message);
}
