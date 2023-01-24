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
 ******************************************************************************/
package org.eclipse.kura.core.db;

import static java.util.Objects.isNull;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2DbQueryableWireRecordStoreImpl {

    private static final Logger logger = LoggerFactory.getLogger(H2DbQueryableWireRecordStoreImpl.class);

    private H2DbQueryableWireRecordStoreImpl() {
    }

    public static List<WireRecord> performQuery(final H2DbServiceImpl h2Db, final String query)
            throws KuraStoreException {

        try {
            return h2Db.withConnection(c -> {
                final List<WireRecord> dataRecords = new ArrayList<>();

                try (final Statement stmt = c.createStatement(); final ResultSet rset = stmt.executeQuery(query)) {
                    while (rset.next()) {
                        final WireRecord wireRecord = new WireRecord(convertSQLRowToWireRecord(rset));
                        dataRecords.add(wireRecord);
                    }
                }

                return dataRecords;
            });
        } catch (final Exception e) {
            throw new KuraStoreException(e, null);
        }
    }

    private static Map<String, TypedValue<?>> convertSQLRowToWireRecord(final ResultSet rset) throws SQLException {
        final Map<String, TypedValue<?>> wireRecordProperties = new HashMap<>();
        final ResultSetMetaData rmet = rset.getMetaData();
        for (int i = 1; i <= rmet.getColumnCount(); i++) {
            String fieldName = rmet.getColumnLabel(i);
            Object dbExtractedData = rset.getObject(i);

            if (isNull(fieldName)) {
                fieldName = rmet.getColumnName(i);
            }

            if (isNull(dbExtractedData)) {
                continue;
            }

            if (dbExtractedData instanceof Blob) {
                final Blob dbExtractedBlob = (Blob) dbExtractedData;
                final int dbExtractedBlobLength = (int) dbExtractedBlob.length();
                dbExtractedData = dbExtractedBlob.getBytes(1, dbExtractedBlobLength);
            }

            try {
                final TypedValue<?> value = TypedValues.newTypedValue(dbExtractedData);
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
}
