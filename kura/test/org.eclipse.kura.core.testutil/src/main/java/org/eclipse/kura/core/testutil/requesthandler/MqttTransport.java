/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.testutil.requesthandler;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.core.util.MqttTopicUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.message.KuraPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttTransport implements Transport {

    private static final Logger logger = LoggerFactory.getLogger(MqttTransport.class);

    private static final String MQTT_DATA_TRANSPORT_FACTORY_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";
    private static final String SIMPLE_ARTEMIS_BROKER_SERVICE_PID = "org.eclipse.kura.broker.artemis.simple.mqtt.BrokerInstance";

    private static final String DEFAULT_CLOUD_SERVICE_PID = "org.eclipse.kura.cloud.CloudService";
    private static final String DEFAULT_MQTT_DATA_TRANSPORT_SERVICE_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";

    private CryptoService cryptoService;

    private DataTransportInspector observerInspector;

    private Marshaller jsonMarshaller;
    private Unmarshaller jsonUnmarshaller;

    private String appId;

    private AtomicBoolean initialized = new AtomicBoolean(false);

    public MqttTransport(final String appId) {
        this.appId = appId;
    }

    @Override
    public void init() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        try {
            final ConfigurationService configurationService = ServiceUtil
                    .trackService(ConfigurationService.class, Optional.empty()).get(1, TimeUnit.MINUTES);
            final DataTransportService mqttDataTransport = ServiceUtil
                    .trackService(DataTransportService.class, Optional.empty()).get(1, TimeUnit.MINUTES);
            cryptoService = ServiceUtil.trackService(CryptoService.class, Optional.empty()).get(1, TimeUnit.MINUTES);
            jsonMarshaller = ServiceUtil
                    .trackService(Marshaller.class,
                            Optional.of("(kura.service.pid=org.eclipse.kura.json.marshaller.unmarshaller.provider)"))
                    .get(1, TimeUnit.MINUTES);
            jsonUnmarshaller = ServiceUtil
                    .trackService(Unmarshaller.class,
                            Optional.of("(kura.service.pid=org.eclipse.kura.json.marshaller.unmarshaller.provider)"))
                    .get(1, TimeUnit.MINUTES);

            final Map<String, Object> brokerProperties = new HashMap<>();

            brokerProperties.put("enabled", true);
            brokerProperties.put("password", new String(cryptoService.encryptAes("foo".toCharArray())));

            ServiceUtil.updateComponentConfiguration(configurationService, DEFAULT_MQTT_DATA_TRANSPORT_SERVICE_PID,
                    getConfigForLocalBroker("test")).get(30, TimeUnit.SECONDS);
            ServiceUtil.updateComponentConfiguration(configurationService, SIMPLE_ARTEMIS_BROKER_SERVICE_PID,
                    brokerProperties).get(30, TimeUnit.SECONDS);

            final Map<String, Object> cloudServiceProperties = Collections.singletonMap("payload.encoding",
                    "simple-json");

            ServiceUtil.updateComponentConfiguration(configurationService, DEFAULT_CLOUD_SERVICE_PID,
                    cloudServiceProperties).get(30, TimeUnit.SECONDS);

            final DataTransportService observer = ServiceUtil
                    .createFactoryConfiguration(configurationService, DataTransportService.class, "observer",
                            MQTT_DATA_TRANSPORT_FACTORY_PID, getConfigForLocalBroker("observer"))
                    .get(30, TimeUnit.SECONDS);
            observerInspector = new DataTransportInspector(observer, true);
            final DataTransportInspector underTestInspector = new DataTransportInspector(mqttDataTransport, false);

            final CompletableFuture<Void> underTestConnected = underTestInspector.connected();

            mqttDataTransport.connect();

            underTestConnected.get(1, TimeUnit.MINUTES);

            final CompletableFuture<Void> observerConnected = observerInspector.connected();

            observer.connect();

            observerConnected.get(1, TimeUnit.MINUTES);

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response runRequest(String resource, MethodSpec method) {
        return runRequest(resource, method, null);
    }

    @Override
    public Response runRequest(String resource, MethodSpec method, String requestBody) {
        try {
            final KuraPayload requestPayload = new KuraPayload();

            if (requestBody != null) {
                requestPayload.setBody(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            final KuraPayload response = observerInspector.runRequest(adaptResource(resource, method), requestPayload)
                    .get(1, TimeUnit.MINUTES);

            final int status = (int) (long) response.getMetric("response.code");
            final Optional<String> body = Optional.ofNullable(response.getBody())
                    .map(s -> new String(s, StandardCharsets.UTF_8));

            return new Response(status, body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String adaptResource(final String resource, final MethodSpec method) {
        return method.getRequestHandlerMethod() + resource;
    }

    private Map<String, Object> getConfigForLocalBroker(final String clientId) throws KuraException {
        final Map<String, Object> properties = new HashMap<>();

        properties.put("broker-url", "mqtt://localhost:1883/");
        properties.put("username", "mqtt");
        properties.put("client-id", clientId);
        properties.put("topic.context.account-name", "mqtt");
        properties.put("password", new String(cryptoService.encryptAes("foo".toCharArray())));

        return properties;
    }

    private class DataTransportInspector {

        private final DataTransportService dataTransportService;
        private final Random random = new Random();

        private Optional<CompletableFuture<Void>> connectFuture = Optional.empty();
        private Optional<CompletableFuture<Void>> disconnectFuture = Optional.empty();
        private Optional<MessageLookup> messageLookup = Optional.empty();

        DataTransportInspector(final DataTransportService dataTransportService, final boolean subscribe) {
            this.dataTransportService = dataTransportService;

            dataTransportService.addDataTransportListener(new DataTransportListener() {

                @Override
                public void onConnectionEstablished(boolean newSession) {
                    if (subscribe) {
                        try {
                            dataTransportService.subscribe("#", 0);
                        } catch (Exception e) {
                            logger.warn("failed to subscribe", e);
                        }
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

        private class MessageLookup {

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

        CompletableFuture<KuraPayload> runRequest(final String resource, final KuraPayload request)
                throws KuraException {

            final String requestId = Integer.toString(random.nextInt());

            request.addMetric("requester.client.id", "test");
            request.addMetric("request.id", requestId);

            final byte[] data = jsonMarshaller.marshal(request).getBytes(StandardCharsets.UTF_8);

            final String topic = "$EDC/mqtt/test/" + appId + "/" + resource;

            final CompletableFuture<byte[]> message = new CompletableFuture<>();

            this.messageLookup = Optional
                    .of(new MessageLookup(message, "$EDC/mqtt/test/" + appId + "/REPLY/" + requestId));

            dataTransportService.publish(topic, data, 0, false);

            return message.thenApply(d -> {
                try {
                    return jsonUnmarshaller.unmarshal(new String(d), KuraPayload.class);
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }).whenComplete((ok, ex) -> this.messageLookup = Optional.empty());

        }
    }

}
