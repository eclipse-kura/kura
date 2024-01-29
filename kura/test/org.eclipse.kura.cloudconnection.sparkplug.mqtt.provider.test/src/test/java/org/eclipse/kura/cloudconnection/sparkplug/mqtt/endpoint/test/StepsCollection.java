/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraDisconnectException;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint;
import org.eclipse.kura.data.DataService;
import org.osgi.service.event.EventAdmin;

public class StepsCollection {

    SparkplugCloudEndpoint endpoint = new SparkplugCloudEndpoint();
    private Exception occurredException;

    /*
     * Given
     */

    void givenDataService(DataService dataService) {
        this.endpoint.setDataService(dataService);
    }

    void givenEventAdmin(EventAdmin eventAdmin) {
        this.endpoint.setEventAdmin(eventAdmin);
    }

    void givenCloudConnectionListener(CloudConnectionListener listener) {
        this.endpoint.registerCloudConnectionListener(listener);
    }

    void givenUnregisterCloudConnectionListener(CloudConnectionListener listener) {
        this.endpoint.unregisterCloudConnectionListener(listener);
    }

    void givenCloudDeliveryListener(CloudDeliveryListener listener) {
        this.endpoint.registerCloudDeliveryListener(listener);
    }

    void givenUnregisterCloudDeliveryListener(CloudDeliveryListener listener) {
        this.endpoint.unregisterCloudDeliveryListener(listener);
    }
    
    void givenActivated(Map<String, Object> properties) {
        this.endpoint.activate(properties);
    }

    /*
     * When
     */

    void whenOnConnectionEstabilished() {
        this.endpoint.onConnectionEstablished();
    }

    void whenOnDisconnected() {
        this.endpoint.onDisconnected();
    }

    void whenOnConnectionLost() {
        this.endpoint.onConnectionLost(new Throwable());
    }

    void whenActivate(Map<String, Object> properties) {
        try {
            givenActivated(properties);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    void whenUpdate() {
        this.endpoint.update();
    }

    void whenIsConnected() {
        this.endpoint.isConnected();
    }

    void whenDisconnect() throws KuraDisconnectException {
        this.endpoint.disconnect();
    }

    void whenConnect() throws KuraConnectException {
        this.endpoint.connect();
    }

    void whenOnMessageConfirmed(int messageId, String topic) {
        this.endpoint.onMessageConfirmed(messageId, topic);
    }

    void whenDeactivate() {
        try {
            this.endpoint.deactivate();
        } catch (Exception e) {
            this.occurredException = e;
        }

    }

    /*
     * Then
     */

    <E extends Exception> void thenExceptionOccurred(Class<E> expectedException) {
        assertNotNull(this.occurredException);
        assertEquals(expectedException.getName(), this.occurredException.getClass().getName());
    }

    /*
     * Utilities
     */

    class PropertiesBuilder {

        private Map<String, Object> properties = new HashMap<>();

        public PropertiesBuilder add(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public Map<String, Object> build() {
            return this.properties;
        }

    }


}
