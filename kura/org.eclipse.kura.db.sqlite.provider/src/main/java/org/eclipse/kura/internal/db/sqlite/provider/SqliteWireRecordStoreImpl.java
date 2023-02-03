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
import java.util.List;
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
import org.eclipse.kura.util.wire.store.SqlWireRecordStoreHelper;
import org.eclipse.kura.util.wire.store.SqlWireRecordStoreQueries;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.store.provider.WireRecordStore;

@SuppressWarnings("restriction")
public class SqliteWireRecordStoreImpl implements WireRecordStore {

    private static final Map<Class<? extends TypedValue<?>>, String> TYPE_MAPPING = buildTypeMapping();

    private final SqlWireRecordStoreHelper helper;

    public SqliteWireRecordStoreImpl(final ConnectionProvider provider, final String tableName)
            throws KuraStoreException {
        final String sanitizedTableName = sanitizeSql(tableName);

        final SqlWireRecordStoreQueries queries = SqlWireRecordStoreQueries.builder()
                .withSqlAddColumn("ALTER TABLE " + sanitizedTableName + " ADD COLUMN {0} {1};")
                .withSqlCreateTable("CREATE TABLE IF NOT EXISTS " + sanitizedTableName
                        + " (ID INTEGER PRIMARY KEY, TIMESTAMP BIGINT);")
                .withSqlRowCount("SELECT COUNT(*) FROM " + sanitizedTableName + ";")
                .withSqlDeleteRangeTable("DELETE FROM " + sanitizedTableName + " WHERE ID IN (SELECT ID FROM "
                        + sanitizedTableName + " ORDER BY ID ASC LIMIT {0});")
                .withSqlDropColumn("ALTER TABLE " + sanitizedTableName + " DROP COLUMN {0};")
                .withSqlInsertRecord("INSERT INTO " + sanitizedTableName + " ({0}) VALUES ({1});")
                .withSqlTruncateTable("TRUNCATE TABLE " + sanitizedTableName + ";")
                .withSqlCreateTimestampIndex("CREATE INDEX IF NOT EXISTS " + sanitizeSql(tableName + "_TIMESTAMP")
                        + " ON " + sanitizedTableName + " (TIMESTAMP DESC);")
                .build();

        this.helper = new SqlWireRecordStoreHelper(provider, tableName, queries, this::getJdbcType, this::sanitizeSql);

        this.helper.createTable();
        this.helper.createTimestampIndex();
    }

    @Override
    public synchronized void truncate(final int noOfRecordsToKeep) throws KuraStoreException {
        this.helper.truncate(noOfRecordsToKeep);
    }

    @Override
    public synchronized int getSize() throws KuraStoreException {
        return this.helper.getSize();
    }

    @Override
    public synchronized void insertRecords(final List<WireRecord> records) throws KuraStoreException {
        this.helper.insertRecords(records);
    }

    @Override
    public void close() {
        // nothing to shutdown
    }

    private String sanitizeSql(final String string) {
        final String sanitizedName = string.replace("\"", "\"\"");
        return "\"" + sanitizedName + "\"";
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

    private Optional<String> getJdbcType(final TypedValue<?> value) {
        return Optional.ofNullable(value).flatMap(v -> Optional.ofNullable(TYPE_MAPPING.get(v.getClass())));
    }

}
