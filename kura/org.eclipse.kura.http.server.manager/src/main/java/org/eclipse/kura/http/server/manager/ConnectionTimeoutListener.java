/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.io.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionTimeoutListener implements Connection.Listener {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionTimeoutListener.class);

    private Map<Connection, Long> openedConnections = new ConcurrentHashMap<>();

    private ScheduledExecutorService cleanUpExecutor = Executors.newSingleThreadScheduledExecutor();

    private int httpSessionTimeout;

    public ConnectionTimeoutListener(int httpsSessionTimeout) {
        this.httpSessionTimeout = httpsSessionTimeout;
        this.cleanUpExecutor.scheduleAtFixedRate(this::cleanUpTask, 0, 30, TimeUnit.SECONDS);
    }

    @Override
    public void onOpened(Connection connection) {
        this.openedConnections.put(connection, System.currentTimeMillis());
    }

    @Override
    public void onClosed(Connection connection) {
        this.openedConnections.remove(connection);
    }

    private void cleanUpTask() {

        for (Map.Entry<Connection, Long> connectionEntry : openedConnections.entrySet()) {
            Connection connection = connectionEntry.getKey();

            long startTime = TimeUnit.SECONDS.convert(connectionEntry.getValue(), TimeUnit.MILLISECONDS);
            long currentTime = TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);

            if (currentTime - startTime > this.httpSessionTimeout) {
                connection.close();
                this.openedConnections.remove(connection);
                logger.debug("Connection {} evicted due timeout expired.", connection);
            }
        }

    }
}
