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
 * 
 *******************************************************************************/
package org.eclipse.kura.internal.wire.db.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

@RunWith(Parameterized.class)
public class DbWireComponentsTest {

    @Test
    public void shouldNotEmitPropertyOfUnsupportedType()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAColumnWithData("test", 1, 2, 3, 4, 5, 6);

        whenQueryIsPerformed("SELECT MEDIAN(\"test\") FROM \"" + tableName + "\";");

        thenEmittedEnvelopeIsEmpty();
    }

    @Test
    public void shouldEmitPropertyAfterManualCastFromUnsupportedType()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAColumnWithData("test", 1, 2, 3, 4, 5);

        whenQueryIsPerformed("SELECT CAST(MEDIAN(\"test\") AS BIGINT) AS OUT FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty("OUT", TypedValues.newLongValue(3));
    }

    @Test
    public void shouldSupportInteger()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(23));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty("foo", TypedValues.newIntegerValue(23));
    }

    @Test
    public void shouldSupportLong()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newLongValue(23));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty("foo", TypedValues.newLongValue(23));
    }

    @Test
    public void shouldSupportBoolean()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newBooleanValue(true), "bar",
                TypedValues.newBooleanValue(false));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty("foo", TypedValues.newBooleanValue(true));
        thenFilterEmitsEnvelopeWithProperty("bar", TypedValues.newBooleanValue(false));
    }

    @Test
    public void shouldSupportDouble()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newDoubleValue(1234.5d));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty("foo", TypedValues.newDoubleValue(1234.5d));
    }

    @Test
    public void shouldSupportFloat()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newFloatValue(1234.5f));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty("foo", TypedValues.newDoubleValue(1234.5d));
    }

    @Test
    public void shouldSupportString()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newStringValue("bar"));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty("foo", TypedValues.newStringValue("bar"));
    }

    @Test
    public void shouldSupportByteArray()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newByteArrayValue(new byte[] { 1, 2, 3 }));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithByteArrayProperty("foo", new byte[] { 1, 2, 3 });
    }

    @Test
    public void shouldSupportMultipleEnvelopes()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(23));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(24));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(25));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty(0, "foo", TypedValues.newIntegerValue(23));
        thenFilterEmitsEnvelopeWithProperty(1, "foo", TypedValues.newIntegerValue(24));
        thenFilterEmitsEnvelopeWithProperty(2, "foo", TypedValues.newIntegerValue(25));
    }

    @Test
    public void shouldEmitEmptyEnvelopesByDefault()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(23));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\" LIMIT 0;");

        thenFilterEmitsEmptyEnvelope();
    }

    @Test
    public void shouldNotEmitEmptyEnvelopesIfConfigured()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenFilterWithConfig(this.target.filterEmitOnEmptyResultKey(), false);
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(23));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\" LIMIT 0;");

        thenFilterEmitNoEnvelope();
    }

    @Test
    public void shouldSupportCacheExpirationInterval()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenFilterWithConfig(this.target.filterCacheExpirationIntervalKey(), 5);
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(23));
        givenPerformedQuery("SELECT * FROM \"" + tableName + "\";");
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(24));
        givenPerformedQuery("SELECT * FROM \"" + tableName + "\";");

        whenTimePasses(6, TimeUnit.SECONDS);
        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\" ORDER BY ID ASC;");

        thenFilterEmitsEnvelopeWithProperty(0, 0, "foo", TypedValues.newIntegerValue(23));
        thenFilterEmitsEnvelopeWithProperty(1, 0, "foo", TypedValues.newIntegerValue(23));
        thenFilterEmitsEnvelopeWithProperty(2, 0, "foo", TypedValues.newIntegerValue(23));
        thenFilterEmitsEnvelopeWithProperty(2, 1, "foo", TypedValues.newIntegerValue(24));
    }

    @Test
    public void shouldSupportMaximumTableSize()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        givenStoreWithConfig(this.target.storeMaximumSizeKey(), 5);
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(1));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(2));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(3));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(4));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(5));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(6));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(7));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\" ORDER BY ID DESC;");

        thenEnvelopeRecordCountIs(0, 5);
        thenFilterEmitsEnvelopeWithProperty(0, 0, "foo", TypedValues.newIntegerValue(7));
        thenFilterEmitsEnvelopeWithProperty(0, 1, "foo", TypedValues.newIntegerValue(6));
        thenFilterEmitsEnvelopeWithProperty(0, 2, "foo", TypedValues.newIntegerValue(5));
        thenFilterEmitsEnvelopeWithProperty(0, 3, "foo", TypedValues.newIntegerValue(4));
        thenFilterEmitsEnvelopeWithProperty(0, 4, "foo", TypedValues.newIntegerValue(3));
    }

    @Test
    public void shouldSupportCleanupRecordKeep()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        givenStoreWithConfig(this.target.storeMaximumSizeKey(), 5, this.target.storeCleanupRecordsKeepKey(), 2);
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(1));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(2));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(3));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(4));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(5));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(6));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\" ORDER BY ID DESC;");

        thenEnvelopeRecordCountIs(0, 2);
        thenFilterEmitsEnvelopeWithProperty(0, 0, "foo", TypedValues.newIntegerValue(6));
        thenFilterEmitsEnvelopeWithProperty(0, 1, "foo", TypedValues.newIntegerValue(5));
    }

    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    private final String tableName;
    private final String h2DbServicePid;

    private final TestTarget target;

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

    @Parameters
    public static Collection<TestTarget> targets() {
        return Arrays.asList(TestTarget.WIRE_RECORD_QUERY_AND_WIRE_RECORD_STORE, TestTarget.DB_FILTER_AND_DB_STORE);
    }

    public DbWireComponentsTest(final TestTarget target)
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        this.target = target;

        final int nextId = NEXT_ID.incrementAndGet();

        this.storeEmitterPid = "storeEmitter" + nextId;
        this.testStorePid = "testDbStore" + nextId;
        this.testFilterPid = "testDbFilter" + nextId;
        this.testReceiverPid = "testReceiver" + nextId;
        this.testEmitterPid = "testEmitter" + nextId;
        this.tableName = "testTable" + nextId;
        this.h2DbServicePid = "h2db" + nextId;

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
        storeConfig.put(target.storeNameKey(), tableName);
        storeConfig.put(target.storeReferenceKey(), h2DbServiceFilter);

        final Map<String, Object> filterConfig = Collections.singletonMap(target.filterReferenceKey(),
                h2DbServiceFilter);

        final GraphBuilder builder = new GraphBuilder().addTestEmitterReceiver(this.testEmitterPid)
                .addWireComponent(this.testStorePid, target.storeFactoryPid(), storeConfig, 1, 1)
                .addWireComponent(this.testFilterPid, target.filterFactoryPid(), filterConfig, 1, 1)
                .addTestEmitterReceiver(this.testReceiverPid).addTestEmitterReceiver(this.storeEmitterPid)
                .addWire(this.testEmitterPid, this.testFilterPid).addWire(this.testFilterPid, this.testReceiverPid)
                .addWire(this.storeEmitterPid, this.testStorePid);

        builder.replaceExistingGraph(FrameworkUtil.getBundle(DbWireComponentsTest.class).getBundleContext(),
                wireGraphService).get(30, TimeUnit.SECONDS);

        this.testEmitter = builder.getTrackedWireComponent(this.testEmitterPid);
        this.testReceiver = builder.getTrackedWireComponent(this.testReceiverPid);
        this.testStoreEmitter = builder.getTrackedWireComponent(this.storeEmitterPid);
    }

    private void givenAColumnWithData(final String name, final Object... data) {
        for (final Object value : data) {
            this.testStoreEmitter.emit(Collections.singletonMap(name, TypedValues.newTypedValue(value)));
        }
    }

    private void givenAnEnvelopeReceivedByStore(final Object... args) {

        this.testStoreEmitter.emit(collectArgsToMap(args, TypedValue.class::cast));
    }

    private void givenStoreWithConfig(final Object... args)
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        givenComponentWithConfig(this.testStorePid, collectArgsToMap(args, Function.identity()));
    }

    private void givenFilterWithConfig(final Object... args)
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        givenComponentWithConfig(this.testFilterPid, collectArgsToMap(args, Function.identity()));
    }

    private void givenComponentWithConfig(final String pid, final Map<String, Object> properties)
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        WireTestUtil.updateComponentConfiguration(configurationService, pid, properties).get(30, TimeUnit.SECONDS);
    }

    private void givenPerformedQuery(final String sql)
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        whenQueryIsPerformed(sql);
    }

    private void whenQueryIsPerformed(final String sql)
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        WireTestUtil.updateComponentConfiguration(this.configurationService, this.testFilterPid,
                Collections.singletonMap(target.filterQueryPropertyKey(), sql)).get(30, TimeUnit.SECONDS);

        final CompletableFuture<WireEnvelope> nextEnvelope = this.testReceiver.nextEnvelope();

        this.testEmitter.emit();

        try {
            this.receivedEnvelopes.add(nextEnvelope.get(1, TimeUnit.SECONDS));
        } catch (final TimeoutException e) {
            // do nothing
        }
    }

    private void whenTimePasses(final long amount, final TimeUnit timeUnit) throws InterruptedException {
        Thread.sleep(timeUnit.toMillis(amount));
    }

    private void thenEmittedEnvelopeIsEmpty() {
        final WireEnvelope envelope = this.receivedEnvelopes.get(0);

        if (envelope.getRecords().isEmpty()) {
            return;
        }

        final WireRecord record = envelope.getRecords().get(0);

        assertTrue(record.getProperties().isEmpty());
    }

    private void thenFilterEmitsEnvelopeWithProperty(final String key, final TypedValue<?> value) {
        thenFilterEmitsEnvelopeWithProperty(0, 0, key, value);
    }

    private void thenFilterEmitsEnvelopeWithProperty(final int recordIndex, final String key,
            final TypedValue<?> value) {
        thenFilterEmitsEnvelopeWithProperty(0, recordIndex, key, value);
    }

    private void thenFilterEmitsEnvelopeWithProperty(final int envelopeIndex, final int recordIndex, final String key,
            final TypedValue<?> value) {
        final WireEnvelope envelope = this.receivedEnvelopes.get(envelopeIndex);

        final WireRecord record = envelope.getRecords().get(recordIndex);

        assertEquals(value, record.getProperties().get(key));
    }

    private void thenFilterEmitsEnvelopeWithByteArrayProperty(final String key, final byte[] value) {
        thenFilterEmitsEnvelopeWithByteArrayProperty(0, 0, key, value);
    }

    private void thenFilterEmitsEnvelopeWithByteArrayProperty(final int recordIndex, final String key,
            final byte[] value) {
        thenFilterEmitsEnvelopeWithByteArrayProperty(0, recordIndex, key, value);
    }

    private void thenFilterEmitsEnvelopeWithByteArrayProperty(final int envelopeIndex, final int recordIndex,
            final String key, final byte[] value) {
        final WireEnvelope envelope = this.receivedEnvelopes.get(envelopeIndex);

        final WireRecord record = envelope.getRecords().get(recordIndex);

        assertArrayEquals(value, (byte[]) record.getProperties().get(key).getValue());
    }

    private void thenFilterEmitsEmptyEnvelope() {
        final WireEnvelope envelope = this.receivedEnvelopes.get(0);

        assertEquals(0, envelope.getRecords().size());
    }

    private void thenFilterEmitNoEnvelope() {
        assertEquals(0, this.receivedEnvelopes.size());
    }

    private void thenEnvelopeRecordCountIs(final int envelopeIndex, final int recordCount) {
        assertEquals(recordCount, this.receivedEnvelopes.get(envelopeIndex).getRecords().size());
    }

    private <T> Map<String, T> collectArgsToMap(final Object[] args, final Function<Object, T> valueMapper) {
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
