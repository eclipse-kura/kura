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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraDisconnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.device.SparkplugDevice;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransportOptions;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload.Metric;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

public class SparkplugDeviceTest extends SparkplugIntegrationTest {

    private static final long DEFAULT_TIMEOUT_MS = 10_000L;

    private DataTransportListener listener = mock(DataTransportListener.class);
    private MqttCallback callback = mock(MqttCallback.class);

    private Map<String, Object> kuraMetricsToPublish = new HashMap<>();
    private Date kuraTimestampToPublish;
    private byte[] kuraBodyToPublish;
    private KuraPosition kuraPositionToPublish;
    private Date kuraPositionDate;

    @Before
    public void setup() throws Exception {
        sparkplugDataTransport.addDataTransportListener(this.listener);
        client.setCallback(callback);
        client.subscribe("spBv1.0/g1/NBIRTH/n1", 0);
        client.subscribe("spBv1.0/g1/DBIRTH/n1/d1", 0);
        client.subscribe("spBv1.0/g1/DDATA/n1/d1", 0);
        setupDataTransportService("g1", "n1", "");
    }

    @After
    public void cleanup() throws MqttException {
        sparkplugDataTransport.removeDataTransportListener(this.listener);
        client.unsubscribe("spBv1.0/g1/NBIRTH/n1");
        client.unsubscribe("spBv1.0/g1/DBIRTH/n1/d1");
        client.unsubscribe("spBv1.0/g1/DDATA/n1/d1");
        sparkplugDataTransport.disconnect(0L);
    }

    /*
     * Scenarios
     */

    @Test
    public void shouldPublishDeviceBirth() throws Exception {
        givenUpdate("d1");
        givenEndpointConnect();
        givenKuraMetricToPublish("metric.string", "test string");

        whenPublish();

        thenMessageDelivered("spBv1.0/g1/NBIRTH/n1", 0, false, 0L);
        thenMessageDelivered("spBv1.0/g1/DBIRTH/n1/d1", 0, false, 1L);
    }

    @Test
    public void shouldPublishDeviceDataMessageOnUnchangedMetrics() throws Exception {
        givenUpdate("d1");
        givenEndpointConnect();
        givenKuraMetricToPublish("metric.string", "test string");
        givenPublish();

        whenPublish();

        thenMessageDelivered("spBv1.0/g1/NBIRTH/n1", 0, false, 0L);
        thenMessageDelivered("spBv1.0/g1/DBIRTH/n1/d1", 0, false, 1L);
        thenMessageDelivered("spBv1.0/g1/DDATA/n1/d1", 0, false, 2L);
    }

    @Test
    public void shouldRepublishDeviceBirthOnChangedMetrics() throws Exception {
        givenUpdate("d1");
        givenEndpointConnect();
        givenKuraMetricToPublish("metric.string", "test string");
        givenPublish();
        givenKuraMetricToPublish("metric.int", 12);

        whenPublish();

        thenMessageDelivered("spBv1.0/g1/NBIRTH/n1", 0, false, 0L);
        thenMessageDelivered("spBv1.0/g1/DBIRTH/n1/d1", 0, false, 1L);
    }

    @Test
    public void shouldRepublishDeviceBirthOnReconnection() throws Exception {
        givenUpdate("d1");
        givenKuraMetricToPublish("metric.string", "test string");
        givenEndpointConnect();
        givenPublish();
        givenReconnect();

        whenPublish();

        thenMessageDelivered("spBv1.0/g1/NBIRTH/n1", 0, false, 0L);
        thenMessageDelivered("spBv1.0/g1/DBIRTH/n1/d1", 0, false, 1L);
        thenMessageDelivered("spBv1.0/g1/NBIRTH/n1", 0, false, 0L);
        thenMessageDelivered("spBv1.0/g1/DBIRTH/n1/d1", 0, false, 1L);
    }

    @Test
    public void shouldPublishTimestamp() throws Exception {
        givenUpdate("d1");
        givenEndpointConnect();
        givenKuraTimestampToPublish(new Date());

        whenPublish();

        thenDeliveredMessageContainsTimestamp("spBv1.0/g1/DBIRTH/n1/d1", this.kuraTimestampToPublish.getTime());
    }

