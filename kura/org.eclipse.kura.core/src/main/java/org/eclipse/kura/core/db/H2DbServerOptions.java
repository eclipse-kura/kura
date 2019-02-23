/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
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
            serverType = ServerType.WEB;
        } else if (ServerType.PG.name().equals(serverTypeString)) {
            serverType = ServerType.PG;
        } else {
            serverType = ServerType.TCP;
        }
    }

    public Boolean isServerEnabled() {
        return isServerEnabled;
    }

    public ServerType getServerType() {
        return serverType;
    }

    public String getServerCommandLine() {
        return serverCommandLine;
    }
}