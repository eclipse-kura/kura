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
package org.eclipse.kura.http.server.manager;

import java.io.IOException;

import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens a Jetty connector, retrying while its port is still held by a server instance that is
 * being stopped: when the HttpService is restarted, the new Jetty instance may try to bind its
 * ports a few milliseconds before the previous one has released them.
 */
final class ConnectorOpener {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorOpener.class);

    private ConnectorOpener() {
    }

    static void open(final ServerConnector connector, final int attempts, final long retryDelayMillis)
            throws IOException {
        IOException failure = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                connector.open();
                return;
            } catch (final IOException e) {
                failure = e;
                if (attempt < attempts) {
                    logger.warn("Unable to open {}, retrying in {} ms (attempt {} of {})", connector, retryDelayMillis,
                            attempt, attempts);
                    sleep(retryDelayMillis, e);
                }
            }
        }
        throw failure;
    }

    private static void sleep(final long millis, final IOException failure) throws IOException {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure;
        }
    }
}
