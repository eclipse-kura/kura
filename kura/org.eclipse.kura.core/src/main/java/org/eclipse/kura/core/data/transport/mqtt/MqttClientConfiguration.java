/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.data.transport.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

public class MqttClientConfiguration {

    private final String brokerUrl;
    private final String clientId;
    private final PersistenceType persistenceType;
    private final MqttConnectOptions connectOptions;

    public enum PersistenceType {
        FILE, MEMORY
    };

    public MqttClientConfiguration(String brokerUrl, String clientId, PersistenceType persistenceType,
            MqttConnectOptions connectOptions) {
        super();
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.persistenceType = persistenceType;
        this.connectOptions = connectOptions;
    }

    public String getBrokerUrl() {
        return this.brokerUrl;
    }

    public String getClientId() {
        return this.clientId;
    }

    public PersistenceType getPersistenceType() {
        return this.persistenceType;
    }

    public MqttConnectOptions getConnectOptions() {
        return this.connectOptions;
    }
}
