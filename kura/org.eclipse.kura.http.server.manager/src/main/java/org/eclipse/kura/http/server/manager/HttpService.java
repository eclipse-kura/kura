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

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.security.keystore.KeystoreChangedEvent;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServlet;

public class HttpService implements ConfigurableComponent, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpService.class);

    private HttpServiceOptions options;

    private SystemService systemService;
    private KeystoreService keystoreService;
    private HttpServlet dispatcherServlet;
    private EventListener eventListener;

    private String keystoreServicePid;

    private JettyServerHolder jettyServerHolder;

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void setKeystoreService(KeystoreService keystoreService, final Map<String, Object> properties) {
        this.keystoreService = keystoreService;
        this.keystoreServicePid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
    }

    public void setDispatcherServlet(HttpServlet dispatcherServlet) {
        this.dispatcherServlet = dispatcherServlet;
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void activate(Map<String, Object> properties) {
        logger.info("Activating {}", this.getClass().getSimpleName());

        this.options = new HttpServiceOptions(properties, this.systemService.getKuraHome());

        activateHttpService();

        logger.info("Activating... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updating {}", this.getClass().getSimpleName());

        HttpServiceOptions updatedOptions = new HttpServiceOptions(properties, this.systemService.getKuraHome());

        if (!this.options.equals(updatedOptions)) {
            logger.debug("Updating, new props");
            this.options = updatedOptions;

            restartHttpService();
        }

        logger.info("Updating... Done.");
    }

    public void deactivate() {
        logger.info("Deactivating {}", this.getClass().getSimpleName());

        deactivateHttpService();
    }

    private synchronized void restartHttpService() {
        deactivateHttpService();
        activateHttpService();
    }

    private synchronized void activateHttpService() {
        try {
            logger.info("starting Jetty instance...");
            this.jettyServerHolder = new JettyServerHolder(this.options, Optional.ofNullable(this.keystoreService),
                    this.dispatcherServlet, this.eventListener);
            logger.info("starting Jetty instance...done");
        } catch (final Exception e) {
            logger.error("Could not start Jetty Web server", e);
        }
    }

    private synchronized void deactivateHttpService() {
        try {
            logger.info("stopping Jetty instance...");
            this.jettyServerHolder.stop();
        } catch (final Exception e) {
            logger.error("Could not stop Jetty Web server", e);
        }
    }

    @Override
    public void handleEvent(final Event event) {
        if (!(event instanceof KeystoreChangedEvent)) {
            return;
        }

        final KeystoreChangedEvent keystoreChangedEvent = (KeystoreChangedEvent) event;

        if (keystoreChangedEvent.getSenderPid().equals(keystoreServicePid)) {
            restartHttpService();
        }
    }

}
