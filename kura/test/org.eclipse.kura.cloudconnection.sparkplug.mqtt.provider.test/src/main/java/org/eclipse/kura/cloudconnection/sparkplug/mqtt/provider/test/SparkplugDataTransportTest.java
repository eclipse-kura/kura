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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.provider.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugTopics;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransportOptions;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Payloads are already tested in unit tests, not verifying them here
 *
 */
public class SparkplugDataTransportTest extends SparkplugIntegrationTest {

    private static final long DEFAULT_TIMEOUT_MS = 45_000L;

    private DataTransportListener listener = mock(DataTransportListener.class);
    private Exception occurredException;
    private MqttCallback callback = mock(MqttCallback.class);

    @Before
    public void addListener() throws Exception {
        sparkplugDataTransport.addDataTransportListener(this.listener);
        client.setCallback(callback);
        client.subscribe("spBv1.0/g1/NBIRTH/n1", 0);
    }

    @After
    public void removeListenerAndDisconnect() throws MqttException {
        sparkplugDataTransport.removeDataTransportListener(this.listener);
        client.unsubscribe("spBv1.0/g1/NBIRTH/n1");
        sparkplugDataTransport.disconnect(0L);
    }

    /*
     * Scenarios
     */

    @Test
    public void shouldSendNodeBirthWithoutPrimaryHost() throws Exception {
        givenUpdated("g1", "n1", "", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);

        whenConnect();

        thenListenerNotifiedOnConnectionEstabilished();
        thenMessageSent("spBv1.0/g1/NBIRTH/n1", 0, false);
    }

    @Test
    public void shouldNotSendNodeBirthWithPrimaryHostOffline() throws Exception {
        givenUpdated("g1", "n1", "h1", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);
        givenConnected();

        whenPrimaryHostReportsState("h1", false, new Date().getTime());

        thenListenerNotNotifiedOnConnectionEstabilished();
        thenMessageNotSent("spBv1.0/g1/NBIRTH/n1");
    }

    @Test
    public void shouldSendNodeBirthWithPrimaryHostOnline() throws Exception {
        givenUpdated("g1", "n1", "h1", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);
        givenConnected();

        whenPrimaryHostReportsState("h1", true, new Date().getTime());

        thenListenerNotifiedOnConnectionEstabilished();
        thenMessageSent("spBv1.0/g1/NBIRTH/n1", 0, false);
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenUpdated(String groupId, String nodeId, String primaryHostId, String serverUris,
            String clientId, String username, int keepAlive, int connectionTimeoutSec) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(SparkplugDataTransportOptions.KEY_GROUP_ID, groupId);
        properties.put(SparkplugDataTransportOptions.KEY_NODE_ID, nodeId);
        properties.put(SparkplugDataTransportOptions.KEY_PRIMARY_HOST_APPLICATION_ID, primaryHostId);
        properties.put(SparkplugDataTransportOptions.KEY_SERVER_URIS, serverUris);
        properties.put(SparkplugDataTransportOptions.KEY_CLIENT_ID, clientId);
        properties.put(SparkplugDataTransportOptions.KEY_USERNAME, username);
        properties.put(SparkplugDataTransportOptions.KEY_KEEP_ALIVE, keepAlive);
        properties.put(SparkplugDataTransportOptions.KEY_CONNECTION_TIMEOUT, connectionTimeoutSec);

        sparkplugDataTransport.update(properties);
    }

    private void givenConnected() throws KuraException, MqttException {
        sparkplugDataTransport.connect();
    }

    /*
     * When
     */

    private void whenConnect() throws KuraException, MqttException {
        givenConnected();
    }

    private void whenPrimaryHostReportsState(String hostId, boolean isOnline, long timestamp) throws MqttException {
        JsonObject rootObject = new JsonObject();
        rootObject.addProperty("online", isOnline);
        rootObject.addProperty("timestamp", timestamp);
        
        Gson gson = new Gson();

        String topic = SparkplugTopics.getStateTopic(hostId);
        byte[] payload = gson.toJson(rootObject).getBytes();

        client.publish(topic, payload, 1, true);
    }

    /*
     * Then
     */

    private <E extends Exception> void thenExceptionOccurred(Class<E> expectedException) {
        assertNotNull(this.occurredException);
        assertEquals(expectedException.getName(), this.occurredException.getClass().getName());
    }

    private void thenListenerNotifiedOnConnectionEstabilished() {
        verify(this.listener, timeout(DEFAULT_TIMEOUT_MS).times(1)).onConnectionEstablished(true);
    }

    private void thenListenerNotNotifiedOnConnectionEstabilished() {
        verify(this.listener, timeout(DEFAULT_TIMEOUT_MS).times(0)).onConnectionEstablished(true);
    }

    private void thenMessageSent(String expectedTopic, int expectedQos, boolean expectedRetained) throws Exception {
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);

        verify(this.callback, timeout(DEFAULT_TIMEOUT_MS).times(1)).messageArrived(topicCaptor.capture(),
                messageCaptor.capture());

        String actualTopic = topicCaptor.getValue();
        MqttMessage actualMessage = messageCaptor.getValue();

        assertEquals(expectedTopic, actualTopic);
        assertEquals(expectedQos, actualMessage.getQos());
        assertEquals(expectedRetained, actualMessage.isRetained());
    }

    private void thenMessageNotSent(String expectedTopic) throws Exception {
        verify(this.callback, timeout(DEFAULT_TIMEOUT_MS).times(0)).messageArrived(eq(expectedTopic),
                any(MqttMessage.class));
    }

}
