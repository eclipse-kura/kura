/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloud;

import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * CloudClientListener is the interface to be implemented by applications that needs to be notified of events in the
 * {@link CloudClient}.
 * Arrived methods are invoked whenever a message is sent to a appTopic associated to the CloudClient.
 * The Arrived method signatures are differentiated based on whether the incoming messages have been
 * published to a data topic (by default accountName/#) or a control topic (by default $EDC/accountName/#).
 * 
 * @deprecated Please consider using {@link CloudConnectionListener} and {@link CloudSubscriberListener} instead
 */
@ConsumerType
@Deprecated
public interface CloudClientListener {

    /**
     * Called by the CloudClient when it receives a published control message from the broker.
     * If the message received has a binary payload that it has NOT been encoded using the
     * the KuraPayload class, the received bytes will be set as the body field of a new
     * KuraPaylaod instance which is passed to the callback Listener interface.
     *
     * @param deviceId
     *            The deviceId this message was addressed to.
     * @param appTopic
     *            The appTopic the message arrived on.
     * @param msg
     *            The KuraPayload that arrived.
     * @param qos
     *            The Quality of Service that the message was received on.
     * @param retain
     *            Whether the message was retained by the broker.
     */
    void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain);

    /**
     * Called by the client when it receives a published data message from the broker.
     * If the message received has a binary payload that it has NOT been encoded using the
     * the KuraPayload class, the received bytes will be set as the body field of a new
     * KuraPaylaod instance which is passed to the callback Listener interface.
     *
     * @param deviceId
     *            The asset ID of the semanticTopic prefix the message arrived on.
     * @param appTopic
     *            The appTopic the message arrived on.
     * @param msg
     *            The KuraPayload that arrived.
     * @param qos
     *            The Quality of Service that the message was received on.
     * @param retain
     *            Whether the message was retained by the broker.
     */
    void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain);

    /**
     * Called when the client has lost its connection with the broker. Depending on the {@link DataService}
     * configuration, the client will attempt to reconnect and call the
     * {@link CloudClientListener#onConnectionEstablished}
     * method upon a successful reconnect. This is only a notification, the callback handler should
     * not attempt to handle the reconnect.
     * <br>
     * If the bundle using the client relies on subscriptions beyond the default ones,
     * it is responsibility of the application to implement the {@link CloudClientListener#onConnectionEstablished}
     * callback method to restore the subscriptions it needs after a connection loss.
     */
    void onConnectionLost();

    /**
     * Called when the CloudClient has successfully connected with the broker.
     * <br>
     * If the bundle using the client relies on subscriptions beyond the default ones,
     * it is responsibility of the application to implement the {@link CloudClientListener#onConnectionEstablished}
     * callback method to restore the subscriptions it needs after a connection loss.
     */
    void onConnectionEstablished();

    /**
     * Called by the CloudClient when a published message has been fully acknowledged by the broker,
     * as appropriate for the quality of service. The published method is not called for QoS 0 publications.
     *
     * @param messageId
     *            The message id of the published message
     */
    void onMessageConfirmed(int messageId, String appTopic);

    /**
     * Called by the CloudClient when a message has been transfered from the publishing queue
     * to the underlying {@link DataTransportService} for publishing on the wire.
     */
    void onMessagePublished(int messageId, String appTopic);
}
