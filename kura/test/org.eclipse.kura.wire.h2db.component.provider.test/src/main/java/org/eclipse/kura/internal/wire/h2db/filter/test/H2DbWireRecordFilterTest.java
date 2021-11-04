/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.wire.h2db.filter.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.wire.test.GraphBuilder;
import org.eclipse.kura.util.wire.test.TestEmitterReceiver;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

public class H2DbWireRecordFilterTest {

    @Test
    public void shouldNotEmitPropertyOfUnsupportedType()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAColumnWithData("test", 1, 2, 3, 4, 5);

        whenQueryIsPerformed("SELECT MEDIAN(\"test\") FROM \"" + tableName + "\";");

        thenEmittedEnvelopeIsEmpty();
    }

    @Test
    public void shouldEmitPropertyAfterManualCastFromUnsupportedType()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAColumnWithData("test", 1, 2, 3, 4, 5);

        whenQueryIsPerformed("SELECT CAST(MEDIAN(\"test\") AS BIGINT) AS OUT FROM \"" + tableName + "\";");

        thenEmittedEnvelopeHasProperty("OUT", TypedValues.newLongValue(3));
    }

    private static final String STORE_EMITTER_PID = "storeEmitter";
    private static final String H2_DB_WIRE_RECORD_STORE_PID = "testDbStore";
    private static final String H2_DB_WIRE_RECORD_STORE_FACTORY_PID = "org.eclipse.kura.wire.H2DbWireRecordStore";
    private static final String H2DB_WIRE_RECORD_FILTER_PID = "testDbFilter";
    private static final String H2_DB_WIRE_RECORD_FILTER_FACTORY_PID = "org.eclipse.kura.wire.H2DbWireRecordFilter";
    private static final String TEST_RECEIVER_PID = "testReceiver";
    private static final String TEST_EMITTER_PID = "testEmitter";

    private final String tableName = Long.toString(System.currentTimeMillis());
    private final String h2DbServicePid = "store." + tableName;

    private final TestEmitterReceiver testStoreEmitter;
    private final TestEmitterReceiver testEmitter;
    private final TestEmitterReceiver testReceiver;

    private Optional<WireEnvelope> receivedEnvelope = Optional.empty();

    private final ConfigurationService configurationService;

    public H2DbWireRecordFilterTest()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        this.configurationService = WireTestUtil.trackService(ConfigurationService.class, Optional.empty()).get(30,
                TimeUnit.SECONDS);

        final Map<String, Object> h2DbServiceConfig = Collections.singletonMap("db.connector.url",
                "jdbc:h2:mem:db" + tableName);

        WireTestUtil.createFactoryConfiguration(configurationService, H2DbService.class, h2DbServicePid,
                "org.eclipse.kura.core.db.H2DbService", h2DbServiceConfig);

        final WireGraphService wireGraphService = WireTestUtil.trackService(WireGraphService.class, Optional.empty())
                .get(30, TimeUnit.SECONDS);

        final String h2DbServiceFilter = "(kura.service.pid=" + h2DbServicePid + ")";

        final Map<String, Object> storeConfig = new HashMap<>();
        storeConfig.put("table.name", tableName);
        storeConfig.put("H2DbService.target", h2DbServiceFilter);

        final Map<String, Object> filterConfig = Collections.singletonMap("H2DbService.target", h2DbServiceFilter);

        final GraphBuilder builder = new GraphBuilder().addTestEmitterReceiver(TEST_EMITTER_PID)
                .addWireComponent(H2_DB_WIRE_RECORD_STORE_PID, H2_DB_WIRE_RECORD_STORE_FACTORY_PID, storeConfig, 1, 1)
                .addWireComponent(H2DB_WIRE_RECORD_FILTER_PID, H2_DB_WIRE_RECORD_FILTER_FACTORY_PID, filterConfig, 1, 1)
                .addTestEmitterReceiver(TEST_RECEIVER_PID).addTestEmitterReceiver(STORE_EMITTER_PID)
                .addWire(TEST_EMITTER_PID, H2DB_WIRE_RECORD_FILTER_PID)
                .addWire(H2DB_WIRE_RECORD_FILTER_PID, TEST_RECEIVER_PID)
                .addWire(STORE_EMITTER_PID, H2_DB_WIRE_RECORD_STORE_PID);

        builder.replaceExistingGraph(FrameworkUtil.getBundle(H2DbWireRecordFilterTest.class).getBundleContext(),
                wireGraphService).get(30, TimeUnit.SECONDS);

        this.testEmitter = builder.getTrackedWireComponent(TEST_EMITTER_PID);
        this.testReceiver = builder.getTrackedWireComponent(TEST_RECEIVER_PID);
        this.testStoreEmitter = builder.getTrackedWireComponent(STORE_EMITTER_PID);
    }

    private void givenAColumnWithData(final String name, final Object... data) {
        for (final Object value : data) {
            this.testStoreEmitter.emit(Collections.singletonMap(name, TypedValues.newTypedValue(value)));
        }
    }

    private void whenQueryIsPerformed(final String sql)
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        WireTestUtil.updateComponentConfiguration(this.configurationService, H2DB_WIRE_RECORD_FILTER_PID,
                Collections.singletonMap("sql.view", sql)).get(30, TimeUnit.SECONDS);

        final CompletableFuture<WireEnvelope> nextEnvelope = this.testReceiver.nextEnvelope();

        this.testEmitter.emit();

        this.receivedEnvelope = Optional.of(nextEnvelope.get(30, TimeUnit.SECONDS));
    }

    private void thenEmittedEnvelopeIsEmpty() {
        final WireEnvelope envelope = this.receivedEnvelope
                .orElseThrow(() -> new IllegalStateException("no envelopes received"));

        if (envelope.getRecords().isEmpty()) {
            return;
        }

        final WireRecord record = envelope.getRecords().get(0);

        assertTrue(record.getProperties().isEmpty());
    }

    private void thenEmittedEnvelopeHasProperty(final String key, final TypedValue<?> value) {
        final WireEnvelope envelope = this.receivedEnvelope
                .orElseThrow(() -> new IllegalStateException("no envelopes received"));

        final WireRecord record = envelope.getRecords().get(0);

        assertEquals(Collections.singletonMap(key, value), record.getProperties());
    }

}
