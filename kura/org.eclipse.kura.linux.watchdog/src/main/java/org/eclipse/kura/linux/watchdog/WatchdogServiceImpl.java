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
import java.io.Writer;
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

    private volatile boolean enabled;
    private volatile boolean watchdogToStop;
    private List<CriticalComponentImpl> criticalComponentList;

    private ScheduledFuture<?> pollTask;
    private ScheduledExecutorService pollExecutor;

    private RebootCauseFileWriter rebootCauseWriter;
    private WatchdogServiceOptions options;

    private String currentWatchdogDevice;
    private Writer watchdogFileWriter;

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
        if (this.options.isEnabled()) {
            refreshWatchdog();
        }
        closeWatchdogFileWriter();
    }

    public void updated(Map<String, Object> properties) {
        cancelPollTask();

        this.options = new WatchdogServiceOptions(properties);

        if (this.options.isEnabled()) {
            this.rebootCauseWriter = new RebootCauseFileWriter(this.options.getRebootCauseFilePath());

            try (PrintWriter wdWriter = new PrintWriter(this.options.getWatchdogEnabledTemporaryFilePath())) {
                wdWriter.write(this.options.getWatchdogDevice());
            } catch (IOException e) {
                logger.error("Unable to write watchdog enabled temporary file", e);
            }

            this.pollTask = this.pollExecutor.scheduleAtFixedRate(() -> {
                Thread.currentThread().setName("WatchdogServiceImpl");
                checkCriticalComponents();
            }, 0, this.options.getPingInterval(), TimeUnit.MILLISECONDS);
        } else {
            // stop the watchdog
            disableWatchdog();
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

    protected void checkCriticalComponents() { // is it possible to remove enabled and watchdogToStop?
        // enabled is set if the hw watchdog device is activated
        if (this.enabled) {
            boolean failure = false;
            // Critical Services
            for (CriticalComponentImpl csi : this.criticalComponentList) {
                if (csi.isTimedOut()) {
                    failure = true;
                    this.rebootCauseWriter.writeRebootCause("failure in " + csi.getName());
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
            if (this.options.isEnabled()) {
                refreshWatchdog();
                this.enabled = true;
            }
        }
    }

    private synchronized void performWatchdogActions() throws KuraException {
        // watchdogToStop is used to avoid multiple action executions and kill Kura after a while
        if (!this.watchdogToStop) {
            this.watchdogToStop = true;
            runCommand("sync");
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
            closeWatchdogFileWriter();
            this.enabled = false;
        }
    }

    protected Writer getWatchdogFileWriter() throws IOException {
        final String watchdogDeviceFilePath = this.options.getWatchdogDevice();
        if (this.watchdogFileWriter == null || !watchdogDeviceFilePath.equals(currentWatchdogDevice)) {

            closeWatchdogFileWriter();

            File watchdogFile = new File(watchdogDeviceFilePath);

            if (!watchdogFile.exists()) {
                throw new IOException("watchdog device file does not exist: " + watchdogDeviceFilePath);
            }

            this.watchdogFileWriter = new FileWriter(watchdogFile);
            this.currentWatchdogDevice = watchdogDeviceFilePath;
        }
        return this.watchdogFileWriter;
    }

    private void closeWatchdogFileWriter() {
        if (this.watchdogFileWriter != null) {
            try {
                this.watchdogFileWriter.close();
                this.watchdogFileWriter = null;
                this.currentWatchdogDevice = null;
            } catch (IOException e) {
                logger.warn("failed to close watchdog file writer", e);
            }
        }
    }

    private synchronized void writeWatchdogDevice(String value) {
        try {
            final Writer writer = getWatchdogFileWriter();
            writer.write(value);
            writer.flush();
            logger.debug("write {} on watchdog device", value);
        } catch (IOException e) {
            logger.error("IOException on watchdog write", e);
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
