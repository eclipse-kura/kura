/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.configuration.change.manager;

import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;

public class ConfigurationChangeManager implements ConfigurableComponent, ServiceTrackerListener {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationChangeManager.class);

    private class ChangedConfiguration {

        protected long timestamp;
        protected String pid;

        public ChangedConfiguration(String pid) {
            this.timestamp = new Date().getTime();
            this.pid = pid;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ChangedConfiguration)) {
                return false;
            }

            return ((ChangedConfiguration) other).pid.equals(this.pid);
        }

        @Override
        public int hashCode() {
            return this.pid.hashCode();
        }

    }

    private ConfigurationChangeManagerOptions options;
    private CloudPublisher cloudPublisher;

    private final ScheduledExecutorService scheduledSendQueueExecutor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> futureSendQueue;
    private volatile boolean acceptNotifications = false;
    private final Queue<ChangedConfiguration> notificationsQueue = new LinkedList<>();
    private ComponentsServiceTracker serviceTracker;

    /*
     * Dependencies
     */

    public void setCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher = cloudPublisher;
    }

    public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
        if (this.cloudPublisher == cloudPublisher) {
            this.cloudPublisher = null;
        }
    }

    /*
     * Activation APIs
     */

    public void activate(final Map<String, Object> properties) throws InvalidSyntaxException {
        logger.info("Activating ConfigurationChangeManager...");

        this.acceptNotifications = false;

        BundleContext bundleContext = FrameworkUtil.getBundle(ConfigurationChangeManager.class).getBundleContext();
        this.serviceTracker = new ComponentsServiceTracker(bundleContext);

        updated(properties);

        logger.info("Activating ConfigurationChangeManager... Done.");
    }

    public void updated(final Map<String, Object> properties) {
        logger.info("Updating ConfigurationChangeManager...");

        this.options = new ConfigurationChangeManagerOptions(properties);

        if (this.options.isEnabled()) {
            this.acceptNotifications = true;
            this.serviceTracker.open(true);
            this.serviceTracker.addServiceTrackerListener(this);
        } else {
            this.acceptNotifications = false;
            this.serviceTracker.removeServiceTrackerListener(this);
            this.serviceTracker.close();
        }

        logger.info("Updating ConfigurationChangeManager... Done.");
    }

    public void deactivate() {
        logger.info("Deactivating ConfigurationChangeManager...");
        this.acceptNotifications = false;
        this.serviceTracker.removeServiceTrackerListener(this);
        this.serviceTracker.close();
        logger.info("Deactivating ConfigurationChangeManager... Done.");
    }

    @Override
    public void onConfigurationChanged(String pid) {
        if (this.acceptNotifications) {
            ChangedConfiguration conf = new ChangedConfiguration(pid);
            if (this.notificationsQueue.contains(conf)) {
                this.notificationsQueue.remove(conf);
            }
            this.notificationsQueue.add(conf);

            if (this.futureSendQueue != null) {
                this.futureSendQueue.cancel(false);
            }
            this.futureSendQueue = this.scheduledSendQueueExecutor.schedule(this::sendQueue,
                    this.options.getSendDelay(), TimeUnit.SECONDS);
        }
    }

    private byte[] createJsonFromNotificationsQueue() {
        GsonBuilder builder = new GsonBuilder();
        builder.setExclusionStrategies(new ExclusionStrategy() {

            @Override
            public boolean shouldSkipClass(Class<?> arg0) {
                return false;
            }

            @Override
            public boolean shouldSkipField(FieldAttributes arg0) {
                return arg0.getName().equals("timestamp");
            }

        });
        return builder.create().toJson(this.notificationsQueue).getBytes();
    }

    private void sendQueue() {
        KuraPayload payload = new KuraPayload();
        payload.setTimestamp(new Date(this.notificationsQueue.peek().timestamp));
        payload.setBody(createJsonFromNotificationsQueue());

        this.notificationsQueue.clear();

        if (this.cloudPublisher != null) {
            try {
                this.cloudPublisher.publish(new KuraMessage(payload));
            } catch (KuraException e) {
                logger.error("Error publishing configuration change event.", e);
            }
        }
    }

}
