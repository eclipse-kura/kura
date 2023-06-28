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
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.wire.test.GraphBuilder;
import org.eclipse.kura.util.wire.test.TestEmitterReceiver;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

public class WireAssetTestBase {

    private static final String WIRE_ASSET_FACTORY_PID = "org.eclipse.kura.wire.WireAsset";
    private static final String LOGGER_FACTORY_PID = "org.eclipse.kura.wire.Logger";
    private static Optional<ServiceRegistration<Driver>> driverRegistration = Optional.empty();

    private TestEmitterReceiver testReceiver;
    private TestEmitterReceiver testEmitter;
    private MockDriver driver = new MockDriver();

    private List<WireEnvelope> envelopes = new ArrayList<>();

    protected void givenAssetChannel(final String name, final boolean listen, final DataType dataType,
            final OptionalDouble scale, final OptionalDouble offset) {
        final Map<String, Object> config = new HashMap<>();

        config.put("driver.pid", "testDriver");
        config.put(name + "#+name", name);
        config.put(name + "#+type", ChannelType.READ.name());
        config.put(name + "#+value.type", dataType.name());
        config.put(name + "#+enabled", true);
        config.put(name + "#+listen", listen);

        if (scale.isPresent()) {
            config.put(name + "#+scale", Double.toString(scale.getAsDouble()));
        }

        if (offset.isPresent()) {
            config.put(name + "#+offset", Double.toString(offset.getAsDouble()));
        }

        givenAssetConfig(config);
    }

    protected void givenAssetConfig(final Map<String, Object> assetConfig) {
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

            final long listenChannelCount = assetConfig.keySet().stream().filter(k -> {
                final String[] split = k.split("[#]");

                if (split.length != 2) {
                    return false;
                }

                return split[1].equals("+listen");

            }).filter(k -> Objects.equals(true, assetConfig.get(k)))
                    .count();

            synchronized (driver.listeners) {
                while (driver.listeners.size() != listenChannelCount) {
                    driver.listeners.wait(30000);
                }
            }

        } catch (final Exception e) {
            throw new IllegalStateException("failed to setup test graph", e);
        }
    }

    protected void givenChannelValue(final String key, final Object value) {
        driver.addReadResult(key, TypedValues.newTypedValue(value));
    }

    protected void givenChannelValues(final String key, final Object... values) {
        for (final Object value : values) {
            givenChannelValue(key, value);
        }
    }

    protected void whenAssetReceivesEnvelope() {

        testEmitter.emit();
    }

    protected void whenDriverEmitsEvents(final Object... values) {
        final Iterator<Object> iter = Arrays.asList(values).iterator();

        while (iter.hasNext()) {
            final String channelName = (String) iter.next();
            final TypedValue<?> value = TypedValues.newTypedValue(iter.next());

            driver.emitChannelEvent(channelName, value);
        }
    }

    protected void whenAssetReceivesEnvelopes(final int count) {
        for (int i = 0; i < count; i++) {
            whenAssetReceivesEnvelope();
        }
    }

    protected void awaitEnvelope(final int index) {
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

    protected void thenAssetOutputContains(final int index, final Object... properties) {
        awaitEnvelope(index);

        final WireEnvelope envelope = envelopes.get(index);

        final Iterator<Object> iter = Arrays.asList(properties).iterator();

        while (iter.hasNext()) {
            final String key = (String) iter.next();
            final TypedValue<?> value = TypedValues.newTypedValue(iter.next());

            assertEquals(value, envelope.getRecords().get(0).getProperties().get(key));
        }
    }

    protected void thenAssetOutputContainsKey(final int index, final String key) {
        awaitEnvelope(index);

        final WireEnvelope envelope = envelopes.get(index);

        assertTrue(envelope.getRecords().get(0).getProperties().containsKey(key));
    }

    protected void thenAssetOutputPropertyCountIs(final int index, final int expectedCount) {
        awaitEnvelope(index);

        final WireEnvelope envelope = envelopes.get(index);

        assertEquals(expectedCount, envelope.getRecords().get(0).getProperties().size());
    }

    protected void thenAssetOutputDoesNotContain(final int index, final String... properties) {
        awaitEnvelope(index);

        final WireEnvelope envelope = envelopes.get(index);

        final Iterator<String> iter = Arrays.asList(properties).iterator();

        while (iter.hasNext()) {
            final String key = (String) iter.next();

            assertFalse(envelope.getRecords().get(0).getProperties().containsKey(key));
        }
    }

    protected void thenTotalEmittedEnvelopeCountAfter1SecIs(final int expectedCount) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new IllegalStateException("sleep interrupted");
        }

        assertEquals(expectedCount, envelopes.size());
    }

}
