/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.wire.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudPublisherTest {

    private static final Logger logger = LoggerFactory.getLogger(CloudPublisherTest.class);
    
    public interface FakeCloudPublisher extends org.eclipse.kura.cloudconnection.publisher.CloudPublisher {

        public KuraMessage getMessage();
    }


    @Test
    public void testOnWireReceive() throws InvalidSyntaxException, NoSuchFieldException, KuraException {
        // test publishing a normal message with topic replacement

        String topic = "my test topic";

        CloudPublisher cp = new CloudPublisher();

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        cp.bindWireHelperService(wireHelperServiceMock);

        BundleContext bundleCtxMock = mock(BundleContext.class);
        Filter filter = mock(Filter.class);
        when(bundleCtxMock.createFilter(anyString())).thenReturn(filter);

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(bundleCtxMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("CloudPublisher.target", "cspid");
        properties.put("publish.position", "none");

        cp.activate(ctxMock, properties);

        org.eclipse.kura.cloudconnection.publisher.CloudPublisher cloudPublisherMock = mock(
                org.eclipse.kura.cloudconnection.publisher.CloudPublisher.class);

        when(cloudPublisherMock.publish((KuraMessage) anyObject())).thenAnswer(invocation -> {
            KuraMessage message = invocation.getArgumentAt(1, KuraMessage.class);

            KuraPayload payload = message.getPayload();

            assertNull(payload.getBody());
            assertNull(payload.getPosition());
            assertNotNull(payload.getTimestamp());
            assertNotNull(payload.metrics());
            assertEquals(2, payload.metrics().size());
            assertEquals("val", payload.getMetric("key"));
            assertEquals(topic, payload.getMetric("topic"));

            return 1234;
        });

        TestUtil.setFieldValue(cp, "cloudConnectionPublisher", cloudPublisherMock);

        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<>();
        Map<String, TypedValue<?>> recordProps = new HashMap<>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("key", val);
        val = new StringValue(topic);
        recordProps.put("topic", val);
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);
        WireEnvelope wireEnvelope = new WireEnvelope(emitterPid, wireRecords);

        cp.onWireReceive(wireEnvelope);

        verify(cloudPublisherMock, times(1)).publish((KuraMessage) anyObject());
    }

    @Test
    public void testOnWireReceiveWithBasicPosition()
            throws InvalidSyntaxException, NoSuchFieldException, KuraException {
        // test publishing a normal message with topic replacement and position
        String positionType = "basic";
        String topic = "my test topic";

        CloudPublisher cp = new CloudPublisher();

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        cp.bindWireHelperService(wireHelperServiceMock);

        BundleContext bundleCtxMock = mock(BundleContext.class);
        Filter filter = mock(Filter.class);
        when(bundleCtxMock.createFilter(anyString())).thenReturn(filter);

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(bundleCtxMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("CloudPublisher.target", "cspid");
        properties.put("publish.position", positionType);

        cp.activate(ctxMock, properties);

        org.eclipse.kura.cloudconnection.publisher.CloudPublisher cloudPublisherMock = mock(
                org.eclipse.kura.cloudconnection.publisher.CloudPublisher.class);

        PositionService positionServiceMock = mock(PositionService.class);

        when(positionServiceMock.getNmeaPosition()).thenReturn(new NmeaPosition(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        when(cloudPublisherMock.publish((KuraMessage) anyObject())).thenAnswer(invocation -> {
            KuraMessage message = invocation.getArgumentAt(1, KuraMessage.class);

            KuraPayload payload = message.getPayload();
            KuraPosition position = payload.getPosition();

            assertNull(payload.getBody());
            assertNotNull(position);
            assertNotNull(position.getAltitude());
            assertNotNull(position.getLatitude());
            assertNotNull(position.getLongitude());
            assertNull(position.getSatellites());
            assertNotNull(payload.getTimestamp());
            assertNotNull(payload.metrics());
            assertEquals(2, payload.metrics().size());
            assertEquals("val", payload.getMetric("key"));
            assertEquals(topic, payload.getMetric("topic"));

            return 1234;
        });

        TestUtil.setFieldValue(cp, "cloudConnectionPublisher", cloudPublisherMock);
        TestUtil.setFieldValue(cp, "positionService", positionServiceMock);

        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<>();
        Map<String, TypedValue<?>> recordProps = new HashMap<>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("key", val);
        val = new StringValue(topic);
        recordProps.put("topic", val);
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);
        WireEnvelope wireEnvelope = new WireEnvelope(emitterPid, wireRecords);

        cp.onWireReceive(wireEnvelope);

        verify(cloudPublisherMock, times(1)).publish((KuraMessage) anyObject());
    }

    @Test
    public void testOnWireReceiveWithFullPosition() throws InvalidSyntaxException, NoSuchFieldException, KuraException {
        // test publishing a normal message with topic replacement and position

        String positionType = "full";
        String topic = "my test topic";

        CloudPublisher cp = new CloudPublisher();

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        cp.bindWireHelperService(wireHelperServiceMock);

        BundleContext bundleCtxMock = mock(BundleContext.class);
        Filter filter = mock(Filter.class);
        when(bundleCtxMock.createFilter(anyString())).thenReturn(filter);

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(bundleCtxMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("CloudPublisher.target", "cspid");
        properties.put("publish.position", positionType);

        cp.activate(ctxMock, properties);

        org.eclipse.kura.cloudconnection.publisher.CloudPublisher cloudPublisherMock = mock(
                org.eclipse.kura.cloudconnection.publisher.CloudPublisher.class);

        PositionService positionServiceMock = mock(PositionService.class);

        when(positionServiceMock.getNmeaPosition()).thenReturn(new NmeaPosition(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        when(cloudPublisherMock.publish((KuraMessage) anyObject())).thenAnswer(invocation -> {
            KuraMessage message = invocation.getArgumentAt(1, KuraMessage.class);

            KuraPayload payload = message.getPayload();
            KuraPosition position = payload.getPosition();

            assertNull(payload.getBody());
            assertNotNull(position);
            assertNotNull(position.getAltitude());
            assertNotNull(position.getLatitude());
            assertNotNull(position.getLongitude());
            assertNotNull(position.getHeading());
            assertNotNull(position.getPrecision());
            assertNotNull(position.getSpeed());
            assertNotNull(position.getSatellites());
            assertNotNull(payload.getTimestamp());
            assertNotNull(payload.metrics());
            assertEquals(2, payload.metrics().size());
            assertEquals("val", payload.getMetric("key"));
            assertEquals(topic, payload.getMetric("topic"));

            return 1234;
        });

        TestUtil.setFieldValue(cp, "cloudConnectionPublisher", cloudPublisherMock);
        TestUtil.setFieldValue(cp, "positionService", positionServiceMock);

        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<>();
        Map<String, TypedValue<?>> recordProps = new HashMap<>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("key", val);
        val = new StringValue(topic);
        recordProps.put("topic", val);
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);
        WireEnvelope wireEnvelope = new WireEnvelope(emitterPid, wireRecords);

        cp.onWireReceive(wireEnvelope);

        verify(cloudPublisherMock, times(1)).publish((KuraMessage) anyObject());
    }
    
    @Test
    public void testTopicReplacementOnWireReceive() throws InvalidSyntaxException, NoSuchFieldException, KuraException {
        // test publishing a normal message with topic replacement

        FakeCloudPublisher fakeCloudPublisher = new FakeCloudPublisher() {

            private KuraMessage kmessage = new KuraMessage(null);

            @Override
            public String publish(KuraMessage message) throws KuraException {
                kmessage = message;
                return null;
            }

            @Override
            public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
                // TODO Auto-generated method stub

            }

            @Override
            public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
                // TODO Auto-generated method stub

            }

            @Override
            public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
                // TODO Auto-generated method stub

            }

            @Override
            public void unregisterCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
                // TODO Auto-generated method stub

            }

            @Override
            public KuraMessage getMessage() {
                return kmessage;
            }
        };

        String topic = "my test topic";

        CloudPublisher cp = new CloudPublisher();

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        cp.bindWireHelperService(wireHelperServiceMock);

        BundleContext bundleCtxMock = mock(BundleContext.class);
        Filter filter = mock(Filter.class);
        when(bundleCtxMock.createFilter(anyString())).thenReturn(filter);

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(bundleCtxMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("CloudPublisher.target", "cspid");
        properties.put("publish.position", "none");
        properties.put("app.topic", "$assetName/$test");

        cp.activate(ctxMock, properties);
        cp.setCloudPublisher(fakeCloudPublisher);

        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<>();
        Map<String, TypedValue<?>> recordProps = new HashMap<>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("key", val);
        val = new StringValue(topic);
        recordProps.put("topic", val);
        recordProps.put("assetName", new StringValue("testAsset"));
        recordProps.put("test", new StringValue("replaceTest"));
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);
        WireEnvelope wireEnvelope = new WireEnvelope(emitterPid, wireRecords);
        cp.onWireReceive(wireEnvelope);

        KuraMessage km = fakeCloudPublisher.getMessage();
        Map<String, Object> messageProps = km.getProperties();
        for (String key : messageProps.keySet()) {
            logger.info("{} -> {}", key, messageProps.get(key));
        }
        KuraPayload payload = km.getPayload();

        assertNull(payload.getBody());
        assertNull(payload.getPosition());
        assertNotNull(payload.getTimestamp());
        assertNotNull(payload.metrics());
        assertEquals(4, payload.metrics().size());
        assertEquals("val", payload.getMetric("key"));
        assertEquals(topic, payload.getMetric("topic"));

        String appTopic = fillAppTopicPlaceholders("$assetName/$test", km);
        assertEquals("testAsset/replaceTest", appTopic);
        
        logger.info("appTopic = {}",appTopic);

    }

    private String fillAppTopicPlaceholders(String appTopic, KuraMessage message) {
        String TOPIC_PATTERN_STRING = "\\$([^\\s/]+)";
        Pattern TOPIC_PATTERN = Pattern.compile(TOPIC_PATTERN_STRING);
        Matcher matcher = TOPIC_PATTERN.matcher(appTopic);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            Map<String, Object> properties = message.getProperties();
            if (properties.containsKey(matcher.group(1))) {
                String replacement = matcher.group(0);

                Object value = properties.get(matcher.group(1));
                if (replacement != null) {
                    matcher.appendReplacement(buffer, value.toString());
                }
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

}
