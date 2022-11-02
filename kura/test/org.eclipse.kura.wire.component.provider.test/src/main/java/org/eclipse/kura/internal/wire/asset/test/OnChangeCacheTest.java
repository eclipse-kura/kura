/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.wire.asset.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.channel.listener.ChannelEvent;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.wire.test.GraphBuilder;
import org.eclipse.kura.util.wire.test.TestEmitterReceiver;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

public class OnChangeCacheTest {

    @Test
    public void shouldAywaysEmitAllValuesOnReadIfOnChangeIsDisalbed() {
        givenAssetWithChangeCacheDisabled();
        givenChannelValues("foo", "bar", "bar");
        givenChannelValues("bar", 15, 15);

        whenAssetReceivesEnvelopes(2);

        thenAssetOutputContains(0, "assetName", "testAsset");
        thenAssetOutputContains(0, "foo", "bar");
        thenAssetOutputContainsKey(0, "foo_timestamp");
        thenAssetOutputContains(0, "bar", 15);
        thenAssetOutputContainsKey(0, "bar_timestamp");

        thenAssetOutputContains(1, "assetName", "testAsset");
        thenAssetOutputContains(1, "foo", "bar");
        thenAssetOutputContainsKey(1, "foo_timestamp");
        thenAssetOutputContains(1, "bar", 15);
        thenAssetOutputContainsKey(1, "bar_timestamp");
    }

    @Test
    public void shouldAywaysEmitEventsIfOnChangeIsDisalbed() {
        givenAssetWithChangeCacheDisabled();

        whenDriverEmitsEvents(
                "foo", "bar", //
                "bar", 15, //
                "foo", "bar", //
                "bar", 15 //
        );

        thenAssetOutputContains(0, "assetName", "testAsset");
        thenAssetOutputContains(0, "foo", "bar");
        thenAssetOutputContainsKey(0, "foo_timestamp");

        thenAssetOutputContains(1, "assetName", "testAsset");
        thenAssetOutputContains(1, "bar", 15);
        thenAssetOutputContainsKey(1, "bar_timestamp");

        thenAssetOutputContains(2, "assetName", "testAsset");
        thenAssetOutputContains(2, "foo", "bar");
        thenAssetOutputContainsKey(2, "foo_timestamp");

        thenAssetOutputContains(3, "assetName", "testAsset");
        thenAssetOutputContains(3, "bar", 15);
        thenAssetOutputContainsKey(3, "bar_timestamp");

    }

    @Test
    public void shouldNotEmitSameValuesAgainOnReadIfNotChanged() {
        givenAssetWithChangeCacheEnabledAndEmitEmptyEnvelopesSetTo(false);
        givenChannelValues("foo", "bar", "bar");
        givenChannelValues("bar", 15, 15);

        whenAssetReceivesEnvelopes(2);

        thenAssetOutputContains(0, "assetName", "testAsset");
        thenAssetOutputContains(0, "foo", "bar");
        thenAssetOutputContainsKey(0, "foo_timestamp");
        thenAssetOutputContains(0, "bar", 15);
        thenAssetOutputContainsKey(0, "bar_timestamp");

        thenTotalEmittedEnvelopeCountAfter1SecIs(1);
    }

    @Test
    public void shouldNotEmitSameValuesAgainOnChannelEventIfNotChanged() {
        givenAssetWithChangeCacheEnabledAndEmitEmptyEnvelopesSetTo(false);

        whenDriverEmitsEvents(
                "foo", "bar", //
                "bar", 15, //
                "foo", "bar", //
                "bar", 15 //
        );

        thenAssetOutputContains(0, "assetName", "testAsset");
        thenAssetOutputContains(0, "foo", "bar");
        thenAssetOutputContainsKey(0, "foo_timestamp");

        thenAssetOutputContains(1, "assetName", "testAsset");
        thenAssetOutputContains(1, "bar", 15);
        thenAssetOutputContainsKey(1, "bar_timestamp");

        thenTotalEmittedEnvelopeCountAfter1SecIs(2);
    }

