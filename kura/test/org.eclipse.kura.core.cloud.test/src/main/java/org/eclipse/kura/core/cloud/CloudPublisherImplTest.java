/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.cloud;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloudconnection.CloudConnectionConstants;
import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.BeforeClass;
import org.junit.Test;

public class CloudPublisherImplTest {

    private static CountDownLatch dependencyLatch = new CountDownLatch(4);

    private static ConfigurationService cfgSvc;

    private static CloudConnectionFactory cloudConnectionFactory;

    private static CloudServiceImpl cloudServiceImpl;

    private static CloudPublisher cloudPublisher;

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
        CloudPublisherImplTest.cfgSvc = cfgSvc;
        dependencyLatch.countDown();
    }

    public void unbindCfgSvc(ConfigurationService cfgSvc) {
        CloudPublisherImplTest.cfgSvc = null;
    }

    public void bindCloudFactory(CloudConnectionFactory cloudConnectionFactory) throws KuraException {
        CloudPublisherImplTest.cloudConnectionFactory = cloudConnectionFactory;
        if ("org.eclipse.kura.cloud.CloudService".equals(cloudConnectionFactory.getFactoryPid())) {
            cloudConnectionFactory.createConfiguration("org.eclipse.kura.cloud.CloudService");
            dependencyLatch.countDown();
        }
    }

    public void bindCloudService(CloudService cloudService) throws KuraException {
        cloudServiceImpl = (CloudServiceImpl) cloudService;
        cfgSvc.createFactoryConfiguration("org.eclipse.kura.cloud.publisher.CloudPublisher",
                "org.eclipse.kura.cloud.publisher.CloudPublisher-1",
                Collections.singletonMap(CloudConnectionConstants.CLOUD_CONNECTION_SERVICE_PID_PROP_NAME.value(),
                        "org.eclipse.kura.cloud.CloudService"),
                true);
        dependencyLatch.countDown();
    }

    public void unbindCloudService(CloudService cloudService) {
        cloudServiceImpl = null;
    }

    public void unbindCloudFactory(CloudConnectionFactory cloudConnectionFactory) {
        CloudPublisherImplTest.cloudConnectionFactory = null;
    }

    public void bindCloudPublisher(CloudPublisher cloudPublisher) {
        CloudPublisherImplTest.cloudPublisher = cloudPublisher;
        dependencyLatch.countDown();
    }

    public void unbindCloudPublisher(CloudPublisher cloudPublisher) {
        CloudPublisherImplTest.cloudPublisher = null;
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testServiceExists() {
        assertNotNull(CloudPublisherImplTest.cfgSvc);
        assertNotNull(CloudPublisherImplTest.cloudConnectionFactory);
        assertNotNull(CloudPublisherImplTest.cloudServiceImpl);
        assertNotNull(CloudPublisherImplTest.cloudPublisher);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test(expected = IllegalArgumentException.class)
    public void testPublishNullMessage() throws KuraException {
        cloudPublisher.publish(null);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testPublishQos0() throws KuraException {
        KuraPayload payload = new KuraPayload();
        KuraMessage message = new KuraMessage(payload);

        String result = cloudPublisher.publish(message);
        assertNull(result);
    }
    
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testPublishQos1() throws KuraException {
        KuraPayload payload = new KuraPayload();
        KuraMessage message = new KuraMessage(payload);

        ComponentConfiguration cloudPubConfig = cfgSvc.getComponentConfiguration("org.eclipse.kura.cloud.publisher.CloudPublisher-1");
        Map<String, Object> cloudPubConfigProps = cloudPubConfig.getConfigurationProperties();
        cloudPubConfigProps.put("qos", 1);
        cfgSvc.updateConfiguration("org.eclipse.kura.cloud.publisher.CloudPublisher-1", cloudPubConfigProps);
        
        String result = cloudPublisher.publish(message);
        assertNotNull(result);
    }

}
