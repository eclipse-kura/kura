/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.message.KuraPayload;
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

public class CloudPublisherTest {

    @Test
    public void testActivateFilterException() throws InvalidSyntaxException {
        // test activation where exception during filter creation - tests null filter behavior
        CloudPublisher cp = new CloudPublisher();

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        cp.bindWireHelperService(wireHelperServiceMock);

        BundleContext bundleCtxMock = mock(BundleContext.class);
        when(bundleCtxMock.createFilter(anyString())).thenThrow(new InvalidSyntaxException("test", "filter"));

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(bundleCtxMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("cloud.service.pid", "cspid");
        properties.put("publish.priority", 1);
        properties.put("publish.qos", 0);
        properties.put("publish.retain", true);


        try {
            cp.activate(ctxMock, properties);
            fail("Exception was expected.");
        } catch (NullPointerException e) {
            // expected
        }

        verify(ctxMock, times(1)).getBundleContext();
        verify(bundleCtxMock, times(1))
                .createFilter("(&(objectClass=org.eclipse.kura.cloud.CloudService)(kura.service.pid=cspid))");
    }

    @Test
    public void testOnWireReceive() throws InvalidSyntaxException, NoSuchFieldException, KuraException {
        // test publishing a normal message with topic replacement

        int prio = 1;
        int qos = 0;
        boolean retain = true;
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
        properties.put("cloud.service.pid", "cspid");
        properties.put("publish.priority", prio);
        properties.put("publish.qos", qos);
        properties.put("publish.retain", retain);
        properties.put("publish.topic", "$topic");

        cp.activate(ctxMock, properties);

        verify(ctxMock, times(1)).getBundleContext();
        verify(bundleCtxMock, times(1))
                .createFilter("(&(objectClass=org.eclipse.kura.cloud.CloudService)(kura.service.pid=cspid))");

        CloudService cloudServiceMock = mock(CloudService.class);

        CloudClient cloudClientMock = mock(CloudClient.class);

        when(cloudClientMock.publish(eq(topic), (KuraPayload) anyObject(), eq(qos), eq(retain), eq(prio)))
                .thenAnswer(invocation -> {
                    KuraPayload payload = invocation.getArgumentAt(1, KuraPayload.class);

                    assertNull(payload.getBody());
                    assertNull(payload.getTimestamp());
                    assertNotNull(payload.metrics());
                    assertEquals(2, payload.metrics().size());
                    assertEquals("val", payload.getMetric("key"));
                    assertEquals(topic, payload.getMetric("topic"));

                    return 1234;
                });

        TestUtil.setFieldValue(cp, "cloudService", cloudServiceMock);
        TestUtil.setFieldValue(cp, "cloudClient", cloudClientMock);

        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<WireRecord>();
        Map<String, TypedValue<?>> recordProps = new HashMap<String, TypedValue<?>>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("key", val);
        val = new StringValue(topic);
        recordProps.put("topic", val);
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);
        WireEnvelope wireEnvelope = new WireEnvelope(emitterPid, wireRecords);

        cp.onWireReceive(wireEnvelope);

        verify(cloudClientMock, times(1)).publish(eq(topic), (KuraPayload) anyObject(), eq(qos), eq(retain), eq(prio));
    }

    @Test
    public void testOnWireReceiveControlPublishWithException()
            throws InvalidSyntaxException, NoSuchFieldException, KuraException {

        // test sending a control message, where exception occurs in the end (it's only logged - helps testing log
        // messages)

        int prio = 1;
        int qos = 0;
        boolean retain = true;
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
        properties.put("cloud.service.pid", "cspid");
        properties.put("publish.priority", prio);
        properties.put("publish.qos", qos);
        properties.put("publish.retain", retain);
        properties.put("publish.topic", "$topic");
        properties.put("publish.control.messages", true);

        cp.activate(ctxMock, properties);

        verify(ctxMock, times(1)).getBundleContext();
        verify(bundleCtxMock, times(1))
                .createFilter("(&(objectClass=org.eclipse.kura.cloud.CloudService)(kura.service.pid=cspid))");

        CloudService cloudServiceMock = mock(CloudService.class);

        CloudClient cloudClientMock = mock(CloudClient.class);
        when(cloudClientMock.controlPublish(eq(topic), (KuraPayload) anyObject(), eq(qos), eq(retain), eq(prio)))
                .thenThrow(new NullPointerException("test"));

        TestUtil.setFieldValue(cp, "cloudService", cloudServiceMock);
        TestUtil.setFieldValue(cp, "cloudClient", cloudClientMock);

        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<WireRecord>();
        Map<String, TypedValue<?>> recordProps = new HashMap<String, TypedValue<?>>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("key", val);
        val = new StringValue(topic);
        recordProps.put("topic", val);
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);
        WireEnvelope wireEnvelope = new WireEnvelope(emitterPid, wireRecords);

        cp.onWireReceive(wireEnvelope);

        verify(cloudClientMock, times(1)).controlPublish(eq(topic), (KuraPayload) anyObject(), eq(qos), eq(retain),
                eq(prio));
    }
}