    @Test
    public void shouldEmitOnlyChangedValuesOnRead() {
        givenAssetWithChangeCacheEnabledAndEmitEmptyEnvelopesSetTo(false);
        givenChannelValues("foo", "bar", "bar");
        givenChannelValues("bar", 15, 16);

        whenAssetReceivesEnvelopes(2);

        thenAssetOutputContains(0, "assetName", "testAsset");
        thenAssetOutputContains(0, "foo", "bar");
        thenAssetOutputContainsKey(0, "foo_timestamp");
        thenAssetOutputContains(0, "bar", 15);
        thenAssetOutputContainsKey(0, "bar_timestamp");

        thenAssetOutputContains(1, "assetName", "testAsset");
        thenAssetOutputDoesNotContain(1, "foo");
        thenAssetOutputDoesNotContain(1, "foo_timestamp");
        thenAssetOutputContains(1, "bar", 16);
        thenAssetOutputContainsKey(1, "bar_timestamp");

    }

    @Test
    public void shouldNotEmitOnlyChangedValuesOnChannelEvent() {
        givenAssetWithChangeCacheEnabledAndEmitEmptyEnvelopesSetTo(false);

        whenDriverEmitsEvents(
                "foo", "bar", //
                "bar", 15, //
                "foo", "bar", //
                "bar", 15, //
                "foo", "baz", //
                "bar", 16, //
                "foo", "baz", //
                "bar", 16 //
        );

        thenAssetOutputContains(0, "assetName", "testAsset");
        thenAssetOutputContains(0, "foo", "bar");
        thenAssetOutputContainsKey(0, "foo_timestamp");

        thenAssetOutputContains(1, "assetName", "testAsset");
        thenAssetOutputContains(1, "bar", 15);
        thenAssetOutputContainsKey(1, "bar_timestamp");

        thenAssetOutputContains(2, "assetName", "testAsset");
        thenAssetOutputContains(2, "foo", "baz");
        thenAssetOutputContainsKey(2, "foo_timestamp");

        thenAssetOutputContains(3, "assetName", "testAsset");
        thenAssetOutputContains(3, "bar", 16);
        thenAssetOutputContainsKey(3, "bar_timestamp");

        thenTotalEmittedEnvelopeCountAfter1SecIs(4);
    }

    private static final String WIRE_ASSET_FACTORY_PID = "org.eclipse.kura.wire.WireAsset";
    private static final String LOGGER_FACTORY_PID = "org.eclipse.kura.wire.Logger";
    private static final Map<String, Object> TEST_ASSET_CONFIG = new HashMap<>();
    private static Optional<ServiceRegistration<Driver>> driverRegistration = Optional.empty();

    private TestEmitterReceiver testReceiver;
    private TestEmitterReceiver testEmitter;
    private MockDriver driver = new MockDriver();

    private List<WireEnvelope> envelopes = new ArrayList<>();

    static {
        TEST_ASSET_CONFIG.put("driver.pid", "testDriver");
        TEST_ASSET_CONFIG.put("foo#+name", "foo");
        TEST_ASSET_CONFIG.put("foo#+type", ChannelType.READ.name());
        TEST_ASSET_CONFIG.put("foo#+value.type", DataType.STRING.name());
        TEST_ASSET_CONFIG.put("foo#+enabled", true);
        TEST_ASSET_CONFIG.put("foo#+listen", true);
        TEST_ASSET_CONFIG.put("bar#+name", "bar");
        TEST_ASSET_CONFIG.put("bar#+type", ChannelType.READ.name());
        TEST_ASSET_CONFIG.put("bar#+value.type", DataType.INTEGER.name());
        TEST_ASSET_CONFIG.put("bar#+enabled", true);
        TEST_ASSET_CONFIG.put("bar#+listen", true);
    }

    private void givenAssetWithChangeCacheDisabled() {
        givenAssetConfig(TEST_ASSET_CONFIG);
    }

