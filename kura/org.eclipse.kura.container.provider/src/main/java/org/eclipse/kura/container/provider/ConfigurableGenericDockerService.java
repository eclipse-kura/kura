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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.kura.container.orchestration.provider.ContainerDescriptor;
import org.eclipse.kura.container.orchestration.provider.ContainerStates;
import org.eclipse.kura.container.orchestration.provider.DockerService;

public class ConfigurableGenericDockerService implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurableGenericDockerService.class);
    private static final String APP_ID = "org.eclipse.kura.container.provider.ConfigurableGenericDockerService";
    public static final String DEFAULT_INSTANCE_PID = APP_ID;

    private ConfigurableGenericDockerServiceOptions serviceOptions;
    private ContainerDescriptor registeredContainerRefrence;

    private DockerService dockerService;

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
        ConfigurableGenericDockerServiceOptions newProps = new ConfigurableGenericDockerServiceOptions(properties);

        if (newProps.equals(this.serviceOptions)) {
            return;
        }

        if (!isNull(this.serviceOptions) && this.serviceOptions.isEnabled()) {
            stopRunningMicroservice();
        }

        handleContainerRegistry(newProps);
        this.serviceOptions = newProps;

        if (this.serviceOptions.isEnabled()) {
            startNewMicroservice();
        }

    }

    public void deactivate() {
        logger.info("deactivate...");
        logger.info("cleaning up related container: {}", this.serviceOptions.getContainerName());

        if (this.serviceOptions.isEnabled()) {
            stopRunningMicroservice();
        }

        if (this.registeredContainerRefrence != null) {
            this.dockerService.unregisterContainer(this.registeredContainerRefrence);
        }

        logger.info("deactivate...done");
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    private void startNewMicroservice() {
        if (isNull(this.serviceOptions) || isNull(this.dockerService)) {
            return;
        }
        try {
            this.dockerService.startContainer(this.registeredContainerRefrence);
            this.registeredContainerRefrence.setContainerState(ContainerStates.STARTING);
        } catch (KuraException e) {
            logger.error("Error managing microservice state", e);
        }
    }

    private void stopRunningMicroservice() {
        if (isNull(this.serviceOptions) || isNull(this.dockerService)) {
            return;
        }
        try {
            this.dockerService.stopContainer(this.registeredContainerRefrence);
            this.registeredContainerRefrence.setContainerState(ContainerStates.STOPPING);
        } catch (KuraException e) {
            logger.error("Error stopping microservice {}", this.serviceOptions.getContainerName(), e);
        }
    }

    private void handleContainerRegistry(ConfigurableGenericDockerServiceOptions newProps) {
        if (this.registeredContainerRefrence != null) {
            this.dockerService.unregisterContainer(this.registeredContainerRefrence);
        }

        this.registeredContainerRefrence = newProps.getContainerDescriptor();
        this.dockerService.registerContainer(this.registeredContainerRefrence);
    }
}