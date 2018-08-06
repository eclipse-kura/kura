/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.demo.heater;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Heater implements ConfigurableComponent, CloudConnectionListener, CloudDeliveryListener {

    private static final Logger logger = LoggerFactory.getLogger(Heater.class);

    // Publishing Property Names
    private static final String MODE_PROP_NAME = "mode";
    private static final String MODE_PROP_PROGRAM = "Program";
    private static final String MODE_PROP_MANUAL = "Manual";
    private static final String MODE_PROP_VACATION = "Vacation";

    private static final String PROGRAM_SETPOINT_NAME = "program.setPoint";
    private static final String MANUAL_SETPOINT_NAME = "manual.setPoint";

    private static final String TEMP_INITIAL_PROP_NAME = "temperature.initial";
    private static final String TEMP_INCREMENT_PROP_NAME = "temperature.increment";

    private static final String PUBLISH_RATE_PROP_NAME = "publish.rate";

    private final ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;

    private float temperature;
    private Map<String, Object> properties;
    private final Random random;

    private CloudPublisher cloudPublisher;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public Heater() {
        super();
        this.random = new Random();
        this.worker = Executors.newSingleThreadScheduledExecutor();
    }

    public void setCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher = cloudPublisher;
        this.cloudPublisher.registerCloudConnectionListener(Heater.this);
        this.cloudPublisher.registerCloudDeliveryListener(Heater.this);
    }

    public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher.unregisterCloudConnectionListener(Heater.this);
        this.cloudPublisher.unregisterCloudDeliveryistener(Heater.this);
        this.cloudPublisher = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("Activating Heater...");

        this.properties = properties;
        for (Entry<String, Object> property : properties.entrySet()) {
            logger.info("Update - {}: {}", property.getKey(), property.getValue());
        }

        // get the mqtt client for this application
        try {
            // Don't subscribe because these are handled by the default
            // subscriptions and we don't want to get messages twice
            doUpdate(false);
        } catch (Exception e) {
            logger.error("Error during component activation", e);
            throw new ComponentException(e);
        }
        logger.info("Activating Heater... Done.");
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating Heater...");

        // shutting down the worker and cleaning up the properties
        this.worker.shutdown();

        logger.debug("Deactivating Heater... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updated Heater...");

        // store the properties received
        this.properties = properties;
        for (Entry<String, Object> property : properties.entrySet()) {
            logger.info("Update - {}: {}", property.getKey(), property.getValue());
        }

        // try to kick off a new job
        doUpdate(true);
        logger.info("Updated Heater... Done.");
    }

    // ----------------------------------------------------------------
    //
    // Cloud Application Callback Methods
    //
    // ----------------------------------------------------------------

    @Override
    public void onConnectionLost() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConnectionEstablished() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessageConfirmed(String messageId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDisconnected() {
        // TODO Auto-generated method stub

    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    /**
     * Called after a new set of properties has been configured on the service
     */
    private void doUpdate(boolean onUpdate) {
        // cancel a current worker handle if one if active
        if (this.handle != null) {
            this.handle.cancel(true);
        }

        if (!this.properties.containsKey(TEMP_INITIAL_PROP_NAME)
                || !this.properties.containsKey(PUBLISH_RATE_PROP_NAME)) {
            logger.info(
                    "Update Heater - Ignore as properties do not contain TEMP_INITIAL_PROP_NAME and PUBLISH_RATE_PROP_NAME.");
            return;
        }

        // reset the temperature to the initial value
        if (!onUpdate) {
            this.temperature = (Float) this.properties.get(TEMP_INITIAL_PROP_NAME);
        }

        // schedule a new worker based on the properties of the service
        int pubrate = (Integer) this.properties.get(PUBLISH_RATE_PROP_NAME);
        this.handle = this.worker.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setName(getClass().getSimpleName());
                doPublish();
            }
        }, 0, pubrate, TimeUnit.SECONDS);
    }

    /**
     * Called at the configured rate to publish the next temperature measurement.
     */
    private void doPublish() {
        if (this.cloudPublisher == null) {
            logger.info("No cloud publisher selected. Cannot publish!");
            return;
        }

        // fetch the publishing configuration from the publishing properties
        String mode = (String) this.properties.get(MODE_PROP_NAME);

        // Increment the simulated temperature value
        float setPoint = 0;
        float tempIncr = (Float) this.properties.get(TEMP_INCREMENT_PROP_NAME);
        if (MODE_PROP_PROGRAM.equals(mode)) {
            setPoint = (Float) this.properties.get(PROGRAM_SETPOINT_NAME);
        } else if (MODE_PROP_MANUAL.equals(mode)) {
            setPoint = (Float) this.properties.get(MANUAL_SETPOINT_NAME);
        } else if (MODE_PROP_VACATION.equals(mode)) {
            setPoint = 6.0F;
        }
        if (this.temperature + tempIncr < setPoint) {
            this.temperature += tempIncr;
        } else {
            this.temperature -= 4 * tempIncr;
        }

        // Allocate a new payload
        KuraPayload payload = new KuraPayload();

        // Timestamp the message
        payload.setTimestamp(new Date());

        // Add the temperature as a metric to the payload
        payload.addMetric("temperatureInternal", this.temperature);
        payload.addMetric("temperatureExternal", 5.0F);
        payload.addMetric("temperatureExhaust", 30.0F);

        int code = this.random.nextInt();
        if (this.random.nextInt() % 5 == 0) {
            payload.addMetric("errorCode", code);
        } else {
            payload.addMetric("errorCode", 0);
        }

        KuraMessage message = new KuraMessage(payload);

        // Publish the message
        try {
            this.cloudPublisher.publish(message);
            logger.info("Published message: {}", payload);
        } catch (Exception e) {
            logger.error("Cannot publish message: {}", message, e);
        }
    }
}
