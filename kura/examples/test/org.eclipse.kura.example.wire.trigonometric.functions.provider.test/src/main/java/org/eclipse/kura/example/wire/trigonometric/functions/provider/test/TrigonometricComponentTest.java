/*******************************************************************************
 * Copyright (c) 2020 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.trigonometric.functions.provider.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.wire.test.GraphBuilder;
import org.eclipse.kura.util.wire.test.TestEmitterReceiver;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrigonometricComponentTest {

    // See:
    // http://stackoverflow.com/questions/7161338/using-osgi-declarative-services-in-the-context-of-a-junit-test

    private static final Logger logger = LoggerFactory.getLogger(TrigonometricComponentTest.class);

    private static final String UNDER_TEST_PID = "under.test";
    private static final String TEST_EMITTER_PID = "test.emitter.pid";
    private static final String TEST_RECEIVER_PID = "test.receiver.pid";

    // in ports
    private static final int IN_PORT = 0;

    // out ports
    private static final int OUT_PORT = 0;

    // configuration properties of component under test
    private static final String OPERAND_NAME_PROP_NAME = "operand.name";
    private static final String RESULT_NAME_PROP_NAME = "result.name";
    private static final String EMIT_RECEIVED_PROPERTIES = "emit.received.properties";
    private static final String TRIGONOMETRIC_OPERATION = "trigonometric.operator";

    private static final String OPERAND_NAME_DEFAULT = "operand";
    private static final String RESULT_NAME_DEFAULT = "result";
    private static final boolean EMIT_RECEIVED_PROPERTIES_DEFAULT = false;
    private static final String TRIGONOMETRIC_OPERATION_DEFAULT = "SIN";

    // tolerance for errors with inverse trigonometric functions
    private static final double ACCEPTABLE_ERROR = 0.0000000000000004;

    private static WireGraphService wireGraphService;
    private static ConfigurationService configurationService;
    private static CountDownLatch dependencyLatch = new CountDownLatch(3); // initialize with number of dependencies

    private static TestEmitterReceiver outReceiver;
    private static TestEmitterReceiver inEmitter;

    private static Map<String, TypedValue<?>> mapWithZero = new HashMap<>();
    private static Map<String, TypedValue<?>> mapWithHalf = new HashMap<>();
    private static Map<String, TypedValue<?>> mapWithQuarterPi = new HashMap<>();
    private static Map<String, TypedValue<?>> mapWithOne = new HashMap<>();
    private static Map<String, TypedValue<?>> mapWithHalfPi = new HashMap<>();
    private static Map<String, TypedValue<?>> mapWithPi = new HashMap<>();
    private static Map<String, TypedValue<?>> mapWithTwoPi = new HashMap<>();
    private static Map<String, TypedValue<?>> mapWithThreeHalvesPi = new HashMap<>();

    public TrigonometricComponentTest() {
        super();
        logger.info("{} created", System.identityHashCode(this));
    }

    //
    // OSGi activation methods. These methods are called only once.
    // There is only a single instance of this class created by the OSGi framework.
    protected void activate(ComponentContext componentContext) {
        logger.info("{} activated", System.identityHashCode(this));
        dependencyLatch.countDown();
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("{} deactivated", System.identityHashCode(this));
    }

    public void bindWireGraphService(WireGraphService wireGraphService) {
        logger.info("{} bound", System.identityHashCode(this));
        TrigonometricComponentTest.wireGraphService = wireGraphService;
        dependencyLatch.countDown();
    }

    public void bindConfigurationService(ConfigurationService configurationService) {
        logger.info("{} bound", System.identityHashCode(this));
        TrigonometricComponentTest.configurationService = configurationService;
        dependencyLatch.countDown();
    }

    //
    // JUnit 4 stuff
    @BeforeClass
    public static void setUpOnce() throws InterruptedException {
        // Wait for OSGi dependencies
        logger.info("waiting for dependencies...");

        if (!dependencyLatch.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("timeout waiting for dependencies");
        } else {
            logger.info("waiting for dependencies... done");
        }

    }

    @Before
    public void setUp() throws KuraException, InterruptedException, TimeoutException, ExecutionException {
        logger.info("{} setup", System.identityHashCode(this));

        inEmitter = null;
        outReceiver = null;

        final GraphBuilder builder = new GraphBuilder();

        final BundleContext bundleContext = FrameworkUtil.getBundle(TrigonometricComponentTest.class)
                .getBundleContext();

        builder.addWireComponent(UNDER_TEST_PID, "org.eclipse.kura.wire.TrigonometricOperators", 1, 1) //
                .addTestEmitterReceiver(TEST_EMITTER_PID) //
                .addTestEmitterReceiver(TEST_RECEIVER_PID) //
                .addWire(TEST_EMITTER_PID, 0, UNDER_TEST_PID, IN_PORT) //
                .addWire(UNDER_TEST_PID, OUT_PORT, TEST_RECEIVER_PID, 0);

        try {
            builder.replaceExistingGraph(bundleContext, wireGraphService).get(30, TimeUnit.SECONDS);

            inEmitter = builder.getTrackedWireComponent(TEST_EMITTER_PID);
            outReceiver = builder.getTrackedWireComponent(TEST_RECEIVER_PID);
        } catch (KuraException | ExecutionException e) {
            logger.error("Test error", e);
            throw e;
        }

        mapWithZero.put(OPERAND_NAME_DEFAULT, TypedValues.newDoubleValue(0));
        mapWithHalf.put(OPERAND_NAME_DEFAULT, TypedValues.newDoubleValue(0.5));
        mapWithQuarterPi.put(OPERAND_NAME_DEFAULT, TypedValues.newDoubleValue(Math.PI / 4));
        mapWithOne.put(OPERAND_NAME_DEFAULT, TypedValues.newDoubleValue(1));
        mapWithHalfPi.put(OPERAND_NAME_DEFAULT, TypedValues.newDoubleValue(Math.PI / 2));
        mapWithPi.put(OPERAND_NAME_DEFAULT, TypedValues.newDoubleValue(Math.PI));
        mapWithThreeHalvesPi.put(OPERAND_NAME_DEFAULT, TypedValues.newDoubleValue(Math.PI * 1.5));
        mapWithTwoPi.put(OPERAND_NAME_DEFAULT, TypedValues.newDoubleValue(Math.PI * 2));

    }

    @Test
    public void wireComponentExists() throws KuraException {
        WireGraphConfiguration wgc;
        try {
            wgc = wireGraphService.get();
        } catch (KuraException e) {
            logger.error("Test error", e);
            throw e;
        }

        assertTrue(wgc.getWireComponentConfigurations().stream()
                .anyMatch(wcc -> UNDER_TEST_PID.equals(wcc.getConfiguration().getPid())));
    }

    @Test
    public void wireComponentHasDefaultProperties() throws KuraException {
        WireGraphConfiguration wgc;
        try {
            wgc = wireGraphService.get();
        } catch (KuraException e) {
            logger.error("Test error", e);
            throw e;
        }

        assertTrue(wgc.getWireComponentConfigurations().stream()
                .anyMatch(wcc -> matchesDefaultConfiguration(wcc.getConfiguration())));
    }

    private boolean matchesDefaultConfiguration(ComponentConfiguration cc) {
        if (cc.getPid().equals(UNDER_TEST_PID)) {
            Map<String, Object> props = cc.getConfigurationProperties();
            return OPERAND_NAME_DEFAULT.equals(props.get(OPERAND_NAME_PROP_NAME))
                    && RESULT_NAME_DEFAULT.equals(props.get(RESULT_NAME_PROP_NAME))
                    && EMIT_RECEIVED_PROPERTIES_DEFAULT == (boolean) props.get(EMIT_RECEIVED_PROPERTIES)
                    && TRIGONOMETRIC_OPERATION_DEFAULT.equals(props.get(TRIGONOMETRIC_OPERATION));
        }
        return false;
    }

    @Test
    public void testSinOperation() throws Exception {
        try {
            Map<String, Double> result = calculateResults();
            assertTrue(0.0 == result.get("zero").doubleValue());
            assertTrue(1.0 == result.get("halfPi").doubleValue());
            assertTrue(Math.abs(0.0 - result.get("pi").doubleValue()) < ACCEPTABLE_ERROR);
            assertTrue(-1.0 == result.get("threeHalvesPi").doubleValue());
        } catch (TimeoutException e) {
            fail("Timeout waiting for envelope");
        }
    }

    @Test
    public void testCosOperation() throws Exception {
        try {
            updateTrigonometricOperation("COS");
            Map<String, Double> result = calculateResults();
            assertTrue(1.0 == result.get("zero").doubleValue());
            assertTrue(Math.abs(0.0 - result.get("halfPi").doubleValue()) < ACCEPTABLE_ERROR);
            assertTrue(-1.0 == result.get("pi").doubleValue());
            assertTrue(Math.abs(0.0 - result.get("threeHalvesPi").doubleValue()) < ACCEPTABLE_ERROR);
        } catch (TimeoutException e) {
            fail("Timeout waiting for envelope");
        }
    }

    @Test
    public void testTanOperation() throws Exception {
        try {
            updateTrigonometricOperation("TAN");
            Map<String, Double> result = calculateResults();
            assertTrue(Math.abs(0.0 - result.get("zero").doubleValue()) < ACCEPTABLE_ERROR);
            assertTrue(Math.abs(1.0 - result.get("quarterPi").doubleValue()) < ACCEPTABLE_ERROR);
            assertTrue(Math.abs(0.0 - result.get("pi").doubleValue()) < ACCEPTABLE_ERROR);
        } catch (TimeoutException e) {
            fail("Timeout waiting for envelope");
        }
    }

    @Test
    public void testArcSinOperation() throws Exception {
        try {
            updateTrigonometricOperation("ASIN");
            Map<String, Double> result = calculateInverseResults();
            assertTrue(0.0 == result.get("zero").doubleValue());
            assertTrue(Math.abs((Math.PI / 6) - result.get("half").doubleValue()) < ACCEPTABLE_ERROR);
            assertTrue((Math.PI / 2) == result.get("one").doubleValue());
        } catch (TimeoutException e) {
            fail("Timeout waiting for envelope");
        }
    }

    @Test
    public void testArcCosOperation() throws Exception {
        try {
            updateTrigonometricOperation("ACOS");
            Map<String, Double> result = calculateInverseResults();
            assertTrue((Math.PI / 2) == result.get("zero").doubleValue());
            assertTrue(Math.abs((Math.PI / 3) - result.get("half").doubleValue()) < ACCEPTABLE_ERROR);
            assertTrue(0.0 == result.get("one").doubleValue());

        } catch (TimeoutException e) {
            fail("Timeout waiting for envelope");
        }
    }

    @Test
    public void testArcTanOperation() throws Exception {
        try {
            updateTrigonometricOperation("ATAN");
            Map<String, Double> result = calculateInverseResults();
            assertTrue(0.0 == result.get("zero").doubleValue());
            assertTrue((Math.PI / 4) == result.get("one").doubleValue());

        } catch (TimeoutException e) {
            fail("Timeout waiting for envelope");
        }
    }

    private Map<String, Double> calculateResults() throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, Double> results = new HashMap<>();
        results.put("zero", performTrigonometricOperation(mapWithZero));
        results.put("quarterPi", performTrigonometricOperation(mapWithQuarterPi));
        results.put("halfPi", performTrigonometricOperation(mapWithHalfPi));
        results.put("pi", performTrigonometricOperation(mapWithPi));
        results.put("threeHalvesPi", performTrigonometricOperation(mapWithThreeHalvesPi));
        results.put("2pi", performTrigonometricOperation(mapWithTwoPi));
        return results;
    }

    private Map<String, Double> calculateInverseResults()
            throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, Double> results = new HashMap<>();
        results.put("zero", performTrigonometricOperation(mapWithZero));
        results.put("half", performTrigonometricOperation(mapWithHalf));
        results.put("one", performTrigonometricOperation(mapWithOne));
        return results;
    }

    private Double performTrigonometricOperation(Map<String, TypedValue<?>> properties)
            throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<WireEnvelope> out0Recfuture = outReceiver.nextEnvelope();
        inEmitter.emit(new WireRecord(properties));
        WireRecord receivedRecord = out0Recfuture.get(1, TimeUnit.SECONDS).getRecords().get(0);
        return (Double) receivedRecord.getProperties().get(RESULT_NAME_DEFAULT).getValue();
    }

    private void updateTrigonometricOperation(String operation)
            throws InterruptedException, ExecutionException, TimeoutException {

        Map<String, Object> props = new HashMap<>();
        props.put(TRIGONOMETRIC_OPERATION, operation);
        WireTestUtil.updateWireComponentConfiguration(configurationService, UNDER_TEST_PID, props).get(30,
                TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws Exception {
        try {
            wireGraphService.delete();
        } catch (KuraException e) {
            logger.error("Test error", e);
            throw e;
        }
    }

    @AfterClass
    public static void tearDownOnce() {
        logger.info("tear down once");
    }

}
