package org.eclipse.kura.core.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;

/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * Eurotech
 *******************************************************************************/
public class MqttDataTransportTest {

    private static final String ARTEMIS_XML_PID = "org.eclipse.kura.broker.artemis.xml.BrokerInstance";

    private static final String TEST_MQTT_DATA_TRANSPORT_PID = "test";
    private static final String SSL_MANAGER_SERVICE_FACTORY_PID = "org.eclipse.kura.ssl.SslManagerService";
    private static final String TEST_SSL_MANAGER_SERVICE_FILTER = "(kura.service.pid=testSsl)";
    private static final String TEST_KEYSTORE_FILTER = "(kura.service.pid=testKeystore)";
    private static final String TEST_SSL_MANAGER_SERVICE_PID = "testSsl";
    private static final String TEST_KEYSTORE_PID = "testKeystore";
    private static final String KEYSTORE_SERVICE_FACTORY_PID = "org.eclipse.kura.core.keystore.FilesystemKeystoreServiceImpl";
    private static final String MQTT_DATA_TRANSPORT_FACTORY_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";
    private static File brokerKeyStore;
    private static File brokerTrustStore;
    private static File mqttKeyStore;
    private static File mqttTrustStore;

    private static ConfigurationService configurationService;

    @BeforeClass
    public static void setUp() throws IOException, InterruptedException, ExecutionException, TimeoutException,
            KuraException, InvalidSyntaxException {
        brokerKeyStore = deployResource("/keystores/brokerkeystore.ks");
        brokerTrustStore = deployResource("/keystores/brokertruststore.ks");
        mqttKeyStore = deployResource("/keystores/mqttkeystore.ks");
        mqttTrustStore = deployResource("/keystores/mqtttruststore.ks");

        String brokerConfigXml = loadResource("/artemis_config.xml");
        brokerConfigXml = brokerConfigXml.replaceAll("[$]keyStorePath", brokerKeyStore.getAbsolutePath());
        brokerConfigXml = brokerConfigXml.replaceAll("[$]trustStorePath", brokerTrustStore.getAbsolutePath());

        final Map<String, Object> brokerConfig = new HashMap<>();

        brokerConfig.put("enabled", true);
        brokerConfig.put("brokerXml", brokerConfigXml);
        brokerConfig.put("users", "mqtt=bar|amq");
        brokerConfig.put("defaultUser", "mqtt");

        configurationService = WireTestUtil.trackService(ConfigurationService.class, Optional.empty()).get(30,
                TimeUnit.SECONDS);

        WireTestUtil.updateComponentConfiguration(configurationService, ARTEMIS_XML_PID, brokerConfig).get(30,
                TimeUnit.SECONDS);
    }

    @AfterClass
    public static void tearDown()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        final Map<String, Object> brokerConfig = new HashMap<>();

        brokerConfig.put("enabled", false);

        WireTestUtil.updateComponentConfiguration(configurationService, ARTEMIS_XML_PID, brokerConfig).get(30,
                TimeUnit.SECONDS);
    }

    @Test
    public void shouldConnectOverPlainMqtt() throws Exception {
        try (final Fixture fixture = new Fixture()) {
            final DataTransportService test = fixture.createFactoryConfiguration(DataTransportService.class,
                    TEST_MQTT_DATA_TRANSPORT_PID, MQTT_DATA_TRANSPORT_FACTORY_PID, MqttDataTransportOptions
                            .defaultConfiguration().withBrokerUrl("mqtt://localhost:1889").toProperties())
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
                            .defaultConfiguration().withBrokerUrl("mqtts://localhost:8888").toProperties())
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
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl("mqtts://localhost:8888")
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
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl("wss://localhost:8888")
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
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl("mqtts://localhost:8888")
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            test.connect();
        }
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
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl("wss://localhost:8888")
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            test.connect();
        }
    }

    @Test
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
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl("mqtts://localhost:8889")
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
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl("wss://localhost:8889")
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
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl("mqtts://localhost:8889")
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
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl("wss://localhost:8889")
                                    .withSslManagerTargetFilter(TEST_SSL_MANAGER_SERVICE_FILTER).toProperties())
                    .get(30, TimeUnit.SECONDS);

            assertNotNull(test);
            test.connect();
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
                            MqttDataTransportOptions.defaultConfiguration().withBrokerUrl("wss://localhost:8889")
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

        private Optional<Boolean> hostnameVerification = Optional.empty();
        private Optional<String> keystoreTargetFilter = Optional.empty();
        private Optional<Boolean> revocationCheckEnabled = Optional.empty();

        private SslManagerServiceOptions() {
        }

        static SslManagerServiceOptions defaultConfiguration() {
            return new SslManagerServiceOptions();
        }

        SslManagerServiceOptions withRevocationCheckEnabled(final boolean revocationCheckEnabled) {
            this.revocationCheckEnabled = Optional.of(revocationCheckEnabled);
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

        Map<String, Object> toProperties() {
            final Map<String, Object> result = new HashMap<>();

            this.hostnameVerification.ifPresent(v -> result.put("ssl.hostname.verification", v));
            this.keystoreTargetFilter.ifPresent(v -> result.put("KeystoreService.target", v));
            this.revocationCheckEnabled.ifPresent(v -> result.put("ssl.revocation.check.enabled", v));

            return result;
        }
    }

    private static class KeystoreServiceOptions {

        private Optional<String> keystorePath = Optional.empty();
        private Optional<String> keystorePassword = Optional.empty();

        private KeystoreServiceOptions() {
        }

        static KeystoreServiceOptions defaultConfiguration() {
            return new KeystoreServiceOptions();
        }

        KeystoreServiceOptions withKeystorePath(final String keystorePath) {
            this.keystorePath = Optional.of(keystorePath);
            return this;
        }

        KeystoreServiceOptions withKeystorePassword(final String keystorePassword, final CryptoService cryptoService)
                throws KuraException {
            this.keystorePassword = Optional.of(new String(cryptoService.encryptAes(keystorePassword.toCharArray())));
            return this;
        }

        Map<String, Object> toProperties() {
            final Map<String, Object> result = new HashMap<>();

            this.keystorePath.ifPresent(v -> result.put("keystore.path", v));
            this.keystorePassword.ifPresent(v -> result.put("keystore.password", v));

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

    private static File deployResource(final String resourcePath) throws IOException {

        final File target = Files.createTempFile(null, null).toFile();

        final byte[] buf = new byte[4096];

        try (final FileOutputStream out = new FileOutputStream(target);
                final InputStream in = MqttDataTransportTest.class.getResourceAsStream(resourcePath)) {
            int rd;
            while ((rd = in.read(buf)) > 0) {
                out.write(buf, 0, rd);
            }
        }

        return target;
    }

    private static String loadResource(final String resourcePath) throws IOException {

        final byte[] buf = new byte[4096];

        try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final InputStream in = MqttDataTransportTest.class.getResourceAsStream(resourcePath)) {
            int rd;
            while ((rd = in.read(buf)) > 0) {
                out.write(buf, 0, rd);
            }
            return out.toString();
        }

    }

}
