/*******************************************************************************
 * Copyright (c) 2022, 2024 Eurotech and/or its affiliates and others
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.container.orchestration.ContainerConfiguration;
import org.eclipse.kura.container.orchestration.ContainerConfiguration.ContainerConfigurationBuilder;
import org.eclipse.kura.container.orchestration.ContainerInstanceDescriptor;
import org.eclipse.kura.container.orchestration.ContainerNetworkConfiguration.ContainerNetworkConfigurationBuilder;
import org.eclipse.kura.container.orchestration.ContainerOrchestrationService;
import org.eclipse.kura.container.orchestration.ContainerState;
import org.eclipse.kura.container.orchestration.ImageConfiguration.ImageConfigurationBuilder;
import org.eclipse.kura.container.orchestration.ImageInstanceDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TritonServerContainerManager implements TritonServerInstanceManager {

    private static final Logger logger = LoggerFactory.getLogger(TritonServerContainerManager.class);

    private static final int MONITOR_PERIOD = 30;
    private static final String TRITON_CONTAINER_NAME = "tritonserver-kura";
    private static final String TRITON_LOGGING_TYPE = "DEFAULT";
    private static final String TRITON_INTERNAL_MODEL_REPO = "/models";
    private static final String TRITON_INTERNAL_BACKENDS_FOLDER = "/backends";
    private static final boolean TRITON_FRAMEWORK_MANAGED = true;
    private static final List<Integer> TRITON_INTERNAL_PORTS = Arrays.asList(8000, 8001, 8002);

    private TritonServerServiceOptions options;
    private ContainerOrchestrationService containerOrchestrationService;
    private String decryptionFolderPath;

    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> scheduledFuture;

    protected TritonServerContainerManager(TritonServerServiceOptions options,
            ContainerOrchestrationService containerOrchestrationService, String decryptionFolderPath) {
        this.options = options;
        this.containerOrchestrationService = containerOrchestrationService;
        this.decryptionFolderPath = decryptionFolderPath;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void start() {
        if (!isImageAvailable()) {
            logger.error("Docker image not available on disk. Aborting....");
            return;
        }
        startContainerServerMonitor();
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

    private void startContainerServerMonitor() {
        this.scheduledFuture = this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            Thread.currentThread().setName(getClass().getSimpleName());
            if (!isServerRunning()) {
                startLocalServer();
            }
        }, 0, MONITOR_PERIOD, TimeUnit.SECONDS);
    }

    private void startLocalServer() {
        ContainerConfiguration containerConfiguration = createContainerConfiguration();
        try {
            String containerID = this.containerOrchestrationService.startContainer(containerConfiguration);
            logger.info("Nvidia Triton Container started. Container ID: {}", containerID);
        } catch (InterruptedException e) {
            logger.info("Nvidia Triton Container start interrupted.", e);
            Thread.currentThread().interrupt();
        } catch (KuraException e) {
            logger.info("Nvidia Triton Container not started.", e);
        }
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
        try {
            String containerID = getContainerID();
            this.containerOrchestrationService.stopContainer(containerID);
            this.containerOrchestrationService.deleteContainer(containerID);
        } catch (KuraException e) {
            logger.error("Can't stop container. Caused by", e);
        }
    }

    private synchronized void killLocalServer() {
        stopLocalServer();
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
        try {
            final Optional<ContainerInstanceDescriptor> existingInstance = this.containerOrchestrationService
                    .listContainerDescriptors().stream().filter(c -> c.getContainerName().equals(TRITON_CONTAINER_NAME))
                    .findAny();

            if (!existingInstance.isPresent()) {
                return false;
            }

            ContainerInstanceDescriptor descr = existingInstance.get();
            return descr.getContainerState() == ContainerState.ACTIVE
                    || descr.getContainerState() == ContainerState.STARTING;
        } catch (IllegalStateException e) {
            logger.error("Cannot retrieve container status information", e);
            return false;
        }
    }

    private boolean isImageAvailable() {
        List<ImageInstanceDescriptor> existingImage;

        try {
            existingImage = this.containerOrchestrationService.listImageInstanceDescriptors();
        } catch (IllegalStateException e) {
            logger.error("Cannot retrieve container image status information", e);
            return false;
        }

        for (ImageInstanceDescriptor imageDescriptor : existingImage) {
            if (imageDescriptor.getImageName().equals(this.options.getContainerImage())
                    && imageDescriptor.getImageTag().equals(this.options.getContainerImageTag())) {
                return true;
            }

        }

        return false;
    }

    private String getContainerID() throws KuraException {
        final Optional<ContainerInstanceDescriptor> existingInstance = this.containerOrchestrationService
                .listContainerDescriptors().stream().filter(c -> c.getContainerName().equals(TRITON_CONTAINER_NAME))
                .findAny();

        if (!existingInstance.isPresent()) {
            throw new KuraException(KuraErrorCode.NOT_FOUND, "Can't find Kura-managed Triton server container");
        }

        ContainerInstanceDescriptor descr = existingInstance.get();
        return descr.getContainerId();
    }

    private static void sleepFor(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug(e.getMessage(), e);
        }
    }

    private ContainerConfiguration createContainerConfiguration() {

        ImageConfigurationBuilder imageConfigBuilder = new ImageConfigurationBuilder();
        imageConfigBuilder.setImageName(this.options.getContainerImage());
        imageConfigBuilder.setImageTag(this.options.getContainerImageTag());
        imageConfigBuilder.setRegistryCredentials(Optional.empty());

        ContainerNetworkConfigurationBuilder networkConfigurationBuilder = new ContainerNetworkConfigurationBuilder();
        networkConfigurationBuilder.setNetworkMode(Optional.empty());

        ContainerConfigurationBuilder builder = new ContainerConfigurationBuilder();

        builder.setImageConfiguration(imageConfigBuilder.build());
        builder.setContainerNetowrkConfiguration(networkConfigurationBuilder.build());

        builder.setContainerName(TRITON_CONTAINER_NAME);
        builder.setFrameworkManaged(TRITON_FRAMEWORK_MANAGED);
        builder.setLoggingType(TRITON_LOGGING_TYPE);
        builder.setInternalPorts(TRITON_INTERNAL_PORTS);
        builder.setExternalPorts(
                Arrays.asList(this.options.getHttpPort(), this.options.getGrpcPort(), this.options.getMetricsPort()));

        builder.setMemory(this.options.getContainerMemory());
        builder.setCpus(this.options.getContainerCpus());
        builder.setGpus(this.options.getContainerGpus());
        builder.setRuntime(this.options.getContainerRuntime());
        builder.setDeviceList(this.options.getDevices());

        Map<String, String> volumes = new HashMap<>();
        if (this.options.isModelEncryptionPasswordSet()) {
            volumes.put(this.decryptionFolderPath, TRITON_INTERNAL_MODEL_REPO);
        } else {
            volumes.put(this.options.getModelRepositoryPath(), TRITON_INTERNAL_MODEL_REPO);
        }
        if (!this.options.getBackendsPath().isEmpty()) {
            volumes.put(this.options.getBackendsPath(), TRITON_INTERNAL_BACKENDS_FOLDER);
        }
        builder.setVolumes(Collections.unmodifiableMap(volumes));

        List<String> entrypointOverride = new ArrayList<>();
        entrypointOverride.add("tritonserver");
        entrypointOverride.add("--model-repository=" + TRITON_INTERNAL_MODEL_REPO);
        entrypointOverride.add("--model-control-mode=explicit");
        if (!this.options.getBackendsPath().isEmpty()) {
            entrypointOverride.add("--backend-directory=" + TRITON_INTERNAL_BACKENDS_FOLDER);
        }
        if (!this.options.getBackendsConfigs().isEmpty()) {
            this.options.getBackendsConfigs().forEach(config -> entrypointOverride.add("--backend-config=" + config));
        }
        builder.setEntryPoint(entrypointOverride);

        return builder.build();
    }
}