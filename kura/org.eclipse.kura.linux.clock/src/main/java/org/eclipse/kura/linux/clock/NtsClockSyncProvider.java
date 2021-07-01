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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NtsClockSyncProvider implements ClockSyncProvider {

    private final Path chronyConfigLocation = Paths.get("/etc/chrony/crony.conf");

    private static final Logger logger = LoggerFactory.getLogger(NtsClockSyncProvider.class);
    private Map<String, Object> properties;
    private String ntsConfig;

    private CommandExecutorService executorService;

    public NtsClockSyncProvider(CommandExecutorService service) {
        this.executorService = service;
    }

    /**
     * @param listener
     *            not used because hardware clock is synced by cronyd every 11 minutes
     */
    @Override
    public void init(Map<String, Object> properties, ClockSyncListener listener) throws KuraException {
        this.properties = properties;

        readProperties();
        writeConfiguration();
    }

    private void writeConfiguration() {

        try {
            logger.info("Saving previous chrony configuration file at {}", chronyConfigLocation);
            Files.copy(chronyConfigLocation, Paths.get(chronyConfigLocation.toString() + ".bak"));
            Files.write(chronyConfigLocation, ntsConfig.getBytes());
        } catch (IOException e) {
            logger.error("Unable to write chrony configuration file at {}", chronyConfigLocation);
        }
    }

    @Override
    public void start() throws KuraException {

        if (!isChronydRunning()) {
            logger.info("chronyd down, trying to start...");

            boolean startChronyd = controlChronyd("start");
            if (!startChronyd)
                logger.error("Unable to start chronyd.");
        }

        logger.info("chrony deamon is up and running.");

        if (syncClock())
            logger.info("Clock synced");
        else
            logger.info("Clock not synced");

    }

    private boolean syncClock() {

        logger.info("Forcing clock syncronization...");

        Command chronycMakeStep = new Command(new String[] { "chronyc", "makestep" });
        CommandStatus chronycMakeStepStatus = this.executorService.execute(chronycMakeStep);

        return chronycMakeStepStatus.getExitStatus().isSuccessful();
    }

    @Override
    public void stop() throws KuraException {
        logger.info("Stopping chrony daemon...");

        if (controlChronyd("stop"))
            logger.info("chronyd stopped");
        else
            logger.info("Unable to stop chronyd");
    }

    @Override
    public Date getLastSync() {
        return new Date();
    }

    private boolean isChronydRunning() {

        logger.info("Checking chrony deamon status...");

        Command checkChronyStatus = new Command(new String[] { "systemctl", "is-active", "chrony", "--quit" });
        CommandStatus chronyStatus = executorService.execute(checkChronyStatus);

        return chronyStatus.getExitStatus().isSuccessful();
    }

    private boolean controlChronyd(String command) {
        Command startChronyStatus = new Command(new String[] { "systemctl", command, "chrony" });
        CommandStatus chronyStatus = executorService.execute(startChronyStatus);

        return chronyStatus.getExitStatus().isSuccessful();
    }

    private void readProperties() throws KuraException {
        this.ntsConfig = (String) this.properties.get("clock.nts.config");
        if (this.ntsConfig == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, "clock.nts.config");
        }
    }

}
