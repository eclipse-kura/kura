/*******************************************************************************
 * Copyright (c) 2011, 2016 Red Hat and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.camel.cloud;

import static org.apache.camel.builder.ExchangeBuilder.anExchange;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_CONTROL;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_DEVICEID;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_QOS;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_RETAIN;
import static org.eclipse.kura.camel.camelcloud.KuraCloudClientConstants.CAMEL_KURA_CLOUD_TOPIC;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.message.KuraPayload;

/**
 * Consumer implementation for {@link KuraCloudComponent}
 */
public class KuraCloudConsumer extends DefaultConsumer implements CloudClientListener {

    private final CloudClient cloudClient;

    public KuraCloudConsumer(Endpoint endpoint, Processor processor, CloudClient cloudClient) {
        super(endpoint, processor);
        this.cloudClient = cloudClient;
    }

    // Life-cycle

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        this.log.debug("Starting CloudClientListener.");

        this.cloudClient.addCloudClientListener(this);
        performSubscribe();
    }

    @Override
    protected void doStop() throws Exception {
        try {
            this.cloudClient.unsubscribe(getEndpoint().getTopic());
        } catch (Exception e) {
            this.log.info("Failed to unsubscribe", e);
        }
        this.cloudClient.removeCloudClientListener(this);
        this.log.debug("Stopping CloudClientListener.");
        super.doStop();
    }

    // CloudClientListener callbacks

    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        onInternalMessageArrived(deviceId, appTopic, msg, qos, retain, true);
    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        onInternalMessageArrived(deviceId, appTopic, msg, qos, retain, false);
    }

    @Override
    public void onConnectionLost() {
        this.log.debug("Executing empty 'onConnectionLost' callback.");
    }

    @Override
    public void onConnectionEstablished() {
        this.log.debug("Executing empty 'onConnectionLost' callback.");
        performSubscribe();
    }

    private void performSubscribe() {
        try {
            this.log.debug("Perform subscribe: {} / {}", this.cloudClient, getEndpoint().getTopic());
            this.cloudClient.subscribe(getEndpoint().getTopic(), 0);
        } catch (KuraException e) {
            this.log.warn("Failed to subscribe", e);
        }
    }

    @Override
    public void onMessageConfirmed(int messageId, String appTopic) {
        this.log.debug("Executing empty 'onMessageConfirmed' callback with message ID {} and application topic {}.", messageId, appTopic);
    }

    @Override
    public void onMessagePublished(int messageId, String appTopic) {
        this.log.debug("Executing empty 'onMessagePublished' callback with message ID {} and application topic {}.", messageId, appTopic);
    }

    // Helpers

    private void onInternalMessageArrived(String deviceId, String appTopic, KuraPayload message, int qos, boolean retain, boolean control) {
        this.log.debug("Received message with deviceId {}, application topic {}.", deviceId, appTopic);
        Exchange exchange = anExchange(getEndpoint().getCamelContext()).withBody(message).withHeader(CAMEL_KURA_CLOUD_TOPIC, appTopic).withHeader(CAMEL_KURA_CLOUD_DEVICEID, deviceId)
                .withHeader(CAMEL_KURA_CLOUD_QOS, qos).withHeader(CAMEL_KURA_CLOUD_CONTROL, control).withHeader(CAMEL_KURA_CLOUD_RETAIN, retain).build();
        exchange.setFromEndpoint(getEndpoint());
        try {
            getProcessor().process(exchange);
        } catch (Exception e) {
            handleException("Error while processing an incoming message:", e);
        }
    }

    // Getters

    @Override
    public KuraCloudEndpoint getEndpoint() {
        return (KuraCloudEndpoint) super.getEndpoint();
    }

}
