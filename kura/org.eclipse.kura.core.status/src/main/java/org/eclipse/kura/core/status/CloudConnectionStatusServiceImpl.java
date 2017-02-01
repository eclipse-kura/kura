/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.status;

import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.core.status.runnables.BlinkStatusRunnable;
import org.eclipse.kura.core.status.runnables.HeartbeatStatusRunnable;
import org.eclipse.kura.core.status.runnables.LogStatusRunnable;
import org.eclipse.kura.core.status.runnables.OnOffStatusRunnable;
import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.status.CloudConnectionStatusComponent;
import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.eclipse.kura.status.CloudConnectionStatusService;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudConnectionStatusServiceImpl implements CloudConnectionStatusService {

    private static final String STATUS_NOTIFICATION_URL = "ccs.status.notification.url";

    private static final Logger s_logger = LoggerFactory.getLogger(CloudConnectionStatusServiceImpl.class);

    private SystemService systemService;
    private GPIOService gpioService;

    private KuraGPIOPin notificationLED;

    private final ExecutorService notificationExecutor;
    private Future<?> notificationWorker;

    private final IdleStatusComponent idleComponent;

    private int currentNotificationType;
    private CloudConnectionStatusEnum currentStatus = null;

    private final HashSet<CloudConnectionStatusComponent> componentRegistry = new HashSet<CloudConnectionStatusComponent>();

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------
    public CloudConnectionStatusServiceImpl() {
        super();
        this.notificationExecutor = Executors.newSingleThreadExecutor();
        this.idleComponent = new IdleStatusComponent();
    }

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        this.systemService = null;
    }

    public void setGPIOService(GPIOService gpioService) {
        this.gpioService = gpioService;
    }

    public void unsetGPIOService(GPIOService gpioService) {
        this.gpioService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {
        s_logger.info("Activating CloudConnectionStatus service...");

        String urlFromConfig = this.systemService.getProperties().getProperty(STATUS_NOTIFICATION_URL,
                CloudConnectionStatusURL.S_CCS + CloudConnectionStatusURL.S_NONE);

        Properties props = CloudConnectionStatusURL.parseURL(urlFromConfig);

        try {
            int notificationType = (Integer) props.get("notification_type");

            switch (notificationType) {
            case CloudConnectionStatusURL.TYPE_LED:
                this.currentNotificationType = CloudConnectionStatusURL.TYPE_LED;

                this.notificationLED = this.gpioService.getPinByTerminal((Integer) props.get("led"),
                        KuraGPIODirection.OUTPUT, KuraGPIOMode.OUTPUT_OPEN_DRAIN, KuraGPIOTrigger.NONE);

                this.notificationLED.open();
                s_logger.info("CloudConnectionStatus active on LED {}.", props.get("led"));
                break;
            case CloudConnectionStatusURL.TYPE_LOG:
                this.currentNotificationType = CloudConnectionStatusURL.TYPE_LOG;

                s_logger.info("CloudConnectionStatus active on log.");
                break;
            case CloudConnectionStatusURL.TYPE_NONE:
                this.currentNotificationType = CloudConnectionStatusURL.TYPE_NONE;

                s_logger.info("Cloud Connection Status notification disabled");
                break;
            }
        } catch (Exception ex) {
            s_logger.error("Error activating Cloud Connection Status!");
        }

        register(this.idleComponent);
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.info("Deactivating CloudConnectionStatus service...");

        unregister(this.idleComponent);
    }

    // ----------------------------------------------------------------
    //
    // Cloud Connection Status APIs
    //
    // ----------------------------------------------------------------

    @Override
    public void register(CloudConnectionStatusComponent component) {
        this.componentRegistry.add(component);
        internalUpdateStatus();
    }

    @Override
    public void unregister(CloudConnectionStatusComponent component) {
        this.componentRegistry.remove(component);
        internalUpdateStatus();
    }

    @Override
    public boolean updateStatus(CloudConnectionStatusComponent component, CloudConnectionStatusEnum status) {
        try {
            component.setNotificationStatus(status);
            internalUpdateStatus();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    private void internalUpdateStatus() {

        CloudConnectionStatusComponent maxPriorityComponent = this.idleComponent;

        for (CloudConnectionStatusComponent c : this.componentRegistry) {
            if (c.getNotificationPriority() > maxPriorityComponent.getNotificationPriority()) {
                maxPriorityComponent = c;
            }
        }

        if (this.currentStatus == null || this.currentStatus != maxPriorityComponent.getNotificationStatus()) {
            this.currentStatus = maxPriorityComponent.getNotificationStatus();

            if (this.notificationWorker != null) {
                this.notificationWorker.cancel(true);
                this.notificationWorker = null;
            }

            // Avoid NPE if CloudConnectionStatusComponent doesn't initialize its internal status.
            // Defaults to OFF
            this.currentStatus = this.currentStatus == null ? CloudConnectionStatusEnum.OFF : this.currentStatus;

            this.notificationWorker = this.notificationExecutor.submit(getWorker(this.currentStatus));
        }
    }

    private Runnable getWorker(CloudConnectionStatusEnum status) {
        if (this.currentNotificationType == CloudConnectionStatusURL.TYPE_LED) {
            switch (status) {
            case ON:
                return new OnOffStatusRunnable(this.notificationLED, true);
            case OFF:
                return new OnOffStatusRunnable(this.notificationLED, false);
            case SLOW_BLINKING:
                return new BlinkStatusRunnable(this.notificationLED, CloudConnectionStatusEnum.SLOW_BLINKING_ON_TIME,
                        CloudConnectionStatusEnum.SLOW_BLINKING_OFF_TIME);
            case FAST_BLINKING:
                return new BlinkStatusRunnable(this.notificationLED, CloudConnectionStatusEnum.FAST_BLINKING_ON_TIME,
                        CloudConnectionStatusEnum.FAST_BLINKING_OFF_TIME);
            case HEARTBEAT:
                return new HeartbeatStatusRunnable(this.notificationLED);
            }
        } else if (this.currentNotificationType == CloudConnectionStatusURL.TYPE_LOG) {
            return new LogStatusRunnable(status);
        } else if (this.currentNotificationType == CloudConnectionStatusURL.TYPE_NONE) {
            return new Runnable() {

                @Override
                public void run() {
                    /* Empty runnable */ }
            };
        }

        return new Runnable() {

            @Override
            public void run() {
                s_logger.error("Error getting worker for Cloud Connection Status");
            }
        };
    }
}
