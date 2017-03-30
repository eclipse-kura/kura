/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.wire.provider.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.test.annotation.TestTarget;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This WireServiceTest is responsible to test all the API methods of
 * {@link WireService}
 */
public final class WireServiceTest {

    /** A latch to be initialized with the no of OSGi dependencies needed */
    private static CountDownLatch dependencyLatch = new CountDownLatch(2);

    /** The Wire Emitter PID */
    private static final String emitterPid = "org.eclipse.kura.wire.test.emitter";

    /** The Wire Receiver PID */
    private static final String receiverPid = "org.eclipse.kura.wire.test.receiver";

    /** Configuration Service Reference */
    private static ConfigurationService s_configService;

    /** Logger */
    private static final Logger s_logger = LoggerFactory.getLogger(WireServiceTest.class);

    /** Configuration Service Reference */
    private static WireService s_wireService;

    /**
     * Binds the configuration service dependency
     *
     * @param configService
     *            the configuration service dependency
     */
    public void bindConfigService(final ConfigurationService configService) {
        if (s_configService == null) {
            s_configService = configService;
            dependencyLatch.countDown();
        }
    }

    /**
     * Binds the wire service dependency
     *
     * @param wireService
     *            the wire service dependency
     */
    public void bindWireService(final WireService wireService) {
        if (s_wireService == null) {
            s_wireService = wireService;
            dependencyLatch.countDown();
        }
    }

    /**
     * Tests {@link WireService} methods
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testCreateDeleteGetWireConfiguration() throws Exception {
        WireConfiguration configuration = null;
        configuration = s_wireService.createWireConfiguration(emitterPid, receiverPid);
        assertNotNull(configuration);
        assertNotNull(configuration.getWire());
        assertEquals(configuration.getEmitterPid(), emitterPid);
        assertEquals(configuration.getReceiverPid(), receiverPid);
        final Set<WireConfiguration> configs = s_wireService.getWireConfigurations();
        assertEquals(1, configs.size());
        s_wireService.deleteWireConfiguration(configuration);
        assertEquals(0, configs.size());
    }

    /**
     * Tests the condition in case the emitter PID or receiver PID are not
     * assigned to any available wire component
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test(expected = KuraException.class)
    public void testEmitterReceiverPidNotAvailable() throws KuraException {
        s_wireService.createWireConfiguration("x", "y");
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetConfiguration() throws KuraException {
        s_wireService.createWireConfiguration(emitterPid, receiverPid);

        ComponentConfiguration config = ((SelfConfiguringComponent) s_wireService).getConfiguration();

        Map<String, Object> props = config.getConfigurationProperties();

        assertEquals("org.eclipse.kura.wire.WireService", config.getDefinition().getId());
        assertEquals("WireService", config.getDefinition().getName());
        assertTrue(config.getDefinition().getDescription().contains("Wire Components"));
        assertTrue(props.containsKey("1.emitter"));
        assertTrue(props.containsKey("1.filter"));
        assertTrue(props.containsKey("1.receiver"));
        assertEquals(emitterPid, props.get("1.emitter"));
        assertNull(props.get("1.filter"));
        assertEquals(receiverPid, props.get("1.receiver"));
    }

    /**
     * Tests the condition in case the emitter PID and receiver PID are same
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testSameEmitterAndReceiverPid() throws KuraException {
        final WireConfiguration configuration = s_wireService.createWireConfiguration(emitterPid, emitterPid);
        assertNull(configuration);
    }

    /**
     * Tests the availability of injected OSGi services
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testServiceExists() {
        assertNotNull(WireServiceTest.s_wireService);
        assertNotNull(WireServiceTest.s_configService);
    }

    /**
     * Unbinds the configuration service dependency
     *
     * @param configService
     *            the configuration service dependency
     */
    public void unbindConfigService(final ConfigurationService configService) {
        if (s_configService == configService) {
            s_configService = null;
        }
    }

    /**
     * Unbinds the wire service dependency
     *
     * @param wireService
     *            the wire service dependency
     */
    public void unbindWireService(final WireService wireService) {
        if (s_wireService == wireService) {
            s_wireService = null;
        }
    }

    /**
     * JUnit Callback to be triggered before creating the instance of this suite
     *
     * @throws Exception
     *             if the dependent services are null
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        // Wait for OSGi dependencies
        s_logger.info("Setting Up The Testcase....");
        try {
            boolean ok = dependencyLatch.await(10, TimeUnit.SECONDS);

            assertTrue("Dependencies OK", ok);
        } catch (final InterruptedException e) {
            fail("OSGi dependencies unfulfilled");
        }
    }
}
