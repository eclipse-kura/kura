/*******************************************************************************
 * Copyright (c) 2011, 2016 Red Hat and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.camel.camelcloud;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.message.KuraPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.eclipse.kura.KuraErrorCode.CONFIGURATION_ERROR;
import static org.eclipse.kura.KuraErrorCode.OPERATION_NOT_SUPPORTED;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.*;
import static java.lang.String.format;
import static org.apache.camel.ServiceStatus.Started;

public class CamelCloudClient implements CloudClient {

    private final Logger s_logger = LoggerFactory.getLogger(CamelCloudClient.class);

    private ScheduledExecutorService executorService;

    private final CamelCloudService cloudService;

    private final CamelContext camelContext;

    private final ProducerTemplate producerTemplate;

    private final List<CloudClientListener> cloudClientListeners = new LinkedList<CloudClientListener>();

    private final String applicationId;

    private final String baseEndpoint;

    public CamelCloudClient(CamelCloudService cloudService, CamelContext camelContext, String applicationId, String baseEndpoint) {
        this.cloudService = cloudService;
        this.camelContext = camelContext;
        this.producerTemplate = camelContext.createProducerTemplate();
        this.applicationId = applicationId;
        this.baseEndpoint = baseEndpoint;
        this.executorService = camelContext.getExecutorServiceManager().newDefaultScheduledThreadPool(this, "cloudClientExecutor");
    }

    public CamelCloudClient(CamelCloudService cloudService, CamelContext camelContext, String applicationId) {
        this(cloudService, camelContext, applicationId, "vm:%s");
    }

    // Cloud client API

    @Override
    public String getApplicationId() {
        return applicationId;
    }

    @Override
    public void release() {
        cloudService.release(applicationId);
    }

    @Override
    public boolean isConnected() {
        return camelContext.getStatus() == Started;
    }

    @Override
    public int publish(String topic, KuraPayload kuraPayload, int qos, boolean retain) throws KuraException {
        return publish(topic, kuraPayload, qos, retain, 5);
    }

    @Override
    public int publish(String topic, KuraPayload kuraPayload, int qos, boolean retain, int priority) throws KuraException {
        return doPublish(false, null, topic, kuraPayload, qos, retain, priority);
    }

    @Override
    public int publish(String s, byte[] bytes, int i, boolean b, int i1) throws KuraException {
        KuraPayload kuraPayload = new KuraPayload();
        kuraPayload.setBody(bytes);
        return publish(s, kuraPayload, i, b);
    }

    @Override
    public int controlPublish(String topic, KuraPayload payload, int qos, boolean retain, int priority) throws KuraException {
        return doPublish(true, null, topic, payload, qos, retain, priority);
    }

    @Override
    public int controlPublish(String deviceId, String topic, KuraPayload kuraPayload, int qos, boolean retain, int priority) throws KuraException {
        return doPublish(true, deviceId, topic, kuraPayload, qos, retain, priority);
    }

    @Override
    public int controlPublish(String deviceId, String topic, byte[] payload, int qos, boolean b, int priority) throws KuraException {
        KuraPayload kuraPayload = new KuraPayload();
        kuraPayload.setBody(payload);
        return doPublish(true, deviceId, topic, kuraPayload, qos, b, priority);
    }

    @Override
    public void subscribe(String topic, int qos) throws KuraException {
        doSubscribe(false, topic, qos);
    }

    @Override
    public void controlSubscribe(String topic, int qos) throws KuraException {
        doSubscribe(true, topic, qos);
    }

    @Override
    public void unsubscribe(String topic) throws KuraException {
        final String internalQueue = applicationId + ":" + topic;
        try {
            executorService.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        camelContext.stopRoute(internalQueue);
                        camelContext.removeRoute(internalQueue);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }, 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public void controlUnsubscribe(String topic) throws KuraException {
        unsubscribe(topic);
    }

    @Override
    public void addCloudClientListener(CloudClientListener cloudClientListener) {
        cloudClientListeners.add(cloudClientListener);
    }

    @Override
    public void removeCloudClientListener(CloudClientListener cloudClientListener) {
        cloudClientListeners.remove(cloudClientListener);
    }

    @Override
    public List<Integer> getUnpublishedMessageIds() throws KuraException {
        throw new KuraException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public List<Integer> getInFlightMessageIds() throws KuraException {
        throw new KuraException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public List<Integer> getDroppedInFlightMessageIds() throws KuraException {
        throw new KuraException(OPERATION_NOT_SUPPORTED);
    }

    // Helpers

    private int doPublish(boolean isControl, String deviceId, String topic, KuraPayload kuraPayload, int qos, boolean retain, int priority) throws KuraException {
        String target = target(applicationId + ":" + topic);
        int kuraMessageId = Math.abs(new Random().nextInt());

        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(CAMEL_KURA_CLOUD_CONTROL, isControl);
        headers.put(CAMEL_KURA_CLOUD_MESSAGEID, kuraMessageId);
        headers.put(CAMEL_KURA_CLOUD_DEVICEID, deviceId);
        headers.put(CAMEL_KURA_CLOUD_QOS, qos);
        headers.put(CAMEL_KURA_CLOUD_RETAIN, retain);
        headers.put(CAMEL_KURA_CLOUD_PRIORITY, priority);

        producerTemplate.sendBodyAndHeaders(target, kuraPayload, headers);
        return kuraMessageId;
    }

    private void doSubscribe(final boolean isControl, final String topic, final int qos) throws KuraException {
        s_logger.debug("About to subscribe to topic {} with QOS {}.", topic, qos);
        final String internalQueue = applicationId + ":" + topic;
        try {
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(target(internalQueue)).
                            routeId(internalQueue).
                            process(new Processor() {
                                @Override
                                public void process(Exchange exchange) throws Exception {
                                    for(CloudClientListener listener : cloudClientListeners) {
                                        Object body = exchange.getIn().getBody();
                                        KuraPayload payload;
                                        if(body instanceof KuraPayload) {
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
            s_logger.warn("Error while adding subscription route. Rethrowing root cause.");
            throw new KuraException(CONFIGURATION_ERROR, e);
        }
    }

    private String target(String topic) {
        if (baseEndpoint.contains("%s")) {
            return format(baseEndpoint, topic);
        }
        return baseEndpoint + topic;
    }

}
