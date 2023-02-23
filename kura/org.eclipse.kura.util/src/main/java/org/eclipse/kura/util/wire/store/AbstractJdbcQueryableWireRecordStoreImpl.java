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
import static java.util.Objects.requireNonNull;

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

public abstract class AbstractJdbcQueryableWireRecordStoreImpl {

    private static final Logger logger = LoggerFactory.getLogger(AbstractJdbcQueryableWireRecordStoreImpl.class);

    private final ConnectionProvider provider;

    protected AbstractJdbcQueryableWireRecordStoreImpl(final ConnectionProvider provider) {
        this.provider = requireNonNull(provider, "Connection provider cannot be null");
    }

    protected abstract Optional<Object> extractColumnValue(ResultSet resultSet, ResultSetMetaData metadata,
            int columnIndex)
            throws SQLException;

    public List<WireRecord> performQuery(final String query)
            throws KuraStoreException {

        try {
            return provider.withConnection(c -> {
                try (final Statement stmt = c.createStatement();
                        final ResultSet rset = stmt.executeQuery(query)) {
                    final List<WireRecord> dataRecords = new ArrayList<>();

                    while (rset.next()) {
                        final WireRecord wireRecord = new WireRecord(convertSQLRowToWireRecord(rset));
                        dataRecords.add(wireRecord);
                    }

                    return dataRecords;
                }
            });
        } catch (final Exception e) {
            throw new KuraStoreException(e, null);
        }

    }

    protected Map<String, TypedValue<?>> convertSQLRowToWireRecord(final ResultSet rset)
            throws SQLException {
        final Map<String, TypedValue<?>> wireRecordProperties = new HashMap<>();
        final ResultSetMetaData rmet = rset.getMetaData();
        for (int columnIndex = 1; columnIndex <= rmet.getColumnCount(); columnIndex++) {
            String fieldName = rmet.getColumnLabel(columnIndex);

            if (isNull(fieldName)) {
                fieldName = getWireRecordPropertyName(rmet, columnIndex);
            }

            final Optional<Object> dbExtractedData = extractColumnValue(rset, rmet, columnIndex);

            if (!dbExtractedData.isPresent()) {
                continue;
            }

            try {
                final TypedValue<?> value = TypedValues.newTypedValue(dbExtractedData.get());
                wireRecordProperties.put(fieldName, value);
            } catch (final Exception e) {
                handleConversionException(rmet, columnIndex, fieldName, dbExtractedData.get(), e);
            }

        }
        return wireRecordProperties;
    }

    protected String getWireRecordPropertyName(final ResultSetMetaData resultSetMetaData, final int columnIndex)
            throws SQLException {
        return resultSetMetaData.getColumnName(columnIndex);
    }

    protected void handleConversionException(final ResultSetMetaData rmet, int columnIndex, String fieldName,
            final Object dbExtractedData, final Exception e) throws SQLException {
        logger.error(
                "Failed to convert result for column {} (SQL type {}, Java type {}) "
                        + "to any of the supported Wires data type, "
                        + "please consider using a conversion function like CAST in your query. "
                        + "The result for this column will not be included in emitted envelope",
                fieldName, rmet.getColumnTypeName(columnIndex), dbExtractedData.getClass().getName(), e);
    }

}
