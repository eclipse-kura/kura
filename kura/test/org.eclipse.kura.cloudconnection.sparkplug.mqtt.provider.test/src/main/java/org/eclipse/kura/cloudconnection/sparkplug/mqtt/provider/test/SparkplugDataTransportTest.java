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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugBProtobufPayloadBuilder;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugPayloads;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugTopics;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransportOptions;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.tahu.protobuf.SparkplugBProto.DataType;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload.Metric;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Payloads are already tested in unit tests, not verifying them here
 *
 */
public class SparkplugDataTransportTest extends SparkplugIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(SparkplugDataTransportTest.class);
    private static final long DEFAULT_TIMEOUT_MS = 10_000L;

    private DataTransportListener listener = mock(DataTransportListener.class);
    private MqttCallback callback = mock(MqttCallback.class);

    @Before
    public void setup() throws Exception {
        sparkplugDataTransport.addDataTransportListener(this.listener);
        client.setCallback(callback);
        client.subscribe("spBv1.0/g1/NBIRTH/n1", 0);
        client.subscribe("spBv1.0/g1/NDEATH/n1", 1);
    }

    @After
    public void cleanup() throws MqttException {
        sparkplugDataTransport.removeDataTransportListener(this.listener);
        client.unsubscribe("spBv1.0/g1/NBIRTH/n1");
        client.unsubscribe("spBv1.0/g1/NDEATH/n1");
        sparkplugDataTransport.disconnect(0L);
    }

    /*
     * Scenarios
     */

    @Test
    public void shouldSendNodeBirthWithoutPrimaryHost() throws Exception {
        givenUpdated("g1", "n1", "", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);

        whenConnect();

        thenListenerNotifiedOnConnectionEstabilished(1);
        thenMessageDeliveredOnce("spBv1.0/g1/NBIRTH/n1", 0, false, 0L);
    }

    @Test
    public void shouldNotSendNodeBirthWithPrimaryHostOffline() throws Exception {
        givenUpdated("g1", "n1", "h1", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);
        givenConnected();

        whenPrimaryHostReportsState("h1", false, new Date().getTime());

        thenListenerNotNotifiedOnConnectionEstabilished();
    }

    @Test
    public void shouldSendNodeBirthWithPrimaryHostOnline() throws Exception {
        givenUpdated("g1", "n1", "h1", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);
        givenConnected();

        whenPrimaryHostReportsState("h1", true, new Date().getTime());

        thenListenerNotifiedOnConnectionEstabilished(1);
        thenMessageDeliveredOnce("spBv1.0/g1/NBIRTH/n1", 0, false, 0L);
    }

    @Test
    public void shouldDisconnectCleanWhenPrimaryHostOffline() throws Exception {
        givenUpdated("g1", "n1", "h1", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);
        givenConnected();
        givenPrimaryHostReportsState("h1", true, new Date().getTime());

        whenPrimaryHostReportsState("h1", false, new Date().getTime());

        thenListenerNotifiedOnDisconnecting();
        thenListenerNotifiedOnDisconnected();
        thenMessageDeliveredOnce("spBv1.0/g1/NDEATH/n1", 0, false, 0L);
    }

    @Test
    public void shouldIgnoreStateMessagesWithOutdatedTimestamp() throws Exception {
        givenUpdated("g1", "n1", "h1", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);
        givenConnected();
        givenPrimaryHostReportsState("h1", true, new Date().getTime());

        whenPrimaryHostReportsState("h1", false, 1234L);

        thenListenerNotNotifiedOnDisconnecting();
        thenListenerNotNotifiedOnDisconnected();
    }

    @Test
    public void shouldIgnoreStateMessagesWithoutPrimaryHost() throws Exception {
        givenUpdated("g1", "n1", "", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);
        givenConnected();

        whenPrimaryHostReportsState("h1", false, new Date().getTime());

        thenListenerNotNotifiedOnDisconnecting();
        thenListenerNotNotifiedOnDisconnected();
    }

    @Test
    public void shouldIgnoreStateMessagesFromOtherPrimaryHost() throws Exception {
        givenUpdated("g1", "n1", "h1", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);
        givenConnected();
        givenPrimaryHostReportsState("h1", true, new Date().getTime());

        whenPrimaryHostReportsState("h2", false, new Date().getTime());

        thenListenerNotNotifiedOnDisconnecting();
        thenListenerNotNotifiedOnDisconnected();
    }

    @Test
    public void shouldDisconnectCleanWithDeathCertificate() throws Exception {
        givenUpdated("g1", "n1", "", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);
        givenConnected();

        whenDisconnect(0L);

        thenListenerNotifiedOnDisconnecting();
        thenListenerNotifiedOnDisconnected();
        thenMessageDeliveredOnce("spBv1.0/g1/NDEATH/n1", 0, false, 0L);
    }

    @Test
    public void shouldRestabilishSessionWhenRebirthRequestArrives() throws Exception {
        givenUpdated("g1", "n1", "", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);
        givenConnected();

        whenPrimaryHostRequestsRebirth("g1", "n1", true, new Date().getTime());

        thenListenerNotifiedOnDisconnecting();
        thenListenerNotifiedOnDisconnected();
        thenListenerNotifiedOnConnectionEstabilished(2);
        thenMessageDeliveredOnce("spBv1.0/g1/NDEATH/n1", 0, false, 0L);
        thenMessageDeliveredTwice("spBv1.0/g1/NBIRTH/n1", 0, false, 0L);
    }

    @Test
    public void shouldIgnoreRebirthWhenDifferentNode() throws Exception {
        givenUpdated("g1", "n1", "", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);
        givenConnected();

        whenPrimaryHostRequestsRebirth("g1", "n2", true, new Date().getTime());

        thenListenerNotifiedOnConnectionEstabilished(1);
        thenListenerNotNotifiedOnDisconnecting();
        thenListenerNotNotifiedOnDisconnected();
    }

    @Test
    public void shouldIgnoreRebirthWhenDifferentGroup() throws Exception {
        givenUpdated("g1", "n1", "", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);
        givenConnected();

        whenPrimaryHostRequestsRebirth("g2", "n1", true, new Date().getTime());

        thenListenerNotifiedOnConnectionEstabilished(1);
        thenListenerNotNotifiedOnDisconnecting();
        thenListenerNotNotifiedOnDisconnected();
    }

    @Test
    public void shouldIgnoreRebirthWhenMetricIsFalse() throws Exception {
        givenUpdated("g1", "n1", "", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);
        givenConnected();

        whenPrimaryHostRequestsRebirth("g1", "n1", false, new Date().getTime());

        thenListenerNotifiedOnConnectionEstabilished(1);
        thenListenerNotNotifiedOnDisconnecting();
        thenListenerNotNotifiedOnDisconnected();
    }

    @Test
    public void shouldForwardSTATEandNCMDmessagesToListeners() throws Exception {
        givenUpdated("g1", "n1", "", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);
        givenConnected();

        whenPrimaryHostReportsState("h1", false, new Date().getTime());
        whenPrimaryHostRequestsRebirth("g1", "n1", false, new Date().getTime());

        thenListenerNotifiedOnMessageArrived("spBv1.0/g1/NCMD/n1");
        thenListenerNotifiedOnMessageArrived("spBv1.0/STATE/h1");
    }

    @Test
    public void shouldIncrementBdSeqOnSuccessfulReconnection() throws Exception {
        givenUpdated("g1", "n1", "", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);
        givenConnected();
        givenDisconnect(0L);

        whenConnect();

        thenMessageDeliveredOnce("spBv1.0/g1/NBIRTH/n1", 0, false, 0L);
        thenMessageDeliveredOnce("spBv1.0/g1/NDEATH/n1", 0, false, 0L);
        thenMessageDeliveredOnce("spBv1.0/g1/NBIRTH/n1", 0, false, 1L);
    }

    @Test
    public void shouldNotIncrementBdSeqOnUnsuccessfulConnection() throws Exception {
        givenUpdated("g1", "n1", "", "tcp://wrong.url:1883", "test.device", "mqtt", 60, 30);
        givenConnectedAdmitToFail();
        givenUpdated("g1", "n1", "", "tcp://localhost:1883", "test.device", "mqtt", 60, 30);

        whenConnect();

        thenMessageDeliveredOnce("spBv1.0/g1/NBIRTH/n1", 0, false, 0L);
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

    private void givenConnectedAdmitToFail() {
        try {
            givenConnected();
        } catch (Exception e) {
        }
    }

    private void givenPrimaryHostReportsState(String hostId, boolean isOnline, long timestamp) throws MqttException {
        JsonObject rootObject = new JsonObject();
        rootObject.addProperty("online", isOnline);
        rootObject.addProperty("timestamp", timestamp);

        Gson gson = new Gson();

        String topic = SparkplugTopics.getStateTopic(hostId);
        byte[] payload = gson.toJson(rootObject).getBytes();

        logger.info("Sending STATE message [ online: {}, timestamp: {} ]", isOnline, timestamp);

        // for testing, do not publish with retain true
        client.publish(topic, payload, 1, false);
    }

    private void givenDisconnect(long quiesceTimeout) {
        sparkplugDataTransport.disconnect(quiesceTimeout);
    }

    /*
     * When
     */

    private void whenConnect() throws KuraException, MqttException {
        givenConnected();
    }

    private void whenPrimaryHostReportsState(String hostId, boolean isOnline, long timestamp) throws MqttException {
        givenPrimaryHostReportsState(hostId, isOnline, timestamp);
    }

    private void whenDisconnect(long quiesceTimeout) {
        givenDisconnect(quiesceTimeout);
    }

    private void whenPrimaryHostRequestsRebirth(String groupId, String nodeId, boolean isRebirthRequested,
            long timestamp) throws MqttException {
        SparkplugBProtobufPayloadBuilder payloadBuilder = new SparkplugBProtobufPayloadBuilder();
        payloadBuilder.withMetric(SparkplugPayloads.NODE_CONTROL_REBIRTH_METRIC_NAME, isRebirthRequested,
                DataType.Boolean,
                timestamp);
        payloadBuilder.withTimestamp(timestamp);

        client.publish(SparkplugTopics.getNodeCommandTopic(groupId, nodeId), payloadBuilder.build(), 0, false);
    }

    /*
     * Then
     */

    private void thenListenerNotifiedOnConnectionEstabilished(int expectedTimes) {
        verify(this.listener, timeout(DEFAULT_TIMEOUT_MS).times(expectedTimes)).onConnectionEstablished(true);
    }

    private void thenListenerNotNotifiedOnConnectionEstabilished() {
        verify(this.listener, never()).onConnectionEstablished(true);
    }

    private void thenListenerNotifiedOnDisconnecting() {
        verify(this.listener, timeout(DEFAULT_TIMEOUT_MS).times(1)).onDisconnecting();
    }

    private void thenListenerNotNotifiedOnDisconnecting() {
        verify(this.listener, never()).onDisconnecting();
    }

    private void thenListenerNotifiedOnDisconnected() {
        verify(this.listener, timeout(DEFAULT_TIMEOUT_MS).times(1)).onDisconnected();
    }

    private void thenListenerNotNotifiedOnDisconnected() {
        verify(this.listener, never()).onDisconnected();
    }

    private void thenListenerNotifiedOnMessageArrived(String topic) {
        verify(this.listener, timeout(DEFAULT_TIMEOUT_MS).times(1)).onMessageArrived(eq(topic),
                any(byte[].class), anyInt(), anyBoolean());
    }

    private void thenMessageDeliveredOnce(String expectedTopic, int expectedQos, boolean expectedRetained,
            long expectedBdSeq) throws Exception {
        thenMessageDelivered(expectedTopic, expectedQos, expectedRetained, expectedBdSeq, 1);
    }

    private void thenMessageDeliveredTwice(String expectedTopic, int expectedQos, boolean expectedRetained,
            long expectedBdSeq) throws Exception {
        thenMessageDelivered(expectedTopic, expectedQos, expectedRetained, expectedBdSeq, 2);
    }

    private void thenMessageDelivered(String expectedTopic, int expectedQos, boolean expectedRetained,
            long expectedBdSeq, int expectedTimes) throws Exception {
        verify(this.callback, timeout(DEFAULT_TIMEOUT_MS).times(expectedTimes)).messageArrived(eq(expectedTopic),
                argThat((MqttMessage message) -> isMessageMatching(message, expectedQos, expectedRetained,
                        expectedBdSeq)));
    }

    private static boolean isMessageMatching(MqttMessage message, int expectedQos, boolean expectedRetained,
            long expectedBdSeq) {
        if (message.getQos() != expectedQos) {
            return false;
        }

        if (message.isRetained() != expectedRetained) {
            return false;
        }

        try {
            Payload receivedPayload = Payload.parseFrom(message.getPayload());

            Optional<Metric> bdSeqMetric = receivedPayload.getMetricsList().stream()
                    .filter(metric -> metric.getName().equals(SparkplugBProtobufPayloadBuilder.BDSEQ_METRIC_NAME))
                    .findFirst();

            if (!bdSeqMetric.isPresent()) {
                return false;
            }

            if (bdSeqMetric.get().getLongValue() != expectedBdSeq) {
                return false;
            }

            return true;
        } catch (InvalidProtocolBufferException e) {
            return false;
        }
    }

}
