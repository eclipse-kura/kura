/*******************************************************************************
 * Copyright (c) 2011, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.system.SystemService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.moquette.broker.Server;

@RunWith(Suite.class)
@SuiteClasses({ MqttDataTransportTest.class, DataServiceTest.class,
        CloudServiceTest.class })
public class BaseCloudTests {

    private static final Logger logger = LoggerFactory.getLogger(BaseCloudTests.class);

    private static CountDownLatch dependencyLatch = new CountDownLatch(3);
    private static ConfigurationService configService;
    private static DataService dataService;
    private static SystemService systemService;

    public void setConfigService(ConfigurationService configService) {
        BaseCloudTests.configService = configService;
        dependencyLatch.countDown();
    }

    public void unsetConfigService(ConfigurationService configService) {
        if (BaseCloudTests.configService.equals(configService)) {
            BaseCloudTests.configService = null;
        }
    }

    public void setDataService(DataService dataService) {
        BaseCloudTests.dataService = dataService;
        dependencyLatch.countDown();
    }

    public void unsetDataService(DataService dataService) {
        if (BaseCloudTests.dataService.equals(dataService)) {
            BaseCloudTests.dataService = null;
        }
    }

    public void setSystemService(SystemService systemService) {
        BaseCloudTests.systemService = systemService;
        dependencyLatch.countDown();
    }

    public void unsetSystemService(SystemService systemService) {
        if (BaseCloudTests.systemService.equals(systemService)) {
            BaseCloudTests.systemService = null;
        }
    }

    @BeforeClass
    public static void setupTestClass() throws Exception {
        logger.info("setupTestClass...");
        Server.main(new String[] {});
        try {
            dependencyLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("OSGi dependencies unfulfilled", e);
        }

        try {
            ComponentConfiguration mqttConfig = configService
                    .getComponentConfiguration("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport");
            Map<String, Object> mqttProps = mqttConfig.getConfigurationProperties();

            logger.info("Changing cloud credentials...");
            mqttProps.put("broker-url", "mqtt://localhost:1883/");
            mqttProps.put("topic.context.account-name", "ethdev");
            mqttProps.put("username", "");
            mqttProps.put("password", "");

            String clientId = null;
            try {
                clientId = systemService.getPrimaryMacAddress();
            } catch (Throwable t) {
                // do nothing
            }
            if (clientId == null || clientId.isEmpty()) {
                clientId = "cloudbees-kura";
            }
            mqttProps.put("client-id", clientId);
            configService.updateConfiguration("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport",
                    mqttProps);

            ComponentConfiguration dataConfig = configService
                    .getComponentConfiguration("org.eclipse.kura.data.DataService");
            Map<String, Object> dataProps = dataConfig.getConfigurationProperties();
            dataProps.put("connect.auto-on-startup", false);
            dataProps.put("enable.rate.limit", false);
            configService.updateConfiguration("org.eclipse.kura.data.DataService", dataProps);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            throw new Exception("Failed to reconfigure the broker settings - failing out", e);
        }

        if (!dataService.isConnected()) {
            dataService.connect();
        }
    }

    @AfterClass
    public static void teardownTestClass() throws Exception {
        logger.info("teardownTestClass...");
        if (dataService != null && dataService.isConnected()) {
            dataService.disconnect(0);
        }
    }
}
