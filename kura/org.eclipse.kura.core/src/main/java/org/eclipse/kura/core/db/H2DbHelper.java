/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.db;

import java.sql.SQLException;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.db.H2DbService;
import org.h2.tools.Server;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2DbHelper implements ConfigurableComponent {

    private static final String H2_DB_SERVICE_FACTORY_PID = "org.eclipse.kura.core.db.H2DbService";

    private enum ServerType {
        WEB,
        TCP,
        PG
    };

    private static final Logger logger = LoggerFactory.getLogger(H2DbHelper.class);
    private Server server;
    private ConfigurationService configurationService;

    protected void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    protected void unsetConfigurationService(ConfigurationService configurationService) {
        this.configurationService = null;
    }

    protected void activate(Map<String, Object> properties) {
        logger.info("activating...");
        startDefaultDbServiceInstance();
        updated(properties);
        logger.info("activating...done");
    }

    protected void updated(Map<String, Object> properties) {
        logger.info("updating...");
        restartServer(new ServerConfiguration(properties));
        logger.info("updating...done");
    }

    protected void deactivate() {
        logger.info("deactivating...");
        shutdownServer();
        logger.info("deactivating...done");
    }

    private void startDefaultDbServiceInstance() {
        try {
            if (configurationService.getComponentConfiguration(H2DbService.DEFAULT_INSTANCE_PID) == null) {
                configurationService.createFactoryConfiguration(H2_DB_SERVICE_FACTORY_PID,
                        H2DbService.DEFAULT_INSTANCE_PID, null, true);
            }
        } catch (KuraException e) {
            throw new ComponentException(e);
        }
    }

    private void restartServer(ServerConfiguration configuration) {
        shutdownServer();
        Server server = null;
        if (configuration.isServerEnabled) {
            try {
                logger.info("Starting DB server...");
                final String[] commandline = configuration.serverCommandLine.split(" ");
                logger.debug("Server type: {}, commandline: {}", configuration.serverType, commandline);
                switch (configuration.serverType) {
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

    private static class ServerConfiguration {

        private static final String DB_SERVER_ENABLED_PROP_NAME = "db.server.enabled";
        private static final String DB_SERVER_TYPE_PROP_NAME = "db.server.type";
        private static final String DB_COMMAND_LINE_PROP_NAME = "db.server.commandline";

        Boolean isServerEnabled = false;
        ServerType serverType;
        String serverCommandLine = "-tcpPort 9123 -tcpAllowOthers -ifExists";

        public ServerConfiguration(Map<String, Object> properties) {
            this.isServerEnabled = (Boolean) properties.getOrDefault(DB_SERVER_ENABLED_PROP_NAME, isServerEnabled);
            this.serverCommandLine = (String) properties.getOrDefault(DB_COMMAND_LINE_PROP_NAME, serverCommandLine);

            final String serverTypeString = (String) properties.getOrDefault(DB_SERVER_TYPE_PROP_NAME, "TCP");

            if (ServerType.TCP.name().equals(serverTypeString)) {
                serverType = ServerType.TCP;
            } else if (ServerType.WEB.name().equals(serverTypeString)) {
                serverType = ServerType.WEB;
            } else if (ServerType.PG.name().equals(serverTypeString)) {
                serverType = ServerType.PG;
            } else {
                throw new IllegalArgumentException("Invalid server type: " + serverTypeString);
            }
        }
    }
}
