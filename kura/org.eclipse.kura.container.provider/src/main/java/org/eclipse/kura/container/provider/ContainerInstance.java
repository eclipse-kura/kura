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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.UnaryOperator;

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

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private DockerService dockerService;

    private State state = new Disabled(new ContainerInstanceOptions(Collections.emptyMap()));

    public void setDockerService(final DockerService dockerService) {
        this.dockerService = dockerService;
    }

    public void unsetDockerService(final DockerService dockerService) {
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

        if (newProps.isEnabled()) {
            this.dockerService.registerListener(this);
        } else {
            this.dockerService.unregisterListener(this);
        }

        updateState(s -> s.onConfigurationUpdated(newProps));
    }

    public void deactivate() {
        logger.info("deactivate...");

        updateState(State::onDisabled);

        this.executor.shutdown();
        this.dockerService.unregisterListener(this);

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

    private interface State {

        public default State onConnect() {
            return this;
        }

        public default State onConfigurationUpdated(final ContainerInstanceOptions options) {
            return this;
        }

        public default State onContanierReady(final String containerId) {
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

        private ContainerInstanceOptions options;

        public Disabled(ContainerInstanceOptions options) {
            this.options = options;
        }

        @Override
        public State onConfigurationUpdated(ContainerInstanceOptions options) {
            if (options.isEnabled()) {
                return new Starting(options);
            } else {
                return new Disabled(options);
            }
        }

        @Override
        public State onConnect() {
            if (this.options.isEnabled()) {
                return new Starting(this.options);
            } else {
                return this;
            }
        }

    }

    private class Starting implements State {

        private final ContainerInstanceOptions options;
        private final Future<?> startupFuture;

        public Starting(final ContainerInstanceOptions options) {
            this.options = options;
            this.startupFuture = executor.submit(() -> startMicroservice(options));
        }

        @Override
        public State onConfigurationUpdated(ContainerInstanceOptions newOptions) {
            if (newOptions.equals(this.options)) {
                return this;
            }

            startupFuture.cancel(true);

            if (newOptions.isEnabled()) {
                return new Starting(newOptions);
            } else {
                return new Disabled(newOptions);
            }
        }

        @Override
        public State onContanierReady(final String containerId) {
            return new Created(options, containerId);
        }

        @Override
        public State onStartupFailure() {
            return new Failed(options);
        }

        @Override
        public State onDisabled() {
            startupFuture.cancel(true);
            return new Disabled(this.options);
        }

        private void startMicroservice(final ContainerInstanceOptions options) {
            boolean unlimitedRetries = options.isUnlimitedRetries();
            int maxRetries = options.getMaxDownloadRetries();
            int retryInterval = options.getRetryInterval();

            ContainerDescriptor registeredContainerRefrence = options.getContainerDescriptor();

            int retries = 0;
            while ((unlimitedRetries || retries < maxRetries) && !Thread.currentThread().isInterrupted()) {
                try {
                    logger.info("Tentative number: {}", retries);

                    if (retries > 0) {
                        Thread.sleep(retryInterval);
                    }

                    final String containerId = dockerService.startContainer(registeredContainerRefrence);

                    updateState(s -> s.onContanierReady(containerId));
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

    private class Failed implements State {

        final ContainerInstanceOptions options;

        public Failed(ContainerInstanceOptions options) {
            this.options = options;
        }

        @Override
        public State onConfigurationUpdated(ContainerInstanceOptions newOptions) {
            return new Starting(newOptions);
        }

        @Override
        public State onConnect() {
            return new Starting(options);
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
                dockerService.stopContainer(this.containerId);
                dockerService.deleteContainer(this.containerId);
            } catch (Exception e) {
                logger.error("Error stopping microservice {}", options.getContainerName(), e);
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