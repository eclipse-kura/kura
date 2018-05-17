/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.wire.script.filter.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.osgi.service.component.ComponentContext;

public class ScriptFilterTest {

    @Test
    public void testEvaluationSequence() throws NoSuchFieldException {
        // test activation with update, script execution and deactivation

        ScriptFilter svc = new ScriptFilter();

        WireHelperService whsMock = mock(WireHelperService.class);
        svc.bindWireHelperService(whsMock);

        WireSupport wsMock = mock(WireSupport.class);
        when(whsMock.newWireSupport(svc, null)).thenReturn(wsMock);

        doAnswer(invocation -> {
            List<WireRecord> records = invocation.getArgumentAt(0, List.class);

            assertEquals(2, records.size());

            assertEquals("val", records.get(0).getProperties().get("key").getValue());
            assertEquals(5, records.get(1).getProperties().get("five").getValue());

            return null;
        }).when(wsMock).emit(anyObject());

        Map<String, Object> properties = new HashMap<>();
        String script = "logger.info('testing');\n logger.info(input.emitterPid);\n" // some logging
                + "logger.info(input.records.length);\n" // some more logging
                + "var one = input.records[0];\n logger.info(one.topic);\n"
                + "// one.prop3 = newBooleanValue(true);\n" // modification of the input objects is not supported!
                + "output.add(one);\n" // add the first input record to output
                + "var five = newIntegerValue(5);\n logger.info(five);\n" // prepare and add a new wire record
                + "var rec = newWireRecord();\n rec.five = five;\n output.add(rec);";
        properties.put("script", script);
        svc.activate(mock(ComponentContext.class), properties);

        assertNotNull(TestUtil.getFieldValue(svc, "scriptEngine"));
        assertNotNull(TestUtil.getFieldValue(svc, "bindings"));
        assertNotNull(TestUtil.getFieldValue(svc, "script"));

        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<>();

        // add 2 wires
        Map<String, TypedValue<?>> recordProps = new HashMap<>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("key", val);
        val = new StringValue("topic");
        recordProps.put("topic", val);
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);

        recordProps = new HashMap<>();
        val = new StringValue("val2");
        recordProps.put("key2", val);
        record = new WireRecord(recordProps);
        wireRecords.add(record);

        WireEnvelope wireEnvelope = new WireEnvelope(emitterPid, wireRecords);

        svc.onWireReceive(wireEnvelope);

        verify(wsMock, times(1)).emit(anyObject());

        svc.deactivate();
    }

    @Test
    public void testNoScript() throws NoSuchFieldException {
        // test scenario where no script is provided

        ScriptFilter svc = new ScriptFilter();

        WireHelperService whsMock = mock(WireHelperService.class);
        svc.bindWireHelperService(whsMock);

        Map<String, Object> properties = new HashMap<>();
        svc.activate(mock(ComponentContext.class), properties);

        assertNotNull(TestUtil.getFieldValue(svc, "scriptEngine"));
        assertNotNull(TestUtil.getFieldValue(svc, "bindings"));
        assertNull(TestUtil.getFieldValue(svc, "script"));

        svc.onWireReceive(null);
    }

}
