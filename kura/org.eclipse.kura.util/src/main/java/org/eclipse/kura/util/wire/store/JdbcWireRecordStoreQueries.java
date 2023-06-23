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

import static java.util.Objects.requireNonNull;

public class JdbcWireRecordStoreQueries {

    private final String sqlAddColumn;
    private final String sqlDropColumn;
    private final String sqlCreateTable;
    private final String sqlRowCount;
    private final String sqlDeleteRangeTable;
    private final String sqlInsertRecord;
    private final String sqlTruncateTable;
    private final String sqlCreateTimestampIndex;

    private JdbcWireRecordStoreQueries(Builder builder) {
        this.sqlAddColumn = requireNonNull(builder.sqlAddColumn);
        this.sqlDropColumn = requireNonNull(builder.sqlDropColumn);
        this.sqlCreateTable = requireNonNull(builder.sqlCreateTable);
        this.sqlRowCount = requireNonNull(builder.sqlRowCount);
        this.sqlDeleteRangeTable = requireNonNull(builder.sqlDeleteRangeTable);
        this.sqlInsertRecord = requireNonNull(builder.sqlInsertRecord);
        this.sqlTruncateTable = requireNonNull(builder.sqlTruncateTable);
        this.sqlCreateTimestampIndex = requireNonNull(builder.sqlCreateTimestampIndex);
    }

    public String getSqlAddColumn() {
        return sqlAddColumn;
    }

    public String getSqlDropColumn() {
        return sqlDropColumn;
    }

    public String getSqlCreateTable() {
        return sqlCreateTable;
    }

    public String getSqlRowCount() {
        return sqlRowCount;
    }

    public String getSqlDeleteRangeTable() {
        return sqlDeleteRangeTable;
    }

    public String getSqlInsertRecord() {
        return sqlInsertRecord;
    }

    public String getSqlTruncateTable() {
        return sqlTruncateTable;
    }

    public String getSqlCreateTimestampIndex() {
        return sqlCreateTimestampIndex;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String sqlAddColumn;
        private String sqlDropColumn;
        private String sqlCreateTable;
        private String sqlRowCount;
        private String sqlDeleteRangeTable;
        private String sqlInsertRecord;
        private String sqlTruncateTable;
        private String sqlCreateTimestampIndex;

        public Builder withSqlAddColumn(String sqlAddColumn) {
            this.sqlAddColumn = sqlAddColumn;
            return this;
        }

        public Builder withSqlDropColumn(String sqlDropColumn) {
            this.sqlDropColumn = sqlDropColumn;
            return this;
        }

        public Builder withSqlCreateTable(String sqlCreateTable) {
            this.sqlCreateTable = sqlCreateTable;
            return this;
        }

        public Builder withSqlRowCount(String sqlRowCount) {
            this.sqlRowCount = sqlRowCount;
            return this;
        }

        public Builder withSqlDeleteRangeTable(String sqlDeleteRangeTable) {
            this.sqlDeleteRangeTable = sqlDeleteRangeTable;
            return this;
        }

        public Builder withSqlInsertRecord(String sqlInsertRecord) {
            this.sqlInsertRecord = sqlInsertRecord;
            return this;
        }

        public Builder withSqlTruncateTable(String sqlTruncateTable) {
            this.sqlTruncateTable = sqlTruncateTable;
            return this;
        }

        public Builder withSqlCreateTimestampIndex(String sqlCreateTimestampIndex) {
            this.sqlCreateTimestampIndex = sqlCreateTimestampIndex;
            return this;
        }

        public JdbcWireRecordStoreQueries build() {
            return new JdbcWireRecordStoreQueries(this);
        }
    }
}
