/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.test.net;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.wifi.WpaSupplicantManager;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class WpaSupplicantTest extends TestCase {

    private static final Logger s_logger = LoggerFactory.getLogger(WpaSupplicantTest.class);

    private static CountDownLatch dependencyLatch = new CountDownLatch(1);	// initialize with number of dependencies
    private static final String IFACE_NAME = "wlan0";
    private WpaSupplicantManager wpaSupplicantManager;
    private CommandExecutorService executorService;

    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
        dependencyLatch.countDown();
    }

    public void unsetExecutorService(CommandExecutorService executorService) {
        this.executorService = null;
        dependencyLatch.countDown();
    }

    @Override
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @BeforeClass
    public void setUp() {
        // Wait for OSGi dependencies
        try {
            dependencyLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("OSGi dependencies unfulfilled");
            System.exit(1);
        }
        this.wpaSupplicantManager = new WpaSupplicantManager(this.executorService);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testStart() {
        s_logger.info("Test start wpa_supplicant");

        try {
            this.wpaSupplicantManager.start(IFACE_NAME, null);
            assertTrue("wpa_supplicant is started", this.wpaSupplicantManager.isRunning(IFACE_NAME));

            boolean validPid = (this.wpaSupplicantManager.getPid(IFACE_NAME) > 0) ? true : false;
            assertTrue("Valid wpa_supplicant PID", validPid);
        } catch (Exception e) {
            fail("testEnable failed: " + e);
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testStop() {
        s_logger.info("Test stop wpa_supplicant");
        try {
            this.wpaSupplicantManager.stop(IFACE_NAME);
            assertFalse("wpa_supplicant is disabled", this.wpaSupplicantManager.isRunning(IFACE_NAME));
        } catch (Exception e) {
            fail("testStop failed: " + e);
        }
    }
}