    private void givenAssetWithChangeCacheEnabledAndEmitEmptyEnvelopesSetTo(final boolean emitEmptyEnvelopes) {
        final Map<String, Object> assetConfig = new HashMap<>(TEST_ASSET_CONFIG);
        assetConfig.put("emit.on.change", true);
        assetConfig.put("emit.empty.envelopes", emitEmptyEnvelopes);

        givenAssetConfig(assetConfig);
    }

    private void givenAssetConfig(final Map<String, Object> assetConfig) {
        try {
            if (driverRegistration.isPresent()) {
                driverRegistration.get().unregister();
            }

            final BundleContext bundleContext = FrameworkUtil.getBundle(OnChangeCacheTest.class).getBundleContext();

            final WireGraphService wireGraphService = WireTestUtil
                    .trackService(WireGraphService.class, Optional.empty())
                    .get(30, TimeUnit.SECONDS);

            final GraphBuilder graphBuilder = new GraphBuilder().addTestEmitterReceiver("emitter")
                    .addTestEmitterReceiver("receiver")
                    .addWireComponent("testAsset", WIRE_ASSET_FACTORY_PID, assetConfig, 1, 1)
                    .addWireComponent("testLogger", LOGGER_FACTORY_PID,
                            Collections.singletonMap("log.verbosity", "VERBOSE"), 1, 0)
                    .addWire("emitter", "testAsset").addWire("testAsset", "receiver")
                    .addWire("testAsset", "testLogger");

            graphBuilder.replaceExistingGraph(bundleContext, wireGraphService).get(30, TimeUnit.SECONDS);

            testEmitter = graphBuilder.getTrackedWireComponent("emitter");
            testReceiver = graphBuilder.getTrackedWireComponent("receiver");

            testReceiver.setConsumer(e -> {
                synchronized (envelopes) {
                    this.envelopes.add(e);
                    this.envelopes.notifyAll();
                }
            });

            final Dictionary<String, Object> properties = new Hashtable<>();
            properties.put("kura.service.pid", "testDriver");

            driverRegistration = Optional.of(bundleContext.registerService(Driver.class, driver, properties));

            try {
                driver.preparedReadCalled.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                fail("driver not ready");
            }

            synchronized (driver.listeners) {
                while (driver.listeners.size() != 2) {
                    driver.listeners.wait(30000);
                }
            }

        } catch (final Exception e) {
            throw new IllegalStateException("failed to setup test graph", e);
        }
    }

    private void givenChannelValue(final String key, final Object value) {
        driver.addReadResult(key, TypedValues.newTypedValue(value));
    }

    private void givenChannelValues(final String key, final Object... values) {
        for (final Object value : values) {
            givenChannelValue(key, value);
        }
    }

    private void whenAssetReceivesEnvelope() {

        testEmitter.emit();
    }

    private void whenDriverEmitsEvents(final Object... values) {
        final Iterator<Object> iter = Arrays.asList(values).iterator();

        while (iter.hasNext()) {
            final String channelName = (String) iter.next();
            final TypedValue<?> value = TypedValues.newTypedValue(iter.next());

            driver.emitChannelEvent(channelName, value);
        }
    }

    private void whenAssetReceivesEnvelopes(final int count) {
        for (int i = 0; i < count; i++) {
            whenAssetReceivesEnvelope();
        }
    }

    private void awaitEnvelope(final int index) {
        synchronized (envelopes) {
            if (index >= envelopes.size()) {
                try {
                    envelopes.wait(30000);
                } catch (InterruptedException e) {
                    throw new IllegalStateException("Interrupted while waiting for envelope");
                }

                if (index >= envelopes.size()) {
                    fail("expected to receive at least " + (index + 1) + " envelopes");
                }
            }
        }
    }

    private void thenAssetOutputContains(final int index, final Object... properties) {
        awaitEnvelope(index);

        final WireEnvelope envelope = envelopes.get(index);

        final Iterator<Object> iter = Arrays.asList(properties).iterator();

        while (iter.hasNext()) {
            final String key = (String) iter.next();
            final TypedValue<?> value = TypedValues.newTypedValue(iter.next());

            assertEquals(value, envelope.getRecords().get(0).getProperties().get(key));
        }
    }

