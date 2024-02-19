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
    private CloudSubscriberListener subListener1 = mock(CloudSubscriberListener.class);
    private CloudSubscriberListener subListener2 = mock(CloudSubscriberListener.class);
    private CloudSubscriberListener subListener3 = mock(CloudSubscriberListener.class);

    /*
     * Scenarios
     */

    @Test
    public void removeSomeShouldNotReturnTopicsToUnsubscribe() {
        givenSubscriptionMapWith("t1", 0, this.subListener1);
        givenSubscriptionMapWith("t1", 0, this.subListener2);
        
        whenRemoveIsCalledFor(this.subListener1);

        thenSubscriptionRecordsContains(new SubscriptionRecord("t1", 0));
        thenMatchingListenersContains("t1", 0, this.subListener2);
        thenReturnedTopicsToUnsubscribeIsEmpty();
    }

    @Test
    public void removeAllShouldReturnTopicsToUnsubscribe() {
        givenSubscriptionMapWith("t1", 0, this.subListener1);
        givenSubscriptionMapWith("t1", 0, this.subListener2);

        whenRemoveIsCalledFor(this.subListener1, this.subListener2);

        thenSubscriptionRecordsNotContains(new SubscriptionRecord("t1", 0));
        thenMatchingListenersNotContains("t1", 0, this.subListener2);
        thenReturnedTopicsToUnsubscribeContains("t1");
    }

    @Test
    public void shouldMatchSingleLevelWildCard1() {
        givenSubscriptionMapWith("A/B", 0, this.subListener1);
        givenSubscriptionMapWith("A/+/C", 0, this.subListener2);
        givenSubscriptionMapWith("A/B/C", 0, this.subListener3);

        whenGetListenersMatching("A/B/C", 0);

        thenReturnedMatchingListenersNotContains(this.subListener1);
        thenReturnedMatchingListenersContains(this.subListener2);
        thenReturnedMatchingListenersContains(this.subListener3);
    }

    @Test
    public void shouldMatchSingleLevelWildCard2() {
        givenSubscriptionMapWith("A/B", 0, this.subListener1);
        givenSubscriptionMapWith("A/+", 0, this.subListener2);
        givenSubscriptionMapWith("A/B/C", 0, this.subListener3);

        whenGetListenersMatching("A/B", 0);

        thenReturnedMatchingListenersContains(this.subListener1);
        thenReturnedMatchingListenersContains(this.subListener2);
        thenReturnedMatchingListenersNotContains(this.subListener3);
    }

    @Test
    public void shouldMatchMultiLevelWildCard() {
        givenSubscriptionMapWith("A/B", 0, this.subListener1);
        givenSubscriptionMapWith("A/#", 0, this.subListener2);
        givenSubscriptionMapWith("A/B/C", 0, this.subListener3);

        whenGetListenersMatching("A/B/C", 0);

        thenReturnedMatchingListenersNotContains(this.subListener1);
        thenReturnedMatchingListenersContains(this.subListener2);
        thenReturnedMatchingListenersContains(this.subListener3);
    }

    @Test
    public void shouldMatchWithCorrectQos() {
        givenSubscriptionMapWith("A/B", 1, this.subListener1);
        givenSubscriptionMapWith("A/#", 0, this.subListener2);
        givenSubscriptionMapWith("A/B/C", 1, this.subListener3);

        whenGetListenersMatching("A/B/C", 0);

        thenReturnedMatchingListenersNotContains(this.subListener1);
        thenReturnedMatchingListenersContains(this.subListener2);
        thenReturnedMatchingListenersNotContains(this.subListener3);
    }

    @Test
    public void shouldMatchWithMaximumSubscribedQos() {
        givenSubscriptionMapWith("A/B/C", 0, this.subListener1);

        whenGetListenersMatching("A/B/C", 1);

        thenReturnedMatchingListenersContains(this.subListener1);
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenSubscriptionMapWith(String topicFilter, int qos, CloudSubscriberListener listener) {
        this.subscriptionsMap.add(topicFilter, qos, listener);
    }

    /*
     * When
     */

    private void whenRemoveIsCalledFor(CloudSubscriberListener... listeners) {
        for (CloudSubscriberListener listener : listeners) {
            this.returnedTopicsToUnsubscribe.addAll(this.subscriptionsMap.remove(listener));
        }
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
