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

public class TritonServerLocalManager {

    private static final Logger logger = LoggerFactory.getLogger(TritonServerLocalManager.class);

    private static final int MONITOR_PERIOD = 30;
    private static final String[] TRITONSERVER = new String[] { "tritonserver" };

    private final CommandExecutorService commandExecutorService;
    private final TritonServerServiceOptions options;
    private Command serverCommand;

    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> scheduledFuture;

    protected TritonServerLocalManager(TritonServerServiceOptions options,
            CommandExecutorService commandExecutorService) {
        this.options = options;
        this.commandExecutorService = commandExecutorService;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    protected void start() {
        startLocalServerMonitor();
    }

    protected void stop() {
        stopLocalServerMonitor();
        stopScheduledExecutor();
    }

    private void startLocalServerMonitor() {
        if (this.options.isLocalEnabled()) {
            this.serverCommand = createServerCommand();
            this.scheduledFuture = this.scheduledExecutorService.scheduleAtFixedRate(() -> {
                Thread.currentThread().setName(getClass().getSimpleName());
                if (!isLocalServerRunning()) {
                    startLocalServer();
                }
            }, 0, MONITOR_PERIOD, TimeUnit.SECONDS);
        }
    }

    private void startLocalServer() {
        this.commandExecutorService.execute(this.serverCommand, status -> {
            if (status.getExitStatus().isSuccessful()) {
                logger.info("Nvidia Triton Server started");
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

    private void stopMonitor() {
        if (nonNull(this.scheduledFuture)) {
            this.scheduledFuture.cancel(true);
            while (!this.scheduledFuture.isDone()) {
                sleepFor(500);
            }
        }
    }

    private synchronized void stopLocalServer() {
        TritonServerLocalManager.this.commandExecutorService.kill(TRITONSERVER, LinuxSignal.SIGINT);
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

    private boolean isLocalServerRunning() {
        boolean isRunning = false;
        if (this.commandExecutorService.isRunning(TRITONSERVER)) {
            isRunning = true;
        }
        return isRunning;
    }

    protected static void sleepFor(long timeout) {
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
        commandString.add("--model-repository=" + this.options.getModelRepositoryPath());
        commandString.add("--backend-directory=" + this.options.getBackendsPath());
        if (!this.options.getBackendsConfigs().isEmpty()) {
            this.options.getBackendsConfigs().forEach(config -> commandString.add("--backend-config=" + config));
        }
        commandString.add("--http-port=" + this.options.getHttpPort());
        commandString.add("--grpc-port=" + this.options.getGrpcPort());
        commandString.add("--metrics-port=" + this.options.getMetricsPort());
        commandString.add("--model-control-mode=explicit");
        if (!this.options.getModels().isEmpty()) {
            this.options.getModels().forEach(model -> commandString.add("--load-model=" + model));
        }
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
