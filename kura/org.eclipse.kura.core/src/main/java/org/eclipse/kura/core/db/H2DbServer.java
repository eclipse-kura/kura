/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.core.db;

import java.sql.SQLException;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2DbServer implements ConfigurableComponent {

    enum ServerType {
        WEB,
        TCP,
        PG
    }

    private static final Logger logger = LoggerFactory.getLogger(H2DbServer.class);
    private Server server;

    protected void activate(Map<String, Object> properties) {
        logger.info("activating...");
        updated(properties);
        logger.info("activating...done");
    }

    protected void updated(Map<String, Object> properties) {
        logger.info("updating...");
        restartServer(new H2DbServerOptions(properties));
        logger.info("updating...done");
    }

    protected void deactivate() {
        logger.info("deactivating...");
        shutdownServer();
        logger.info("deactivating...done");
    }

    private void restartServer(H2DbServerOptions configuration) {
        shutdownServer();
        Server server = null;
        if (configuration.isServerEnabled()) {
            try {
                logger.info("Starting DB server...");
                final String[] commandline = configuration.getServerCommandLine().split(" ");
                logger.debug("Server type: {}, commandline: {}", configuration.getServerType(), commandline);
                switch (configuration.getServerType()) {
                case TCP:
                    server = Server.createTcpServer(commandline);
                    break;
                case WEB:
                    server = Server.createWebServer(commandline);
                    break;
                case PG:
                    server = Server.createPgServer(commandline);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown server type");
                }
                server.start();
                this.server = server;
                logger.info("Starting DB server...done");
            } catch (SQLException e) {
                logger.error("Failed to start server", e);
                shutdownServer(server);
            }
        }
    }

    private void shutdownServer() {
        shutdownServer(this.server);
        this.server = null;
    }

    private void shutdownServer(Server server) {
        if (server != null) {
            try {
                logger.info("Shutting down DB server...");
                server.stop();
                logger.info("Shutting down DB server...done");
            } catch (Exception e) {
                logger.error("failed to shutdown DB server", e);
            }
        }
    }
}
