/*******************************************************************************
 * Copyright (c) 2021, 2025 Eurotech and/or its affiliates and others
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.cert.X509CRL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.core.testutil.event.EventAdminUtil;
import org.eclipse.kura.core.testutil.http.TestServer;
import org.eclipse.kura.core.testutil.pki.TestCA;
import org.eclipse.kura.core.testutil.pki.TestCA.CRLCreationOptions;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.security.keystore.KeystoreChangedEvent;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class MqttDataTransportTest extends BaseCloudTests {

    private static final String TEST_MQTT_DATA_TRANSPORT_PID = "test";
    private static final String SSL_MANAGER_SERVICE_FACTORY_PID = "org.eclipse.kura.ssl.SslManagerService";
    private static final String TEST_SSL_MANAGER_SERVICE_FILTER = "(kura.service.pid=testSsl)";
    private static final String TEST_KEYSTORE_FILTER = "(kura.service.pid=testKeystore)";
    private static final String TEST_TRUSTSTORE_FILTER = "(kura.service.pid=testTruststore)";
    private static final String TEST_SSL_MANAGER_SERVICE_PID = "testSsl";
    private static final String TEST_KEYSTORE_PID = "testKeystore";
    private static final String TEST_TRUSTSTORE_PID = "testTruststore";
    private static final String KEYSTORE_SERVICE_FACTORY_PID = "org.eclipse.kura.core.keystore.FilesystemKeystoreServiceImpl";
    private static final String MQTT_DATA_TRANSPORT_FACTORY_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";

    @Test
    public void shouldConnectOverPlainMqtt() throws Exception {
        try (final Fixture fixture = new Fixture()) {
            final DataTransportService test = fixture.createFactoryConfiguration(DataTransportService.class,
                    TEST_MQTT_DATA_TRANSPORT_PID, MQTT_DATA_TRANSPORT_FACTORY_PID, MqttDataTransportOptions
                            .defaultConfiguration().withBrokerUrl(BROKER_ADDR_MQTT).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            test.connect();
        }
    }

    @Test
    public void shouldNotConnectOverMqttsWithoutKeystore() throws Exception {
        try (final Fixture fixture = new Fixture()) {
            final DataTransportService test = fixture.createFactoryConfiguration(DataTransportService.class,
                    TEST_MQTT_DATA_TRANSPORT_PID, MQTT_DATA_TRANSPORT_FACTORY_PID, MqttDataTransportOptions
                            .defaultConfiguration().withBrokerUrl(BROKER_ADDR_MQTTS).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            try {
                test.connect();
            } catch (final KuraConnectException e) {
                return;
            }

            fail("connection should have failed");
        }
    }

    @Test
    public void shouldNotConnectOverMqttsWithHostnameIdentificationEnabled() throws Exception {
        try (final Fixture fixture = new Fixture()) {

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_KEYSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID, KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttTrustStore.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_SSL_MANAGER_SERVICE_PID,
                    SSL_MANAGER_SERVICE_FACTORY_PID,
                    SslManagerServiceOptions.defaultConfiguration().withHostnameVerification(true)
                            .withKeystoreTargetFilter(TEST_KEYSTORE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            final DataTransportService test = fixture
                    .createFactoryConfiguration(DataTransportService.class, TEST_MQTT_DATA_TRANSPORT_PID,
                            MQTT_DATA_TRANSPORT_FACTORY_PID,
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl(BROKER_ADDR_MQTTS)
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            try {
                test.connect();
            } catch (final KuraConnectException e) {
                return;
            }

            fail("connection should have failed");
        }
    }

    @Test
    public void shouldNotConnectOverWssWithHostnameIdentificationEnabled() throws Exception {
        try (final Fixture fixture = new Fixture()) {

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_KEYSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID, KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttTrustStore.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_SSL_MANAGER_SERVICE_PID,
                    SSL_MANAGER_SERVICE_FACTORY_PID,
                    SslManagerServiceOptions.defaultConfiguration().withHostnameVerification(true)
                            .withKeystoreTargetFilter(TEST_KEYSTORE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            final DataTransportService test = fixture
                    .createFactoryConfiguration(DataTransportService.class, TEST_MQTT_DATA_TRANSPORT_PID,
                            MQTT_DATA_TRANSPORT_FACTORY_PID,
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl(BROKER_ADDR_WSS)
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            try {
                test.connect();
            } catch (final KuraConnectException e) {
                return;
            }

            fail("connection should have failed");
        }
    }

    @Test
    public void shouldConnectOverMqttsWithHostnameIdentificationDisabled() throws Exception {
        try (final Fixture fixture = new Fixture()) {

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_KEYSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID, KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttTrustStore.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_SSL_MANAGER_SERVICE_PID,
                    SSL_MANAGER_SERVICE_FACTORY_PID,
                    SslManagerServiceOptions.defaultConfiguration().withKeystoreTargetFilter(TEST_KEYSTORE_FILTER)
                            .withHostnameVerification(false).toProperties())
                    .get(30, TimeUnit.SECONDS);

            final DataTransportService test = fixture
                    .createFactoryConfiguration(DataTransportService.class, TEST_MQTT_DATA_TRANSPORT_PID,
                            MQTT_DATA_TRANSPORT_FACTORY_PID,
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl(BROKER_ADDR_MQTTS)
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            test.connect();
        }
    }

    @Test
    public void shouldSupportRevocation() throws Exception {
        final BundleContext bundleContext = FrameworkUtil.getBundle(MqttDataTransportTest.class).getBundleContext();

        final TestServer server = new TestServer(8087, Optional.empty());

        server.setResource("/crl.pem", encodeCrl(brokerCA.generateCRL(CRLCreationOptions.builder().build())));

        try (final Fixture fixture = new Fixture()) {

            CompletableFuture<KeystoreChangedEvent> nextEvent = EventAdminUtil.nextEvent(
                    new String[] { KeystoreChangedEvent.EVENT_TOPIC }, KeystoreChangedEvent.class, bundleContext);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_KEYSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID,
                    KeystoreServiceOptions.defaultConfiguration().withKeystorePath(mqttTrustStore.getAbsolutePath())
                            .withCrlManagerEnabled(true).withCrlUrls(new String[] { "http://localhost:8087/crl.pem" })
                            .toProperties())
                    .get(30, TimeUnit.SECONDS);

            nextEvent.get(10, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_SSL_MANAGER_SERVICE_PID,
                    SSL_MANAGER_SERVICE_FACTORY_PID,
                    SslManagerServiceOptions.defaultConfiguration().withKeystoreTargetFilter(TEST_KEYSTORE_FILTER)
                            .withHostnameVerification(false).withRevocationCheckEnabled(true)
                            .withRevocationCheckMode(SslManagerServiceOptions.RevocationCheckMode.CRL_ONLY)
                            .toProperties())
                    .get(30, TimeUnit.SECONDS);

            final DataTransportService test = fixture
                    .createFactoryConfiguration(DataTransportService.class, TEST_MQTT_DATA_TRANSPORT_PID,
                            MQTT_DATA_TRANSPORT_FACTORY_PID,
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl(BROKER_ADDR_MQTTS)
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);

            test.connect();

            nextEvent = EventAdminUtil.nextEvent(new String[] { KeystoreChangedEvent.EVENT_TOPIC },
                    KeystoreChangedEvent.class, bundleContext);

            brokerCA.revokeCertificate(brokerCertificate);
            server.setResource("/crl.pem", encodeCrl(brokerCA.generateCRL(CRLCreationOptions.builder().build())));

            nextEvent.get(10, TimeUnit.SECONDS);

            test.disconnect(0);

            try {
                test.connect();
            } catch (final KuraConnectException e) {
                return;
            }

            fail("connection should have failed");
        } finally {
            server.close();
        }
    }

    private static byte[] encodeCrl(final X509CRL crl) throws IOException {
        final byte[] crlData;

        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            TestCA.encodeToPEM(crl, out);
            crlData = out.toByteArray();
        }

        return crlData;
    }

    @Test
    public void shouldConnectOverWssWithHostnameIdentificationDisabled() throws Exception {
        try (final Fixture fixture = new Fixture()) {

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_KEYSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID, KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttTrustStore.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_SSL_MANAGER_SERVICE_PID,
                    SSL_MANAGER_SERVICE_FACTORY_PID,
                    SslManagerServiceOptions.defaultConfiguration().withKeystoreTargetFilter(TEST_KEYSTORE_FILTER)
                            .withHostnameVerification(false).toProperties())
                    .get(30, TimeUnit.SECONDS);

            final DataTransportService test = fixture
                    .createFactoryConfiguration(DataTransportService.class, TEST_MQTT_DATA_TRANSPORT_PID,
                            MQTT_DATA_TRANSPORT_FACTORY_PID,
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl(BROKER_ADDR_WSS)
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            test.connect();
        }
    }

    @Test
    @Ignore // reason: moquette broker not configured with truststore
    public void shouldNotConnectOverMqttsWithClientSideAuthWithoutKey() throws Exception {
        try (final Fixture fixture = new Fixture()) {

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_KEYSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID, KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttTrustStore.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_SSL_MANAGER_SERVICE_PID,
                    SSL_MANAGER_SERVICE_FACTORY_PID,
                    SslManagerServiceOptions.defaultConfiguration().withKeystoreTargetFilter(TEST_KEYSTORE_FILTER)
                            .withHostnameVerification(false).toProperties())
                    .get(30, TimeUnit.SECONDS);

            final DataTransportService test = fixture
                    .createFactoryConfiguration(DataTransportService.class, TEST_MQTT_DATA_TRANSPORT_PID,
                            MQTT_DATA_TRANSPORT_FACTORY_PID,
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl(BROKER_ADDR_MQTTS)
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            try {
                test.connect();
            } catch (final KuraConnectException e) {
                return;
            }

            fail("connection should have failed");
        }
    }

    @Test
    @Ignore // reason: moquette broker not configured with truststore
    public void shouldNotConnectOverWssWithClientSideAuthWithoutKey() throws Exception {
        try (final Fixture fixture = new Fixture()) {

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_KEYSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID, KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttTrustStore.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_SSL_MANAGER_SERVICE_PID,
                    SSL_MANAGER_SERVICE_FACTORY_PID,
                    SslManagerServiceOptions.defaultConfiguration().withKeystoreTargetFilter(TEST_KEYSTORE_FILTER)
                            .withHostnameVerification(false).toProperties())
                    .get(30, TimeUnit.SECONDS);

            final DataTransportService test = fixture
                    .createFactoryConfiguration(DataTransportService.class, TEST_MQTT_DATA_TRANSPORT_PID,
                            MQTT_DATA_TRANSPORT_FACTORY_PID,
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl(BROKER_ADDR_WSS)
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            try {
                test.connect();
            } catch (final KuraConnectException e) {
                return;
            }

            fail("connection should have failed");
        }
    }

    @Test
    public void shouldConnectOverMqttsWithClientSideAuth() throws Exception {
        try (final Fixture fixture = new Fixture()) {

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_KEYSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID, KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttKeyStore.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_SSL_MANAGER_SERVICE_PID,
                    SSL_MANAGER_SERVICE_FACTORY_PID,
                    SslManagerServiceOptions.defaultConfiguration().withKeystoreTargetFilter(TEST_KEYSTORE_FILTER)
                            .withHostnameVerification(false).toProperties())
                    .get(30, TimeUnit.SECONDS);

            final DataTransportService test = fixture
                    .createFactoryConfiguration(DataTransportService.class, TEST_MQTT_DATA_TRANSPORT_PID,
                            MQTT_DATA_TRANSPORT_FACTORY_PID,
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl(BROKER_ADDR_MQTTS)
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            test.connect();
        }
    }

    @Test
    public void shouldConnectOverWssWithClientSideAuth() throws Exception {
        try (final Fixture fixture = new Fixture()) {

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_KEYSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID, KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttKeyStore.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_SSL_MANAGER_SERVICE_PID,
                    SSL_MANAGER_SERVICE_FACTORY_PID,
                    SslManagerServiceOptions.defaultConfiguration().withKeystoreTargetFilter(TEST_KEYSTORE_FILTER)
                            .withHostnameVerification(false).toProperties())
                    .get(30, TimeUnit.SECONDS);

            final DataTransportService test = fixture
                    .createFactoryConfiguration(DataTransportService.class, TEST_MQTT_DATA_TRANSPORT_PID,
                            MQTT_DATA_TRANSPORT_FACTORY_PID,
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl(BROKER_ADDR_WSS)
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            test.connect();
        }
    }

    @Test
    public void shouldConnectOverWssWithClientSideAuthWithSeparateTruststore() throws Exception {
        try (final Fixture fixture = new Fixture()) {

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_TRUSTSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID, KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttTrustStore.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_KEYSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID,
                    KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttKeyStoreKeyOnly.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_SSL_MANAGER_SERVICE_PID,
                    SSL_MANAGER_SERVICE_FACTORY_PID,
                    SslManagerServiceOptions.defaultConfiguration().withKeystoreTargetFilter(TEST_KEYSTORE_FILTER)
                            .withTruststoreTargetFilter(TEST_TRUSTSTORE_FILTER).withHostnameVerification(false)
                            .toProperties())
                    .get(30, TimeUnit.SECONDS);

            final DataTransportService test = fixture
                    .createFactoryConfiguration(DataTransportService.class, TEST_MQTT_DATA_TRANSPORT_PID,
                            MQTT_DATA_TRANSPORT_FACTORY_PID,
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl(BROKER_ADDR_WSS)
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            test.connect();
        }
    }

    @Test
    public void shouldNotConnectOverWssWithClientSideAuthWithWrongTruststore() throws Exception {
        try (final Fixture fixture = new Fixture()) {

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_TRUSTSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID, KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttTrustStore.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_SSL_MANAGER_SERVICE_PID,
                    SSL_MANAGER_SERVICE_FACTORY_PID,
                    SslManagerServiceOptions.defaultConfiguration().withKeystoreTargetFilter(TEST_KEYSTORE_FILTER)
                            .withTruststoreTargetFilter(TEST_KEYSTORE_FILTER).withHostnameVerification(false)
                            .toProperties())
                    .get(30, TimeUnit.SECONDS);

            final DataTransportService test = fixture
                    .createFactoryConfiguration(DataTransportService.class, TEST_MQTT_DATA_TRANSPORT_PID,
                            MQTT_DATA_TRANSPORT_FACTORY_PID,
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl(BROKER_ADDR_WSS)
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            try {
                test.connect();
            } catch (final KuraConnectException e) {
                return;
            }

            fail("connection should have failed");
        }
    }

    @Test
    public void shouldConnectOverMqttsWithClientSideAuthWithSeparateTruststore() throws Exception {
        try (final Fixture fixture = new Fixture()) {

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_TRUSTSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID, KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttTrustStore.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_KEYSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID,
                    KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttKeyStoreKeyOnly.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_SSL_MANAGER_SERVICE_PID,
                    SSL_MANAGER_SERVICE_FACTORY_PID,
                    SslManagerServiceOptions.defaultConfiguration().withKeystoreTargetFilter(TEST_KEYSTORE_FILTER)
                            .withTruststoreTargetFilter(TEST_TRUSTSTORE_FILTER).withHostnameVerification(false)
                            .toProperties())
                    .get(30, TimeUnit.SECONDS);

            final DataTransportService test = fixture
                    .createFactoryConfiguration(DataTransportService.class, TEST_MQTT_DATA_TRANSPORT_PID,
                            MQTT_DATA_TRANSPORT_FACTORY_PID,
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl(BROKER_ADDR_MQTTS)
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            test.connect();
        }
    }

    @Test
    public void connectionShouldFailIfSeparateTruststoreIsUnset() throws Exception {
        try (final Fixture fixture = new Fixture()) {

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_TRUSTSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID, KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttTrustStore.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_KEYSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID,
                    KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttKeyStoreKeyOnly.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_SSL_MANAGER_SERVICE_PID,
                    SSL_MANAGER_SERVICE_FACTORY_PID,
                    SslManagerServiceOptions.defaultConfiguration().withKeystoreTargetFilter(TEST_KEYSTORE_FILTER)
                            .withTruststoreTargetFilter(TEST_TRUSTSTORE_FILTER).withHostnameVerification(false)
                            .toProperties())
                    .get(30, TimeUnit.SECONDS);

            final DataTransportService test = fixture
                    .createFactoryConfiguration(DataTransportService.class, TEST_MQTT_DATA_TRANSPORT_PID,
                            MQTT_DATA_TRANSPORT_FACTORY_PID,
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl(BROKER_ADDR_MQTTS)
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            WireTestUtil
                    .updateComponentConfiguration(configurationService, TEST_SSL_MANAGER_SERVICE_PID,
                            SslManagerServiceOptions.defaultConfiguration()
                                    .withTruststoreTargetFilter("(kura.service.pid=changeit)").toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            try {
                test.connect();
            } catch (final KuraConnectException e) {
                return;
            }

            fail("connection should have failed");
        }
    }

    @Test
    public void shouldNotConnectOverMqttsWithClientSideAuthWithWrongTruststore() throws Exception {
        try (final Fixture fixture = new Fixture()) {

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_TRUSTSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID, KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttTrustStore.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_SSL_MANAGER_SERVICE_PID,
                    SSL_MANAGER_SERVICE_FACTORY_PID,
                    SslManagerServiceOptions.defaultConfiguration().withKeystoreTargetFilter(TEST_KEYSTORE_FILTER)
                            .withTruststoreTargetFilter(TEST_KEYSTORE_FILTER).withHostnameVerification(false)
                            .toProperties())
                    .get(30, TimeUnit.SECONDS);

            final DataTransportService test = fixture
                    .createFactoryConfiguration(DataTransportService.class, TEST_MQTT_DATA_TRANSPORT_PID,
                            MQTT_DATA_TRANSPORT_FACTORY_PID,
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl(BROKER_ADDR_MQTTS)
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            try {
                test.connect();
            } catch (final KuraConnectException e) {
                return;
            }

            fail("connection should have failed");
        }
    }

    @Test
    public void connectionShouldFailWithRevocationChechEnabled() throws Exception {
        try (final Fixture fixture = new Fixture()) {

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_KEYSTORE_PID,
                    KEYSTORE_SERVICE_FACTORY_PID, KeystoreServiceOptions.defaultConfiguration()
                            .withKeystorePath(mqttKeyStore.getAbsolutePath()).toProperties())
                    .get(30, TimeUnit.SECONDS);

            fixture.createFactoryConfiguration(ConfigurableComponent.class, TEST_SSL_MANAGER_SERVICE_PID,
                    SSL_MANAGER_SERVICE_FACTORY_PID,
                    SslManagerServiceOptions.defaultConfiguration().withKeystoreTargetFilter(TEST_KEYSTORE_FILTER)
                            .withHostnameVerification(false).withRevocationCheckEnabled(true).toProperties())
                    .get(30, TimeUnit.SECONDS);

            final DataTransportService test = fixture
                    .createFactoryConfiguration(DataTransportService.class, TEST_MQTT_DATA_TRANSPORT_PID,
                            MQTT_DATA_TRANSPORT_FACTORY_PID,
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl(BROKER_ADDR_WSS)
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            test.connect();
        }
    }

    private static class MqttDataTransportOptions {

        private Optional<String> brokerUrl = Optional.empty();
        private Optional<String> username = Optional.empty();
        private Optional<String> password = Optional.empty();
        private Optional<String> sslManagerTargetFilter = Optional.empty();

        private MqttDataTransportOptions() {
        }

        static MqttDataTransportOptions defaultConfiguration() {
            return new MqttDataTransportOptions().withUsername("mqtt").withPassword("bar");
        }

        MqttDataTransportOptions withBrokerUrl(final String arg) {
            this.brokerUrl = Optional.of(arg);
            return this;
        }

        MqttDataTransportOptions withUsername(final String arg) {
            this.username = Optional.of(arg);
            return this;
        }

        MqttDataTransportOptions withPassword(final String arg) {
            this.password = Optional.of(arg);
            return this;
        }

        MqttDataTransportOptions withSslManagerTargetFilter(final String arg) {
            this.sslManagerTargetFilter = Optional.of(arg);
            return this;
        }

        Map<String, Object> toProperties() {
            final Map<String, Object> result = new HashMap<>();

            this.brokerUrl.ifPresent(v -> result.put("broker-url", v));
            this.username.ifPresent(v -> result.put("username", v));
            this.password.ifPresent(v -> result.put("password", v));
            this.sslManagerTargetFilter.ifPresent(v -> result.put("SslManagerService.target", v));

            return result;
        }
    }

    private static class SslManagerServiceOptions {

        public enum RevocationCheckMode {
            PREFER_OCSP,
            PREFER_CRL,
            CRL_ONLY
        }

        private Optional<Boolean> hostnameVerification = Optional.empty();
        private Optional<String> keystoreTargetFilter = Optional.empty();
        private Optional<String> truststoreTargetFilter = Optional.empty();
        private Optional<Boolean> revocationCheckEnabled = Optional.empty();
        private Optional<RevocationCheckMode> revocationCheckMode = Optional.empty();

        private SslManagerServiceOptions() {
        }

        static SslManagerServiceOptions defaultConfiguration() {
            return new SslManagerServiceOptions();
        }

        SslManagerServiceOptions withRevocationCheckEnabled(final boolean revocationCheckEnabled) {
            this.revocationCheckEnabled = Optional.of(revocationCheckEnabled);
            return this;
        }

        SslManagerServiceOptions withRevocationCheckMode(final RevocationCheckMode revocationCheckMode) {
            this.revocationCheckMode = Optional.of(revocationCheckMode);
            return this;
        }

        SslManagerServiceOptions withHostnameVerification(final boolean hostnameVerification) {
            this.hostnameVerification = Optional.of(hostnameVerification);
            return this;
        }

        SslManagerServiceOptions withKeystoreTargetFilter(final String keystoreTargetFilter) {
            this.keystoreTargetFilter = Optional.of(keystoreTargetFilter);
            return this;
        }

        SslManagerServiceOptions withTruststoreTargetFilter(final String truststoreTargetFilter) {
            this.truststoreTargetFilter = Optional.of(truststoreTargetFilter);
            return this;
        }

        Map<String, Object> toProperties() {
            final Map<String, Object> result = new HashMap<>();

            this.hostnameVerification.ifPresent(v -> result.put("ssl.hostname.verification", v));
            this.keystoreTargetFilter.ifPresent(v -> result.put("KeystoreService.target", v));
            this.truststoreTargetFilter.ifPresent(v -> result.put("TruststoreKeystoreService.target", v));
            this.revocationCheckEnabled.ifPresent(v -> result.put("ssl.revocation.check.enabled", v));
            this.revocationCheckMode.ifPresent(v -> result.put("ssl.revocation.mode", v.name()));

            return result;
        }
    }

    private static class KeystoreServiceOptions {

        private Optional<String> keystorePath = Optional.empty();
        private Optional<String> keystorePassword = Optional.empty();
        private boolean crlManagerEnabled = false;
        private Optional<String[]> crlUrls = Optional.empty();

        private KeystoreServiceOptions() {
        }

        static KeystoreServiceOptions defaultConfiguration() {
            return new KeystoreServiceOptions();
        }

        KeystoreServiceOptions withCrlManagerEnabled(boolean crlManagerEnabled) {
            this.crlManagerEnabled = crlManagerEnabled;
            return this;
        }

        KeystoreServiceOptions withCrlUrls(String[] crlUrls) {
            this.crlUrls = Optional.of(crlUrls);
            return this;
        }

        KeystoreServiceOptions withKeystorePath(final String keystorePath) {
            this.keystorePath = Optional.of(keystorePath);
            return this;
        }

        Map<String, Object> toProperties() {
            final Map<String, Object> result = new HashMap<>();

            this.keystorePath.ifPresent(v -> result.put("keystore.path", v));
            this.keystorePassword.ifPresent(v -> result.put("keystore.password", v));
            result.put("crl.check.interval", 1L);
            result.put("crl.check.interval.time.unit", TimeUnit.SECONDS.name());
            result.put("crl.update.interval", 1L);
            result.put("crl.update.interval.time.unit", TimeUnit.SECONDS.name());
            result.put("crl.management.enabled", crlManagerEnabled);
            this.crlUrls.ifPresent(u -> result.put("crl.urls", u));

            return result;
        }
    }

    private static class Fixture implements AutoCloseable {

        private final Set<String> createdPids = new HashSet<>();

        public <T> CompletableFuture<T> createFactoryConfiguration(final Class<T> classz, final String pid,
                final String factoryPid, final Map<String, Object> properties) {
            return WireTestUtil.createFactoryConfiguration(configurationService, classz, pid, factoryPid, properties)
                    .whenComplete((ok, ex) -> {
                        if (ex == null) {
                            createdPids.add(pid);
                        }
                    });
        }

        @Override
        public void close() throws Exception {

            for (final String pid : createdPids) {
                WireTestUtil.deleteFactoryConfiguration(configurationService, pid).get(30, TimeUnit.SECONDS);
            }

        }
    }

}
