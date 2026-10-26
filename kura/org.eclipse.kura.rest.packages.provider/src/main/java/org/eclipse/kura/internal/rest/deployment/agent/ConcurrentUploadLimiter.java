/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.deployment.agent;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows at most one deployment package upload to be in progress at any time. Additional requests are rejected with
 * 429 before the request body is read.
 *
 * The slot is taken by the request filter and is held until the {@link DeploymentAgentService} reports that the
 * uploaded package has been processed, so that uploaded files cannot pile up in the installation queue. The slot is
 * released earlier only if the request fails before the installation is queued.
 */
@Provider
@LimitConcurrentUploads
public class ConcurrentUploadLimiter implements ContainerRequestFilter, ContainerResponseFilter, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentUploadLimiter.class);

    private static final String GUARD_PROPERTY = ConcurrentUploadLimiter.class.getName() + ".guard";

    private DeploymentAgentService deploymentAgentService;

    public void setDeploymentAgentService(final DeploymentAgentService deploymentAgentService) {
        this.deploymentAgentService = deploymentAgentService;
    }

    @Override
    public synchronized void filter(final ContainerRequestContext context) throws IOException {
        updateState(State::onDownloadStarted);
        context.setProperty(GUARD_PROPERTY, state.getGuard());
    }

    @Override
    public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
            throws IOException {
        final Optional<Guard> guard = guardFromContext(requestContext);

        if (guard.isPresent()) {
            guard.get().close();
        }
    }

    @Override
    public void handleEvent(final Event event) {
        final Optional<String> eventUrl = Optional
                .ofNullable(event.getProperty(DeploymentAgentService.EVENT_PACKAGE_URL))
                .filter(String.class::isInstance).map(String.class::cast);

        if (eventUrl.isPresent()) {
            updateState(s -> s.onInstallFinished(eventUrl.get()));
        }
    }

    private static void deleteFile(final String url) {
        try {
            Files.deleteIfExists(Paths.get(new URI(url)));
        } catch (final Exception e) {
            logger.warn("Cannot delete uploaded file: {}", url, e);
        }
    }

    private static WebApplicationException tooManyRequests() {
        return new WebApplicationException(
                Response.status(Status.TOO_MANY_REQUESTS).header("Connection", "close").build());
    }

    private State state = new Idle(Optional.empty());

    private synchronized void updateState(final UnaryOperator<State> transitionFunction) {
        this.state = transitionFunction.apply(state);
    }

    private class State {

        private final Guard guard;

        protected State(final Guard guard) {
            this.guard = guard;
        }

        Guard getGuard() {
            return this.guard;
        }

        State onDownloadStarted() {
            throw tooManyRequests();
        }

        State onInstallStarting(String url) {
            return this;
        }

        State onInstallStarted(String url) {
            return this;
        }

        State onRequestCompleted() {
            return new Idle(Optional.empty());
        }

        State onInstallFinished(String url) {
            return this;
        }

        State onFailure() {
            return new Idle(Optional.empty());
        }

    }

    private class Idle extends State {

        Idle(final Optional<String> previousDownloadUrl) {
            super(new Guard());

            if (previousDownloadUrl.isPresent()) {
                deleteFile(previousDownloadUrl.get());
            }
        }

        @Override
        public State onDownloadStarted() {
            return new Downloading(getGuard());
        }

        @Override
        public State onFailure() {
            return this;
        }
    }

    private class Downloading extends State {

        protected Downloading(Guard guard) {
            super(guard);
        }

        @Override
        public State onInstallStarting(String url) {
            return new HasDownloadUrl(url, getGuard());
        }

        @Override
        public State onInstallStarted(String url) {
            return new Installing(url, getGuard());
        }

    }

    private class HasDownloadUrl extends State {

        protected final String url;

        public HasDownloadUrl(final String url, final Guard guard) {
            super(guard);
            this.url = url;
        }

        @Override
        public State onInstallStarted(String url) {
            return new Installing(url, getGuard());
        }

        @Override
        public State onInstallFinished(String url) {
            if (Objects.equals(this.url, url)) {
                return new Idle(Optional.of(url));
            } else {
                return this;
            }
        }

        @Override
        public State onFailure() {
            return onRequestCompleted();
        }

        @Override
        public State onRequestCompleted() {
            return new Idle(Optional.of(this.url));
        }
    }

    private class Installing extends HasDownloadUrl {

        public Installing(final String url, final Guard guard) {
            super(url, guard);
        }

        @Override
        public State onDownloadStarted() {
            if (!deploymentAgentService.isInstallingDeploymentPackage(url)) {
                return new Idle(Optional.of(this.url)).onDownloadStarted();
            } else {
                throw tooManyRequests();
            }
        }

        @Override
        public State onRequestCompleted() {
            return this;
        }
    }

    public static Optional<Guard> guardFromContext(final ContainerRequestContext requestContext) {
        return Optional.ofNullable(requestContext.getProperty(GUARD_PROPERTY)).filter(Guard.class::isInstance)
                .map(Guard.class::cast);
    }

    public class Guard implements AutoCloseable {

        private void updateStateIfCurrent(final UnaryOperator<State> transitionFunction) {

            updateState(s -> {
                if (s.getGuard() != this) {
                    return s;
                }

                return transitionFunction.apply(s);
            });
        }

        public void onInstallStarting(final String url) {
            updateStateIfCurrent(s -> s.onInstallStarting(url));
        }

        public void onInstallStarted(final String url) {
            updateStateIfCurrent(s -> s.onInstallStarted(url));
        }

        public void onFailure() {
            updateStateIfCurrent(State::onFailure);
        }

        @Override
        public void close() {
            updateStateIfCurrent(State::onRequestCompleted);
        }
    }
}
