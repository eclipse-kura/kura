/*******************************************************************************
 * Copyright (c) 2020 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.logic.multiport.provider.test;

import static org.junit.Assert.assertFalse;
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
import org.slf4j.LoggerFactory;

public class BoolLogicTest {

    // See:
    // http://stackoverflow.com/questions/7161338/using-osgi-declarative-services-in-the-context-of-a-junit-test

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BoolLogicTest.class);

    private static final String UNDER_TEST_PID = "under.test";
    private static final String TEST_FIRST_EMITTER_PID = "test.first.emitter.pid";
    private static final String TEST_SECOND_EMITTER_PID = "test.second.emitter.pid";
    private static final String TEST_RECEIVER_PID = "test.receiver.pid";

    // in ports
    private static final int IN0_PORT = 0;
    private static final int IN1_PORT = 1;

    // out ports
    private static final int OUT_PORT = 0;

    // configuration properties of component under test
    private static final String FIRST_OPERAND_NAME_PROP_NAME = "operand.name.1";
    private static final String SECOND_OPERAND_NAME_PROP_NAME = "operand.name.2";
    // private static final String RESULT_NAME_PROP_NAME = "result.name";
    private static final String BARRIER_MODALITY_PROPERTY_KEY = "barrier";
    private static final String BOOLEAN_OPERATION = "logical.operator";

    private static final String OPERAND_NAME_DEFAULT = "operand";
    private static final String RESULT_NAME_DEFAULT = "result";
    private static final boolean BARRIER_MODALITY_PROPERTY_DEFAULT = false;
    private static final String BOOLEAN_OPERATION_DEFAULT = "AND";

    private static WireGraphService wireGraphService;
    private static ConfigurationService configurationService;
    private static CountDownLatch dependencyLatch = new CountDownLatch(3); // initialize with number of dependencies

    private static TestEmitterReceiver in1emitter;
    private static TestEmitterReceiver outReceiver;
    private static TestEmitterReceiver in0emitter;

    private static Map<String, TypedValue<?>> mapWithTrue = new HashMap<>();
    private static Map<String, TypedValue<?>> mapWithFalse = new HashMap<>();

    public BoolLogicTest() {
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
        BoolLogicTest.wireGraphService = wireGraphService;
        dependencyLatch.countDown();
    }

    public void bindConfigurationService(ConfigurationService configurationService) {
        logger.info("{} bound", System.identityHashCode(this));
        BoolLogicTest.configurationService = configurationService;
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

        in0emitter = null;
        in1emitter = null;
        outReceiver = null;

        final GraphBuilder builder = new GraphBuilder();

        final BundleContext bundleContext = FrameworkUtil.getBundle(BoolLogicTest.class).getBundleContext();

        builder.addWireComponent(UNDER_TEST_PID, "org.eclipse.kura.wire.LogicalOperators", 2, 1) //
                .addTestEmitterReceiver(TEST_FIRST_EMITTER_PID) //
                .addTestEmitterReceiver(TEST_SECOND_EMITTER_PID).addTestEmitterReceiver(TEST_RECEIVER_PID) //
                .addWire(TEST_FIRST_EMITTER_PID, 0, UNDER_TEST_PID, IN0_PORT) //
                .addWire(TEST_SECOND_EMITTER_PID, 0, UNDER_TEST_PID, IN1_PORT) //
                .addWire(UNDER_TEST_PID, OUT_PORT, TEST_RECEIVER_PID, 0);

        try {
            builder.replaceExistingGraph(bundleContext, wireGraphService).get(30, TimeUnit.SECONDS);

            in1emitter = builder.getTrackedWireComponent(TEST_SECOND_EMITTER_PID);
            in0emitter = builder.getTrackedWireComponent(TEST_FIRST_EMITTER_PID);
            outReceiver = builder.getTrackedWireComponent(TEST_RECEIVER_PID);
        } catch (KuraException | ExecutionException e) {
            logger.error("Test error", e);
            throw e;
        }

        mapWithTrue.put(OPERAND_NAME_DEFAULT, TypedValues.newBooleanValue(true));
        mapWithFalse.put(OPERAND_NAME_DEFAULT, TypedValues.newBooleanValue(false));

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
            return OPERAND_NAME_DEFAULT.equals(props.get(FIRST_OPERAND_NAME_PROP_NAME))
                    && OPERAND_NAME_DEFAULT.equals(props.get(SECOND_OPERAND_NAME_PROP_NAME))
                    && BARRIER_MODALITY_PROPERTY_DEFAULT == (boolean) props.get(BARRIER_MODALITY_PROPERTY_KEY)
                    && BOOLEAN_OPERATION_DEFAULT.equals(props.get(BOOLEAN_OPERATION));
        }
        return false;
    }

    @Test
    public void testAndOperation() throws Exception {
        try {
            Map<String, Boolean> result = calculateTruthTable();
            assertTrue(result.get("tt"));
            assertFalse(result.get("tf"));
            assertFalse(result.get("ft"));
            assertFalse(result.get("ff"));
        } catch (TimeoutException e) {
            fail("Timeout waiting for envelope");
        }
    }

    @Test
    public void testXorOperation() throws Exception {
        updateLogicOperation("XOR");
        try {
            Map<String, Boolean> result = calculateTruthTable();
            assertFalse(result.get("tt"));
            assertTrue(result.get("tf"));
            assertTrue(result.get("ft"));
            assertFalse(result.get("ff"));
        } catch (TimeoutException e) {
            fail("Timeout waiting for envelope");
        }
    }

    @Test
    public void testOrOperation() throws Exception {
        updateLogicOperation("OR");
        try {
            Map<String, Boolean> result = calculateTruthTable();
            assertTrue(result.get("tt"));
            assertTrue(result.get("tf"));
            assertTrue(result.get("ft"));
            assertFalse(result.get("ff"));
        } catch (TimeoutException e) {
            fail("Timeout waiting for envelope");
        }
    }

    @Test
    public void testNorOperation() throws Exception {
        updateLogicOperation("NOR");
        try {
            Map<String, Boolean> result = calculateTruthTable();
            assertFalse(result.get("tt"));
            assertFalse(result.get("tf"));
            assertFalse(result.get("ft"));
            assertTrue(result.get("ff"));
        } catch (TimeoutException e) {
            fail("Timeout waiting for envelope");
        }
    }

    @Test
    public void testNandOperation() throws Exception {
        try {
            updateLogicOperation("NAND");
            Map<String, Boolean> result = calculateTruthTable();
            assertFalse(result.get("tt"));
            assertTrue(result.get("tf"));
            assertTrue(result.get("ft"));
            assertTrue(result.get("ff"));
        } catch (TimeoutException e) {
            fail("Timeout waiting for envelope");
        }
    }

    @Test
    public void testNotOperation() throws Exception {
        updateLogicOperation("NOT");
        try {
            CompletableFuture<WireEnvelope> out0Recfuture = outReceiver.nextEnvelope();
            in0emitter.emit(new WireRecord(mapWithTrue));
            WireRecord receivedRecord = out0Recfuture.get(1, TimeUnit.SECONDS).getRecords().get(0);
            assertFalse((boolean) receivedRecord.getProperties().get(RESULT_NAME_DEFAULT).getValue());
            out0Recfuture = outReceiver.nextEnvelope();
            in0emitter.emit(new WireRecord(mapWithFalse));
            receivedRecord = out0Recfuture.get(1, TimeUnit.SECONDS).getRecords().get(0);
            assertTrue((boolean) receivedRecord.getProperties().get(RESULT_NAME_DEFAULT).getValue());

        } catch (TimeoutException e) {
            fail("Timeout waiting for envelope");
        }
    }

    @Test
    public void testIllegalOperation() throws Exception {
        updateLogicOperation("ILLEGAL");
        try {
            Map<String, Boolean> result = calculateTruthTable();
            assertTrue(result.get("tt"));
            assertFalse(result.get("tf"));
            assertFalse(result.get("ft"));
            assertFalse(result.get("ff"));
        } catch (TimeoutException e) {
            fail("Timeout waiting for envelope");
        }
    }

    private Map<String, Boolean> calculateTruthTable()
            throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, Boolean> results = new HashMap<>();
        results.put("tt", performBooleanOperation(true, true));
        results.put("tf", performBooleanOperation(true, false));
        results.put("ft", performBooleanOperation(false, true));
        results.put("ff", performBooleanOperation(false, false));
        return results;
    }

    private Boolean performBooleanOperation(boolean operator1, boolean operator2)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (operator1) {
            in0emitter.emit(new WireRecord(mapWithTrue));
        } else {
            in0emitter.emit(new WireRecord(mapWithFalse));
        }
        CompletableFuture<WireEnvelope> out0Recfuture = outReceiver.nextEnvelope();
        if (operator2) {
            in1emitter.emit(new WireRecord(mapWithTrue));
        } else {
            in1emitter.emit(new WireRecord(mapWithFalse));
        }
        WireRecord receivedRecord = out0Recfuture.get(1, TimeUnit.SECONDS).getRecords().get(0);
        return (boolean) receivedRecord.getProperties().get(RESULT_NAME_DEFAULT).getValue();
    }

    private void updateLogicOperation(String operation)
            throws InterruptedException, ExecutionException, TimeoutException {

        Map<String, Object> props = new HashMap<>();
        props.put(BOOLEAN_OPERATION, operation);
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
