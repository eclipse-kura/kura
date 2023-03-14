/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.message.store.provider;

import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.message.store.StoredMessage;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a message store suitable for supporting the Kura default
 * {@link org.eclipse.kura.data.DataService} implementation.
 * 
 * See {@link StoredMessage} for a description of the stored message fields.
 * 
 * @since 2.5
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface MessageStore {

    /**
     * Inserts a new message in the store. The implementation must set the value of
     * the <code>createdOn</code> message parameter to the current time.
     * <br>
     * 
     * @param topic    the value of the <code>topic</code> parameter.
     * @param payload  topic the value of the <code>payload</code> parameter.
     * @param qos      topic the value of the <code>QoS</code> parameter.
     * @param retain   topic the value of the <code>retain</code> parameter.
     * @param priority topic the value of the <code>priority</code> parameter.
     * @return An identifier for the stored message.
     * @throws KuraStoreException
     */
    public int store(String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraStoreException;

    /**
     * Sets the value of the <code>publishedOn</code> parameter to the current time.
     * <br>
     * This method must be used for messages with QoS = 0.
     *
     * @param msgId the message identifier
     * @throws KuraStoreException
     */
    public void markAsPublished(int msgId) throws KuraStoreException;

    /**
     * Sets the value of the <code>publishedOn</code> parameter to the current time
     * and associates the given {@link DataTransportToken} with the current
     * message.
     * <br>
     * This method must be used for messages with QoS >= 1.
     *
     * @param msgId              the message identifier.
     * @param dataTransportToken the {@link DataTransportToken}.
     * @throws KuraStoreException
     */
    public void markAsPublished(int msgId, DataTransportToken dataTransportToken) throws KuraStoreException;

    /**
     * Sets the value of the <code>confirmedOn</code> parameter to the current time.
     * <br>
     * This method must be used for messages with QoS >= 1.
     *
     * @param msgId the message identifier.
     * @throws KuraStoreException
     */
    public void markAsConfirmed(int msgId) throws KuraStoreException;

    /**
     * Gets the next message that should be published, if any.
     * 
     * The returned message must have the following properties:
     * 
     * <ol>
     * <li>The <code>publishedOn</code> parameter must not be set.</li>
     * <li>It must have the lowest value of the <code>priority</code> numeric
     * parameter (highest priority) between messages that satisfy 1.</li>
     * <li>It must have the minimum <code>createdOn</code> parameter value between
     * the messages that satisfy 2.</li>
     * </ol>
     * 
     * In other words it must be the oldest message between the ones with highest
     * priority that have not been published yet.
     *
     * @return the next message that should be published, if any.
     * @throws KuraStoreException
     */
    public Optional<StoredMessage> getNextMessage() throws KuraStoreException;

    /**
     * Retrieves the message with the given identifier from the store.
     * 
     * @param msgId the message identifier.
     * @return the retrieved message, or empty if there is no message in the store
     *         with the given identifier.
     * @throws KuraStoreException
     */
    public Optional<StoredMessage> get(int msgId) throws KuraStoreException;

    /**
     * Returns the number of messages currently in the store.
     * This should include all messages, regardless of the value of their
     * parameters.
     * 
     * @return the message count.
     * @throws KuraStoreException
     */
    public int getMessageCount() throws KuraStoreException;

    /**
     * Returns the list of messages whose <code>publishedOn</code> parameter is not
     * set.
     * <br>
     * It is not necessary to return the message <code>payload</code>.
     *
     * @return the unpublished message list.
     * @throws KuraStoreException
     */
    public List<StoredMessage> getUnpublishedMessages() throws KuraStoreException;

    /**
     * Returns the list of messages that satisfy all of the following conditions:
     * 
     * <ol>
     * <li>The value of the <code>QoS</code> parameter is greater than 0.</li>
     * <li>The <code>publishedOn</code> parameter is set.</li>
     * <li>The <code>confirmedOn</code> parameter is not set.</li>
     * <li>The <code>droppedOn</code> parameter is not set.</li>
     * </ol>
     * 
     * It is not necessary to return the message <code>payload</code>.
     *
     * @return the in-flight message list.
     * @throws KuraStoreException
     */
    public List<StoredMessage> getInFlightMessages() throws KuraStoreException;

    /**
     * Returns the list of messages with the following property:
     * 
     * <ol>
     * <li>The <code>droppedOn</code> parameter must be set.</li>
     * </ol>
     * 
     * It is not necessary to return the message <code>payload</code>.
     *
     * @return the dropped message list.
     * @throws KuraStoreException
     */
    public List<StoredMessage> getDroppedMessages() throws KuraStoreException;

    /**
     * Removes the value of the <code>publishedOn</code> parameter from the messages
     * that satisfy all of the following conditions:
     * 
     * <ul>
     * <li>The <code>publishedOn</code> parameter is set.</li>
     * <li>The <code>confirmedOn</code> parameter is not set.</li>
     * </ul>
     *
     * @throws KuraStoreException
     */
    public void unpublishAllInFlighMessages() throws KuraStoreException;

    /**
     * Sets the value of the <code>droppedOn</code> parameter to the current
     * timestamp to all messages that satisfy all of the following conditions:
     *
     * <ul>
     * <li>The value of the <code>QoS</code> parameter is greater than 0.</li>
     * <li>The <code>publishedOn</code> parameter is set.</li>
     * <li>The <code>confirmedOn</code> parameter is not set.</li>
     * </ul>
     * 
     * @throws KuraStoreException
     */
    public void dropAllInFlightMessages() throws KuraStoreException;

    /**
     * Deletes the messages that satisfy any of the following conditions:
     * 
     * <ul>
     * <li>The value of the <code>droppedOn</code> parameter is set and is more than
     * <code>purgeAgeSeconds</code> in the past.</li>
     * <li>The value of the <code>confirmedOn</code> parameter is set and is more
     * than
     * <code>purgeAgeSeconds</code> in the past.</li>
     * <li>The value of the <code>QoS</code> parameter is 0 and
     * <code>publishedOn</code> is set and is more than <code>purgeAgeSeconds</code>
     * in the
     * past.</li>
     * </ul>
     *
     * @param purgeAgeSeconds the purge age in seconds.
     * @throws KuraStoreException
     */
    public void deleteStaleMessages(int purgeAgeSeconds) throws KuraStoreException;

    /**
     * Closes the message store, releasing any runtime resources allocated for it.
     */
    public void close();
}
