/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.cloud.subscriber;

import org.eclipse.kura.core.cloud.CloudServiceOptions;
import org.eclipse.kura.core.util.MqttTopicUtil;

public class CloudSubscriptionRecord {

    private final String topic;
    private final int qos;

    private String topicFilter;

    public CloudSubscriptionRecord(final String topic, final int qos) {
        this.topic = topic;
        this.qos = qos;
    }

    public String getTopic() {
        return this.topic;
    }

    public int getQos() {
        return this.qos;
    }

    public boolean matches(final String topic) {
        if (topicFilter == null) {
            topicFilter = this.topic.replaceAll(CloudServiceOptions.getTopicAccountToken(), "+")
                    .replaceAll(CloudServiceOptions.getTopicClientIdToken(), "+");
        }
        return MqttTopicUtil.isMatched(this.topicFilter, topic);
    }

    @Override
    public int hashCode() {
        return topic.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof CloudSubscriptionRecord)) {
            return false;
        }
        return ((CloudSubscriptionRecord) obj).topic.equals(topic);
    }
}
