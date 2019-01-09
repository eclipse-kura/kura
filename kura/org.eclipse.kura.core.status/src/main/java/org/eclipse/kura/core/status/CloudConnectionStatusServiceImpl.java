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
package org.eclipse.kura.core.status;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.core.status.runnables.BlinkStatusRunnable;
import org.eclipse.kura.core.status.runnables.HeartbeatStatusRunnable;
import org.eclipse.kura.core.status.runnables.LogStatusRunnable;
import org.eclipse.kura.core.status.runnables.OnOffStatusRunnable;
import org.eclipse.kura.core.status.runnables.StatusRunnable;
import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.status.CloudConnectionStatusComponent;
import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.eclipse.kura.status.CloudConnectionStatusService;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudConnectionStatusServiceImpl implements CloudConnectionStatusService {

    private static final String STATUS_NOTIFICATION_URL = "ccs.status.notification.url";

    private static final Logger logger = LoggerFactory.getLogger(CloudConnectionStatusServiceImpl.class);

    private SystemService systemService;
    private GPIOService gpioService;

    private final ExecutorService notificationExecutor;
    private Future<?> notificationWorker;

    private final IdleStatusComponent idleComponent;

    private CloudConnectionStatusEnum currentStatus = null;

    private final HashSet<CloudConnectionStatusComponent> componentRegistry = new HashSet<>();

    private Properties properties;

    private StatusRunnable statusRunnable;

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
        logger.info("Activating CloudConnectionStatus service...");

        String urlFromConfig = this.systemService.getProperties().getProperty(STATUS_NOTIFICATION_URL,
                CloudConnectionStatusURL.CCS + CloudConnectionStatusURL.NONE);

        this.properties = CloudConnectionStatusURL.parseURL(urlFromConfig);

        register(this.idleComponent);
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("Deactivating CloudConnectionStatus service...");

        this.notificationExecutor.shutdownNow();

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

            if (this.statusRunnable != null) {
                this.statusRunnable.stopRunnable();
            }
            if (this.notificationWorker != null) {
                this.notificationWorker.cancel(true);
                this.notificationWorker = null;
            }

            // Avoid NPE if CloudConnectionStatusComponent doesn't initialize its internal status.
            // Defaults to OFF
            this.currentStatus = this.currentStatus == null ? CloudConnectionStatusEnum.OFF : this.currentStatus;

            this.statusRunnable = getRunnable(this.currentStatus);
            this.notificationWorker = this.notificationExecutor.submit(this.statusRunnable);
        }
    }

    private StatusRunnable getRunnable(CloudConnectionStatusEnum status) {
        StatusRunnable runnable = null;

        StatusNotificationTypeEnum notificationType = (StatusNotificationTypeEnum) this.properties
                .get(CloudConnectionStatusURL.NOTIFICATION_TYPE);

        switch (notificationType) {
        case LED:
            if (this.properties.get("linux_led") != null) {
                runnable = getLinuxStatusWorker(status);
            }
            if (runnable == null && this.properties.get("led") != null) {
                runnable = getGpioStatusWorker(status);
            }
            if (runnable == null) {
                runnable = getLogStatusWorker(status);
            }
            break;
        case LOG:
            runnable = getLogStatusWorker(status);
            break;
        default:
            runnable = getNoneStatusWorker();
        }
        return runnable;
    }

    private StatusRunnable getNoneStatusWorker() {
        return new StatusRunnable() {

            @Override
            public void run() {
                /* Empty runnable */ }

            @Override
            public void stopRunnable() {
                /* Empty runnable */
            }
        };
    }

    private StatusRunnable getLogStatusWorker(CloudConnectionStatusEnum status) {
        return new LogStatusRunnable(status);
    }

    private StatusRunnable getLinuxStatusWorker(CloudConnectionStatusEnum status) {
        StatusRunnable runnable = null;

        String ledPath = this.properties.getProperty("linux_led");
        File f = new File(ledPath);
        if (f.exists() && f.isDirectory()) {
            LedManager linuxLedManager = new LinuxLedManager(ledPath);
            runnable = createLedRunnable(status, linuxLedManager);
        }
        return runnable;
    }

    private StatusRunnable getGpioStatusWorker(CloudConnectionStatusEnum status) {
        int gpioLed = (Integer) this.properties.get("led");
        boolean inverted = (Boolean) this.properties.get("inverted");
        LedManager gpioLedManager = new GpioLedManager(this.gpioService, gpioLed, inverted);

        return createLedRunnable(status, gpioLedManager);
    }

    private StatusRunnable createLedRunnable(CloudConnectionStatusEnum status, LedManager linuxLedManager) {
        StatusRunnable runnable;
        switch (status) {
        case ON:
            runnable = new OnOffStatusRunnable(linuxLedManager, true);
            break;
        case OFF:
            runnable = new OnOffStatusRunnable(linuxLedManager, false);
            break;
        case SLOW_BLINKING:
            runnable = new BlinkStatusRunnable(linuxLedManager);
            break;
        case FAST_BLINKING:
            runnable = new BlinkStatusRunnable(linuxLedManager);
            break;
        default:
            runnable = new HeartbeatStatusRunnable(linuxLedManager);
        }
        return runnable;
    }
}
