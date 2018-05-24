/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.publisher;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The CloudPublisher interface is an abstraction on top of the {@link CloudEndpoint} to simplify the
 * publishing process for each application running in the framework.
 * A CloudPublisher is used to publish a message with the specified KuraPayload to a cloud platform.
 * The CloudPublisher and the associated CloudEndpoint implementations abstract, to the user applications, all the low
 * level specificities like the message destination address, quality of service or properties because those are added by
 * the {@link CloudPublisher} implementation based, for example, on a configuration.
 *
 * When an application wants to publish, it has to take a CloudPublisher instance and use the
 * {@link CloudPublisher#publish(KuraMessage)} method, passing as argument a {@link KuraMessage}.
 *
 * Every KuraMessage accepted by the CloudPublisher is associated to a string identifier that can be
 * used to confirm that the KuraMessage has been published.
 * However, the semantics of the confirmation depends on both the implementation and the configuration of the
 * connection.
 * For example, if the protocol of the cloud connection supports message identifiers and acknowledgments, the
 * implementation will map the message identifier to the KuraMessage identifier and confirm it when
 * the message identifier is acknowledged. If the protocol does not support message identifiers, or the message does not
 * request an acknowledge, a confirmed KuraMessage identifier may at most indicate that the message has been
 * successfully transmitted. There is no guarantee that a KuraMessage identifier will ever be confirmed. It is
 * important that the implementation of the CloudPublisher and its configuration match the assumptions of the
 * API consumer about the delivery.
 *
 * For example, if the correct behavior of an application requires guaranteed message delivery, the
 * application should be configured to use a reliable publisher. Of course, applications that do not require this will
 * work with any publisher.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface CloudPublisher {

    /**
     * Publishes the received {@link KuraMessage} using the associated cloud connection.
     *
     * @param message
     *            The {@link KuraMessage} to be published
     * @return a String representing the message ID
     * @throws KuraException
     *             if the publishing operation fails.
     */
    public String publish(KuraMessage message) throws KuraException;

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

    /**
     * The implementation will register the {@link CloudDeliveryListener} instance passed as argument. Once a cloud
     * connection related event happens, all the registered {@link CloudDeliveryListener}s will be notified.
     *
     * @param cloudDeliveryListener
     *            a {@link CloudDeliveryListener} instance
     */
    public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener);

    /**
     * Unregisters the provided {@link CloudDeliveryListener} instance from cloud connection related events
     * notifications.
     *
     * @param cloudConnectionListener
     */
    public void unregisterCloudDeliveryistener(CloudDeliveryListener cloudDeliveryListener);

}
