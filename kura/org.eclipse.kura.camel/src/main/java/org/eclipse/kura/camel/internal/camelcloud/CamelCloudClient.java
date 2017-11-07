/*******************************************************************************
 * Copyright (c) 2011, 2017 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.internal.camelcloud;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.camel.ServiceStatus.Started;
import static org.eclipse.kura.KuraErrorCode.CONFIGURATION_ERROR;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_CONTROL;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_DEVICEID;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_MESSAGEID;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_PRIORITY;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_QOS;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_RETAIN;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.function.IntSupplier;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.StartupListener;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultShutdownStrategy;
import org.apache.camel.spi.ShutdownStrategy;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.camel.camelcloud.CamelCloudService;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.message.KuraPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Kura {@link CloudClient} based in Apache Camel
 */
public class CamelCloudClient implements CloudClient {

    private static final Logger logger = LoggerFactory.getLogger(CamelCloudClient.class);

    private final CamelCloudService cloudService;

    private final CamelContext camelContext;

    private final ProducerTemplate producerTemplate;

    private final List<CloudClientListener> cloudClientListeners = new CopyOnWriteArrayList<>();

    private final String applicationId;

    private final String baseEndpoint;

    private final ExecutorService executorService;

    private final Random messageIdRandom = new Random();

    private final IntSupplier messageIdGenerator = () -> Math.abs(this.messageIdRandom.nextInt());

    public CamelCloudClient(CamelCloudService cloudService, CamelContext camelContext, String applicationId,
            String baseEndpoint) {
        this.cloudService = cloudService;
        this.camelContext = camelContext;
        this.producerTemplate = camelContext.createProducerTemplate();
        this.applicationId = applicationId;
        this.baseEndpoint = baseEndpoint;
        this.executorService = camelContext.getExecutorServiceManager().newThreadPool(this,
                "CamelCloudClient/" + applicationId, 0, 1);
    }

    public CamelCloudClient(CamelCloudService cloudService, CamelContext camelContext, String applicationId) {
        this(cloudService, camelContext, applicationId, "vm:%s");
    }

    // Cloud client API

    @Override
    public String getApplicationId() {
        return this.applicationId;
    }

    @Override
    public void release() {
        this.cloudService.release(this.applicationId);
        this.camelContext.getExecutorServiceManager().shutdown(this.executorService);
    }

    @Override
    public boolean isConnected() {
        return this.camelContext.getStatus() == Started;
    }

    @Override
    public int publish(String topic, KuraPayload kuraPayload, int qos, boolean retain) throws KuraException {
        return publish(topic, kuraPayload, qos, retain, 5);
    }

    @Override
    public int publish(String deviceId, String appTopic, KuraPayload payload, int qos, boolean retain)
            throws KuraException {
        return doPublish(false, deviceId, appTopic, payload, qos, retain, 5);
    }

    @Override
    public int publish(String topic, KuraPayload kuraPayload, int qos, boolean retain, int priority)
            throws KuraException {
        return doPublish(false, null, topic, kuraPayload, qos, retain, priority);
    }

    @Override
    public int publish(String deviceId, String appTopic, KuraPayload payload, int qos, boolean retain, int priority)
            throws KuraException {
        return doPublish(false, deviceId, appTopic, payload, qos, retain, priority);
    }

    @Override
    public int publish(String topic, byte[] payload, int qos, boolean retain, int priority) throws KuraException {
        KuraPayload kuraPayload = new KuraPayload();
        kuraPayload.setBody(payload);
        return publish(topic, kuraPayload, qos, retain, priority);
    }

