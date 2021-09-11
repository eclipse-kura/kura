/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.db;

import java.sql.SQLException;
import java.util.Map;

import org.eclipse.kura.annotation.ServicePid;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.h2.tools.Server;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = H2DbServerConfig.class,factory = true)
@ServicePid("org.eclipse.kura.core.db.H2DbServer")
@Component(service = ConfigurableComponent.class, configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, name = "org.eclipse.kura.core.db.H2DbServer")
public class H2DbServer implements ConfigurableComponent {

    enum ServerType {
        WEB,
        TCP,
        PG
    }

    private static final Logger logger = LoggerFactory.getLogger(H2DbServer.class);
    private Server server;

    @Activate
    protected void activate(Map<String, Object> properties) {
        logger.info("activating...");
        updated(properties);
        logger.info("activating...done");
    }

    @Modified
    protected void updated(Map<String, Object> properties) {
        logger.info("updating...");
        restartServer(new H2DbServerOptions(properties));
        logger.info("updating...done");
    }

    @Deactivate
    protected void deactivate() {
        logger.info("deactivating...");
        shutdownServer();
        logger.info("deactivating...done");
    }

    private void restartServer(H2DbServerOptions configuration) {
        shutdownServer();
        Server newServer = null;
        if (configuration.isServerEnabled()) {
            try {
                logger.info("Starting DB server...");
                final String[] commandline = configuration.getServerCommandLine().split(" ");
                logger.debug("Server type: {}, commandline: {}", configuration.getServerType(), commandline);
                switch (configuration.getServerType()) {
                case TCP:
                    newServer = Server.createTcpServer(commandline);
                    break;
                case WEB:
                    newServer = Server.createWebServer(commandline);
                    break;
                case PG:
                    newServer = Server.createPgServer(commandline);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown server type");
                }
                newServer.start();
                this.server = newServer;
                logger.info("Starting DB server...done");
            } catch (SQLException e) {
                logger.error("Failed to start server", e);
                shutdownServer(newServer);
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
