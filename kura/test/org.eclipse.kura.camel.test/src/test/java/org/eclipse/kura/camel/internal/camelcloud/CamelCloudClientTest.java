/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.camel.internal.camelcloud;

import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_CONTROL;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_DEVICEID;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_MESSAGEID;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_PRIORITY;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_QOS;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_RETAIN;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.StartupListener;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.ExecutorServiceManager;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.camel.camelcloud.CamelCloudService;
import org.eclipse.kura.message.KuraPayload;
import org.junit.Test;

public class CamelCloudClientTest {

    @Test
    public void testPublish() throws KuraException {
        CamelCloudService cloudServiceMock = mock(CamelCloudService.class);
        CamelContext camelContextMock = mock(CamelContext.class);
        String applicationId = "appId";
        String baseEndpoint = "endpoint";

        String appTopic = "topic";
        byte[] payload = new byte[] { 0x01, 0x02 };
        int qos = 0;
        boolean retain = false;
        int priority = 0;

        ExecutorServiceManager esmgrMock = mock(ExecutorServiceManager.class);
        when(camelContextMock.getExecutorServiceManager()).thenReturn(esmgrMock);

        ProducerTemplate producerMock = mock(ProducerTemplate.class);
        when(camelContextMock.createProducerTemplate()).thenReturn(producerMock);

        ExecutorService execSvcMock = mock(ExecutorService.class);
        when(esmgrMock.newThreadPool(anyObject(), eq("CamelCloudClient/" + applicationId), eq(0), eq(1)))
                .thenReturn(execSvcMock);

        doAnswer(invocation -> {
            final String target = invocation.getArgumentAt(0, String.class);
            assertEquals("endpoint" + applicationId + ":" + appTopic, target);

            final KuraPayload load = invocation.getArgumentAt(1, KuraPayload.class);
            assertArrayEquals(payload, load.getBody());

            final Map<String, Object> headers = invocation.getArgumentAt(2, Map.class);
            assertFalse((boolean) headers.get(CAMEL_KURA_CLOUD_CONTROL));
            assertNotNull(headers.get(CAMEL_KURA_CLOUD_MESSAGEID));
            assertNull(headers.get(CAMEL_KURA_CLOUD_DEVICEID));
            assertEquals(qos, headers.get(CAMEL_KURA_CLOUD_QOS));
            assertEquals(retain, headers.get(CAMEL_KURA_CLOUD_RETAIN));
            assertEquals(priority, headers.get(CAMEL_KURA_CLOUD_PRIORITY));

            return null;
        }).when(producerMock).sendBodyAndHeaders(anyString(), anyObject(), anyObject());

        CamelCloudClient ccc = new CamelCloudClient(cloudServiceMock, camelContextMock, applicationId, baseEndpoint);

        ccc.publish(appTopic, payload, qos, retain, priority);

        verify(producerMock).sendBodyAndHeaders(anyString(), anyObject(), anyObject());
    }

    @Test
    public void testPublishWithDeviceId() throws KuraException {
        CamelCloudService cloudServiceMock = mock(CamelCloudService.class);
        CamelContext camelContextMock = mock(CamelContext.class);
        String applicationId = "appId";
        String baseEndpoint = "endpoint:%s";

        String deviceId = "deviceId";
        String appTopic = "topc";
        byte[] payload = new byte[] { 0x01 };
        int qos = 2;
        boolean retain = false;
        int priority = 7;

        ExecutorServiceManager esmgrMock = mock(ExecutorServiceManager.class);
        when(camelContextMock.getExecutorServiceManager()).thenReturn(esmgrMock);

        ProducerTemplate producerMock = mock(ProducerTemplate.class);
        when(camelContextMock.createProducerTemplate()).thenReturn(producerMock);

        ExecutorService execSvcMock = mock(ExecutorService.class);
        when(esmgrMock.newThreadPool(anyObject(), eq("CamelCloudClient/" + applicationId), eq(0), eq(1)))
                .thenReturn(execSvcMock);

        doAnswer(invocation -> {
            final String target = invocation.getArgumentAt(0, String.class);
            assertEquals("endpoint:" + deviceId + ":" + applicationId + ":" + appTopic, target);

            final KuraPayload load = invocation.getArgumentAt(1, KuraPayload.class);
            assertArrayEquals(payload, load.getBody());

            final Map<String, Object> headers = invocation.getArgumentAt(2, Map.class);
            assertFalse((boolean) headers.get(CAMEL_KURA_CLOUD_CONTROL));
            assertNotNull(headers.get(CAMEL_KURA_CLOUD_MESSAGEID));
            assertEquals(deviceId, headers.get(CAMEL_KURA_CLOUD_DEVICEID));
            assertEquals(qos, headers.get(CAMEL_KURA_CLOUD_QOS));
            assertEquals(retain, headers.get(CAMEL_KURA_CLOUD_RETAIN));
            assertEquals(priority, headers.get(CAMEL_KURA_CLOUD_PRIORITY));

            return null;
        }).when(producerMock).sendBodyAndHeaders(anyString(), anyObject(), anyObject());

        CamelCloudClient ccc = new CamelCloudClient(cloudServiceMock, camelContextMock, applicationId, baseEndpoint);

        ccc.publish(deviceId, appTopic, payload, qos, retain, priority);

        verify(producerMock).sendBodyAndHeaders(anyString(), anyObject(), anyObject());
    }

