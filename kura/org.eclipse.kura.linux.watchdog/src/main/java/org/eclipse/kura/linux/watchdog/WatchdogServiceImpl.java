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

    private volatile boolean isEnabled;
    private volatile boolean watchdogToStop;
    private int pingInterval;
    private String watchdogDevice;
    private volatile boolean configEnabled;   // initialized in properties, if false -> no watchdog
    private List<CriticalComponentImpl> criticalServiceList;

    private ScheduledFuture<?> pollTask;
    private ScheduledExecutorService pollExecutor;

    protected void activate(Map<String, Object> properties) {
        this.criticalServiceList = new CopyOnWriteArrayList<CriticalComponentImpl>();
        this.isEnabled = false;
        this.watchdogToStop = false;
        this.pollExecutor = Executors.newSingleThreadScheduledExecutor();

        updated(properties);
    }

    protected void deactivate() {
        cancelPollTask();
        shutdownPollExecutor();
        refreshWatchdog();
    }

    public void updated(Map<String, Object> properties) {
        readProperties(properties);

        cancelPollTask();

        if (!this.configEnabled) {
            // stop the watchdog
            disableWatchdog();
        } else {
            this.pollTask = this.pollExecutor.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName("WatchdogServiceImpl");
                    checkCriticalComponents();
                }
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
        for (CriticalComponentImpl csi : this.criticalServiceList) {
            if (criticalComponent.equals(csi.getCriticalComponent())) {
                existing = true;
            }
        }
        if (!existing) {
            this.criticalServiceList.add(component);
        }

        logger.debug("Added " + criticalComponent.getCriticalComponentName() + ", with timeout = "
                + criticalComponent.getCriticalComponentTimeout() + ", list contains " + this.criticalServiceList.size()
                + " critical services");
    }

    @Override
    @Deprecated
    public void registerCriticalService(CriticalComponent criticalComponent) {
        registerCriticalComponent(criticalComponent);
    }

    @Override
    public void unregisterCriticalComponent(CriticalComponent criticalComponent) {
        for (CriticalComponentImpl csi : this.criticalServiceList) {
            if (criticalComponent.equals(csi.getCriticalComponent())) {
                this.criticalServiceList.remove(csi);
                logger.debug("Critical service " + criticalComponent.getCriticalComponentName() + " removed, "
                        + System.currentTimeMillis());
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
        List<CriticalComponent> componentList = new ArrayList<CriticalComponent>();
        for (CriticalComponentImpl cci : this.criticalServiceList) {
            componentList.add(cci.getCriticalComponent());
        }
        return componentList;
    }

    @Override
    public void checkin(CriticalComponent criticalService) {
        for (CriticalComponentImpl csi : this.criticalServiceList) {
            if (criticalService.equals(csi.getCriticalComponent())) {
                csi.update();
            }
        }
    }

    private void checkCriticalComponents() {
        // isEnabled is set if the hw watchdog device is activated
        if (this.isEnabled) {
            boolean failure = false;
            // Critical Services
            for (CriticalComponentImpl csi : this.criticalServiceList) {
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
                this.isEnabled = true;
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
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, e, "'" + command + "' failed");
        } catch (InterruptedException e) {
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, e, "'" + command + "' failed");
        }
    }

    private void refreshWatchdog() {
        writeWatchdogDevice("w");
    }

    private void disableWatchdog() {
        if (this.isEnabled) {
            writeWatchdogDevice("V");
            this.isEnabled = false;
        }
    }

    private synchronized void writeWatchdogDevice(String value) {
        File f = null;
        FileWriter bw = null;
        try {
            f = new File(this.watchdogDevice);
            bw = new FileWriter(f);
            bw.write(value);
            logger.debug("write {} on watchdog device", value);
        } catch (IOException e) {
            logger.error("IOException on disable watchdog", e);
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                logger.error("IOException on closing watchdog file", e);
            }
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
            if (this.configEnabled) {
                PrintWriter wdWriter = null;
                try {
                    wdWriter = new PrintWriter(WD_FILE);
                    wdWriter.write(this.watchdogDevice);
                } catch (IOException e) {
                    logger.error("Unable to write watchdog config file", e);
                } finally {
                    if (wdWriter != null)
                        wdWriter.close();
                }
                logger.debug("activating WatchdogService with watchdog enabled");
            } else {
                logger.debug("activating WatchdogService with watchdog disabled");
            }
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
