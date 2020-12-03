/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.data.transport.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

public class MqttClientConfiguration {

    private final String brokerUrl;
    private final String clientId;
    private final PersistenceType persistenceType;
    private final MqttConnectOptions connectOptions;

    public enum PersistenceType {
        FILE,
        MEMORY
    }

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
