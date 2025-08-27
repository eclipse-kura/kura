/*******************************************************************************
 * Copyright (c) 2011, 2025 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.test;

import java.util.HashMap;
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
import io.moquette.broker.config.FileResourceLoader;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.IResourceLoader;
import io.moquette.broker.config.ResourceLoaderConfig;

@RunWith(Suite.class)
@SuiteClasses({ InventoryHandlerTest.class, CloudDeploymentHandlerTest.class, CommURITest.class,
        ComponentConfigurationImplTest.class, ConfigurationServiceTest.class, NetUtilTest.class,
        NetworkServiceTest.class, SystemAdminServiceTest.class })
public class AllCoreTests {

    private static final Logger logger = LoggerFactory.getLogger(AllCoreTests.class);

    /** A latch to be initialized with the no of OSGi dependencies needed */
    private static CountDownLatch dependencyLatch = new CountDownLatch(3);

    private static ConfigurationService configService;
    private static DataService dataService;
    private static SystemService sysService;

    static Server mqttBroker;

    public void setConfigService(ConfigurationService configService) {
        AllCoreTests.configService = configService;
        dependencyLatch.countDown();
    }

    public void unsetConfigService(ConfigurationService configService) {
        AllCoreTests.configService = configService;
    }

    public void setDataService(DataService dataService) {
        AllCoreTests.dataService = dataService;
        dependencyLatch.countDown();
    }

    public void unsetDataService(DataService dataService) {
        AllCoreTests.dataService = dataService;
    }

    public void setSystemService(SystemService sysService) {
        AllCoreTests.sysService = sysService;
        dependencyLatch.countDown();
    }

    public void unsetSystemService(SystemService sysService) {
        AllCoreTests.sysService = sysService;
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        logger.info("setUpClass...");

        // start Moquette
        startMqttBroker();

        // Wait for OSGi dependencies
        logger.info("Setting Up The Testcase....");
        try {
            dependencyLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("OSGi dependencies unfulfilled", e);
        }

        try {
            // update the settings
            ComponentConfiguration mqttConfig = configService
                    .getComponentConfiguration("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport");
            Map<String, Object> mqttProps = mqttConfig.getConfigurationProperties();

            logger.info("Changing cloud credentials...");
            mqttProps.put("broker-url", "mqtt://localhost:1883/");
            mqttProps.put("topic.context.account-name", "ethdev");
            mqttProps.put("username", "");
            mqttProps.put("password", "");

            // cloudbees fails in getting the primary MAC address
            // we need to compensate for it.
            String clientId = null;
            try {
                clientId = sysService.getPrimaryMacAddress();
            } catch (Exception t) {
                // ignore.
            }
            if (clientId == null || clientId.isEmpty()) {
                clientId = "cloudbees-kura";
            }
            mqttProps.put("client-id", clientId);
            configService.updateConfiguration("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport", mqttProps);

            ComponentConfiguration dataConfig = configService
                    .getComponentConfiguration("org.eclipse.kura.data.DataService");
            Map<String, Object> dataProps = dataConfig.getConfigurationProperties();
            dataProps.put("connect.auto-on-startup", false);
            dataProps.put("enable.rate.limit", false);
            configService.updateConfiguration("org.eclipse.kura.data.DataService", dataProps);

            Map<String, Object> cloudProps = new HashMap<>();
            cloudProps.put("topic.control-prefix", "EDC");
            configService.updateConfiguration("org.eclipse.kura.cloud.CloudService", cloudProps);

            // waiting for the configuration to be applied
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            throw new Exception("Failed to reconfigure the broker settings - failing out", e);
        }

        // connect
        if (!dataService.isConnected()) {
            dataService.connect();
        }
    }

    public static void startMqttBroker() throws Exception {

        logger.info("Starting Moquette MQTT broker... with path: {}", "moquette.conf");
        IResourceLoader fileLoader = new FileResourceLoader();
        IConfig classPathConfig = new ResourceLoaderConfig(fileLoader, "moquette.conf");

        mqttBroker = new Server();
        mqttBroker.startServer(classPathConfig);
        logger.info("Moquette MQTT broker started");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> stopBroker()));
    }

    public static void stopBroker() {
        if (mqttBroker != null) {
            mqttBroker.stopServer();
        }
        logger.info("Moquette MQTT broker stopped");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        logger.info("tearDownClass...");
        if (dataService != null && dataService.isConnected()) {
            dataService.disconnect(0);
        }
        stopBroker();
    }
}
