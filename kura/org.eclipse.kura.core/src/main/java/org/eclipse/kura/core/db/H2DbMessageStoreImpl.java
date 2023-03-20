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

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.message.store.StoredMessage;
import org.eclipse.kura.message.store.StoredMessage.Builder;
import org.eclipse.kura.util.jdbc.ConnectionProvider;
import org.eclipse.kura.util.jdbc.JdbcUtil;
import org.eclipse.kura.util.message.store.AbstractJdbcMessageStoreImpl;
import org.eclipse.kura.util.message.store.JdbcMessageStoreQueries;
import org.h2.api.ErrorCode;

@SuppressWarnings("restriction")
public class H2DbMessageStoreImpl extends AbstractJdbcMessageStoreImpl {

    private static final String CREATE_INDEX_IF_NOT_EXISTS = "CREATE INDEX IF NOT EXISTS ";

    private static final String UPDATE = "UPDATE ";

    private static final String DELETE_FROM = "DELETE FROM ";

    private static final String SELECT_MESSAGE_METADATA_FROM = "SELECT id, topic, qos, retain, createdOn, publishedOn, "
            + "publishedMessageId, confirmedOn, priority, sessionId, droppedOn FROM ";

    private static final String ALTER_TABLE = "ALTER TABLE ";

    private static final int PAYLOAD_BYTE_SIZE_THRESHOLD = 200;
    /**
     * The error with code 22003 is thrown when a value is out of range when
     * converting to another data type.
     */
    private static final int NUMERIC_VALUE_OUT_OF_RANGE_1 = 22003;
    /**
     * The error with code 22004 is thrown when a value is out of range when
     * converting to another column's data type.
     */
    private static final int NUMERIC_VALUE_OUT_OF_RANGE_2 = 22004;

    private String sqlSetNextId;
    private String sqlGetFreeId;

    public H2DbMessageStoreImpl(final ConnectionProvider provider, final String table) throws KuraStoreException {
        super(provider, table);

        initDb();
    }

    private void initDb() throws KuraStoreException {
        this.sqlSetNextId = ALTER_TABLE + super.escapedTableName + " ALTER COLUMN id RESTART WITH ?;";
        this.sqlGetFreeId = "SELECT A.X FROM SYSTEM_RANGE(1, 2147483647) AS A LEFT OUTER JOIN " + this.escapedTableName
                + " AS B ON A.X = B.ID WHERE B.ID IS NULL LIMIT 1";

        super.createTable();
        super.createIndexes();
    }

    @Override
    protected JdbcMessageStoreQueries buildSqlMessageStoreQueries() {

        return JdbcMessageStoreQueries.builder().withSqlCreateTable("CREATE TABLE IF NOT EXISTS "
                + super.escapedTableName
                + " (id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, topic VARCHAR(32767 CHARACTERS), qos INTEGER, retain BOOLEAN, "
                + "createdOn TIMESTAMP, publishedOn TIMESTAMP, publishedMessageId INTEGER, confirmedOn TIMESTAMP, "
                + "smallPayload VARBINARY, largePayload BLOB(16777216), priority INTEGER,"
                + " sessionId VARCHAR(32767 CHARACTERS), droppedOn TIMESTAMP);")
                .withSqlMessageCount("SELECT COUNT(*) FROM " + super.escapedTableName + ";")
                .withSqlStore("INSERT INTO " + super.escapedTableName
                        + " (topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, smallPayload, largePayload, priority, "
                        + "sessionId, droppedOn) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")
                .withSqlGetMessage(
                        "SELECT id, topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, "
                                + "smallPayload, largePayload, priority, sessionId, droppedOn FROM "
                                + this.escapedTableName + " WHERE id = ?")
                .withSqlGetNextMessage("SELECT a.id, a.topic, a.qos, a.retain, a.createdOn, a.publishedOn, "
                        + "a.publishedMessageId, a.confirmedOn, a.smallPayload, a.largePayload, a.priority, a.sessionId, a.droppedOn FROM "
                        + this.escapedTableName + " AS a JOIN (SELECT id, publishedOn FROM " + super.escapedTableName
                        + " ORDER BY publishedOn ASC NULLS FIRST, priority ASC, createdOn ASC LIMIT 1) AS b "
                        + "WHERE a.id = b.id AND b.publishedOn IS NULL;")
                .withSqlSetPublishedQoS1(UPDATE + super.escapedTableName
                        + " SET publishedOn = ?, publishedMessageId = ?, sessionId = ? WHERE id = ?;")
                .withSqlSetPublishedQoS0(UPDATE + super.escapedTableName + " SET publishedOn = ? WHERE id = ?;")
                .withSqlSetConfirmed(UPDATE + this.escapedTableName + " SET confirmedOn = ? WHERE id = ?;")
                .withSqlAllUnpublishedMessages(SELECT_MESSAGE_METADATA_FROM + super.escapedTableName
                        + " WHERE publishedOn IS NULL ORDER BY priority ASC, createdOn ASC;")
                .withSqlAllInFlightMessages(SELECT_MESSAGE_METADATA_FROM + super.escapedTableName
                        + " WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL AND droppedOn IS NULL "
                        + "ORDER BY priority ASC, createdOn ASC")
                .withSqlAllDroppedInFlightMessages(SELECT_MESSAGE_METADATA_FROM + super.escapedTableName
                        + " WHERE droppedOn IS NOT NULL ORDER BY priority ASC, createdOn ASC;")
                .withSqlUnpublishAllInFlightMessages(UPDATE + super.escapedTableName
                        + " SET publishedOn = NULL WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;")
                .withSqlDropAllInFlightMessages(UPDATE + super.escapedTableName
                        + " SET droppedOn = ? WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;")
                .withSqlDeleteDroppedMessages(DELETE_FROM + super.escapedTableName
                        + " WHERE droppedOn <= DATEADD('MILLISECOND', ?, TIMESTAMP '1970-01-01 00:00:00') AND droppedOn IS NOT NULL;")
                .withSqlDeleteConfirmedMessages(DELETE_FROM + super.escapedTableName
                        + " WHERE confirmedOn <= DATEADD('MILLISECOND', ?, TIMESTAMP '1970-01-01 00:00:00') AND confirmedOn IS NOT NULL;")
                .withSqlDeletePublishedMessages(DELETE_FROM + super.escapedTableName
                        + " WHERE qos = 0 AND publishedOn <= DATEADD('MILLISECOND', ?, TIMESTAMP '1970-01-01 00:00:00') AND publishedOn IS NOT NULL;")
                .withSqlCreateNextMessageIndex(
                        CREATE_INDEX_IF_NOT_EXISTS + super.escapeIdentifier(super.tableName + "_nextMsg") + " ON "
                                + super.escapedTableName + " (publishedOn ASC, priority ASC, createdOn ASC, qos);")
                .withSqlCreatePublishedOnIndex(
                        CREATE_INDEX_IF_NOT_EXISTS + super.escapeIdentifier(super.tableName + "_PUBLISHEDON") + " ON "
                                + this.escapedTableName + " (publishedOn DESC);")
                .withSqlCreateConfirmedOnIndex(
                        CREATE_INDEX_IF_NOT_EXISTS + super.escapeIdentifier(super.tableName + "_CONFIRMEDON") + " ON "
                                + this.escapedTableName + " (confirmedOn DESC);")
                .withSqlCreateDroppedOnIndex(
                        CREATE_INDEX_IF_NOT_EXISTS + super.escapeIdentifier(super.tableName + "_DROPPEDON") + " ON "
                                + this.escapedTableName + " (droppedOn DESC);")
                .build();
    }

