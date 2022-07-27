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
 ******************************************************************************/
package org.eclipse.kura.ai.triton.server;

import static java.util.Objects.nonNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TritonServerNativeManager implements TritonServerInstanceManager {

    private static final Logger logger = LoggerFactory.getLogger(TritonServerNativeManager.class);

    private static final int MONITOR_PERIOD = 30;
    private static final String[] TRITONSERVER = new String[] { "tritonserver" };

    private final String decryptionFolderPath;
    private final CommandExecutorService commandExecutorService;
    private final TritonServerServiceOptions options;
    private Command serverCommand;

    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> scheduledFuture;

    protected TritonServerNativeManager(TritonServerServiceOptions options,
            CommandExecutorService commandExecutorService, String decryptionFolderPath) {
        this.options = options;
        this.commandExecutorService = commandExecutorService;
        this.decryptionFolderPath = decryptionFolderPath;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void start() {
        startLocalServerMonitor();
    }

    @Override
    public void stop() {
        stopLocalServerMonitor();
        stopScheduledExecutor();
    }

    @Override
    public void kill() {
        killLocalServerMonitor();
        stopScheduledExecutor();
    }

    private void startLocalServerMonitor() {
        this.serverCommand = createServerCommand();
        this.scheduledFuture = this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            Thread.currentThread().setName(getClass().getSimpleName());
            if (!isServerRunning()) {
                startLocalServer();
            }
        }, 0, MONITOR_PERIOD, TimeUnit.SECONDS);
    }

    private void startLocalServer() {
        this.commandExecutorService.execute(this.serverCommand, status -> {
            if (status.getExitStatus().isSuccessful()) {
                logger.info("Nvidia Triton Server gracefully shutdown");
            } else {
                logger.info("Nvidia Triton Server not started. Exit value: {}", status.getExitStatus().getExitCode());
            }
            if (logger.isDebugEnabled()) {
                logger.debug("execute command {} :: exited with code - {}", this.serverCommand,
                        status.getExitStatus().getExitCode());
                logger.debug("execute stderr {}",
                        new String(((ByteArrayOutputStream) this.serverCommand.getErrorStream()).toByteArray(),
                                StandardCharsets.UTF_8));
                logger.debug("execute stdout {}",
                        new String(((ByteArrayOutputStream) this.serverCommand.getOutputStream()).toByteArray(),
                                StandardCharsets.UTF_8));
            }
        });
    }

    private void stopLocalServerMonitor() {
        stopMonitor();
        stopLocalServer();
    }

    private void killLocalServerMonitor() {
        stopMonitor();
        killLocalServer();
    }

    private void stopMonitor() {
        if (nonNull(this.scheduledFuture)) {
            this.scheduledFuture.cancel(true);
            while (!this.scheduledFuture.isDone()) {
                sleepFor(500);
            }
        }
    }

    private synchronized void stopLocalServer() {
        TritonServerNativeManager.this.commandExecutorService.kill(TRITONSERVER, LinuxSignal.SIGINT);
    }

    private synchronized void killLocalServer() {
        TritonServerNativeManager.this.commandExecutorService.kill(TRITONSERVER, LinuxSignal.SIGKILL);
    }

    private void stopScheduledExecutor() {
        if (this.scheduledExecutorService != null) {
            this.scheduledExecutorService.shutdown();
            while (!this.scheduledExecutorService.isTerminated()) {
                sleepFor(500);
            }
            this.scheduledExecutorService = null;
        }
    }

    @Override
    public boolean isServerRunning() {
        boolean isRunning = false;
        if (this.commandExecutorService.isRunning(TRITONSERVER)) {
            isRunning = true;
        }
        return isRunning;
    }

    private static void sleepFor(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug(e.getMessage(), e);
        }
    }

    private Command createServerCommand() {
        List<String> commandString = new ArrayList<>();
        commandString.add("tritonserver");
        if (this.options.isModelEncryptionPasswordSet()) {
            commandString.add("--model-repository=" + this.decryptionFolderPath);
        } else {
            commandString.add("--model-repository=" + this.options.getModelRepositoryPath());
        }
        commandString.add("--backend-directory=" + this.options.getBackendsPath());
        if (!this.options.getBackendsConfigs().isEmpty()) {
            this.options.getBackendsConfigs().forEach(config -> commandString.add("--backend-config=" + config));
        }
        commandString.add("--http-port=" + this.options.getHttpPort());
        commandString.add("--grpc-port=" + this.options.getGrpcPort());
        commandString.add("--metrics-port=" + this.options.getMetricsPort());
        commandString.add("--model-control-mode=explicit");
        commandString.add("2>&1");
        commandString.add("|");
        commandString.add("systemd-cat");
        commandString.add("-t tritonserver");
        commandString.add("-p info");
        Command command = new Command(commandString.toArray(new String[0]));
        command.setTimeout(-1);
        command.setExecuteInAShell(true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        command.setErrorStream(err);
        command.setOutputStream(out);
        logger.debug("Triton command: {}", commandString);
        return command;
    }

}
