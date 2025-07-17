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
 *******************************************************************************/
package org.eclipse.kura.core.test;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bouncycastle.asn1.x500.X500Name;
import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.testutil.pki.TestCA;
import org.eclipse.kura.core.testutil.pki.TestCA.CertificateCreationOptions;
import org.eclipse.kura.core.testutil.pki.TestCA.TestCAException;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.moquette.broker.Server;
import io.moquette.broker.config.FluentConfig;
import io.moquette.broker.config.IConfig;

public class BaseCloudTests {

    private static final Logger logger = LoggerFactory.getLogger(BaseCloudTests.class);

    protected static final String BROKER_ADDR_MQTT = "mqtt://localhost:1883";
    protected static final String BROKER_ADDR_MQTTS = "mqtts://localhost:8883";
    protected static final String BROKER_ADDR_WSS = "wss://localhost:8083";

    protected static final String DEFAULT_CLOUD_SERVICE_PID = "org.eclipse.kura.cloud.CloudService";
    protected static final String DEFAULT_DATA_SERVICE_PID = "org.eclipse.kura.data.DataService";
    protected static final String DEFAULT_MQTT_DATA_TRANSPORT_SERVICE_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";

    protected static ConfigurationService configurationService;
    protected static DataService dataService;
    protected static CloudService cloudService;
    private static Server mqttBroker;

    protected static TestCA brokerCA;
    protected static X509Certificate brokerCertificate;
    protected static File brokerKeyStore;
    protected static File mqttKeyStore;
    protected static File mqttKeyStoreKeyOnly;
    protected static File mqttTrustStore;

    @BeforeClass
    public static void setupTestClass() throws Exception {
        try {
            logger.info("Setup BaseCloudTests");

            configureKeystores();
            startMoquetteBroker(brokerKeyStore.getAbsolutePath());

            configurationService = WireTestUtil
                    .trackService(ConfigurationService.class, Optional.empty()).get(30,
                    TimeUnit.SECONDS);
            dataService = WireTestUtil.trackService(DataService.class, Optional.empty()).get(30, TimeUnit.SECONDS);
            cloudService = WireTestUtil.trackService(CloudService.class, Optional.empty()).get(30, TimeUnit.SECONDS);

            configureCloudService();
            configureDataService();
            configureMqttDataTransport();

            connectDataService();
        } catch (Exception e) {
            throw new Exception("Failed to reconfigure the broker settings - failing out", e);
        }
    }

    @AfterClass
    public static void teardownTestClass() throws Exception {
        logger.info("teardownTestClass...");
        if (dataService != null && dataService.isConnected()) {
            dataService.disconnect(0);
        }
        stopMoquetteBroker();
    }

    private static void startMoquetteBroker(String jksPath) throws IOException {
        if (mqttBroker != null) {
            logger.info("Moquette broker already running");
            return;
        }

        IConfig brokerConfig = new FluentConfig().port(1883).host("0.0.0.0").disablePersistence().build();
        brokerConfig.setProperty(IConfig.NETTY_MAX_BYTES_PROPERTY_NAME, "16777216");
        brokerConfig.setProperty(IConfig.JKS_PATH_PROPERTY_NAME, jksPath);
        brokerConfig.setProperty(IConfig.KEY_STORE_PASSWORD_PROPERTY_NAME, "changeit");
        brokerConfig.setProperty(IConfig.KEY_MANAGER_PASSWORD_PROPERTY_NAME, "changeit");
        brokerConfig.setProperty(IConfig.SSL_PORT_PROPERTY_NAME, "8883");
        brokerConfig.setProperty(IConfig.WEB_SOCKET_PORT_PROPERTY_NAME, "8080");
        brokerConfig.setProperty(IConfig.WSS_PORT_PROPERTY_NAME, "8083");

        mqttBroker = new Server();
        mqttBroker.startServer(brokerConfig);

        Runtime.getRuntime().addShutdownHook(new Thread(BaseCloudTests::stopMoquetteBroker));
        logger.info("Moquette broker started");
    }

    private static void stopMoquetteBroker() {
        if (mqttBroker != null) {
            mqttBroker.stopServer();
            mqttBroker = null;
            logger.info("Moquette broker stopped");
        }
    }

    private static void configureKeystores() throws TestCAException, IOException {
        final KeyPair brokerKeyPair = TestCA.generateKeyPair();
        final TestCA clientCA = new TestCA(
                CertificateCreationOptions.builder(new X500Name("cn=client CA, dc=baz.com")).build());
        final KeyPair clientKeyPair = TestCA.generateKeyPair();
        final X509Certificate clientCertificate = clientCA.createAndSignCertificate(
                CertificateCreationOptions.builder(new X500Name("cn=client, dc=baz.com")).build(), clientKeyPair);

        brokerCA = new TestCA(CertificateCreationOptions.builder(new X500Name("cn=broker CA, dc=bar.com")).build());
        brokerCertificate = brokerCA.createAndSignCertificate(
                CertificateCreationOptions.builder(new X500Name("cn=broker, dc=bar.com")).build(), brokerKeyPair);

        brokerKeyStore = TestCA.writeKeystore( //
                new PrivateKeyEntry(brokerKeyPair.getPrivate(),
                        new Certificate[] { brokerCertificate, brokerCA.getCertificate() }), //
                new TrustedCertificateEntry(clientCA.getCertificate()));
        mqttKeyStore = TestCA.writeKeystore(
                new PrivateKeyEntry(clientKeyPair.getPrivate(),
                        new Certificate[] { clientCertificate, clientCA.getCertificate() }),
                new TrustedCertificateEntry(brokerCertificate));
        mqttKeyStoreKeyOnly = TestCA.writeKeystore(new PrivateKeyEntry(clientKeyPair.getPrivate(),
                new Certificate[] { clientCertificate, clientCA.getCertificate() }));
        mqttTrustStore = TestCA.writeKeystore(new TrustedCertificateEntry(brokerCA.getCertificate()));
    }

    private static void configureCloudService()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        final Map<String, Object> cloudServiceProperties = new HashMap<>();
        cloudServiceProperties.put("payload.encoding", "simple-json");
        /*
         * Set a control topic without $ as prefix: some brokers do not allow access to topics starting with $ (like
         * moquette, mosquitto)
         */
        cloudServiceProperties.put("topic.control-prefix", "EDC");

        ServiceUtil
                .updateComponentConfiguration(configurationService, DEFAULT_CLOUD_SERVICE_PID, cloudServiceProperties)
                .get(30, TimeUnit.SECONDS);
    }

    private static void configureDataService()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        Map<String, Object> dataProps = new HashMap<>();
        dataProps.put("connect.auto-on-startup", false);
        dataProps.put("enable.rate.limit", false);

        ServiceUtil
                .updateComponentConfiguration(configurationService, DEFAULT_DATA_SERVICE_PID, dataProps)
                .get(30, TimeUnit.SECONDS);
    }

    private static void configureMqttDataTransport()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("broker-url", "mqtt://localhost:1883/");
        properties.put("username", "mqtt");
        properties.put("client-id", "test");
        properties.put("topic.context.account-name", "mqtt");

        ServiceUtil.updateComponentConfiguration(configurationService, DEFAULT_MQTT_DATA_TRANSPORT_SERVICE_PID,
                properties).get(30, TimeUnit.SECONDS);
    }

    protected static void connectDataService() throws KuraConnectException {
        if (!dataService.isConnected()) {
            dataService.connect();
        }
    }

}
