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
package org.eclipse.kura.linux.watchdog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.Duration;
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

    private static final String[] STOP_WATCHDOGD_COMMANDS = { "systemctl stop watchdog", "service watchdog stop",
            "/etc/init.d/watchdog stop", "/etc/init.d/watchdog.sh stop" };

    private static final Logger logger = LoggerFactory.getLogger(WatchdogServiceImpl.class);

    private static final long GRACE_PERIOD = Duration.ofMinutes(5).toNanos();

    private Long timedOutOn;
    private List<CriticalComponentRegistration> criticalComponentRegistrations;
    private ScheduledExecutorService pollExecutor;
    private ScheduledFuture<?> pollTask;
    private Writer watchdogFileWriter;
    private WatchdogServiceOptions options;

    protected void activate(Map<String, Object> properties) {
        this.criticalComponentRegistrations = new CopyOnWriteArrayList<>();
        this.pollExecutor = Executors.newSingleThreadScheduledExecutor();

        updated(properties);
    }

    protected void deactivate() {
        cancelPollTask();
        shutdownPollExecutor();
        if (this.watchdogFileWriter != null) {
            refreshWatchdog();
            closeWatchdogFileWriter();
        }
    }

    public void updated(Map<String, Object> properties) {
        WatchdogServiceOptions newOptions = new WatchdogServiceOptions(properties);

        this.pollExecutor.submit(() -> {
            Thread.currentThread().setName("WatchdogServiceImpl");
            cancelPollTask();
            if (this.watchdogFileWriter != null) {
                disableWatchdog();
            }
            doUpdate(newOptions);
        });
    }

    private void stopWatchdogd() {
        for (final String stopCommand : STOP_WATCHDOGD_COMMANDS) {
            try {
                runCommand(stopCommand);
                Thread.sleep(5000);
                return;
            } catch (Exception e) {
                logger.debug("Command failed: {}", stopCommand, e);
            }
        }
    }

    private void openWatchdog(final String watchdogDevice, final boolean tryStopWatchdogd) throws IOException {
        try {
            this.watchdogFileWriter = getWatchdogDeviceWriter(watchdogDevice);
        } catch (IOException e) {
            if (tryStopWatchdogd) {
                stopWatchdogd();
                openWatchdog(watchdogDevice, false);
            } else {
                throw e;
            }
        }
    }

    private void doUpdate(WatchdogServiceOptions newOptions) {
        if (!newOptions.isEnabled()) {
            return;
        }

        this.timedOutOn = null;
        this.watchdogFileWriter = null;

        String watchdogDevice = newOptions.getWatchdogDevice();

        if (!isWatchdogDeviceAvailable(watchdogDevice)) {
            logger.error("Watchdog device '{}' does not exist", watchdogDevice);
            return;
        }

        try {
            openWatchdog(watchdogDevice, true);
        } catch (IOException e) {
            logger.error("Failed to open watchdog device", e);
            return;
        }

        try (PrintWriter wdWriter = new PrintWriter(newOptions.getWatchdogEnabledTemporaryFilePath())) {
            wdWriter.write(watchdogDevice);
        } catch (IOException e) {
            logger.warn("Unable to write watchdog enabled temporary file. Continuing anyway", e);
        }

        this.options = newOptions;

        this.pollTask = this.pollExecutor.scheduleAtFixedRate(() -> {
            Thread.currentThread().setName("WatchdogServiceImpl");
            checkCriticalComponents();
        }, 0, this.options.getPingInterval(), TimeUnit.MILLISECONDS);
    }

    protected Writer getWatchdogDeviceWriter(String watchdogDevice) throws IOException {
        return new FileWriter(new File(watchdogDevice));
    }

    protected boolean isWatchdogDeviceAvailable(String watchdogDevice) {
        return new File(watchdogDevice).exists();
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
        boolean found = this.criticalComponentRegistrations.stream()
                .anyMatch(ccr -> ccr.getCriticalComponent() == criticalComponent);
        if (found) {
            logger.warn("Critical component '{}' already registered", criticalComponent.getCriticalComponentName());
            return;
        }
        CriticalComponentRegistration ccr = new CriticalComponentRegistration(criticalComponent);
        this.criticalComponentRegistrations.add(ccr);
    }

    @Override
    @Deprecated
    public void registerCriticalService(CriticalComponent criticalComponent) {
        registerCriticalComponent(criticalComponent);
    }

    @Override
    public void unregisterCriticalComponent(CriticalComponent criticalComponent) {
        this.criticalComponentRegistrations.removeIf(ccr -> ccr.getCriticalComponent() == criticalComponent);
    }

    @Override
    @Deprecated
    public void unregisterCriticalService(CriticalComponent criticalComponent) {
        unregisterCriticalComponent(criticalComponent);
    }

    @Override
    public List<CriticalComponent> getCriticalComponents() {
        List<CriticalComponent> result = new ArrayList<>();
        for (CriticalComponentRegistration ccr : this.criticalComponentRegistrations) {
            result.add(ccr.getCriticalComponent());
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public void checkin(CriticalComponent criticalComponent) {
        for (CriticalComponentRegistration ccr : this.criticalComponentRegistrations) {
            if (ccr.getCriticalComponent() == criticalComponent) {
                ccr.update();
                break;
            }
        }
    }

    protected void checkCriticalComponents() {
        logger.debug("Starting critical components check...");
        if (this.timedOutOn == null) {
            CriticalComponentRegistration ccr = getAnyTimedOutRegistration();
            if (ccr != null) {
                this.timedOutOn = System.nanoTime();
                logger.warn("Critical component '{}' timed out. System will reboot", ccr.getCriticalComponentName());

                RebootCauseFileWriter rebootCauseWriter = new RebootCauseFileWriter(
                        this.options.getRebootCauseFilePath());
                rebootCauseWriter.writeRebootCause(ccr.getCriticalComponentName());

                try {
                    logger.debug("Requesting debug.");
                    rebootSystem();
                } catch (KuraException e) {
                    logger.error("System reboot failed. Watchdog will not be refreshed", e);
                    this.timedOutOn -= GRACE_PERIOD;
                }
            }
        }

        if (this.timedOutOn == null || (System.nanoTime()) - this.timedOutOn < GRACE_PERIOD) {
            logger.debug("Refreshing watchdog.");
            refreshWatchdog();
        }
    }

    private CriticalComponentRegistration getAnyTimedOutRegistration() {
        CriticalComponentRegistration result = null;

        for (CriticalComponentRegistration ccr : this.criticalComponentRegistrations) {
            if (ccr.isTimedOut()) {
                result = ccr;
                break;
            }
        }

        return result;
    }

    private synchronized void rebootSystem() throws KuraException {
        try {
            runCommand("sync");
        } catch (KuraException e) {
            logger.error("Filesystem sync failed. Continuing", e);
        }

        runCommand("reboot");
    }

    private void runCommand(String command) throws KuraException {
        try {
            int exitCode = 0;
            Process proc = Runtime.getRuntime().exec(command);
            exitCode = proc.waitFor();
            if (exitCode != 0) {
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, command, exitCode);
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }
    }

    private void refreshWatchdog() {
        try {
            writeWatchdogDevice("w");
        } catch (IOException e) {
            logger.error("Failed to refresh watchdog device '{}'", this.options.getWatchdogDevice(), e);
        }
    }

    private void disableWatchdog() {
        try {
            writeWatchdogDevice("V");
        } catch (IOException e) {
            logger.error("Failed to write magic character to watchdog device '{}'", this.options.getWatchdogDevice(),
                    e);
        }
        closeWatchdogFileWriter();
    }

    private void closeWatchdogFileWriter() {
        try {
            this.watchdogFileWriter.close();
            this.watchdogFileWriter = null;
        } catch (IOException e) {
            logger.error("Failed to close watchdog device '{}'", this.options.getWatchdogDevice(), e);
        }
    }

    private synchronized void writeWatchdogDevice(String value) throws IOException {
        this.watchdogFileWriter.write(value);
        this.watchdogFileWriter.flush();
    }

    private void cancelPollTask() {
        if (this.pollTask != null && !this.pollTask.isCancelled()) {
            logger.debug("Cancelling watchdog task...");
            this.pollTask.cancel(false);
            logger.debug("Watchdog task cancelled? = {}", this.pollTask.isCancelled());
            this.pollTask = null;
        }
    }

    private void shutdownPollExecutor() {
        if (this.pollExecutor != null) {
            logger.debug("Terminating watchdog executor...");
            this.pollExecutor.shutdown();
            logger.debug("Watchdog executor terminated? - {}", this.pollExecutor.isTerminated());
            this.pollExecutor = null;
        }
    }
}
