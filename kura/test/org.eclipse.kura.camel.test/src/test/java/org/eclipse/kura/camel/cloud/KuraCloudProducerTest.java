/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.camel.cloud;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.message.KuraPayload;
import org.junit.Test;

public class KuraCloudProducerTest {

    @Test
    public void testProcessNoBody() throws Exception {
        // process without body

        KuraCloudEndpoint endpointMock = mock(KuraCloudEndpoint.class);
        KuraCloudProducer kcp = new KuraCloudProducer(endpointMock, null);

        Exchange exchangeMock = mock(Exchange.class);
        Message msgMock = mock(Message.class);
        when(exchangeMock.getIn()).thenReturn(msgMock);

        try {
            kcp.process(exchangeMock);
        } catch (RuntimeException e) {
            assertEquals("Cannot produce null payload.", e.getMessage());
        }
    }

    @Test
    public void testProcessNoControl() throws Exception {
        // process with body and no control flag

        KuraCloudEndpoint endpointMock = mock(KuraCloudEndpoint.class);
        CloudClient clientMock = mock(CloudClient.class);
        KuraCloudProducer kcp = new KuraCloudProducer(endpointMock, clientMock);

        Exchange exchangeMock = mock(Exchange.class);
        Message msgMock = mock(Message.class);
        when(exchangeMock.getIn()).thenReturn(msgMock);

        Object body = new byte[] { 0x01, 0x02, 0x03 };
        when(msgMock.getBody()).thenReturn(body);

        String topic = "topic";
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_TOPIC, String.class)).thenReturn(topic);
        int qos = 1;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_QOS, Integer.class)).thenReturn(qos);
        int prio = 1;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_PRIORITY, Integer.class)).thenReturn(prio);
        boolean retain = false;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_RETAIN, Boolean.class)).thenReturn(retain);
        boolean control = false;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_CONTROL, Boolean.class)).thenReturn(control);
        String deviceId = "id";
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_DEVICEID, String.class)).thenReturn(deviceId);

        when(clientMock.publish(eq(topic), (KuraPayload) anyObject(), eq(qos), eq(retain), eq(prio)))
                .thenAnswer(invocation -> {
                    Object[] args = invocation.getArguments();
                    KuraPayload payload = (KuraPayload) args[1];
                    assertEquals(body, payload.getBody());
                    return 1;
                });

        kcp.process(exchangeMock);

        verify(clientMock, times(1)).publish(eq(topic), (KuraPayload) anyObject(), eq(qos), eq(retain), eq(prio));
    }

    @Test
    public void testProcessControlNoDeviceId() throws Exception {
        // process with control flag and no device id

        KuraCloudEndpoint endpointMock = mock(KuraCloudEndpoint.class);
        CloudClient clientMock = mock(CloudClient.class);
        KuraCloudProducer kcp = new KuraCloudProducer(endpointMock, clientMock);

        Exchange exchangeMock = mock(Exchange.class);
        Message msgMock = mock(Message.class);
        when(exchangeMock.getIn()).thenReturn(msgMock);

        Object body = new byte[] { 0x01, 0x02, 0x03 };
        when(msgMock.getBody()).thenReturn(body);

        String topic = "topic";
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_TOPIC, String.class)).thenReturn(topic);
        int qos = 1;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_QOS, Integer.class)).thenReturn(qos);
        int prio = 1;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_PRIORITY, Integer.class)).thenReturn(prio);
        boolean retain = false;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_RETAIN, Boolean.class)).thenReturn(retain);
        boolean control = true;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_CONTROL, Boolean.class)).thenReturn(control);
        String deviceId = null;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_DEVICEID, String.class)).thenReturn(deviceId);

        when(clientMock.controlPublish(eq(topic), (KuraPayload) anyObject(), eq(qos), eq(retain), eq(prio)))
                .thenAnswer(invocation -> {
                    Object[] args = invocation.getArguments();
                    KuraPayload payload = (KuraPayload) args[1];
                    assertEquals(body, payload.getBody());
                    return 1;
                });

        kcp.process(exchangeMock);

        verify(clientMock, times(1)).controlPublish(eq(topic), (KuraPayload) anyObject(), eq(qos), eq(retain),
                eq(prio));
    }

    @Test
    public void testProcessOtherBody() throws Exception {
        // process with Object body and no control flag

        KuraCloudEndpoint endpointMock = mock(KuraCloudEndpoint.class);
        CloudClient clientMock = mock(CloudClient.class);
        KuraCloudProducer kcp = new KuraCloudProducer(endpointMock, clientMock);

        Exchange exchangeMock = mock(Exchange.class);
        Message msgMock = mock(Message.class);
        when(exchangeMock.getIn()).thenReturn(msgMock);

        Object body = new Object();
        when(msgMock.getBody()).thenReturn(body);
        byte[] bodyB = new byte[] { 0x01, 0x02, 0x03 };
        when(msgMock.getBody(byte[].class)).thenReturn(bodyB);

        String topic = "topic";
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_TOPIC, String.class)).thenReturn(topic);
        int qos = 1;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_QOS, Integer.class)).thenReturn(qos);
        int prio = 1;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_PRIORITY, Integer.class)).thenReturn(prio);
        boolean retain = false;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_RETAIN, Boolean.class)).thenReturn(retain);
        boolean control = false;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_CONTROL, Boolean.class)).thenReturn(control);
        String deviceId = "id";
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_DEVICEID, String.class)).thenReturn(deviceId);

        when(clientMock.publish(eq(topic), (KuraPayload) anyObject(), eq(qos), eq(retain), eq(prio)))
                .thenAnswer(invocation -> {
                    Object[] args = invocation.getArguments();
                    KuraPayload payload = (KuraPayload) args[1];
                    assertEquals(bodyB, payload.getBody());
                    return 1;
                });

        kcp.process(exchangeMock);

        verify(clientMock, times(1)).publish(eq(topic), (KuraPayload) anyObject(), eq(qos), eq(retain), eq(prio));
    }

    @Test
    public void testProcessOtherBodyString() throws Exception {
        // process with String body and no control flag

        KuraCloudEndpoint endpointMock = mock(KuraCloudEndpoint.class);
        CloudClient clientMock = mock(CloudClient.class);
        KuraCloudProducer kcp = new KuraCloudProducer(endpointMock, clientMock);

        Exchange exchangeMock = mock(Exchange.class);
        Message msgMock = mock(Message.class);
        when(exchangeMock.getIn()).thenReturn(msgMock);

        Object body = new Object();
        when(msgMock.getBody()).thenReturn(body);
        byte[] bodyB = null;
        when(msgMock.getBody(byte[].class)).thenReturn(bodyB);
        String bodyS = "body";
        when(msgMock.getBody(String.class)).thenReturn(bodyS);

        String topic = "topic";
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_TOPIC, String.class)).thenReturn(topic);
        int qos = 1;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_QOS, Integer.class)).thenReturn(qos);
        int prio = 1;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_PRIORITY, Integer.class)).thenReturn(prio);
        boolean retain = false;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_RETAIN, Boolean.class)).thenReturn(retain);
        boolean control = false;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_CONTROL, Boolean.class)).thenReturn(control);
        String deviceId = "id";
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_DEVICEID, String.class)).thenReturn(deviceId);

        when(clientMock.publish(eq(topic), (KuraPayload) anyObject(), eq(qos), eq(retain), eq(prio)))
                .thenAnswer(invocation -> {
                    Object[] args = invocation.getArguments();
                    KuraPayload payload = (KuraPayload) args[1];
                    assertArrayEquals(bodyS.getBytes(), payload.getBody());
                    return 1;
                });

        kcp.process(exchangeMock);

        verify(clientMock, times(1)).publish(eq(topic), (KuraPayload) anyObject(), eq(qos), eq(retain), eq(prio));
    }

    @Test
    public void testProcess() throws Exception {
        // process with body and control flag and device id

        KuraCloudEndpoint endpointMock = mock(KuraCloudEndpoint.class);
        CloudClient clientMock = mock(CloudClient.class);
        KuraCloudProducer kcp = new KuraCloudProducer(endpointMock, clientMock);

        Exchange exchangeMock = mock(Exchange.class);
        Message msgMock = mock(Message.class);
        when(exchangeMock.getIn()).thenReturn(msgMock);

        Object body = new byte[] { 0x01, 0x02, 0x03 };
        when(msgMock.getBody()).thenReturn(body);

        String topic = "topic";
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_TOPIC, String.class)).thenReturn(topic);
        int qos = 1;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_QOS, Integer.class)).thenReturn(qos);
        int prio = 1;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_PRIORITY, Integer.class)).thenReturn(prio);
        boolean retain = false;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_RETAIN, Boolean.class)).thenReturn(retain);
        boolean control = true;
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_CONTROL, Boolean.class)).thenReturn(control);
        String deviceId = "id";
        when(msgMock.getHeader(KuraCloudClientConstants.CAMEL_KURA_CLOUD_DEVICEID, String.class)).thenReturn(deviceId);

        when(clientMock.controlPublish(eq(deviceId), eq(topic), (KuraPayload) anyObject(), eq(qos), eq(retain),
                eq(prio))).thenAnswer(invocation -> {
                    Object[] args = invocation.getArguments();
                    KuraPayload payload = (KuraPayload) args[2];
                    assertEquals(body, payload.getBody());
                    return 1;
                });

        kcp.process(exchangeMock);

        verify(clientMock, times(1)).controlPublish(eq(deviceId), eq(topic), (KuraPayload) anyObject(), eq(qos),
                eq(retain), eq(prio));
    }
}
