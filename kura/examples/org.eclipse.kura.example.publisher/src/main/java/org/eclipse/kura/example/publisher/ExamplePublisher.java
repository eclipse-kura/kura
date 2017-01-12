/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
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

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExamplePublisher implements ConfigurableComponent, CloudClientListener {

    private static final Logger logger = LoggerFactory.getLogger(ExamplePublisher.class);

    // Cloud Application identifier
    private static final String APP_ID = "EXAMPLE_PUBLISHER";

    // Publishing Property Names
    private static final String PUBLISH_RATE_PROP_NAME = "publish.rate";
    private static final String PUBLISH_TOPIC_PROP_NAME = "publish.appTopic";
    private static final String PUBLISH_QOS_PROP_NAME = "publish.qos";
    private static final String PUBLISH_RETAIN_PROP_NAME = "publish.retain";
    private static final String TEMP_INITIAL_PROP_NAME = "metric.temperature.initial";
    private static final String TEMP_INCREMENT_PROP_NAME = "metric.temperature.increment";
    private static final String[] METRIC_PROP_NAMES = { "metric.string", "metric.string.oneof", "metric.long",
            "metric.integer", "metric.integer.fixed", "metric.short", "metric.double", "metric.float", "metric.char",
            "metric.byte", "metric.boolean", "metric.password" };

    private CloudService cloudService;
    private CloudClient cloudClient;

    private ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;

    private float temperature;
    private Map<String, Object> properties;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setCloudService(CloudService cloudService) {
        this.cloudService = cloudService;
    }

    public void unsetCloudService(CloudService cloudService) {
        this.cloudService = null;
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

        // get the mqtt client for this application
        try {

            // Acquire a Cloud Application Client for this Application
            logger.info("Getting CloudApplicationClient for {}...", APP_ID);
            this.cloudClient = this.cloudService.newCloudClient(APP_ID);
            this.cloudClient.addCloudClientListener(this);

            // Don't subscribe because these are handled by the default
            // subscriptions and we don't want to get messages twice
            doUpdate();
        } catch (Exception e) {
            logger.error("Error during component activation", e);
            throw new ComponentException(e);
        }
        logger.info("Activating ExamplePublisher... Done.");
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating ExamplePublisher...");

        // shutting down the worker and cleaning up the properties
        this.worker.shutdown();

        // Releasing the CloudApplicationClient
        logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
        this.cloudClient.release();

        logger.debug("Deactivating ExamplePublisher... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updated ExamplePublisher...");

        // store the properties received
        this.properties = properties;
        dumpProperties("Update", properties);

        // try to kick off a new job
        doUpdate();
        logger.info("Updated ExamplePublisher... Done.");
    }

    // ----------------------------------------------------------------
    //
    // Cloud Application Callback Methods
    //
    // ----------------------------------------------------------------

    @Override
    public void onConnectionEstablished() {
        logger.info("Connection established");

        try {
            // Getting the lists of unpublished messages
            logger.info("Number of unpublished messages: {}", this.cloudClient.getUnpublishedMessageIds().size());
        } catch (KuraException e) {
            logger.error("Cannot get the list of unpublished messages");
        }

        try {
            // Getting the lists of in-flight messages
            logger.info("Number of in-flight messages: {}", this.cloudClient.getInFlightMessageIds().size());
        } catch (KuraException e) {
            logger.error("Cannot get the list of in-flight messages");
        }

        try {
            // Getting the lists of dropped in-flight messages
            logger.info("Number of dropped in-flight messages: {}",
                    this.cloudClient.getDroppedInFlightMessageIds().size());
        } catch (KuraException e) {
            logger.error("Cannot get the list of dropped in-flight messages");
        }
    }

    @Override
    public void onConnectionLost() {
        logger.warn("Connection lost!");
    }

    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        logger.info("Control message arrived on assetId: {} and semantic topic: {}", deviceId, appTopic);
    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        logger.info("Message arrived on assetId: {} and semantic topic: {}", deviceId, appTopic);
    }

    @Override
    public void onMessagePublished(int messageId, String appTopic) {
        logger.info("Published message with ID: {} on application topic: {}", messageId, appTopic);
    }

    @Override
    public void onMessageConfirmed(int messageId, String appTopic) {
        logger.info("Confirmed message with ID: {} on application topic: {}", messageId, appTopic);
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

        if (!this.properties.containsKey(TEMP_INITIAL_PROP_NAME)
                || !this.properties.containsKey(PUBLISH_RATE_PROP_NAME)) {
            logger.info(
                    "Update ExamplePublisher - Ignore as properties do not contain TEMP_INITIAL_PROP_NAME and PUBLISH_RATE_PROP_NAME.");
            return;
        }

        // reset the temperature to the initial value
        this.temperature = (Float) this.properties.get(TEMP_INITIAL_PROP_NAME);

        // schedule a new worker based on the properties of the service
        int pubrate = (Integer) this.properties.get(PUBLISH_RATE_PROP_NAME);
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
        // fetch the publishing configuration from the publishing properties
        String topic = (String) this.properties.get(PUBLISH_TOPIC_PROP_NAME);
        Integer qos = (Integer) this.properties.get(PUBLISH_QOS_PROP_NAME);
        Boolean retain = (Boolean) this.properties.get(PUBLISH_RETAIN_PROP_NAME);

        // Increment the simulated temperature value
        float tempIncr = (Float) this.properties.get(TEMP_INCREMENT_PROP_NAME);
        this.temperature += tempIncr;

        // Allocate a new payload
        KuraPayload payload = new KuraPayload();

        // Timestamp the message
        payload.setTimestamp(new Date());

        // Add the temperature as a metric to the payload
        payload.addMetric("temperature", this.temperature);

        // add all the other metrics
        for (String metric : METRIC_PROP_NAMES) {
            if ("metric.char".equals(metric)) {
                // publish character as a string as the
                // "char" type is not support in the EDC Payload
                payload.addMetric(metric, String.valueOf(this.properties.get(metric)));
            } else if ("metric.short".equals(metric)) {
                // publish short as an integer as the
                // "short " type is not support in the EDC Payload
                payload.addMetric(metric, ((Short) this.properties.get(metric)).intValue());
            } else if ("metric.byte".equals(metric)) {
                // publish byte as an integer as the
                // "byte" type is not support in the EDC Payload
                payload.addMetric(metric, ((Byte) this.properties.get(metric)).intValue());
            } else {
                payload.addMetric(metric, this.properties.get(metric));
            }
        }

        // Publish the message
        try {
            int messageId = this.cloudClient.publish(topic, payload, qos, retain);
            logger.info("Published to {} message: {} with ID: {}", new Object[] { topic, payload, messageId });
        } catch (Exception e) {
            logger.error("Cannot publish topic: " + topic, e);
        }
    }
}
