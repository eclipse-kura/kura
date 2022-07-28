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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.UnaryOperator;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.container.orchestration.ContainerConfiguration;
import org.eclipse.kura.container.orchestration.ContainerInstanceDescriptor;
import org.eclipse.kura.container.orchestration.ContainerOrchestrationService;
import org.eclipse.kura.container.orchestration.listener.ContainerOrchestrationServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerInstance implements ConfigurableComponent, ContainerOrchestrationServiceListener {

    private static final Logger logger = LoggerFactory.getLogger(ContainerInstance.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ContainerOrchestrationService containerOrchestrationService;

    private State state = new Disabled(new ContainerInstanceOptions(Collections.emptyMap()));

    public void setContainerOrchestrationService(final ContainerOrchestrationService containerOrchestrationService) {
        this.containerOrchestrationService = containerOrchestrationService;
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

        if (newProps.isEnabled()) {
            this.containerOrchestrationService.registerListener(this);
        } else {
            this.containerOrchestrationService.unregisterListener(this);
        }

        updateState(s -> s.onConfigurationUpdated(newProps));
    }

    public void deactivate() {
        logger.info("deactivate...");

        updateState(State::onDisabled);

        this.executor.shutdown();
        this.containerOrchestrationService.unregisterListener(this);

        logger.info("deactivate...done");
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    @Override
    public void onConnect() {
        updateState(State::onConnect);
    }

    @Override
    public void onDisconnect() {
        //
    }

    @Override
    public void onDisabled() {
        updateState(State::onDisabled);
    }

    private synchronized void updateState(final UnaryOperator<State> update) {
        final State previous = this.state;
        final State newState = update.apply(previous);
        logger.info("State update: {} -> {}", previous.getClass().getSimpleName(), newState.getClass().getSimpleName());

        this.state = newState;
    }

    private Optional<ContainerInstanceDescriptor> getExistingContainer(final String containerName) {
        return containerOrchestrationService.listContainerDescriptors().stream()
                .filter(c -> c.getContainerName().equals(containerName)).findAny();
    }

    private interface State {

        public default State onConnect() {
            return this;
        }

        public default State onConfigurationUpdated(final ContainerInstanceOptions options) {
            return this;
        }

        public default State onContainerReady(final String containerId) {
            return this;
        }

        public default State onStartupFailure() {
            return this;
        }

        public default State onDisabled() {
            return this;
        }

    }

    private class Disabled implements State {

        private final ContainerInstanceOptions options;

        public Disabled(ContainerInstanceOptions options) {
            this.options = options;
        }

        @Override
        public State onConfigurationUpdated(ContainerInstanceOptions options) {
            return updateStateInternal(options);
        }

        @Override
        public State onConnect() {
            return updateStateInternal(this.options);
        }

        private State updateStateInternal(ContainerInstanceOptions newOptions) {
            if (!newOptions.isEnabled()) {
                return new Disabled(newOptions);
            }

            final Optional<String> existingContainerId;

            try {
                existingContainerId = getExistingContainer(newOptions.getContainerConfiguration().getContainerName())
                        .map(ContainerInstanceDescriptor::getContainerName);
            } catch (final Exception e) {
                logger.warn("failed to get existing container state", e);
                return new Disabled(newOptions);
            }

            if (existingContainerId.isPresent()) {
                logger.info("found existing container with name {}",
                        newOptions.getContainerConfiguration().getContainerName());

                if (newOptions.isEnabled()) {
                    return new Starting(newOptions);
                } else {
                    return new Created(newOptions, existingContainerId.get()).onDisabled();
                }
            } else {
                return new Starting(newOptions);
            }

        }

    }

    private class Starting implements State {

        private final ContainerInstanceOptions options;
        private final Future<?> startupFuture;

        public Starting(final ContainerInstanceOptions options) {
            this.options = options;
            this.startupFuture = ContainerInstance.this.executor.submit(() -> startMicroservice(options));
        }

        @Override
        public State onConfigurationUpdated(ContainerInstanceOptions newOptions) {
            if (newOptions.equals(this.options)) {
                return this;
            }

            this.startupFuture.cancel(true);

            if (newOptions.isEnabled()) {
                return new Starting(newOptions);
            } else {
                return new Disabled(newOptions);
            }
        }

        @Override
        public State onContainerReady(final String containerId) {
            return new Created(this.options, containerId);
        }

        @Override
        public State onStartupFailure() {
            return new Disabled(this.options);
        }

        @Override
        public State onDisabled() {
            this.startupFuture.cancel(true);

            try {
                final Optional<ContainerInstanceDescriptor> existingInstance = getExistingContainer(
                        this.options.getContainerName());

                if (existingInstance.isPresent()) {
                    return new Created(this.options, existingInstance.get().getContainerId()).onDisabled();
                }
            } catch (final Exception e) {
                logger.warn("failed to check container state", e);
            }

            return new Disabled(this.options);
        }

        private void startMicroservice(final ContainerInstanceOptions options) {
            boolean unlimitedRetries = options.isUnlimitedRetries();
            int maxRetries = options.getMaxDownloadRetries();
            int retryInterval = options.getRetryInterval();

            final ContainerConfiguration containerConfiguration = options.getContainerConfiguration();

            int retries = 0;
            while ((unlimitedRetries || retries < maxRetries) && !Thread.currentThread().isInterrupted()) {
                try {
                    logger.info("Tentative number: {}", retries);

                    if (retries > 0) {
                        Thread.sleep(retryInterval);
                    }

                    final String containerId = ContainerInstance.this.containerOrchestrationService
                            .startContainer(containerConfiguration);
                    updateState(s -> s.onContainerReady(containerId));

                    return;

                } catch (InterruptedException e) {
                    logger.info("interrupted exiting");
                    Thread.currentThread().interrupt();
                    return;
                } catch (KuraException e) {
                    logger.error("Error managing microservice state", e);
                    if (!unlimitedRetries) {
                        retries++;
                    }
                }
            }

            updateState(State::onStartupFailure);

            logger.warn("Unable to start microservice...giving up");
        }

    }

    private class Created implements State {

        private final ContainerInstanceOptions options;
        private final String containerId;

        public Created(ContainerInstanceOptions options, String containerId) {
            this.options = options;
            this.containerId = containerId;
        }

        private void deleteContainer() {
            try {
                ContainerInstance.this.containerOrchestrationService.stopContainer(this.containerId);
            } catch (Exception e) {
                logger.error("Error stopping microservice {}", this.options.getContainerName(), e);
            }
            
            try {
                ContainerInstance.this.containerOrchestrationService.deleteContainer(this.containerId);
            } catch (Exception e) {
                logger.error("Error deleting microservice {}", this.options.getContainerName(), e);
            }
        }

        @Override
        public State onConfigurationUpdated(ContainerInstanceOptions options) {
            if (options.equals(this.options)) {
                return this;
            }

            deleteContainer();

            if (options.isEnabled()) {
                return new Starting(options);
            } else {
                return new Disabled(this.options);
            }
        }

        @Override
        public State onDisabled() {
            deleteContainer();
            return new Disabled(this.options);
        }

    }

}