    @Override
    public synchronized int store(String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraStoreException {

        validate(topic);

        try {
            return (int) storeInternal(topic, payload, qos, retain, priority);
        } catch (KuraStoreException e) {
            handleKuraStoreException(e);
            return (int) storeInternal(topic, payload, qos, retain, priority);
        }

    }

    private void handleKuraStoreException(final KuraStoreException e) throws KuraStoreException {

        final Throwable cause = e.getCause();

        if (!(cause instanceof SQLException)) {
            throw e;
        }

        final int errorCode = ((SQLException) cause).getErrorCode();

        if (errorCode == NUMERIC_VALUE_OUT_OF_RANGE_1 || errorCode == NUMERIC_VALUE_OUT_OF_RANGE_2
                || errorCode == ErrorCode.SEQUENCE_EXHAUSTED || errorCode == ErrorCode.DUPLICATE_KEY_1) {

            if (super.getMessageCountInternal() >= Integer.MAX_VALUE) {
                throw new KuraStoreException("Table size is greater or equal than integer max value");
            }

            final int freeId = super.connectionProvider.withPreparedStatement(this.sqlGetFreeId,
                    (c, stmt) -> JdbcUtil.getFirstColumnValue(stmt::executeQuery, ResultSet::getInt),
                    "failed to get free ID");

            super.execute(this.sqlSetNextId, freeId);
            return;
        }

        throw e;
    }

    @Override
    protected synchronized long storeInternal(String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraStoreException {

        final Timestamp now = new Timestamp(new Date().getTime());

        return super.connectionProvider.withConnection(c -> {

            final long result;

            try (PreparedStatement pstmt = c.prepareStatement(super.queries.getSqlStore(), new String[] { "id" })) {
                pstmt.setString(1, topic);
                pstmt.setInt(2, qos);
                pstmt.setBoolean(3, retain);
                pstmt.setTimestamp(4, now, this.utcCalendar);
                pstmt.setTimestamp(5, null);
                pstmt.setInt(6, -1);
                pstmt.setTimestamp(7, null);

                if (payload == null || payload.length < PAYLOAD_BYTE_SIZE_THRESHOLD) {
                    pstmt.setBytes(8, payload);
                    pstmt.setNull(9, Types.BLOB);
                } else {
                    pstmt.setNull(8, Types.VARBINARY);
                    pstmt.setBinaryStream(9, new ByteArrayInputStream(payload), payload.length);
                }

                pstmt.setInt(10, priority);
                pstmt.setString(11, null);
                pstmt.setTimestamp(12, null);

                pstmt.execute();

                result = (long) JdbcUtil.getFirstColumnValue(pstmt::getGeneratedKeys, ResultSet::getInt);

            }

            c.commit();

            return result;
        }, "Cannot store message");

    }

    @Override
    protected Builder buildStoredMessageBuilder(ResultSet rs, boolean includePayload) throws SQLException {
        StoredMessage.Builder result = super.buildStoredMessageBuilder(rs, false);

        if (includePayload) {
            byte[] payload = rs.getBytes("smallPayload");
            if (payload == null) {
                payload = rs.getBytes("largePayload");
            }

            result = result.withPayload(payload);
        }

        return result;

    }

}
