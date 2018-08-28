/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.BeforeClass;
import org.junit.Test;

public class CloudServiceTest {

    private static CountDownLatch dependencyLatch = new CountDownLatch(3);

    private static ConfigurationService cfgSvc;

    private static CloudConnectionFactory cloudConnectionFactory;

    private static CloudServiceImpl cloudServiceImpl;

    @BeforeClass
    public static void setup() throws KuraException {
        try {
            dependencyLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    public void bindCfgSvc(ConfigurationService cfgSvc) {
        CloudServiceTest.cfgSvc = cfgSvc;
        dependencyLatch.countDown();
    }

    public void unbindCfgSvc(ConfigurationService cfgSvc) {
        CloudServiceTest.cfgSvc = null;
    }

    public void bindCloudFactory(CloudConnectionFactory cloudConnectionFactory) throws KuraException {
        CloudServiceTest.cloudConnectionFactory = cloudConnectionFactory;
        if ("org.eclipse.kura.cloud.CloudService".equals(cloudConnectionFactory.getFactoryPid())) {
            cloudConnectionFactory.createConfiguration("org.eclipse.kura.cloud.CloudService");
            dependencyLatch.countDown();
        }
    }

    public void bindCloudService(CloudService cloudService) {
        cloudServiceImpl = (CloudServiceImpl) cloudService;
        dependencyLatch.countDown();
    }

    public void unbindCloudService(CloudService cloudService) {
        cloudServiceImpl = null;
    }

    public void unbindCloudFactory(CloudConnectionFactory cloudConnectionFactory) {
        CloudServiceTest.cloudConnectionFactory = null;
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testServiceExists() {
        assertNotNull(CloudServiceTest.cfgSvc);
        assertNotNull(CloudServiceTest.cloudConnectionFactory);
        assertNotNull(CloudServiceTest.cloudServiceImpl);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test(expected = KuraException.class)
    public void testConnectCannotConnect() throws KuraException {
        cloudServiceImpl.connect();
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testDisconnect() throws KuraException {
        cloudServiceImpl.disconnect();
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetConnectionInfo() {
        Map<String, String> connectionProps = cloudServiceImpl.getInfo();

        assertNotNull(connectionProps);
        assertEquals(4, connectionProps.size());
        assertNotNull(connectionProps.get("Broker URL"));
        assertNotNull(connectionProps.get("Account"));
        assertNotNull(connectionProps.get("Username"));
        assertNotNull(connectionProps.get("Client ID"));
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetNotificationPublisherPid() {
        String pid = cloudServiceImpl.getNotificationPublisherPid();
        assertEquals("org.eclipse.kura.cloud.publisher.CloudNotificationPublisher", pid);
    }

}
