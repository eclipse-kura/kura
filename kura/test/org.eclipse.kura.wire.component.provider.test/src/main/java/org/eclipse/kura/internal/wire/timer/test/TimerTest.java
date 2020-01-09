/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.timer.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.util.wire.test.GraphBuilder;
import org.eclipse.kura.util.wire.test.TestEmitterReceiver;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class TimerTest {

    private static final String TIMER_FACTORY_PID = "org.eclipse.kura.wire.Timer";

    private static final String TEST_TIMER_PID = "testTimer";
    private static final String TEST_EMITTER_RECEIVER_PID = "testEmitterReceiver";

    private TestEmitterReceiver testEmitterReceiver;

    private static WireGraphService wireGraphService;
    private static ConfigurationService configurationService;

    private static final CountDownLatch activated = new CountDownLatch(1);

    protected void activate() {
        activated.countDown();
    }

    public void bindWireGraphService(WireGraphService wireGraphService) {
        TimerTest.wireGraphService = wireGraphService;
    }

    public void bindConfigurationService(ConfigurationService configurationService) {
        TimerTest.configurationService = configurationService;
    }

    @BeforeClass
    public static void beforeClass() throws InterruptedException {
        if (!activated.await(60, TimeUnit.SECONDS)) {
            throw new IllegalStateException("dependencies not resolved");
        }
    }

    @Before
    public void setUp() throws InterruptedException, ExecutionException, TimeoutException, KuraException {
        final BundleContext context = FrameworkUtil.getBundle(TimerTest.class).getBundleContext();

        final GraphBuilder builder = new GraphBuilder();

        builder.addWireComponent(TEST_TIMER_PID, TIMER_FACTORY_PID) //
                .addTestEmitterReceiver(TEST_EMITTER_RECEIVER_PID) //
                .addWire(TEST_TIMER_PID, TEST_EMITTER_RECEIVER_PID) //
                .replaceExistingGraph(context, wireGraphService) //
                .get(60, TimeUnit.SECONDS);

        this.testEmitterReceiver = builder.getTrackedWireComponent(TEST_EMITTER_RECEIVER_PID);
    }

    @Test
    public void shouldSupportDefaultConfig() {

        final AtomicInteger count = new AtomicInteger();

        this.testEmitterReceiver.setConsumer(e -> count.incrementAndGet());

        sleep(11000);

        final int finalCount = count.get();

        assertTrue("count should greater than 0 but was " + finalCount, finalCount > 0);
        assertTrue("count should be at most 2 but was " + finalCount, finalCount <= 2);
    }

    @Test
    public void shouldTickEvery100Millis() throws InterruptedException, ExecutionException, TimeoutException {
        WireTestUtil.updateWireComponentConfiguration(configurationService, TEST_TIMER_PID, TimerConfig.defaultConfig() //
                .withInterval(100) //
                .withTimeUnit(TimeUnit.MILLISECONDS) //
                .toProperties() //
        ).get(60, TimeUnit.SECONDS);

        final AtomicInteger count = new AtomicInteger();

        this.testEmitterReceiver.setConsumer(e -> count.incrementAndGet());

        sleep(1000);
        final int finalCount = count.get();

        assertTrue(finalCount >= 9);
        assertTrue(finalCount <= 11);
    }

    @Test
    public void shouldTickEvery10Millis() throws InterruptedException, ExecutionException, TimeoutException {
        WireTestUtil.updateWireComponentConfiguration(configurationService, TEST_TIMER_PID, TimerConfig.defaultConfig() //
                .withInterval(10) //
                .withTimeUnit(TimeUnit.MILLISECONDS) //
                .toProperties()).get(60, TimeUnit.SECONDS);

        final AtomicInteger count = new AtomicInteger();

        this.testEmitterReceiver.setConsumer(e -> count.incrementAndGet());

        sleep(1000);
        final int finalCount = count.get();

        assertTrue("count should be greater than 90 but was " + finalCount, finalCount >= 90);
        assertTrue("count should be less than 110 but was " + finalCount, finalCount <= 110);
    }

    @Test
    public void shouldSupportFixedRateScheduling() throws InterruptedException, ExecutionException, TimeoutException {
        WireTestUtil.updateWireComponentConfiguration(configurationService, TEST_TIMER_PID, TimerConfig.defaultConfig() //
                .withInterval(100) //
                .withTimeUnit(TimeUnit.MILLISECONDS).toProperties()).get(60, TimeUnit.SECONDS);

        final AtomicInteger count = new AtomicInteger();

        this.testEmitterReceiver.setConsumer(e -> {
            count.incrementAndGet();
            sleep(90);
        });

        sleep(1000);
        final int finalCount = count.get();

        assertTrue("count should be at least 9 but was " + finalCount, finalCount >= 9);
        assertTrue("count should be at most 11 but was " + finalCount, finalCount <= 11);
    }

    @Test
    public void shouldSupportReconfiguration() throws InterruptedException, ExecutionException, TimeoutException {
        final AtomicInteger count = new AtomicInteger();

        this.testEmitterReceiver.setConsumer(e -> count.incrementAndGet());

        WireTestUtil.updateWireComponentConfiguration(configurationService, TEST_TIMER_PID, TimerConfig.defaultConfig() //
                .withInterval(100) //
                .withTimeUnit(TimeUnit.MILLISECONDS) //
                .toProperties() //
        ).get(60, TimeUnit.SECONDS);

        sleep(1000);
        {
            final int finalCount = count.get();

            assertTrue(finalCount >= 9);
            assertTrue(finalCount <= 11);
        }

        WireTestUtil
                .updateWireComponentConfiguration(configurationService, TEST_TIMER_PID,
                        TimerConfig.defaultConfig().withInterval(50).withTimeUnit(TimeUnit.MILLISECONDS).toProperties())
                .get(60, TimeUnit.SECONDS);

        sleep(500);

        {
            final int finalCount = count.get();

            assertTrue(finalCount >= 9 + 9);
            assertTrue(finalCount <= 11 + 11);
        }

    }

    @Test
    public void shouldSupportCronExpression() throws InterruptedException, ExecutionException, TimeoutException {
        WireTestUtil.updateWireComponentConfiguration(configurationService, TEST_TIMER_PID, TimerConfig.defaultConfig() //
                .withType(TimerType.CRON) //
                .withCronExpression("0/1 * * * * ?") //
                .toProperties()).get(60, TimeUnit.SECONDS);

        final AtomicInteger count = new AtomicInteger();

        this.testEmitterReceiver.setConsumer(e -> {
            count.incrementAndGet();
            sleep(900);
        });

        sleep(10000);
        final int finalCount = count.get();

        assertTrue("count should be at least 9 but was " + finalCount, finalCount >= 9);
        assertTrue("count should be at most 11 but was " + finalCount, finalCount <= 11);
    }

    @Test
    public void shouldTolerateInvalidCronExpression()
            throws InterruptedException, ExecutionException, TimeoutException {
        final AtomicInteger count = new AtomicInteger();
        this.testEmitterReceiver.setConsumer(e -> count.incrementAndGet());

        WireTestUtil.updateWireComponentConfiguration(configurationService, TEST_TIMER_PID, TimerConfig.defaultConfig() //
                .withType(TimerType.CRON) //
                .withCronExpression("invalid") //
                .toProperties()).get(60, TimeUnit.SECONDS);

        sleep(2000);
        {
            final int finalCount = count.get();

            assertEquals(0, finalCount);
        }

        WireTestUtil.updateWireComponentConfiguration(configurationService, TEST_TIMER_PID, TimerConfig.defaultConfig() //
                .withType(TimerType.CRON) //
                .withCronExpression("0/1 * * * * ?") //
                .toProperties()).get(60, TimeUnit.SECONDS);

        sleep(10000);

        {
            final int finalCount = count.get();

            assertTrue("count should be at least 9 but was " + finalCount, finalCount >= 9);
            assertTrue("count should be at most 12 but was " + finalCount, finalCount <= 12);
        }

    }

    @Test
    public void shouldContinueToTickIfReceiverThrows()
            throws InterruptedException, ExecutionException, TimeoutException {
        WireTestUtil.updateWireComponentConfiguration(configurationService, TEST_TIMER_PID, TimerConfig.defaultConfig() //
                .withInterval(100) //
                .withTimeUnit(TimeUnit.MILLISECONDS) //
                .toProperties() //
        ).get(60, TimeUnit.SECONDS);

        final AtomicInteger count = new AtomicInteger();

        this.testEmitterReceiver.setConsumer(e -> {
            count.incrementAndGet();
            throw new RuntimeException("test exception");
        });

        sleep(1000);
        final int finalCount = count.get();

        assertTrue(finalCount >= 9);
        assertTrue(finalCount <= 11);
    }

    private void sleep(final long millis) {
        synchronized (this) {
            try {
                this.wait(millis);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private enum TimerType {
        SIMPLE,
        CRON
    }

    private static final class TimerConfig {

        private static final String INTERVAL_PROP = "simple.interval";
        private static final String TIME_UNIT_PROP = "simple.time.unit";
        private static final String TYPE_PROP = "type";
        private static final String CRON_EXPRESSION_PROP = "cron.interval";

        private Optional<Integer> interval = Optional.empty();
        private Optional<TimeUnit> timeUnit = Optional.empty();
        private Optional<String> cronExpression = Optional.empty();
        private Optional<String> type = Optional.empty();

        public static TimerConfig defaultConfig() {
            return new TimerConfig();
        }

        public TimerConfig withType(final TimerType type) {
            this.type = Optional.of(type.name());
            return this;
        }

        public TimerConfig withCronExpression(final String cronExpression) {
            this.cronExpression = Optional.of(cronExpression);
            return this;
        }

        public TimerConfig withInterval(final int interval) {
            this.interval = Optional.of(interval);
            return this;
        }

        public TimerConfig withTimeUnit(final TimeUnit timeUnit) {
            this.timeUnit = Optional.of(timeUnit);
            return this;
        }

        public Map<String, Object> toProperties() {
            final Map<String, Object> result = new HashMap<>();
            if (this.interval.isPresent()) {
                result.put(INTERVAL_PROP, this.interval.get());
            }
            if (this.timeUnit.isPresent()) {
                result.put(TIME_UNIT_PROP, this.timeUnit.get().name());
            }
            if (this.cronExpression.isPresent()) {
                result.put(CRON_EXPRESSION_PROP, this.cronExpression.get());
            }
            if (this.type.isPresent()) {
                result.put(TYPE_PROP, this.type.get());
            }
            return result;
        }
    }

}