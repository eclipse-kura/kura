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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.subscriber.test;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.subscriber.SparkplugSubscriber;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.tahu.protobuf.SparkplugBProto;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload.DataSet;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload.Metric;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload.Metric.MetricValueExtension;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload.Template;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.osgi.service.event.EventAdmin;

import com.google.protobuf.ByteString;

public class SparkplugSubscriberTest {

    private DataService dataService = mock(DataService.class);
    private SparkplugCloudEndpoint endpoint = new SparkplugCloudEndpoint();
    private SparkplugSubscriber sub1 = new SparkplugSubscriber();
    private SparkplugSubscriber sub2 = new SparkplugSubscriber();
    private SparkplugSubscriber sub3 = new SparkplugSubscriber();
    private SparkplugSubscriber sub4 = new SparkplugSubscriber();
    private CloudSubscriberListener subListener = mock(CloudSubscriberListener.class);
    private CloudConnectionListener cloudConnectionListener = mock(CloudConnectionListener.class);

    /*
     * Scenarios
     */

    @Test
    public void shouldForwardMessagesToCorrectSubscribers() throws Exception {
        givenInitialSetup(true);
        givenRegisterSubscriber("a/b/c", 0, this.sub1);
        givenRegisterSubscriber("a/+/c", 0, this.sub2);
        givenRegisterSubscriber("a/#", 0, this.sub3);
        givenRegisterSubscriber("a/b", 0, this.sub4);
        givenRegisterCloudSubscriberListener(this.sub1, this.subListener);
        givenRegisterCloudSubscriberListener(this.sub2, this.subListener);
        givenRegisterCloudSubscriberListener(this.sub3, this.subListener);
        givenRegisterCloudSubscriberListener(this.sub4, this.subListener);

        whenOnMessageArrivedWithAllSupportedMetrics("a/b/c", 0);

        thenSubscriberListenerNotifiedOnMessageArrived(this.subListener, 3);
    }

    @Test
    public void shouldNotForwardMessagesToUnsubscribed() throws Exception {
        givenInitialSetup(true);
        givenRegisterSubscriber("a/b/c", 0, this.sub1);
        givenRegisterSubscriber("a/+/c", 0, this.sub2);
        givenRegisterSubscriber("a/#", 0, this.sub3);
        givenRegisterSubscriber("a/b", 0, this.sub4);
        givenRegisterCloudSubscriberListener(this.sub1, this.subListener);
        givenRegisterCloudSubscriberListener(this.sub2, this.subListener);
        givenRegisterCloudSubscriberListener(this.sub3, this.subListener);
        givenRegisterCloudSubscriberListener(this.sub4, this.subListener);
        givenUnregisterSubscriber(this.sub1);
        givenUnregisterSubscriber(this.sub2);

        whenOnMessageArrivedWithAllSupportedMetrics("a/b/c", 0);

        thenSubscriberListenerNotifiedOnMessageArrived(this.subListener, 1);
    }

    @Test
    public void shouldSubscribeOnConnectionEstabilished() throws Exception {
        givenInitialSetup(false);
        givenRegisterSubscriber("a/b/c", 0, this.sub1);
        givenRegisterSubscriber("a/+/c", 0, this.sub2);
        givenRegisterSubscriber("a/#", 1, this.sub3);
        givenRegisterSubscriber("a/#", 0, this.sub4);

        whenEndpointEstablishConnection();

        thenSubscribed("a/b/c", 0);
        thenSubscribed("a/+/c", 0);
        thenSubscribed("a/#", 1);
        thenSubscribed("a/#", 0);
    }

    @Test
    public void shouldNotifyCloudConnectionListenerOnDisconnected() {
        givenCloudConnectionListener(this.sub1, this.cloudConnectionListener);
        
        whenOnDisconnected(this.sub1);
        
        thenConnectionListenerNotifiedOnDisconnected(this.cloudConnectionListener);
    }