    @Override
    public int publish(String deviceId, String appTopic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraException {
        KuraPayload kuraPayload = new KuraPayload();
        kuraPayload.setBody(payload);
        return publish(deviceId, appTopic, kuraPayload, qos, retain, priority);
    }

    @Override
    public int controlPublish(String topic, KuraPayload payload, int qos, boolean retain, int priority)
            throws KuraException {
        return doPublish(true, null, topic, payload, qos, retain, priority);
    }

    @Override
    public int controlPublish(String deviceId, String topic, KuraPayload kuraPayload, int qos, boolean retain,
            int priority) throws KuraException {
        return doPublish(true, deviceId, topic, kuraPayload, qos, retain, priority);
    }

    @Override
    public int controlPublish(String deviceId, String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraException {
        KuraPayload kuraPayload = new KuraPayload();
        kuraPayload.setBody(payload);
        return doPublish(true, deviceId, topic, kuraPayload, qos, retain, priority);
    }

    @Override
    public void subscribe(String topic, int qos) throws KuraException {
        forkSubscribe(false, null, topic, qos);
    }

    @Override
    public void subscribe(String deviceId, String appTopic, int qos) throws KuraException {
        forkSubscribe(false, deviceId, appTopic, qos);
    }

    @Override
    public void controlSubscribe(String topic, int qos) throws KuraException {
        forkSubscribe(true, null, topic, qos);
    }

    @Override
    public void controlSubscribe(String deviceId, String appTopic, int qos) throws KuraException {
        forkSubscribe(true, deviceId, appTopic, qos);
    }

    @Override
    public void unsubscribe(String topic) throws KuraException {
        doUnsubscribe(null, topic);
    }

    @Override
    public void unsubscribe(String deviceId, String appTopic) throws KuraException {
        doUnsubscribe(deviceId, appTopic);
    }

    @Override
    public void controlUnsubscribe(String topic) throws KuraException {
        doUnsubscribe(null, topic);
    }

    @Override
    public void controlUnsubscribe(String deviceId, String appTopic) throws KuraException {
        doUnsubscribe(deviceId, appTopic);
    }

    @Override
    public void addCloudClientListener(CloudClientListener cloudClientListener) {
        this.cloudClientListeners.add(cloudClientListener);
    }

    @Override
    public void removeCloudClientListener(CloudClientListener cloudClientListener) {
        this.cloudClientListeners.remove(cloudClientListener);
    }

    @Override
    public List<Integer> getUnpublishedMessageIds() throws KuraException {
        return Collections.emptyList();
    }

    @Override
    public List<Integer> getInFlightMessageIds() throws KuraException {
        return Collections.emptyList();
    }

    @Override
    public List<Integer> getDroppedInFlightMessageIds() throws KuraException {
        return Collections.emptyList();
    }

    // Helpers

    private void doUnsubscribe(String deviceId, String topic) throws KuraException {
        final String internalQueue = buildTopicName(deviceId, topic);

        try {
            ShutdownStrategy strategy = this.camelContext.getShutdownStrategy();
            if (strategy instanceof DefaultShutdownStrategy) {
                if (((DefaultShutdownStrategy) strategy).getCurrentShutdownTaskFuture() != null) {
                    logger.info("Skipping cleanup of '{}' since the camel context is being shut down", internalQueue);
                    // we are "in shutdown" and would deadlock
                    return;
                }
            }

            // perform shutdown

            this.camelContext.stopRoute(internalQueue);
            this.camelContext.removeRoute(internalQueue);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.SUBSCRIPTION_ERROR, e, internalQueue);
        }
    }

    private String buildTopicName(final String deviceId, final String topic) {
        requireNonNull(topic, "'topic' must not be null");

        if (deviceId == null) {
            return String.format("%s:%s", this.applicationId, topic);
        } else {
            return String.format("%s:%s:%s", deviceId, this.applicationId, topic);
        }
    }

    private int doPublish(boolean isControl, String deviceId, String topic, KuraPayload kuraPayload, int qos,
            boolean retain, int priority) throws KuraException {
        final String target = target(buildTopicName(deviceId, topic));
        final int kuraMessageId = this.messageIdGenerator.getAsInt();

        final Map<String, Object> headers = new HashMap<>();
        headers.put(CAMEL_KURA_CLOUD_CONTROL, isControl);
        headers.put(CAMEL_KURA_CLOUD_MESSAGEID, kuraMessageId);
        headers.put(CAMEL_KURA_CLOUD_DEVICEID, deviceId);
        headers.put(CAMEL_KURA_CLOUD_QOS, qos);
        headers.put(CAMEL_KURA_CLOUD_RETAIN, retain);
        headers.put(CAMEL_KURA_CLOUD_PRIORITY, priority);

        logger.trace("Publishing: {} -> {} / {}", target, kuraPayload, this.camelContext);

        this.producerTemplate.sendBodyAndHeaders(target, kuraPayload, headers);

        return kuraMessageId;
    }

    private void forkSubscribe(final boolean isControl, final String deviceId, final String topic, final int qos)
            throws KuraException {
        /*
         * This construct is needed due to CAMEL-10206
         *
         * It does fork off the subscription process, which actually creates a
         * new camel route, into the background since we currently may be in the
         * process of starting the camel context. If that is the case then the
         * newly added route won't be started since the camel context is in the
         * "starting" mode. Events won't get processed.
         *
         * So we do fork off the subscription process after the camel context
         * has been started. The executor is needed since, according to the
         * camel javadoc on StartupListener, the camel context may still be in
         * "starting" mode when the "onCamelContextStarted" method is called.
         */
        try {
            this.camelContext.addStartupListener(new StartupListener() {

                @Override
                public void onCamelContextStarted(CamelContext context, boolean alreadyStarted) throws Exception {
                    CamelCloudClient.this.executorService.submit(new Callable<Void>() {

                        @Override
                        public Void call() throws Exception {
                            doSubscribe(isControl, deviceId, topic, qos);
                            return null;
                        }
                    });
                }
            });
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.SUBSCRIPTION_ERROR, e, buildTopicName(deviceId, topic));
        }
    }

    private void doSubscribe(final boolean isControl, String deviceId, final String topic, final int qos)
            throws KuraException {
        logger.debug("About to subscribe to topic {}:{} with QOS {}.", deviceId, topic, qos);
        final String internalQueue = buildTopicName(deviceId, topic);
        logger.debug("\tInternal target: {} / {}", target(internalQueue), this.camelContext);
        try {
            this.camelContext.addRoutes(new RouteBuilder() {

                @Override
                public void configure() throws Exception {
                    from(target(internalQueue)).routeId(internalQueue).process(new Processor() {

                        @Override
                        public void process(Exchange exchange) throws Exception {
                            logger.debug("Processing: {}", exchange);
                            for (CloudClientListener listener : CamelCloudClient.this.cloudClientListeners) {
                                logger.debug("\t{}", listener);
                                Object body = exchange.getIn().getBody();
                                KuraPayload payload;
                                if (body instanceof KuraPayload) {
                                    payload = (KuraPayload) body;
                                } else {
                                    payload = new KuraPayload();
                                    payload.setBody(getContext().getTypeConverter().convertTo(byte[].class, body));
                                }
                                String deviceId = exchange.getIn().getHeader(CAMEL_KURA_CLOUD_DEVICEID, String.class);
                                int qos = exchange.getIn().getHeader(CAMEL_KURA_CLOUD_QOS, 0, int.class);
                                listener.onMessageArrived(deviceId, "camel", payload, qos, true);
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            logger.warn("Error while adding subscription route. Rethrowing root cause.");
            throw new KuraException(CONFIGURATION_ERROR, e);
        }
    }

    private String target(String topic) {
        if (this.baseEndpoint.contains("%s")) {
            return format(this.baseEndpoint, topic);
        }
        return this.baseEndpoint + topic;
    }
}
