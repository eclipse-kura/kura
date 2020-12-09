/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 *
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *   Eurotech
 *******************************************************************************/

package org.eclipse.kura.demo.bnd.heater;

import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.ComponentException;

/**
 * The Heater demo using annotations
 * <p>
 * <strong>Note:</strong> The component must be marked as
 * {@code immediate = true} and {@code configurationPolicy = REQUIRE}.
 * </p>
 *
 * @author David Woodard
 */
@Designate(ocd = HeaterConfig.class)
@Component(
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = "service.pid=org.eclipse.kura.demo.bnd.heater.Heater"
)
public class Heater implements ConfigurableComponent, CloudClientListener {

    private static final Logger logger = LoggerFactory.getLogger(Heater.class);

    // Cloud Application identifier
    private static final String APP_ID = "heater";

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
    private static final String PUBLISH_TOPIC_PROP_NAME = "publish.semanticTopic";
    private static final String PUBLISH_QOS_PROP_NAME = "publish.qos";
    private static final String PUBLISH_RETAIN_PROP_NAME = "publish.retain";

    private CloudService cloudService;
    private CloudClient cloudClient;

    private final ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;

    private float temperature;
    private Map<String, Object> properties;
    private final Random random;

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

    @Reference(
            name = "CloudService",
            service = CloudService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.STATIC,
            unbind = "unsetCloudService",
            target = "(kura.service.pid=org.eclipse.kura.cloud.CloudService)"
    )
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

    @Activate
    protected void activate(Map<String, Object> properties) {
        logger.info("Activating Heater...");

        this.properties = properties;
        for (String s : properties.keySet()) {
            logger.info("Activate - {}: {}", s, properties.get(s));
        }

        // get the mqtt client for this application
        try {

            // Acquire a Cloud Application Client for this Application
            logger.info("Getting CloudClient for {}...", APP_ID);
            this.cloudClient = this.cloudService.newCloudClient(APP_ID);
            this.cloudClient.addCloudClientListener(this);

            // Don't subscribe because these are handled by the default
            // subscriptions and we don't want to get messages twice
            doUpdate(false);
        } catch (Exception e) {
            logger.error("Error during component activation", e);
            throw new ComponentException(e);
        }
        logger.info("Activating Heater... Done.");
    }

    @Deactivate
    protected void deactivate() {
        logger.debug("Deactivating Heater...");

        // shutting down the worker and cleaning up the properties
        this.worker.shutdown();

        // Releasing the CloudApplicationClient
        logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
        this.cloudClient.release();

        logger.debug("Deactivating Heater... Done.");
    }

    @Modified
    public void updated(Map<String, Object> properties) {
        logger.info("Updated Heater...");

        // store the properties received
        this.properties = properties;
        for (String s : properties.keySet()) {
            logger.info("Update - {}: {}", s, properties.get(s));
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
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConnectionLost() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConnectionEstablished() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessageConfirmed(int messageId, String appTopic) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessagePublished(int messageId, String appTopic) {
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
        // fetch the publishing configuration from the publishing properties
        String topic = (String) this.properties.get(PUBLISH_TOPIC_PROP_NAME);
        Integer qos = (Integer) this.properties.get(PUBLISH_QOS_PROP_NAME);
        Boolean retain = (Boolean) this.properties.get(PUBLISH_RETAIN_PROP_NAME);
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

        // Publish the message
        try {
            this.cloudClient.publish(topic, payload, qos, retain);
            logger.info("Published to {} message: {}", topic, payload);
        } catch (Exception e) {
            logger.error("Cannot publish topic: {}", topic, e);
        }
    }
}
