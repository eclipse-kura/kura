/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.wire.regexfilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;

public class RegexFilterTest {

    @Test
    public void testActivate() throws InvalidSyntaxException, NoSuchFieldException {
        // test that activation sets the necessary fields

        RegexFilter rf = new RegexFilter();

        String filter = "foo";
        Integer filterType = 0;

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        rf.bindWireHelperService(wireHelperServiceMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("kura.service.pid", "spid");
        properties.put("regex.filter", filter);
        properties.put("filter.type", filterType);

        rf.activate(properties, mock(ComponentContext.class));

        assertEquals(filter, TestUtil.getFieldValue(rf, "filter"));
        assertEquals(FilterType.RETAIN, TestUtil.getFieldValue(rf, "filterType"));
        assertEquals("spid", TestUtil.getFieldValue(rf, "componentPid"));
    }

    @Test
    public void testOnWireReceiveNoFilter() throws InvalidSyntaxException, NoSuchFieldException {
        // no filter means all records get returned

        RegexFilter rf = new RegexFilter();

        Integer filterType = 0;

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        rf.bindWireHelperService(wireHelperServiceMock);

        WireSupport wsMock = mock(WireSupport.class);
        when(wireHelperServiceMock.newWireSupport(rf, null)).thenReturn(wsMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("kura.service.pid", "spid");
        properties.put("filter.type", filterType);

        rf.activate(properties, mock(ComponentContext.class));

        String topic = "topic";
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

        rf.onWireReceive(wireEnvelope);

        verify(wsMock, times(1)).emit(wireRecords);
    }

    @Test
    public void testOnWireReceiveAll() throws InvalidSyntaxException, NoSuchFieldException {
        // .* filter means all records get returned

        RegexFilter rf = new RegexFilter();

        String filter = ".*";
        Integer filterType = 0;

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        rf.bindWireHelperService(wireHelperServiceMock);

        WireSupport wsMock = mock(WireSupport.class);
        when(wireHelperServiceMock.newWireSupport(rf, null)).thenReturn(wsMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("kura.service.pid", "spid");
        properties.put("regex.filter", filter);
        properties.put("filter.type", filterType);

        rf.activate(properties, mock(ComponentContext.class));

        String topic = "topic";
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

        rf.onWireReceive(wireEnvelope);

        verify(wsMock, times(1)).emit(wireRecords);
    }

    @Test
    public void testOnWireReceiveEmpty() throws InvalidSyntaxException, NoSuchFieldException {
        // filter matches none of the available properties

        RegexFilter rf = new RegexFilter();

        String filter = "filter";
        Integer filterType = 0;

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        rf.bindWireHelperService(wireHelperServiceMock);

        WireSupport wsMock = mock(WireSupport.class);
        when(wireHelperServiceMock.newWireSupport(rf, null)).thenReturn(wsMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("kura.service.pid", "spid");
        properties.put("regex.filter", filter);
        properties.put("filter.type", filterType);

        rf.activate(properties, mock(ComponentContext.class));

        String topic = "topic";
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

        doAnswer(invocation -> {
            List<WireRecord> records = invocation.getArgumentAt(0, List.class);

            assertNotNull(records);
            assertEquals(1, records.size());
            assertNotNull(records.get(0));
            assertNotNull(records.get(0).getProperties());
            assertEquals(0, records.get(0).getProperties().size());

            return null;
        }).when(wsMock).emit(anyObject());

        rf.onWireReceive(wireEnvelope);

        verify(wsMock, times(1)).emit(anyObject());
    }

    @Test
    public void testOnWireReceivePartial() throws InvalidSyntaxException, NoSuchFieldException {
        // only return certain records with certain properties

        RegexFilter rf = new RegexFilter();

        String filter = ".*y.*";
        Integer filterType = 0;

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        rf.bindWireHelperService(wireHelperServiceMock);

        WireSupport wsMock = mock(WireSupport.class);
        when(wireHelperServiceMock.newWireSupport(rf, null)).thenReturn(wsMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("kura.service.pid", "spid");
        properties.put("regex.filter", filter);
        properties.put("filter.type", filterType);

        rf.activate(properties, mock(ComponentContext.class));

        String topic = "topic";
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

        doAnswer(invocation -> {
            List<WireRecord> records = invocation.getArgumentAt(0, List.class);

            assertNotNull(records);
            assertEquals(1, records.size());
            assertNotNull(records.get(0));
            assertNotNull(records.get(0).getProperties());
            assertEquals(1, records.get(0).getProperties().size());
            assertTrue(records.get(0).getProperties().containsKey("key"));

            return null;
        }).when(wsMock).emit(anyObject());

        rf.onWireReceive(wireEnvelope);

        verify(wsMock, times(1)).emit(anyObject());
    }

    @Test
    public void testOnWireReceivePartial2() throws InvalidSyntaxException, NoSuchFieldException {
        // only return certain records with certain properties

        RegexFilter rf = new RegexFilter();

        String filter = ".*ab[bB].*";
        Integer filterType = 0;

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        rf.bindWireHelperService(wireHelperServiceMock);

        WireSupport wsMock = mock(WireSupport.class);
        when(wireHelperServiceMock.newWireSupport(rf, null)).thenReturn(wsMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("kura.service.pid", "spid");
        properties.put("regex.filter", filter);
        properties.put("filter.type", filterType);

        rf.activate(properties, mock(ComponentContext.class));

        String topic = "topic";
        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<WireRecord>();
        Map<String, TypedValue<?>> recordProps = new HashMap<String, TypedValue<?>>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("ABBA", val);
        val = new StringValue(topic);
        recordProps.put("abBa", val);
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);

        recordProps = new HashMap<String, TypedValue<?>>();
        val = new StringValue("val");
        recordProps.put("ABCABBAB", val);
        val = new StringValue(topic);
        recordProps.put("acbabbc", val);
        record = new WireRecord(recordProps);
        wireRecords.add(record);

        recordProps = new HashMap<String, TypedValue<?>>();
        val = new StringValue("val");
        recordProps.put("abcacba", val);
        val = new StringValue(topic);
        recordProps.put("aba", val);
        record = new WireRecord(recordProps);
        wireRecords.add(record);

        WireEnvelope wireEnvelope = new WireEnvelope(emitterPid, wireRecords);

        doAnswer(invocation -> {
            List<WireRecord> records = invocation.getArgumentAt(0, List.class);

            assertNotNull(records);
            assertEquals(3, records.size()); // all records and filtered properties!

            WireRecord wireRecord = records.get(0);
            assertNotNull(wireRecord);
            Map<String, TypedValue<?>> props = wireRecord.getProperties();
            assertNotNull(props);
            assertEquals(1, props.size());
            assertTrue(props.containsKey("abBa"));

            wireRecord = records.get(1);
            assertNotNull(wireRecord);
            props = wireRecord.getProperties();
            assertNotNull(props);
            assertEquals(1, props.size());
            assertTrue(props.containsKey("acbabbc"));

            wireRecord = records.get(2);
            assertNotNull(wireRecord);
            props = wireRecord.getProperties();
            assertNotNull(props);
            assertEquals(0, props.size());

            return null;
        }).when(wsMock).emit(anyObject());

        rf.onWireReceive(wireEnvelope);

        verify(wsMock, times(1)).emit(anyObject());
    }

    @Test
    public void testOnWireReceivePartial3() throws InvalidSyntaxException, NoSuchFieldException {
        // only return certain records with certain properties

        RegexFilter rf = new RegexFilter();

        String filter = "^ab.*a";
        Integer filterType = 0;

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        rf.bindWireHelperService(wireHelperServiceMock);

        WireSupport wsMock = mock(WireSupport.class);
        when(wireHelperServiceMock.newWireSupport(rf, null)).thenReturn(wsMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("kura.service.pid", "spid");
        properties.put("regex.filter", filter);
        properties.put("filter.type", filterType);

        rf.activate(properties, mock(ComponentContext.class));

        String topic = "topic";
        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<WireRecord>();
        Map<String, TypedValue<?>> recordProps = new HashMap<String, TypedValue<?>>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("ABBA", val);
        val = new StringValue(topic);
        recordProps.put("abBa", val);
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);

        recordProps = new HashMap<String, TypedValue<?>>();
        val = new StringValue("val");
        recordProps.put("ABCABBAB", val);
        val = new StringValue(topic);
        recordProps.put("acbabbca", val);
        record = new WireRecord(recordProps);
        wireRecords.add(record);

        recordProps = new HashMap<String, TypedValue<?>>();
        val = new StringValue("val");
        recordProps.put("abcacba", val);
        val = new StringValue(topic);
        recordProps.put("abac", val);
        record = new WireRecord(recordProps);
        wireRecords.add(record);

        WireEnvelope wireEnvelope = new WireEnvelope(emitterPid, wireRecords);

        doAnswer(invocation -> {
            List<WireRecord> records = invocation.getArgumentAt(0, List.class);

            assertNotNull(records);
            assertEquals(3, records.size());

            WireRecord wireRecord = records.get(0);
            assertNotNull(wireRecord);
            Map<String, TypedValue<?>> props = wireRecord.getProperties();
            assertNotNull(props);
            assertEquals(1, props.size());
            assertTrue(props.containsKey("abBa"));

            wireRecord = records.get(1);
            assertNotNull(wireRecord);
            props = wireRecord.getProperties();
            assertNotNull(props);
            assertEquals(0, props.size());

            wireRecord = records.get(2);
            assertNotNull(wireRecord);
            props = wireRecord.getProperties();
            assertNotNull(props);
            assertEquals(1, props.size());
            assertTrue(props.containsKey("abcacba"));

            return null;
        }).when(wsMock).emit(anyObject());

        rf.onWireReceive(wireEnvelope);

        verify(wsMock, times(1)).emit(anyObject());
    }

}
