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

package org.eclipse.kura.configuration.change.manager.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.configuration.change.manager.ConfigurationChangeManager;
import org.eclipse.kura.configuration.change.manager.ConfigurationChangeManagerOptions;
import org.eclipse.kura.configuration.change.manager.test.mocks.MockCloudPublisher;
import org.eclipse.kura.configuration.change.manager.test.mocks.MockServiceTracker;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;

public class ConfigurationChangeManagerTest {

    private MockCloudPublisher mockPublisher = new MockCloudPublisher();
    private MockServiceTracker mockServiceTracker = new MockServiceTracker();
    private ConfigurationChangeManager configurationChangeManager = new ConfigurationChangeManager();

    /*
     * Scenarios
     */

    @Test
    public void shouldNotPublishWhenDisabled() throws InterruptedException, ExecutionException {
        givenConfigurationChangeManager(false, 0L);

        whenConfigurationChanges("test.pid");

        thenNoMessagesPublished(0L);
    }

    @Test
    public void shouldPublishOneMessage() throws InterruptedException, ExecutionException, TimeoutException {
        givenConfigurationChangeManager(true, 2L);

        whenConfigurationChanges("test1", "test2");

        thenMessagePublished(2L, "test1", "test2");
    }

    @Test
    public void shouldAccumulateAndPublishOneMessages() throws InterruptedException, ExecutionException {
        givenConfigurationChangeManager(true, 10L);

        whenConfigurationChanges("pid1", "pid2", "pid3", "pid4", "pid5", "pid5");

        thenMessagePublished(10L, "pid1", "pid2", "pid3", "pid4", "pid5");
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenConfigurationChangeManager(boolean enabled, long sendDelaySec) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationChangeManagerOptions.KEY_ENABLED, enabled);
        properties.put(ConfigurationChangeManagerOptions.KEY_SEND_DELAY, sendDelaySec);

        try {
            this.configurationChangeManager.updated(properties);
        } catch (NullPointerException npe) {
            // expected NPE since the service tracker cannot be instatiated (BundleContext not available)
        }
    }

    /*
     * When
     */

    private void whenConfigurationChanges(String... changedPids) {
        for (String pid : changedPids) {
            this.mockServiceTracker.simulateConfigChange(pid);
        }
    }

    /*
     * Then
     */

    private void thenNoMessagesPublished(long sendDelaySec)
            throws InterruptedException, ExecutionException {
        CountDownLatch waitForSendDelay = new CountDownLatch(1);
        this.mockPublisher.addPublishCountLatch(waitForSendDelay);
        assertFalse(waitForSendDelay.await(sendDelaySec + 5, TimeUnit.SECONDS));
    }

    private void thenMessagePublished(long sendDelaySec, String... changedPids)
            throws InterruptedException, ExecutionException {
        CountDownLatch waitForSendDelay = new CountDownLatch(1);
        this.mockPublisher.addPublishCountLatch(waitForSendDelay);
        waitForSendDelay.await(sendDelaySec + 5, TimeUnit.SECONDS);

        for (String pid : changedPids) {
            assertTrue("Not all pids have been published.", this.mockPublisher.isPidPublished(pid));
        }
    }

    /*
     * Utilities
     */

    @Before
    public void cleanup() throws InvalidSyntaxException {
        this.configurationChangeManager.setCloudPublisher(this.mockPublisher);
        this.mockServiceTracker.setServiceTrackerListener(this.configurationChangeManager);
    }

}
