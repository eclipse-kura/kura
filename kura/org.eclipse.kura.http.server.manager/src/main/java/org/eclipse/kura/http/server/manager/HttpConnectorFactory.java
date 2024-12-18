/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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

import java.util.EnumSet;

import org.apache.felix.http.jetty.ConnectorFactory;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public class HttpConnectorFactory implements ConnectorFactory {

    private final int port;

    public HttpConnectorFactory(int port) {
        this.port = port;
    }

    @Override
    public Connector createConnector(Server server) {
        HttpConnectionFactory connFactory = new HttpConnectionFactory();
        configureHttpConnectionFactory(connFactory);

        ServerConnector connector = new ServerConnector(server, config.getAcceptors(), config.getSelectors(),
                connFactory);

        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.addCustomizer(new BlockHttpMethods(EnumSet.of(HttpMethod.TRACE)));

        for (final int port : ports) {
            final ServerConnector newConnector = new ServerConnector(serverConnector.getServer(),
                    new HttpConnectionFactory(httpConfiguration));

            customizeConnector(newConnector, port);
            serverConnector.getServer().addConnector(newConnector);
        }
    }

}
