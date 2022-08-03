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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationChangeManager implements ConfigurableComponent, ServiceTrackerListener {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationChangeManager.class);
    
    private static final String METRIC_CHANGED_PIDS = "changed.pids";
    private static final String METRIC_LAST_SNAPSHOT_ID = "last.snapshot.id";
    private static final String METRIC_NON_PERSISTED_CHANGED_PIDS = "non.persisted.changed.pids";

    private ConfigurationChangeManagerOptions options;
    private ConfigurationService configurationService;
    private CloudPublisher cloudPublisher;

    private ScheduledExecutorService scheduledSendQueueExecutor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> futureSendQueue;
    private volatile boolean acceptNotifications = false;
    private Queue<Notification> notificationsQueue = new LinkedList<>();
    private long lastSnapshotId = 0L;
    private ComponentsServiceTracker serviceTracker;

    private class Notification {

        protected long timestamp;
        protected String changedPid;

        public Notification(String changedPid) {
            this.timestamp = new Date().getTime();
            this.changedPid = changedPid;
        }
    }

    /*
     * Dependencies
     */

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void unsetConfigurationService(ConfigurationService configurationService) {
        if (this.configurationService == configurationService) {
            this.configurationService = null;
        }
    }

    public void setCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher = cloudPublisher;
    }

    public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
        if(this.cloudPublisher == cloudPublisher) {
            this.cloudPublisher = null;
        }
    }

    /*
     * Activation APIs
     */

    protected void activate(final Map<String, Object> properties) throws InvalidSyntaxException {
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
            this.serviceTracker.open(true);
            this.serviceTracker.addServiceTrackerListener(this);
            this.acceptNotifications = true;
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
            this.notificationsQueue.add(new Notification(pid));

            if (this.futureSendQueue != null) {
                this.futureSendQueue.cancel(false);
            }
            this.futureSendQueue = scheduledSendQueueExecutor.schedule(() -> sendQueue(), this.options.getSendDelay(),
                    TimeUnit.SECONDS);
        }
    }

    private void retrieveLastSnapshotId() {
        try {
            this.lastSnapshotId = Collections.max(this.configurationService.getSnapshots());
        } catch (KuraException e) {
            logger.error("Error getting last snapshot ID.", e);
        }
    }

    private String getCsvFromPidsList(List<String> pids) {
        StringBuilder csvModifiedPids = new StringBuilder();
        for (String pid : pids) {
            csvModifiedPids.append(pid);
            csvModifiedPids.append(",");
        }
        
        if(csvModifiedPids.length() > 1) {
            return csvModifiedPids.substring(0, csvModifiedPids.length() - 1);
        } else {
            return csvModifiedPids.toString();
        }
    }

    private List<String> getNonPersistedPidsFrom(List<String> pids) {
        List<String> nonPersistedPids = new ArrayList<>(pids);
        
        try {
            List<ComponentConfiguration> persistedConfigs = this.configurationService.getSnapshot(this.lastSnapshotId);
            List<String> persistedPids = persistedConfigs.stream().map(ComponentConfiguration::getPid).collect(Collectors.toList());

            nonPersistedPids.removeAll(persistedPids);
        } catch (KuraException e) {
            logger.error("Error retrieving configurations from last snapshot.", e);
        }

        return nonPersistedPids;
    }

    private void sendQueue() {
        retrieveLastSnapshotId();
        
        List<String> changedPids = new ArrayList<>();
        for (Notification notification : this.notificationsQueue) {
            changedPids.add(notification.changedPid);
        }

        KuraPayload payload = new KuraPayload();
        payload.setTimestamp(new Date(this.notificationsQueue.peek().timestamp));

        payload.addMetric(METRIC_CHANGED_PIDS, getCsvFromPidsList(changedPids));
        payload.addMetric(METRIC_LAST_SNAPSHOT_ID, this.lastSnapshotId);
        payload.addMetric(METRIC_NON_PERSISTED_CHANGED_PIDS, getCsvFromPidsList(getNonPersistedPidsFrom(changedPids)));

        KuraMessage kuraMessage = new KuraMessage(payload);

        logger.info("\nMessage to send:\n{}\n", kuraMessage.getPayload().metrics());
        
        if(this.cloudPublisher != null) {
            try {
                this.cloudPublisher.publish(kuraMessage);
            } catch(KuraException e) {
                logger.error("Error publishing configuration change event.", e);
            }
        }

        this.notificationsQueue.clear();
    }

}
