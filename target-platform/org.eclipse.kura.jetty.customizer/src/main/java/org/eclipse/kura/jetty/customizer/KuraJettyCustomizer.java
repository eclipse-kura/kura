/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Red Hat Inc
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.jetty.customizer;

import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.equinox.http.jetty.JettyCustomizer;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;

public class KuraJettyCustomizer extends JettyCustomizer {

    @Override
    public Object customizeHttpConnector(final Object connector, final Dictionary<String, ?> settings) {
        customizeConnector(connector);
        return connector;
    }

    @Override
    public Object customizeHttpsConnector(final Object connector, final Dictionary<String, ?> settings) {
        customizeConnector(connector);
        return connector;
    }

    private void customizeConnector(Object connector) {
        if (!(connector instanceof ServerConnector)) {
            return;
        }

        final ServerConnector serverConnector = (ServerConnector) connector;

        for (final ConnectionFactory factory : serverConnector.getConnectionFactories()) {
            if (!(factory instanceof HttpConnectionFactory)) {
                continue;
            }

            final HttpConnectionFactory httpConnectionFactory = (HttpConnectionFactory) factory;

            httpConnectionFactory.getHttpConfiguration().setSendServerVersion(false);

            List<Customizer> customizers = httpConnectionFactory.getHttpConfiguration().getCustomizers();
            if (customizers == null) {
                customizers = new LinkedList<>();
                httpConnectionFactory.getHttpConfiguration().setCustomizers(customizers);
            }

            customizers.add(new ForwardedRequestCustomizer());
        }
    }

}
