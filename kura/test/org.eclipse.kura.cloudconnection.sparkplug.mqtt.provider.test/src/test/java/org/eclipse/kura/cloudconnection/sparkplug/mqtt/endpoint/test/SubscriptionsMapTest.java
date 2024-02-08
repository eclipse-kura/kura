/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SubscriptionRecord;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SubscriptionsMap;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.junit.Test;

public class SubscriptionsMapTest {

    private SubscriptionsMap subscriptionsMap = new SubscriptionsMap();
    private List<String> returnedTopicsToUnsubscribe = new LinkedList<>();
    private List<CloudSubscriberListener> returnedMatchingListeners = new LinkedList<>();
    private CloudSubscriberListener l1 = mock(CloudSubscriberListener.class);
    private CloudSubscriberListener l2 = mock(CloudSubscriberListener.class);
    private CloudSubscriberListener l3 = mock(CloudSubscriberListener.class);

    /*
     * Scenarios
     */

    @Test
    public void removeSomeShouldNotReturnTopicsToUnsubscribe() {
        givenAdd("t1", 0, this.l1);
        givenAdd("t1", 0, this.l2);
        
        whenRemove(this.l1);

        thenSubscriptionRecordsContains(new SubscriptionRecord("t1", 0));
        thenMatchingListenersContains("t1", 0, this.l2);
        thenReturnedTopicsToUnsubscribeIsEmpty();
    }

    @Test
    public void removeAllShouldReturnTopicsToUnsubscribe() {
        givenAdd("t1", 0, this.l1);
        givenAdd("t1", 0, this.l2);
        givenRemove(this.l1);

        whenRemove(this.l2);

        thenSubscriptionRecordsNotContains(new SubscriptionRecord("t1", 0));
        thenMatchingListenersNotContains("t1", 0, this.l2);
        thenReturnedTopicsToUnsubscribeContains("t1");
    }

    @Test
    public void shouldMatchSingleLevelWildCard1() {
        givenAdd("A/B", 0, this.l1);
        givenAdd("A/+/C", 0, this.l2);
        givenAdd("A/B/C", 0, this.l3);

        whenGetListenersMatching("A/B/C", 0);

        thenReturnedMatchingListenersNotContains(this.l1);
        thenReturnedMatchingListenersContains(this.l2);
        thenReturnedMatchingListenersContains(this.l3);
    }

    @Test
    public void shouldMatchSingleLevelWildCard2() {
        givenAdd("A/B", 0, this.l1);
        givenAdd("A/+", 0, this.l2);
        givenAdd("A/B/C", 0, this.l3);

        whenGetListenersMatching("A/B", 0);

        thenReturnedMatchingListenersContains(this.l1);
        thenReturnedMatchingListenersContains(this.l2);
        thenReturnedMatchingListenersNotContains(this.l3);
    }

    @Test
    public void shouldMatchMultiLevelWildCard() {
        givenAdd("A/B", 0, this.l1);
        givenAdd("A/#", 0, this.l2);
        givenAdd("A/B/C", 0, this.l3);

        whenGetListenersMatching("A/B/C", 0);

        thenReturnedMatchingListenersNotContains(this.l1);
        thenReturnedMatchingListenersContains(this.l2);
        thenReturnedMatchingListenersContains(this.l3);
    }

    @Test
    public void shouldMatchWithCorrectQos() {
        givenAdd("A/B", 1, this.l1);
        givenAdd("A/#", 0, this.l2);
        givenAdd("A/B/C", 1, this.l3);

        whenGetListenersMatching("A/B/C", 0);

        thenReturnedMatchingListenersNotContains(this.l1);
        thenReturnedMatchingListenersContains(this.l2);
        thenReturnedMatchingListenersNotContains(this.l3);
    }

    @Test
    public void shouldMatchWithMaximumSubscribedQos() {
        givenAdd("A/B/C", 0, this.l1);

        whenGetListenersMatching("A/B/C", 1);

        thenReturnedMatchingListenersContains(this.l1);
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenAdd(String topicFilter, int qos, CloudSubscriberListener listener) {
        this.subscriptionsMap.add(topicFilter, qos, listener);
    }

    private void givenRemove(CloudSubscriberListener listener) {
        this.returnedTopicsToUnsubscribe.addAll(this.subscriptionsMap.remove(listener));
    }

    /*
     * When
     */

    private void whenRemove(CloudSubscriberListener listener) {
        givenRemove(listener);
    }

    private void whenGetListenersMatching(String topic, int qos) {
        this.returnedMatchingListeners = this.subscriptionsMap.getMatchingListeners(topic, qos);
    }

    /*
     * Then
     */

    private void thenSubscriptionRecordsContains(SubscriptionRecord subscription) {
        assertTrue(this.subscriptionsMap.getSubscriptionRecords().contains(subscription));
    }

    private void thenSubscriptionRecordsNotContains(SubscriptionRecord subscription) {
        assertFalse(this.subscriptionsMap.getSubscriptionRecords().contains(subscription));
    }

    private void thenMatchingListenersContains(String topic, int qos, CloudSubscriberListener listener) {
        whenGetListenersMatching(topic, qos);
        thenReturnedMatchingListenersContains(listener);
    }

    private void thenMatchingListenersNotContains(String topic, int qos, CloudSubscriberListener listener) {
        whenGetListenersMatching(topic, qos);
        thenReturnedMatchingListenersNotContains(listener);
    }

    private void thenReturnedTopicsToUnsubscribeIsEmpty() {
        assertTrue(this.returnedTopicsToUnsubscribe.isEmpty());
    }

    private void thenReturnedTopicsToUnsubscribeContains(String expectedTopic) {
        assertTrue(this.returnedTopicsToUnsubscribe.contains(expectedTopic));
    }

    private void thenReturnedMatchingListenersContains(CloudSubscriberListener listener) {
        assertTrue(this.returnedMatchingListeners.contains(listener));
    }

    private void thenReturnedMatchingListenersNotContains(CloudSubscriberListener listener) {
        assertFalse(this.returnedMatchingListeners.contains(listener));
    }

    /*
     * Utils
     */

}
