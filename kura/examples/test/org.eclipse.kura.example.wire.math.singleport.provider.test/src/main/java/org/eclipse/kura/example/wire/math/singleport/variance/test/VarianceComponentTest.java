package org.eclipse.kura.example.wire.math.singleport.variance.test;

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

public class VarianceComponentTest {
    // See:
    // http://stackoverflow.com/questions/7161338/using-osgi-declarative-services-in-the-context-of-a-junit-test

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(VarianceComponentTest.class);

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
    private static final String WINDOW_SIZE_PROP_NAME = "window.size";

    private static final String OPERAND_NAME_DEFAULT = "operand";
    private static final String RESULT_NAME_DEFAULT = "result";
    private static final boolean EMIT_RECEIVED_PROPERTIES_DEFAULT = false;
    private static final int WINDOW_SIZE_DEFAULT = 10;

    private static WireGraphService wireGraphService;
    private static ConfigurationService configurationService;
    private static CountDownLatch dependencyLatch = new CountDownLatch(3); // initialize with number of dependencies

    private static TestEmitterReceiver outReceiver;
    private static TestEmitterReceiver inEmitter;

    public VarianceComponentTest() {
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
        VarianceComponentTest.wireGraphService = wireGraphService;
        dependencyLatch.countDown();
    }

    public void bindConfigurationService(ConfigurationService configurationService) {
        logger.info("{} bound", System.identityHashCode(this));
        VarianceComponentTest.configurationService = configurationService;
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

        final BundleContext bundleContext = FrameworkUtil.getBundle(VarianceComponentTest.class).getBundleContext();

        builder.addWireComponent(UNDER_TEST_PID, "org.eclipse.kura.wire.Variance", 1, 1) //
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

    }

    @Test
    public void varianceWireComponentExists() throws KuraException {
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
    public void varianceWireComponentHasDefaultProperties() throws KuraException {
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
                    && EMIT_RECEIVED_PROPERTIES_DEFAULT == (boolean) props.get(EMIT_RECEIVED_PROPERTIES)
                    && WINDOW_SIZE_DEFAULT == (int) props.get(WINDOW_SIZE_PROP_NAME)
                    && RESULT_NAME_DEFAULT.equals(props.get(RESULT_NAME_PROP_NAME));
        }
        return false;
    }

    @Test
    public void testVariance() throws Exception {
        logger.info("### TESTING VARIANCE COMPONENT ###");
        Map<String, Object> props = new HashMap<>();
        props.put(WINDOW_SIZE_PROP_NAME, 10);
        try {
            WireTestUtil.updateWireComponentConfiguration(configurationService, UNDER_TEST_PID, props).get(30,
                    TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            logger.error("Test error", e);
            throw e;
        }

        Map<String, TypedValue<?>> myMap = new HashMap<>();
        for (int i = 1; i < 10; i++) {
            myMap.clear();
            myMap.put(OPERAND_NAME_DEFAULT, TypedValues.newDoubleValue(3));
            inEmitter.emit(new WireRecord(myMap));
        }
        CompletableFuture<WireEnvelope> out0Recfuture = outReceiver.nextEnvelope();
        myMap.clear();
        myMap.put(OPERAND_NAME_DEFAULT, TypedValues.newDoubleValue(10));
        inEmitter.emit(new WireRecord(myMap));

        try {
            WireRecord receivedRecord = out0Recfuture.get(1, TimeUnit.SECONDS).getRecords().get(0);
            logger.info("received {}", receivedRecord.getProperties());
            assertTrue(((double) receivedRecord.getProperties().get(RESULT_NAME_DEFAULT).getValue() == 4.9));

        } catch (TimeoutException e) {
            fail("Timeout waiting for envelope");
        }
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
