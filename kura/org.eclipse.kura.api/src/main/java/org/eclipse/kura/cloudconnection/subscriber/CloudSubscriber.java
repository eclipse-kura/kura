/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.subscriber;

import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface intended to have a specific implementation associated to a {@link CloudEndpoint} that wraps the
 * specificities related to the targeted cloud provider.
 *
 * The {@link CloudSubscriber} interface is an abstraction on top of the {@link CloudEndpoint} to simplify the
 * subscription and notification process, for each application running in the framework.
 *
 * When an application wants to receive a message from the cloud, it has to take a {@link CloudSubscriber} instance and
 * register itself as a {@link CloudSubscriberListener}, in order to be notified when a message is received from the
 * associated cloud stack.
 *
 * In most cases, the consumers are not interested in the header of the received message and assume to always receive the
 * same kind of message. In order to receive different kinds of messages, the consumer should register to multiple
 * subscribers.
 *
 * Some messaging protocols have a hierarchical addressing structure supporting multilevel wildcard subscriptions. For
 * example, an MQTT address (topic) hierarchy might look like
 * building/${building-number}/apartment/${apartment-number}/heating/temperature.
 * To receive the heating temperature measurements for all the buildings and apartments the subscriber can be configured
 * to subscribe to building/+/apartment/+/heating/temperature. When a message is received, the subscriber will notify
 * the application providing the payload received and some properties that are implementation specific, depending on the
 * subscriber, and that can allow the application to understand the corresponding resource.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface CloudSubscriber {

    /**
     * Registers the {@link CloudSubscriberListener} instance passed as an argument. All the registered
     * {@link CloudSubscriberListener}s will be notified by the implementation when a message is received.
     *
     * @param listener
     *            a {@link CloudSubscriberListener} instance that will be notified when a message will be received from
     *            the remote cloud platform.
     */
    public void registerCloudSubscriberListener(CloudSubscriberListener listener);

    /**
     * Unregisters the provided {@link CloudSubscriberListener} from the list of the notified listeners.
     *
     * @param listener
     */
    public void unregisterCloudSubscriberListener(CloudSubscriberListener listener);

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
