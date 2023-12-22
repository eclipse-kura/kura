/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraNotConnectedException;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransportOptions;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Before;
import org.junit.Test;

public class SparkplugDataTransportTest {

    private MqttAsyncClient client;
    private SparkplugDataTransport transport = new SparkplugDataTransport();
    private DataTransportListener listener = mock(DataTransportListener.class);
    private Exception occurredException;
    private String returnedString;
    private DataTransportToken deliveryToken;

    /*
     * Scenarios
     */

    // update

    @Test
    public void shouldNotifyListenersOnUpdate() throws Exception {
        givenDataTransportDataListener(this.listener);
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(false).build());

        whenUpdate(getDefaultProperties("test-client", "user"));

        thenListenerNotifiedOnConfigurationUpdating(1, false);
        thenListenerNotifiedOnConfigurationUpdated(1, false);
    }

    @Test
    public void shouldNotifyListenersOnUpdateWasConnected() throws Exception {
        givenDataTransportDataListener(this.listener);
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(true).build());
        givenInitSparkplugParameters("g1", "h1", 1L);

        whenUpdate(getDefaultProperties("test-client", "user"));

        thenListenerNotifiedOnConfigurationUpdating(1, true);
        thenListenerNotifiedOnConfigurationUpdated(1, true);
    }

    @Test
    public void shouldNotifyListenersOnFailingUpdate() throws Exception {
        givenDataTransportDataListener(this.listener);
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(false).build());

        whenUpdate(getDefaultProperties("test-client", "user"));

        thenListenerNotifiedOnConfigurationUpdating(1, false);
        thenListenerNotifiedOnConfigurationUpdated(1, false);
    }

    // disconnect

    @Test
    public void shouldDisconnectIfConnected() throws Exception {
        givenDataTransportDataListener(this.listener);
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(true).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenDisconnect(100);

        thenClientDisconnects(1, 100);
        thenListenerNotifiedOnDisconnecting(1);
        thenListenerNotifiedOnDisconnected(1);
    }

    @Test
    public void shouldNotDisconnectIfNotConnected() throws Exception {
        givenDataTransportDataListener(this.listener);
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(false).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenDisconnect(100);

        thenClientDisconnects(0, 100);
        thenListenerNotifiedOnDisconnecting(1);
        thenListenerNotifiedOnDisconnected(1);
    }

    @Test
    public void shouldNotThrowExceptionsOnDisconnectFailure() throws Exception {
        givenDataTransportDataListener(this.listener);
        givenMqttClient(new MockClientBuilder().asFailingWithTimeout().isConnected(true).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenDisconnect(100);

        thenListenerNotifiedOnDisconnecting(1);
        thenListenerNotifiedOnDisconnected(1);
    }

    // connect

    @Test
    public void shouldThrowIllegalStateExceptionWhenConnectOnAlreadyConnected() throws Exception {
        givenDataTransportDataListener(this.listener);
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(true).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenConnect();

        thenExceptionOccurred(IllegalStateException.class);
    }

    @Test
    public void shouldThrowIllegalStateExceptionWhenNotInitialized() throws Exception {
        givenDataTransportDataListener(this.listener);
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(true).build());

        whenConnect();

        thenExceptionOccurred(IllegalStateException.class);
        thenListenerNotifiedConnected(0, true);
        thenClientConnects(0);
    }

    @Test
    public void shouldConnect() throws Exception {
        givenDataTransportDataListener(this.listener);
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(false).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenConnect();

        thenListenerNotifiedConnected(1, true);
        thenClientConnects(1);
    }

    @Test
    public void shouldThrowKuraConnectExceptionOnConnectFailure() throws Exception {
        givenDataTransportDataListener(this.listener);
        givenMqttClient(new MockClientBuilder().asFailingWithTimeout().isConnected(false).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenConnect();

        thenListenerNotifiedConnected(0, true);
        thenExceptionOccurred(KuraConnectException.class);
    }

    // getters

    @Test
    public void shouldReturnCurrentBrokerUrl() throws Exception {
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(true).withCurrentServerURI("current")
                        .withServerURI("broker").build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenGetBrokerUrl();

        thenReturnedStringEquals("current");
    }

    @Test
    public void shouldReturnBrokerUrl() throws Exception {
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(false).withCurrentServerURI("current")
                .withServerURI("broker").build());
        givenInitSparkplugParameters("g1", "h1", 0L);
        
        whenGetBrokerUrl();
        
        thenReturnedStringEquals("broker");
    }

    @Test
    public void shouldReturnEmptyBrokerUrlIfNotInitialized() throws Exception {
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(false).withCurrentServerURI("current")
                .withServerURI("broker").build());

        whenGetBrokerUrl();

        thenReturnedStringEquals("");
    }

    @Test
    public void shouldReturnEmptyAccountName() throws Exception {
        whenGetAccountName();

        thenReturnedStringEquals("");
    }

    @Test
    public void shouldReturnCorrectUsername() throws Exception {
        givenActivated(getDefaultProperties("test-client", "user"));
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenGetUsername();

        thenReturnedStringEquals("user");
    }

    @Test
    public void shouldReturnEmptyUsernameIfNotInitialized() throws Exception {
        givenActivated(getDefaultProperties("test-client", "user"));

        whenGetUsername();

        thenReturnedStringEquals("");
    }

    @Test
    public void shouldReturnCorrectClientId() throws Exception {
        givenActivated(getDefaultProperties("test-client", "user"));
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenGetClientId();

        thenReturnedStringEquals("test-client");
    }

    @Test
    public void shouldReturnEmptyClientIdIfNotInitialized() throws Exception {
        givenActivated(getDefaultProperties("test-client", "user"));

        whenGetClientId();

        thenReturnedStringEquals("");
    }

    // subscribe

    @Test
    public void shouldThrowKuraNotConnectedExceptionOnSubscribeIfNotConnected() throws Exception {
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(false).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenSubscribe("topic1", 0);

        thenExceptionOccurred(KuraNotConnectedException.class);
    }

    @Test
    public void shouldThrowIllegalStateExceptionOnSubscribeIfNotInitialized() throws Exception {
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(true).build());

        whenSubscribe("topic1", 0);

        thenExceptionOccurred(IllegalStateException.class);
    }

    @Test
    public void shouldNotFailSubscribeIfClientFails() throws Exception {
        givenMqttClient(new MockClientBuilder().asFailingWithTimeout().isConnected(true).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenSubscribe("topic1", 0);

        thenClientSubscribed(1, "topic1", 0);
    }

    @Test
    public void shouldSubscribeCorrectly() throws Exception {
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(true).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenSubscribe("topic1", 0);

        thenClientSubscribed(1, "topic1", 0);
    }

    // unsubscribe

    @Test
    public void shouldThrowKuraNotConnectedExceptionOnUnsubscribeIfNotConnected() throws Exception {
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(false).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenUnsubscribe("topic1");

        thenExceptionOccurred(KuraNotConnectedException.class);
    }

    @Test
    public void shouldThrowIllegalStateExceptionOnUnsubscribeIfNotInitialized() throws Exception {
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(true).build());

        whenUnsubscribe("topic1");

        thenExceptionOccurred(IllegalStateException.class);
    }

    @Test
    public void shouldNotFailUnsubscribeIfClientFails() throws Exception {
        givenMqttClient(new MockClientBuilder().asFailingWithTimeout().isConnected(true).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenUnsubscribe("topic1");

        thenClientUnsubscribed(1, "topic1");
    }

    @Test
    public void shouldUnsubscribeCorrectly() throws Exception {
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(true).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenUnsubscribe("topic1");

        thenClientUnsubscribed(1, "topic1");
    }

    // publish

    @Test
    public void shouldThrowKuraNotConnectedExceptionOnPublishIfNotConnected() throws Exception {
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(false).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenPublish("topic", "data".getBytes(), 0, false);

        thenExceptionOccurred(KuraNotConnectedException.class);
    }

    @Test
    public void shouldThrowIllegalStateExceptionOnPublishIfNotInitialized() throws Exception {
        givenMqttClient(new MockClientBuilder().asFunctional().isConnected(true).build());

        whenPublish("topic", "data".getBytes(), 0, false);

        thenExceptionOccurred(IllegalStateException.class);
    }

    @Test
    public void shouldNotFailPublishIfClientFails() throws Exception {
        givenMqttClient(new MockClientBuilder().asFailingWithTimeout().isConnected(true).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenPublish("topic", "data".getBytes(), 0, false);

        thenClientPublished(1, "topic", "data".getBytes(), 0, false);
        thenRetunedNullDataTransportToken();
    }

    @Test
    public void shouldReturnNullTokenWhenPublishWithQos0() throws Exception {
        givenMqttClient(new MockClientBuilder().asFailingWithTimeout().isConnected(true).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenPublish("topic", "data".getBytes(), 0, false);

        thenClientPublished(1, "topic", "data".getBytes(), 0, false);
        thenRetunedNullDataTransportToken();
    }

    @Test
    public void shouldReturnNonNullTokenWhenPublishWithQos1() throws Exception {
        givenMqttClient(new MockClientBuilder().asFailingWithTimeout().isConnected(true).build());
        givenInitSparkplugParameters("g1", "h1", 0L);

        whenPublish("topic", "data".getBytes(), 1, false);

        thenClientPublished(1, "topic", "data".getBytes(), 1, false);
        thenReturnedValidDataTransportToken();
    }

    // MqttCallback

    @Test
    public void shouldNotifyListenersOnConnectionLost() {
        givenDataTransportDataListener(this.listener);

        whenOnConnectionLost();

        thenListenersNotifiedOnConnectionLost(1);
    }

    @Test
    public void shouldNotifyListenersOnMessageConfirmedWithQos1() throws Exception {
        givenDataTransportDataListener(this.listener);

        whenDeliveryComplete(1);

        thenListenersNotifiedOnMessageConfirmed(1);
    }

    @Test
    public void shouldNotNotifyListenersOnMessageConfirmedWithQos0() throws Exception {
        givenDataTransportDataListener(this.listener);

        whenDeliveryComplete(0);

        thenListenersNotifiedOnMessageConfirmed(0);
    }

    @Test
    public void shouldNotifyListenersOnMessageArrived() throws Exception {
        givenDataTransportDataListener(this.listener);

        whenMessageArrived("test", "data".getBytes(), 0, false);

        thenListenersNotifiedOnMessageArrived(1, "test", "data".getBytes(), 0, false);
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenMqttClient(MqttAsyncClient client) {
        this.client = client;
        this.transport.setMqttAsyncClient(client);
    }

    private void givenActivated(Map<String, Object> properties) {
        this.transport.activate(properties);
    }

    private void givenInitSparkplugParameters(String groupId, String hostId, long bdSeq) {
        this.transport.initSparkplugParameters(groupId, hostId, bdSeq);
    }

    private void givenDataTransportDataListener(DataTransportListener listener) {
        this.transport.addDataTransportListener(listener);
    }

    /*
     * When
     */

    private void whenUpdate(Map<String, Object> properties) {
        this.transport.update(properties);
    }

    private void whenDisconnect(long quiesceTimeout) {
        this.transport.disconnect(quiesceTimeout);
    }

    private void whenConnect() {
        try {
            this.transport.connect();
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenGetBrokerUrl() {
        this.returnedString = this.transport.getBrokerUrl();
    }

    private void whenGetAccountName() {
        this.returnedString = this.transport.getAccountName();
    }

    private void whenGetClientId() {
        this.returnedString = this.transport.getClientId();
    }

    private void whenGetUsername() {
        this.returnedString = this.transport.getUsername();
    }

    private void whenSubscribe(String topic, int qos) {
        try {
            this.transport.subscribe(topic, qos);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenUnsubscribe(String topic) {
        try {
            this.transport.unsubscribe(topic);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenPublish(String topic, byte[] payload, int qos, boolean retain) {
        try {
            this.deliveryToken = this.transport.publish(topic, payload, qos, retain);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenOnConnectionLost() {
        this.transport.connectionLost(new Throwable());
    }

    private void whenDeliveryComplete(int qos) throws MqttException {
        MqttMessage message = mock(MqttMessage.class);
        when(message.getQos()).thenReturn(qos);

        IMqttDeliveryToken token = mock(IMqttDeliveryToken.class);
        when(token.getMessage()).thenReturn(message);

        this.transport.deliveryComplete(token);
    }

    private void whenMessageArrived(String topic, byte[] payload, int qos, boolean retain) throws Exception {
        MqttMessage message = mock(MqttMessage.class);
        when(message.getPayload()).thenReturn(payload);
        when(message.getQos()).thenReturn(qos);
        when(message.isRetained()).thenReturn(retain);

        this.transport.messageArrived(topic, message);
    }

    /*
     * Then
     */

    private void thenListenerNotifiedOnConfigurationUpdating(int expectedTimes, boolean expectedWasConnected) {
        verify(this.listener, times(expectedTimes)).onConfigurationUpdating(expectedWasConnected);
    }

    private void thenListenerNotifiedOnConfigurationUpdated(int expectedTimes, boolean expectedWasConnected) {
        verify(this.listener, times(expectedTimes)).onConfigurationUpdated(expectedWasConnected);
    }

    private void thenListenerNotifiedOnDisconnecting(int expectedTimes) {
        verify(this.listener, times(expectedTimes)).onDisconnecting();
    }

    private void thenListenerNotifiedOnDisconnected(int expectedTimes) {
        verify(this.listener, times(expectedTimes)).onDisconnected();
    }

    private void thenListenerNotifiedConnected(int expectedTimes, boolean expectedNewSession) {
        verify(this.listener, times(expectedTimes)).onConnectionEstablished(expectedNewSession);
    }

    private void thenListenersNotifiedOnConnectionLost(int expectedTimes) {
        verify(this.listener, times(expectedTimes)).onConnectionLost(any(Throwable.class));
    }

    private void thenListenersNotifiedOnMessageConfirmed(int expectedTimes) {
        verify(this.listener, times(expectedTimes)).onMessageConfirmed(any(DataTransportToken.class));
    }

    private void thenListenersNotifiedOnMessageArrived(int expectedTimes, String expectedTopic, byte[] expectedPayload,
            int expectedQos, boolean expectedRetain) {
        verify(this.listener, times(expectedTimes)).onMessageArrived(expectedTopic, expectedPayload, expectedQos,
                expectedRetain);
    }

    private void thenClientDisconnects(int expectedTimes, long expectedQuiesceTimeout) throws MqttException {
        verify(this.client, times(expectedTimes)).disconnect(expectedQuiesceTimeout);
    }

    private void thenClientConnects(int expectedTimes) throws MqttException {
        verify(this.client, times(expectedTimes)).connect(any(MqttConnectOptions.class));
    }

    private void thenClientSubscribed(int expectedTimes, String expectedTopic, int expectedQos) throws MqttException {
        verify(this.client, times(expectedTimes)).subscribe(expectedTopic, expectedQos);
    }

    private void thenClientUnsubscribed(int expectedTimes, String expectedTopic) throws MqttException {
        verify(this.client, times(expectedTimes)).unsubscribe(expectedTopic);
    }

    private void thenClientPublished(int expectedTimes, String expectedTopic, byte[] expectedPayload, int expectedQos,
            boolean expectedRetain) throws MqttException {
        verify(this.client, times(expectedTimes)).publish(expectedTopic, expectedPayload, expectedQos, expectedRetain);
    }

    private void thenRetunedNullDataTransportToken() {
        assertNull(this.deliveryToken);
    }

    private void thenReturnedValidDataTransportToken() {
        assertNull(this.deliveryToken);
    }

    private <E extends Exception> void thenExceptionOccurred(Class<E> expectedException) {
        assertNotNull(this.occurredException);
        assertEquals(expectedException.getName(), this.occurredException.getClass().getName());
    }

    private void thenReturnedStringEquals(String expectedString) {
        assertEquals(expectedString, this.returnedString);
    }


    /*
     * Utilities
     */

    @Before
    public void activateComponent() {
        givenActivated(getDefaultProperties("test-client", null));
    }

    private Map<String, Object> getDefaultProperties(String clientId, String username) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(SparkplugDataTransportOptions.KEY_SERVER_URIS, "tcp://broker:1883");
        properties.put(SparkplugDataTransportOptions.KEY_CLIENT_ID, clientId);
        properties.put(SparkplugDataTransportOptions.KEY_KEEP_ALIVE, 100);
        properties.put(SparkplugDataTransportOptions.KEY_USERNAME, username);
        properties.put(SparkplugDataTransportOptions.KEY_PASSWORD, new Password("pass"));
        properties.put(SparkplugDataTransportOptions.KEY_CONNECTION_TIMEOUT, 100);

        return properties;
    }

    private class MockClientBuilder {

        private MqttAsyncClient client = mock(MqttAsyncClient.class);

        public MockClientBuilder isConnected(boolean isConnected) {
            when(this.client.isConnected()).thenReturn(isConnected);
            return this;
        }

        public MockClientBuilder asFunctional() throws MqttException {
            IMqttToken token = mock(IMqttToken.class);
            IMqttDeliveryToken deliveryToken = mock(IMqttDeliveryToken.class);
            when(deliveryToken.getMessageId()).thenReturn(0);

            when(this.client.connect(any(MqttConnectOptions.class))).thenReturn(token);
            when(this.client.disconnect(anyLong())).thenReturn(token);
            when(this.client.subscribe(anyString(), anyInt())).thenReturn(token);
            when(this.client.unsubscribe(anyString())).thenReturn(token);
            when(this.client.publish(anyString(), any(byte[].class), anyInt(), anyBoolean())).thenReturn(deliveryToken);
            return this;
        }

        public MockClientBuilder asFailingWithTimeout() throws MqttException {
            IMqttToken token = mock(IMqttToken.class);
            doThrow(new MqttException(0)).when(token).waitForCompletion(anyLong());

            when(this.client.connect(any(MqttConnectOptions.class))).thenReturn(token);
            when(this.client.disconnect(anyLong())).thenReturn(token);
            when(this.client.subscribe(anyString(), anyInt())).thenReturn(token);
            when(this.client.unsubscribe(anyString())).thenReturn(token);

            doThrow(new MqttException(0)).when(this.client).publish(anyString(), any(byte[].class), anyInt(),
                    anyBoolean());

            return this;
        }

        public MockClientBuilder withCurrentServerURI(String currentServerURI) {
            when(this.client.getCurrentServerURI()).thenReturn(currentServerURI);
            return this;
        }

        public MockClientBuilder withServerURI(String serverURI) {
            when(this.client.getServerURI()).thenReturn(serverURI);
            return this;
        }

        public MqttAsyncClient build() {
            return this.client;
        }

    }

}