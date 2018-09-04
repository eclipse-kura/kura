/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
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
