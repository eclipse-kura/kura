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
import org.junit.Ignore;
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

    private static final String emitterPid = "org.eclipse.kura.wire.test.emitter";

    private static final String receiverPid = "org.eclipse.kura.wire.test.receiver";

    private static ConfigurationService configService;

    private static final Logger logger = LoggerFactory.getLogger(WireServiceTest.class);

    private static WireService wireService;

    /**
     * Binds the configuration service dependency
     *
     * @param cfgService
     *            the configuration service dependency
     */
    public void bindConfigService(final ConfigurationService cfgService) {
        if (configService == null) {
            configService = cfgService;
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
        if (WireServiceTest.wireService == null) {
            WireServiceTest.wireService = wireService;
            dependencyLatch.countDown();
        }
    }

    /**
     * Tests the condition in case the emitter PID or receiver PID are not
     * assigned to any available wire component
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test(expected = KuraException.class)
    public void testEmitterReceiverPidNotAvailable() throws KuraException {
        wireService.createWireConfiguration("x", "y");
    }

    /**
     * Tests the condition in case the emitter PID and receiver PID are same
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testSameEmitterAndReceiverPid() throws KuraException {
        final WireConfiguration configuration = wireService.createWireConfiguration(emitterPid, emitterPid);
        assertNull(configuration);
    }

    /**
     * Tests the availability of injected OSGi services
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testServiceExists() {
        assertNotNull(WireServiceTest.wireService);
        assertNotNull(WireServiceTest.configService);
    }

    /**
     * Unbinds the configuration service dependency
     *
     * @param cfgService
     *            the configuration service dependency
     */
    public void unbindConfigService(final ConfigurationService cfgService) {
        if (configService == cfgService) {
            configService = null;
        }
    }

    /**
     * Unbinds the wire service dependency
     *
     * @param wreService
     *            the wire service dependency
     */
    public void unbindWireService(final WireService wreService) {
        if (wireService == wreService) {
            wireService = null;
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
        logger.info("Setting Up The Testcase....");
        try {
            boolean ok = dependencyLatch.await(10, TimeUnit.SECONDS);

            assertTrue("Dependencies OK", ok);
        } catch (final InterruptedException e) {
            fail("OSGi dependencies unfulfilled");
        }
    }
}
