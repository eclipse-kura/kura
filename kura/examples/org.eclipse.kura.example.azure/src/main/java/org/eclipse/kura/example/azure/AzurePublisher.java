/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.example.azure;

import java.util.Map;
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

public class AzurePublisher implements ConfigurableComponent, CloudClientListener 
{
    private static final Logger s_logger = LoggerFactory.getLogger(AzurePublisher.class);

    // Publishing Property Names
    private static final String PUBLISH_RATE_PROP_NAME   = "publish.rate";
    private static final String PUBLISH_QOS_PROP_NAME    = "publish.qos";
    private static final String PUBLISH_RETAIN_PROP_NAME = "publish.retain";
    private static final String TEMP_INITIAL_PROP_NAME   = "metric.temperature.initial";
    private static final String TEMP_INCREMENT_PROP_NAME = "metric.temperature.increment";

    // Cloud Application identifier - for Azure IoT APP_ID must be messages 
    private static final String APP_ID = "messages";

    private CloudService cloudService;
    private CloudClient cloudClient;

    private final ScheduledExecutorService m_worker;
    private ScheduledFuture<?> m_handle;

    private float m_temperature;
    private Map<String, Object> m_properties;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public AzurePublisher() {
        super();
        this.m_worker = Executors.newSingleThreadScheduledExecutor();
    }

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

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) 
    {
        s_logger.info("Activating AzurePublisher...");

        this.m_properties = properties;
        for (String s : properties.keySet()) {
            s_logger.info("Activate - " + s + ": " + properties.get(s));
        }

        // get the mqtt client for this application
        try {

            // Acquire a Cloud Application Client for this Application
            s_logger.info("Getting CloudApplicationClient for {}...", APP_ID);
            this.cloudClient = this.cloudService.newCloudClient(APP_ID);
            this.cloudClient.addCloudClientListener(this);

            // Don't subscribe because these are handled by the default
            // subscriptions and we don't want to get messages twice
            doUpdate();
        } 
        catch (Exception e) {
            s_logger.error("Error during component activation", e);
            throw new ComponentException(e);
        }
        s_logger.info("Activating AzurePublisher... Done.");
    }

    protected void deactivate(ComponentContext componentContext) 
    {
        s_logger.debug("Deactivating AzurePublisher...");

        // shutting down the worker and cleaning up the properties
        this.m_worker.shutdown();

        // Releasing the CloudApplicationClient
        s_logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
        this.cloudClient.release();

        s_logger.debug("Deactivating AzurePublisher... Done.");
    }

    public void updated(Map<String, Object> properties) {
        s_logger.info("Updated AzurePublisher...");

        // store the properties received
        this.m_properties = properties;
        for (String s : properties.keySet()) {
            s_logger.info("Update - " + s + ": " + properties.get(s));
        }

        // try to kick off a new job
        doUpdate();
        s_logger.info("Updated AzurePublisher... Done.");
    }

    // ----------------------------------------------------------------
    //
    // DataServiceListener Callback Methods
    //
    // ----------------------------------------------------------------

    @Override
    public void onConnectionEstablished() {
        s_logger.info("Connection established");
        try {
	        s_logger.info("Subscribing to messages/devicebound/# ...");
			this.cloudClient.subscribe("devicebound/#", 0);

			s_logger.info("Subscribing to messages/devicebound/# ... Done.");
		} 
        catch (KuraException e) {
            s_logger.error("Error during subscription to cloud-to-device messages", e);
		}
    }

    @Override
    public void onMessagePublished(int messageId, String appTopic) {
        s_logger.info("Published message with ID: {} on application topic: {}", messageId, appTopic);
    }

    @Override
    public void onMessageConfirmed(int messageId, String appTopic) {
        s_logger.info("Confirmed message with ID: {} on application topic: {}", messageId, appTopic);
    }

    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        s_logger.info("Control Message Arrived - deviceId: "+deviceId);
        s_logger.info("Control Message Arrived - appTopic: "+appTopic);
        s_logger.info("Control Message Arrived - payload: "+new String(msg.getBody()));
        s_logger.info("Control Message Arrived - qos: "+ qos);
        s_logger.info("Control Message Arrived - retained: "+ retain);
        s_logger.info("Control message arrived on assetId: {} and semantic topic: {}", deviceId, appTopic);
    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        s_logger.info("Control Message Arrived - deviceId: "+deviceId);
        s_logger.info("Control Message Arrived - appTopic: "+appTopic);
        s_logger.info("Message Arrived - payload: "+ new String(msg.getBody()));
        s_logger.info("Message Arrived - qos: "+ qos);
        s_logger.info("Message Arrived - retained: "+ retain);
	}

	@Override
	public void onConnectionLost() {
        s_logger.warn("Connection lost!");
	}

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    /**
     * Called after a new set of properties has been configured on the service
     */
    private void doUpdate() {
        // cancel a current worker handle if one if active
        if (this.m_handle != null) {
            this.m_handle.cancel(true);
        }

        if (!this.m_properties.containsKey(TEMP_INITIAL_PROP_NAME) ||
            !this.m_properties.containsKey(PUBLISH_RATE_PROP_NAME)) {
            s_logger.info(
                    "Update AzurePublisher - Ignore as properties do not contain TEMP_INITIAL_PROP_NAME and PUBLISH_RATE_PROP_NAME.");
            return;
        }

        // reset the temperature to the initial value
        this.m_temperature = (Float) this.m_properties.get(TEMP_INITIAL_PROP_NAME);

        // schedule a new worker based on the properties of the service
        int pubrate = (Integer) this.m_properties.get(PUBLISH_RATE_PROP_NAME);
        this.m_handle = this.m_worker.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                doPublish();
            }
        }, 0, pubrate, TimeUnit.MILLISECONDS);
    }

    /**
     * Called at the configured rate to publish the next temperature measurement.
     */
    private void doPublish() 
    {
        // Increment the simulated temperature value
        float tempIncr = (Float) this.m_properties.get(TEMP_INCREMENT_PROP_NAME);
        this.m_temperature += tempIncr;

        // Build a new JSON payload
        StringBuilder sbJson = new StringBuilder();
        sbJson.append("{ temperature: ")
        	  .append(this.m_temperature)
        	  .append("}");

//        // Add the temperature as a metric to the payload
//        KuraPayload payload = new KuraPayload();
//        payload.setTimestamp(new Date());
//        payload.addMetric("temperature", this.m_temperature);
//
//        // add all the other metrics
//        for (String metric : METRIC_PROP_NAMES) {
//            if ("metric.char".equals(metric)) {
//                // publish character as a string as the
//                // "char" type is not support in the EDC Payload
//                payload.addMetric(metric, String.valueOf(this.m_properties.get(metric)));
//            } else if ("metric.short".equals(metric)) {
//                // publish short as an integer as the
//                // "short " type is not support in the EDC Payload
//                payload.addMetric(metric, ((Short) this.m_properties.get(metric)).intValue());
//            } else if ("metric.byte".equals(metric)) {
//                // publish byte as an integer as the
//                // "byte" type is not support in the EDC Payload
//                payload.addMetric(metric, ((Byte) this.m_properties.get(metric)).intValue());
//            } else {
//                payload.addMetric(metric, this.m_properties.get(metric));
//            }
//        }

        // Publish the message
        // fetch the publishing configuration from the publishing properties
        String  topic    = "events/"; // Telemetry topics in Azure are messages/events
        Integer qos      = (Integer) this.m_properties.get(PUBLISH_QOS_PROP_NAME);
        Boolean retain   = (Boolean) this.m_properties.get(PUBLISH_RETAIN_PROP_NAME);
        Integer priority = 5; // as recommended by Kura guidelines 
        String  payload  = sbJson.toString();
        try {
        
        	int messageId = this.cloudClient.publish(topic, payload.getBytes(), qos, retain, priority);
            s_logger.info("Published to {} message: {} with ID: {}", new Object[] { topic, payload, messageId });
        } 
        catch (Exception e) {
            s_logger.error("Cannot publish topic: " + topic, e);
        }
    }
}
