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

import java.util.Map;

import org.eclipse.kura.core.db.H2DbServer.ServerType;

class H2DbServerOptions {

    private static final String DB_SERVER_ENABLED_PROP_NAME = "db.server.enabled";
    private static final String DB_SERVER_TYPE_PROP_NAME = "db.server.type";
    private static final String DB_COMMAND_LINE_PROP_NAME = "db.server.commandline";

    private static final Boolean DB_SERVER_ENABLED_DEFAULT = false;
    private static final String DB_SERVER_COMMAND_LINE_DEFAULT = "-tcpPort 9123 -tcpAllowOthers -ifExists";

    private final Boolean isServerEnabled;
    private final ServerType serverType;
    private final String serverCommandLine;

    @SuppressWarnings("unchecked")
    private <T> T getSafe(Object o, T defaultValue) {
        if (defaultValue.getClass().isInstance(o)) {
            return (T) o;
        }
        return defaultValue;
    }

    public H2DbServerOptions(Map<String, Object> properties) {
        this.isServerEnabled = getSafe(properties.get(DB_SERVER_ENABLED_PROP_NAME), DB_SERVER_ENABLED_DEFAULT);
        this.serverCommandLine = getSafe(properties.get(DB_COMMAND_LINE_PROP_NAME), DB_SERVER_COMMAND_LINE_DEFAULT);

        final String serverTypeString = (String) properties.getOrDefault(DB_SERVER_TYPE_PROP_NAME, "TCP");

        if (ServerType.WEB.name().equals(serverTypeString)) {
            this.serverType = ServerType.WEB;
        } else if (ServerType.PG.name().equals(serverTypeString)) {
            this.serverType = ServerType.PG;
        } else {
            this.serverType = ServerType.TCP;
        }
    }

    public Boolean isServerEnabled() {
        return this.isServerEnabled;
    }

    public ServerType getServerType() {
        return this.serverType;
    }

    public String getServerCommandLine() {
        return this.serverCommandLine;
    }
}