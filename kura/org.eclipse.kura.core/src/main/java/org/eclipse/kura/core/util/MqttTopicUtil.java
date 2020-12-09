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
package org.eclipse.kura.core.util;

import org.eclipse.paho.client.mqttv3.MqttTopic;

public final class MqttTopicUtil {

    private MqttTopicUtil() {
    }

    public static void validate(final String topicString, final boolean wildcardAllowed) {
        MqttTopic.validate(topicString, wildcardAllowed);
    }

    public static boolean isMatched(final String topicFilter, final String topicName) {
        return MqttTopic.isMatched(topicFilter, topicName);
    }
}
