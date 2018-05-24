/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech and/or its affiliates
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.example.publisher;

import static java.util.Objects.nonNull;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExamplePublisher implements ConfigurableComponent, CloudSubscriberListener, CloudConnectionListener, CloudDeliveryListener {

    private static final Logger logger = LoggerFactory.getLogger(ExamplePublisher.class);

    private CloudPublisher cloudPublisher;

    private CloudSubscriber cloudSubscriber;

    private ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;

    private float temperature;
    private Map<String, Object> properties;

    private ExamplePublisherOptions examplePublisherOptions;

    public void setCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher = cloudPublisher;
        this.cloudPublisher.registerCloudConnectionListener(ExamplePublisher.this);
        this.cloudPublisher.registerCloudDeliveryListener(ExamplePublisher.this);
    }

    public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher.unregisterCloudConnectionListener(ExamplePublisher.this);
        this.cloudPublisher.unregisterCloudDeliveryistener(ExamplePublisher.this);
        this.cloudPublisher = null;
    }

    public void setCloudSubscriber(CloudSubscriber cloudSubscriber) {
        this.cloudSubscriber = cloudSubscriber;
        this.cloudSubscriber.registerCloudSubscriberListener(ExamplePublisher.this);
        this.cloudSubscriber.registerCloudConnectionListener(ExamplePublisher.this);
    }

    public void unsetCloudSubscriber(CloudSubscriber cloudSubscriber) {
        this.cloudSubscriber.unregisterCloudSubscriberListener(ExamplePublisher.this);
        this.cloudSubscriber.unregisterCloudConnectionListener(ExamplePublisher.this);
        this.cloudSubscriber = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("Activating ExamplePublisher...");

        // start worker
        this.worker = Executors.newSingleThreadScheduledExecutor();

        this.properties = properties;
        dumpProperties("Activate", properties);

        this.examplePublisherOptions = new ExamplePublisherOptions(properties);

        doUpdate();

        logger.info("Activating ExamplePublisher... Done.");
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("Deactivating ExamplePublisher...");

        // shutting down the worker and cleaning up the properties
        this.worker.shutdown();

        logger.info("Deactivating ExamplePublisher... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updated ExamplePublisher...");

        // store the properties received
        this.properties = properties;
        dumpProperties("Update", properties);

        this.examplePublisherOptions = new ExamplePublisherOptions(properties);

        // try to kick off a new job
        doUpdate();
        logger.info("Updated ExamplePublisher... Done.");
    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    /**
     * Dump properties in stable order
     *
     * @param properties
     *            the properties to dump
     */
    private static void dumpProperties(final String action, final Map<String, Object> properties) {
        final Set<String> keys = new TreeSet<>(properties.keySet());
        for (final String key : keys) {
            logger.info("{} - {}: {}", action, key, properties.get(key));
        }
    }

    /**
     * Called after a new set of properties has been configured on the service
     */
    private void doUpdate() {
        // cancel a current worker handle if one if active
        if (this.handle != null) {
            this.handle.cancel(true);
        }

        // reset the temperature to the initial value
        this.temperature = this.examplePublisherOptions.getTempInitial();

        // schedule a new worker based on the properties of the service
        int pubrate = this.examplePublisherOptions.getPublishRate();
        this.handle = this.worker.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                doPublish();
            }
        }, 0, pubrate, TimeUnit.MILLISECONDS);
    }

    /**
     * Called at the configured rate to publish the next temperature measurement.
     */
    private void doPublish() {
        // Increment the simulated temperature value
        float tempIncr = this.examplePublisherOptions.getTempIncrement();
        this.temperature += tempIncr;

        // Allocate a new payload
        KuraPayload payload = new KuraPayload();

        // Timestamp the message
        payload.setTimestamp(new Date());

        // Add the temperature as a metric to the payload
        payload.addMetric("temperature", this.temperature);

        // add all the other metrics
        for (String metric : this.examplePublisherOptions.getMetricsPropertiesNames()) {
            if ("metric.char".equals(metric)) {
                // publish character as a string as the
                // "char" type is not supported in the Kura Payload
                payload.addMetric(metric, String.valueOf(this.properties.get(metric)));
            } else if ("metric.short".equals(metric)) {
                // publish short as an integer as the
                // "short " type is not supported in the Kura Payload
                payload.addMetric(metric, ((Short) this.properties.get(metric)).intValue());
            } else if ("metric.byte".equals(metric)) {
                // publish byte as an integer as the
                // "byte" type is not supported in the Kura Payload
                payload.addMetric(metric, ((Byte) this.properties.get(metric)).intValue());
            } else {
                payload.addMetric(metric, this.properties.get(metric));
            }
        }

        // Publish the message
        try {
            if (nonNull(this.cloudPublisher)) {
                KuraMessage message = new KuraMessage(payload);
                String messageId = this.cloudPublisher.publish(message);
                logger.info("Published to message: {} with ID: {}", message, messageId);
            }
        } catch (Exception e) {
            logger.error("Cannot publish: ", e);
        }
    }

    private void logReceivedMessage(KuraMessage msg) {
        KuraPayload payload = msg.getPayload();
        Date timestamp = payload.getTimestamp();
        if (timestamp != null) {
            logger.info("Message timestamp: {}", timestamp.getTime());
        }

        KuraPosition position = payload.getPosition();
        if (position != null) {
            logger.info("Position latitude: {}", position.getLatitude());
            logger.info("         longitude: {}", position.getLongitude());
            logger.info("         altitude: {}", position.getAltitude());
            logger.info("         heading: {}", position.getHeading());
            logger.info("         precision: {}", position.getPrecision());
            logger.info("         satellites: {}", position.getSatellites());
            logger.info("         speed: {}", position.getSpeed());
            logger.info("         status: {}", position.getStatus());
            logger.info("         timestamp: {}", position.getTimestamp());
        }

        byte[] body = payload.getBody();
        if (body != null && body.length != 0) {
            logger.info("Body lenght: {}", body.length);
        }

        if (payload.metrics() != null) {
            for (Entry<String, Object> entry : payload.metrics().entrySet()) {
                logger.info("Message metric: {}, value: {}", entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void onConnectionEstablished() {
        logger.info("Connection established");
    }

    @Override
    public void onConnectionLost() {
        logger.warn("Connection lost!");
    }

    @Override
    public void onMessageArrived(KuraMessage message) {
        logReceivedMessage(message);
    }

    @Override
    public void onDisconnected() {
        logger.warn("On disconnected");
    }

    @Override
    public void onMessageConfirmed(String messageId) {
        logger.info("Confirmed message with id: {}", messageId);
    }
}
