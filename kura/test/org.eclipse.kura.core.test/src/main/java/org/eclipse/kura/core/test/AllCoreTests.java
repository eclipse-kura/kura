/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Eurotech
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

import io.moquette.server.Server;

@RunWith(Suite.class)
@SuiteClasses({ DataServiceTest.class, CloudDeploymentHandlerTest.class, CloudServiceTest.class, CommURITest.class,
        ComponentConfigurationImplTest.class, ConfigurationServiceTest.class, NetUtilTest.class,
        NetworkServiceTest.class, SystemAdminServiceTest.class })
public class AllCoreTests {

    private static final Logger s_logger = LoggerFactory.getLogger(AllCoreTests.class);

    /** A latch to be initialized with the no of OSGi dependencies needed */
    private static CountDownLatch dependencyLatch = new CountDownLatch(3);

    private static ConfigurationService s_configService;
    private static DataService s_dataService;
    private static SystemService s_sysService;

    public void setConfigService(ConfigurationService configService) {
        s_configService = configService;
        dependencyLatch.countDown();
    }

    public void unsetConfigService(ConfigurationService configService) {
        s_configService = configService;
    }

    public void setDataService(DataService dataService) {
        s_dataService = dataService;
        dependencyLatch.countDown();
    }

    public void unsetDataService(DataService dataService) {
        s_dataService = dataService;
    }

    public void setSystemService(SystemService sysService) {
        s_sysService = sysService;
        dependencyLatch.countDown();
    }

    public void unsetSystemService(SystemService sysService) {
        s_sysService = sysService;
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        s_logger.info("setUpClass...");

        // start Moquette
        Server.main(new String[] {});

        // Wait for OSGi dependencies
        s_logger.info("Setting Up The Testcase....");
        try {
            dependencyLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("OSGi dependencies unfulfilled", e);
        }

        try {
            // update the settings
            ComponentConfiguration mqttConfig = s_configService
                    .getComponentConfiguration("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport");
            Map<String, Object> mqttProps = mqttConfig.getConfigurationProperties();

            // mqttProps.put("broker-url", "mqtt://iot.eclipse.org:1883/");
            // mqttProps.put("topic.context.account-name", "guest");
            // mqttProps.put("username", "guest");
            // mqttProps.put("password", "welcome");

            s_logger.info("Changing cloud credentials...");
            mqttProps.put("broker-url", "mqtt://localhost:1883/");
            mqttProps.put("topic.context.account-name", "ethdev");
            mqttProps.put("username", "");
            mqttProps.put("password", "");

            // cloudbees fails in getting the primary MAC address
            // we need to compensate for it.
            String clientId = null;
            try {
                clientId = s_sysService.getPrimaryMacAddress();
            } catch (Throwable t) {
                // ignore.
            }
            if (clientId == null || clientId.isEmpty()) {
                clientId = "cloudbees-kura";
            }
            mqttProps.put("client-id", clientId);
            s_configService.updateConfiguration("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport",
                    mqttProps);

            ComponentConfiguration dataConfig = s_configService
                    .getComponentConfiguration("org.eclipse.kura.data.DataService");
            Map<String, Object> dataProps = dataConfig.getConfigurationProperties();
            dataProps.put("connect.auto-on-startup", false);
            dataProps.put("enable.rate.limit", false);
            s_configService.updateConfiguration("org.eclipse.kura.data.DataService", dataProps);

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
        if (!s_dataService.isConnected()) {
            s_dataService.connect();
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        s_logger.info("tearDownClass...");
        if (s_dataService != null && s_dataService.isConnected()) {
            s_dataService.disconnect(0);
        }
    }
}
