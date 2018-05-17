/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.wire.fifo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FifoTest {

    private static final Logger logger = LoggerFactory.getLogger(FifoTest.class);

    @Test
    public void testMultiRetain() throws InterruptedException {
        CountDownLatch wiresLatch = new CountDownLatch(10);

        Fifo fifo = new Fifo();

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        fifo.bindWireHelperService(wireHelperServiceMock);

        WireSupport wireSupportMock = new WireSupport() {

            long ts = 0L;

            @Override
            public void updated(Wire wire, Object value) {
                logger.info("updated called");

            }

            @Override
            public void producersConnected(Wire[] wires) {
                logger.info("producersConnected called");

            }

            @Override
            public Object polled(Wire wire) {
                logger.info("polled called");
                return null;
            }

            @Override
            public void consumersConnected(Wire[] wires) {
                logger.info("consumersConnected called");
            }

            @Override
            public void emit(List<WireRecord> wireRecords) {
                WireRecord wireRecord = wireRecords.get(0);

                TypedValue<Long> typedValue = (TypedValue<Long>) wireRecord.getProperties().get("timestamp");
                long val = (long) typedValue.getValue();

                assertTrue(val > ts); // verify it's really FIFO
                ts = val;

                try {
                    Thread.sleep(20); // slow down a bit
                } catch (InterruptedException e) {
                }

                wiresLatch.countDown();
            }
        };
        when(wireHelperServiceMock.newWireSupport(fifo, null)).thenReturn(wireSupportMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("discard.envelopes", false);
        properties.put("queue.capacity", 5);

        fifo.activate(properties, mock(ComponentContext.class));

        for (int i = 0; i < 10; i++) {
            WireEnvelope wireEnvelope = createWireEnvelope();
            fifo.onWireReceive(wireEnvelope);
            Thread.sleep(2);
        }

        // give it > 200ms
        boolean ok = wiresLatch.await(300, TimeUnit.MILLISECONDS);
        assertTrue("Expected all envelopes to be processed", ok);
    }

    @Test
    public void testMultiDiscard() throws InterruptedException {
        CountDownLatch wiresLatch = new CountDownLatch(7); // one more than should be processed!

        Fifo fifo = new Fifo();

        WireHelperService wireHelperServiceMock = mock(WireHelperService.class);
        fifo.bindWireHelperService(wireHelperServiceMock);

        WireSupport wireSupportMock = new WireSupport() {

            long ts = 0L;

            @Override
            public void updated(Wire wire, Object value) {
                logger.info("updated called");

            }

            @Override
            public void producersConnected(Wire[] wires) {
                logger.info("producersConnected called");

            }

            @Override
            public Object polled(Wire wire) {
                logger.info("polled called");
                return null;
            }

            @Override
            public void consumersConnected(Wire[] wires) {
                logger.info("consumersConnected called");
            }

            @Override
            public void emit(List<WireRecord> wireRecords) {
                WireRecord wireRecord = wireRecords.get(0);

                TypedValue<Long> typedValue = (TypedValue<Long>) wireRecord.getProperties().get("timestamp");
                long val = (long) typedValue.getValue();

                assertTrue(val > ts); // verify it's really FIFO
                ts = val;

                try {
                    Thread.sleep(100); // slow down a bit
                } catch (InterruptedException e) {
                }

                wiresLatch.countDown();
            }
        };
        when(wireHelperServiceMock.newWireSupport(fifo, null)).thenReturn(wireSupportMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("discard.envelopes", true);
        properties.put("queue.capacity", 5);

        fifo.activate(properties, mock(ComponentContext.class));

        for (int i = 0; i < 10; i++) {
            WireEnvelope wireEnvelope = createWireEnvelope();
            fifo.onWireReceive(wireEnvelope);
            Thread.sleep(2);
        }

        assertFalse("Not all envelopes expected to be processed", wiresLatch.await(1000, TimeUnit.MILLISECONDS));
        assertEquals(1, wiresLatch.getCount());
    }

    private WireEnvelope createWireEnvelope() {
        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<WireRecord>();
        Map<String, TypedValue<?>> recordProps = new HashMap<String, TypedValue<?>>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("key", val);
        val = new LongValue(System.currentTimeMillis());
        recordProps.put("timestamp", val);
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);
        WireEnvelope wireEnvelope = new WireEnvelope(emitterPid, wireRecords);

        return wireEnvelope;
    }

}
