/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloudconnection;

import java.util.Collections;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.osgi.annotation.versioning.ProviderType;

/**
 * CloudEndpoint provide APIs to ease the communication with a cloud platform allowing to publish a message, manage
 * subscribers or get information about the specific cloud endpoint configuration.
 *
 * Each CloudEndpoint is referenced by zero or more {@link CloudPublisher} and {@link CloudSubscriber} instances.
 *
 * Applications should not use directly this API but, instead, use the {@link CloudPublisher} and the
 * {@link CloudSubscriber} interfaces to give applications the capabilities to publish and receive messages.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
@ProviderType
public interface CloudEndpoint {

    /**
     * Publishes the received {@link KuraMessage} to the associated cloud platform and returns, if supported, a String
     * representing a message ID.
     * {@code null} is returned if the cloud endpoint will not confirm the message delivery, either because this is not
     * supported by the underlying protocol or because the cloud endpoint itself is not implemented or configured to
     * request the confirmation.
     *
     * @param message
     *            the {@link KuraMessage} to be published
     * @return a String representing the message ID or {@code null} if not supported
     * @throws KuraException
     *             if the publishing operation fails.
     */
    public String publish(KuraMessage message) throws KuraException;

    /**
     * Registers the provided {@link CloudSubscriberListener} using the specified {@code subscriptionProperties} that will allow
     * to disambiguate the specific subscriptions.
     *
     * @param subscriptionProperties
     *            a map representing the subscription context
     * @param subscriber
     *            a {@link CloudSubscriberListener} object that will be notified when a message is received in a context
     *            that matches the one identified by the subscription properties.
     */
    public void registerSubscriber(Map<String, Object> subscriptionProperties, CloudSubscriberListener subscriber);

    /**
     * Unregisters the subscriber identified by the provided {@code subscriptionProperties}
     * 
     * @param subscriptionProperties
     */
    public void unregisterSubscriber(Map<String, Object> subscriptionProperties);

    /**
     * Provides information related to the associated connection. The information provided depends on the specific
     * implementation and type of connection to the remote resource. The default implementation returns an empty
     * map.
     *
     * @return a map that represents all the information related to the specific connection.
     */
    public default Map<String, String> getInfo() {
        return Collections.emptyMap();
    }

}