    @Test
    public void shouldPublishBody() throws Exception {
        givenUpdate("d1");
        givenEndpointConnect();
        givenKuraBodyToPublish("some random data".getBytes());

        whenPublish();

        thenDeliveredMessageContainsBody("spBv1.0/g1/DBIRTH/n1/d1", "some random data".getBytes());
    }

    @Test
    public void shouldFlattenKuraPositionIntoMetrics() throws Exception {
        givenUpdate("d1");
        givenEndpointConnect();
        givenKuraPositionToPublish(699.3, 200.0, 250.5, 349.0, 89.98, 12, 30.2, 1, new Date());

        whenPublish();

        thenDeliveredMessageContainsKuraPositionMetrics("spBv1.0/g1/DBIRTH/n1/d1", 699.3, 200.0, 250.5, 349.0, 89.98,
                12, 30.2, 1, this.kuraPositionDate.getTime());
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenUpdate(String deviceId) {
        final Map<String, Object> properties = new HashMap<>();
        properties.put(SparkplugDevice.KEY_DEVICE_ID, deviceId);
        sparkplugDevice.update(properties);
    }

    private void givenEndpointConnect() throws KuraConnectException {
        sparkplugCloudEndpoint.connect();
    }

    private void givenKuraMetricToPublish(String key, Object value) {
        this.kuraMetricsToPublish.put(key, value);
    }

    private void givenKuraTimestampToPublish(Date timestamp) {
        this.kuraTimestampToPublish = timestamp;
    }

    private void givenKuraBodyToPublish(byte[] body) {
        this.kuraBodyToPublish = body;
    }

    private void givenKuraPositionToPublish(double altitude, double heading, double latitude, double longitude,
            double precision, int satellites, double speed, int status, Date timestamp) {
        this.kuraPositionToPublish = new KuraPosition();
        this.kuraPositionToPublish.setAltitude(altitude);
        this.kuraPositionToPublish.setHeading(heading);
        this.kuraPositionToPublish.setLatitude(latitude);
        this.kuraPositionToPublish.setLongitude(longitude);
        this.kuraPositionToPublish.setPrecision(precision);
        this.kuraPositionToPublish.setSatellites(satellites);
        this.kuraPositionToPublish.setSpeed(speed);
        this.kuraPositionToPublish.setStatus(status);
        this.kuraPositionToPublish.setTimestamp(timestamp);
        this.kuraPositionDate = timestamp;
    }

    private void givenPublish() throws KuraException {
        KuraPayload payload = new KuraPayload();

        this.kuraMetricsToPublish.forEach(payload::addMetric);

        payload.setTimestamp(this.kuraTimestampToPublish);
        payload.setBody(this.kuraBodyToPublish);
        payload.setPosition(this.kuraPositionToPublish);

        sparkplugDevice.publish(new KuraMessage(payload, new HashMap<>()));
    }

    private void givenReconnect() throws KuraConnectException, KuraDisconnectException {
        sparkplugCloudEndpoint.disconnect();
        sparkplugCloudEndpoint.connect();
    }

    /*
     * When
     */

    private void whenPublish() throws KuraException {
        givenPublish();
    }

    /*
     * Then
     */

    private void thenMessageDelivered(String expectedTopic, int expectedQos, boolean expectedRetained,
            long expectedSeq) throws Exception {
        verifyMessageDeliveredWithMatcher(expectedTopic, (MqttMessage message) -> {
            try {
                Payload receivedPayload = Payload.parseFrom(message.getPayload());

                boolean matches = message.getQos() == expectedQos;
                matches &= message.isRetained() == expectedRetained;
                matches &= receivedPayload.getSeq() == expectedSeq;

                return matches;
            } catch (Exception e) {
                return false;
            }
        });
    }

    private void thenDeliveredMessageContainsTimestamp(String expectedTopic, long expectedTimestamp) throws Exception {
        verifyMessageDeliveredWithMatcher(expectedTopic, (MqttMessage message) -> {
            try {
                Payload receivedPayload = Payload.parseFrom(message.getPayload());

                return expectedTimestamp == receivedPayload.getTimestamp();
            } catch (Exception e) {
                return false;
            }
        });
    }

    private void thenDeliveredMessageContainsBody(String expectedTopic, byte[] expectedBody) throws Exception {
        verifyMessageDeliveredWithMatcher(expectedTopic, (MqttMessage message) -> {
            try {
                Payload receivedPayload = Payload.parseFrom(message.getPayload());
                return Arrays.equals(expectedBody, receivedPayload.getBody().toByteArray());
            } catch (Exception e) {
                return false;
            }
        });
    }

    private void thenDeliveredMessageContainsKuraPositionMetrics(String expectedTopic, double expectedAltitude, double expectedHeading, double expectedLatitude, double expectedLongitude,
            double expectedPrecision, int expectedSatellites, double expectedSpeed, int expectedStatus,
            long expectedTimestamp) throws Exception {
        verifyMessageDeliveredWithMatcher(expectedTopic, (MqttMessage message) -> {
            try {
                Payload receivedPayload = Payload.parseFrom(message.getPayload());
                List<Metric> metrics = receivedPayload.getMetricsList();

                Optional<Metric> altitude = metrics.stream()
                        .filter(metric -> metric.getName().equals("kura.position.altitude")).findFirst();
                Optional<Metric> heading = metrics.stream()
                        .filter(metric -> metric.getName().equals("kura.position.heading")).findFirst();
                Optional<Metric> latitude = metrics.stream()
                        .filter(metric -> metric.getName().equals("kura.position.latitude")).findFirst();
                Optional<Metric> longitude = metrics.stream()
                        .filter(metric -> metric.getName().equals("kura.position.longitude")).findFirst();
                Optional<Metric> precision = metrics.stream()
                        .filter(metric -> metric.getName().equals("kura.position.precision")).findFirst();
                Optional<Metric> satellites = metrics.stream()
                        .filter(metric -> metric.getName().equals("kura.position.satellites")).findFirst();
                Optional<Metric> speed = metrics.stream()
                        .filter(metric -> metric.getName().equals("kura.position.speed")).findFirst();
                Optional<Metric> status = metrics.stream()
                        .filter(metric -> metric.getName().equals("kura.position.status")).findFirst();
                Optional<Metric> timestamp = metrics.stream()
                        .filter(metric -> metric.getName().equals("kura.position.timestamp")).findFirst();

                if (!altitude.isPresent() || !heading.isPresent() || !latitude.isPresent() || !longitude.isPresent()
                        || !precision.isPresent() || !satellites.isPresent() || !speed.isPresent()
                        || !status.isPresent() || !timestamp.isPresent()) {
                    return false;
                }
                
                boolean matches = altitude.get().getDoubleValue() == expectedAltitude;
                matches &= heading.get().getDoubleValue() == expectedHeading;
                matches &= latitude.get().getDoubleValue() == expectedLatitude;
                matches &= longitude.get().getDoubleValue() == expectedLongitude;
                matches &= precision.get().getDoubleValue() == expectedPrecision;
                matches &= satellites.get().getIntValue() == expectedSatellites;
                matches &= speed.get().getDoubleValue() == expectedSpeed;
                matches &= status.get().getIntValue() == expectedStatus;
                matches &= timestamp.get().getLongValue() == expectedTimestamp;

                return matches;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /*
     * Utils
     */

    private void setupDataTransportService(String groupId, String nodeId, String primaryHostId) {
        final Map<String, Object> properties = new HashMap<>();
        properties.put(SparkplugDataTransportOptions.KEY_GROUP_ID, groupId);
        properties.put(SparkplugDataTransportOptions.KEY_NODE_ID, nodeId);
        properties.put(SparkplugDataTransportOptions.KEY_PRIMARY_HOST_APPLICATION_ID, primaryHostId);
        properties.put(SparkplugDataTransportOptions.KEY_SERVER_URIS, "tcp://localhost:1883");
        properties.put(SparkplugDataTransportOptions.KEY_CLIENT_ID, "test.device");
        properties.put(SparkplugDataTransportOptions.KEY_USERNAME, "mqtt");
        properties.put(SparkplugDataTransportOptions.KEY_KEEP_ALIVE, 60);
        properties.put(SparkplugDataTransportOptions.KEY_CONNECTION_TIMEOUT, 30);

        sparkplugDataTransport.update(properties);
    }

    private void verifyMessageDeliveredWithMatcher(String expectedTopic, ArgumentMatcher<MqttMessage> matcher)
            throws Exception {
        verify(this.callback, timeout(DEFAULT_TIMEOUT_MS).atLeastOnce()).messageArrived(eq(expectedTopic),
                argThat(matcher));
    }

}
