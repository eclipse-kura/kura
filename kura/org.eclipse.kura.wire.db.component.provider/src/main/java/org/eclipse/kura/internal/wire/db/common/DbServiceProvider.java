package org.eclipse.kura.internal.wire.db.common;

import java.sql.SQLException;

import org.eclipse.kura.wire.WireRecord;

// change name
public interface DbServiceProvider {

    public void truncate(final int noOfRecordsToKeep, final String tableName, final String limit,
            final String tableSize) throws SQLException;

    public int getTableSize(final String tableName) throws SQLException;

    public void reconcileTable(final String tableName) throws SQLException;

    public void reconcileColumns(final String tableName, final WireRecord wireRecord) throws SQLException;

    public void insertDataRecord(final String tableName, final WireRecord wireRecord) throws SQLException;
}
