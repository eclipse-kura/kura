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
import static org.eclipse.kura.util.jdbc.JdbcUtil.getFirstColumnValue;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.message.store.StoredMessage;
import org.eclipse.kura.message.store.provider.MessageStore;
import org.eclipse.kura.util.jdbc.ConnectionProvider;
import org.eclipse.kura.util.message.store.SqlMessageStoreHelper;
import org.eclipse.kura.util.message.store.SqlMessageStoreQueries;
import org.h2.api.ErrorCode;

@SuppressWarnings("restriction")
public class H2DbMessageStoreImpl implements MessageStore {

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

    private final Calendar utcCalendar;

    private final String tableName;
    private final String sanitizedTableName;

    private final String sqlSetNextId;
    private final String sqlGetFreeId;

    private SqlMessageStoreHelper helper;

    public H2DbMessageStoreImpl(final ConnectionProvider provider, final String table)
            throws KuraStoreException {

        this.utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        this.tableName = table;

        this.sanitizedTableName = sanitizeSql(table);

        this.sqlSetNextId = ALTER_TABLE + this.sanitizedTableName + " ALTER COLUMN id RESTART WITH ?;";
        this.sqlGetFreeId = "SELECT A.X FROM SYSTEM_RANGE(1, 2147483647) AS A LEFT OUTER JOIN "
                + this.sanitizedTableName + " AS B ON A.X = B.ID WHERE B.ID IS NULL LIMIT 1";

        final SqlMessageStoreQueries queries = SqlMessageStoreQueries.builder()
                .withSqlCreateTable("CREATE TABLE IF NOT EXISTS " + this.sanitizedTableName
                        + " (id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, topic VARCHAR(32767 CHARACTERS), qos INTEGER, retain BOOLEAN, "
                        + "createdOn TIMESTAMP, publishedOn TIMESTAMP, publishedMessageId INTEGER, confirmedOn TIMESTAMP, "
                        + "smallPayload VARBINARY, largePayload BLOB(16777216), priority INTEGER,"
                        + " sessionId VARCHAR(32767 CHARACTERS), droppedOn TIMESTAMP);")
                .withSqlMessageCount("SELECT COUNT(*) FROM " + this.sanitizedTableName + ";")
                .withSqlStore("INSERT INTO " + this.sanitizedTableName
                        + " (topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, smallPayload, largePayload, priority, "
                        + "sessionId, droppedOn) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")
                .withSqlGetMessage(
                        "SELECT id, topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, "
                                + "smallPayload, largePayload, priority, sessionId, droppedOn FROM "
                                + this.sanitizedTableName
                                + " WHERE id = ?")
                .withSqlGetNextMessage("SELECT a.id, a.topic, a.qos, a.retain, a.createdOn, a.publishedOn, "
                        + "a.publishedMessageId, a.confirmedOn, a.smallPayload, a.largePayload, a.priority, a.sessionId, a.droppedOn FROM "
                        + this.sanitizedTableName + " AS a JOIN (SELECT id, publishedOn FROM " + this.sanitizedTableName
                        + " ORDER BY publishedOn ASC NULLS FIRST, priority ASC, createdOn ASC LIMIT 1) AS b "
                        + "WHERE a.id = b.id AND b.publishedOn IS NULL;")
                .withSqlSetPublishedQoS1(UPDATE + this.sanitizedTableName
                        + " SET publishedOn = ?, publishedMessageId = ?, sessionId = ? WHERE id = ?;")
                .withSqlSetPublishedQoS0(UPDATE + this.sanitizedTableName + " SET publishedOn = ? WHERE id = ?;")
                .withSqlSetConfirmed(UPDATE + this.sanitizedTableName + " SET confirmedOn = ? WHERE id = ?;")
                .withSqlAllUnpublishedMessages(SELECT_MESSAGE_METADATA_FROM + this.sanitizedTableName
                        + " WHERE publishedOn IS NULL ORDER BY priority ASC, createdOn ASC;")
                .withSqlAllInFlightMessages(SELECT_MESSAGE_METADATA_FROM + this.sanitizedTableName
                        + " WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL AND droppedOn IS NULL "
                        + "ORDER BY priority ASC, createdOn ASC")
                .withSqlAllDroppedInFlightMessages(SELECT_MESSAGE_METADATA_FROM + this.sanitizedTableName
                        + " WHERE droppedOn IS NOT NULL ORDER BY priority ASC, createdOn ASC;")
                .withSqlUnpublishAllInFlightMessages(UPDATE + this.sanitizedTableName
                        + " SET publishedOn = NULL WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;")
                .withSqlDropAllInFlightMessages(UPDATE + this.sanitizedTableName
                        + " SET droppedOn = ? WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;")
                .withSqlDeleteDroppedMessages(
                        DELETE_FROM + this.sanitizedTableName
                                + " WHERE droppedOn <= DATEADD('MILLISECOND', ?, TIMESTAMP '1970-01-01 00:00:00') AND droppedOn IS NOT NULL;")
                .withSqlDeleteConfirmedMessages(
                        DELETE_FROM + this.sanitizedTableName
                                + " WHERE confirmedOn <= DATEADD('MILLISECOND', ?, TIMESTAMP '1970-01-01 00:00:00') AND confirmedOn IS NOT NULL;")
                .withSqlDeletePublishedMessages(DELETE_FROM + this.sanitizedTableName
                        + " WHERE qos = 0 AND publishedOn <= DATEADD('MILLISECOND', ?, TIMESTAMP '1970-01-01 00:00:00') AND publishedOn IS NOT NULL;")
                .withSqlCreateNextMessageIndex(CREATE_INDEX_IF_NOT_EXISTS + sanitizeSql(this.tableName + "_nextMsg")
                        + " ON " + this.sanitizedTableName + " (publishedOn ASC, priority ASC, createdOn ASC, qos);")
                .withSqlCreatePublishedOnIndex(CREATE_INDEX_IF_NOT_EXISTS + sanitizeSql(this.tableName + "_PUBLISHEDON")
                        + " ON " + this.sanitizedTableName + " (publishedOn DESC);")
                .withSqlCreateConfirmedOnIndex(CREATE_INDEX_IF_NOT_EXISTS + sanitizeSql(this.tableName + "_CONFIRMEDON")
                        + " ON " + this.sanitizedTableName + " (confirmedOn DESC);")
                .withSqlCreateDroppedOnIndex(CREATE_INDEX_IF_NOT_EXISTS + sanitizeSql(this.tableName + "_DROPPEDON")
                        + " ON " + this.sanitizedTableName + " (droppedOn DESC);")
                .build();

        this.helper = SqlMessageStoreHelper.builder()
                .withConnectionProvider(provider)
                .withQueries(queries)
                .withExplicitCommitEnabled(true)
                .build();

        this.helper.createTable();
        this.helper.createIndexes();
    }

