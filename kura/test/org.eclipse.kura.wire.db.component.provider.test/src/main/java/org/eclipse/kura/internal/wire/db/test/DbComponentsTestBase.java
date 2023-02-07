/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.wire.db.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.db.BaseDbService;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.wire.test.GraphBuilder;
import org.eclipse.kura.util.wire.test.TestEmitterReceiver;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.junit.After;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

public class DbComponentsTestBase {

    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    protected final String tableName;
    protected final String dbServicePid;

    protected final WireComponentTestTarget wireComponentTestTarget;
    protected final StoreTestTarget storeTestTarget;

    private final TestEmitterReceiver testStoreEmitter;
    private final TestEmitterReceiver testEmitter;
    private final TestEmitterReceiver testReceiver;

    private final String storeEmitterPid;
    private final String testStorePid;
    private final String testFilterPid;
    private final String testReceiverPid;
    private final String testEmitterPid;

    private List<WireEnvelope> receivedEnvelopes = new ArrayList<>();

    private final ConfigurationService configurationService;

    public DbComponentsTestBase(final WireComponentTestTarget wireComponentTestTarget,
            final StoreTestTarget storeTestTarget)
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        this.wireComponentTestTarget = wireComponentTestTarget;
        this.storeTestTarget = storeTestTarget;

        final String suffix = getClass().getSimpleName() + "-" + NEXT_ID.incrementAndGet();

        this.storeEmitterPid = "storeEmitter" + suffix;
        this.testStorePid = "testDbStore" + suffix;
        this.testFilterPid = "testDbFilter" + suffix;
        this.testReceiverPid = "testReceiver" + suffix;
        this.testEmitterPid = "testEmitter" + suffix;
        this.tableName = "testTable" + suffix;
        this.dbServicePid = "db" + suffix;

        this.configurationService = WireTestUtil.trackService(ConfigurationService.class, Optional.empty()).get(30,
                TimeUnit.SECONDS);

        WireTestUtil.createFactoryConfiguration(configurationService, BaseDbService.class, dbServicePid,
                this.storeTestTarget.factoryPid(), this.storeTestTarget.getConfigurationForDatabase(dbServicePid));

        final WireGraphService wireGraphService = WireTestUtil.trackService(WireGraphService.class, Optional.empty())
                .get(30, TimeUnit.SECONDS);

        final String h2DbServiceFilter = "(kura.service.pid=" + dbServicePid + ")";

        final Map<String, Object> storeConfig = new HashMap<>();
        storeConfig.put(wireComponentTestTarget.storeNameKey(), tableName);
        storeConfig.put(wireComponentTestTarget.storeReferenceKey(), h2DbServiceFilter);

        final Map<String, Object> filterConfig = Collections.singletonMap(wireComponentTestTarget.filterReferenceKey(),
                h2DbServiceFilter);

        final GraphBuilder builder = new GraphBuilder().addTestEmitterReceiver(this.testEmitterPid)
                .addWireComponent(this.testStorePid, wireComponentTestTarget.storeFactoryPid(), storeConfig, 1, 1)
                .addWireComponent(this.testFilterPid, wireComponentTestTarget.filterFactoryPid(), filterConfig, 1, 1)
                .addTestEmitterReceiver(this.testReceiverPid).addTestEmitterReceiver(this.storeEmitterPid)
                .addWire(this.testEmitterPid, this.testFilterPid).addWire(this.testFilterPid, this.testReceiverPid)
                .addWire(this.storeEmitterPid, this.testStorePid);

        builder.replaceExistingGraph(FrameworkUtil.getBundle(DbWireComponentsTest.class).getBundleContext(),
                wireGraphService).get(30, TimeUnit.SECONDS);

