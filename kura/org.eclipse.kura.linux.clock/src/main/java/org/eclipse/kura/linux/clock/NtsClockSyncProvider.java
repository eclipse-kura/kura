/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.clock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class NtsClockSyncProvider implements ClockSyncProvider {

    private static final String NTS_PROVIDER = "chronyd";

    private static final Logger logger = LoggerFactory.getLogger(NtsClockSyncProvider.class);

    private final CommandExecutorService executorService;
    private ScheduledExecutorService schedulerExecutor;

    private Map<String, Object> properties;
    private ClockSyncListener listener;

    private Path chronyConfigLocation;
    private String ntsConfig;

    private Date lastSyncValue;
    private long lastOffset;

    private Gson gson;

    private long lastSyncTime;

    public NtsClockSyncProvider(CommandExecutorService service) {
        this.executorService = service;
    }

    @Override
    public void init(Map<String, Object> properties, ClockSyncListener listener) throws KuraException {
        this.properties = properties;
        this.listener = listener;

        this.gson = new Gson();

        readProperties();
        writeConfiguration();
    }

    private void writeConfiguration() {

        if (this.ntsConfig != null && !this.ntsConfig.isEmpty()) {
            try {
                logger.info("Saving previous chrony configuration file at {}", this.chronyConfigLocation);
                Files.copy(this.chronyConfigLocation, Paths.get(this.chronyConfigLocation.toString() + ".bak"));
                Files.write(this.chronyConfigLocation, this.ntsConfig.getBytes());
            } catch (IOException e) {
                logger.error("Unable to write chrony configuration file at {}", this.chronyConfigLocation);
            }
        }

    }

    @Override
    public void start() throws KuraException {

        if (!isChronydRunning()) {
            logger.info("chronyd down, trying to start...");

            boolean startChronyd = controlChronyd("start");
            if (!startChronyd) {
                logger.error("Unable to start chronyd.");
            }
        }

        logger.info("chrony deamon is up and running.");

        if (syncClock()) {
            logger.info("Clock synced");
        } else {
            logger.info("Clock not synced");
        }

        this.schedulerExecutor = Executors.newSingleThreadScheduledExecutor();

        this.schedulerExecutor.scheduleAtFixedRate(this::readAndUpdateSyncInfo, 0, 60, TimeUnit.SECONDS);

    }

    protected boolean syncClock() {

        logger.info("Forcing clock syncronization...");

        Command chronycMakeStep = new Command(new String[] { "chronyc", "makestep" });
        CommandStatus chronycMakeStepStatus = this.executorService.execute(chronycMakeStep);

        boolean clockSynced = chronycMakeStepStatus.getExitStatus().isSuccessful();

        if (clockSynced) {
            readAndUpdateSyncInfo();
        }

        return clockSynced;
    }

    private void readAndUpdateSyncInfo() {

        logger.info("Starting read the journal for clock updates...");

        // check either chronyd or chrony unit because both are used in journal alternatively
        Command journalClockUpdateRead = new Command(
                new String[] { "journalctl", "-r", "-n", "1", "-u", NTS_PROVIDER, "-u", "chrony", "-b", "-o", "json",
                        "-S", "today", "--output-fields", "MESSAGE", "-g", "'System clock was stepped by'" });

        journalClockUpdateRead.setOutputStream(new ByteArrayOutputStream());
        CommandStatus journalClockUpdateReadStatus = this.executorService.execute(journalClockUpdateRead);
        ByteArrayOutputStream journalClockUpdateReadStatusStream = (ByteArrayOutputStream) journalClockUpdateReadStatus
                .getOutputStream();

        if (journalClockUpdateReadStatus.getExitStatus().isSuccessful()
                && journalClockUpdateReadStatusStream.size() > 0) {

            JournalChronyEntry journalEntry = this.gson.fromJson(
                    new String(journalClockUpdateReadStatusStream.toByteArray(), StandardCharsets.UTF_8),
                    JournalChronyEntry.class);

            logger.info("Journal successfully readed. Last event was at: {}",
                    Instant.EPOCH.plus(journalEntry.getTime(), ChronoUnit.MICROS));

            if (journalEntry.getTime() > this.lastSyncTime) {

                this.lastSyncTime = journalEntry.getTime();
                this.lastSyncValue = new Date();

                this.lastOffset = parseOffset(journalEntry.getMessage());
                this.listener.onClockUpdate(this.lastOffset);
            }
        } else {
            logger.warn("Unable to get system clock status from system journal");
        }

    }

    private long parseOffset(String message) {
        String secondsString = message.split(" ")[5];
        Float microseconds = Float.valueOf(secondsString) * 1_000_000;

        return TimeUnit.SECONDS.convert(microseconds.longValue(), TimeUnit.MICROSECONDS);
    }

    @Override
    public void stop() throws KuraException {
        logger.info("Stopping chrony daemon...");

        if (controlChronyd("stop")) {
            logger.info("chronyd stopped");
        } else {
            logger.warn("Unable to stop chronyd");
        }

        if (this.schedulerExecutor != null) {
            this.schedulerExecutor.shutdown();
            this.schedulerExecutor = null;
        }
    }

    @Override
    public Date getLastSync() {
        return this.lastSyncValue;
    }

    private boolean isChronydRunning() {

        logger.info("Checking chrony deamon status...");

        Command checkChronyStatus = new Command(new String[] { "systemctl", "is-active", NTS_PROVIDER });
        CommandStatus chronyStatus = this.executorService.execute(checkChronyStatus);

        return chronyStatus.getExitStatus().isSuccessful();
    }

    private boolean controlChronyd(String command) {
        Command startChronyStatus = new Command(new String[] { "systemctl", command, NTS_PROVIDER });
        CommandStatus chronyStatus = this.executorService.execute(startChronyStatus);

        return chronyStatus.getExitStatus().isSuccessful();
    }

    private void readProperties() throws KuraException {
        this.ntsConfig = (String) this.properties.get("clock.nts.config");
        if (this.ntsConfig == null || this.ntsConfig.isEmpty()) {
            logger.info("No Chrony configuration provided. Using system configuration.");
        }

        String ntsConfigLocation = (String) this.properties.get("clock.nts.config.location");
        if (ntsConfigLocation == null || ntsConfigLocation.isEmpty()) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING);
        } else {
            this.chronyConfigLocation = Paths.get(ntsConfigLocation);
        }
    }

    private class JournalChronyEntry {

        @SerializedName("__REALTIME_TIMESTAMP")
        private final long time;
        @SerializedName("MESSAGE")
        private final String message;

        @SuppressWarnings("unused")
        public JournalChronyEntry(long time, String message) {
            super();
            this.time = time;
            this.message = message;
        }

        public long getTime() {
            return this.time;
        }

        public String getMessage() {
            return this.message;
        }

    }

}
