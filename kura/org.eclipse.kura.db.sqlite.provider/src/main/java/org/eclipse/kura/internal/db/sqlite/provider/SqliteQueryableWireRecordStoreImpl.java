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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Optional;

import org.eclipse.kura.util.jdbc.ConnectionProvider;
import org.eclipse.kura.util.wire.store.AbstractJdbcQueryableWireRecordStoreImpl;

@SuppressWarnings("restriction")
public class SqliteQueryableWireRecordStoreImpl extends AbstractJdbcQueryableWireRecordStoreImpl {

    protected SqliteQueryableWireRecordStoreImpl(ConnectionProvider provider) {
        super(provider);
    }

    @Override
    protected Optional<Object> extractColumnValue(ResultSet resultSet, ResultSetMetaData metadata, int columnIndex)
            throws SQLException {
        Object dbExtractedData = resultSet.getObject(columnIndex);

        if (dbExtractedData == null) {
            return Optional.empty();
        }

        final String typeName = metadata.getColumnTypeName(columnIndex);

        if ("INT".equals(typeName)) {
            dbExtractedData = resultSet.getInt(columnIndex);
        } else if ("BIGINT".equals(typeName) || "INTEGER".equals(typeName)) {
            dbExtractedData = resultSet.getLong(columnIndex);
        } else if ("BOOLEAN".equals(typeName)) {
            dbExtractedData = resultSet.getBoolean(columnIndex);
        } else if ("BLOB".equals(typeName)) {
            dbExtractedData = resultSet.getBytes(columnIndex);
        }

        return Optional.of(dbExtractedData);
    }
}
