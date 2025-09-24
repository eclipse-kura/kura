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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.io.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpiredConnectionCleanerService {

    private static final Logger logger = LoggerFactory.getLogger(ExpiredConnectionCleanerService.class);

    private Map<Connection, Long> openedConnections = new ConcurrentHashMap<>();

    private ScheduledExecutorService cleanUpExecutor = Executors.newSingleThreadScheduledExecutor();

    private int connectionTimeout;

    private ScheduledFuture<?> task;

    public ExpiredConnectionCleanerService(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        this.task = this.cleanUpExecutor.scheduleAtFixedRate(this::cleanUpTask, 0, 30, TimeUnit.SECONDS);
    }

    private void cleanUpTask() {

        Iterator<Map.Entry<Connection, Long>> connectionIterator = this.openedConnections.entrySet().iterator();
        while (connectionIterator.hasNext()) {
            Entry<Connection, Long> connectionEntry = connectionIterator.next();
            Connection connection = connectionEntry.getKey();

            long startTime = connectionEntry.getValue();
            long currentTime = System.currentTimeMillis();

            if (currentTime - startTime > TimeUnit.MILLISECONDS.convert(this.connectionTimeout, TimeUnit.SECONDS)) {
                connection.close();
                connectionIterator.remove();
                logger.debug("Connection {} evicted due timeout expired.", connection);
            }
        }

    }

    public void add(Connection connection, long timeMillis) {
        this.openedConnections.put(connection, timeMillis);
    }

    public void remove(Connection connection) {
        this.openedConnections.remove(connection);
    }

    public void shutdown() {
        this.task.cancel(true);
        this.cleanUpExecutor.shutdownNow();
    }
}
