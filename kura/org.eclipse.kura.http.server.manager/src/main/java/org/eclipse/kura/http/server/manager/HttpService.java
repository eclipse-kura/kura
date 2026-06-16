/*******************************************************************************
 * Copyright (c) 2019, 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.http.server.manager;

import java.util.EventListener;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.security.keystore.KeystoreChangedEvent;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServlet;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
@Component(
    name = "org.eclipse.kura.http.server.manager.HttpService",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    service = { org.eclipse.kura.configuration.ConfigurableComponent.class, org.osgi.service.event.EventHandler.class },
    property = {
        "kura.ui.service.hide:Boolean=true",
        "event.topics=org/eclipse/kura/security/keystore/KeystoreChangedEvent/KEYSTORE_CHANGED" })
@Designate(ocd = HttpServiceMetatype.class)
public class HttpService implements ConfigurableComponent, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpService.class);

    private HttpServiceOptions options;

    private KeystoreService keystoreService;
    private HttpServlet dispatcherServlet;
    private EventListener eventListener;

    private String keystoreServicePid;

    private JettyServerHolder jettyServerHolder;
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private Future<?> restartTask = CompletableFuture.completedFuture(null);

    @Reference(name = "KeystoreService",
            service = org.eclipse.kura.security.keystore.KeystoreService.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policyOption = ReferencePolicyOption.GREEDY,
            unbind = "-")
    public void setKeystoreService(KeystoreService keystoreService, final Map<String, Object> properties) {
        this.keystoreService = keystoreService;
        this.keystoreServicePid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
    }

    @Reference(name = "HttpServlet", service = jakarta.servlet.http.HttpServlet.class, target = "(http.felix.dispatcher=*)", unbind = "-")
    public void setDispatcherServlet(HttpServlet dispatcherServlet) {
        this.dispatcherServlet = dispatcherServlet;
    }

    @Reference(name = "EventListener", service = java.util.EventListener.class, target = "(http.felix.dispatcher=*)", unbind = "-")
    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    @Activate
    public void activate(Map<String, Object> properties) {
        logger.info("Activating {}", this.getClass().getSimpleName());

        this.options = new HttpServiceOptions(properties);

        startHttpService(this.options);

        logger.info("Activating... Done.");
    }

    @Modified
    public synchronized void updated(Map<String, Object> properties) {
        logger.info("Updating {}", this.getClass().getSimpleName());

        HttpServiceOptions updatedOptions = new HttpServiceOptions(properties);

        if (!this.options.equals(updatedOptions)) {
            logger.debug("Updating, new props");
            this.options = updatedOptions;

            cancelRestartTask();
            restartHttpService();
        }

        logger.info("Updating... Done.");
    }

    @Deactivate
    public synchronized void deactivate() {
        logger.info("Deactivating {}", this.getClass().getSimpleName());

        cancelRestartTask();
        stopHttpService();
        shutdownExecutor();
    }

    private synchronized void restartHttpService() {
        stopHttpService();
        startHttpService(this.options);
    }

    private synchronized void startHttpService(final HttpServiceOptions options) {
        this.executorService.submit(() -> {
            try {
                logger.info("starting Jetty instance...");
                this.jettyServerHolder = new JettyServerHolder(options, Optional.ofNullable(this.keystoreService),
                        this.dispatcherServlet, this.eventListener);
                logger.info("starting Jetty instance...done");
            } catch (final Exception e) {
                logger.error("Could not start Jetty Web server", e);
            }
        });
    }

    private synchronized void stopHttpService() {
        this.executorService.submit(() -> {
            try {
                logger.info("stopping Jetty instance...");
                if (this.jettyServerHolder != null) {
                    this.jettyServerHolder.stop();
                }
            } catch (final Exception e) {
                logger.error("Could not stop Jetty Web server", e);
            }
        });
    }

    private synchronized void shutdownExecutor() {
        try {
            this.executorService.shutdown();
            this.executorService.awaitTermination(30, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Could not stop executor", e);
        } finally {
            this.executorService.shutdownNow();
        }
    }

    private synchronized void cancelRestartTask() {
        if (!this.restartTask.isDone()) {
            this.restartTask.cancel(false);
        }
    }

    private synchronized void scheduleDeferredRestart() {
        cancelRestartTask();

        try {
            this.restartTask = this.executorService.schedule(this::restartHttpService, 10, TimeUnit.SECONDS);
        } catch (final Exception e) {
            logger.warn("failed to schedule restart task", e);
        }
    }

    @Override
    public void handleEvent(final Event event) {
        if (!(event instanceof KeystoreChangedEvent)) {
            return;
        }

        final KeystoreChangedEvent keystoreChangedEvent = (KeystoreChangedEvent) event;

        if (keystoreChangedEvent.getSenderPid().equals(keystoreServicePid)) {
            scheduleDeferredRestart();
        }
    }

}
