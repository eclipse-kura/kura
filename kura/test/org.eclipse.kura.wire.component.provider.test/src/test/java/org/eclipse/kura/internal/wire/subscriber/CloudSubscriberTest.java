/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.wire.subscriber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;

public class CloudSubscriberTest {

    @Test
    public void testActivate() throws InvalidSyntaxException {
        // test activation with filter exception

        CloudSubscriber cs = new CloudSubscriber();

        int qos = 0;
        boolean retain = false;

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        cs.bindWireHelperService(wireHelperServiceMock);

        BundleContext bundleCtxMock = mock(BundleContext.class);
        when(bundleCtxMock.createFilter(anyString())).thenThrow(new InvalidSyntaxException("test", "filter"));

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(bundleCtxMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("cloud.service.pid", "cspid");
        properties.put("publish.qos", qos);
        properties.put("publish.retain", retain);
        properties.put("publish.topic", "$topic");

        try {
            cs.activate(ctxMock, properties);
            fail("Expected an exception.");
        } catch (NullPointerException e) {
            // OK
        }

        verify(ctxMock, times(1)).getBundleContext();
        verify(bundleCtxMock, times(1)).createFilter(anyString());
    }

    @Test
    public void testOnMessageArrived() throws InvalidSyntaxException {
        // test arrival of payload with all supported types and its conversion into WireRecord

        CloudSubscriber cs = new CloudSubscriber();

        String deviceId = "DevId";
        String appTopic = "topic";
        int qos = 0;
        boolean retain = false;

        byte[] bytea = new byte[] { 0x01, 0x03 };
        String strval = "strval";

        KuraPayload msg = new KuraPayload();
        msg.addMetric("bool", true);
        msg.addMetric("bytea", bytea);
        msg.addMetric("float", 1.1f);
        msg.addMetric("double", 1.2);
        msg.addMetric("integer", 11);
        msg.addMetric("long", 12l);
        msg.addMetric("string", strval);

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        cs.bindWireHelperService(wireHelperServiceMock);

        WireSupport wsMock = mock(WireSupport.class);
        when(wireHelperServiceMock.newWireSupport(anyObject())).thenReturn(wsMock);

        doAnswer(invocation -> {
            List<?> wireRecords = invocation.getArgumentAt(0, List.class);

            assertNotNull(wireRecords);
            assertEquals(1, wireRecords.size());

            WireRecord record = (WireRecord) wireRecords.get(0);
            Map<String, TypedValue<?>> properties = record.getProperties();
            assertNotNull(properties);
            assertEquals(7, properties.size());
            assertEquals(DataType.BOOLEAN, properties.get("bool").getType());
            assertEquals(true, properties.get("bool").getValue());
            assertEquals(DataType.BYTE_ARRAY, properties.get("bytea").getType());
            assertEquals(bytea, properties.get("bytea").getValue());
            assertEquals(DataType.FLOAT, properties.get("float").getType());
            assertEquals(1.1f, properties.get("float").getValue());
            assertEquals(DataType.DOUBLE, properties.get("double").getType());
            assertEquals(1.2, properties.get("double").getValue());
            assertEquals(DataType.INTEGER, properties.get("integer").getType());
            assertEquals(11, properties.get("integer").getValue());
            assertEquals(DataType.LONG, properties.get("long").getType());
            assertEquals(12l, properties.get("long").getValue());
            assertEquals(DataType.STRING, properties.get("string").getType());
            assertEquals(strval, properties.get("string").getValue());

            return null;
        }).when(wsMock).emit(anyObject());

        BundleContext bundleCtxMock = mock(BundleContext.class);
        Filter filter = mock(Filter.class);
        when(bundleCtxMock.createFilter(anyString())).thenReturn(filter);

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(bundleCtxMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("cloud.service.pid", "cspid");
        properties.put("subscribe.qos", qos);
        properties.put("subscribe.deviceId", deviceId);
        properties.put("subscribe.appTopic", "$topic");

        cs.activate(ctxMock, properties);

        verify(ctxMock, times(1)).getBundleContext();
        verify(bundleCtxMock, times(1))
                .createFilter("(&(objectClass=org.eclipse.kura.cloud.CloudService)(kura.service.pid=cspid))");

        cs.onMessageArrived(deviceId, appTopic, msg, qos, retain);

        verify(wsMock, times(1)).emit(anyObject());
    }

    @Test
    public void testActivateDeactivate() throws NoSuchFieldException, InvalidSyntaxException, KuraException {
        // test activation and deactivation in a sequence

        CloudSubscriber cs = new CloudSubscriber();

        CloudClient ccMock = mock(CloudClient.class);
        TestUtil.setFieldValue(cs, "cloudClient", ccMock);

        String deviceId = "DevId";
        String appTopic = "topic";
        int qos = 0;

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        cs.bindWireHelperService(wireHelperServiceMock);

        WireSupport wsMock = mock(WireSupport.class);
        when(wireHelperServiceMock.newWireSupport(anyObject())).thenReturn(wsMock);

        BundleContext bundleCtxMock = mock(BundleContext.class);
        Filter filter = mock(Filter.class);
        when(bundleCtxMock.createFilter(anyString())).thenReturn(filter);

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(bundleCtxMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("cloud.service.pid", "cspid");
        properties.put("subscribe.qos", qos);
        properties.put("subscribe.deviceId", deviceId);
        properties.put("subscribe.appTopic", appTopic);

        cs.activate(ctxMock, properties);

        verify(ctxMock, times(1)).getBundleContext();
        verify(bundleCtxMock, times(1))
                .createFilter("(&(objectClass=org.eclipse.kura.cloud.CloudService)(kura.service.pid=cspid))");

        cs.deactivate(ctxMock);

        verify(ccMock, times(1)).release();
        verify(ccMock, times(1)).unsubscribe(deviceId, appTopic);
    }
}