    @Test
    public void testControlPublishWithDeviceId() throws KuraException {
        CamelCloudService cloudServiceMock = mock(CamelCloudService.class);
        CamelContext camelContextMock = mock(CamelContext.class);
        String applicationId = "appId";
        String baseEndpoint = "endpoint:%s";

        String deviceId = "deviceId";
        String appTopic = "topc";
        byte[] payload = new byte[] { 0x01 };
        int qos = 2;
        boolean retain = false;
        int priority = 7;

        ExecutorServiceManager esmgrMock = mock(ExecutorServiceManager.class);
        when(camelContextMock.getExecutorServiceManager()).thenReturn(esmgrMock);

        ProducerTemplate producerMock = mock(ProducerTemplate.class);
        when(camelContextMock.createProducerTemplate()).thenReturn(producerMock);

        ExecutorService execSvcMock = mock(ExecutorService.class);
        when(esmgrMock.newThreadPool(anyObject(), eq("CamelCloudClient/" + applicationId), eq(0), eq(1)))
                .thenReturn(execSvcMock);

        doAnswer(invocation -> {
            final String target = invocation.getArgumentAt(0, String.class);
            assertEquals("endpoint:" + deviceId + ":" + applicationId + ":" + appTopic, target);

            final KuraPayload load = invocation.getArgumentAt(1, KuraPayload.class);
            assertArrayEquals(payload, load.getBody());

            final Map<String, Object> headers = invocation.getArgumentAt(2, Map.class);
            assertTrue((boolean) headers.get(CAMEL_KURA_CLOUD_CONTROL));
            assertNotNull(headers.get(CAMEL_KURA_CLOUD_MESSAGEID));
            assertEquals(deviceId, headers.get(CAMEL_KURA_CLOUD_DEVICEID));
            assertEquals(qos, headers.get(CAMEL_KURA_CLOUD_QOS));
            assertEquals(retain, headers.get(CAMEL_KURA_CLOUD_RETAIN));
            assertEquals(priority, headers.get(CAMEL_KURA_CLOUD_PRIORITY));

            return null;
        }).when(producerMock).sendBodyAndHeaders(anyString(), anyObject(), anyObject());

        CamelCloudClient ccc = new CamelCloudClient(cloudServiceMock, camelContextMock, applicationId, baseEndpoint);

        ccc.controlPublish(deviceId, appTopic, payload, qos, retain, priority);

        verify(producerMock).sendBodyAndHeaders(anyString(), anyObject(), anyObject());
    }

    @Test
    public void testSubscribeException1() throws Exception {
        CamelCloudService cloudServiceMock = mock(CamelCloudService.class);
        CamelContext camelContextMock = mock(CamelContext.class);
        String applicationId = "appId";
        String baseEndpoint = "endpoint";

        String deviceId = "deviceId";
        String appTopic = "topc";
        int qos = 2;

        ExecutorServiceManager esmgrMock = mock(ExecutorServiceManager.class);
        when(camelContextMock.getExecutorServiceManager()).thenReturn(esmgrMock);

        ProducerTemplate producerMock = mock(ProducerTemplate.class);
        when(camelContextMock.createProducerTemplate()).thenReturn(producerMock);

        ExecutorService execSvcMock = mock(ExecutorService.class);
        when(esmgrMock.newThreadPool(anyObject(), eq("CamelCloudClient/" + applicationId), eq(0), eq(1)))
                .thenReturn(execSvcMock);

        CamelCloudClient ccc = new CamelCloudClient(cloudServiceMock, camelContextMock, applicationId, baseEndpoint);

        doThrow(new RuntimeException("test")).when(camelContextMock).addStartupListener(anyObject());

        try {
            ccc.subscribe(deviceId, appTopic, qos);
            fail("Expected an exception");
        } catch (Exception e) {
            assertEquals("test", e.getCause().getMessage());
        }
    }

