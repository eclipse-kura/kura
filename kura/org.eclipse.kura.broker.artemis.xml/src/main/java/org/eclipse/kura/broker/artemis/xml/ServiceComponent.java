/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.broker.artemis.xml;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.broker.artemis.core.ServerConfiguration;
import org.eclipse.kura.broker.artemis.core.ServerManager;
import org.eclipse.kura.broker.artemis.core.UserAuthentication;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceComponent implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ServiceComponent.class);

    private ServerConfiguration configuration;
    private ServerManager server;

    public void activate(final Map<String, Object> properties) throws Exception {
        final ServerConfiguration cfg = parse(properties);
        if (cfg != null) {
            start(cfg);
        }
    }

    public void modified(final Map<String, Object> properties) throws Exception {
        final ServerConfiguration cfg = parse(properties);

        if (this.configuration == cfg) {
            logger.debug("Configuration identical .... skipping update");
            return;
        }

        if (this.configuration != null && this.configuration.equals(cfg)) {
            logger.debug("Configuration equal .... skipping update");
            return;
        }

        stop();
        if (cfg != null) {
            start(cfg);
        }
    }

    public void deactivate() throws Exception {
        stop();
    }

    private void start(final ServerConfiguration configuration) throws Exception {
        logger.info("Starting Artemis");

        this.server = new ServerManager(configuration);
        this.server.start();

        this.configuration = configuration;
    }

    private void stop() throws Exception {
        logger.info("Stopping Artemis");

        if (this.server != null) {
            this.server.stop();
            this.server = null;
        }

        this.configuration = null;
    }

    private ServerConfiguration parse(final Map<String, Object> properties) {

        // is enabled?

        if (!Boolean.TRUE.equals(properties.get("enabled"))) {
            return null;
        }

        // parse broker XML

        final String brokerXml = (String) properties.get("brokerXml");
        if (brokerXml == null || brokerXml.isEmpty()) {
            return null;
        }

        // parse required protocols

        final Set<String> requiredProtocols = new HashSet<>();
        {
            final Object v = properties.get("requiredProtocols");
            if (v instanceof String[]) {
                requiredProtocols.addAll(Arrays.asList((String[]) v));
            } else if (v instanceof String) {
                final String vs = (String) v;
                final String[] reqs = vs.split("\\s*,\\s*");
                requiredProtocols.addAll(Arrays.asList(reqs));
            }
        }

        // create security configuration

        final UserAuthentication.Builder auth = new UserAuthentication.Builder();

        final String defaultUser = (String) properties.get("defaultUser");
        if (defaultUser != null) {
            auth.defaultUser(defaultUser);
        }

        auth.parseUsers((String) properties.get("users"));

        // create result

        final ServerConfiguration cfg = new ServerConfiguration();
        cfg.setBrokerXml(brokerXml);
        cfg.setRequiredProtocols(requiredProtocols);
        cfg.setUserAuthentication(auth.build());
        return cfg;
    }

}
