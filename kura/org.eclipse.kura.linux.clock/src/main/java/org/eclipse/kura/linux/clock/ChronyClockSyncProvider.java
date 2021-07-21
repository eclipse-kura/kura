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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChronyClockSyncProvider implements ClockSyncProvider {

    private static final String CHRONY_DAEMON = "chronyd";

    private static final Logger logger = LoggerFactory.getLogger(ChronyClockSyncProvider.class);

    private final CommandExecutorService executorService;
    private ScheduledExecutorService schedulerExecutor;

    private Map<String, Object> properties;

    private String chronyConfig;

    private Date lastSyncValue;

    private final Path[] chronyConfigLocations = new Path[] { Paths.get("/etc/chrony.conf"),
            Paths.get("/etc/chrony/chrony.conf") };

    private final CryptoService cryptoService;

    public ChronyClockSyncProvider(CommandExecutorService commandExecutorService, CryptoService cryptoService) {
        this.executorService = commandExecutorService;
        this.cryptoService = cryptoService;
    }

    /**
     * listener is ignored because we do not need to manually update hardware clock
     */

    @Override
    public void init(Map<String, Object> properties, ClockSyncListener listener) throws KuraException {
        this.properties = properties;

        readProperties();
        writeConfiguration();
    }

    private void writeConfiguration() throws KuraException {

        if (this.chronyConfig != null && !this.chronyConfig.isEmpty()) {

            Path chronyConfigLocation = null;

            for (Path loc : this.chronyConfigLocations) {
                if (Files.exists(loc)) {
                    chronyConfigLocation = loc;
                    break;
                }
            }

            if (chronyConfigLocation == null) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID);
            }

            try {
                String chronyConfigLocationContent = new String(Files.readAllBytes(chronyConfigLocation),
                        StandardCharsets.UTF_8);

                if (this.cryptoService.sha256Hash(this.chronyConfig)
                        .equals(this.cryptoService.sha256Hash(chronyConfigLocationContent))) {

                    logger.debug("chrony configuration not changed");
                    return;
                }

                String chronyConfigLocationBackup = chronyConfigLocation.toString() + ".bak";

                logger.info("Saving previous chrony configuration file at {}", chronyConfigLocationBackup);
                Files.copy(chronyConfigLocation, Paths.get(chronyConfigLocationBackup),
                        StandardCopyOption.REPLACE_EXISTING);
                Files.write(chronyConfigLocation, this.chronyConfig.getBytes());
            } catch (IOException e) {
                logger.error("Unable to write chrony configuration file at {}", chronyConfigLocation, e);
            } catch (NoSuchAlgorithmException e) {
                logger.error("Unable to get files hash", e);
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

    }

    protected boolean syncClock() {

        logger.info("Forcing clock syncronization...");

        Command chronycMakeStep = new Command(new String[] { "chronyc", "makestep" });
        CommandStatus chronycMakeStepStatus = this.executorService.execute(chronycMakeStep);

        return chronycMakeStepStatus.getExitStatus().isSuccessful();
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

        Command checkChronyStatus = new Command(new String[] { "systemctl", "is-active", CHRONY_DAEMON });
        CommandStatus chronyStatus = this.executorService.execute(checkChronyStatus);

        return chronyStatus.getExitStatus().isSuccessful();
    }

    private boolean controlChronyd(String command) {
        Command startChronyStatus = new Command(new String[] { "systemctl", command, CHRONY_DAEMON });
        CommandStatus chronyStatus = this.executorService.execute(startChronyStatus);

        return chronyStatus.getExitStatus().isSuccessful();
    }

    private void readProperties() {
        this.chronyConfig = (String) this.properties.get("chrony.advanced.config");
        if (this.chronyConfig == null || this.chronyConfig.isEmpty()) {
            logger.info("No Chrony configuration provided. Using system configuration.");
        }

    }

}
