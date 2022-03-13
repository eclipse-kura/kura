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
  *******************************************************************************/

package org.eclipse.kura.container.provider;

import static java.util.Objects.isNull;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.container.orchestration.ContainerDescriptor;
import org.eclipse.kura.container.orchestration.DockerService;
import org.eclipse.kura.container.orchestration.listener.DockerServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerInstance implements ConfigurableComponent, DockerServiceListener {

    private static final Logger logger = LoggerFactory.getLogger(ContainerInstance.class);
    private static final String APP_ID = "org.eclipse.kura.container.provider.ConfigurableGenericDockerService";
    public static final String DEFAULT_INSTANCE_PID = APP_ID;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private ContainerInstanceOptions serviceOptions;

    private DockerService dockerService;

    private String containerId;

    public void setDockerService(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    public void unsetDockerService(DockerService dockerService) {
        if (this.dockerService == dockerService) {
            this.dockerService = null;
        }
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    public void activate(final Map<String, Object> properties) {
        logger.info("activating...");
        updated(properties);

        logger.info("activating...done");
    }

    public void updated(Map<String, Object> properties) {
        ContainerInstanceOptions newProps = new ContainerInstanceOptions(properties);

        if (newProps.equals(this.serviceOptions)) {
            return;
        }

        this.serviceOptions = newProps;

        if (this.serviceOptions.isEnabled()) {
            this.dockerService.registerListener(this, this.serviceOptions.getContainerName());
            this.executor.schedule(this::startMicroservice, 0, TimeUnit.SECONDS);
        } else {
            if (!isNull(this.containerId)) {
                stopRunningMicroservice();
            }
            this.dockerService.unregisterListener(this);
        }

    }

    public void deactivate() {
        logger.info("deactivate...");
        logger.info("cleaning up related container: {}", this.serviceOptions.getContainerName());

        this.executor.shutdown();

        if (this.serviceOptions.isEnabled()) {
            stopRunningMicroservice();
        }
        this.dockerService.unregisterListener(this);

        logger.info("deactivate...done");
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    private void startMicroservice() {
        boolean unlimitedRetries = this.serviceOptions.isUnlimitedRetries();
        int maxRetries = this.serviceOptions.getMaxDownloadRetries();
        int retryInterval = this.serviceOptions.getRetryInterval();

        ContainerDescriptor registeredContainerRefrence = this.serviceOptions.getContainerDescriptor();

        int retries = 0;
        while (unlimitedRetries || retries < maxRetries) {
            logger.info("Tentative number: {}", retries);
            try {
                this.containerId = this.dockerService.startContainer(registeredContainerRefrence);
                return;
            } catch (KuraException e) {
                logger.error("Error managing microservice state", e);
            }

            if (!unlimitedRetries) {
                retries++;
            }

            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }

        logger.warn("Unable to start microservice...giving up");
    }

    private void stopRunningMicroservice() {
        try {
            this.dockerService.stopContainer(this.containerId);
            this.dockerService.deleteContainer(this.containerId);
        } catch (Exception e) {
            logger.error("Error stopping microservice {}", this.serviceOptions.getContainerName(), e);
        }
    }

    @Override
    public void onConnect() {
        // stopRunningMicroservice();
        startMicroservice();
    }

    @Override
    public void onDisconnect() {
        // stopRunningMicroservice();

    }

    @Override
    public void onDisabled() {
        stopRunningMicroservice();
    }
}