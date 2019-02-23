/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.data;

import java.util.List;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.db.H2DbService;

/**
 * DataStore implementation have the responsibility of doing the bookkeeping of
 * the messages that are in transient in the system. A message in the system
 * normally flows through the following states: stored -> published ->
 * confirmed (or dropped). The data store should be able to store messages, track and update
 * their state, and perform certain queries for messages in a given state.
 */
public interface DataStore {

    public void start(H2DbService dbService, int houseKeeperInterval, int purgeAge, int capacity)
            throws KuraStoreException;

    public void update(int houseKeeperInterval, int purgeAge, int capacity);

    public void stop();

    /**
     * Stores an MQTT message for deferred publication. An identifier is always
     * generated and returned, even for messages published with QoS = 0. The
     * store policy is FIFO within each priority level, 0 being the highest
     * priority.
     * 
     * @param topic
     * @param payload
     * @param qos
     * @param retain
     * @param priority
     * @return
     * @throws KuraStoreException
     */
    public DataMessage store(String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraStoreException;

    /**
     * Acknowledges the publication of the DataMessage with the given ID
     * associating it to the protocol (e.g. MQTT) message ID (QoS > 0).
     * 
     * @param msgId
     * @param publishedMsgId
     * @param sessionId
     *            TODO
     * @throws KuraStoreException
     */
    public void published(int msgId, int publishedMsgId, String sessionId) throws KuraStoreException;

    /**
     * Acknowledges the publication of the DataMessage with the given ID. This
     * is typically called for messages published with QoS = 0.
     * 
     * @param msgId
     * @param publishedMsgId
     * @throws KuraStoreException
     */
    public void published(int msgId) throws KuraStoreException;

    /**
     * Acknowledges the delivery of the DataMessage published with the given
     * protocol (e.g. MQTT) message ID. This method is only called for messages
     * published with QoS > 0.
     * 
     * @param msgId
     * @throws KuraStoreException
     */
    public void confirmed(int msgId) throws KuraStoreException;

    /**
     * Gets the next unpublished message. Messages with higher
     * priority (0 is the highest priority) are returned first. Within each
     * priority level the oldest unpublished message is returned first.
     * 
     * @return
     * @throws KuraStoreException
     */
    public DataMessage getNextMessage() throws KuraStoreException;

    /**
     * Returns a message from the DataStore by its message id.
     * 
     * @param msgId
     *            ID of the message to be loaded
     * @return Loaded message or null if not found.
     * @throws KuraStoreException
     */
    public DataMessage get(int msgId) throws KuraStoreException;

    /**
     * Finds the list of all unpublished messages and returns them WITHOUT loading the payload.
     * 
     * @return
     * @throws KuraStoreException
     */
    public List<DataMessage> allUnpublishedMessagesNoPayload() throws KuraStoreException;

    /**
     * Finds the list of all published but not yet confirmed messages and returns them WITHOUT loading the payload.
     * These are only messages published with QoS > 0.
     * Messages published with QoS = 0 do not belong to the results list.
     * 
     * @return
     * @throws KuraStoreException
     */
    public List<DataMessage> allInFlightMessagesNoPayload() throws KuraStoreException;

    /**
     * Finds the list of all published messages that will not be confirmed and returns them WITHOUT loading the payload.
     * These are only messages published with QoS > 0.
     * Messages published with QoS = 0 do not belong to the results list.
     * 
     * @return
     */
    public List<DataMessage> allDroppedInFlightMessagesNoPayload() throws KuraStoreException;

    /**
     * Marks all in-flight messages as unpublished.
     * 
     * @throws KuraStoreException
     */
    public void unpublishAllInFlighMessages() throws KuraStoreException;

    /**
     * Drops all in-flight messages.
     * 
     * @throws KuraStoreException
     */
    public void dropAllInFlightMessages() throws KuraStoreException;

    /**
     * Deletes stale messages.
     * These are either published messages with QoS = 0 or confirmed messages with QoS > 0, whose age exceeds the
     * argument.
     * 
     * @param purgeAge
     * @throws KuraStoreException
     */
    public void deleteStaleMessages(int purgeAge) throws KuraStoreException;

    /**
     * Checks and attempts to repair the store.
     * 
     * @throws KuraStoreException
     */
    public void repair() throws KuraStoreException;
}
