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

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraNotConnectedException;
import org.eclipse.kura.KuraTimeoutException;
import org.eclipse.kura.KuraTooManyInflightMessagesException;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.osgi.annotation.versioning.ProviderType;

/**
 * DataTransportService implementations provide the ability of connecting to a
 * remote broker, publish messages, subscribe to topics, receive messages on the
 * subscribed topics, and disconnect from the remote message broker.
 *
 * The <a href="http://www.osgi.org/wiki/uploads/Links/whiteboard.pdf">whiteboard pattern</a>
 * is used to notify the service users about events such as message arrived, connection lost etc.
 * through the {@link DataTransportListener}
 *
 * {@see DataTransportListener}
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface DataTransportService {

    /**
     * Connects to the remote broker. This method will block until the
     * connection is established or a timeout occurs. The actual configuration
     * needed to establish a connection is protocol specific (e.g. MQTT) and is
     * exposed through the ConfigurationAdmin.
     *
     * @throws KuraConnectException
     *             the caller MAY retry connecting a later time.
     */
    public void connect() throws KuraConnectException;

    /**
     * Returns true if the DataTransportService is currently connected to the remote server.
     */
    public boolean isConnected();

    public String getBrokerUrl();

    /**
     * Returns the account name associated with the DataTransportService
     */
    public String getAccountName();

    public String getUsername();

    public String getClientId();

    /**
     * Disconnects from the broker. This method will block, up to the specified
     * duration, allowing the protocol implementation to complete delivery of
     * in-flight messages before actually disconnecting from the broker.
     *
     * @param quiesceTimeout
     *            - timeout that will be used before forcing a disconnect
     */
    public void disconnect(long quiesceTimeout);

    /**
     * Subscribes to a topic on the broker. This method MAY block until the
     * underlying protocol message (e.g. the MQTT SUBSCRIBE message) is
     * acknowledged by the broker or a timeout occurs. This message is
     * idempotent so the caller may safely retry subscribing. The timeout
     * interval used by the service is configurable through the
     * ConfigurationService.
     *
     * @param topic
     * @param qos
     * @throws KuraTimeoutException
     *             TODO
     * @throws KuraException
     * @throws KuraNotConnectedException
     *             TODO
     */
    public void subscribe(String topic, int qos) throws KuraTimeoutException, KuraException, KuraNotConnectedException;

    /**
     * Unsubscribes to a topic on the broker. This method MAY block until the
     * underlying protocol message (e.g. the MQTT UNSUBSCRIBE message) is
     * acknowledged by the broker or a timeout occurs. The timeout
     * interval used by the service is configurable through the
     * ConfigurationService.
     *
     * @param topic
     * @throws KuraTimeoutException
     * @throws KuraException
     * @throws KuraNotConnectedException
     *             TODO
     */
    public void unsubscribe(String topic) throws KuraTimeoutException, KuraException, KuraNotConnectedException;

    /**
     * Enqueues a message for publishing with the underlying transport implementation.
     * An active connection to the remote server is required.
     *
     * @param topic
     * @param payload
     * @param qos
     * @param retain
     * @return
     * @throws KuraTooManyInflightMessagesException
     * @throws KuraException
     * @throws KuraNotConnectedException
     *             TODO
     */
    public DataTransportToken publish(String topic, byte[] payload, int qos, boolean retain)
            throws KuraTooManyInflightMessagesException, KuraException, KuraNotConnectedException;

    /**
     * Adds a listener.
     *
     * @param listener
     *
     * @since 1.0.8
     */
    public void addDataTransportListener(DataTransportListener listener);

    /**
     * Removes a listener.
     *
     * @param listener
     *
     * @since 1.0.8
     */
    public void removeDataTransportListener(DataTransportListener listener);
}
