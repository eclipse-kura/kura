/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.wire.script.filter.provider.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.eclipse.kura.wire.script.filter.provider.ScriptFilter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.wireadmin.Wire;

public class ScriptFilterTest {

    private static CountDownLatch dependencyLatch = new CountDownLatch(2);

    private static ScriptFilter filter;
    private static ConfigurationService cfgsvc;
    private static WireGraphService wireGraphSvc;

    private static Object filterLock = new Object();

    @BeforeClass
    public static void setup() throws KuraException {
        try {
            dependencyLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void init() throws InterruptedException {
        // may need to wait for filter to be registered and injected
        if (filter == null) {
            synchronized (filterLock) {
                filterLock.wait(3000);
            }
        }
    }

    protected void activate() throws KuraException {
        if (cfgsvc != null) {
            Map<String, Object> props = new HashMap<>();
            String script = "logger.info('testing');\n logger.info(input.emitterPid);\n" // some logging
                    + "logger.info(input.records.length);\n" // some more logging
                    + "var one = input.records[0];\n logger.info(one.topic);\n"
                    + "// one.prop3 = newBooleanValue(true);\n" // modification of the input objects is not supported!
                    + "output.add(one);\n" // add the first input record to output
                    + "var five = newIntegerValue(5);\n logger.info(five);\n" // prepare and add a new wire record
                    + "var rec = newWireRecord();\n rec.five = five;\n output.add(rec);";
            props.put("script", script);

            cfgsvc.createFactoryConfiguration("org.eclipse.kura.wire.ScriptFilter", "foo", props, false);
        }
    }

    @Test
    public void testSvcs() {
        assertNotNull(cfgsvc);
        assertNotNull(wireGraphSvc);
        assertNotNull(filter);
    }

    @Test
    public void testReceive() throws SQLException {
        Wire wire = mock(Wire.class);
        doAnswer(invocation -> {
            WireEnvelope envelope = invocation.getArgumentAt(0, WireEnvelope.class);

            assertEquals(2, envelope.getRecords().size());

            return null;
        }).when(wire).update(anyObject());

        final Dictionary wireProperties = new Hashtable<>();
        wireProperties.put("emitter.port", 0);

        when(wire.getProperties()).thenReturn(wireProperties);

        Wire[] wires = { wire };
        filter.consumersConnected(wires);

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

        filter.onWireReceive(wireEnvelope);

        verify(wire, times(1)).update(anyObject());
    }

    protected void bindFilter(WireComponent filter) {
        ScriptFilterTest.filter = (ScriptFilter) filter;
        synchronized (filterLock) {
            filterLock.notifyAll();
        }
    }

    protected void unbindFilter(WireComponent filter) {
        ScriptFilterTest.filter = null;
    }

    protected void bindCfgSvc(ConfigurationService cfgSvc) {
        ScriptFilterTest.cfgsvc = cfgSvc;
        dependencyLatch.countDown();
    }

    protected void unbindCfgSvc(ConfigurationService cfgSvc) {
        ScriptFilterTest.cfgsvc = null;
    }

    protected void bindWireGraphSvc(final WireGraphService wireGraphSvc) {
        ScriptFilterTest.wireGraphSvc = wireGraphSvc;
        dependencyLatch.countDown();
    }

    protected void unbindWireGraphSvc(WireGraphService wireGraphSvc) {
        ScriptFilterTest.wireGraphSvc = null;
    }

}
