/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.util.jdbc.ConnectionProvider;
import org.eclipse.kura.util.wire.store.AbstractJdbcWireRecordStoreImpl;
import org.eclipse.kura.util.wire.store.JdbcWireRecordStoreQueries;

@SuppressWarnings("restriction")
public class SqliteWireRecordStoreImpl extends AbstractJdbcWireRecordStoreImpl {

    private static final Map<Class<? extends TypedValue<?>>, String> TYPE_MAPPING = buildTypeMapping();

    public SqliteWireRecordStoreImpl(final ConnectionProvider provider, final String tableName)
            throws KuraStoreException {
        super(provider, tableName);

        super.createTable();
        super.createTimestampIndex();
    }

    @Override
    protected JdbcWireRecordStoreQueries buildSqlWireRecordStoreQueries() {
        return JdbcWireRecordStoreQueries.builder()
                .withSqlAddColumn("ALTER TABLE " + super.escapedTableName + " ADD COLUMN {0} {1};")
                .withSqlCreateTable("CREATE TABLE IF NOT EXISTS " + super.escapedTableName
                        + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, TIMESTAMP BIGINT);")
                .withSqlRowCount("SELECT COUNT(*) FROM " + super.escapedTableName + ";")
                .withSqlDeleteRangeTable("DELETE FROM " + super.escapedTableName + " WHERE ID IN (SELECT ID FROM "
                        + super.escapedTableName + " ORDER BY ID ASC LIMIT {0});")
                .withSqlDropColumn("ALTER TABLE " + super.escapedTableName + " DROP COLUMN {0};")
                .withSqlInsertRecord("INSERT INTO " + super.escapedTableName + " ({0}) VALUES ({1});")
                .withSqlTruncateTable("DELETE FROM " + super.escapedTableName + ";")
                .withSqlCreateTimestampIndex(
                        "CREATE INDEX IF NOT EXISTS " + super.escapeIdentifier(tableName + "_TIMESTAMP") + " ON "
                                + super.escapedTableName + " (TIMESTAMP DESC);")
                .build();
    }

    @Override
    protected Optional<String> getMappedSqlType(final TypedValue<?> value) {
        return Optional.ofNullable(value).flatMap(v -> Optional.ofNullable(TYPE_MAPPING.get(v.getClass())));
    }

    private static Map<Class<? extends TypedValue<?>>, String> buildTypeMapping() {
        final Map<Class<? extends TypedValue<?>>, String> result = new HashMap<>();

        result.put(StringValue.class, "TEXT");
        result.put(IntegerValue.class, "INT");
        result.put(LongValue.class, "BIGINT");
        result.put(BooleanValue.class, "BOOLEAN");
        result.put(DoubleValue.class, "DOUBLE");
        result.put(FloatValue.class, "FLOAT");
        result.put(ByteArrayValue.class, "BLOB");

        return Collections.unmodifiableMap(result);
    }

}
