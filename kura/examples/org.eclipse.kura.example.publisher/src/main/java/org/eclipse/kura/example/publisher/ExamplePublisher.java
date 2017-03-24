/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExamplePublisher implements ConfigurableComponent, CloudClientListener {

    /**
     * Inner class defined to track the CloudServices as they get added, modified or removed.
     * Specific methods can refresh the cloudService definition and setup again the Cloud Client.
     *
     */
    private final class CloudPublisherServiceTrackerCustomizer
            implements ServiceTrackerCustomizer<CloudService, CloudService> {

        @Override
        public CloudService addingService(final ServiceReference<CloudService> reference) {
            ExamplePublisher.this.cloudService = ExamplePublisher.this.bundleContext.getService(reference);
            try {
                // recreate the Cloud Client
                setupCloudClient();
            } catch (final KuraException e) {
                logger.error("Cloud Client setup failed!", e);
            }
            return ExamplePublisher.this.cloudService;
        }

        @Override
        public void modifiedService(final ServiceReference<CloudService> reference, final CloudService service) {
            ExamplePublisher.this.cloudService = ExamplePublisher.this.bundleContext.getService(reference);
            try {
                // recreate the Cloud Client
                setupCloudClient();
            } catch (final KuraException e) {
                logger.error("Cloud Client setup failed!", e);
            }
        }

        @Override
        public void removedService(final ServiceReference<CloudService> reference, final CloudService service) {
            ExamplePublisher.this.cloudService = null;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ExamplePublisher.class);

    private ServiceTrackerCustomizer<CloudService, CloudService> cloudServiceTrackerCustomizer;
    private ServiceTracker<CloudService, CloudService> cloudServiceTracker;
    private CloudService cloudService;
    private CloudClient cloudClient;
    private String oldSubscriptionTopic;

    private ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;

    private float temperature;
    private Map<String, Object> properties;

    private BundleContext bundleContext;

    private ExamplePublisherOptions examplePublisherOptions;

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

        this.bundleContext = componentContext.getBundleContext();

        this.examplePublisherOptions = new ExamplePublisherOptions(properties);

        this.cloudServiceTrackerCustomizer = new CloudPublisherServiceTrackerCustomizer();
        initCloudServiceTracking();
        doUpdate();
        subscribe();

        logger.info("Activating ExamplePublisher... Done.");
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating ExamplePublisher...");

        // shutting down the worker and cleaning up the properties
        this.worker.shutdown();

        // Releasing the CloudApplicationClient
        logger.info("Releasing CloudApplicationClient for {}...", this.examplePublisherOptions.getAppId());
        // close the client
        closeCloudClient();
        oldSubscriptionTopic = null;

        if (nonNull(this.cloudServiceTracker)) {
            this.cloudServiceTracker.close();
        }

        logger.debug("Deactivating ExamplePublisher... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updated ExamplePublisher...");

        // store the properties received
        this.properties = properties;
        dumpProperties("Update", properties);

        this.examplePublisherOptions = new ExamplePublisherOptions(properties);

        if (nonNull(this.cloudServiceTracker)) {
            this.cloudServiceTracker.close();
        }
        initCloudServiceTracking();

        // try to kick off a new job
        doUpdate();
        subscribe();
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

        subscribe();
    }

    @Override
    public void onConnectionLost() {
        logger.warn("Connection lost!");
    }

    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        logger.info("Control message arrived on assetId: {} and semantic topic: {}", deviceId, appTopic);
        logReceivedMessage(msg);
    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        logger.info("Message arrived on assetId: {} and semantic topic: {}", deviceId, appTopic);
        logReceivedMessage(msg);
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
        // fetch the publishing configuration from the publishing properties
        String topic = this.examplePublisherOptions.getAppTopic();
        Integer qos = this.examplePublisherOptions.getPublishQos();
        Boolean retain = this.examplePublisherOptions.getPublishRetain();

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
            if (nonNull(this.cloudService) && nonNull(this.cloudClient)) {
                int messageId = this.cloudClient.publish(topic, payload, qos, retain);
                logger.info("Published to {} message: {} with ID: {}", new Object[] { topic, payload, messageId });
            }
        } catch (Exception e) {
            logger.error("Cannot publish topic: {}", topic, e);
        }
    }

    private void initCloudServiceTracking() {
        String selectedCloudServicePid = this.examplePublisherOptions.getCloudServicePid();
        String filterString = String.format("(&(%s=%s)(kura.service.pid=%s))", Constants.OBJECTCLASS,
                CloudService.class.getName(), selectedCloudServicePid);
        Filter filter = null;
        try {
            filter = this.bundleContext.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            logger.error("Filter setup exception ", e);
        }
        this.cloudServiceTracker = new ServiceTracker<>(this.bundleContext, filter, this.cloudServiceTrackerCustomizer);
        this.cloudServiceTracker.open();
    }

    private void closeCloudClient() {
        if (nonNull(this.cloudClient)) {
            this.cloudClient.removeCloudClientListener(this);
            this.cloudClient.release();
            this.cloudClient = null;
        }
    }

    private void setupCloudClient() throws KuraException {
        closeCloudClient();
        // create the new CloudClient for the specified application
        final String appId = this.examplePublisherOptions.getAppId();
        this.cloudClient = this.cloudService.newCloudClient(appId);
        this.cloudClient.addCloudClientListener(this);
    }

    private void logReceivedMessage(KuraPayload msg) {
        Date timestamp = msg.getTimestamp();
        if (timestamp != null) {
            logger.info("Message timestamp: {}", timestamp.getTime());
        }

        KuraPosition position = msg.getPosition();
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

        byte[] body = msg.getBody();
        if (body != null && body.length != 0) {
            logger.info("Body lenght: {}", body.length);
        }

        if (msg.metrics() != null) {
            for (Entry<String, Object> entry : msg.metrics().entrySet()) {
                logger.info("Message metric: {}, value: {}", entry.getKey(), entry.getValue());
            }
        }
    }

    private void subscribe() {
        try {
            if (oldSubscriptionTopic != null) {
                this.cloudClient.unsubscribe(oldSubscriptionTopic);
            }
            logger.info("Subscribing to application topic {}", this.examplePublisherOptions.getSubscribeTopic());
            String newSubscriptionTopic = this.examplePublisherOptions.getSubscribeTopic();
            this.cloudClient.subscribe(this.examplePublisherOptions.getSubscribeTopic(), 0);
            oldSubscriptionTopic = newSubscriptionTopic;
        } catch (KuraStoreException e) {
            logger.warn("Failed to request device shadow", e);
        } catch (KuraException e) {
            logger.warn("Failed to subscribe", e);
        }
    }
}
