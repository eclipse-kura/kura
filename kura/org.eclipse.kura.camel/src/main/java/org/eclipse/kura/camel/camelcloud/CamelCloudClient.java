/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.*;
import static java.lang.String.format;
import static org.apache.camel.ServiceStatus.Started;

public class CamelCloudClient implements CloudClient {

    private final Logger LOG = LoggerFactory.getLogger(CamelCloudClient.class);

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
    }

    public CamelCloudClient(CamelCloudService cloudService, CamelContext camelContext, String applicationId) {
        this(cloudService, camelContext, applicationId, "seda:%s");
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
    public int controlPublish(String topic, KuraPayload payload, int i, boolean retain, int priority) throws KuraException {
        return doPublish(true, null, topic, payload, i, retain, priority);
    }

    @Override
    public int controlPublish(String s, String s1, KuraPayload kuraPayload, int i, boolean b, int i1) throws KuraException {
        return doPublish(true, s, s1, kuraPayload, i, b, i1);
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
        String internalQueue = applicationId + ":" + topic;
        try {
            camelContext.stopRoute(internalQueue);
            camelContext.removeRoute(internalQueue);
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
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Integer> getInFlightMessageIds() throws KuraException {
        throw new UnsupportedOperationException();    }

    @Override
    public List<Integer> getDroppedInFlightMessageIds() throws KuraException {
        throw new UnsupportedOperationException();
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
        LOG.debug("About to subscribe to topic {} with QOS {}.", topic, qos);
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
            LOG.warn("Error while adding subscription route. Rethrowing root cause.");
            throw new RuntimeException(e);
        }
    }

    private String target(String topic) {
        if (baseEndpoint.contains("%s")) {
            return format(baseEndpoint, topic);
        }
        return baseEndpoint + topic;
    }

}
