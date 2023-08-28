/*******************************************************************************
 * Copyright (c) 2018, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.cloud;

import static org.eclipse.kura.util.wire.test.WireTestUtil.createFactoryConfiguration;
import static org.eclipse.kura.util.wire.test.WireTestUtil.updateComponentConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.util.MqttTopicUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.eclipse.kura.message.KuraBirthPayload;
import org.eclipse.kura.message.KuraDeviceProfile;
import org.eclipse.kura.net.modem.ModemReadyEvent;
import org.eclipse.kura.security.tamper.detection.TamperDetectionService;
import org.eclipse.kura.security.tamper.detection.TamperEvent;
import org.eclipse.kura.security.tamper.detection.TamperStatus;
import org.eclipse.kura.system.ExtendedProperties;
import org.eclipse.kura.system.ExtendedPropertyGroup;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class CloudServiceTest {

    private static final String MQTT_DATA_TRANSPORT_FACTORY_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";
    private static final String SIMPLE_ARTEMIS_BROKER_SERVICE_PID = "org.eclipse.kura.broker.artemis.simple.mqtt.BrokerInstance";

    private static final String DEFAULT_CLOUD_SERVICE_PID = "org.eclipse.kura.cloud.CloudService";
    private static final String DEFAULT_MQTT_DATA_TRANSPORT_SERVICE_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";

    private static final Logger logger = LoggerFactory.getLogger(CloudServiceTest.class);

    private static CountDownLatch dependencyLatch = new CountDownLatch(6);

    private static ConfigurationService cfgSvc;

    private static CloudConnectionFactory cloudConnectionFactory;

    private static DataTransportService mqttDataTransport;
    private static DataTransportInspector underTestInspector;

    private static CryptoService cryptoService;

    private static CloudServiceImpl cloudServiceImpl;

    private static DataTransportService observer;
    private static DataTransportInspector observerInspector;

    private static EventAdmin eventAdmin;

    @BeforeClass
    public static void setup()
            throws KuraException, InterruptedException, ExecutionException, TimeoutException, InvalidSyntaxException {
        try {
            dependencyLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> updatedProp = new HashMap<>();
        updatedProp.put("client-id", "test");

        final Map<String, Object> brokerProperties = new HashMap<>();

        brokerProperties.put("enabled", true);
        brokerProperties.put("password", new String(cryptoService.encryptAes("foo".toCharArray())));

        updateComponentConfiguration(cfgSvc, DEFAULT_MQTT_DATA_TRANSPORT_SERVICE_PID, updatedProp).get(30,
                TimeUnit.SECONDS);
        updateComponentConfiguration(cfgSvc, SIMPLE_ARTEMIS_BROKER_SERVICE_PID, brokerProperties).get(30,
                TimeUnit.SECONDS);

        final Map<String, Object> cloudServiceProperties = Collections.singletonMap("payload.encoding", "simple-json");

        updateComponentConfiguration(cfgSvc, DEFAULT_CLOUD_SERVICE_PID, cloudServiceProperties).get(30,
                TimeUnit.SECONDS);

        observer = createFactoryConfiguration(cfgSvc, DataTransportService.class, "observer",
                MQTT_DATA_TRANSPORT_FACTORY_PID, getConfigForLocalBroker("observer")).get(30, TimeUnit.SECONDS);
        observerInspector = new DataTransportInspector(observer);

        final CompletableFuture<Void> connected = observerInspector.connected();

        observer.connect();

        connected.get(1, TimeUnit.MINUTES);

        underTestInspector = new DataTransportInspector(mqttDataTransport);

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

    public void bindDataTransportService(DataTransportService service) {
        mqttDataTransport = service;
        dependencyLatch.countDown();
    }

    public void unbindCloudFactory(CloudConnectionFactory cloudConnectionFactory) {
        CloudServiceTest.cloudConnectionFactory = null;
    }

    public void bindCryptoService(CryptoService service) {
        cryptoService = service;
        dependencyLatch.countDown();
    }

    public void bindEventAdmin(EventAdmin service) {
        eventAdmin = service;
        dependencyLatch.countDown();
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
    public void testConnectCannotConnect()
            throws KuraException, InterruptedException, ExecutionException, TimeoutException, InvalidSyntaxException {

        final CompletableFuture<Void> diconnected = underTestInspector.disconnected();

        cloudServiceImpl.disconnect();

        diconnected.get(30, TimeUnit.SECONDS);

        updateComponentConfiguration(cfgSvc, DEFAULT_MQTT_DATA_TRANSPORT_SERVICE_PID, getConfigForNonExistingBroker())
                .get(1, TimeUnit.MINUTES);

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

    @Test
    public void shouldSupportAdditionalBirthProperties()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {

        final Map<String, Object> properties = new HashMap<>();

        properties.put(ModemReadyEvent.IMEI, "fooImei");
        properties.put(ModemReadyEvent.IMSI, "fooImsi");
        properties.put(ModemReadyEvent.ICCID, "fooIccid");
        properties.put(ModemReadyEvent.RSSI, "0");
        properties.put(ModemReadyEvent.FW_VERSION, "fooFwVer");

        final ModemReadyEvent modemReadyEvent = new ModemReadyEvent(properties);

        eventAdmin.sendEvent(modemReadyEvent);
        cloudServiceImpl.setSystemService(createMockSystemService(Optional.empty()));
        final JsonObject metrics = publishBirthAndGetMetrics();

        assertEquals("getCpuVersion", metrics.get(KuraDeviceProfile.CPU_VERSION_KEY).asString());
        assertEquals("fooFwVer", metrics.get("modem_firmware_version").asString());

    }

    @Test
    public void shouldSupportEmptyExtendedProperties()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {

        cloudServiceImpl.setSystemService(createMockSystemService(Optional.empty()));
        final JsonObject metrics = publishBirthAndGetMetrics();

        assertNull(metrics.get("extended_properties"));
    }

    @Test
    public void shouldSupportExtendedPropertiesSerialization()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {

        final Map<String, String> first = new HashMap<>();

        first.put("string", "str");
        first.put("foo", "bar");

        final Map<String, String> empty = Collections.emptyMap();

        final ExtendedPropertyGroup firstGroup = new ExtendedPropertyGroup("first", first);
        final ExtendedPropertyGroup emptyGroup = new ExtendedPropertyGroup("empty", empty);

        final ExtendedProperties properties = new ExtendedProperties("1.5", Arrays.asList(firstGroup, emptyGroup));

        cloudServiceImpl.setSystemService(createMockSystemService(Optional.of(properties)));
        final JsonObject metrics = publishBirthAndGetMetrics();

        final JsonObject parsedExtendedPropertes = Json.parse(metrics.get("extended_properties").asString()).asObject();

        assertEquals("1.5", parsedExtendedPropertes.get("version").asString());

        final JsonObject groups = parsedExtendedPropertes.get("properties").asObject();

        final JsonObject firstObject = groups.get("first").asObject();

        assertEquals(2, firstObject.size());
        assertEquals("str", firstObject.get("string").asString());
        assertEquals("bar", firstObject.get("foo").asString());

        final JsonObject emptyObject = groups.get("empty").asObject();

        assertTrue(emptyObject.isEmpty());

    }

    @Test
    public void shouldNotPublishTamperStatusIfTamperDetectionIsNotAvailable()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {

        final JsonObject metrics = publishBirthAndGetMetrics();

        assertNull(metrics.get("tamper_status"));
    }

    @Test
    public void shouldPublishTamperStatusIfTamperDetectionIsAvailable()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {

        final TamperDetectionService tamperDetectionService = Mockito.mock(TamperDetectionService.class);
        Mockito.when(tamperDetectionService.getTamperStatus())
                .thenReturn(new TamperStatus(true, Collections.emptyMap()));

        final ServiceRegistration<?> reg = FrameworkUtil.getBundle(CloudServiceTest.class).getBundleContext()
                .registerService(TamperDetectionService.class, tamperDetectionService, null);

        try {
            JsonObject metrics = publishBirthAndGetMetrics();

            assertEquals(KuraBirthPayload.TamperStatus.TAMPERED.name(), metrics.get("tamper_status").asString());

            Mockito.when(tamperDetectionService.getTamperStatus())
                    .thenReturn(new TamperStatus(false, Collections.emptyMap()));

            metrics = publishBirthAndGetMetrics();

            assertEquals(KuraBirthPayload.TamperStatus.NOT_TAMPERED.name(), metrics.get("tamper_status").asString());
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void shouldRepublishBirthOnTamperEvent()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {

        final TamperDetectionService tamperDetectionService = Mockito.mock(TamperDetectionService.class);
        Mockito.when(tamperDetectionService.getTamperStatus())
                .thenReturn(new TamperStatus(true, Collections.emptyMap()));

        final ServiceRegistration<?> reg = FrameworkUtil.getBundle(CloudServiceTest.class).getBundleContext()
                .registerService(TamperDetectionService.class, tamperDetectionService, null);

        try {
            JsonObject metrics = publishBirthAndGetMetrics();

            assertEquals(KuraBirthPayload.TamperStatus.TAMPERED.name(), metrics.get("tamper_status").asString());

            final TamperStatus tamperStatus = new TamperStatus(false, Collections.emptyMap());

            Mockito.when(tamperDetectionService.getTamperStatus()).thenReturn(tamperStatus);

            final CompletableFuture<byte[]> message = observerInspector.nextMessage("$EDC/mqtt/underTest/MQTT/BIRTH");
            eventAdmin.postEvent(new TamperEvent("foo", tamperStatus));

            metrics = getMetrics(message.get(35, TimeUnit.SECONDS));

            assertEquals(KuraBirthPayload.TamperStatus.NOT_TAMPERED.name(), metrics.get("tamper_status").asString());
        } finally {
            reg.unregister();
        }
    }

    private JsonObject publishBirthAndGetMetrics() throws InterruptedException, ExecutionException, TimeoutException,
            KuraException, InvalidSyntaxException, KuraConnectException {
        final CompletableFuture<Void> disconnected = underTestInspector.disconnected();

        cloudServiceImpl.disconnect();

        disconnected.get(30, TimeUnit.SECONDS);

        updateComponentConfiguration(cfgSvc, DEFAULT_MQTT_DATA_TRANSPORT_SERVICE_PID,
                getConfigForLocalBroker("underTest")).get(1, TimeUnit.MINUTES);

        final CompletableFuture<byte[]> message = observerInspector.nextMessage("$EDC/mqtt/underTest/MQTT/BIRTH");

        for (int i = 0; i < 3; i++) {
            try {
                final CompletableFuture<Void> connected = underTestInspector.connected();

                cloudServiceImpl.connect();

                connected.get(30, TimeUnit.SECONDS);
                break;
            } catch (final Exception e) {
                logger.warn("connection failed", e);
            }
        }

        assertEquals(true, cloudServiceImpl.isConnected());

        return getMetrics(message.get(30, TimeUnit.SECONDS));
    }

    private static JsonObject getMetrics(final byte[] message) {
        final JsonObject messageObject = Json.parse(new String(message, StandardCharsets.UTF_8)).asObject();
        return messageObject.get("metrics").asObject();
    }

    private static Map<String, Object> getConfigForLocalBroker(final String clientId) throws KuraException {
        final Map<String, Object> properties = new HashMap<>();

        properties.put("broker-url", "mqtt://localhost:1883/");
        properties.put("username", "mqtt");
        properties.put("client-id", clientId);
        properties.put("topic.context.account-name", "mqtt");
        properties.put("password", new String(cryptoService.encryptAes("foo".toCharArray())));

        return properties;
    }

    private Map<String, Object> getConfigForNonExistingBroker() {
        final Map<String, Object> properties = new HashMap<>();

        properties.put("broker-url", "mqtt://broker-url:1883/");

        return properties;
    }

    private static SystemService createMockSystemService(final Optional<ExtendedProperties> extendedProperties) {
        return (SystemService) Proxy.newProxyInstance(CloudServiceTest.class.getClassLoader(),
                new Class<?>[] { SystemService.class }, (obj, method, args) -> {
                    if ("getExtendedProperties".equals(method.getName())) {
                        return extendedProperties;
                    } else if (method.getReturnType() == String.class) {
                        return method.getName();
                    } else if (method.getReturnType() == int.class) {
                        return 0;
                    } else if (method.getReturnType() == long.class) {
                        return 0L;
                    } else {
                        return null;
                    }
                });
    }

    private static class DataTransportInspector {

        private final DataTransportService dataTransportService;

        private Optional<CompletableFuture<Void>> connectFuture = Optional.empty();
        private Optional<CompletableFuture<Void>> disconnectFuture = Optional.empty();
        private Optional<MessageLookup> messageLookup = Optional.empty();

        DataTransportInspector(final DataTransportService dataTransportService) {
            this.dataTransportService = dataTransportService;

            dataTransportService.addDataTransportListener(new DataTransportListener() {

                @Override
                public void onConnectionEstablished(boolean newSession) {
                    try {
                        dataTransportService.subscribe("#", 0);
                    } catch (Exception e) {
                        logger.warn("failed to subscribe", e);
                    }
                    if (connectFuture.isPresent()) {
                        connectFuture.get().complete(null);
                    }
                    connectFuture = Optional.empty();
                }

                @Override
                public void onDisconnecting() {
                    // do nothing
                }

                @Override
                public void onDisconnected() {
                    if (disconnectFuture.isPresent()) {
                        disconnectFuture.get().complete(null);
                    }
                    disconnectFuture = Optional.empty();
                }

                @Override
                public void onConfigurationUpdating(boolean wasConnected) {
                    // do nothing

                }

                @Override
                public void onConfigurationUpdated(boolean wasConnected) {
                    // do nothing
                }

                @Override
                public void onConnectionLost(Throwable cause) {
                    if (disconnectFuture.isPresent()) {
                        disconnectFuture.get().complete(null);
                    }
                    disconnectFuture = Optional.empty();
                }

                @Override
                public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
                    if (!messageLookup.isPresent()) {
                        return;
                    }

                    final MessageLookup lookup = messageLookup.get();

                    if (MqttTopicUtil.isMatched(lookup.topicFilter, topic)) {
                        lookup.future.complete(payload);
                        messageLookup = Optional.empty();
                    }
                }

                @Override
                public void onMessageConfirmed(DataTransportToken token) {
                    // do nothing
                }

            });
        }

        private static class MessageLookup {

            private final CompletableFuture<byte[]> future;
            private final String topicFilter;

            public MessageLookup(CompletableFuture<byte[]> future, String topicFilter) {
                this.future = future;
                this.topicFilter = topicFilter;
            }
        }

        CompletableFuture<Void> connected() {
            if (dataTransportService.isConnected()) {
                return CompletableFuture.completedFuture(null);
            }

            final CompletableFuture<Void> result = new CompletableFuture<>();

            this.connectFuture = Optional.of(result);

            return result;
        }

        CompletableFuture<Void> disconnected() {
            if (!dataTransportService.isConnected()) {
                return CompletableFuture.completedFuture(null);
            }

            final CompletableFuture<Void> result = new CompletableFuture<>();

            this.disconnectFuture = Optional.of(result);

            return result;
        }

        CompletableFuture<byte[]> nextMessage(final String topicFilter) {
            final CompletableFuture<byte[]> result = new CompletableFuture<>();

            this.messageLookup = Optional.of(new MessageLookup(result, topicFilter));

            return result;
        }
    }

}