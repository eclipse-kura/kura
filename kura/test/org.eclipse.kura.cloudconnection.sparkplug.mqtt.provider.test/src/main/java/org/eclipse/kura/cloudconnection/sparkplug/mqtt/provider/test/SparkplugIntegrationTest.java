/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.provider.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudConnectionConstants;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.device.SparkplugDevice;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.moquette.broker.Server;
import io.moquette.broker.config.ClasspathResourceLoader;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.IResourceLoader;
import io.moquette.broker.config.ResourceLoaderConfig;

public class SparkplugIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(SparkplugIntegrationTest.class);

    private static final String SPARKPLUG_FACTORY_PID = "org.eclipse.kura.cloudconnection.sparkplug.mqtt.factory.SparkplugCloudConnectionFactory";
    static final String CLOUD_ENDPOINT_PID = "org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint";
    static final String DATA_TRANSPORT_SERVICE_PID = "org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport";
    static final String SPARKPLUG_DEVICE_PID = "test.device";

    private static CountDownLatch dependenciesLatch = new CountDownLatch(1);

    static ConfigurationService configurationService;
    static SparkplugCloudEndpoint sparkplugCloudEndpoint;
    static SparkplugDataTransport sparkplugDataTransport;
    static SparkplugDevice sparkplugDevice;
    static MqttClient client;
    static Server mqttBroker;

    public void bindConfigurationService(ConfigurationService confService) {
        configurationService = confService;
        dependenciesLatch.countDown();
        logger.info("ConfigurationService ready");
    }

    public void activate() {
        logger.info("Activating {}", this.getClass().getName());
    }

    @BeforeClass
    public static void initialize() {
        try {
            if (!dependenciesLatch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Test dependencies not satisfied");
            }

            startMqttBroker();
            createSparkplugCloudConnection();
            connectDefaultPahoClient();
            createSparkplugDevice();

            logger.info("Test environment successfully setup");
        } catch (InterruptedException e) {
            fail("Error in environment setup. See logs");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Error during test environment setup", e);
        }
    }

    @AfterClass
    public static void tearDown() throws MqttException {
        client.disconnect();
        stopBroker();
    }

    @Test
    public void shouldBeSetup() {
        assertNotNull(configurationService);
        assertNotNull(sparkplugCloudEndpoint);
        assertNotNull(sparkplugDataTransport);
    }

    public static void startMqttBroker() throws Exception {
        IResourceLoader classpathLoader = new ClasspathResourceLoader();
        IConfig classPathConfig = new ResourceLoaderConfig(classpathLoader);

        mqttBroker = new Server();
        mqttBroker.startServer(classPathConfig);
        logger.info("Moquette MQTT broker started");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> stopBroker()));
    }

    public static void stopBroker() {
        mqttBroker.stopServer();
        logger.info("Moquette MQTT broker stopped");
    }

    public static <T> T trackService(Class<T> clazz, String pid)
            throws InterruptedException, ExecutionException, TimeoutException {
        return (T) ServiceUtil.trackService(clazz, Optional.of(String.format("(kura.service.pid=%s)", pid))).get(30,
                TimeUnit.SECONDS);
    }

    private static void createSparkplugCloudConnection()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException {
        if (!configurationService.getConfigurableComponentPids().contains(SPARKPLUG_FACTORY_PID)) {
            CloudConnectionFactory factory = ServiceUtil.createFactoryConfiguration(configurationService,
                    CloudConnectionFactory.class, SPARKPLUG_FACTORY_PID, SPARKPLUG_FACTORY_PID, null)
                    .get(30, TimeUnit.SECONDS);
            factory.createConfiguration(CLOUD_ENDPOINT_PID);

            sparkplugCloudEndpoint = (SparkplugCloudEndpoint) trackService(CloudEndpoint.class, CLOUD_ENDPOINT_PID);
            sparkplugDataTransport = (SparkplugDataTransport) trackService(DataTransportService.class,
                    DATA_TRANSPORT_SERVICE_PID);

            deactivateSsl();

            logger.info("Got references for Sparkplug CloudEndpoint and DataTransportService");
        }
    }

    private static void createSparkplugDevice()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException {
        if (!configurationService.getConfigurableComponentPids().contains(SPARKPLUG_DEVICE_PID)) {
            final Map<String, Object> properties = new HashMap<>();
            properties.put(CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value(), CLOUD_ENDPOINT_PID);
            properties.put(SparkplugDevice.KEY_DEVICE_ID, "d1");

            configurationService.createFactoryConfiguration(
                    "org.eclipse.kura.cloudconnection.sparkplug.mqtt.device.SparkplugDevice", SPARKPLUG_DEVICE_PID,
                    properties, false);
            sparkplugDevice = (SparkplugDevice) trackService(CloudPublisher.class, SPARKPLUG_DEVICE_PID);
        }
    }

    private static void connectDefaultPahoClient() throws Exception {
        client = new MqttClient("tcp://localhost:1883", "sparkplug.it.test", new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(false);
        options.setCleanSession(true);
        client.connect(options);
    }

    private static void deactivateSsl() {
        SslManagerService sslService = mock(SslManagerService.class);
        sparkplugDataTransport.setSslManagerService(sslService);
        sparkplugDataTransport.unsetSslManagerService(sslService);
    }

}
