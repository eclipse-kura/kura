/*******************************************************************************
 * Copyright (c) 2022, 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.db.sqlite.provider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.db.BaseDbService;
import org.eclipse.kura.util.configuration.Property;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
@Component(
    name = "org.eclipse.kura.internal.db.sqlite.provider.SqliteDebugShell",
    service = { org.eclipse.kura.internal.db.sqlite.provider.SqliteDebugShell.class },
    property = {
        "osgi.command.scope=sqlitedbg",
        "osgi.command.function=executeQuery" })
public class SqliteDebugShell {

    private static final Property<String> KURA_SERVICE_PID = new Property<>(ConfigurationService.KURA_SERVICE_PID,
            String.class);

    private final Map<String, BaseDbService> dbServices = new HashMap<>();
    private final Set<String> allowedPids = new HashSet<>();

    @Reference(name = "BaseDbService",
            service = org.eclipse.kura.db.BaseDbService.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetDbService")
    public void setDbService(final BaseDbService dbService, final Map<String, Object> properties) {
        final Optional<String> kuraServiePid = KURA_SERVICE_PID.getOptional(properties);

        if (kuraServiePid.isPresent()) {
            dbServices.put(kuraServiePid.get(), dbService);
        }
    }

    public void unsetDbService(final BaseDbService dbService) {
        this.dbServices.values().removeIf(s -> s == dbService);
    }

    synchronized void setPidAllowed(final String pid, final boolean allowed) {
        if (!allowed) {
            this.allowedPids.remove(pid);
        } else {
            this.allowedPids.add(pid);
        }
    }

    public synchronized void executeQuery(final String dbServicePid, final String sql) throws SQLException {
        if (!allowedPids.contains(dbServicePid) || !dbServices.containsKey(dbServicePid)) {
            throw new IllegalArgumentException("Database instance with pid " + dbServicePid + " is not available");
        }

        final BaseDbService dbService = this.dbServices.get(dbServicePid);

        try (final Connection conn = dbService.getConnection(); final Statement statement = conn.createStatement()) {

            final boolean hasResultSet = statement.execute(sql);

            if (!hasResultSet) {
                System.out.println(statement.getUpdateCount() + " rows changed");
                return;
            }

            try (final ResultSet result = statement.getResultSet()) {

                final ResultSetMetaData meta = result.getMetaData();

                final StringBuilder builder = new StringBuilder();

                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    builder.append("| ").append(meta.getColumnName(i)).append("\t");
                }

                builder.append("|");
                System.out.println(builder.toString());

                while (result.next()) {
                    builder.setLength(0);

                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        builder.append("| ").append(result.getObject(i)).append("\t");
                    }

                    builder.append("|");
                    System.out.println(builder.toString());
                }

            }

        }
    }

}
