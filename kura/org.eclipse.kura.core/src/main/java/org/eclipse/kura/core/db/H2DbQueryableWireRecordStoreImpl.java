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
import java.util.Optional;

import org.eclipse.kura.util.jdbc.ConnectionProvider;
import org.eclipse.kura.util.wire.store.AbstractJdbcQueryableWireRecordStoreImpl;

@SuppressWarnings("restriction")
public class H2DbQueryableWireRecordStoreImpl extends AbstractJdbcQueryableWireRecordStoreImpl {

    protected H2DbQueryableWireRecordStoreImpl(ConnectionProvider provider) {
        super(provider);
    }

    @Override
    protected Optional<Object> extractColumnValue(final ResultSet rset, final ResultSetMetaData rmet, final int i)
            throws SQLException {
        Object dbExtractedData = rset.getObject(i);

        if (isNull(dbExtractedData)) {
            return Optional.empty();
        }

        if (dbExtractedData instanceof Blob) {
            final Blob dbExtractedBlob = (Blob) dbExtractedData;
            final int dbExtractedBlobLength = (int) dbExtractedBlob.length();
            dbExtractedData = dbExtractedBlob.getBytes(1, dbExtractedBlobLength);
        }

        return Optional.of(dbExtractedData);
    }
}
