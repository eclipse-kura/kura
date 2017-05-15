/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.watchdog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.watchdog.CriticalComponent;
import org.eclipse.kura.watchdog.WatchdogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatchdogServiceImpl implements WatchdogService, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(WatchdogServiceImpl.class);

    private static final String PROPERTY_ENABLED = "enabled";
    private static final String PROPERTY_PING_INTERVAL = "pingInterval";
    private static final String PROPERTY_WD_DEVICE = "watchdogDevice";
    private static final String WD_FILE = "/tmp/watchdog";

    private volatile boolean enabled;
    private volatile boolean watchdogToStop;
    private int pingInterval;
    private String watchdogDevice;
    private volatile boolean configEnabled;
    private List<CriticalComponentImpl> criticalComponentList;

    private ScheduledFuture<?> pollTask;
    private ScheduledExecutorService pollExecutor;

    protected void activate(Map<String, Object> properties) {
        this.criticalComponentList = new CopyOnWriteArrayList<>();
        this.enabled = false;
        this.watchdogToStop = false;
        this.pollExecutor = Executors.newSingleThreadScheduledExecutor();

        updated(properties);
    }

    protected void deactivate() {
        cancelPollTask();
        shutdownPollExecutor();
        if (this.configEnabled) {
            refreshWatchdog();
        }
    }

    public void updated(Map<String, Object> properties) {
        readProperties(properties);

        cancelPollTask();

        if (!this.configEnabled) {
            // stop the watchdog
            disableWatchdog();
        } else {
            this.pollTask = this.pollExecutor.scheduleAtFixedRate(() -> {
                Thread.currentThread().setName("WatchdogServiceImpl");
                checkCriticalComponents();
            }, 0, this.pingInterval, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    @Deprecated
    public void startWatchdog() {
    }

    @Override
    @Deprecated
    public void stopWatchdog() {
    }

    @Override
    public int getHardwareTimeout() {
        return 0;
    }

    @Override
    public void registerCriticalComponent(CriticalComponent criticalComponent) {
        final CriticalComponentImpl component = new CriticalComponentImpl(criticalComponent);
        boolean existing = false;
        for (CriticalComponentImpl csi : this.criticalComponentList) {
            if (criticalComponent.equals(csi.getCriticalComponent())) {
                existing = true;
                break;
            }
        }
        if (!existing) {
            this.criticalComponentList.add(component);
        }

        logger.debug(new StringBuilder("Added ").append(criticalComponent.getCriticalComponentName())
                .append(", with timeout = ").append(criticalComponent.getCriticalComponentTimeout())
                .append(", list contains ").append(this.criticalComponentList.size()).append(" critical services")
                .toString());
    }

    @Override
    @Deprecated
    public void registerCriticalService(CriticalComponent criticalComponent) {
        registerCriticalComponent(criticalComponent);
    }

    @Override
    public void unregisterCriticalComponent(CriticalComponent criticalComponent) {
        for (CriticalComponentImpl csi : this.criticalComponentList) {
            if (criticalComponent.equals(csi.getCriticalComponent())) {
                this.criticalComponentList.remove(csi);
                logger.debug(new StringBuffer("Critical service ").append(criticalComponent.getCriticalComponentName())
                        .append(" removed, ").append(System.currentTimeMillis()).toString());
                break;
            }
        }
    }

    @Override
    @Deprecated
    public void unregisterCriticalService(CriticalComponent criticalComponent) {
        unregisterCriticalComponent(criticalComponent);
    }

    @Override
    public List<CriticalComponent> getCriticalComponents() {
        List<CriticalComponent> componentList = new ArrayList<>();
        for (CriticalComponentImpl cci : this.criticalComponentList) {
            componentList.add(cci.getCriticalComponent());
        }
        return Collections.unmodifiableList(componentList);
    }

    @Override
    public void checkin(CriticalComponent criticalService) {
        for (CriticalComponentImpl csi : this.criticalComponentList) {
            if (criticalService.equals(csi.getCriticalComponent())) {
                csi.update();
                break;
            }
        }
    }

    private void checkCriticalComponents() { // is it possible to remove enabled and watchdogToStop?
        // enabled is set if the hw watchdog device is activated
        if (this.enabled) {
            boolean failure = false;
            // Critical Services
            for (CriticalComponentImpl csi : this.criticalComponentList) {
                if (csi.isTimedOut()) {
                    failure = true;
                    logger.warn("Critical service {} failed -> SYSTEM REBOOT", csi.getName());
                    break;
                }
            }

            // refresh watchdog if there aren't failures and the watchdog must not be stopped.
            if (!failure && !this.watchdogToStop) {
                refreshWatchdog();
            } else {
                try {
                    performWatchdogActions();
                } catch (KuraException e) {
                    logger.error("Failed to perform watchdog actions.", e);
                }
            }
        } else {
            if (this.configEnabled) {
                refreshWatchdog();
                this.enabled = true;
            }
        }
    }

    private synchronized void performWatchdogActions() throws KuraException {
        // watchdogToStop is used to avoid multiple action executions and kill Kura after a while
        if (!this.watchdogToStop) {
            this.watchdogToStop = true;
            runCommand("reboot");
        } else {
            refreshWatchdog();
        }
    }

    private void runCommand(String command) throws KuraException {
        try {
            Process proc = Runtime.getRuntime().exec(command);
            proc.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, e, "'" + command + "' failed");
        }
    }

    private void refreshWatchdog() {
        writeWatchdogDevice("w");
    }

    private void disableWatchdog() {
        if (this.enabled) {
            writeWatchdogDevice("V");
            this.enabled = false;
        }
    }

    private synchronized void writeWatchdogDevice(String value) {
        File f = new File(this.watchdogDevice);
        try (FileWriter bw = new FileWriter(f);) {
            bw.write(value);
            logger.debug("write {} on watchdog device", value);
        } catch (IOException e) {
            logger.error("IOException on disable watchdog", e);
        }
    }

    private void readProperties(Map<String, Object> properties) {
        if (properties.get(PROPERTY_PING_INTERVAL) != null) {
            this.pingInterval = (Integer) properties.get(PROPERTY_PING_INTERVAL);
        }
        if (properties.get(PROPERTY_WD_DEVICE) != null && !((String) properties.get(PROPERTY_WD_DEVICE)).isEmpty()) {
            this.watchdogDevice = (String) properties.get(PROPERTY_WD_DEVICE);
        }
        if (properties.get(PROPERTY_ENABLED) != null) {
            this.configEnabled = (Boolean) properties.get(PROPERTY_ENABLED);
        }
        if (this.configEnabled) {
            try (PrintWriter wdWriter = new PrintWriter(WD_FILE);) {
                wdWriter.write(this.watchdogDevice);
            } catch (IOException e) {
                logger.error("Unable to write watchdog config file", e);
            }
            logger.debug("activating WatchdogService with watchdog enabled");
        } else {
            logger.debug("activating WatchdogService with watchdog disabled");
        }
    }

    private void cancelPollTask() {
        if (this.pollTask != null && !this.pollTask.isCancelled()) {
            logger.debug("Cancelling WatchdogServiceImpl task ...");
            this.pollTask.cancel(true);
            logger.info("WatchdogServiceImpl task cancelled? = {}", this.pollTask.isCancelled());
            this.pollTask = null;
        }
    }

    private void shutdownPollExecutor() {
        if (this.pollExecutor != null) {
            logger.debug("Terminating WatchdogServiceImpl executor ...");
            this.pollExecutor.shutdownNow();
            logger.info("WatchdogServiceImpl Thread terminated? - {}", this.pollExecutor.isTerminated());
            this.pollExecutor = null;
        }
    }

}