        this.testEmitter = builder.getTrackedWireComponent(this.testEmitterPid);
        this.testReceiver = builder.getTrackedWireComponent(this.testReceiverPid);
        this.testStoreEmitter = builder.getTrackedWireComponent(this.storeEmitterPid);
    }

    @After
    public void cleanUp()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        final WireGraphService wireGraphService = WireTestUtil.trackService(WireGraphService.class, Optional.empty())
                .get(30, TimeUnit.SECONDS);

        new GraphBuilder().replaceExistingGraph(FrameworkUtil.getBundle(DbWireComponentsTest.class).getBundleContext(),
                wireGraphService).get(30, TimeUnit.SECONDS);

        WireTestUtil.deleteFactoryConfiguration(this.configurationService, dbServicePid).get(30, TimeUnit.SECONDS);
    }

    protected void givenAColumnWithData(final String name, final Object... data) {
        for (final Object value : data) {
            this.testStoreEmitter.emit(Collections.singletonMap(name, TypedValues.newTypedValue(value)));
        }
    }

    protected void givenAnEnvelopeReceivedByStore(final Object... args) {

        this.testStoreEmitter.emit(collectArgsToMap(args, TypedValue.class::cast));
    }

    protected void givenStoreWithConfig(final Object... args)
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        givenComponentWithConfig(this.testStorePid, collectArgsToMap(args, Function.identity()));
    }

    protected void givenFilterWithConfig(final Object... args)
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        givenComponentWithConfig(this.testFilterPid, collectArgsToMap(args, Function.identity()));
    }

    protected void whenDatabaseIsReconfigured()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        WireTestUtil
                .updateComponentConfiguration(this.configurationService, dbServicePid,
                        this.storeTestTarget.getConfigurationForDatabase(System.nanoTime() + ""))
                .get(30, TimeUnit.SECONDS);
    }

    protected void givenComponentWithConfig(final String pid, final Map<String, Object> properties)
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        WireTestUtil.updateComponentConfiguration(configurationService, pid, properties).get(30, TimeUnit.SECONDS);
    }

    protected void givenPerformedQuery(final String sql)
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        whenQueryIsPerformed(sql);
    }

    protected void whenQueryIsPerformed(final String sql)
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        WireTestUtil
                .updateComponentConfiguration(this.configurationService, this.testFilterPid,
                        Collections.singletonMap(wireComponentTestTarget.filterQueryPropertyKey(), sql))
                .get(30, TimeUnit.SECONDS);

        final CompletableFuture<WireEnvelope> nextEnvelope = this.testReceiver.nextEnvelope();

        this.testEmitter.emit();

        try {
            this.receivedEnvelopes.add(nextEnvelope.get(1, TimeUnit.SECONDS));
        } catch (final TimeoutException e) {
            // do nothing
        }
    }

    protected void whenTimePasses(final long amount, final TimeUnit timeUnit) throws InterruptedException {
        Thread.sleep(timeUnit.toMillis(amount));
    }

    protected void thenEmittedEnvelopeIsEmpty() {
        final WireEnvelope envelope = this.receivedEnvelopes.get(0);

        if (envelope.getRecords().isEmpty()) {
            return;
        }

        final WireRecord record = envelope.getRecords().get(0);

        assertTrue(record.getProperties().isEmpty());
    }

    protected void thenEmittedRecordCountIs(final int expectedValue) {
        assertEquals(expectedValue, this.receivedEnvelopes.get(0).getRecords().size());
    }

    protected void thenFilterEmitsEnvelopeWithProperty(final String key, final TypedValue<?> value) {
        thenFilterEmitsEnvelopeWithProperty(0, 0, key, value);
    }

    protected void thenFilterEmitsEnvelopeWithProperty(final int recordIndex, final String key,
            final TypedValue<?> value) {
        thenFilterEmitsEnvelopeWithProperty(0, recordIndex, key, value);
    }

    protected void thenFilterEmitsEnvelopeWithProperty(final int envelopeIndex, final int recordIndex, final String key,
            final TypedValue<?> value) {
        final WireEnvelope envelope = this.receivedEnvelopes.get(envelopeIndex);

        final WireRecord record = envelope.getRecords().get(recordIndex);

        assertEquals(value, record.getProperties().get(key));
    }

    protected void thenFilterEmitsEnvelopeWithoutProperty(final int envelopeIndex, final int recordIndex,
            final String key) {
        final WireEnvelope envelope = this.receivedEnvelopes.get(envelopeIndex);

        final WireRecord record = envelope.getRecords().get(recordIndex);

        if (record.getProperties().containsKey(key)) {
            fail("record contains property \"" + key + "\" with value: " + record.getProperties().get(key)
                    + " envelope records: "
                    + envelope.getRecords().stream().map(WireRecord::getProperties).collect(Collectors.toList()));
        }
    }

    protected void thenFilterEmitsEnvelopeWithByteArrayProperty(final String key, final byte[] value) {
        thenFilterEmitsEnvelopeWithByteArrayProperty(0, 0, key, value);
    }

    protected void thenFilterEmitsEnvelopeWithByteArrayProperty(final int envelopeIndex, final int recordIndex,
            final String key, final byte[] value) {
        final WireEnvelope envelope = this.receivedEnvelopes.get(envelopeIndex);

        final WireRecord record = envelope.getRecords().get(recordIndex);

        assertArrayEquals(value, (byte[]) record.getProperties().get(key).getValue());
    }

    protected void thenFilterEmitsEmptyEnvelope() {
        final WireEnvelope envelope = this.receivedEnvelopes.get(0);

        assertEquals(0, envelope.getRecords().size());
    }

    protected void thenFilterEmitNoEnvelope() {
        assertEquals(0, this.receivedEnvelopes.size());
    }

    protected void thenEnvelopeRecordCountIs(final int envelopeIndex, final int recordCount) {
        assertEquals(recordCount, this.receivedEnvelopes.get(envelopeIndex).getRecords().size());
    }

    protected <T> Map<String, T> collectArgsToMap(final Object[] args, final Function<Object, T> valueMapper) {
        final Iterator<Object> iter = Arrays.asList(args).iterator();
        final Map<String, T> properties = new HashMap<>();

        while (iter.hasNext()) {
            final String key = (String) iter.next();
            final T value = valueMapper.apply(iter.next());

            properties.put(key, value);
        }

        return properties;
    }
}