    @Test
    public void shouldNotifyCloudConnectionListenerOnConnectionLost() {
        givenCloudConnectionListener(this.sub1, this.cloudConnectionListener);

        whenOnConnectionLost(this.sub1);

        thenConnectionListenerNotifiedOnConnectionLost(this.cloudConnectionListener);
    }

    @Test
    public void shouldNotifyCloudConnectionListenerOnConnectionEstablished() {
        givenCloudConnectionListener(this.sub1, this.cloudConnectionListener);

        whenOnConnectionEstablished(this.sub1);

        thenConnectionListenerNotifiedOnConnectionEstablished(this.cloudConnectionListener);
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenInitialSetup(boolean isConnected) {
        when(this.dataService.isConnected()).thenReturn(isConnected);
        this.endpoint.setDataService(this.dataService);
        
        EventAdmin mockEventAdmin = mock(EventAdmin.class);
        this.endpoint.setEventAdmin(mockEventAdmin);
    }

    private void givenRegisterSubscriber(String topicFilter, int qos, CloudSubscriberListener listener)
            throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(SparkplugSubscriber.KEY_TOPIC_FILTER, topicFilter);
        properties.put(SparkplugSubscriber.KEY_QOS, qos);

        this.endpoint.registerSubscriber(properties, listener);
    }

    private void givenUnregisterSubscriber(CloudSubscriberListener listener) {

        this.endpoint.unregisterSubscriber(listener);
    }

    private void givenRegisterCloudSubscriberListener(CloudSubscriber subscriber, CloudSubscriberListener listener)
            throws Exception {
        subscriber.registerCloudSubscriberListener(listener);
    }

    private void givenCloudConnectionListener(CloudSubscriber subscriber, CloudConnectionListener listener) {
        subscriber.registerCloudConnectionListener(listener);
    }

    /*
     * When
     */

    private void whenOnMessageArrivedWithAllSupportedMetrics(String topic, int qos) {
        SparkplugBProto.Payload.Builder builder = Payload.newBuilder();

        builder.addMetrics(getBooleanMetric("metric.boolean", true));
        builder.addMetrics(getBytesMetric("metric.bytes", "test".getBytes()));
        builder.addMetrics(getDatasetMetric("metric.dataset", DataSet.getDefaultInstance()));
        builder.addMetrics(getDoubleMetric("metric.double", 12.3));
        builder.addMetrics(getExtensionValueMetric("metric.extension", MetricValueExtension.getDefaultInstance()));
        builder.addMetrics(getFloatMetric("metric.float", 12f));
        builder.addMetrics(getIntegerMetric("metric.int", 11));
        builder.addMetrics(getLongMetric("metric.long", 123L));
        builder.addMetrics(getStringMetric("metric.string", "test"));
        builder.addMetrics(getTemplateMetric("metric.template", Template.getDefaultInstance()));

        builder.setBody(ByteString.copyFrom("example".getBytes()));
        builder.setTimestamp(1000L);
        builder.setSeq(100L);

        this.endpoint.onMessageArrived(topic, builder.build().toByteArray(), qos, false);
    }

    private void whenEndpointEstablishConnection() {
        when(this.dataService.isConnected()).thenReturn(true);
        this.endpoint.onConnectionEstablished();
    }

    private void whenOnDisconnected(SparkplugSubscriber subscriber) {
        subscriber.onDisconnected();
    }

    private void whenOnConnectionLost(SparkplugSubscriber subscriber) {
        subscriber.onConnectionLost();
    }

    private void whenOnConnectionEstablished(SparkplugSubscriber subscriber) {
        subscriber.onConnectionEstablished();
    }

    /*
     * Then
     */

