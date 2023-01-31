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
package org.eclipse.kura.util.wire.store;

import static java.util.Objects.isNull;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.jdbc.ConnectionProvider;
import org.eclipse.kura.wire.WireRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlQueryableWireRecordStoreHelper {

    private static final Logger logger = LoggerFactory.getLogger(SqlQueryableWireRecordStoreHelper.class);

    private SqlQueryableWireRecordStoreHelper() {
    }

    public static List<WireRecord> performQuery(final ConnectionProvider provider, final String query,
            final ColumnExtractor extractor)
            throws KuraStoreException {

        try {
            return provider.withConnection(c -> {
                try (final Statement stmt = c.createStatement();
                        final ResultSet rset = stmt.executeQuery(query)) {
                    final List<WireRecord> dataRecords = new ArrayList<>();

                    while (rset.next()) {
                        final WireRecord wireRecord = new WireRecord(convertSQLRowToWireRecord(rset, extractor));
                        dataRecords.add(wireRecord);
                    }

                    return dataRecords;
                }
            });
        } catch (final Exception e) {
            throw new KuraStoreException(e, null);
        }

    }

    public static Map<String, TypedValue<?>> convertSQLRowToWireRecord(final ResultSet rset,
            final ColumnExtractor extractor)
            throws SQLException {
        final Map<String, TypedValue<?>> wireRecordProperties = new HashMap<>();
        final ResultSetMetaData rmet = rset.getMetaData();
        for (int i = 1; i <= rmet.getColumnCount(); i++) {
            String fieldName = rmet.getColumnLabel(i);

            if (isNull(fieldName)) {
                fieldName = rmet.getColumnName(i);
            }

            final Optional<Object> dbExtractedData = extractor.extract(rset, rmet, i);

            if (!dbExtractedData.isPresent()) {
                continue;
            }

            try {
                final TypedValue<?> value = TypedValues.newTypedValue(dbExtractedData.get());
                wireRecordProperties.put(fieldName, value);
            } catch (final Exception e) {
                logger.error(
                        "Failed to convert result for column {} (SQL type {}, Java type {}) "
                                + "to any of the supported Wires data type, "
                                + "please consider using a conversion function like CAST in your query. "
                                + "The result for this column will not be included in emitted envelope",
                        fieldName, rmet.getColumnTypeName(i), dbExtractedData.getClass().getName(), e);
            }

        }
        return wireRecordProperties;
    }

    public interface ColumnExtractor {
        public Optional<Object> extract(ResultSet resultSet, ResultSetMetaData metadata, int index) throws SQLException;
    }
}
