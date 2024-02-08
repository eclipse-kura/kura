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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;

public class SubscriptionsMap {

    private Map<SubscriptionRecord, Set<CloudSubscriberListener>> subscriptions = new HashMap<>();

    public void add(String topicFilter, int qos, CloudSubscriberListener listener) {
        SubscriptionRecord subscription = new SubscriptionRecord(topicFilter, qos);

        Set<CloudSubscriberListener> listeners = this.subscriptions.computeIfAbsent(subscription,
                key -> new CopyOnWriteArraySet<CloudSubscriberListener>());

        listeners.add(listener);
    }

    public List<String> remove(CloudSubscriberListener listener) {
        List<String> topicsToUnsubscribe = new ArrayList<>();

        this.subscriptions.entrySet().removeIf(entry -> {
            entry.getValue().remove(listener);

            if (entry.getValue().isEmpty()) {
                topicsToUnsubscribe.add(entry.getKey().getTopicFilter());
                return true;
            }

            return false;
        });

        return topicsToUnsubscribe;
    }

    public List<CloudSubscriberListener> getMatchingListeners(String topic, int qos) {
        List<CloudSubscriberListener> result = new ArrayList<>();

        this.subscriptions.forEach((subscription, listeners) -> {
            if (subscription.matches(topic, qos)) {
                result.addAll(listeners);
            }
        });

        return result;
    }

    public Set<SubscriptionRecord> getSubscriptionRecords() {
        return this.subscriptions.keySet();
    }


}
