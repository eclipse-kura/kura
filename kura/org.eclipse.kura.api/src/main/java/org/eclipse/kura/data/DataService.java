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
package org.eclipse.kura.data;

import java.util.List;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraNotConnectedException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.KuraTimeoutException;
import org.eclipse.kura.data.listener.DataServiceListener;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The DataService provides the ability of connecting to a remote
 * broker, publish messages, subscribe to topics, receive messages on the
 * subscribed topics, and disconnect from the remote message broker.
 * The DataService delegates to the {@link DataTransportService} the implementation
 * of the transport protocol used to interact with the remote server.
 * <br>
 * The DataService offers methods and configuration options to manage the
 * connection to the remote server. For example, it can be configured
 * to auto-connect to the remote server on start-up or it offers
 * methods for applications to directly manage the connection.
 * It also adds the capability of storing published messages in a persistent store
 * and send them over the wire at a later time.
 * The purpose is to relieve service users from implementing their own persistent store.
 * Service users may publish messages independently on the DataService connection status.
 * <br>
 * In order to overcome the potential latencies introduced by buffering messages,
 * the DataService allows to assign a priority level to each published message.
 * Dependently on the store configuration there are certain guarantees that stored
 * messages are not lost due to sudden crashes or power outages.
 * <br>
 * The <a href="http://www.osgi.org/wiki/uploads/Links/whiteboard.pdf">whiteboard pattern</a>
 * is used to notify the service users about events such as message arrived, connection lost etc.
 * through the {@link DataServiceListener}.
 * {@see DataServiceListener}
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface DataService {

    /**
     * Connects to the broker if not already connected.
     */
    public void connect() throws KuraConnectException;

    /**
     * Answers if the DataService is connected to the broker.
     *
     * @return
     */
    public boolean isConnected();

    public boolean isAutoConnectEnabled();

    public int getRetryInterval();

    /**
     * Disconnects from the broker. This method will block, up to the specified
     * duration, allowing the protocol implementation to complete delivery of
     * in-flight messages before actually disconnecting from the broker.
     * If the Data Service is configured to auto-connect on startup and it's
     * explicitly disconnected it will not automatically reconnect.
     *
     * @param quiesceTimeout
     */
    public void disconnect(long quiesceTimeout);

    /**
     * Subscribes to the specified topic with the remote server.
     * The method requires an active connection with the remote server and it is operates synchronously.
     * The implementation is a pass-through to the {@link DataTransportService#subscribe} method.
     *
     * @param topic
     * @param qos
     * @throws KuraTimeoutException
     * @throws KuraException
     * @throws KuraNotConnectedException
     *             TODO
     */
    public void subscribe(String topic, int qos) throws KuraTimeoutException, KuraException, KuraNotConnectedException;

    /**
     * Unubscribes to the specified topic with the remote server.
     * The method requires an active connection with the remote server and it is operates synchronously.
     * The implementation is a pass-through to the {@link DataTransportService#unsubscribe} method.
     *
     * @param topic
     * @throws KuraTimeoutException
     * @throws KuraException
     * @throws KuraNotConnectedException
     *             TODO
     */
    public void unsubscribe(String topic) throws KuraTimeoutException, KuraException, KuraNotConnectedException;

    /**
     * Publishes a message to the broker. This method quickly returns deferring
     * the actual message publication accordingly to the current service policy
     * and to the specified priority, 0 being the highest.
     *
     * Messages are confirmed asynchronously to the caller by the
     * {@link DataServiceListener#onMessageConfirmed} callback.
     *
     * A unique identifier is always returned, independently on the specified
     * QoS or priority level, which can be used to match the asynchronous
     * message confirm.
     *
     * The actual semantics associated to a message confirm is as follows:
     * <ul>
     * <li>For messages published at QoS = 0, receiving the confirm just means that
     * the message is about to be transmitted on the wire without any guarantee
     * that it eventually will.
     * <li>For messages published at QoS &gt; 0, receiving the confirm means that the
     * broker acknowledged the message.
     * </ul>
     *
     * Priority level 0 (highest) should be used sparingly and reserved for
     * messages that should be sent with the minimum latency.
     * For example Cloud life-cycle messages are published with priority 0
     * as soon the connection is established and just before disconnecting.
     * <br>
     * Data messages, tolerating an higher latency, may be published with a
     * lower priority. Within each priority level and each QoS level, messages
     * are guaranteed do be delivered in order (oldest first).
     * <br>
     * The KuraStoreCapacityReachedException is thrown if the database buffer
     * has reached its capacity for messages that are not yet published or
     * they are still in transit. The limit does not apply to internal messages
     * with the priority less than 2.
     * These priority levels are reserved to the framework which uses it for life-cycle messages
     * - birth and death certificates - and replies to request/response flows.
     *
     * @param topic
     * @param payload
     * @param qos
     * @param retain
     * @param priority
     * @return
     * @throws KuraStoreException
     */
    public int publish(String topic, byte[] payload, int qos, boolean retain, int priority) throws KuraStoreException;

    /**
     * Finds the list of identifiers of messages that have not been published yet.
     * Given the service has no means of knowing who
     * published the message, a regex topic must be specified in order to find
     * only the relevant identifiers.
     *
     *
     * @param topicRegex
     * @return
     * @throws KuraStoreException
     */
    List<Integer> getUnpublishedMessageIds(String topicRegex) throws KuraStoreException;

    /**
     * Finds the list of identifiers of messages that are still in-flight
     * (messages published but not confirmed yet).
     * This only applies to messages published with QoS &gt; 0.
     * Given the service has no means of knowing who
     * published the message, a regex topic must be specified in order to find
     * only the relevant identifiers.
     *
     * @param topicRegex
     * @return
     * @throws KuraStoreException
     */
    List<Integer> getInFlightMessageIds(String topicRegex) throws KuraStoreException;

    /**
     * Finds the list of identifiers of in-flight messages that have been dropped.
     * This only applies to messages published with QoS &gt; 0.
     * On the establishment of a new connection, the service can be configured
     * either to republish or drop in-flight messages.
     * The former option can be used if service users tolerate publishing message
     * duplicates.
     * The latter option can be used it service users tolerate losing messages.
     * Given the service has no means of knowing who
     * published the message, a regex topic must be specified in order to find
     * only the relevant identifiers.
     */
    List<Integer> getDroppedInFlightMessageIds(String topicRegex) throws KuraStoreException;

    /**
     * Adds a listener.
     *
     * @param listener
     *
     * @since 1.0.8
     */
    public void addDataServiceListener(DataServiceListener listener);

    /**
     * Removes a listener.
     *
     * @param listener
     *
     * @since 1.0.8
     */
    public void removeDataServiceListener(DataServiceListener listener);
}