    private void thenAssetOutputContainsKey(final int index, final String key) {
        awaitEnvelope(index);

        final WireEnvelope envelope = envelopes.get(index);

        assertTrue(envelope.getRecords().get(0).getProperties().containsKey(key));
    }

    private void thenAssetOutputPropertyCountIs(final int index, final int expectedCount) {
        awaitEnvelope(index);

        final WireEnvelope envelope = envelopes.get(index);

        assertEquals(expectedCount, envelope.getRecords().get(0).getProperties().size());
    }

    private void thenAssetOutputDoesNotContain(final int index, final String... properties) {
        awaitEnvelope(index);

        final WireEnvelope envelope = envelopes.get(index);

        final Iterator<String> iter = Arrays.asList(properties).iterator();

        while (iter.hasNext()) {
            final String key = (String) iter.next();

            assertFalse(envelope.getRecords().get(0).getProperties().containsKey(key));
        }
    }

    private void thenTotalEmittedEnvelopeCountAfter1SecIs(final int expectedCount) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new IllegalStateException("sleep interrupted");
        }

        assertEquals(expectedCount, envelopes.size());
    }

    private static class MockDriver implements Driver {

        private final Map<String, List<TypedValue<?>>> values = new HashMap<>();
        private final Map<String, ChannelListener> listeners = new HashMap<>();
        private CompletableFuture<Void> preparedReadCalled = new CompletableFuture<>();

        @Override
        public void connect() throws ConnectionException {
        }

        @Override
        public void disconnect() throws ConnectionException {
        }

        @Override
        public ChannelDescriptor getChannelDescriptor() {
            // TODO Auto-generated method stub
            return new ChannelDescriptor() {

                @Override
                public Object getDescriptor() {
                    return Collections.emptyList();
                }
            };
        }

        @Override
        public void read(List<ChannelRecord> records) throws ConnectionException {
            for (final ChannelRecord record : records) {
                final Optional<TypedValue<?>> value = Optional.ofNullable(values.get(record.getChannelName()))
                        .flatMap(l -> {
                            if (l.isEmpty()) {
                                return Optional.empty();
                            } else {
                                return Optional.of(l.remove(0));
                            }
                        });

                if (value.isPresent()) {
                    record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
                    record.setValue(value.get());
                } else {
                    record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE));
                }

                record.setTimestamp(System.currentTimeMillis());
            }

        }

        @Override
        public synchronized void registerChannelListener(Map<String, Object> channelConfig, ChannelListener listener)
                throws ConnectionException {
            synchronized (listeners) {
                listeners.put((String) channelConfig.get("+name"), listener);
                listeners.notifyAll();
            }
        }

        @Override
        public void unregisterChannelListener(ChannelListener listener) throws ConnectionException {
            synchronized (listeners) {
                final Iterator<Entry<String, ChannelListener>> iter = listeners.entrySet().iterator();

                while (iter.hasNext()) {
                    final Entry<String, ChannelListener> e = iter.next();

                    if (e.getValue() == listener) {
                        iter.remove();
                    }
                }
            }
        }

        @Override
        public void write(List<ChannelRecord> records) throws ConnectionException {
        }

        synchronized void addReadResult(final String channelName, final TypedValue<?> value) {
            this.values.computeIfAbsent(channelName, a -> new ArrayList<>()).add(value);
        }

        synchronized void emitChannelEvent(final String channelName, final TypedValue<?> value) {
            for (final Entry<String, ChannelListener> e : listeners.entrySet()) {

                if (!e.getKey().equals(channelName)) {
                    continue;
                }

                final ChannelRecord record = ChannelRecord.createReadRecord(channelName, value.getType());
                record.setValue(value);
                record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
                record.setTimestamp(System.currentTimeMillis());

                e.getValue().onChannelEvent(new ChannelEvent(record));
            }
        }

        @Override
        public PreparedRead prepareRead(List<ChannelRecord> records) {
            preparedReadCalled.complete(null);

            throw new UnsupportedOperationException();
        }

    }

}