    private void thenSubscriberListenerNotifiedOnMessageArrived(CloudSubscriberListener subscriberListener,
            int expectedTimes) {
        verify(subscriberListener, timeout(1000L).times(expectedTimes))
                .onMessageArrived(argThat(new ArgumentMatcher<KuraMessage>() {

            @Override
            public boolean matches(KuraMessage message) {
                KuraPayload payload = message.getPayload();
                
                boolean bodyMatches = Arrays.equals(payload.getBody(), "example".getBytes());
                boolean timestampMatches = payload.getTimestamp().getTime() == 1000L;
                boolean seqMatches = ((long) payload.getMetric("seq")) == 100L;
                
                return bodyMatches && timestampMatches && seqMatches;
            }

        }));
    }

    private void thenConnectionListenerNotifiedOnDisconnected(CloudConnectionListener subscriberListener) {
        verify(subscriberListener, timeout(1000L).times(1)).onDisconnected();
    }

    private void thenConnectionListenerNotifiedOnConnectionLost(CloudConnectionListener subscriberListener) {
        verify(subscriberListener, timeout(1000L).times(1)).onConnectionLost();
    }

    private void thenConnectionListenerNotifiedOnConnectionEstablished(CloudConnectionListener subscriberListener) {
        verify(subscriberListener, timeout(1000L).times(1)).onConnectionEstablished();
    }

    private void thenSubscribed(String expectedTopicFilter, int expectedQos) throws KuraException {
        verify(this.dataService, times(1)).subscribe(expectedTopicFilter, expectedQos);
    }



    /*
     * Utils
     */

    private Metric getBooleanMetric(String name, boolean value) {
        Payload.Metric.Builder metricBuilder = Payload.Metric.newBuilder();
        metricBuilder.setName(name);
        metricBuilder.setBooleanValue(value);
        return metricBuilder.build();
    }

    private Metric getBytesMetric(String name, byte[] value) {
        Payload.Metric.Builder metricBuilder = Payload.Metric.newBuilder();
        metricBuilder.setName(name);
        metricBuilder.setBytesValue(ByteString.copyFrom(value));
        return metricBuilder.build();
    }

    private Metric getDatasetMetric(String name, DataSet value) {
        Payload.Metric.Builder metricBuilder = Payload.Metric.newBuilder();
        metricBuilder.setName(name);
        metricBuilder.setDatasetValue(value);
        return metricBuilder.build();
    }

    private Metric getDoubleMetric(String name, double value) {
        Payload.Metric.Builder metricBuilder = Payload.Metric.newBuilder();
        metricBuilder.setName(name);
        metricBuilder.setDoubleValue(value);
        return metricBuilder.build();
    }

    private Metric getExtensionValueMetric(String name, MetricValueExtension value) {
        Payload.Metric.Builder metricBuilder = Payload.Metric.newBuilder();
        metricBuilder.setName(name);
        metricBuilder.setExtensionValue(value);
        return metricBuilder.build();
    }

    private Metric getFloatMetric(String name, float value) {
        Payload.Metric.Builder metricBuilder = Payload.Metric.newBuilder();
        metricBuilder.setName(name);
        metricBuilder.setFloatValue(value);
        return metricBuilder.build();
    }

    private Metric getIntegerMetric(String name, int value) {
        Payload.Metric.Builder metricBuilder = Payload.Metric.newBuilder();
        metricBuilder.setName(name);
        metricBuilder.setIntValue(value);
        return metricBuilder.build();
    }

    private Metric getLongMetric(String name, long value) {
        Payload.Metric.Builder metricBuilder = Payload.Metric.newBuilder();
        metricBuilder.setName(name);
        metricBuilder.setLongValue(value);
        return metricBuilder.build();
    }
    
    private Metric getStringMetric(String name, String value) {
        Payload.Metric.Builder metricBuilder = Payload.Metric.newBuilder();
        metricBuilder.setName(name);
        metricBuilder.setStringValue(value);
        return metricBuilder.build();
    }

    private Metric getTemplateMetric(String name, Template value) {
        Payload.Metric.Builder metricBuilder = Payload.Metric.newBuilder();
        metricBuilder.setName(name);
        metricBuilder.setTemplateValue(value);
        return metricBuilder.build();
    }

}
