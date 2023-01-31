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
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.util.jdbc.ConnectionProvider;
import org.eclipse.kura.util.wire.store.SqlQueryableWireRecordStoreHelper;
import org.eclipse.kura.wire.WireRecord;

@SuppressWarnings("restriction")
public class SqliteQueryableWireRecordStoreImpl {

    private SqliteQueryableWireRecordStoreImpl() {
    }

    public static List<WireRecord> performQuery(final ConnectionProvider provider, final String query)
            throws KuraStoreException {

        return SqlQueryableWireRecordStoreHelper.performQuery(provider, query,
                SqliteQueryableWireRecordStoreImpl::extractColumnResult);
    }

    private static Optional<Object> extractColumnResult(final ResultSet rset, final ResultSetMetaData rmet, final int i)
            throws SQLException {
        Object dbExtractedData = rset.getObject(i);

        if (dbExtractedData == null) {
            return Optional.empty();
        }

        final String typeName = rmet.getColumnTypeName(i);

        if ("INT".equals(typeName)) {
            dbExtractedData = rset.getInt(i);
        } else if ("BIGINT".equals(typeName)) {
            dbExtractedData = rset.getLong(i);
        } else if ("BOOLEAN".equals(typeName)) {
            dbExtractedData = rset.getBoolean(i);
        } else if ("BLOB".equals(typeName)) {
            dbExtractedData = rset.getBytes(i);
        }

        return Optional.of(dbExtractedData);
    }
}
