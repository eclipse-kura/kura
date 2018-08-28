/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.wire.subscriber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import org.eclipse.kura.cloudconnection.message.KuraMessage;
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
    public void testOnMessageArrived() throws InvalidSyntaxException {
        // test arrival of payload with all supported types and its conversion into WireRecord

        CloudSubscriber cs = new CloudSubscriber();

        byte[] bytea = new byte[] { 0x01, 0x03 };
        String strval = "strval";

        KuraPayload payload = new KuraPayload();
        payload.addMetric("bool", true);
        payload.addMetric("bytea", bytea);
        payload.addMetric("float", 1.1f);
        payload.addMetric("double", 1.2);
        payload.addMetric("integer", 11);
        payload.addMetric("long", 12l);
        payload.addMetric("string", strval);

        KuraMessage message = new KuraMessage(payload);

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        cs.bindWireHelperService(wireHelperServiceMock);

        WireSupport wsMock = mock(WireSupport.class);
        when(wireHelperServiceMock.newWireSupport(cs, null)).thenReturn(wsMock);

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
        properties.put("CloudSubscriber.target", "cspid");

        cs.activate(ctxMock, properties);

        cs.onMessageArrived(message);

        verify(wsMock, times(1)).emit(anyObject());
    }

    @Test
    public void testActivateDeactivate() throws NoSuchFieldException, InvalidSyntaxException, KuraException {
        // test activation and deactivation in a sequence

        CloudSubscriber cs = new CloudSubscriber();

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        cs.bindWireHelperService(wireHelperServiceMock);

        WireSupport wsMock = mock(WireSupport.class);
        when(wireHelperServiceMock.newWireSupport(cs, null)).thenReturn(wsMock);

        BundleContext bundleCtxMock = mock(BundleContext.class);
        Filter filter = mock(Filter.class);
        when(bundleCtxMock.createFilter(anyString())).thenReturn(filter);

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(bundleCtxMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("CloudSubscriber.target", "cspid");

        cs.activate(ctxMock, properties);

        cs.deactivate(ctxMock);
    }
}