    private String sanitizeSql(final String string) {
        final String sanitizedName = string.replace("\"", "\"\"");
        return "\"" + sanitizedName + "\"";
    }

    @Override
    public synchronized int getMessageCount() throws KuraStoreException {

        return (int) this.helper.getMessageCount();
    }

    public synchronized int store(String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraStoreException {

        validate(topic);

        try {
            return storeInternal(topic, payload, qos, retain, priority);
        } catch (KuraStoreException e) {
            handleKuraStoreException(e);
            return storeInternal(topic, payload, qos, retain, priority);
        }

    }

    private void handleKuraStoreException(final KuraStoreException e)
            throws KuraStoreException {

        final Throwable cause = e.getCause();

        if (!(cause instanceof SQLException)) {
            throw e;
        }

        final int errorCode = ((SQLException) cause).getErrorCode();

        if (errorCode == NUMERIC_VALUE_OUT_OF_RANGE_1 || errorCode == NUMERIC_VALUE_OUT_OF_RANGE_2
                || errorCode == ErrorCode.SEQUENCE_EXHAUSTED || errorCode == ErrorCode.DUPLICATE_KEY_1) {

            if (this.helper.getMessageCount() >= Integer.MAX_VALUE) {
                throw new KuraStoreException("Table size is greater or equal than integer max value");
            }

            final int freeId = this.helper.getConnectionProvider().withPreparedStatement(this.sqlGetFreeId,
                    (c, stmt) -> getFirstColumnValue(stmt::executeQuery, ResultSet::getInt), "failed to get free ID");

            this.helper.execute(this.sqlSetNextId, freeId);
            return;
        }

        throw e;
    }

    private void validate(String topic) throws KuraStoreException {

        if (topic == null || topic.trim().length() == 0) {
            throw new KuraStoreException(null, "topic must be not null and not empty");
        }

    }

    private synchronized int storeInternal(String topic, byte[] payload, int qos, boolean retain,
            int priority)
            throws KuraStoreException {

        final Timestamp now = new Timestamp(new Date().getTime());

        return this.helper.getConnectionProvider().withConnection(c -> {

            final int result;

            try (PreparedStatement pstmt = c.prepareStatement(this.helper.getQueries().getSqlStore(),
                    new String[] { "id" })) {
                pstmt.setString(1, topic);
                pstmt.setInt(2, qos);
                pstmt.setBoolean(3, retain);
                pstmt.setTimestamp(4, now, this.utcCalendar);
                pstmt.setTimestamp(5, null);
                pstmt.setInt(6, -1);
                pstmt.setTimestamp(7, null);

                if (isNull(payload) || payload.length < PAYLOAD_BYTE_SIZE_THRESHOLD) {
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

                result = getFirstColumnValue(pstmt::getGeneratedKeys, ResultSet::getInt);

            }

            c.commit();

            return result;
        }, "Cannot store message");

    }

    @Override
    public synchronized Optional<StoredMessage> get(int msgId) throws KuraStoreException {
        return this.helper.get(msgId, this::buildStoredMessage);
    }

    @Override
    public synchronized Optional<StoredMessage> getNextMessage() throws KuraStoreException {
        return this.helper.getNextMessage(this::buildStoredMessage);
    }

    @Override
    public synchronized void markAsPublished(int msgId, DataTransportToken token) throws KuraStoreException {
        this.helper.markAsPublished(msgId, token);
    }

    @Override
    public synchronized void markAsPublished(int msgId) throws KuraStoreException {

        this.helper.markAsPublished(msgId);
    }

    @Override
    public synchronized void markAsConfirmed(int msgId) throws KuraStoreException {

        this.helper.markAsConfirmed(msgId);
    }

    @Override
    public synchronized List<StoredMessage> getUnpublishedMessages() throws KuraStoreException {
        return this.helper.getUnpublishedMessages();
    }

    @Override
    public synchronized List<StoredMessage> getInFlightMessages() throws KuraStoreException {
        return this.helper.getInFlightMessages();
    }

    @Override
    public synchronized List<StoredMessage> getDroppedMessages() throws KuraStoreException {
        return this.helper.getDroppedMessages();
    }

    @Override
    public synchronized void unpublishAllInFlighMessages() throws KuraStoreException {

        this.helper.unpublishAllInFlighMessages();
    }

    @Override
    public synchronized void dropAllInFlightMessages() throws KuraStoreException {

        this.helper.dropAllInFlightMessages();
    }

    @Override
    public synchronized void deleteStaleMessages(int purgeAge) throws KuraStoreException {

        this.helper.deleteStaleMessages(purgeAge);
    }

    private StoredMessage buildStoredMessage(ResultSet rs) throws SQLException {
        StoredMessage.Builder builder = this.helper.buildStoredMessageBuilder(rs, false);

        byte[] payload = rs.getBytes("smallPayload");
        if (payload == null) {
            payload = rs.getBytes("largePayload");
        }

        builder = builder.withPayload(payload);
        return builder.build();
    }

    @Override
    public void close() {
        // nothing to close
    }

}
