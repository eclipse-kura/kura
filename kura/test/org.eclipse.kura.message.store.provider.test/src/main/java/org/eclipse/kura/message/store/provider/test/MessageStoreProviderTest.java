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
 *******************************************************************************/
package org.eclipse.kura.message.store.provider.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.message.store.StoredMessage;
import org.eclipse.kura.message.store.provider.MessageStore;
import org.eclipse.kura.message.store.provider.MessageStoreProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MessageStoreProviderTest {

    @Test
    public void shouldStoreSimpleMessage() throws KuraStoreException {
        givenMessageStore();

        whenMessageIsStored("testTopic", byteArray(1, 2, 3, 4), 1, true, 7);

        thenMessageTopicIs(0, "testTopic");
        thenMessagePayloadIs(0, byteArray(1, 2, 3, 4));
        thenMessageQoSIs(0, 1);
        thenMessageRetainIs(0, true);
        thenMessagePriorityIs(0, 7);
        thenMessageCreatedOnIsInThePast(0);
        thenMessagePublishedOnIsNotSet(0);
        thenMessageConfirmedOnIsNotSet(0);
        thenMessageDroppedOnIsNotSet(0);
        thenDataTransportTokenIsNotSet(0);
    }

    @Test
    public void shouldGetMessageCountEmpty() throws KuraStoreException {
        givenMessageStore();

        thenMessageCountIs(0);
    }

    @Test
    public void shouldGetMessageCountNonEmpty() throws KuraStoreException {
        givenMessageStore();

        whenMessageIsStored("testTopic", byteArray(1, 2, 3, 4), 1, true, 7);
        whenMessageIsStored("testTopic", byteArray(1, 2, 3, 4), 1, true, 7);

        thenMessageCountIs(2);
    }

    @Test
    public void shouldStoreNullPayload() throws KuraStoreException {
        givenMessageStore();

        whenMessageIsStored("testTopic", null, 1, true, 7);

        thenMessagePayloadIs(0, null);
    }

    @Test
    public void shouldNotAllowNullTopic() throws KuraStoreException {
        givenMessageStore();

        whenMessageIsStored(null, byteArray(1), 1, true, 7);

        thenKuraStoreExceptionIsThrown();
    }

    @Test
    public void shouldResetIdentityGeneratorWithNoStoredMessages() throws KuraStoreException {
        givenMessageStore();

        givenLastMessageId(Integer.MAX_VALUE);

        whenMessageIsStored("testTopic", byteArray(1, 2, 3, 4), 1, true, 7);

        thenNoExceptionIsThrown();
        thenLastMessageIdIs(1);
    }

    @Test
    public void shouldResetIdentityGeneratorWithSomeMessages() throws KuraStoreException {
        givenMessageStore();

        whenMessageIsStored("testTopic", byteArray(1, 2, 3, 4), 1, true, 7);
        whenMessageIsStored("testTopic", byteArray(1, 2, 3, 4), 1, true, 7);

        givenLastMessageId(Integer.MAX_VALUE);

        whenMessageIsStored("testTopic", byteArray(1, 2, 3, 4), 1, true, 7);

        thenNoExceptionIsThrown();
        thenLastMessageIdIs(3);
    }

    @Test
    public void shouldSupportMarkAsPublishedWithQoSZero() throws KuraStoreException {
        givenMessageStore();
        givenStoredMessage("testTopic", byteArray(1, 2, 3, 4), 0, true, 7);

        whenMessageIsMarkedAsPublished(0);

        thenMessageCreatedOnIsInThePast(0);
        thenMessagePublishedOnIsInThePast(0);
        thenMessageConfirmedOnIsNotSet(0);
        thenMessageDroppedOnIsNotSet(0);
        thenDataTransportTokenIsNotSet(0);
    }

    @Test
    public void shouldSupportMarkAsPublishedWithDataTransportToken() throws KuraStoreException {
        givenMessageStore();
        givenStoredMessage("testTopic", byteArray(1, 2, 3, 4), 1, true, 7);

        whenMessageIsMarkedAsPublished(0, new DataTransportToken(23, "foo"));

        thenMessageCreatedOnIsInThePast(0);
        thenMessagePublishedOnIsInThePast(0);
        thenMessageConfirmedOnIsNotSet(0);
        thenMessageDroppedOnIsNotSet(0);
        thenDataTransportTokenIs(0, new DataTransportToken(23, "foo"));
    }

    @Test
    public void shouldSupportMarkAsConfirmed() throws KuraStoreException {
        givenMessageStore();
        givenStoredMessage("testTopic", byteArray(1, 2, 3, 4), 1, true, 7);

        whenMessageIsMarkedAsPublished(0, new DataTransportToken(23, "foo"));
        whenMessageIsMarkedAsConfirmed(0);

        thenMessageCreatedOnIsInThePast(0);
        thenMessagePublishedOnIsInThePast(0);
        thenMessageConfirmedOnIsInThePast(0);
        thenMessageDroppedOnIsNotSet(0);
        thenDataTransportTokenIs(0, new DataTransportToken(23, "foo"));
    }

    @Test
    public void shouldGetNextMessageWithSingleMessage() throws KuraStoreException {
        givenMessageStore();

        whenMessageIsStored("testTopic", byteArray(1, 2, 3, 4), 1, true, 7);

        thenNextMessageTopicIs("testTopic");
        thenNextMessagePayloadIs(byteArray(1, 2, 3, 4));
        thenNextMessageQoSIs(1);
        thenNextMessageRetainIs(true);
        thenNextMessagePriorityIs(7);
        thenNextMessageCreatedOnIsInThePast();
        thenNextMessagePublishedOnIsNotSet();
        thenNextMessageConfirmedOnIsNotSet();
        thenNextMessageDroppedOnIsNotSet();
        thenNextMessageDataTransportTokenIsNotSet();
    }

    @Test
    public void shouldReturnOldestMessage() throws KuraStoreException {
        givenMessageStore();

        whenMessageIsStored("1", byteArray(1, 2, 3, 4), 1, true, 7);
        whenMessageIsStored("2", byteArray(2, 2, 3, 4), 1, true, 7);
        whenMessageIsStored("3", byteArray(3, 2, 3, 4), 1, true, 7);
        whenMessageIsStored("4", byteArray(4, 2, 3, 4), 1, true, 7);
        whenMessageIsStored("5", byteArray(5, 2, 3, 4), 1, true, 7);

        thenNextMessageTopicIs("1");
        thenNextMessagePayloadIs(byteArray(1, 2, 3, 4));
        thenNextMessageQoSIs(1);
        thenNextMessageRetainIs(true);
        thenNextMessagePriorityIs(7);
        thenNextMessageCreatedOnIsInThePast();
        thenNextMessagePublishedOnIsNotSet();
        thenNextMessageConfirmedOnIsNotSet();
        thenNextMessageDroppedOnIsNotSet();
        thenNextMessageDataTransportTokenIsNotSet();
    }

    @Test
    public void shouldReturnOldestNonPublishedMessage() throws KuraStoreException {
        givenMessageStore();
        givenStoredMessage("1", byteArray(1, 2, 3, 4), 1, true, 7);
        givenStoredMessage("2", byteArray(2, 2, 3, 4), 2, false, 7);
        givenStoredMessage("3", byteArray(3, 2, 3, 4), 3, true, 7);
        givenStoredMessage("4", byteArray(4, 2, 3, 4), 4, false, 7);
        givenStoredMessage("5", byteArray(5, 2, 3, 4), 5, true, 7);

        whenMessageIsMarkedAsPublished(0);
        whenMessageIsMarkedAsPublished(1);

        thenNextMessageTopicIs("3");
        thenNextMessagePayloadIs(byteArray(3, 2, 3, 4));
        thenNextMessageQoSIs(3);
        thenNextMessageRetainIs(true);
        thenNextMessagePriorityIs(7);
        thenNextMessageCreatedOnIsInThePast();
        thenNextMessagePublishedOnIsNotSet();
        thenNextMessageConfirmedOnIsNotSet();
        thenNextMessageDroppedOnIsNotSet();
        thenNextMessageDataTransportTokenIsNotSet();
    }

    @Test
    public void shouldReturnHighestPriorityMessage() throws KuraStoreException {
        givenMessageStore();

        whenMessageIsStored("1", byteArray(1, 2, 3, 4), 1, true, 7);
        whenMessageIsStored("2", byteArray(2, 2, 3, 4), 2, false, 6);
        whenMessageIsStored("3", byteArray(3, 2, 3, 4), 3, true, 2);
        whenMessageIsStored("4", byteArray(4, 2, 3, 4), 4, false, 4);
        whenMessageIsStored("5", byteArray(5, 2, 3, 4), 5, true, 3);

        thenNextMessageTopicIs("3");
        thenNextMessagePayloadIs(byteArray(3, 2, 3, 4));
        thenNextMessageQoSIs(3);
        thenNextMessageRetainIs(true);
        thenNextMessagePriorityIs(2);
        thenNextMessageCreatedOnIsInThePast();
        thenNextMessagePublishedOnIsNotSet();
        thenNextMessageConfirmedOnIsNotSet();
        thenNextMessageDroppedOnIsNotSet();
        thenNextMessageDataTransportTokenIsNotSet();
    }

    @Test
    public void shouldReturnOldestNonPublishedMessageWithHighestPriority() throws KuraStoreException {
        givenMessageStore();
        givenStoredMessage("1", byteArray(1, 2, 3, 4), 1, true, 7);
        givenStoredMessage("6", byteArray(3, 2, 3, 4), 3, true, 2);
        givenStoredMessage("2", byteArray(2, 2, 3, 4), 2, false, 2);
        givenStoredMessage("3", byteArray(3, 2, 3, 4), 3, true, 2);
        givenStoredMessage("4", byteArray(4, 2, 3, 4), 4, false, 1);
        givenStoredMessage("5", byteArray(5, 2, 3, 4), 5, true, 3);

        whenMessageIsMarkedAsPublished(4);
        whenMessageIsMarkedAsPublished(1, new DataTransportToken(1, "bar"));

        thenNextMessageTopicIs("2");
        thenNextMessagePayloadIs(byteArray(2, 2, 3, 4));
        thenNextMessageQoSIs(2);
        thenNextMessageRetainIs(false);
        thenNextMessagePriorityIs(2);
        thenNextMessageCreatedOnIsInThePast();
        thenNextMessagePublishedOnIsNotSet();
        thenNextMessageConfirmedOnIsNotSet();
        thenNextMessageDroppedOnIsNotSet();
        thenNextMessageDataTransportTokenIsNotSet();
    }

    @Test
    public void shouldRetireveUnpublishedMessageList() throws KuraStoreException {
        givenMessageStore();
        givenStoredMessage("1", byteArray(1, 2, 3, 4), 1, true, 7);
        givenStoredMessage("6", byteArray(3, 2, 3, 4), 3, true, 2);
        givenStoredMessage("2", byteArray(2, 2, 3, 4), 2, false, 2);
        givenStoredMessage("3", byteArray(3, 2, 3, 4), 3, true, 2);
        givenStoredMessage("4", byteArray(4, 2, 3, 4), 0, false, 1);
        givenStoredMessage("5", byteArray(5, 2, 3, 4), 5, true, 3);

        whenMessageIsMarkedAsPublished(4);
        whenMessageIsMarkedAsPublished(1, new DataTransportToken(1, "bar"));
        whenUnpublishedMessagesAreRertieved();

        thenRetrievedMessageIdSetIs(0, 2, 3, 5);
    }

    @Test
    public void shouldRetireveInFlightMessageList() throws KuraStoreException {
        givenMessageStore();
        givenStoredMessage("1", byteArray(1, 2, 3, 4), 1, true, 7);
        givenStoredMessage("6", byteArray(3, 2, 3, 4), 3, true, 2);
        givenStoredMessage("2", byteArray(2, 2, 3, 4), 2, false, 2);
        givenStoredMessage("3", byteArray(3, 2, 3, 4), 3, true, 2);
        givenStoredMessage("4", byteArray(4, 2, 3, 4), 0, false, 1);
        givenStoredMessage("5", byteArray(5, 2, 3, 4), 5, true, 3);

        whenMessageIsMarkedAsPublished(4);
        whenMessageIsMarkedAsPublished(1, new DataTransportToken(1, "bar"));
        whenMessageIsMarkedAsPublished(2, new DataTransportToken(2, "baz"));
        whenInFlightMessagesAreRertieved();

        thenRetrievedMessageIdSetIs(1, 2);
    }

    @Test
    public void shouldRetireveDroppedMessageList() throws KuraStoreException {
        givenMessageStore();
        givenStoredMessage("1", byteArray(1, 2, 3, 4), 1, true, 7);
        givenStoredMessage("6", byteArray(3, 2, 3, 4), 3, true, 2);
        givenStoredMessage("2", byteArray(2, 2, 3, 4), 2, false, 2);
        givenStoredMessage("3", byteArray(3, 2, 3, 4), 3, true, 2);
        givenStoredMessage("4", byteArray(4, 2, 3, 4), 0, false, 1);
        givenStoredMessage("5", byteArray(5, 2, 3, 4), 5, true, 3);

        whenMessageIsMarkedAsPublished(4);
        whenMessageIsMarkedAsPublished(1, new DataTransportToken(1, "bar"));
        whenMessageIsMarkedAsPublished(2, new DataTransportToken(2, "baz"));
        whenInFlightMessagesAreDropped();
        whenMessageIsStored("6", byteArray(5, 2, 3, 4), 5, true, 3);
        whenDroppedMessagesAreRertieved();

        thenRetrievedMessageIdSetIs(1, 2);
    }

    @Test
    public void shouldDeleteOldPublishedMessagesWithQoS0() throws KuraStoreException, InterruptedException {
        givenMessageStore();
        givenStoredMessage("foo", byteArray(1, 2, 3, 4), 0, true, 7);

        whenMessageIsMarkedAsPublished(0);
        whenTimePasses(2, TimeUnit.SECONDS);
        whenStaleMessagesAreDeleted(1);

        thenMessageDoesNotExist(0);
    }

    @Test
    public void shouldNotDeleteOldNonPublishedMessagesWithQoS0() throws KuraStoreException, InterruptedException {
        givenMessageStore();
        givenStoredMessage("foo", byteArray(1, 2, 3, 4), 0, true, 7);

        whenTimePasses(2, TimeUnit.SECONDS);
        whenStaleMessagesAreDeleted(1);

        thenMessageExists(0);
    }

    @Test
    public void shouldNotDeleteRecentPublishedMessagesWithQoS0() throws KuraStoreException {
        givenMessageStore();
        givenStoredMessage("foo", byteArray(1, 2, 3, 4), 0, true, 7);

        whenMessageIsMarkedAsPublished(0);
        whenStaleMessagesAreDeleted(10);

        thenMessageExists(0);
    }

    @Test
    public void shouldNotDeleteRecentNonPublishedMessagesWithQoS0() throws KuraStoreException {
        givenMessageStore();
        givenStoredMessage("foo", byteArray(1, 2, 3, 4), 0, true, 7);

        whenStaleMessagesAreDeleted(10);

        thenMessageExists(0);
    }

    @Test
    public void shouldNotDeleteOldPublishedAndNotConfirmedMessagesWithQoS1()
            throws KuraStoreException, InterruptedException {
        givenMessageStore();
        givenStoredMessage("foo", byteArray(1, 2, 3, 4), 1, true, 7);

        whenMessageIsMarkedAsPublished(0, new DataTransportToken(1, "bar"));
        whenTimePasses(2, TimeUnit.SECONDS);
        whenStaleMessagesAreDeleted(1);

        thenMessageExists(0);
    }

    @Test
    public void shouldNotDeleteRecentPublishedAndNotConfirmedMessagesWithQoS1() throws KuraStoreException {
        givenMessageStore();
        givenStoredMessage("foo", byteArray(1, 2, 3, 4), 1, true, 7);

        whenMessageIsMarkedAsPublished(0, new DataTransportToken(1, "foo"));
        whenStaleMessagesAreDeleted(10);

        thenMessageExists(0);
    }

    @Test
    public void shouldDeleteOldPublishedAndConfirmedMessagesWithQoS1() throws KuraStoreException, InterruptedException {
        givenMessageStore();
        givenStoredMessage("foo", byteArray(1, 2, 3, 4), 1, true, 7);

        whenMessageIsMarkedAsPublished(0, new DataTransportToken(1, "bar"));
        whenMessageIsMarkedAsConfirmed(0);
        whenTimePasses(2, TimeUnit.SECONDS);
        whenStaleMessagesAreDeleted(1);

        thenMessageDoesNotExist(0);
    }

    @Test
    public void shouldNotDeleteMessagesConfirmedRecentlyWithQoS1() throws KuraStoreException, InterruptedException {
        givenMessageStore();
        givenStoredMessage("foo", byteArray(1, 2, 3, 4), 1, true, 7);

        whenMessageIsMarkedAsPublished(0, new DataTransportToken(1, "bar"));
        whenTimePasses(2, TimeUnit.SECONDS);
        whenMessageIsMarkedAsConfirmed(0);

        whenStaleMessagesAreDeleted(1);

        thenMessageExists(0);
    }

    @Test
    public void shouldNotDeleteRecentlyDroppedMessages() throws KuraStoreException {
        givenMessageStore();
        givenStoredMessage("foo", byteArray(1, 2, 3, 4), 1, true, 7);

        whenMessageIsMarkedAsPublished(0, new DataTransportToken(1, "bar"));
        whenInFlightMessagesAreDropped();

        whenStaleMessagesAreDeleted(10);

        thenMessageExists(0);
    }

    @Test
    public void shouldDeleteOldDroppedMessages() throws KuraStoreException, InterruptedException {
        givenMessageStore();
        givenStoredMessage("foo", byteArray(1, 2, 3, 4), 1, true, 7);

        whenMessageIsMarkedAsPublished(0, new DataTransportToken(1, "bar"));
        whenInFlightMessagesAreDropped();
        whenTimePasses(2, TimeUnit.SECONDS);

        whenStaleMessagesAreDeleted(1);

        thenMessageDoesNotExist(0);
    }

    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    private final TestTarget target;
    private final MessageStoreProvider messageStoreProvider;
    private final int id;
    private final String storePid;
    private final String storeName;

    private MessageStore messageStore;
    private List<Integer> messageIds = new ArrayList<>();
    private Optional<Exception> exception = Optional.empty();
    private Optional<List<StoredMessage>> retrievedMessages = Optional.empty();

    @Parameters(name = "{0}")
    public static Collection<TestTarget> targets() {
        return Arrays.asList(TestTarget.H2, TestTarget.SQLITE);
    }

    public MessageStoreProviderTest(final TestTarget target)
            throws InterruptedException, ExecutionException, TimeoutException {
        this.target = target;
        this.id = NEXT_ID.incrementAndGet();
        this.storePid = "db" + this.id;
        this.storeName = "store" + this.id;

        final ConfigurationService configurationService = ServiceUtil
                .trackService(ConfigurationService.class, Optional.empty()).get(30, TimeUnit.SECONDS);
        this.messageStoreProvider = ServiceUtil
                .createFactoryConfiguration(configurationService, MessageStoreProvider.class, this.storePid,
                        this.target.storeFactoryPid(), target.getConfigurationForPid(this.storePid))
                .get(30, TimeUnit.SECONDS);
    }

    private void givenMessageStore() throws KuraStoreException {
        this.messageStore = this.messageStoreProvider.openMessageStore(this.storeName);
    }

    private void givenLastMessageId(final int value) {
        this.target.setMessageId(storePid, storeName, value);
    }

    private void givenStoredMessage(final String topic, final byte[] payload, final int qos, final boolean retain,
            final int priority) throws KuraStoreException {
        messageIds.add(this.messageStore.store(topic, payload, qos, retain, priority));
    }

    private void whenMessageIsStored(final String topic, final byte[] payload, final int qos, final boolean retain,
            final int priority) {
        try {
            givenStoredMessage(topic, payload, qos, retain, priority);
        } catch (final Exception e) {
            this.exception = Optional.of(e);
        }
    }

    private void whenMessageIsMarkedAsPublished(final int messageIndex) throws KuraStoreException {
        this.messageStore.markAsPublished(this.messageIds.get(messageIndex));
    }

    private void whenMessageIsMarkedAsPublished(final int messageIndex, final DataTransportToken token)
            throws KuraStoreException {
        this.messageStore.markAsPublished(this.messageIds.get(messageIndex), token);
    }

    private void whenMessageIsMarkedAsConfirmed(final int messageIndex) throws KuraStoreException {
        this.messageStore.markAsConfirmed(this.messageIds.get(messageIndex));
    }

    private void whenInFlightMessagesAreDropped() throws KuraStoreException {
        this.messageStore.dropAllInFlightMessages();
    }

    private void whenUnpublishedMessagesAreRertieved() throws KuraStoreException {
        this.retrievedMessages = Optional.of(this.messageStore.getUnpublishedMessages());
    }

    private void whenInFlightMessagesAreRertieved() throws KuraStoreException {
        this.retrievedMessages = Optional.of(this.messageStore.getInFlightMessages());
    }

    private void whenDroppedMessagesAreRertieved() throws KuraStoreException {
        this.retrievedMessages = Optional.of(this.messageStore.getDroppedMessages());
    }

    private void thenRetrievedMessageIdSetIs(final int... ids) {
        final List<StoredMessage> messages = this.retrievedMessages
                .orElseThrow(() -> new IllegalStateException("no messages have been retrieved"));

        assertEquals(ids.length, messages.size());

        for (final int id : ids) {
            assertTrue(messages.stream().anyMatch(m -> m.getId() == this.messageIds.get(id)));
        }
    }

    private void thenMessageTopicIs(final int messageIndex, final String topic) throws KuraStoreException {
        assertEquals(topic, getStoredMessage(0).getTopic());
    }

    private void thenMessagePayloadIs(final int messageIndex, final byte[] payload) throws KuraStoreException {
        assertArrayEquals(payload, getStoredMessage(messageIndex).getPayload());
    }

    private void thenMessageQoSIs(final int messageIndex, final int qos) throws KuraStoreException {
        assertEquals(qos, getStoredMessage(messageIndex).getQos());
    }

    private void thenMessageRetainIs(final int messageIndex, final boolean retain) throws KuraStoreException {
        assertEquals(retain, getStoredMessage(messageIndex).isRetain());
    }

    private void thenMessagePriorityIs(final int messageIndex, final int priority) throws KuraStoreException {
        assertEquals(priority, getStoredMessage(messageIndex).getPriority());
    }

    private void thenMessageCreatedOnIsInThePast(final int messageIndex) throws KuraStoreException {
        thenIsInTheRecentPast(getStoredMessage(messageIndex).getCreatedOn());
    }

    private void thenMessagePublishedOnIsInThePast(final int messageIndex) throws KuraStoreException {
        thenIsInTheRecentPast(getStoredMessage(messageIndex).getPublishedOn());
    }

    private void thenMessageConfirmedOnIsInThePast(final int messageIndex) throws KuraStoreException {
        thenIsInTheRecentPast(getStoredMessage(messageIndex).getConfirmedOn());
    }

    private void thenMessagePublishedOnIsNotSet(final int messageIndex) throws KuraStoreException {
        assertEquals(Optional.empty(), getStoredMessage(messageIndex).getPublishedOn());
    }

    private void thenMessageConfirmedOnIsNotSet(final int messageIndex) throws KuraStoreException {
        assertEquals(Optional.empty(), getStoredMessage(messageIndex).getConfirmedOn());
    }

    private void thenMessageDroppedOnIsNotSet(final int messageIndex) throws KuraStoreException {
        assertEquals(Optional.empty(), getStoredMessage(messageIndex).getDroppedOn());
    }

    private void thenDataTransportTokenIsNotSet(final int messageIndex) throws KuraStoreException {
        assertEquals(Optional.empty(), getStoredMessage(messageIndex).getDataTransportToken());
    }

    private void thenDataTransportTokenIs(final int messageIndex, final DataTransportToken token)
            throws KuraStoreException {
        assertEquals(Optional.of(token), getStoredMessage(messageIndex).getDataTransportToken());
    }

    private void thenNextMessageTopicIs(final String topic) throws KuraStoreException {
        assertEquals(topic, getNextMessage().getTopic());
    }

    private void thenNextMessagePayloadIs(final byte[] payload) throws KuraStoreException {
        assertArrayEquals(payload, getNextMessage().getPayload());
    }

    private void thenNextMessageQoSIs(final int qos) throws KuraStoreException {
        assertEquals(qos, getNextMessage().getQos());
    }

    private void thenNextMessageRetainIs(final boolean retain) throws KuraStoreException {
        assertEquals(retain, getNextMessage().isRetain());
    }

    private void thenNextMessagePriorityIs(final int priority) throws KuraStoreException {
        assertEquals(priority, getNextMessage().getPriority());
    }

    private void thenNextMessageCreatedOnIsInThePast() throws KuraStoreException {
        thenIsInTheRecentPast(getNextMessage().getCreatedOn());
    }

    private void thenNextMessagePublishedOnIsInThePast() throws KuraStoreException {
        thenIsInTheRecentPast(getNextMessage().getPublishedOn());
    }

    private void thenNextMessageConfirmedOnIsInThePast() throws KuraStoreException {
        thenIsInTheRecentPast(getNextMessage().getConfirmedOn());
    }

    private void thenNextMessagePublishedOnIsNotSet() throws KuraStoreException {
        assertEquals(Optional.empty(), getNextMessage().getPublishedOn());
    }

    private void thenNextMessageConfirmedOnIsNotSet() throws KuraStoreException {
        assertEquals(Optional.empty(), getNextMessage().getConfirmedOn());
    }

    private void thenNextMessageDroppedOnIsNotSet() throws KuraStoreException {
        assertEquals(Optional.empty(), getNextMessage().getDroppedOn());
    }

    private void thenNextMessageDataTransportTokenIsNotSet() throws KuraStoreException {
        assertEquals(Optional.empty(), getNextMessage().getDataTransportToken());
    }

    private void thenNextMessageDataTransportTokenIs(final DataTransportToken token) throws KuraStoreException {
        assertEquals(Optional.of(token), getNextMessage().getDataTransportToken());
    }

    private void thenMessageExists(final int messageIndex) throws KuraStoreException {
        assertTrue(this.messageStore.get(this.messageIds.get(messageIndex)).isPresent());
    }

    private void thenMessageDoesNotExist(final int messageIndex) throws KuraStoreException {
        assertEquals(Optional.empty(), this.messageStore.get(this.messageIds.get(messageIndex)));
    }

    private void whenStaleMessagesAreDeleted(final int purgeAgeSeconds) throws KuraStoreException {
        this.messageStore.deleteStaleMessages(purgeAgeSeconds);
    }

    private void whenTimePasses(final long amount, final TimeUnit timeUnit) throws InterruptedException {
        Thread.sleep(timeUnit.toMillis(amount));
    }

    private void thenLastMessageIdIs(final int value) {
        assertEquals(value, (int) this.messageIds.get(this.messageIds.size() - 1));
    }

    private void thenMessageCountIs(final int expectedCount) throws KuraStoreException {
        assertEquals(expectedCount, this.messageStore.getMessageCount());
    }

    private void thenNoExceptionIsThrown() {
        if (this.exception.isPresent()) {
            this.exception.get().printStackTrace();
        }
        assertEquals(Optional.empty(), this.exception);
    }

    private void thenIsInTheRecentPast(final Optional<Date> maybeDate) {
        final long now = System.currentTimeMillis();
        final long date = maybeDate.orElseThrow(() -> new IllegalStateException("Date is not set")).toInstant()
                .toEpochMilli();

        assertTrue(date <= now && now - date < TimeUnit.SECONDS.toMillis(5));
    }

    private void thenKuraStoreExceptionIsThrown() {
        assertEquals(KuraStoreException.class,
                this.exception.orElseThrow(() -> new IllegalStateException("expected exception")).getClass());
    }

    private StoredMessage getStoredMessage(final int index) throws KuraStoreException {
        return this.messageStore.get(this.messageIds.get(index))
                .orElseThrow(() -> new IllegalStateException("no stored message"));
    }

    private StoredMessage getNextMessage() throws IllegalStateException, KuraStoreException {
        return this.messageStore.getNextMessage()
                .orElseThrow(() -> new IllegalStateException("no next message returned"));
    }

    private byte[] byteArray(final int... values) {
        final byte[] result = new byte[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }

        return result;
    }
}