    @Test
    public void testSubscribeException2() throws Exception {
        CamelCloudService cloudServiceMock = mock(CamelCloudService.class);
        CamelContext camelContextMock = mock(CamelContext.class);
        String applicationId = "appId";
        String baseEndpoint = "endpoint";

        String deviceId = "deviceId";
        String appTopic = "topc";
        int qos = 2;

        ExecutorServiceManager esmgrMock = mock(ExecutorServiceManager.class);
        when(camelContextMock.getExecutorServiceManager()).thenReturn(esmgrMock);

        ProducerTemplate producerMock = mock(ProducerTemplate.class);
        when(camelContextMock.createProducerTemplate()).thenReturn(producerMock);

        ExecutorService execSvcMock = mock(ExecutorService.class);
        when(esmgrMock.newThreadPool(anyObject(), eq("CamelCloudClient/" + applicationId), eq(0), eq(1)))
        .thenReturn(execSvcMock);

        CamelCloudClient ccc = new CamelCloudClient(cloudServiceMock, camelContextMock, applicationId, baseEndpoint);

        when(execSvcMock.submit((Callable<?>) anyObject())).thenAnswer(invocation -> {
            Callable<?> task = invocation.getArgumentAt(0, Callable.class);

            task.call();

            return null;
        });

        doAnswer(invocation -> {
            StartupListener listener = invocation.getArgumentAt(0, StartupListener.class);

            listener.onCamelContextStarted(camelContextMock, false);

            return null;
        }).when(camelContextMock).addStartupListener(anyObject());

        doThrow(new RuntimeException("test")).when(camelContextMock).addRoutes(anyObject());

        try {
            ccc.subscribe(deviceId, appTopic, qos);
            fail("Expected an exception");
        } catch (Exception e) {
            assertEquals("test", e.getCause().getCause().getMessage());
        }
    }

    @Test
    public void testSubscribe() throws Exception {
        CamelCloudService cloudServiceMock = mock(CamelCloudService.class);
        CamelContext camelContextMock = mock(CamelContext.class);
        String applicationId = "appId";
        String baseEndpoint = "endpoint";

        String deviceId = "deviceId";
        String appTopic = "topc";
        int qos = 2;

        ExecutorServiceManager esmgrMock = mock(ExecutorServiceManager.class);
        when(camelContextMock.getExecutorServiceManager()).thenReturn(esmgrMock);

        ProducerTemplate producerMock = mock(ProducerTemplate.class);
        when(camelContextMock.createProducerTemplate()).thenReturn(producerMock);

        ExecutorService execSvcMock = mock(ExecutorService.class);
        when(esmgrMock.newThreadPool(anyObject(), eq("CamelCloudClient/" + applicationId), eq(0), eq(1)))
                .thenReturn(execSvcMock);

        CamelCloudClient ccc = new CamelCloudClient(cloudServiceMock, camelContextMock, applicationId, baseEndpoint);

        when(execSvcMock.submit((Callable<?>) anyObject())).thenAnswer(invocation -> {
            Callable<?> task = invocation.getArgumentAt(0, Callable.class);

            task.call();

            return null;
        });

        doAnswer(invocation -> {
            StartupListener listener = invocation.getArgumentAt(0, StartupListener.class);

            listener.onCamelContextStarted(camelContextMock, false);

            return null;
        }).when(camelContextMock).addStartupListener(anyObject());

        doAnswer(invocation -> {
            RouteBuilder builder = invocation.getArgumentAt(0, RouteBuilder.class);

            builder.configure();

            return null;
        }).when(camelContextMock).addRoutes(anyObject());

        ccc.subscribe(deviceId, appTopic, qos);
    }

}
