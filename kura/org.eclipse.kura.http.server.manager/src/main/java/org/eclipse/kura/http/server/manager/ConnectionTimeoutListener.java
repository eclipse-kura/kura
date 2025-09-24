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

import org.eclipse.jetty.io.Connection;

public class ConnectionTimeoutListener implements Connection.Listener {

    private ExpiredConnectionCleanerService expiredConnectionCleanerService;

    public ConnectionTimeoutListener(ExpiredConnectionCleanerService expiredConnectionCleanerService) {
        this.expiredConnectionCleanerService = expiredConnectionCleanerService;
    }

    @Override
    public void onOpened(Connection connection) {
        this.expiredConnectionCleanerService.add(connection, System.currentTimeMillis());

    }

    @Override
    public void onClosed(Connection connection) {
        this.expiredConnectionCleanerService.remove(connection);
    }

}
