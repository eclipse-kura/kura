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
package org.eclipse.kura.cloudconnection.subscriber.listener;

import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * The {@link CloudSubscriberListener} interface has to be implemented by applications that needs to be notified of
 * events in the subscriber.
 * Notification methods are invoked whenever a message is received in the associated
 * {@link org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber}.
 *
 * @since 2.0
 */
@ConsumerType
@FunctionalInterface
public interface CloudSubscriberListener {

    /**
     * Called by the {@link org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber} when a message is received from
     * the remote cloud platform.
     * The received message will be parsed and passed as a {@link KuraMessage} to the listener.
     *
     * @param message
     *            The {@link KuraMessage} that wraps the received message.
     */
    public void onMessageArrived(KuraMessage message);
}
